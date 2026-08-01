package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.data.dto.SurgeryCostEstimationDTO;
import com.divudi.core.data.dto.SurgeryCostSummaryDTO;
import com.divudi.core.data.inward.PatientEncounterComponentType;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.core.data.inward.TheatreOccupancyStatus;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.EncounterComponent;
import com.divudi.core.entity.inward.PatientTransferRequest;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.EncounterComponentFacade;
import com.divudi.core.facade.PatientTransferRequestFacade;
import com.divudi.core.util.JsfUtil;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.persistence.TemporalType;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.Legend;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;

@Named
@SessionScoped
public class SurgeryCostReportController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    private EncounterComponentFacade encounterComponentFacade;
    @EJB
    private PatientTransferRequestFacade patientTransferRequestFacade;

    private Institution institution;
    private Institution site;
    private Department department;
    private Date fromDate;
    private Date toDate;
    private Item surgeryType;
    private Item surgeryItem;
    private PatientEncounterDto selectedPatient;
    private Staff selectedAdmitDoctor;
    private Staff selectedSurgeon;
    private Staff selectedAssistantSurgeon;
    private RoomFacilityCharge selectedOtRoom;
    private TheatreOccupancyStatus selectedSurgeryStatus;
    private String surgeryCostEstimationReportType;
    private List<SurgeryCostEstimationDTO> surgeryCostEstimationList;
    private List<SurgeryCostSummaryDTO> surgeryCostSummaryList;
    private String surgeryCostBarChartModel;

    private static final int IN_CLAUSE_BATCH_SIZE = 1000;
    private static final List<BillTypeAtomic> DRUG_CHARGE_BILL_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE,
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION
    );

    // =====================================================================
    // 2. UI SUPPORT — dropdown value sources and label lookups.
    //    Used by the .xhtml directly and by the enrichment/export methods
    //    below, so they're declared once, up front.
    // =====================================================================
    public TheatreOccupancyStatus[] getTheatreOccupancyStatusValues() {
        return TheatreOccupancyStatus.values();
    }

    public String getTheatreOccupancyStatusLabel(TheatreOccupancyStatus status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case SCHEDULED:
                return "Scheduled";
            case SENT_TO_THEATRE:
                return "Sent to Theatre";
            case RECEIVED_IN_THEATRE:
                return "Received in Theatre";
            case IN_THEATRE:
                return "In Theatre";
            case PROCEDURE_COMPLETED:
                return "Procedure Completed";
            case IN_RECOVERY:
                return "In Recovery";
            case RETURNED_TO_WARD:
                return "Returned to Ward";
            case CANCELLED:
                return "Cancelled";
            default:
                return status.name();
        }
    }

    private String getSurgeryCostEstimationReportTypeLabel() {
        if (surgeryCostEstimationReportType == null) {
            return "Detail";
        }
        switch (surgeryCostEstimationReportType) {
            case "bySurgeon":
                return "Summary By Surgeon";
            case "bySurgeryType":
                return "Summary By Surgery Type";
            case "bySurgeonAndService":
                return "Summary By Surgeon and Service";
            case "byOtRoom":
                return "Summary By OT Room";
            default:
                return surgeryCostEstimationReportType;
        }
    }

    public String getSurgeryCostBarChartModel() {
        return surgeryCostBarChartModel;
    }

    public void setSurgeryCostBarChartModel(String surgeryCostBarChartModel) {
        this.surgeryCostBarChartModel = surgeryCostBarChartModel;
    }

    public void processSurgeryCostEstimationReport() {
        surgeryCostEstimationList = new ArrayList<>();
        surgeryCostSummaryList = new ArrayList<>();
        surgeryCostBarChartModel = null;

        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return;
        }

        boolean isSummaryMode = surgeryCostEstimationReportType != null
                && !surgeryCostEstimationReportType.isEmpty()
                && !"detail".equals(surgeryCostEstimationReportType);

        if (isSummaryMode) {
            fetchSurgeryCostSummary(surgeryCostEstimationReportType);
            createSurgeryCostEstimationChart();
            return;
        }

        List<SurgeryCostEstimationDTO> list = fetchBaseSurgeryCostEstimationList();
        if (list == null || list.isEmpty()) {
            JsfUtil.addErrorMessage("No records found.");
            return;
        }

        ReportLookups lookups = buildLookups(list);

        enrichSurgeonsAndAssistants(lookups.procIds, lookups.dtosByProcId);
        enrichOtRoomAndStatus(lookups.billIds, lookups.dtoByBillId);
        enrichChildBillCharges(lookups.billIds, lookups.dtoByBillId);
        enrichRoomCharges(lookups.peIds, lookups.dtosByPeId);
        enrichDrugCharges(lookups.peIds, lookups.dtosByPeId);

        computeTotals(list);

        surgeryCostEstimationList = list;
        createSurgeryCostEstimationChart();
    }

    @SuppressWarnings("unchecked")
    private List<SurgeryCostEstimationDTO> fetchBaseSurgeryCostEstimationList() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(1024);

        jpql.append("SELECT new com.divudi.core.data.dto.SurgeryCostEstimationDTO( ")
                .append("sb.id, proc.id, admission.patient.phn, admission.patient.person.name, ")
                .append("admission.bhtNo, admission.dateOfAdmission, room.roomFacilityCharge.name, ")
                .append("item.name, cat.name, admission.id, billPerson.title, billPerson.name) ")
                .append("FROM BilledBill sb ")
                .append("JOIN sb.procedure proc ")
                .append("JOIN proc.item item ")
                .append("LEFT JOIN item.category cat ")
                .append("JOIN sb.patientEncounter admission ")
                .append("LEFT JOIN admission.currentPatientRoom room ")
                .append("LEFT JOIN sb.staff billStaff LEFT JOIN billStaff.person billPerson ")
                .append("WHERE sb.retired = false ")
                .append("AND sb.cancelled = false ")
                .append("AND sb.billType = :surgeryBillType ")
                .append("AND admission.discharged = true ")
                .append("AND admission.dateOfDischarge BETWEEN :fromDate AND :toDate ")
                .append("AND NOT EXISTS ( ")
                .append("  SELECT cb.id FROM CancelledBill cb ")
                .append("  WHERE cb.retired = false AND cb.billedBill = sb) ");

        params.put("surgeryBillType", BillType.SurgeryBill);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        appendOptionalFilters(jpql, params);

        jpql.append(" ORDER BY admission.dateOfDischarge ASC ");

        return (List<SurgeryCostEstimationDTO>) billFacade.findDTOsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    private void appendOptionalFilters(StringBuilder jpql, Map<String, Object> params) {
        if (institution != null) {
            jpql.append(" AND sb.institution = :institution ");
            params.put("institution", institution);
        }
        if (site != null) {
            jpql.append(" AND sb.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" AND sb.department = :department ");
            params.put("department", department);
        }
        if (surgeryType != null) {
            jpql.append(" AND item.category = :surgeryType ");
            params.put("surgeryType", surgeryType);
        }
        if (surgeryItem != null) {
            jpql.append(" AND item = :surgeryItem ");
            params.put("surgeryItem", surgeryItem);
        }

        String patientSearchTerm = resolvePatientSearchTerm();
        if (patientSearchTerm != null) {
            jpql.append(" AND (LOWER(admission.patient.person.name) LIKE :pn ")
                    .append("     OR LOWER(admission.patient.phn) LIKE :pn) ");
            params.put("pn", "%" + patientSearchTerm + "%");
        }

        if (selectedAdmitDoctor != null) {
            jpql.append(" AND admission.referringDoctor = :selectedAdmitDoctor ");
            params.put("selectedAdmitDoctor", selectedAdmitDoctor);
        }
        if (selectedSurgeon != null) {
            jpql.append(" AND EXISTS (SELECT ec.id FROM EncounterComponent ec ")
                    .append("   WHERE ec.patientEncounter = proc AND ec.retired = false ")
                    .append("     AND ec.patientEncounterComponentType = com.divudi.core.data.inward.PatientEncounterComponentType.Performed_By ")
                    .append("     AND ec.staff = :selectedSurgeon) ");
            params.put("selectedSurgeon", selectedSurgeon);
        }
        if (selectedAssistantSurgeon != null) {
            jpql.append(" AND EXISTS (SELECT ec.id FROM EncounterComponent ec ")
                    .append("   WHERE ec.patientEncounter = proc AND ec.retired = false ")
                    .append("     AND ec.patientEncounterComponentType = com.divudi.core.data.inward.PatientEncounterComponentType.Assisted_by ")
                    .append("     AND ec.staff = :selectedAssistantSurgeon) ");
            params.put("selectedAssistantSurgeon", selectedAssistantSurgeon);
        }
        if (selectedOtRoom != null) {
            jpql.append(" AND EXISTS (SELECT ptr.id FROM PatientTransferRequest ptr ")
                    .append("   WHERE ptr.surgeryBill = sb AND ptr.retired = false ")
                    .append("     AND ptr.toRoomFacilityCharge = :selectedOtRoom) ");
            params.put("selectedOtRoom", selectedOtRoom);
        }
        if (selectedSurgeryStatus != null) {
            jpql.append(" AND EXISTS (SELECT ptr.id FROM PatientTransferRequest ptr ")
                    .append("   WHERE ptr.surgeryBill = sb AND ptr.retired = false ")
                    .append("     AND ptr.theatreOccupancyStatus = :selectedSurgeryStatus) ");
            params.put("selectedSurgeryStatus", selectedSurgeryStatus);
        }
    }

    private String resolvePatientSearchTerm() {
        if (selectedPatient == null) {
            return null;
        }
        if (selectedPatient.getPatientName() != null && !selectedPatient.getPatientName().trim().isEmpty()) {
            return selectedPatient.getPatientName().trim().toLowerCase();
        }
        if (selectedPatient.getPhn() != null && !selectedPatient.getPhn().trim().isEmpty()) {
            return selectedPatient.getPhn().trim().toLowerCase();
        }
        return null;
    }

    private ReportLookups buildLookups(List<SurgeryCostEstimationDTO> list) {
        ReportLookups lookups = new ReportLookups();
        for (SurgeryCostEstimationDTO dto : list) {
            initializeDtoDefaults(dto);
            lookups.billIds.add(dto.getSurgeryBillId());
            lookups.procIds.add(dto.getProcedureId());
            lookups.dtoByBillId.put(dto.getSurgeryBillId(), dto);
            lookups.dtosByProcId.computeIfAbsent(dto.getProcedureId(), k -> new ArrayList<>()).add(dto);
            if (dto.getAdmissionId() != null) {
                lookups.peIds.add(dto.getAdmissionId());
                lookups.dtosByPeId.computeIfAbsent(dto.getAdmissionId(), k -> new ArrayList<>()).add(dto);
            }
        }
        return lookups;
    }

    private void initializeDtoDefaults(SurgeryCostEstimationDTO dto) {
        dto.setTotalHospitalCharge(0.0);
        dto.setProfessionalCharge(0.0);
        dto.setTotalAmount(0.0);
        dto.setBillDiscount(0.0);
        dto.setNetAmount(0.0);
        dto.setRoomCharges(0.0);
        dto.setDrugCharges(0.0);
    }

    private void enrichSurgeonsAndAssistants(Set<Long> procIds,
            Map<Long, List<SurgeryCostEstimationDTO>> dtosByProcId) {
        if (procIds.isEmpty()) {
            return;
        }

        String ecJpql = "SELECT ec.patientEncounter.id, ec.patientEncounterComponentType, "
                + " stp.title, stp.name, bfstp.title, bfstp.name "
                + "FROM EncounterComponent ec "
                + "LEFT JOIN ec.staff st LEFT JOIN st.person stp "
                + "LEFT JOIN ec.billFee bf LEFT JOIN bf.staff bfst LEFT JOIN bfst.person bfstp "
                + "WHERE ec.retired = false AND ec.patientEncounter.id IN :procIds "
                + "AND ec.patientEncounterComponentType IN (:perfType, :asstType)";

        for (List<Long> batch : partition(procIds, IN_CLAUSE_BATCH_SIZE)) {
            Map<String, Object> params = new HashMap<>();
            params.put("procIds", batch);
            params.put("perfType", PatientEncounterComponentType.Performed_By);
            params.put("asstType", PatientEncounterComponentType.Assisted_by);

            List<Object[]> rows = billFacade.findAggregates(ecJpql, params);
            if (rows == null) {
                continue;
            }

            for (Object[] row : rows) {
                Long procId = (Long) row[0];
                PatientEncounterComponentType type = (PatientEncounterComponentType) row[1];
                com.divudi.core.data.Title title1 = (com.divudi.core.data.Title) row[2];
                String name1 = (String) row[3];
                com.divudi.core.data.Title title2 = (com.divudi.core.data.Title) row[4];
                String name2 = (String) row[5];

                String name = (name1 != null && !name1.trim().isEmpty()) ? name1.trim()
                        : (name2 != null ? name2.trim() : null);
                if (name == null || name.isEmpty()) {
                    continue;
                }
                com.divudi.core.data.Title title = title1 != null ? title1 : title2;
                String fullName = (title != null ? title + " " : "") + name;

                List<SurgeryCostEstimationDTO> targetDtos = dtosByProcId.get(procId);
                if (targetDtos == null) {
                    continue;
                }
                boolean isPerformer = type == PatientEncounterComponentType.Performed_By;
                for (SurgeryCostEstimationDTO dto : targetDtos) {
                    if (isPerformer) {
                        dto.setSurgeonName(fullName);
                    } else if (type == PatientEncounterComponentType.Assisted_by) {
                        dto.setAssistantSurgeonName(fullName);
                    }
                }
            }
        }
    }



    private void enrichOtRoomAndStatus(Set<Long> billIds,
            Map<Long, SurgeryCostEstimationDTO> dtoByBillId) {
        if (billIds.isEmpty()) {
            return;
        }

        String ptrJpql = "SELECT ptr.surgeryBill.id, ptr.theatreOccupancyStatus, ptr.toRoomFacilityCharge.name "
                + "FROM PatientTransferRequest ptr "
                + "WHERE ptr.retired = false "
                + "AND ptr.surgeryBill.id IN :billIds "
                + "AND ptr.theatreOccupancyStatus IS NOT NULL "
                + "ORDER BY ptr.createdAt ASC";

        for (List<Long> batch : partition(billIds, IN_CLAUSE_BATCH_SIZE)) {
            Map<String, Object> params = new HashMap<>();
            params.put("billIds", batch);

            List<Object[]> rows = billFacade.findAggregates(ptrJpql, params);
            if (rows == null) {
                continue;
            }

            for (Object[] row : rows) {
                Long billId = (Long) row[0];
                TheatreOccupancyStatus status = (TheatreOccupancyStatus) row[1];
                String roomName = (String) row[2];

                SurgeryCostEstimationDTO dto = dtoByBillId.get(billId);
                if (dto == null) {
                    continue;
                }
                if (roomName != null) {
                    dto.setOtRoomName(roomName);
                }
                if (status != null) {
                    dto.setSurgeryStatusLabel(getTheatreOccupancyStatusLabel(status));
                }
            }
        }
    }

    private void enrichChildBillCharges(Set<Long> billIds,
            Map<Long, SurgeryCostEstimationDTO> dtoByBillId) {
        if (billIds.isEmpty()) {
            return;
        }

        String childJpql = "SELECT b.forwardReferenceBill.id, b.surgeryBillType, SUM(b.netTotal), SUM(b.discount) "
                + "FROM Bill b WHERE b.retired = false AND b.cancelled = false "
                + "AND b.forwardReferenceBill.id IN :billIds "
                + "GROUP BY b.forwardReferenceBill.id, b.surgeryBillType";

        for (List<Long> batch : partition(billIds, IN_CLAUSE_BATCH_SIZE)) {
            Map<String, Object> params = new HashMap<>();
            params.put("billIds", batch);

            List<Object[]> childBillsAgg = billFacade.findAggregates(childJpql, params);
            if (childBillsAgg == null) {
                continue;
            }

            for (Object[] row : childBillsAgg) {
                Long parentId = (Long) row[0];
                SurgeryBillType sbType = (SurgeryBillType) row[1];
                double net = toDouble(row[2]);
                double discount = toDouble(row[3]);

                SurgeryCostEstimationDTO dto = dtoByBillId.get(parentId);
                if (dto == null) {
                    continue;
                }
                if (sbType == SurgeryBillType.ProfessionalFee) {
                    dto.setProfessionalCharge(dto.getProfessionalCharge() + net);
                } else if (sbType == SurgeryBillType.Service
                        || sbType == SurgeryBillType.PharmacyItem
                        || sbType == SurgeryBillType.TimedService) {
                    dto.setTotalHospitalCharge(dto.getTotalHospitalCharge() + net);
                }
                dto.setBillDiscount(dto.getBillDiscount() + discount);
            }
        }
    }

    private void enrichRoomCharges(Set<Long> peIds,
            Map<Long, List<SurgeryCostEstimationDTO>> dtosByPeId) {
        if (peIds.isEmpty()) {
            return;
        }

        String roomJpql
                = "SELECT p.patientEncounter.id, "
                + " SUM(p.calculatedRoomCharge - p.discountRoomCharge) "
                + "   + SUM(p.calculatedMoCharge - p.discountMoCharge) "
                + "   + SUM(p.calculatedNursingCharge - p.discountNursingCharge) "
                + "   + SUM(p.calculatedMaintainCharge - p.discountMaintainCharge) "
                + "   + SUM(p.calculatedMedicalCareCharge - p.discountMedicalCareCharge) "
                + "   + SUM(p.calculatedAdministrationCharge - p.discountAdministrationCharge) "
                + "   + SUM(p.calculatedLinenCharge - p.discountLinenCharge) "
                + "   + SUM(COALESCE(( "
                + "         SELECT SUM(t.calculatedCharge - t.discountCharge) "
                + "         FROM PatientRoomTimedItemCharge t "
                + "         WHERE t.patientRoom = p), 0)) "
                + "FROM PatientRoom p "
                + "WHERE p.retired = false AND p.patientEncounter.id IN :peIds "
                + "GROUP BY p.patientEncounter.id";

        for (List<Long> batch : partition(peIds, IN_CLAUSE_BATCH_SIZE)) {
            Map<String, Object> params = new HashMap<>();
            params.put("peIds", batch);

            List<Object[]> roomList = billFacade.findAggregates(roomJpql, params);
            if (roomList == null) {
                continue;
            }

            for (Object[] row : roomList) {
                Long peId = (Long) row[0];
                double totalRoomCharge = toDouble(row[1]);

                List<SurgeryCostEstimationDTO> dtos = dtosByPeId.get(peId);
                if (dtos == null) {
                    continue;
                }
                for (SurgeryCostEstimationDTO dto : dtos) {
                    dto.setRoomCharges(dto.getRoomCharges() + totalRoomCharge);
                }
            }
        }
    }

    private void enrichDrugCharges(Set<Long> peIds,
            Map<Long, List<SurgeryCostEstimationDTO>> dtosByPeId) {
        if (peIds.isEmpty()) {
            return;
        }

        String drugJpql = "SELECT pb.patientEncounter.id, SUM(pb.netTotal) "
                + "FROM Bill pb "
                + "WHERE pb.retired = false "
                + "AND pb.billTypeAtomic IN :btas "
                + "AND pb.patientEncounter.id IN :peIds "
                + "GROUP BY pb.patientEncounter.id";

        for (List<Long> batch : partition(peIds, IN_CLAUSE_BATCH_SIZE)) {
            Map<String, Object> params = new HashMap<>();
            params.put("peIds", batch);
            params.put("btas", DRUG_CHARGE_BILL_TYPES);

            List<Object[]> drugList = billFacade.findAggregates(drugJpql, params);
            if (drugList == null) {
                continue;
            }

            for (Object[] row : drugList) {
                Long peId = (Long) row[0];
                double drugVal = toDouble(row[1]);

                List<SurgeryCostEstimationDTO> dtos = dtosByPeId.get(peId);
                if (dtos == null) {
                    continue;
                }
                for (SurgeryCostEstimationDTO dto : dtos) {
                    dto.setDrugCharges(drugVal);
                }
            }
        }
    }

    private void computeTotals(List<SurgeryCostEstimationDTO> list) {
        for (SurgeryCostEstimationDTO dto : list) {
            double net = dto.getTotalHospitalCharge() + dto.getProfessionalCharge();
            double total = net + dto.getBillDiscount();
            dto.setNetAmount(net);
            dto.setTotalAmount(total);
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchSurgeryCostSummary(String type) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(1024);
        switch (type) {
            case "bySurgeon":
                jpql.append("SELECT new com.divudi.core.data.dto.SurgeryCostSummaryDTO(p.person.name, COUNT(DISTINCT sb.id)) ")
                        .append("FROM BilledBill sb ")
                        .append("LEFT JOIN sb.staff p ")
                        .append("JOIN sb.patientEncounter admission ");
                appendSummaryWhereClause(jpql, params);
                jpql.append(" GROUP BY p.id, p.person.name ORDER BY COUNT(DISTINCT sb.id) DESC");
                break;
            case "bySurgeryType":
                jpql.append("SELECT new com.divudi.core.data.dto.SurgeryCostSummaryDTO(cat.name, COUNT(DISTINCT sb.id)) ")
                        .append("FROM BilledBill sb ")
                        .append("JOIN sb.procedure proc ")
                        .append("JOIN proc.item item ")
                        .append("LEFT JOIN item.category cat ")
                        .append("JOIN sb.patientEncounter admission ");
                appendSummaryWhereClause(jpql, params);
                jpql.append(" GROUP BY cat.id, cat.name ORDER BY COUNT(DISTINCT sb.id) DESC");
                break;
            case "bySurgeonAndService":
                jpql.append("SELECT new com.divudi.core.data.dto.SurgeryCostSummaryDTO(p.person.name, item.name, COUNT(DISTINCT sb.id)) ")
                        .append("FROM BilledBill sb ")
                        .append("JOIN sb.procedure proc ")
                        .append("JOIN proc.item item ")
                        .append("LEFT JOIN sb.staff p ")
                        .append("JOIN sb.patientEncounter admission ");
                appendSummaryWhereClause(jpql, params);
                jpql.append(" GROUP BY p.id, p.person.name, item.id, item.name ORDER BY p.person.name ASC, COUNT(DISTINCT sb.id) DESC");
                break;
            case "byOtRoom":
                jpql.append("SELECT new com.divudi.core.data.dto.SurgeryCostSummaryDTO(room.name, COUNT(DISTINCT sb.id)) ")
                        .append("FROM BilledBill sb ")
                        .append("JOIN sb.procedure proc ")
                        .append("JOIN proc.item item ")
                        .append("JOIN sb.patientEncounter admission ")
                        .append("LEFT JOIN PatientTransferRequest ptr ON ptr.surgeryBill = sb AND ptr.retired = false ")
                        .append("   AND ptr.id = (SELECT MAX(ptr2.id) FROM PatientTransferRequest ptr2 ")
                        .append("                 WHERE ptr2.retired = false AND ptr2.surgeryBill = sb) ")
                        .append("LEFT JOIN ptr.toRoomFacilityCharge room ");
                appendSummaryWhereClause(jpql, params);
                jpql.append(" GROUP BY room.id, room.name ORDER BY COUNT(DISTINCT sb.id) DESC");
                break;
            default:
                JsfUtil.addErrorMessage("Unsupported summary type: " + type);
                return;
        }

        surgeryCostSummaryList = (List<SurgeryCostSummaryDTO>) billFacade.findDTOsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    private void appendSummaryWhereClause(StringBuilder jpql, Map<String, Object> params) {
        appendSummaryWhereClause(jpql, params, BillType.SurgeryBill, null);
    }

    private void appendSummaryWhereClause(StringBuilder jpql, Map<String, Object> params,
            BillType billType, BillTypeAtomic billTypeAtomic) {
        jpql.append("WHERE sb.retired = false ")
                .append("AND sb.cancelled = false ")
                .append("AND sb.billType = :surgeryBillType ");
        params.put("surgeryBillType", billType);

        if (billTypeAtomic != null) {
            jpql.append("AND sb.billTypeAtomic = :surgeryBillTypeAtomic ");
            params.put("surgeryBillTypeAtomic", billTypeAtomic);
        }

        jpql.append("AND admission.discharged = true ")
                .append("AND admission.dateOfDischarge BETWEEN :fromDate AND :toDate ")
                .append("AND NOT EXISTS ( ")
                .append("  SELECT cb.id FROM CancelledBill cb ")
                .append("  WHERE cb.retired = false AND cb.billedBill = sb) ");

        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        appendOptionalFilters(jpql, params);
    }

    private void createSurgeryCostEstimationChart() {
        if ((surgeryCostEstimationList == null || surgeryCostEstimationList.isEmpty())
                && (surgeryCostSummaryList == null || surgeryCostSummaryList.isEmpty())) {
            surgeryCostBarChartModel = null;
            return;
        }

        BarChart barChart = new BarChart();
        BarData barData = new BarData();

        boolean isSummary = surgeryCostEstimationReportType != null
                && !surgeryCostEstimationReportType.isEmpty()
                && !"detail".equals(surgeryCostEstimationReportType);

        if (isSummary) {
            BarDataset dataset = new BarDataset()
                    .setLabel("Surgery Count")
                    .setBackgroundColor(new RGBAColor(41, 128, 185, 0.7))
                    .setBorderColor(new RGBAColor(41, 128, 185, 1.0))
                    .setBorderWidth(1);

            for (SurgeryCostSummaryDTO dto : surgeryCostSummaryList) {
                String label = dto.getLabel1();
                if (dto.getLabel2() != null && !dto.getLabel2().isEmpty()) {
                    label += " - " + dto.getLabel2();
                }
                barData.addLabel(label != null ? label : "Unknown");
                dataset.addData(dto.getCount());
            }
            barData.addDataset(dataset);
        } else {
            BarDataset dataset = new BarDataset()
                    .setLabel("Net Amount (LKR)")
                    .setBackgroundColor(new RGBAColor(41, 128, 185, 0.7))
                    .setBorderColor(new RGBAColor(41, 128, 185, 1.0))
                    .setBorderWidth(1);

            int limit = Math.min(surgeryCostEstimationList.size(), 20);
            for (int i = 0; i < limit; i++) {
                SurgeryCostEstimationDTO dto = surgeryCostEstimationList.get(i);
                String label = dto.getPatientName() != null ? dto.getPatientName() : dto.getPhn();
                barData.addLabel(label != null ? label : "Unknown");
                dataset.addData(dto.getNetAmount() != null ? dto.getNetAmount() : 0.0);
            }
            barData.addDataset(dataset);
        }

        barChart.setData(barData);

        BarOptions barOptionsObj = new BarOptions();
        Plugins plugins = new Plugins();
        String titleText = "Surgery Cost Estimation";
        if (isSummary) {
            titleText += " - " + getSurgeryCostEstimationReportTypeLabel();
        } else {
            titleText += " - Detail (Top 20)";
        }
        plugins.setTitle(new Title().setDisplay(true).setText(titleText));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        barOptionsObj.setPlugins(plugins);

        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions().setBeginAtZero(true));
        barOptionsObj.setScales(scales);

        barChart.setOptions(barOptionsObj);
        surgeryCostBarChartModel = barChart.toJson();
    }

    public void downloadSurgeryCostEstimationExcel() {
        boolean isSummary = surgeryCostEstimationReportType != null
                && !surgeryCostEstimationReportType.isEmpty()
                && !"detail".equals(surgeryCostEstimationReportType);

        if (isSummary) {
            if (surgeryCostSummaryList == null || surgeryCostSummaryList.isEmpty()) {
                JsfUtil.addErrorMessage("No data to export. Please process the report first.");
                return;
            }
        } else {
            if (surgeryCostEstimationList == null || surgeryCostEstimationList.isEmpty()) {
                JsfUtil.addErrorMessage("No data to export. Please process the report first.");
                return;
            }
        }

        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Surgery Cost Estimation");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle infoStyle = workbook.createCellStyle();
            XSSFFont infoFont = workbook.createFont();
            infoFont.setFontHeightInPoints((short) 9);
            infoStyle.setFont(infoFont);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 9);
            headerFont.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 41, (byte) 128, (byte) 185}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle normalStyle = workbook.createCellStyle();
            XSSFFont normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 8);
            normalStyle.setFont(normalFont);
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(normalStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            XSSFDataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));

            XSSFCellStyle totalStyle = workbook.createCellStyle();
            XSSFFont totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalFont.setFontHeightInPoints((short) 9);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 200, (byte) 100}, null));
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalStyle.setBorderBottom(BorderStyle.MEDIUM);
            totalStyle.setBorderTop(BorderStyle.MEDIUM);
            totalStyle.setBorderLeft(BorderStyle.THIN);
            totalStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle totalNumberStyle = workbook.createCellStyle();
            totalNumberStyle.cloneStyleFrom(totalStyle);
            totalNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalNumberStyle.setDataFormat(format.getFormat("#,##0.00"));

            String[] headers;
            if (isSummary) {
                if ("bySurgeon".equals(surgeryCostEstimationReportType)) {
                    headers = new String[]{"SL No", "Surgeon", "Count"};
                } else if ("bySurgeryType".equals(surgeryCostEstimationReportType)) {
                    headers = new String[]{"SL No", "Surgery Type", "Count"};
                } else if ("bySurgeonAndService".equals(surgeryCostEstimationReportType)) {
                    headers = new String[]{"SL No", "Surgeon", "Surgery", "Count"};
                } else if ("byOtRoom".equals(surgeryCostEstimationReportType)) {
                    headers = new String[]{"SL No", "OT Room", "Count"};
                } else {
                    headers = new String[]{"SL No", "Label 1", "Label 2", "Count"};
                }
            } else {
                headers = new String[]{
                    "SL No", "MRN", "Patient Name", "Admission No", "Admission Date", "Bed No",
                    "Surgeon", "Surgery Type", "Service Name", "Room Charges", "Drug Charges",
                    "Total Hospital Charge", "Professional Charge", "Total Amount", "Bill Discount", "Net Amount"
                };
            }

            int rowIdx = 0;

            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(22);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Surgery Cost Costing " + (isSummary ? "Summary" : "Detail") + " Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(headers.length - 1, 3)));

            rowIdx++;

            List<String> activeFilters = new ArrayList<>();
            if (fromDate != null && toDate != null) {
                activeFilters.add("Period: " + sdf.format(fromDate) + " to " + sdf.format(toDate));
            }
            if (institution != null) {
                activeFilters.add("Institution: " + institution.getName());
            }
            if (site != null) {
                activeFilters.add("Site: " + site.getName());
            }
            if (department != null) {
                activeFilters.add("Department: " + department.getName());
            }
            if (selectedPatient != null) {
                activeFilters.add("Patient MRN: " + (selectedPatient.getPatientName() != null ? selectedPatient.getPatientName() : selectedPatient.getPhn()));
            }
            if (selectedAdmitDoctor != null) {
                activeFilters.add("Admit Doctor: " + selectedAdmitDoctor.getPerson().getNameWithTitle());
            }
            if (selectedSurgeon != null) {
                activeFilters.add("Surgeon: " + selectedSurgeon.getPerson().getNameWithTitle());
            }
            if (selectedAssistantSurgeon != null) {
                activeFilters.add("Assistant Surgeon: " + selectedAssistantSurgeon.getPerson().getNameWithTitle());
            }
            if (selectedSurgeryStatus != null) {
                activeFilters.add("Surgery Status: " + getTheatreOccupancyStatusLabel(selectedSurgeryStatus));
            }
            if (surgeryType != null) {
                activeFilters.add("Surgery Type: " + surgeryType.getName());
            }
            if (surgeryItem != null) {
                activeFilters.add("Surgery Name: " + surgeryItem.getName());
            }
            if (surgeryCostEstimationReportType != null && !surgeryCostEstimationReportType.isEmpty()) {
                activeFilters.add("Report Type: " + getSurgeryCostEstimationReportTypeLabel());
            }

            for (String filter : activeFilters) {
                Row fRow = sheet.createRow(rowIdx++);
                fRow.createCell(0).setCellValue(filter);
                fRow.getCell(0).setCellStyle(infoStyle);
            }

            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(25);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int slNo = 1;

            if (isSummary) {
                long totalCount = 0;
                for (SurgeryCostSummaryDTO dto : surgeryCostSummaryList) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(slNo++);
                    row.getCell(0).setCellStyle(normalStyle);

                    if ("bySurgeonAndService".equals(surgeryCostEstimationReportType)) {
                        row.createCell(1).setCellValue(dto.getLabel1());
                        row.getCell(1).setCellStyle(normalStyle);
                        row.createCell(2).setCellValue(dto.getLabel2());
                        row.getCell(2).setCellStyle(normalStyle);
                        Cell c3 = row.createCell(3);
                        c3.setCellValue(dto.getCount());
                        c3.setCellStyle(numberStyle);
                    } else {
                        row.createCell(1).setCellValue(dto.getLabel1());
                        row.getCell(1).setCellStyle(normalStyle);
                        Cell c2 = row.createCell(2);
                        c2.setCellValue(dto.getCount());
                        c2.setCellStyle(numberStyle);
                    }
                    totalCount += dto.getCount();
                }

                Row totalRow = sheet.createRow(rowIdx++);
                totalRow.setHeightInPoints(20);
                Cell lblCell = totalRow.createCell(0);
                lblCell.setCellValue("Grand Total");
                lblCell.setCellStyle(totalStyle);

                int sumCol = "bySurgeonAndService".equals(surgeryCostEstimationReportType) ? 3 : 2;
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, sumCol - 1));
                for (int col = 1; col < sumCol; col++) {
                    totalRow.createCell(col).setCellStyle(totalStyle);
                }
                Cell tc = totalRow.createCell(sumCol);
                tc.setCellValue(totalCount);
                tc.setCellStyle(totalStyle);
            } else {
                double grandHospital = 0;
                double grandProfessional = 0;
                double grandTotalAmt = 0;
                double grandDiscount = 0;
                double grandNet = 0;

                for (SurgeryCostEstimationDTO dto : surgeryCostEstimationList) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(slNo++);
                    row.createCell(1).setCellValue(dto.getPhn());
                    row.createCell(2).setCellValue(dto.getPatientName());
                    row.createCell(3).setCellValue(dto.getAdmissionNo());
                    row.createCell(4).setCellValue(dto.getAdmissionDate() != null ? sdf.format(dto.getAdmissionDate()) : "");
                    row.createCell(5).setCellValue(dto.getBedNo());
                    row.createCell(6).setCellValue(dto.getSurgeonName());
                    row.createCell(7).setCellValue(dto.getSurgeryTypeName());
                    row.createCell(8).setCellValue(dto.getServiceName());

                    Cell c9 = row.createCell(9);
                    c9.setCellValue(dto.getRoomCharges() != null ? dto.getRoomCharges() : 0.0);
                    c9.setCellStyle(numberStyle);

                    Cell c10 = row.createCell(10);
                    c10.setCellValue(dto.getDrugCharges() != null ? dto.getDrugCharges() : 0.0);
                    c10.setCellStyle(numberStyle);

                    Cell c11 = row.createCell(11);
                    c11.setCellValue(dto.getTotalHospitalCharge() != null ? dto.getTotalHospitalCharge() : 0.0);
                    c11.setCellStyle(numberStyle);
                    grandHospital += dto.getTotalHospitalCharge() != null ? dto.getTotalHospitalCharge() : 0.0;

                    Cell c12 = row.createCell(12);
                    c12.setCellValue(dto.getProfessionalCharge() != null ? dto.getProfessionalCharge() : 0.0);
                    c12.setCellStyle(numberStyle);
                    grandProfessional += dto.getProfessionalCharge() != null ? dto.getProfessionalCharge() : 0.0;

                    Cell c13 = row.createCell(13);
                    c13.setCellValue(dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0);
                    c13.setCellStyle(numberStyle);
                    grandTotalAmt += dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0;

                    Cell c14 = row.createCell(14);
                    c14.setCellValue(dto.getBillDiscount() != null ? dto.getBillDiscount() : 0.0);
                    c14.setCellStyle(numberStyle);
                    grandDiscount += dto.getBillDiscount() != null ? dto.getBillDiscount() : 0.0;

                    Cell c15 = row.createCell(15);
                    c15.setCellValue(dto.getNetAmount() != null ? dto.getNetAmount() : 0.0);
                    c15.setCellStyle(numberStyle);
                    grandNet += dto.getNetAmount() != null ? dto.getNetAmount() : 0.0;

                    for (int col = 0; col < 9; col++) {
                        row.getCell(col).setCellStyle(normalStyle);
                    }
                }

                Row totalRow = sheet.createRow(rowIdx++);
                totalRow.setHeightInPoints(20);
                Cell lblCell = totalRow.createCell(0);
                lblCell.setCellValue("Grand Total");
                lblCell.setCellStyle(totalStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 10));

                for (int col = 1; col <= 10; col++) {
                    totalRow.createCell(col).setCellStyle(totalStyle);
                }

                Cell tc11 = totalRow.createCell(11);
                tc11.setCellValue(grandHospital);
                tc11.setCellStyle(totalNumberStyle);

                Cell tc12 = totalRow.createCell(12);
                tc12.setCellValue(grandProfessional);
                tc12.setCellStyle(totalNumberStyle);

                Cell tc13 = totalRow.createCell(13);
                tc13.setCellValue(grandTotalAmt);
                tc13.setCellStyle(totalNumberStyle);

                Cell tc14 = totalRow.createCell(14);
                tc14.setCellValue(grandDiscount);
                tc14.setCellStyle(totalNumberStyle);

                Cell tc15 = totalRow.createCell(15);
                tc15.setCellValue(grandNet);
                tc15.setCellStyle(totalNumberStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] excelBytes = baos.toByteArray();

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            externalContext.setResponseContentLength(excelBytes.length);
            String fileName = "Surgery_Cost_Estimation_Report_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();
            out.write(excelBytes);
            out.flush();

            facesContext.responseComplete();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating Excel: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void downloadSurgeryCostEstimationPdf() {
        boolean isSummary = surgeryCostEstimationReportType != null
                && !surgeryCostEstimationReportType.isEmpty()
                && !"detail".equals(surgeryCostEstimationReportType);

        if (isSummary) {
            if (surgeryCostSummaryList == null || surgeryCostSummaryList.isEmpty()) {
                JsfUtil.addErrorMessage("No data to export. Please process the report first.");
                return;
            }
        } else {
            if (surgeryCostEstimationList == null || surgeryCostEstimationList.isEmpty()) {
                JsfUtil.addErrorMessage("No data to export. Please process the report first.");
                return;
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            String fileName = "Surgery_Cost_Estimation_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";

            Document document = new Document(PageSize.A3.rotate(), 15, 15, 30, 20);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy HH:mm");

            Paragraph titlePara = new Paragraph("Surgery Cost Costing " + (isSummary ? "Summary" : "Detail") + " Report", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(10);
            document.add(titlePara);

            List<String> activeFilters = new ArrayList<>();
            if (fromDate != null && toDate != null) {
                activeFilters.add("Period: " + sdf.format(fromDate) + " to " + sdf.format(toDate));
            }
            if (institution != null) {
                activeFilters.add("Institution: " + institution.getName());
            }
            if (site != null) {
                activeFilters.add("Site: " + site.getName());
            }
            if (department != null) {
                activeFilters.add("Department: " + department.getName());
            }
            if (selectedPatient != null) {
                activeFilters.add("Patient MRN: " + (selectedPatient.getPatientName() != null ? selectedPatient.getPatientName() : selectedPatient.getPhn()));
            }
            if (selectedAdmitDoctor != null) {
                activeFilters.add("Admit Doctor: " + selectedAdmitDoctor.getPerson().getNameWithTitle());
            }
            if (selectedSurgeon != null) {
                activeFilters.add("Surgeon: " + selectedSurgeon.getPerson().getNameWithTitle());
            }
            if (selectedAssistantSurgeon != null) {
                activeFilters.add("Assistant Surgeon: " + selectedAssistantSurgeon.getPerson().getNameWithTitle());
            }
            if (selectedSurgeryStatus != null) {
                activeFilters.add("Surgery Status: " + getTheatreOccupancyStatusLabel(selectedSurgeryStatus));
            }
            if (surgeryType != null) {
                activeFilters.add("Surgery Type: " + surgeryType.getName());
            }
            if (surgeryItem != null) {
                activeFilters.add("Surgery Name: " + surgeryItem.getName());
            }
            if (surgeryCostEstimationReportType != null && !surgeryCostEstimationReportType.isEmpty()) {
                activeFilters.add("Report Type: " + getSurgeryCostEstimationReportTypeLabel());
            }

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(60);
            info.setWidths(new float[]{3f, 3f});
            info.setSpacingAfter(15);
            info.setHorizontalAlignment(Element.ALIGN_LEFT);

            for (int i = 0; i < activeFilters.size(); i += 2) {
                String f1 = activeFilters.get(i);
                String f2 = (i + 1 < activeFilters.size()) ? activeFilters.get(i + 1) : "";
                addInfoRow(info, f1, f2);
            }
            document.add(info);

            String[] headers;
            float[] widths;
            if (isSummary) {
                if ("bySurgeonAndService".equals(surgeryCostEstimationReportType)) {
                    headers = new String[]{"SL No", "Surgeon", "Surgery", "Count"};
                    widths = new float[]{2f, 8f, 8f, 4f};
                } else {
                    String label = "Category";
                    if ("bySurgeon".equals(surgeryCostEstimationReportType)) {
                        label = "Surgeon";
                    } else if ("bySurgeryType".equals(surgeryCostEstimationReportType)) {
                        label = "Surgery Type";
                    } else if ("byOtRoom".equals(surgeryCostEstimationReportType)) {
                        label = "OT Room";
                    }
                    headers = new String[]{"SL No", label, "Count"};
                    widths = new float[]{2f, 12f, 4f};
                }
            } else {
                headers = new String[]{
                    "SL No", "MRN", "Patient Name", "Admission No", "Admission Date", "Bed No",
                    "Surgeon", "Surgery Type", "Service Name", "Room Charges", "Drug Charges",
                    "Total Hospital Charge", "Professional Charge", "Total Amount", "Bill Discount", "Net Amount"
                };
                widths = new float[]{2f, 4f, 8f, 5f, 7f, 4f, 7f, 6f, 8f, 4f, 4f, 6f, 6f, 6f, 5f, 6f};
            }

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(widths);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int slNo = 1;
            DecimalFormat df = new DecimalFormat("#,##0.00");

            if (isSummary) {
                long totalCount = 0;
                for (SurgeryCostSummaryDTO dto : surgeryCostSummaryList) {
                    table.addCell(new Phrase(String.valueOf(slNo++), normalFont));
                    table.addCell(new Phrase(dto.getLabel1() != null ? dto.getLabel1() : "", normalFont));
                    if ("bySurgeonAndService".equals(surgeryCostEstimationReportType)) {
                        table.addCell(new Phrase(dto.getLabel2() != null ? dto.getLabel2() : "", normalFont));
                    }
                    PdfPCell cCount = new PdfPCell(new Phrase(String.valueOf(dto.getCount()), normalFont));
                    cCount.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cCount);
                    totalCount += dto.getCount();
                }

                int sumColspan = "bySurgeonAndService".equals(surgeryCostEstimationReportType) ? 3 : 2;
                PdfPCell totalLblCell = new PdfPCell(new Phrase("Grand Total", boldFont));
                totalLblCell.setColspan(sumColspan);
                totalLblCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(totalLblCell);

                PdfPCell tgCount = new PdfPCell(new Phrase(String.valueOf(totalCount), boldFont));
                tgCount.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(tgCount);
            } else {
                double grandHospital = 0;
                double grandProfessional = 0;
                double grandTotalAmt = 0;
                double grandDiscount = 0;
                double grandNet = 0;

                for (SurgeryCostEstimationDTO dto : surgeryCostEstimationList) {
                    table.addCell(new Phrase(String.valueOf(slNo++), normalFont));
                    table.addCell(new Phrase(dto.getPhn() != null ? dto.getPhn() : "", normalFont));
                    table.addCell(new Phrase(dto.getPatientName() != null ? dto.getPatientName() : "", normalFont));
                    table.addCell(new Phrase(dto.getAdmissionNo() != null ? dto.getAdmissionNo() : "", normalFont));
                    table.addCell(new Phrase(dto.getAdmissionDate() != null ? sdf.format(dto.getAdmissionDate()) : "", normalFont));
                    table.addCell(new Phrase(dto.getBedNo() != null ? dto.getBedNo() : "", normalFont));
                    table.addCell(new Phrase(dto.getSurgeonName() != null ? dto.getSurgeonName() : "", normalFont));
                    table.addCell(new Phrase(dto.getSurgeryTypeName() != null ? dto.getSurgeryTypeName() : "", normalFont));
                    table.addCell(new Phrase(dto.getServiceName() != null ? dto.getServiceName() : "", normalFont));

                    PdfPCell cRoom = new PdfPCell(new Phrase(df.format(dto.getRoomCharges() != null ? dto.getRoomCharges() : 0.0), normalFont));
                    cRoom.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cRoom);

                    PdfPCell cDrug = new PdfPCell(new Phrase(df.format(dto.getDrugCharges() != null ? dto.getDrugCharges() : 0.0), normalFont));
                    cDrug.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cDrug);

                    double hosp = dto.getTotalHospitalCharge() != null ? dto.getTotalHospitalCharge() : 0.0;
                    double prof = dto.getProfessionalCharge() != null ? dto.getProfessionalCharge() : 0.0;
                    double tot = dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0;
                    double disc = dto.getBillDiscount() != null ? dto.getBillDiscount() : 0.0;
                    double net = dto.getNetAmount() != null ? dto.getNetAmount() : 0.0;

                    grandHospital += hosp;
                    grandProfessional += prof;
                    grandTotalAmt += tot;
                    grandDiscount += disc;
                    grandNet += net;

                    PdfPCell cHosp = new PdfPCell(new Phrase(df.format(hosp), normalFont));
                    cHosp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cHosp);

                    PdfPCell cProf = new PdfPCell(new Phrase(df.format(prof), normalFont));
                    cProf.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cProf);

                    PdfPCell cTot = new PdfPCell(new Phrase(df.format(tot), normalFont));
                    cTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cTot);

                    PdfPCell cDisc = new PdfPCell(new Phrase(df.format(disc), normalFont));
                    cDisc.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cDisc);

                    PdfPCell cNet = new PdfPCell(new Phrase(df.format(net), normalFont));
                    cNet.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cNet);
                }

                PdfPCell totalLblCell = new PdfPCell(new Phrase("Grand Total", boldFont));
                totalLblCell.setColspan(11);
                totalLblCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(totalLblCell);

                PdfPCell tgHosp = new PdfPCell(new Phrase(df.format(grandHospital), boldFont));
                tgHosp.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(tgHosp);

                PdfPCell tgProf = new PdfPCell(new Phrase(df.format(grandProfessional), boldFont));
                tgProf.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(tgProf);

                PdfPCell tgTot = new PdfPCell(new Phrase(df.format(grandTotalAmt), boldFont));
                tgTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(tgTot);

                PdfPCell tgDisc = new PdfPCell(new Phrase(df.format(grandDiscount), boldFont));
                tgDisc.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(tgDisc);

                PdfPCell tgNet = new PdfPCell(new Phrase(df.format(grandNet), boldFont));
                tgNet.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(tgNet);
            }

            document.add(table);
            document.close();

            byte[] pdfBytes = baos.toByteArray();
            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseContentLength(pdfBytes.length);
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");

            OutputStream out = externalContext.getResponseOutputStream();
            out.write(pdfBytes);
            out.flush();
            facesContext.responseComplete();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to generate PDF: " + e.getMessage());
        }
    }

    private void addInfoRow(PdfPTable info, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPadding(3);
        info.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(3);
        info.addCell(v);
    }

    private static double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private static List<List<Long>> partition(Collection<Long> ids, int batchSize) {
        List<List<Long>> batches = new ArrayList<>();
        List<Long> current = new ArrayList<>(batchSize);
        for (Long id : ids) {
            current.add(id);
            if (current.size() == batchSize) {
                batches.add(current);
                current = new ArrayList<>(batchSize);
            }
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    public List<SurgeryCostEstimationDTO> getSurgeryCostEstimationList() {
        return surgeryCostEstimationList;
    }

    public void setSurgeryCostEstimationList(List<SurgeryCostEstimationDTO> surgeryCostEstimationList) {
        this.surgeryCostEstimationList = surgeryCostEstimationList;
    }

    public List<SurgeryCostSummaryDTO> getSurgeryCostSummaryList() {
        return surgeryCostSummaryList;
    }

    public void setSurgeryCostSummaryList(List<SurgeryCostSummaryDTO> surgeryCostSummaryList) {
        this.surgeryCostSummaryList = surgeryCostSummaryList;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public PatientEncounterDto getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatient(PatientEncounterDto selectedPatient) {
        this.selectedPatient = selectedPatient;
    }

    public Staff getSelectedAdmitDoctor() {
        return selectedAdmitDoctor;
    }

    public void setSelectedAdmitDoctor(Staff selectedAdmitDoctor) {
        this.selectedAdmitDoctor = selectedAdmitDoctor;
    }

    public Staff getSelectedSurgeon() {
        return selectedSurgeon;
    }

    public void setSelectedSurgeon(Staff selectedSurgeon) {
        this.selectedSurgeon = selectedSurgeon;
    }

    public Staff getSelectedAssistantSurgeon() {
        return selectedAssistantSurgeon;
    }

    public void setSelectedAssistantSurgeon(Staff selectedAssistantSurgeon) {
        this.selectedAssistantSurgeon = selectedAssistantSurgeon;
    }

    public TheatreOccupancyStatus getSelectedSurgeryStatus() {
        return selectedSurgeryStatus;
    }

    public void setSelectedSurgeryStatus(TheatreOccupancyStatus selectedSurgeryStatus) {
        this.selectedSurgeryStatus = selectedSurgeryStatus;
    }

    public Item getSurgeryType() {
        return surgeryType;
    }

    public void setSurgeryType(Item surgeryType) {
        this.surgeryType = surgeryType;
    }

    public RoomFacilityCharge getSelectedOtRoom() {
        return selectedOtRoom;
    }

    public void setSelectedOtRoom(RoomFacilityCharge selectedOtRoom) {
        this.selectedOtRoom = selectedOtRoom;
    }

    public Item getSurgeryItem() {
        return surgeryItem;
    }

    public void setSurgeryItem(Item surgeryItem) {
        this.surgeryItem = surgeryItem;
    }

    public String getSurgeryCostEstimationReportType() {
        return surgeryCostEstimationReportType;
    }

    public void setSurgeryCostEstimationReportType(String surgeryCostEstimationReportType) {
        this.surgeryCostEstimationReportType = surgeryCostEstimationReportType;
    }

    private static final class ReportLookups {

        final Set<Long> billIds = new LinkedHashSet<>();
        final Set<Long> procIds = new LinkedHashSet<>();
        final Set<Long> peIds = new LinkedHashSet<>();
        final Map<Long, SurgeryCostEstimationDTO> dtoByBillId = new HashMap<>();
        final Map<Long, List<SurgeryCostEstimationDTO>> dtosByProcId = new HashMap<>();
        final Map<Long, List<SurgeryCostEstimationDTO>> dtosByPeId = new HashMap<>();
    }
}

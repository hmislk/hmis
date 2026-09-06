package com.divudi.bean.inward;

import com.divudi.core.data.dto.PatientEncounterDto;
import com.divudi.core.data.dto.RoomOccupancyReportDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.util.stream.Collectors;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;

@Named
@SessionScoped
public class RoomOccupancyReportController implements Serializable {

    @EJB
    private PatientRoomFacade patientRoomFacade;

    private Date fromDate;
    private Date toDate;

    private PatientEncounterDto selectedPatient;
    private Department ward;
    private RoomFacilityCharge roomFacilityCharge;
    private AdmissionType admissionType;
    private RoomCategory roomCategory;
    private Institution institution;
    private Institution site;

    private String reportType = "Detail"; // "Detail", "Summary By Room Category", "Summary By Admission Type"
    private String summaryType = "Category"; // Internal toggle for which summary to render

    private List<RoomOccupancyReportDto> detailDtos;
    private List<RoomOccupancyReportDto> summaryDtos;

    public RoomOccupancyReportController() {
    }

    public void processReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both discharge dates.");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("Discharge from date must not be after discharge to date.");
            return;
        }
        if ("Detail".equals(reportType)) {
            processDetailReport();
        } else {
            processSummaryReport();
        }
    }

    private void processDetailReport() {
        detailDtos = new ArrayList<>();
        summaryDtos = new ArrayList<>();

        StringBuilder jpql = new StringBuilder();
        jpql.append(" select new com.divudi.core.data.dto.RoomOccupancyReportDto( ")
                .append("     pat.phn, ")
                .append("     per.name, ")
                .append("     adm.dateOfAdmission, ")
                .append("     adm.dateOfDischarge, ")
                .append("     rc.name, ")
                .append("     at.name, ")
                .append("     dept.name, ")
                .append("     r.name, ")
                .append("     pr.admittedAt, ")
                .append("     pr.dischargedAt ")
                .append(" ) ")
                .append(" from PatientRoom pr ")
                .append(" left join pr.patientEncounter adm ")
                .append(" left join adm.patient pat ")
                .append(" left join pat.person per ")
                .append(" left join pr.roomFacilityCharge rfc ")
                .append(" left join rfc.room r ")
                .append(" left join rfc.roomCategory rc ")
                .append(" left join rfc.department dept ")
                .append(" left join dept.institution inst ")
                .append(" left join adm.admissionType at ")
                .append(" where pr.retired = false ")
                .append(" and pr.discharged = true ");

        Map<String, Object> params = new HashMap<>();

        jpql.append(" and (pr.dischargedAt between :fd and :td) ");
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (selectedPatient != null && selectedPatient.getPatientEncounter() != null) {
            jpql.append(" and pat = :pt ");
            params.put("pt", selectedPatient.getPatientEncounter().getPatient());
        }

        if (ward != null) {
            jpql.append(" and dept = :wd ");
            params.put("wd", ward);
        }

        if (roomFacilityCharge != null) {
            jpql.append(" and rfc = :rfc ");
            params.put("rfc", roomFacilityCharge);
        }

        if (admissionType != null) {
            jpql.append(" and at = :adt ");
            params.put("adt", admissionType);
        }

        if (roomCategory != null) {
            jpql.append(" and rc = :rc ");
            params.put("rc", roomCategory);
        }

        if (site != null) {
            jpql.append(" and dept.institution = :st ");
            params.put("st", site);
        } else if (institution != null) {
            jpql.append(" and dept.institution = :ins ");
            params.put("ins", institution);
        }

        jpql.append(" order by pr.admittedAt desc ");
        detailDtos = (List<RoomOccupancyReportDto>) patientRoomFacade
                .findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        System.out.println("This is msg " + detailDtos.size());

    }

    private void processSummaryReport() {
        processDetailReport(); // Fetch all raw data first

        summaryDtos = new ArrayList<>();

        if (detailDtos == null || detailDtos.isEmpty()) {
            return;
        }

        if ("Summary By Room Category".equals(reportType)) {
            summaryType = "Category";
            Map<String, Double> map = detailDtos.stream()
                    .filter(dto -> dto.getRoomCategory() != null)
                    .collect(Collectors.groupingBy(RoomOccupancyReportDto::getRoomCategory, Collectors.summingDouble(RoomOccupancyReportDto::getOccupancyDays)));

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                RoomOccupancyReportDto dto = new RoomOccupancyReportDto();
                dto.setGroupName(entry.getKey());
                dto.setTotalOccupancyDays(entry.getValue());
                summaryDtos.add(dto);
            }
        } else if ("Summary By Admission Type".equals(reportType)) {
            summaryType = "AdmissionType";
            Map<String, Double> map = detailDtos.stream()
                    .filter(dto -> dto.getAdmissionType() != null)
                    .collect(Collectors.groupingBy(RoomOccupancyReportDto::getAdmissionType, Collectors.summingDouble(RoomOccupancyReportDto::getOccupancyDays)));

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                RoomOccupancyReportDto dto = new RoomOccupancyReportDto();
                dto.setGroupName(entry.getKey());
                dto.setTotalOccupancyDays(entry.getValue());
                summaryDtos.add(dto);
            }
        }

        // Sort
        summaryDtos.sort((d1, d2) -> d1.getGroupName().compareToIgnoreCase(d2.getGroupName()));
    }

    private static final SimpleDateFormat HEADER_DATE_FMT = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

    public void preProcessDetailPdf(Object document) throws DocumentException {
        Document pdf = (Document) document;
        pdf.setPageSize(PageSize.A4.rotate());
        pdf.setMargins(20f, 20f, 20f, 20f);
        pdf.open();
        addReportHeader(pdf, "Room Occupancy Report - Detail");
    }

    public void preProcessSummaryPdf(Object document) throws DocumentException {
        Document pdf = (Document) document;
        pdf.setPageSize(PageSize.A4);
        pdf.setMargins(30f, 30f, 30f, 30f);
        pdf.open();

        String title = "Summary By Admission Type".equals(reportType)
                ? "Room Occupancy Report - Summary By Admission Type"
                : "Room Occupancy Report - Summary By Room Category";
        addReportHeader(pdf, title);
    }

    private void addReportHeader(Document pdf, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(4f);
        pdf.add(titlePara);

        Paragraph metaPara = new Paragraph(buildFilterSummary(), metaFont);
        metaPara.setAlignment(Element.ALIGN_CENTER);
        metaPara.setSpacingAfter(12f);
        pdf.add(metaPara);
    }

    private String buildFilterSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Discharge Period: ")
                .append(fromDate != null ? HEADER_DATE_FMT.format(fromDate) : "N/A")
                .append(" - ")
                .append(toDate != null ? HEADER_DATE_FMT.format(toDate) : "N/A");

        if (selectedPatient != null) {
            sb.append("  |  Patient MRN: ").append(selectedPatient.getBhtNo());
        }
        if (ward != null) {
            sb.append("  |  Ward: ").append(ward.getName());
        }
        if (roomFacilityCharge != null) {
            sb.append("  |  Bed: ").append(roomFacilityCharge.getName());
        }
        if (roomCategory != null) {
            sb.append("  |  Room Category: ").append(roomCategory.getName());
        }
        if (admissionType != null) {
            sb.append("  |  Admission Type: ").append(admissionType.getName());
        }
        if (institution != null) {
            sb.append("  |  Institution: ").append(institution.getName());
        }
        if (site != null) {
            sb.append("  |  Site: ").append(site.getName());
        }
        sb.append("\nGenerated: ").append(HEADER_DATE_FMT.format(new Date()));
        return sb.toString();
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

    public Department getWard() {
        return ward;
    }

    public void setWard(Department ward) {
        this.ward = ward;
    }

    public RoomFacilityCharge getRoomFacilityCharge() {
        return roomFacilityCharge;
    }

    public void setRoomFacilityCharge(RoomFacilityCharge roomFacilityCharge) {
        this.roomFacilityCharge = roomFacilityCharge;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
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

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public List<RoomOccupancyReportDto> getDetailDtos() {
        return detailDtos;
    }

    public void setDetailDtos(List<RoomOccupancyReportDto> detailDtos) {
        this.detailDtos = detailDtos;
    }

    public List<RoomOccupancyReportDto> getSummaryDtos() {
        return summaryDtos;
    }

    public void setSummaryDtos(List<RoomOccupancyReportDto> summaryDtos) {
        this.summaryDtos = summaryDtos;
    }
}

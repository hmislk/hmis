package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.AdmissionCategoryWiseAdmissionDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import static com.divudi.core.data.inward.AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;
import static com.divudi.core.data.inward.AdmissionStatus.ANY_STATUS;
import static com.divudi.core.data.inward.AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
import static com.divudi.core.data.inward.AdmissionStatus.DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Controller for the Admission Category Wise Admission Report (Issue #22103).
 * Rooted at PatientEncounter so admissions without a confirmed final bill
 * still appear (with zero/blank financials) - the report must support
 * filtering by admission status, including admissions not yet discharged
 * or not yet finalized. The confirmed final bill (if any) is LEFT JOINed in
 * for its own scalar fields, and financial columns are otherwise derived
 * per-admission via 2 batched lookups regardless of result size - no
 * per-row queries, no entity/lazy-loading traversal.
 *
 * TODO(perf): for very large date ranges this still materializes the entire
 * result set in memory. Add pagination or a configurable row cap (see
 * SearchController.getMaxResult()) before exposing this on high-volume
 * deployments - confirm with the requester before adding any UI for it.
 */
@Named
@SessionScoped
public class AdmissionCategoryWiseAdmissionReportController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @Inject
    private SessionController sessionController;

    private Date fromDate;
    private Date toDate;
    private Date dischargeFromDate;
    private Date dischargeToDate;
    private Date invoiceApprovedFromDate;
    private Date invoiceApprovedToDate;
    private String dischargeType;
    private String patientCategory;
    private Institution institution;
    private Institution site;
    private Department department;
    private Staff consultant;
    private Institution sponsor;
    private PaymentMethod paymentMethod;
    private AdmissionType admissionType;
    private AdmissionStatus admissionStatus;

    private List<AdmissionCategoryWiseAdmissionDTO> admissionCategoryWiseAdmissionList;

    /**
     * Rooted at PatientEncounter with a LEFT JOIN to its finalBill, so
     * admissions that have not yet been discharged or settled still appear
     * (with a null bill and zero financials) rather than being silently
     * excluded - required for the "Admitted But Not Discharged" and
     * "Discharged But Final Bill Not Completed" admission-status filters to
     * return any rows at all.
     *
     * Suggested composite index if EXPLAIN shows a full/range scan on
     * patient_encounter: (date_of_admission, discharged, payment_finalized).
     */
    public void processAdmissionCategoryWiseAdmissionReportNew() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Admission From Date and Admission To Date are required.");
            admissionCategoryWiseAdmissionList = new ArrayList<>();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT new com.divudi.core.data.dto.AdmissionCategoryWiseAdmissionDTO(")
                .append("pe.id, ")
                .append("pe.bhtNo, ")
                .append("COALESCE(per.name, ''), ")
                .append("per.title, ")
                .append("pe.admissionType, ")
                .append("pe.paymentMethod, ")
                .append("pe.paymentFinalized")
                .append(") FROM PatientEncounter pe ")
                .append("LEFT JOIN pe.finalBill b ")
                .append("LEFT JOIN pe.patient pt ")
                .append("LEFT JOIN pt.person per ")
                .append("WHERE pe.id IS NOT NULL ")
                .append("AND (b IS NULL OR (b.billType = :billType AND b.retired = :ret AND b.cancelled = :can)) ")
                .append("AND pe.dateOfAdmission BETWEEN :fromDate AND :toDate ");

        params.put("billType", BillType.InwardFinalBill);
        params.put("ret", false);
        params.put("can", false);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        buildAdmissionCategoryFilterJpqlNew(jpql, params);

        jpql.append("ORDER BY pe.admissionType.name, pe.bhtNo ");

        try {
            admissionCategoryWiseAdmissionList = (List<AdmissionCategoryWiseAdmissionDTO>) billFacade.findLightsByJpql(
                    jpql.toString(), params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error loading admission category wise report: " + e.getMessage());
            admissionCategoryWiseAdmissionList = new ArrayList<>();
            return;
        }

        if (admissionCategoryWiseAdmissionList == null || admissionCategoryWiseAdmissionList.isEmpty()) {
            admissionCategoryWiseAdmissionList = new ArrayList<>();
            return;
        }

        enrichAdmissionCategoryWiseFinancialsFast(admissionCategoryWiseAdmissionList);
    }

    private void buildAdmissionCategoryFilterJpqlNew(StringBuilder jpql, Map<String, Object> params) {
        if (dischargeFromDate != null && dischargeToDate != null) {
            jpql.append("AND pe.dateOfDischarge BETWEEN :dfd AND :dtd ");
            params.put("dfd", dischargeFromDate);
            params.put("dtd", dischargeToDate);
        }

        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null) {
            // b is the encounter's confirmed final bill (LEFT JOINed above);
            // its own createdAt is the invoice-approved timestamp. Encounters
            // with no final bill yet (b IS NULL) are naturally excluded by
            // this comparison, which is correct - there is no invoice to
            // have been approved.
            jpql.append("AND b.createdAt BETWEEN :iafd AND :iatd ");
            params.put("iafd", invoiceApprovedFromDate);
            params.put("iatd", invoiceApprovedToDate);
        }

        if (institution != null) {
            jpql.append("AND pe.institution = :inst ");
            params.put("inst", institution);
        }
        if (site != null) {
            jpql.append("AND pe.department.site = :site ");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append("AND pe.department = :dept ");
            params.put("dept", department);
        }
        if (consultant != null) {
            jpql.append("AND pe.referringConsultant = :cons ");
            params.put("cons", consultant);
        }
        if (sponsor != null) {
            jpql.append("AND pe.creditCompany = :sponsor ");
            params.put("sponsor", sponsor);
        }
        if (admissionType != null) {
            jpql.append("AND pe.admissionType = :at ");
            params.put("at", admissionType);
        }
        if (paymentMethod != null) {
            jpql.append("AND pe.paymentMethod = :pm ");
            params.put("pm", paymentMethod);
        }
        if (admissionStatus != null && admissionStatus != ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = FALSE ");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = FALSE ");
                    params.put("dis", true);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append("AND pe.discharged = :dis AND pe.paymentFinalized = TRUE ");
                    params.put("dis", true);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Enriches each row using exactly 2 batched DB round trips, regardless of
     * result size: deposit/advance totals and the confirmed final bill's
     * charge summary.
     * billBalance = invoiceAmount - (sponsorAmount + patientAmount)
     * patientBalance = patientAmount - advance
     */
    private void enrichAdmissionCategoryWiseFinancialsFast(List<AdmissionCategoryWiseAdmissionDTO> rows) {
        List<Long> encounterIds = rows.stream()
                .filter(dto -> dto != null && dto.getAdmissionId() != null)
                .map(AdmissionCategoryWiseAdmissionDTO::getAdmissionId)
                .distinct()
                .collect(Collectors.toList());

        if (encounterIds.isEmpty()) {
            return;
        }

        Map<Long, FinalBillChargeSummary> chargeSummaryByEncounterId = batchFetchFinalBillChargeSummaryByEncounterIds(encounterIds);
        Map<Long, Double> depositByEncounterId = batchFetchDepositTotalsByEncounterIds(encounterIds);

        for (AdmissionCategoryWiseAdmissionDTO dto : rows) {
            if (dto == null || dto.getAdmissionId() == null) {
                continue;
            }

            Long id = dto.getAdmissionId();
            FinalBillChargeSummary chargeSummary = chargeSummaryByEncounterId.get(id);
            double advance = depositByEncounterId.getOrDefault(id, 0.0);

            double invoiceAmount = chargeSummary != null ? chargeSummary.netTotal : 0.0;
            double discount = chargeSummary != null ? chargeSummary.discount : 0.0;
            double sponsorAmount = chargeSummary != null ? chargeSummary.sponsorAmount : 0.0;
            double patientAmount = chargeSummary != null ? chargeSummary.patientAmount : 0.0;
            double professionalFees = chargeSummary != null ? chargeSummary.professionalFee : 0.0;
            double hospitalAmount = chargeSummary != null ? chargeSummary.hospitalFee : 0.0;

            dto.setAdvance(advance);
            dto.setProfessionalFees(professionalFees);
            dto.setHospitalAmount(hospitalAmount);
            dto.setSponsorAmount(sponsorAmount);
            dto.setPatientAmount(patientAmount);
            dto.setDiscount(discount);
            dto.setInvoiceAmount(invoiceAmount);
            dto.setBillBalance(Math.max(0.0, invoiceAmount - (sponsorAmount + patientAmount)));
            dto.setPatientBalance(Math.max(0.0, patientAmount - advance));
        }
    }

    /**
     * Holds one confirmed final Bill's scalar totals plus its BillItems'
     * professional-vs-hospital charge split, keyed by encounter id.
     */
    private static final class FinalBillChargeSummary {

        double netTotal;
        double discount;
        double sponsorAmount;
        double patientAmount;
        double professionalFee;
        double hospitalFee;
    }

    /**
     * Confirmed final Bill scalars (netTotal/discount/settled amounts) plus its
     * BillItems' professional-vs-hospital split, in one round trip. Rooted at
     * Bill with a LEFT JOIN to billItems (rather than driving FROM BillItem) so
     * a bill row survives even if it somehow has zero line items. Grouped by
     * chargeType and split in Java rather than SUM(CASE WHEN ...): a CASE-based
     * version of this query previously failed silently because
     * AbstractFacade.findAggregates() swallows any query exception and returns
     * null with no trace in the logs, so it's built on findObjectsArrayByJpql()
     * instead, which propagates real failures.
     *
     * Identifies "this bill is the encounter's confirmed final bill" via
     * {@code b.id = b.patientEncounter.finalBill.id} rather than the
     * {@code Bill.confirmedFinalBill} flag - that denormalized flag is not
     * reliably populated (found 0 rows with it set true across the entire bill
     * table in local test data, while patientEncounter.finalBill correctly
     * points back at every genuine final bill), so relying on it as a hard
     * filter here silently zeroed out every row's financial columns.
     */
    private Map<Long, FinalBillChargeSummary> batchFetchFinalBillChargeSummaryByEncounterIds(List<Long> encounterIds) {
        if (encounterIds == null || encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT b.patientEncounter.id, b.netTotal, b.discount, b.settledAmountBySponsor, "
                + "b.settledAmountByPatient, bi.inwardChargeType, SUM(bi.netValue) "
                + "FROM Bill b "
                + "LEFT JOIN b.billItems bi "
                + "WHERE b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billType = :bt "
                + "AND b.id = b.patientEncounter.finalBill.id "
                + "AND (bi.id IS NULL OR bi.retired = false) "
                + "AND b.patientEncounter.id IN :ids "
                + "GROUP BY b.patientEncounter.id, b.netTotal, b.discount, b.settledAmountBySponsor, "
                + "b.settledAmountByPatient, bi.inwardChargeType";

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.InwardFinalBill);
        params.put("ids", encounterIds);

        List<Object[]> rows = billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP);
        Map<Long, FinalBillChargeSummary> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 7 || row[0] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            FinalBillChargeSummary summary = result.computeIfAbsent(id, k -> new FinalBillChargeSummary());
            summary.netTotal = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            summary.discount = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            summary.sponsorAmount = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            summary.patientAmount = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

            InwardChargeType chargeType = (InwardChargeType) row[5];
            double net = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;
            if (chargeType == InwardChargeType.ProfessionalCharge || chargeType == InwardChargeType.DoctorAndNurses) {
                summary.professionalFee += net;
            } else if (chargeType != null) {
                summary.hospitalFee += net;
            }
        }
        return result;
    }

    private Map<Long, Double> batchFetchDepositTotalsByEncounterIds(List<Long> encounterIds) {
        if (encounterIds == null || encounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT b.patientEncounter.id, SUM(ABS(p.paidValue)) "
                + "FROM Payment p "
                + "JOIN p.bill b "
                + "WHERE p.retired = false "
                + "AND b.retired = false "
                + "AND b.cancelled = false "
                + "AND b.billTypeAtomic = :bta "
                + "AND b.patientEncounter.id IN :ids "
                + "GROUP BY b.patientEncounter.id";

        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_DEPOSIT);
        params.put("ids", encounterIds);

        return mapEncounterDoubleAggregate(billFacade.findObjectsArrayByJpql(jpql, params, TemporalType.TIMESTAMP));
    }

    private Map<Long, Double> mapEncounterDoubleAggregate(List<Object[]> rows) {
        Map<Long, Double> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Long id = ((Number) row[0]).longValue();
            Double value = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            result.put(id, Math.max(0.0, value));
        }
        return result;
    }

    public StreamedContent getAdmissionCategoryWiseAdmissionExcel() {
        if (admissionCategoryWiseAdmissionList == null || admissionCategoryWiseAdmissionList.isEmpty()) {
            JsfUtil.addErrorMessage("No data available to export.");
            return null;
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Admission Category Wise");

            int rowNum = 0;
            SimpleDateFormat sdt = new SimpleDateFormat("dd MMM yyyy HH:mm");

            Row filterRow1 = sheet.createRow(rowNum++);
            filterRow1.createCell(0).setCellValue("Institution:");
            filterRow1.createCell(1).setCellValue(institution != null ? institution.getName() : "All");

            Row filterRow2 = sheet.createRow(rowNum++);
            filterRow2.createCell(0).setCellValue("Site:");
            filterRow2.createCell(1).setCellValue(site != null ? site.getName() : "All");

            Row filterRow3 = sheet.createRow(rowNum++);
            filterRow3.createCell(0).setCellValue("Department:");
            filterRow3.createCell(1).setCellValue(department != null ? department.getName() : "All");

            Row filterRow4 = sheet.createRow(rowNum++);
            filterRow4.createCell(0).setCellValue("Admission Category:");
            filterRow4.createCell(1).setCellValue(admissionType != null ? admissionType.getName() : "All");

            Row filterRow5 = sheet.createRow(rowNum++);
            filterRow5.createCell(0).setCellValue("From Date:");
            filterRow5.createCell(1).setCellValue(fromDate != null ? sdt.format(fromDate) : "-");

            Row filterRow6 = sheet.createRow(rowNum++);
            filterRow6.createCell(0).setCellValue("To Date:");
            filterRow6.createCell(1).setCellValue(toDate != null ? sdt.format(toDate) : "-");

            rowNum++;

            String[] headers = {
                "No", "BHT", "Patient Name", "Admission Category", "Advance",
                "Professional Fees", "Hospital Amount", "Sponsor Amount", "Patient Amount",
                "Discount", "Invoice Amount", "Bill Balance", "Patient Balance"
            };

            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            CreationHelper helper = wb.getCreationHelper();
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

            int idx = 1;
            for (AdmissionCategoryWiseAdmissionDTO dto : admissionCategoryWiseAdmissionList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(dto.getBhtNo() != null ? dto.getBhtNo() : "");
                row.createCell(2).setCellValue(dto.getPatientName() != null ? dto.getPatientName() : "");
                row.createCell(3).setCellValue(dto.getCategoryName() != null ? dto.getCategoryName() : "");

                for (int col = 4; col <= 12; col++) {
                    Cell moneyCell = row.createCell(col);
                    moneyCell.setCellStyle(moneyStyle);
                }
                row.getCell(4).setCellValue(dto.getAdvance());
                row.getCell(5).setCellValue(dto.getProfessionalFees());
                row.getCell(6).setCellValue(dto.getHospitalAmount());
                row.getCell(7).setCellValue(dto.getSponsorAmount());
                row.getCell(8).setCellValue(dto.getPatientAmount());
                row.getCell(9).setCellValue(dto.getDiscount());
                row.getCell(10).setCellValue(dto.getInvoiceAmount());
                row.getCell(11).setCellValue(dto.getBillBalance());
                row.getCell(12).setCellValue(dto.getPatientBalance());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            byte[] bytes = out.toByteArray();
            return DefaultStreamedContent.builder()
                    .name("Admission_Category_Wise_Admission.xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> new ByteArrayInputStream(bytes))
                    .build();

        } catch (IOException e) {
            java.util.logging.Logger.getLogger(AdmissionCategoryWiseAdmissionReportController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Excel generation failed", e);
            JsfUtil.addErrorMessage("Failed to generate Excel: " + e.getMessage());
            return null;
        }
    }

    public void downloadAdmissionCategoryWiseAdmissionPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        String dates = CommonFunctions.dateRangeForFileName(
                fromDate, toDate,
                sessionController.getApplicationPreference().getLongDateFormat());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        SimpleDateFormat sdt = new SimpleDateFormat("dd MMM yyyy HH:mm");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(com.lowagie.text.PageSize.A4.rotate(), 10f, 10f, 10f, 10f);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            String institutionName = sessionController.getInstitution() != null
                    ? sessionController.getInstitution().getName()
                    : "No Logged Institution";

            document.add(new Paragraph(institutionName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Admission Category Wise Admission Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Date: " + sdf.format(new Date()), FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            if (admissionCategoryWiseAdmissionList == null || admissionCategoryWiseAdmissionList.isEmpty()) {
                document.add(new Paragraph("No admissions for the selected criteria.",
                        FontFactory.getFont(FontFactory.HELVETICA, 12)));
                document.close();
                context.responseComplete();
                return;
            }

            PdfPTable infoTable = buildAdmissionCategoryWiseInfoTable(sdt);
            if (infoTable != null) {
                document.add(infoTable);
            }

            PdfPTable table = new PdfPTable(13);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            float[] columnWidths = {0.8f, 1.5f, 2.5f, 1.8f, 1.2f, 1.4f, 1.4f, 1.3f, 1.3f, 1.1f, 1.3f, 1.3f, 1.3f};
            table.setWidths(columnWidths);

            addAdmissionCategoryWiseHeaderRow(table);

            int idx = 1;
            for (AdmissionCategoryWiseAdmissionDTO row : admissionCategoryWiseAdmissionList) {
                addAdmissionCategoryWiseRow(table, row, idx++);
            }

            document.add(table);
            document.close();

            byte[] bytes = baos.toByteArray();
            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=Admission_Category_Wise_Admission_" + dates + ".pdf");
            response.setContentLength(bytes.length);

            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }

            context.responseComplete();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error generating PDF: " + e.getMessage());
        }
    }

    private PdfPTable buildAdmissionCategoryWiseInfoTable(SimpleDateFormat sdt) throws DocumentException {
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(60);
        info.setSpacingBefore(5);
        info.setWidths(new float[]{1f, 2f});

        addInfoCell(info, "Institution:", institution != null ? institution.getName() : "All");
        addInfoCell(info, "Site:", site != null ? site.getName() : "All");
        addInfoCell(info, "Department:", department != null ? department.getName() : "All");
        addInfoCell(info, "Admission Category:", admissionType != null ? admissionType.getName() : "All");
        addInfoCell(info, "From Date:", fromDate != null ? sdt.format(fromDate) : "-");
        addInfoCell(info, "To Date:", toDate != null ? sdt.format(toDate) : "-");
        addInfoCell(info, "Generated:", sdt.format(new Date()));
        return info;
    }

    private void addAdmissionCategoryWiseHeaderRow(PdfPTable table) {
        java.awt.Color headerBg = new java.awt.Color(33, 37, 41);
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.WHITE);

        String[] headers = {
            "#", "BHT", "Patient Name", "Category", "Advance", "Prof. Fees", "Hospital",
            "Sponsor", "Patient", "Discount", "Invoice", "Bill Bal.", "Patient Bal."
        };

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(3);
            table.addCell(cell);
        }
    }

    private void addAdmissionCategoryWiseRow(PdfPTable table, AdmissionCategoryWiseAdmissionDTO row, int idx) {
        com.lowagie.text.Font normal = FontFactory.getFont(FontFactory.HELVETICA, 7);

        table.addCell(new Phrase(String.valueOf(idx), normal));
        table.addCell(new Phrase(nullSafe(row.getBhtNo()), normal));
        table.addCell(new Phrase(nullSafe(row.getPatientName()), normal));
        table.addCell(new Phrase(nullSafe(row.getCategoryName()), normal));

        PdfPCell c;
        c = new PdfPCell(new Phrase(formatAmount(row.getAdvance()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getProfessionalFees()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getHospitalAmount()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getSponsorAmount()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getPatientAmount()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getDiscount()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getInvoiceAmount()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getBillBalance()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
        c = new PdfPCell(new Phrase(formatAmount(row.getPatientBalance()), normal));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        com.lowagie.text.Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        com.lowagie.text.Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPCell c1 = new PdfPCell(new Phrase(label, bold));
        c1.setBorder(0);
        c1.setPadding(2);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "", normal));
        c2.setBorder(0);
        c2.setPadding(2);
        table.addCell(c2);
    }

    private String formatAmount(Double v) {
        return String.format("%,.2f", v != null ? v : 0.0);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getDischargeFromDate() {
        return dischargeFromDate;
    }

    public void setDischargeFromDate(Date dischargeFromDate) {
        this.dischargeFromDate = dischargeFromDate;
    }

    public Date getDischargeToDate() {
        return dischargeToDate;
    }

    public void setDischargeToDate(Date dischargeToDate) {
        this.dischargeToDate = dischargeToDate;
    }

    public Date getInvoiceApprovedFromDate() {
        return invoiceApprovedFromDate;
    }

    public void setInvoiceApprovedFromDate(Date invoiceApprovedFromDate) {
        this.invoiceApprovedFromDate = invoiceApprovedFromDate;
    }

    public Date getInvoiceApprovedToDate() {
        return invoiceApprovedToDate;
    }

    public void setInvoiceApprovedToDate(Date invoiceApprovedToDate) {
        this.invoiceApprovedToDate = invoiceApprovedToDate;
    }

    public String getDischargeType() {
        return dischargeType;
    }

    public void setDischargeType(String dischargeType) {
        this.dischargeType = dischargeType;
    }

    public String getPatientCategory() {
        return patientCategory;
    }

    public void setPatientCategory(String patientCategory) {
        this.patientCategory = patientCategory;
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

    public Staff getConsultant() {
        return consultant;
    }

    public void setConsultant(Staff consultant) {
        this.consultant = consultant;
    }

    public Institution getSponsor() {
        return sponsor;
    }

    public void setSponsor(Institution sponsor) {
        this.sponsor = sponsor;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public AdmissionStatus getAdmissionStatus() {
        return admissionStatus;
    }

    public void setAdmissionStatus(AdmissionStatus admissionStatus) {
        this.admissionStatus = admissionStatus;
    }

    public List<AdmissionCategoryWiseAdmissionDTO> getAdmissionCategoryWiseAdmissionList() {
        return admissionCategoryWiseAdmissionList;
    }

    public void setAdmissionCategoryWiseAdmissionList(List<AdmissionCategoryWiseAdmissionDTO> admissionCategoryWiseAdmissionList) {
        this.admissionCategoryWiseAdmissionList = admissionCategoryWiseAdmissionList;
    }

}

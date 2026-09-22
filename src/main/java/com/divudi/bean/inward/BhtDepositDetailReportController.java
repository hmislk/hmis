package com.divudi.bean.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.BhtPaymentDetailDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.JsfUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UserSettingsController;

/**
 * Controller for BHT Deposit Detail Report. One row per individual deposit
 * payment. CC settlements excluded. Also covers Inpatient Payment and Post
 * Discharge (post-final-bill) payment rows via the {@code reportType} filter,
 * not just deposits.
 */
@Named
@SessionScoped
public class BhtDepositDetailReportController implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @Inject
    private UserSettingsController userSettingsController;
    @Inject
    private SessionController sessionController;

    private Date fromDate = startOfCurrentMonth();
    private Date toDate = endOfCurrentMonth();
    private String dateBasis = "dischargeDate";
    private String reportType = "ALL";
    private AdmissionStatus admissionStatus = AdmissionStatus.ANY_STATUS;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private Institution site;
    private Department department;

    private List<BhtPaymentDetailDTO> reportRows;
    private double grandTotal;
    private Map<PaymentMethod, Double> totalByMethod = new LinkedHashMap<>();
    private List<PaymentMethod> usedPaymentMethods = new ArrayList<>();

    // Snapshot of the filter values that actually produced `reportRows`,
    // captured by generateReport(). The PDF export's filter-summary block
    // reads these instead of the live filter fields, so it stays consistent
    // even if the form's filter inputs are edited (and re-submitted with the
    // PDF button, which does a full non-ajax form post) without the user
    // clicking "Generate" again.
    private Date appliedFromDate;
    private Date appliedToDate;
    private String appliedDateBasis;
    private String appliedReportType;
    private AdmissionStatus appliedAdmissionStatus;
    private AdmissionType appliedAdmissionType;
    private PaymentMethod appliedPaymentMethod;
    private Institution appliedInstitution;
    private Institution appliedSite;
    private Department appliedDepartment;

    public void generateReport() {
        reportRows = new ArrayList<>();
        grandTotal = 0;
        totalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();

        appliedFromDate = fromDate;
        appliedToDate = toDate;
        appliedDateBasis = dateBasis;
        appliedReportType = reportType;
        appliedAdmissionStatus = admissionStatus;
        appliedAdmissionType = admissionType;
        appliedPaymentMethod = paymentMethod;
        appliedInstitution = institution;
        appliedSite = site;
        appliedDepartment = department;

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        for (PatientEncounter enc : encounters) {
            String patientName = enc.getPatient() != null && enc.getPatient().getPerson() != null
                    ? enc.getPatient().getPerson().getNameWithTitle() : "";

            List<Payment> deposits = fetchDepositPayments(enc);
            for (Payment p : deposits) {
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillId(p.getBill() != null ? p.getBill().getId() : null);
                row.setBillNo(p.getBill() != null ? p.getBill().getDeptId() : "");
                row.setCreatedAt(p.getCreatedAt());
                row.setPaymentMethod(p.getPaymentMethod());
                row.setAmount(Math.abs(p.getPaidValue()));
                row.setReferenceNo(p.getReferenceNo());
                reportRows.add(row);

                double amt = Math.abs(p.getPaidValue());
                grandTotal += amt;
                if (p.getPaymentMethod() != null) {
                    totalByMethod.merge(p.getPaymentMethod(), amt, Double::sum);
                }
            }
        }

        usedPaymentMethods = new ArrayList<>(totalByMethod.keySet());
        reportRows.sort(Comparator.comparing(BhtPaymentDetailDTO::getBillId,
                Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private List<PatientEncounter> fetchEncounters() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select distinct c from PatientEncounter c where c.retired = false");

        if (fromDate != null && toDate != null) {
            if ("admissionDate".equals(dateBasis)) {
                jpql.append(" and c.dateOfAdmission between :fromDate and :toDate");
            } else if ("paymentDate".equals(dateBasis)) {
                jpql.append(" and exists (select 1 from Payment p where p.retired = false"
                        + " and p.bill.retired = false and p.bill.cancelled = false"
                        + " and p.bill.billTypeAtomic in :btas and p.bill.patientEncounter = c"
                        + " and p.createdAt between :fromDate and :toDate");
                if (paymentMethod != null) {
                    jpql.append(" and p.paymentMethod = :pm");
                    params.put("pm", paymentMethod);
                }
                jpql.append(")");
                params.put("btas", reportTypeBillTypeAtomics());
            } else {
                jpql.append(" and c.dateOfDischarge between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }

        if (admissionStatus != null && admissionStatus != AdmissionStatus.ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append(" and c.discharged = :dis");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", false);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", true);
                    break;
                default:
                    break;
            }
        }

        if (admissionType != null) {
            jpql.append(" and c.admissionType = :admType");
            params.put("admType", admissionType);
        }
        if (institution != null) {
            jpql.append(" and c.institution = :ins");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and c.department.site = :site");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and c.department = :dept");
            params.put("dept", department);
        }
        jpql.append(" order by c.bhtNo");
        return patientEncounterFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    private List<BillTypeAtomic> reportTypeBillTypeAtomics() {
        if ("DEPOSIT".equals(reportType)) {
            return Collections.singletonList(BillTypeAtomic.INWARD_DEPOSIT);
        } else if ("PAYMENT".equals(reportType)) {
            return Collections.singletonList(BillTypeAtomic.INWARD_PAYMENT);
        } else if ("POST_FINAL".equals(reportType)) {
            return Collections.singletonList(BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT);
        }
        return Arrays.asList(BillTypeAtomic.INWARD_DEPOSIT, BillTypeAtomic.INWARD_PAYMENT,
                BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT);
    }

    private List<Payment> fetchDepositPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.cancelled = false"
                + " and p.bill.billTypeAtomic in :btas"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("btas", reportTypeBillTypeAtomics());
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        if ("paymentDate".equals(dateBasis) && fromDate != null && toDate != null) {
            jpql.append(" and p.createdAt between :fromDate and :toDate");
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    public double getTotalForMethod(PaymentMethod pm) {
        return totalByMethod.getOrDefault(pm, 0.0);
    }

    /**
     * Amount to show in this row's column for the given payment method - the
     * row's own amount if it was paid by that method, null otherwise (so the
     * per-method columns show one value per row, not the row repeated).
     */
    public Double getRowAmountForMethod(BhtPaymentDetailDTO row, PaymentMethod pm) {
        if (row == null || pm == null || !pm.equals(row.getPaymentMethod())) {
            return null;
        }
        return row.getAmount();
    }

    /**
     * postProcessor for the Excel export (p:dataExporter). p:dataExporter only
     * serializes the exported h:outputText values as text (via the column's
     * f:convertNumber), so monetary columns land as plain strings. This
     * converts those cells back to real numeric cells formatted as
     * "#,##0.00" and appends a totals row (per used payment method plus the
     * grand total), matching the on-screen footer which p:dataExporter does
     * not otherwise export.
     */
    public void postProcessXLSBhtDepositDetail(Object document) {
        if (!(document instanceof Workbook)) {
            return;
        }
        Workbook workbook = (Workbook) document;
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            return;
        }

        List<String> columnKinds = exportedColumnKinds();

        DataFormat dataFormat = workbook.createDataFormat();
        short moneyFormat = dataFormat.getFormat("#,##0.00");

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(moneyFormat);

        int lastDataRow = sheet.getLastRowNum();
        for (int r = 1; r <= lastDataRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (int c = 0; c < columnKinds.size(); c++) {
                if (isMoneyColumn(columnKinds.get(c))) {
                    applyMoneyFormat(row.getCell(c), numberStyle);
                }
            }
        }

        org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle totalLabelStyle = workbook.createCellStyle();
        totalLabelStyle.setFont(boldFont);
        CellStyle totalValueStyle = workbook.createCellStyle();
        totalValueStyle.setFont(boldFont);
        totalValueStyle.setDataFormat(moneyFormat);

        Row totalRow = sheet.createRow(lastDataRow + 1);
        boolean labelWritten = false;
        for (int c = 0; c < columnKinds.size(); c++) {
            String kind = columnKinds.get(c);
            if ("AMOUNT".equals(kind)) {
                Cell cell = totalRow.createCell(c);
                cell.setCellValue(grandTotal);
                cell.setCellStyle(totalValueStyle);
            } else if (kind.startsWith("PM")) {
                PaymentMethod pm = usedPaymentMethods.get(Integer.parseInt(kind.substring(2)));
                Cell cell = totalRow.createCell(c);
                cell.setCellValue(getTotalForMethod(pm));
                cell.setCellStyle(totalValueStyle);
            } else if (!labelWritten) {
                Cell cell = totalRow.createCell(c);
                cell.setCellValue("Total");
                cell.setCellStyle(totalLabelStyle);
                labelWritten = true;
            }
        }
    }

    /**
     * The kind of each exported column, in the same left-to-right order as
     * the rendered columns on {@code inward_report_bht_deposit_detail.xhtml}
     * (only columns currently visible per {@code userSettingsController} are
     * included, matching what p:dataExporter actually exports).
     */
    private List<String> exportedColumnKinds() {
        List<String> kinds = new ArrayList<>();
        if (userSettingsController.isInwardBhtDepositDetailBillNoVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailBhtNoVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailPatientNameVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailAdmissionTypeVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailAdmittedVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailDischargedVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailDateTimeVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailPaymentMethodVisible()) {
            kinds.add("TEXT");
        }
        if (userSettingsController.isInwardBhtDepositDetailAmountVisible()) {
            kinds.add("AMOUNT");
        }
        for (int i = 0; i < usedPaymentMethods.size(); i++) {
            kinds.add("PM" + i);
        }
        if (userSettingsController.isInwardBhtDepositDetailReferenceNoVisible()) {
            kinds.add("TEXT");
        }
        return kinds;
    }

    private boolean isMoneyColumn(String kind) {
        return "AMOUNT".equals(kind) || kind.startsWith("PM");
    }

    private void applyMoneyFormat(Cell cell, CellStyle numberStyle) {
        if (cell == null) {
            return;
        }
        if (cell.getCellType() == CellType.STRING) {
            String text = cell.getStringCellValue();
            if (text == null || text.trim().isEmpty()) {
                return;
            }
            try {
                double value = Double.parseDouble(text.replace(",", "").trim());
                cell.setCellValue(value);
            } catch (NumberFormatException e) {
                return;
            }
        }
        cell.setCellStyle(numberStyle);
    }

    /**
     * Builds a professionally formatted, landscape PDF for the BHT Deposit
     * Detail Report and writes it directly to the response, replacing
     * PrimeFaces' generic dataExporter (which produced a cramped, misaligned
     * portrait table, especially once the dynamic per-payment-method columns
     * are included). Mirrors the PDF layout used elsewhere in the Inward
     * reports (e.g. {@code CreditCompanyDebtorGroupedReportController}).
     * Respects the same column-visibility toggles as the Excel export, see
     * {@link #exportedColumnKinds()}. The filter-summary block reads the
     * appliedXxx snapshot captured by generateReport() rather than the live
     * filter fields, so it can't drift out of sync with the exported rows if
     * the form's inputs are edited after "Generate" but before "PDF" is
     * clicked (Issue #23480).
     */
    public void downloadPdf() {
        if (reportRows == null || reportRows.isEmpty()) {
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response =
                (HttpServletResponse) facesContext.getExternalContext().getResponse();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat dtf = new SimpleDateFormat("dd MMM yyyy HH:mm");
        SimpleDateFormat rowDtf = new SimpleDateFormat("dd-MM-yyyy HH:mm");

        List<String[]> columns = pdfColumnDefs();
        boolean responseStarted = false;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 18, 18, 24, 18);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

            Color headerBg = new Color(89, 89, 89);
            Color totalBg = new Color(0, 77, 64);

            // --- Title / subtitle / printed-by ---
            String institutionName = sessionController.getInstitution() != null
                    ? sessionController.getInstitution().getName() : "Institution";

            Paragraph p1 = new Paragraph(institutionName, titleFont);
            p1.setAlignment(Element.ALIGN_CENTER);
            document.add(p1);

            Paragraph p2 = new Paragraph("BHT Deposit Detail Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            p2.setAlignment(Element.ALIGN_CENTER);
            document.add(p2);

            Paragraph p3 = new Paragraph(
                    "Printed By: "
                    + (sessionController.getLoggedUser() != null
                       && sessionController.getLoggedUser().getWebUserPerson() != null
                            ? sessionController.getLoggedUser().getWebUserPerson().getName() : "-")
                    + "   at " + dtf.format(new Date()),
                    subtitleFont);
            p3.setAlignment(Element.ALIGN_CENTER);
            document.add(p3);

            document.add(new Paragraph(" "));

            // --- Filter details (from the appliedXxx snapshot) ---
            String dateBasisLabel;
            switch (appliedDateBasis) {
                case "admissionDate": dateBasisLabel = "Admission Date"; break;
                case "paymentDate":   dateBasisLabel = "Payment Date"; break;
                default:              dateBasisLabel = "Discharge Date"; break;
            }

            PdfPTable filterTable = new PdfPTable(2);
            filterTable.setWidthPercentage(65);
            filterTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            filterTable.setSpacingBefore(4);
            filterTable.setSpacingAfter(10);
            filterTable.setWidths(new float[]{1f, 2f});

            addPdfInfoRow(filterTable, "Date Basis", dateBasisLabel, labelFont, valueFont);
            addPdfInfoRow(filterTable, "From", appliedFromDate != null ? dtf.format(appliedFromDate) : "-", labelFont, valueFont);
            addPdfInfoRow(filterTable, "To", appliedToDate != null ? dtf.format(appliedToDate) : "-", labelFont, valueFont);
            addPdfInfoRow(filterTable, "Report Type", appliedReportType != null ? appliedReportType : "ALL", labelFont, valueFont);
            addPdfInfoRow(filterTable, "Admission Status",
                    appliedAdmissionStatus != null ? appliedAdmissionStatus.getLabel() : "-", labelFont, valueFont);
            addPdfInfoRow(filterTable, "Admission Type",
                    appliedAdmissionType != null ? appliedAdmissionType.getName() : "All", labelFont, valueFont);
            addPdfInfoRow(filterTable, "Payment Method",
                    appliedPaymentMethod != null ? appliedPaymentMethod.getLabel() : "All", labelFont, valueFont);
            addPdfInfoRow(filterTable, "Institution",
                    appliedInstitution != null ? appliedInstitution.getName() : "All", labelFont, valueFont);
            addPdfInfoRow(filterTable, "Site",
                    appliedSite != null ? appliedSite.getName() : "All", labelFont, valueFont);
            addPdfInfoRow(filterTable, "Department",
                    appliedDepartment != null ? appliedDepartment.getName() : "All", labelFont, valueFont);

            document.add(filterTable);

            // --- Main data table ---
            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(6);
            float[] widths = new float[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                widths[i] = pdfColumnWidth(columns.get(i)[1]);
            }
            table.setWidths(widths);

            for (String[] col : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(col[0], headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(headerBg);
                cell.setPadding(4f);
                table.addCell(cell);
            }

            // --- Data rows ---
            for (BhtPaymentDetailDTO row : reportRows) {
                for (String[] col : columns) {
                    String kind = col[1];
                    if (isMoneyColumn(kind)) {
                        String text = "";
                        if ("AMOUNT".equals(kind)) {
                            text = String.format("%,.2f", row.getAmount());
                        } else if (kind.startsWith("PM")) {
                            PaymentMethod pm = usedPaymentMethods.get(Integer.parseInt(kind.substring(2)));
                            Double amt = getRowAmountForMethod(row, pm);
                            text = amt != null ? String.format("%,.2f", amt) : "";
                        }
                        addPdfCell(table, text, cellFont, Element.ALIGN_RIGHT, null);
                    } else {
                        addPdfCell(table, pdfCellValue(row, kind, sdf, rowDtf), cellFont, Element.ALIGN_LEFT, null);
                    }
                }
            }

            // --- Totals row ---
            int nonMoneyCount = 0;
            for (String[] col : columns) {
                if (!isMoneyColumn(col[1])) {
                    nonMoneyCount++;
                }
            }
            if (nonMoneyCount > 0) {
                PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", totalFont));
                totalLabelCell.setColspan(nonMoneyCount);
                totalLabelCell.setBackgroundColor(totalBg);
                totalLabelCell.setPadding(4f);
                table.addCell(totalLabelCell);
            }
            for (String[] col : columns) {
                String kind = col[1];
                if ("AMOUNT".equals(kind)) {
                    addPdfCell(table, String.format("%,.2f", grandTotal), totalFont, Element.ALIGN_RIGHT, totalBg);
                } else if (kind.startsWith("PM")) {
                    PaymentMethod pm = usedPaymentMethods.get(Integer.parseInt(kind.substring(2)));
                    addPdfCell(table, String.format("%,.2f", getTotalForMethod(pm)), totalFont, Element.ALIGN_RIGHT, totalBg);
                }
            }

            document.add(table);
            document.close();

            byte[] fileBytes = baos.toByteArray();

            // --- Write response atomically ---
            String filename = "BHT_Deposit_Detail_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";
            responseStarted = true;
            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(fileBytes.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(fileBytes);
                out.flush();
            }
            facesContext.responseComplete();

        } catch (Exception e) {
            Logger.getLogger(BhtDepositDetailReportController.class.getName())
                    .log(Level.SEVERE, "Failed to generate BHT Deposit Detail Report PDF", e);
            // response.reset()/getOutputStream() had not run yet, so the JSF
            // response is still intact and a FacesMessage will render normally.
            // Once we start writing to the response the JSF view is bypassed,
            // so a message added after that point would never be shown.
            if (!responseStarted) {
                JsfUtil.addErrorMessage("Failed to generate PDF. Please try again.");
            }
        }
    }

    /**
     * The header/kind of each PDF column, in the same left-to-right order as
     * {@link #exportedColumnKinds()} / the rendered table on
     * {@code inward_report_bht_deposit_detail.xhtml} (only columns currently
     * visible per {@code userSettingsController} are included).
     */
    private List<String[]> pdfColumnDefs() {
        List<String[]> cols = new ArrayList<>();
        if (userSettingsController.isInwardBhtDepositDetailBillNoVisible()) {
            cols.add(new String[]{"Bill No", "BILL_NO"});
        }
        if (userSettingsController.isInwardBhtDepositDetailBhtNoVisible()) {
            cols.add(new String[]{"BHT No", "BHT_NO"});
        }
        if (userSettingsController.isInwardBhtDepositDetailPatientNameVisible()) {
            cols.add(new String[]{"Patient Name", "PATIENT_NAME"});
        }
        if (userSettingsController.isInwardBhtDepositDetailAdmissionTypeVisible()) {
            cols.add(new String[]{"Admission Type", "ADMISSION_TYPE"});
        }
        if (userSettingsController.isInwardBhtDepositDetailAdmittedVisible()) {
            cols.add(new String[]{"Admitted", "ADMITTED"});
        }
        if (userSettingsController.isInwardBhtDepositDetailDischargedVisible()) {
            cols.add(new String[]{"Discharged", "DISCHARGED"});
        }
        if (userSettingsController.isInwardBhtDepositDetailDateTimeVisible()) {
            cols.add(new String[]{"Date / Time", "DATETIME"});
        }
        if (userSettingsController.isInwardBhtDepositDetailPaymentMethodVisible()) {
            cols.add(new String[]{"Payment Method", "PAYMENT_METHOD"});
        }
        if (userSettingsController.isInwardBhtDepositDetailAmountVisible()) {
            cols.add(new String[]{"Amount", "AMOUNT"});
        }
        for (int i = 0; i < usedPaymentMethods.size(); i++) {
            cols.add(new String[]{usedPaymentMethods.get(i).getLabel(), "PM" + i});
        }
        if (userSettingsController.isInwardBhtDepositDetailReferenceNoVisible()) {
            cols.add(new String[]{"Reference No", "REFERENCE_NO"});
        }
        return cols;
    }

    private String pdfCellValue(BhtPaymentDetailDTO row, String kind, SimpleDateFormat sdf, SimpleDateFormat dtf) {
        switch (kind) {
            case "BILL_NO":
                return row.getBillNo() != null ? row.getBillNo() : "";
            case "BHT_NO":
                return row.getBhtNo() != null ? row.getBhtNo() : "";
            case "PATIENT_NAME":
                return row.getPatientName() != null ? row.getPatientName() : "";
            case "ADMISSION_TYPE":
                return row.getAdmissionType() != null ? row.getAdmissionType().getName() : "";
            case "ADMITTED":
                return row.getDateOfAdmission() != null ? sdf.format(row.getDateOfAdmission()) : "";
            case "DISCHARGED":
                return row.getDateOfDischarge() != null ? sdf.format(row.getDateOfDischarge()) : "";
            case "DATETIME":
                return row.getCreatedAt() != null ? dtf.format(row.getCreatedAt()) : "";
            case "PAYMENT_METHOD":
                return row.getPaymentMethod() != null ? row.getPaymentMethod().getLabel() : "";
            case "REFERENCE_NO":
                return row.getReferenceNo();
            default:
                return "";
        }
    }

    private float pdfColumnWidth(String kind) {
        switch (kind) {
            case "BILL_NO": return 1f;
            case "BHT_NO": return 0.8f;
            case "PATIENT_NAME": return 1.8f;
            case "ADMISSION_TYPE": return 1.1f;
            case "ADMITTED": return 0.9f;
            case "DISCHARGED": return 0.9f;
            case "DATETIME": return 1.3f;
            case "PAYMENT_METHOD": return 1.1f;
            case "AMOUNT": return 1f;
            case "REFERENCE_NO": return 1.2f;
            default: return 1f; // PM* columns
        }
    }

    private void addPdfCell(PdfPTable table, String text, Font font, int alignment, Color backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        if (backgroundColor != null) {
            cell.setBackgroundColor(backgroundColor);
        }
        table.addCell(cell);
    }

    private void addPdfInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(3f);
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(3f);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = endOfCurrentMonth();
        dateBasis = "dischargeDate";
        reportType = "ALL";
        admissionStatus = AdmissionStatus.ANY_STATUS;
        admissionType = null;
        paymentMethod = null;
        institution = null;
        site = null;
        department = null;
        reportRows = null;
        grandTotal = 0;
        totalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();
    }

    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    private static Date endOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
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

    public String getDateBasis() {
        return dateBasis;
    }

    public void setDateBasis(String dateBasis) {
        this.dateBasis = dateBasis;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public AdmissionStatus getAdmissionStatus() {
        return admissionStatus;
    }

    public void setAdmissionStatus(AdmissionStatus admissionStatus) {
        this.admissionStatus = admissionStatus;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public List<BhtPaymentDetailDTO> getReportRows() {
        return reportRows;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public List<PaymentMethod> getUsedPaymentMethods() {
        return usedPaymentMethods;
    }

    public Map<PaymentMethod, Double> getTotalByMethod() {
        return totalByMethod;
    }
}

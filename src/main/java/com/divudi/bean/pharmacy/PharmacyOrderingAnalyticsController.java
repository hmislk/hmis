/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ControllerWithReportFilters;
import com.divudi.bean.common.ReportTimerController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.ReportViewType;
import com.divudi.core.data.dto.OrderingRequirementRowDto;
import com.divudi.core.data.reports.PharmacyReports;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.DosageForm;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.pharmacy.PharmacyOrderingRequirementService;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Ordering Requirement Report (issue #22466).
 *
 * Tells a pharmacy buyer what to order and how much. Deliberately thin - every
 * figure is computed by PharmacyOrderingRequirementService, which has no JSF
 * dependency and can therefore be exercised on its own.
 *
 * @author Buddhika
 */
@Named(value = "pharmacyOrderingAnalyticsController")
@SessionScoped
public class PharmacyOrderingAnalyticsController implements Serializable, ControllerWithReportFilters {

    private static final long serialVersionUID = 1L;

    private static final String KEY_WINDOW_MONTHS = "Pharmacy Ordering - Default Consumption Window Months";
    private static final String KEY_TARGET_COVER_MONTHS = "Pharmacy Ordering - Default Target Cover Months";
    private static final String KEY_URGENT_THRESHOLD_MONTHS = "Pharmacy Ordering - Urgent Order Threshold Months";

    @EJB
    private PharmacyOrderingRequirementService pharmacyOrderingRequirementService;

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ReportTimerController reportTimerController;

    // Standard report filters
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;
    private ReportViewType reportViewType;

    // Item filters
    private Category category;
    private DosageForm dosageForm;
    private List<DepartmentType> selectedDepartmentTypes;

    // Report inputs, seeded from configuration so admins can set the house
    // default while buyers flex them per run.
    private double consumptionWindowMonths;
    private double targetCoverMonths;
    private double urgentThresholdMonths;

    private List<OrderingRequirementRowDto> rows;
    private long unclassifiedMovementCount;

    private boolean paginator = true;
    private int rowsPerPage = 50;

    /**
     * Entry point from the Ordering tab.
     *
     * Clears any previous result, defaults the scope to the logged-in
     * institution / site / department, and seeds the three numeric inputs from
     * configuration before deriving the initial date window.
     *
     * Initialisation lives here rather than in an f:viewAction because this
     * controller is @SessionScoped - a view action would re-run on every
     * refresh and back-button navigation and corrupt state the user had set.
     */
    public String navigateToOrderingRequirementReport() {
        rows = new ArrayList<>();
        unclassifiedMovementCount = 0;
        selectedDepartmentTypes = new ArrayList<>();
        category = null;
        dosageForm = null;

        if (institution == null) {
            institution = sessionController.getInstitution();
        }
        if (site == null) {
            site = sessionController.getLoggedSite();
        }
        if (department == null) {
            department = sessionController.getDepartment();
        }

        consumptionWindowMonths = configOptionApplicationController
                .getDoubleValueByKey(KEY_WINDOW_MONTHS, 3.0);
        targetCoverMonths = configOptionApplicationController
                .getDoubleValueByKey(KEY_TARGET_COVER_MONTHS, 3.0);
        urgentThresholdMonths = configOptionApplicationController
                .getDoubleValueByKey(KEY_URGENT_THRESHOLD_MONTHS, 1.0);

        applyWindowFromMonths();

        return "/pharmacy/reports/ordering_reports/ordering_requirement_report?faces-redirect=true";
    }

    /**
     * Recomputes the date window from the consumption-window months input.
     * Invoked when the buyer changes the number of months, so the dates stay in
     * step without them having to set both.
     */
    public void applyWindowFromMonths() {
        if (consumptionWindowMonths <= 0) {
            consumptionWindowMonths = 3.0;
        }
        Date end = toDate == null ? new Date() : toDate;
        Calendar cal = Calendar.getInstance();
        cal.setTime(end);
        cal.add(Calendar.DATE, -(int) Math.round(consumptionWindowMonths
                * PharmacyOrderingRequirementService.DAYS_PER_MONTH) + 1);
        fromDate = CommonFunctions.getStartOfDay(cal.getTime());
        toDate = CommonFunctions.getEndOfDay(end);
    }

    /**
     * Validates the filters and builds the report.
     *
     * All computation is delegated to PharmacyOrderingRequirementService; this
     * method only guards the inputs, wraps the call in the report timer, and
     * picks up the unclassified-movement count that drives the on-screen
     * warning.
     */
    public void processReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select a date range");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From date must be on or before the To date");
            return;
        }
        if (targetCoverMonths <= 0) {
            JsfUtil.addErrorMessage("Target stock cover must be greater than zero");
            return;
        }
        if (urgentThresholdMonths < 0) {
            JsfUtil.addErrorMessage("Urgent threshold cannot be negative");
            return;
        }

        reportTimerController.trackReportExecution(() -> {
            rows = pharmacyOrderingRequirementService.generateReport(
                    fromDate, toDate, institution, site, department,
                    category, dosageForm, selectedDepartmentTypes,
                    targetCoverMonths, urgentThresholdMonths);
            unclassifiedMovementCount = pharmacyOrderingRequirementService
                    .countUnclassifiedMovements(fromDate, toDate, institution, site, department);
        }, PharmacyReports.ORDERING_REQUIREMENT_REPORT, sessionController.getLoggedUser());

        if (rows == null || rows.isEmpty()) {
            JsfUtil.addErrorMessage("No items found for the selected filters");
        }
    }

    /**
     * Streams the current result as an .xlsx download.
     *
     * Writes straight to the HttpServletResponse and calls responseComplete()
     * so JSF does not also try to render the page, following the same pattern
     * as ReportsStock.exportCurrentStockByBatchToExcel().
     */
    public void exportToExcel() {
        if (rows == null || rows.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to export - process the report first");
            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        // Build the whole workbook in memory first. Writing straight to the
        // response stream would commit headers before generation finished, so a
        // POI failure part-way through would hand the user a truncated .xlsx and
        // leave addErrorMessage with nowhere to render.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet sheet = workbook.createSheet("Ordering Requirement");
            int rowIndex = 0;
            int totalColumns = 10;

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle filterStyle = workbook.createCellStyle();
            filterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            Row instRow = sheet.createRow(rowIndex++);
            Cell instCell = instRow.createCell(0);
            instCell.setCellValue(institution != null ? institution.getName() : "All Institutions");
            instCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            Row deptRow = sheet.createRow(rowIndex++);
            Cell deptCell = deptRow.createCell(0);
            deptCell.setCellValue(department != null ? department.getName() : "All Departments");
            deptCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Ordering Requirement Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            Row filterRow = sheet.createRow(rowIndex++);
            Cell filterCell = filterRow.createCell(0);
            filterCell.setCellValue(getFilterSummary());
            filterCell.setCellStyle(filterStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, totalColumns - 1));

            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {
                "Drug Name", "Current Balance", "Consumption (Period)",
                "Avg Monthly Consumption", "Stock Cover (Months)", "Target Stock",
                "Qty to Order", "Est. Cost (Rs.)", "Decision", "Last Supplier"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (OrderingRequirementRowDto r : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                createCell(dataRow, 0, r.getItemName(), dataStyle);
                createCell(dataRow, 1, r.getCurrentBalance(), numberStyle);
                createCell(dataRow, 2, r.getConsumption(), numberStyle);
                createCell(dataRow, 3, r.getAvgMonthlyConsumption(), numberStyle);
                createCell(dataRow, 4, r.getStockCoverDisplay(), dataStyle);
                createCell(dataRow, 5, r.getTargetStock(), numberStyle);
                createCell(dataRow, 6, r.getQuantityToOrder(), numberStyle);
                createCell(dataRow, 7, r.getEstimatedCost(), numberStyle);
                createCell(dataRow, 8, r.getDecision(), dataStyle);
                createCell(dataRow, 9, r.getLastSupplier(), dataStyle);
            }

            for (int i = 0; i < totalColumns; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(buffer);
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Could not create the Excel file: " + e.getMessage());
            return;
        }

        writeDownload(context, response,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "Ordering_Requirement_Report.xlsx", buffer.toByteArray(),
                "Could not send the Excel file: ");
    }

    /**
     * Commits a fully generated binary payload as a download.
     *
     * Kept separate so nothing touches the response until the bytes exist - the
     * point of buffering is lost if headers are set while generation can still
     * fail.
     */
    private void writeDownload(FacesContext context, HttpServletResponse response,
            String contentType, String fileName, byte[] content, String errorPrefix) {
        try {
            response.reset();
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.setContentLength(content.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(content);
                out.flush();
            }
            context.responseComplete();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(errorPrefix + e.getMessage());
        }
    }

    /**
     * Streams the current result as a landscape A4 PDF download.
     *
     * Landscape because eight numeric columns do not fit portrait at a legible
     * font size.
     */
    public void exportToPdf() {
        if (rows == null || rows.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to export - process the report first");
            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        // Same reason as the Excel export: generate fully in memory so an iText
        // failure cannot leave a half-written PDF on the wire.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(document, buffer);
            document.open();

            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font smallFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8);
            com.itextpdf.text.Font headerFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD);

            Paragraph institutionPara = new Paragraph(
                    institution != null ? institution.getName() : "All Institutions", titleFont);
            institutionPara.setAlignment(Element.ALIGN_CENTER);
            document.add(institutionPara);

            Paragraph titlePara = new Paragraph("Ordering Requirement Report", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);

            Paragraph filterPara = new Paragraph(getFilterSummary(), smallFont);
            filterPara.setAlignment(Element.ALIGN_CENTER);
            document.add(filterPara);
            document.add(new Paragraph(" ", smallFont));

            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20, 9, 10, 10, 8, 9, 9, 9, 8, 15});

            String[] headers = {
                "Drug Name", "Current Balance", "Consumption (Period)",
                "Avg Monthly Consumption", "Stock Cover", "Target Stock",
                "Qty to Order", "Est. Cost (Rs.)", "Decision", "Last Supplier"
            };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (OrderingRequirementRowDto r : rows) {
                table.addCell(new PdfPCell(new Phrase(r.getItemName(), smallFont)));
                addNumericCell(table, r.getCurrentBalance(), smallFont);
                addNumericCell(table, r.getConsumption(), smallFont);
                addNumericCell(table, r.getAvgMonthlyConsumption(), smallFont);
                PdfPCell coverCell = new PdfPCell(new Phrase(r.getStockCoverDisplay(), smallFont));
                coverCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(coverCell);
                addNumericCell(table, r.getTargetStock(), smallFont);
                addNumericCell(table, r.getQuantityToOrder(), smallFont);
                addNumericCell(table, r.getEstimatedCost(), smallFont);
                table.addCell(new PdfPCell(new Phrase(r.getDecision(), smallFont)));
                table.addCell(new PdfPCell(new Phrase(r.getLastSupplier(), smallFont)));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            JsfUtil.addErrorMessage("Could not create the PDF file: " + e.getMessage());
            return;
        }

        writeDownload(context, response, "application/pdf",
                "Ordering_Requirement_Report.pdf", buffer.toByteArray(),
                "Could not send the PDF file: ");
    }

    private void addNumericCell(PdfPTable table, double value, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(String.format("%,.2f", value), font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private void createCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int index, double value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * One-line description of the active filters, printed on the Excel and PDF
     * exports so a saved copy stays self-explanatory.
     */
    public String getFilterSummary() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        return "Window: " + (fromDate == null ? "-" : sdf.format(fromDate))
                + " to " + (toDate == null ? "-" : sdf.format(toDate))
                + " | Target Cover: " + targetCoverMonths + " months"
                + " | Urgent Below: " + urgentThresholdMonths + " months"
                + " | Category: " + (category != null ? category.getName() : "All")
                + " | Dosage Form: " + (dosageForm != null ? dosageForm.getName() : "All")
                + " | Department Type: " + getSelectedDepartmentTypesPrintDisplay();
    }

    /** Comma-separated department types for the export header, "All" when none are picked. */
    public String getSelectedDepartmentTypesPrintDisplay() {
        if (selectedDepartmentTypes == null || selectedDepartmentTypes.isEmpty()) {
            return "All";
        }
        return selectedDepartmentTypes.stream()
                .map(DepartmentType::getLabel)
                .collect(Collectors.joining(", "));
    }

    /** Department types offered in the filter, matching the Batch Stock report. */
    public List<DepartmentType> getAvailableDepartmentTypes() {
        return Arrays.asList(
                DepartmentType.Pharmacy,
                DepartmentType.Store,
                DepartmentType.Lab,
                DepartmentType.Kitchen
        );
    }

    /** True when the scope spans more than one department, so the internal
     * transfer caveat applies. */
    public boolean isMultiDepartmentScope() {
        return department == null;
    }

    /** Sum of Est. Cost across every row - the table footer and the summary tile. */
    public double getTotalEstimatedCost() {
        if (rows == null) {
            return 0.0;
        }
        return rows.stream().mapToDouble(OrderingRequirementRowDto::getEstimatedCost).sum();
    }

    /** Number of rows decided Urgent Order, shown as a summary tile. */
    public long getUrgentCount() {
        if (rows == null) {
            return 0;
        }
        return rows.stream().filter(OrderingRequirementRowDto::isUrgent).count();
    }

    /** Number of rows decided Order, shown as a summary tile. */
    public long getOrderCount() {
        if (rows == null) {
            return 0;
        }
        return rows.stream().filter(OrderingRequirementRowDto::isOrder).count();
    }

    // Getters and setters
    @Override
    public Date getFromDate() {
        return fromDate;
    }

    @Override
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @Override
    public Date getToDate() {
        return toDate;
    }

    @Override
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @Override
    public Institution getInstitution() {
        return institution;
    }

    @Override
    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @Override
    public Institution getSite() {
        return site;
    }

    @Override
    public void setSite(Institution site) {
        this.site = site;
    }

    @Override
    public Department getDepartment() {
        return department;
    }

    @Override
    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    @Override
    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    @Override
    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    @Override
    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    @Override
    public ReportViewType getReportViewType() {
        return reportViewType;
    }

    @Override
    public void setReportViewType(ReportViewType reportViewType) {
        this.reportViewType = reportViewType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public DosageForm getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(DosageForm dosageForm) {
        this.dosageForm = dosageForm;
    }

    public List<DepartmentType> getSelectedDepartmentTypes() {
        if (selectedDepartmentTypes == null) {
            selectedDepartmentTypes = new ArrayList<>();
        }
        return selectedDepartmentTypes;
    }

    public void setSelectedDepartmentTypes(List<DepartmentType> selectedDepartmentTypes) {
        this.selectedDepartmentTypes = selectedDepartmentTypes;
    }

    public double getConsumptionWindowMonths() {
        return consumptionWindowMonths;
    }

    public void setConsumptionWindowMonths(double consumptionWindowMonths) {
        this.consumptionWindowMonths = consumptionWindowMonths;
    }

    public double getTargetCoverMonths() {
        return targetCoverMonths;
    }

    public void setTargetCoverMonths(double targetCoverMonths) {
        this.targetCoverMonths = targetCoverMonths;
    }

    public double getUrgentThresholdMonths() {
        return urgentThresholdMonths;
    }

    public void setUrgentThresholdMonths(double urgentThresholdMonths) {
        this.urgentThresholdMonths = urgentThresholdMonths;
    }

    public List<OrderingRequirementRowDto> getRows() {
        return rows;
    }

    public void setRows(List<OrderingRequirementRowDto> rows) {
        this.rows = rows;
    }

    public long getUnclassifiedMovementCount() {
        return unclassifiedMovementCount;
    }

    public boolean isPaginator() {
        return paginator;
    }

    public void setPaginator(boolean paginator) {
        this.paginator = paginator;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }
}

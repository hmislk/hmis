package com.divudi.bean.pharmacy;

import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.PharmacyService;

import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * F15 drill-down Level 1 controller — date-range summary parameterised by
 * transaction type and a multi-selected list of BillTypeAtomic.
 *
 * Reuses the F15 column layout but allows the user to scope the report to
 * a custom date range and a custom subset of bill type atomics. Calls into
 * the *ForAtomics PharmacyService methods added in issue #20237.
 *
 * Parent epic: #20236. This controller: #20238.
 */
@Named
@SessionScoped
public class PharmacyDailyStockDrillDownLevel1Controller implements Serializable {

    public enum TransactionType {
        SALES,
        PURCHASES,
        TRANSFERS,
        ADJUSTMENTS
    }

    @EJB
    private PharmacyService pharmacyService;
    @Inject
    private SessionController sessionController;
    @Inject
    private InstitutionController institutionController;
    @Inject
    private DepartmentController departmentController;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;

    private TransactionType transactionType = TransactionType.SALES;
    private List<BillTypeAtomic> availableAtomics = new ArrayList<>();
    private List<BillTypeAtomic> selectedAtomics = new ArrayList<>();

    private PharmacyBundle bundle;

    public PharmacyDailyStockDrillDownLevel1Controller() {
    }

    /**
     * Entry point used by the menu / direct navigation. Defaults filters to
     * session institution / site / department and today's date if not yet set,
     * then redirects to the L1 page.
     */
    public String navigateToLevel1() {
        if (institution == null) {
            institution = sessionController.getInstitution();
        }
        if (site == null) {
            site = sessionController.getLoggedSite();
        }
        if (department == null) {
            department = sessionController.getDepartment();
        }
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        rebuildAtomicOptions();
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level1?faces-redirect=true";
    }

    /**
     * Entry point reserved for the F15 → L1 drill-down (issue #20239).
     * Copies F15 filters and the chosen transaction type / initial atomic list
     * into L1 state, then redirects. Not yet wired from F15 — kept for #20239.
     */
    public String navigateToLevel1FromF15(Date f15FromDate, Date f15ToDate,
                                          Institution f15Institution, Institution f15Site, Department f15Department,
                                          TransactionType type, List<BillTypeAtomic> initialAtomics) {
        this.fromDate = f15FromDate;
        this.toDate = f15ToDate;
        this.institution = f15Institution;
        this.site = f15Site;
        this.department = f15Department;
        this.transactionType = type != null ? type : TransactionType.SALES;
        rebuildAtomicOptions();
        if (initialAtomics != null && !initialAtomics.isEmpty()) {
            this.selectedAtomics = new ArrayList<>(initialAtomics);
        }
        processReport();
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level1?faces-redirect=true";
    }

    /**
     * AJAX-invoked when the transaction-type radio changes. Repopulates
     * availableAtomics from the matching getPharmacy*BillTypes() and ticks all
     * by default.
     */
    public void onTransactionTypeChange() {
        rebuildAtomicOptions();
    }

    private void rebuildAtomicOptions() {
        availableAtomics = atomicsForCurrentType();
        selectedAtomics = new ArrayList<>(availableAtomics);
    }

    private List<BillTypeAtomic> atomicsForCurrentType() {
        if (transactionType == null) {
            return Collections.emptyList();
        }
        switch (transactionType) {
            case SALES:
                return pharmacyService.getPharmacyIncomeBillTypes();
            case PURCHASES:
                return pharmacyService.getPharmacyPurchaseBillTypes();
            case TRANSFERS:
                return pharmacyService.getPharmacyInternalTransferBillTypes();
            case ADJUSTMENTS:
                return pharmacyService.getPharmacyAdjustmentBillTypes();
            default:
                return Collections.emptyList();
        }
    }

    public void processReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates");
            return;
        }
        if (transactionType == null) {
            JsfUtil.addErrorMessage("Please select a transaction type");
            return;
        }
        Date startOfDay = CommonFunctions.getStartOfDay(fromDate);
        Date endOfDay = CommonFunctions.getEndOfDay(toDate);
        List<BillTypeAtomic> atomics = (selectedAtomics == null || selectedAtomics.isEmpty())
                ? atomicsForCurrentType()
                : selectedAtomics;

        switch (transactionType) {
            case SALES:
                bundle = pharmacyService.fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionTypeDtoForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
            case PURCHASES:
                bundle = pharmacyService.fetchPharmacyStockPurchaseValueByBillTypeDtoCompletedForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
            case TRANSFERS:
                bundle = pharmacyService.fetchPharmacyTransferValueByBillTypeDtoForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
            case ADJUSTMENTS:
                bundle = pharmacyService.fetchPharmacyAdjustmentValueByBillTypeDtoForAtomics(
                        startOfDay, endOfDay, institution, site, department, null, null, null, atomics);
                break;
        }
    }

    /**
     * Back-to-F15 navigation. F15's controller is @SessionScoped so its
     * filter values persist automatically — no explicit hand-back needed.
     */
    public String navigateBackToF15() {
        return "/pharmacy/reports/summary_reports/daily_stock_values_report_optimized?faces-redirect=true";
    }

    /**
     * Navigate to the Level 1 print page — issue #20243. The print page reads
     * the current bundle and filters straight from this @SessionScoped
     * controller, so no data hand-off is needed.
     */
    public String navigateToPrintPage() {
        if (bundle == null) {
            JsfUtil.addErrorMessage("Please generate the report first before printing");
            return null;
        }
        return "/pharmacy/reports/summary_reports/daily_stock_values_drill_down_level1_print?faces-redirect=true";
    }

    @Inject
    private PharmacyDailyStockDrillDownLevel2Controller drillDownLevel2Controller;

    /**
     * Drill from a Level 1 row into Level 2 (per-bill list) — issue #20240.
     * Carries the L1 filters (date range, institution/site/department) plus the
     * row's selection key (BillTypeAtomic, AdmissionType, PaymentScheme — the
     * latter two only meaningful for SALES rows) into the L2 controller.
     */
    public String drillToLevel2(PharmacyRow row) {
        if (row == null || row.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("Cannot drill — row has no bill type");
            return null;
        }
        return drillDownLevel2Controller.navigateToLevel2FromLevel1(
                fromDate, toDate, institution, site, department,
                transactionType, row.getBillTypeAtomic(),
                row.getAdmissionType(), row.getPaymentScheme());
    }

    /**
     * Download the Level 1 result as an .xlsx file — issue #20242.
     * Mirrors the on-screen layout: header band (filters), single coloured
     * section based on transaction type, and a bold total row. Uses the same
     * Apache POI pattern as PharmacyDailyStockReportOptimizedController.downloadExcel.
     */
    public void downloadExcel() {
        if (bundle == null || fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please generate the report first before downloading");
            return;
        }
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String fileName = "Pharmacy_TransactionsByAtomic_"
                    + dateFormat.format(fromDate) + "_to_"
                    + dateFormat.format(toDate) + ".xlsx";

            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            Workbook workbook = createExcelReport();
            OutputStream out = response.getOutputStream();
            workbook.write(out);
            workbook.close();
            out.flush();
            out.close();

            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
            JsfUtil.addErrorMessage("Error generating Excel report: " + e.getMessage());
        }
    }

    private Workbook createExcelReport() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("L1 Transactions");

        short numFmt = workbook.createDataFormat().getFormat("#,##0.00");

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle labelStyle = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);

        CellStyle numStyle = workbook.createCellStyle();
        numStyle.setDataFormat(numFmt);
        numStyle.setAlignment(HorizontalAlignment.RIGHT);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("F15 Drill-Down — Level 1 (Date-Range Summary)");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));

        rowNum++; // blank

        // Header band
        rowNum = writeKeyValueRow(sheet, rowNum, "From Date", dateFormat.format(fromDate), labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "To Date", dateFormat.format(toDate), labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Institution", institution != null ? institution.getName() : "", labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Site", site != null ? site.getName() : "", labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Department", department != null ? department.getName() : "", labelStyle);
        rowNum = writeKeyValueRow(sheet, rowNum, "Transaction Type", transactionType != null ? transactionType.name() : "", labelStyle);

        String atomicsLabel;
        if (selectedAtomics == null || selectedAtomics.isEmpty()) {
            atomicsLabel = "All";
        } else {
            atomicsLabel = selectedAtomics.stream()
                    .map(BillTypeAtomic::getLabel)
                    .collect(Collectors.joining(", "));
        }
        rowNum = writeKeyValueRow(sheet, rowNum, "Selected Atomics", atomicsLabel, labelStyle);

        rowNum++; // blank

        // Single coloured section based on transaction type
        byte[] sectionRgb = colourForTransactionType();
        String sectionTitle = sectionTitleForTransactionType();
        String firstColLabel = firstColLabelForTransactionType();

        String[] cols = {firstColLabel, "Gross Value", "Discount", "Service Charge", "Net Value",
                         "Stock Value (Cost)", "Stock Value (Purchase)", "Stock Value (Retail)"};
        rowNum = writeSectionHeader(sheet, workbook, rowNum, sectionTitle, cols, sectionRgb);

        if (bundle.getRows() != null) {
            for (PharmacyRow row : bundle.getRows()) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.getRowType() != null ? row.getRowType() : "");
                setCellNum(r, 1, row.getGrossTotal(), numStyle);
                setCellNum(r, 2, row.getDiscount(), numStyle);
                setCellNum(r, 3, row.getServiceCharge(), numStyle);
                setCellNum(r, 4, row.getNetTotal(), numStyle);
                setCellNum(r, 5, doubleOrZero(row.getValueOfStocksAtCostRate()), numStyle);
                setCellNum(r, 6, doubleOrZero(row.getValueOfStocksAtPurchaseRate()), numStyle);
                setCellNum(r, 7, doubleOrZero(row.getValueOfStocksAtRetailSaleRate()), numStyle);
            }
        }

        // Total row
        PharmacyRow s = bundle.getSummaryRow();
        if (s != null) {
            CellStyle totalStyle = makeSolidStyle(workbook, sectionRgb, numFmt, true, true);
            rowNum = writeTotalRow(sheet, rowNum, "Total",
                    new double[]{s.getGrossTotal(), s.getDiscount(), s.getServiceCharge(), s.getNetTotal(),
                                 doubleOrZero(s.getValueOfStocksAtCostRate()),
                                 doubleOrZero(s.getValueOfStocksAtPurchaseRate()),
                                 doubleOrZero(s.getValueOfStocksAtRetailSaleRate())},
                    totalStyle);
        }

        for (int c = 0; c < 8; c++) {
            sheet.autoSizeColumn(c);
        }
        return workbook;
    }

    private byte[] colourForTransactionType() {
        if (transactionType == null) {
            return new byte[]{(byte) 0x19, (byte) 0x87, (byte) 0x54};
        }
        switch (transactionType) {
            case SALES:
                return new byte[]{(byte) 0x19, (byte) 0x87, (byte) 0x54}; // green
            case PURCHASES:
                return new byte[]{(byte) 0xFF, (byte) 0x8C, (byte) 0x00}; // orange
            case TRANSFERS:
                return new byte[]{(byte) 0x0D, (byte) 0xA2, (byte) 0xB8}; // cyan
            case ADJUSTMENTS:
                return new byte[]{(byte) 0xDC, (byte) 0x35, (byte) 0x45}; // red
            default:
                return new byte[]{(byte) 0x19, (byte) 0x87, (byte) 0x54};
        }
    }

    private String sectionTitleForTransactionType() {
        if (transactionType == null) {
            return "Transactions";
        }
        switch (transactionType) {
            case SALES:
                return "Sales Transactions";
            case PURCHASES:
                return "Purchase Transactions";
            case TRANSFERS:
                return "Transfer Transactions";
            case ADJUSTMENTS:
                return "Adjustment Transactions";
            default:
                return "Transactions";
        }
    }

    private String firstColLabelForTransactionType() {
        if (transactionType == null) {
            return "Row Type";
        }
        switch (transactionType) {
            case SALES:
                return "Sale Type";
            case PURCHASES:
                return "Purchase Type";
            case TRANSFERS:
                return "Transfer Type";
            case ADJUSTMENTS:
                return "Adjustment Type";
            default:
                return "Row Type";
        }
    }

    private int writeKeyValueRow(Sheet sheet, int rowNum, String key, String value, CellStyle keyStyle) {
        Row r = sheet.createRow(rowNum++);
        Cell k = r.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(keyStyle);
        r.createCell(1).setCellValue(value != null ? value : "");
        return rowNum;
    }

    private int writeSectionHeader(Sheet sheet, XSSFWorkbook wb, int rowNum,
                                   String sectionTitle, String[] colHeaders, byte[] rgb) {
        CellStyle sectionStyle = makeSolidStyle(wb, rgb, (short) 0, true, true);
        Row sectionRow = sheet.createRow(rowNum++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue(sectionTitle);
        sectionCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));

        byte[] lightRgb = lighten(rgb);
        CellStyle colStyle = makeSolidStyle(wb, lightRgb, (short) 0, true, false);
        Row colRow = sheet.createRow(rowNum++);
        for (int i = 0; i < colHeaders.length; i++) {
            Cell c = colRow.createCell(i);
            c.setCellValue(colHeaders[i]);
            c.setCellStyle(colStyle);
        }
        return rowNum;
    }

    private int writeTotalRow(Sheet sheet, int rowNum, String label, double[] values, CellStyle style) {
        Row r = sheet.createRow(rowNum++);
        Cell labelCell = r.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(style);
        for (int i = 0; i < values.length; i++) {
            Cell c = r.createCell(i + 1);
            c.setCellValue(values[i]);
            c.setCellStyle(style);
        }
        return rowNum;
    }

    private void setCellNum(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private double doubleOrZero(java.math.BigDecimal v) {
        return v != null ? v.doubleValue() : 0.0;
    }

    private XSSFCellStyle makeSolidStyle(XSSFWorkbook wb, byte[] rgb, short numFmt, boolean bold, boolean whiteText) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        if (numFmt != 0) {
            style.setDataFormat(numFmt);
        }
        style.setAlignment(HorizontalAlignment.LEFT);
        Font font = wb.createFont();
        font.setBold(bold);
        if (whiteText) {
            font.setColor(IndexedColors.WHITE.getIndex());
        }
        style.setFont(font);
        return style;
    }

    private byte[] lighten(byte[] rgb) {
        byte[] out = new byte[3];
        for (int i = 0; i < 3; i++) {
            int v = (rgb[i] & 0xFF);
            out[i] = (byte) Math.min(255, v + 80);
        }
        return out;
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public List<BillTypeAtomic> getAvailableAtomics() {
        return availableAtomics;
    }

    public void setAvailableAtomics(List<BillTypeAtomic> availableAtomics) {
        this.availableAtomics = availableAtomics;
    }

    public List<BillTypeAtomic> getSelectedAtomics() {
        return selectedAtomics;
    }

    public void setSelectedAtomics(List<BillTypeAtomic> selectedAtomics) {
        this.selectedAtomics = selectedAtomics;
    }

    public PharmacyBundle getBundle() {
        return bundle;
    }

    public void setBundle(PharmacyBundle bundle) {
        this.bundle = bundle;
    }

    public TransactionType[] getTransactionTypes() {
        return TransactionType.values();
    }
}

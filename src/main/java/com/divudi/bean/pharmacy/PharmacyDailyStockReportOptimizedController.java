package com.divudi.bean.pharmacy;

import com.divudi.bean.common.*;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.pharmacy.DailyStockBalanceReport;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.PharmacyService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
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
 * Optimized controller for Daily Stock Balance Report This is a
 * performance-optimized version of the report that uses simplified queries
 *
 * @author Claude Code Optimizer
 */
@Named
@SessionScoped
public class PharmacyDailyStockReportOptimizedController implements Serializable {

    @EJB
    private StockHistoryFacade stockHistoryFacade;
    @EJB
    private PharmacyService pharmacyService;
    @Inject
    private SessionController sessionController;
    @Inject
    private InstitutionController institutionController;
    @Inject
    private DepartmentController departmentController;

    // Filter properties
    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Institution site;
    private Department department;

    // Report data
    private DailyStockBalanceReport dailyStockBalanceReport;

    public PharmacyDailyStockReportOptimizedController() {
    }

    public String navigateToDailyStockReport() {
        if (institution == null) {
            institution = sessionController.getInstitution();

        }
        if (site == null) {
            site = sessionController.getLoggedSite();
        }
        if (department == null) {
            department = sessionController.getDepartment();
        }
        return "/pharmacy/reports/summary_reports/daily_stock_values_report_optimized?faces-redirect=true";
    }

    /**
     * Optimized version of the daily stock balance report processing Uses
     * simplified queries with better performance
     */
    public void processDailyStockBalanceReportOptimized() {
        long overallStartTime = System.currentTimeMillis();

        System.out.println("==========================================");
        System.out.println("=== PERFORMANCE ANALYSIS: Daily Stock Report START ===");
        System.out.println("=== Timestamp: " + new Date() + " ===");
        System.out.println("From Date: " + fromDate);
        System.out.println("Department: " + department);
        System.out.println("Department ID: " + (department != null ? department.getId() : "null"));
        System.out.println("Department Name: " + (department != null ? department.getName() : "null"));

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        if (fromDate == null) {
            JsfUtil.addErrorMessage("Please select a date");
            return;
        }

        // STEP 1: Initialize report object
        long stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 1: Initializing report object...");

        dailyStockBalanceReport = new DailyStockBalanceReport();
        dailyStockBalanceReport.setDate(fromDate);
        dailyStockBalanceReport.setDepartment(department);

        long stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 1 COMPLETED: " + stepDuration + "ms - Report object initialized");

        // STEP 2: Calculate Opening Stock
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 2: Calculating OPENING stock value...");
        System.out.println(">>> Date for opening stock: " + fromDate);
        System.out.println(">>> Department for opening stock: " + department.getName());

        double openingStockValueAtRetailRate = calculateStockValueAtRetailRateOptimized(fromDate, department);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 2 COMPLETED: " + stepDuration + "ms - Opening stock: " + openingStockValueAtRetailRate);
        dailyStockBalanceReport.setOpeningStockValue(openingStockValueAtRetailRate);

        // STEP 2A: Calculate Opening Stock at Cost Rate
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 2A: Calculating OPENING stock value at COST rate...");
        System.out.println(">>> Date for opening stock (cost): " + fromDate);
        System.out.println(">>> Department for opening stock (cost): " + department.getName());

        double openingStockValueAtCostRate = calculateStockValueAtCostRateOptimized(fromDate, department);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 2A COMPLETED: " + stepDuration + "ms - Opening stock at cost rate: " + openingStockValueAtCostRate);
        dailyStockBalanceReport.setOpeningStockValueAtCostRate(openingStockValueAtCostRate);

        // STEP 2B: Calculate Opening Stock at Purchase Rate
        stepStartTime = System.currentTimeMillis();
        double openingStockValueAtPurchaseRate = calculateStockValueAtPurchaseRateOptimized(fromDate, department);
        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 2B COMPLETED: " + stepDuration + "ms - Opening stock at purchase rate: " + openingStockValueAtPurchaseRate);
        dailyStockBalanceReport.setOpeningStockValueAtPurchaseRate(openingStockValueAtPurchaseRate);

        // STEP 3: Calculate date range
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 3: Setting up date range...");

        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(Calendar.DATE, 1);
        toDate = cal.getTime();

        Date startOfTheDay = CommonFunctions.getStartOfDay(fromDate);
        Date endOfTheDay = CommonFunctions.getEndOfDay(fromDate);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 3 COMPLETED: " + stepDuration + "ms");
        System.out.println(">>> Start of day: " + startOfTheDay);
        System.out.println(">>> End of day: " + endOfTheDay);
        System.out.println(">>> Next day (for closing): " + toDate);

        // STEP 4: Fetch Sales Data
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 4: Fetching SALES data...");

        PharmacyBundle saleBundle = pharmacyService.fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionTypeDto(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 4 COMPLETED: " + stepDuration + "ms - Sales data fetched");
        System.out.println(">>> Sales records found: " + (saleBundle != null ? saleBundle.getRows().size() : "null"));
        System.out.println(">>> Sales total: " + (saleBundle != null && saleBundle.getSummaryRow() != null ? saleBundle.getSummaryRow().getNetTotal() : "null"));
        dailyStockBalanceReport.setPharmacySalesByAdmissionTypeAndDiscountSchemeBundle(saleBundle);

        // STEP 4B: Fetch Pre-Bill Stock Movements (informational section)
        // Covers PRE_TO_SETTLE_AT_CASHIER and CANCELLED_PRE — stock that moved
        // within the day but whose financial settlement may not yet be recorded.
        PharmacyBundle preBillBundle = pharmacyService.fetchPharmacyF15StockMovementBundleDto(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPreBillStockMovementsBundle(preBillBundle);

        // STEP 5: Fetch Purchase Data (only completed bills)
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 5: Fetching PURCHASE data (completed=true only)...");

        PharmacyBundle purchaseBundle = pharmacyService.fetchPharmacyStockPurchaseValueByBillTypeDtoCompleted(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 5 COMPLETED: " + stepDuration + "ms - Purchase data fetched");
        System.out.println(">>> Purchase records found: " + (purchaseBundle != null ? purchaseBundle.getRows().size() : "null"));
        System.out.println(">>> Purchase total: " + (purchaseBundle != null && purchaseBundle.getSummaryRow() != null ? purchaseBundle.getSummaryRow().getValueOfStocksAtRetailSaleRate() : "null"));
        dailyStockBalanceReport.setPharmacyPurchaseByBillTypeBundle(purchaseBundle);

        // STEP 6: Fetch Transfer Data
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 6: Fetching TRANSFER data...");

        PharmacyBundle transferBundle = pharmacyService.fetchPharmacyTransferValueByBillTypeDto(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 6 COMPLETED: " + stepDuration + "ms - Transfer data fetched");
        System.out.println(">>> Transfer records found: " + (transferBundle != null ? transferBundle.getRows().size() : "null"));
        System.out.println(">>> Transfer total: " + (transferBundle != null && transferBundle.getSummaryRow() != null ? transferBundle.getSummaryRow().getValueOfStocksAtRetailSaleRate() : "null"));
        dailyStockBalanceReport.setPharmacyTransferByBillTypeBundle(transferBundle);

        // STEP 7: Fetch Adjustment Data
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 7: Fetching ADJUSTMENT data...");

        PharmacyBundle adjustmentBundle = pharmacyService.fetchPharmacyAdjustmentValueByBillTypeDto(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 7 COMPLETED: " + stepDuration + "ms - Adjustment data fetched");
        System.out.println(">>> Adjustment records found: " + (adjustmentBundle != null ? adjustmentBundle.getRows().size() : "null"));
        System.out.println(">>> Adjustment total: " + (adjustmentBundle != null && adjustmentBundle.getSummaryRow() != null ? adjustmentBundle.getSummaryRow().getValueOfStocksAtRetailSaleRate() : "null"));
        dailyStockBalanceReport.setPharmacyAdjustmentsByBillTypeBundle(adjustmentBundle);

        // STEP 8: Calculate Closing Stock
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 8: Calculating CLOSING stock value...");
        System.out.println(">>> Date for closing stock: " + toDate);

        double closingStockValueAtRetailRate = calculateStockValueAtRetailRateOptimized(toDate, department);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 8 COMPLETED: " + stepDuration + "ms - Closing stock: " + closingStockValueAtRetailRate);
        dailyStockBalanceReport.setClosingStockValue(closingStockValueAtRetailRate);

        // STEP 8A: Calculate Closing Stock at Cost Rate
        stepStartTime = System.currentTimeMillis();
        System.out.println("\n🔄 STEP 8A: Calculating CLOSING stock value at COST rate...");
        System.out.println(">>> Date for closing stock (cost): " + toDate);

        double closingStockValueAtCostRate = calculateStockValueAtCostRateOptimized(toDate, department);

        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 8A COMPLETED: " + stepDuration + "ms - Closing stock at cost rate: " + closingStockValueAtCostRate);
        dailyStockBalanceReport.setClosingStockValueAtCostRate(closingStockValueAtCostRate);

        // STEP 8B: Calculate Closing Stock at Purchase Rate
        stepStartTime = System.currentTimeMillis();
        double closingStockValueAtPurchaseRate = calculateStockValueAtPurchaseRateOptimized(toDate, department);
        stepDuration = System.currentTimeMillis() - stepStartTime;
        System.out.println("✅ STEP 8B COMPLETED: " + stepDuration + "ms - Closing stock at purchase rate: " + closingStockValueAtPurchaseRate);
        dailyStockBalanceReport.setClosingStockValueAtPurchaseRate(closingStockValueAtPurchaseRate);

        // OVERALL COMPLETION SUMMARY
        long totalDuration = System.currentTimeMillis() - overallStartTime;
        System.out.println("\n==========================================");
        System.out.println("=== PERFORMANCE ANALYSIS SUMMARY ===");
        System.out.println("✅ TOTAL DURATION: " + totalDuration + "ms (" + (totalDuration/1000.0) + " seconds)");
        System.out.println("📊 Opening Stock (Retail Rate): " + openingStockValueAtRetailRate);
        System.out.println("📊 Opening Stock (Cost Rate): " + openingStockValueAtCostRate);
        System.out.println("📊 Closing Stock (Retail Rate): " + closingStockValueAtRetailRate);
        System.out.println("📊 Closing Stock (Cost Rate): " + closingStockValueAtCostRate);
        System.out.println("📊 Sales Records: " + (saleBundle != null ? saleBundle.getRows().size() : "0"));
        System.out.println("📊 Purchase Records: " + (purchaseBundle != null ? purchaseBundle.getRows().size() : "0"));
        System.out.println("📊 Transfer Records: " + (transferBundle != null ? transferBundle.getRows().size() : "0"));
        System.out.println("📊 Adjustment Records: " + (adjustmentBundle != null ? adjustmentBundle.getRows().size() : "0"));
        System.out.println("=== PERFORMANCE ANALYSIS END ===");
        System.out.println("==========================================");
    }

    /**
     * OPTIMIZED: Calculates the stock value at retail rate for a given date and
     * department. Delegates to the facade method for better encapsulation.
     *
     * @param date The date for which to calculate stock value
     * @param dept The department for which to calculate stock value
     * @return The total stock value at retail rate, or 0.0 if calculation fails
     */
    private double calculateStockValueAtRetailRateOptimized(Date date, Department dept) {
        long stockCalcStartTime = System.currentTimeMillis();

        System.out.println("🔍 === STOCK CALCULATION START ===");
        System.out.println("📅 Date param: " + date);
        System.out.println("🏥 Department: " + (dept != null ? dept.getName() : "null"));
        System.out.println("🆔 Department ID: " + (dept != null ? dept.getId() : "null"));

        try {
            Long departmentId = (dept != null) ? dept.getId() : null;
            System.out.println("🔄 Calling StockHistoryFacade.calculateStockValueAtRetailRateOptimized...");

            long facadeStartTime = System.currentTimeMillis();
            double result = stockHistoryFacade.calculateStockValueAtRetailRateOptimized(date, departmentId);
            long facadeDuration = System.currentTimeMillis() - facadeStartTime;

            long totalDuration = System.currentTimeMillis() - stockCalcStartTime;

            System.out.println("✅ FACADE COMPLETED: " + facadeDuration + "ms");
            System.out.println("💰 Stock Value Result: " + result);
            System.out.println("⏱️ Total Stock Calc Duration: " + totalDuration + "ms");
            System.out.println("🔍 === STOCK CALCULATION END ===");

            return result;
        } catch (Exception e) {
            long errorDuration = System.currentTimeMillis() - stockCalcStartTime;
            System.err.println("🚨 === STOCK CALCULATION ERROR ===");
            System.err.println("⏱️ Time before error: " + errorDuration + "ms");
            System.err.println("❌ Error message: " + e.getMessage());
            System.err.println("📍 Error type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.err.println("🚨 === STOCK CALCULATION ERROR END ===");

            JsfUtil.addErrorMessage(e, "Error calculating stock value at retail rate for date: " + date);
            return 0.0;
        }
    }

    /**
     * Alternative optimized method using a two-step approach. Delegates to the
     * facade method for better encapsulation.
     *
     * @param date The date for which to calculate stock value
     * @param dept The department for which to calculate stock value
     * @return The total stock value at retail rate, or 0.0 if calculation fails
     */
    private double calculateStockValueAtRetailRateTwoStep(Date date, Department dept) {
        try {
            Long departmentId = (dept != null) ? dept.getId() : null;
            return stockHistoryFacade.calculateStockValueAtRetailRateTwoStep(date, departmentId);
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating stock value (two-step method): " + date);
            return 0.0;
        }
    }

    /**
     * OPTIMIZED: Calculates the stock value at cost rate for a given date and
     * department. Delegates to the facade method for better encapsulation.
     *
     * @param date The date for which to calculate stock value
     * @param dept The department for which to calculate stock value
     * @return The total stock value at cost rate, or 0.0 if calculation fails
     */
    private double calculateStockValueAtCostRateOptimized(Date date, Department dept) {
        long stockCalcStartTime = System.currentTimeMillis();

        System.out.println("🔍 === STOCK CALCULATION (COST RATE) START ===");
        System.out.println("📅 Date param: " + date);
        System.out.println("🏥 Department: " + (dept != null ? dept.getName() : "null"));
        System.out.println("🆔 Department ID: " + (dept != null ? dept.getId() : "null"));

        try {
            Long departmentId = (dept != null) ? dept.getId() : null;
            System.out.println("🔄 Calling StockHistoryFacade.calculateStockValueAtCostRateOptimized...");

            long facadeStartTime = System.currentTimeMillis();
            double result = stockHistoryFacade.calculateStockValueAtCostRateOptimized(date, departmentId);
            long facadeDuration = System.currentTimeMillis() - facadeStartTime;

            long totalDuration = System.currentTimeMillis() - stockCalcStartTime;

            System.out.println("✅ FACADE COMPLETED: " + facadeDuration + "ms");
            System.out.println("💰 Stock Value Result (Cost): " + result);
            System.out.println("⏱️ Total Stock Calc Duration: " + totalDuration + "ms");
            System.out.println("🔍 === STOCK CALCULATION (COST RATE) END ===");

            return result;
        } catch (Exception e) {
            long errorDuration = System.currentTimeMillis() - stockCalcStartTime;
            System.err.println("🚨 === STOCK CALCULATION (COST RATE) ERROR ===");
            System.err.println("⏱️ Time before error: " + errorDuration + "ms");
            System.err.println("❌ Error message: " + e.getMessage());
            System.err.println("📍 Error type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.err.println("🚨 === STOCK CALCULATION (COST RATE) ERROR END ===");

            JsfUtil.addErrorMessage(e, "Error calculating stock value at cost rate for date: " + date);
            return 0.0;
        }
    }

    /**
     * Calculates the stock value at purchase rate for a given date and department.
     * Delegates to the facade method. Uses PURCAHSERATE with no fallback.
     *
     * @param date The date for which to calculate stock value
     * @param dept The department for which to calculate stock value
     * @return The total stock value at purchase rate, or 0.0 if calculation fails
     */
    private double calculateStockValueAtPurchaseRateOptimized(Date date, Department dept) {
        try {
            Long departmentId = (dept != null) ? dept.getId() : null;
            return stockHistoryFacade.calculateStockValueAtPurchaseRateOptimized(date, departmentId);
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating stock value at purchase rate for date: " + date);
            return 0.0;
        }
    }

    /**
     * Downloads the daily stock report as an Excel file
     */
    public void downloadExcel() {
        if (dailyStockBalanceReport == null || fromDate == null || department == null) {
            JsfUtil.addErrorMessage("Please generate the report first before downloading");
            return;
        }

        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String fileName = "Daily_Stock_Report_" + dateFormat.format(fromDate) + ".xlsx";

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

    /**
     * Creates Excel workbook for the daily stock report.
     * The layout mirrors the on-screen display: coloured section headers,
     * all columns for each section, and red highlighting on closing stock
     * cells when a mismatch is detected.
     */
    private Workbook createExcelReport() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Daily Stock Report");

        short numFmt = workbook.createDataFormat().getFormat("#,##0.00");

        // ── Title style ───────────────────────────────────────────────────────
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        // ── Plain number style ────────────────────────────────────────────────
        CellStyle numStyle = workbook.createCellStyle();
        numStyle.setDataFormat(numFmt);
        numStyle.setAlignment(HorizontalAlignment.RIGHT);

        // ── Helper: section-header style (coloured background, white bold text) ──
        // colours: Opening/Closing=blue(0x2196F3), Sales=green(0x28a745),
        //          Purchase=orange(0xFF8C00), Transfer=cyan(0x17A2B8),
        //          Adjustment=red(0xDC3545)

        // ── Section total rows are created inline per section with their own colour.

        // ── Mismatch cell style (red background, dark-red bold text) ─────────
        XSSFCellStyle mismatchStyle = (XSSFCellStyle) workbook.createCellStyle();
        mismatchStyle.setDataFormat(numFmt);
        mismatchStyle.setAlignment(HorizontalAlignment.RIGHT);
        mismatchStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        mismatchStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xDD,(byte)0xDD}, null));
        Font mismatchFont = workbook.createFont();
        mismatchFont.setBold(true);
        mismatchFont.setColor(IndexedColors.DARK_RED.getIndex());
        mismatchStyle.setFont(mismatchFont);

        int rowNum = 0;

        // ── Report title ──────────────────────────────────────────────────────
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Daily Stock Value Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));

        rowNum++; // blank row

        // ── Date / Department info ─────────────────────────────────────────────
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Date:");
        dateRow.createCell(1).setCellValue(dateFormat.format(fromDate));

        Row deptRow = sheet.createRow(rowNum++);
        deptRow.createCell(0).setCellValue("Department:");
        deptRow.createCell(1).setCellValue(department != null ? department.getName() : "");

        rowNum++; // blank row

        // ═════════════════════════════════════════════════════════════════════
        // OPENING STOCK  (blue header, 3 value columns)
        // ═════════════════════════════════════════════════════════════════════
        byte[] blue = {(byte)0x0D,(byte)0x6E,(byte)0xFD};
        rowNum = writeSectionHeader(sheet, workbook, rowNum, "Opening Stock",
                new String[]{"Description","","","","","Cost Rate","Purchase Rate","Retail Rate"}, blue);
        Row openingRow = sheet.createRow(rowNum++);
        openingRow.createCell(0).setCellValue("Opening Stock Value");
        setCellNum(openingRow, 5, dailyStockBalanceReport.getOpeningStockValueAtCostRate(), numStyle);
        setCellNum(openingRow, 6, dailyStockBalanceReport.getOpeningStockValueAtPurchaseRate(), numStyle);
        setCellNum(openingRow, 7, dailyStockBalanceReport.getOpeningStockValue(), numStyle);
        rowNum++; // blank row

        // ═════════════════════════════════════════════════════════════════════
        // SALES  (green header, 8 columns)
        // ═════════════════════════════════════════════════════════════════════
        byte[] green = {(byte)0x19,(byte)0x87,(byte)0x54};
        String[] salesCols = {"Sale Type","Gross Value","Discount","Service Charge","Net Value",
                              "Stock Value (Cost)","Stock Value (Purchase)","Stock Value (Retail)"};
        rowNum = writeSectionHeader(sheet, workbook, rowNum, "Sales Transactions", salesCols, green);

        if (dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle() != null
                && dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle().getRows() != null) {
            for (var sale : dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle().getRows()) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(sale.getRowType());
                setCellNum(r, 1, sale.getGrossTotal(), numStyle);
                setCellNum(r, 2, sale.getDiscount(), numStyle);
                setCellNum(r, 3, sale.getServiceCharge(), numStyle);
                setCellNum(r, 4, sale.getNetTotal(), numStyle);
                setCellNum(r, 5, sale.getValueOfStocksAtCostRate().doubleValue(), numStyle);
                setCellNum(r, 6, sale.getValueOfStocksAtPurchaseRate().doubleValue(), numStyle);
                setCellNum(r, 7, sale.getValueOfStocksAtRetailSaleRate().doubleValue(), numStyle);
            }
            var s = dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle().getSummaryRow();
            CellStyle totalGreen = makeSolidStyle(workbook, green, numFmt, true, false);
            if (s != null) {
                rowNum = writeTotalRow(sheet, rowNum, "Total Sales",
                        new double[]{s.getGrossTotal(), s.getDiscount(), s.getServiceCharge(), s.getNetTotal(),
                                     s.getValueOfStocksAtCostRate().doubleValue(),
                                     s.getValueOfStocksAtPurchaseRate().doubleValue(),
                                     s.getValueOfStocksAtRetailSaleRate().doubleValue()},
                        totalGreen);
            }
        }
        rowNum++; // blank row

        // ═════════════════════════════════════════════════════════════════════
        // PURCHASES  (orange header, 8 columns)
        // ═════════════════════════════════════════════════════════════════════
        byte[] orange = {(byte)0xFF,(byte)0x8C,(byte)0x00};
        String[] purchaseCols = {"Purchase Type","Gross Value","","","Net Value",
                                 "Stock Value (Cost)","Stock Value (Purchase)","Stock Value (Retail)"};
        rowNum = writeSectionHeader(sheet, workbook, rowNum, "Purchase Transactions", purchaseCols, orange);

        if (dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle() != null
                && dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle().getRows() != null) {
            for (var p : dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle().getRows()) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(p.getRowType());
                setCellNum(r, 1, p.getGrossTotal(), numStyle);
                setCellNum(r, 4, p.getNetTotal(), numStyle);
                setCellNum(r, 5, p.getValueOfStocksAtCostRate().doubleValue(), numStyle);
                setCellNum(r, 6, p.getValueOfStocksAtPurchaseRate().doubleValue(), numStyle);
                setCellNum(r, 7, p.getValueOfStocksAtRetailSaleRate().doubleValue(), numStyle);
            }
            var s = dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle().getSummaryRow();
            CellStyle totalOrange = makeSolidStyle(workbook, orange, numFmt, true, false);
            if (s != null) {
                rowNum = writeTotalRow(sheet, rowNum, "Total Purchases",
                        new double[]{s.getGrossTotal(), 0, 0, s.getNetTotal(),
                                     s.getValueOfStocksAtCostRate().doubleValue(),
                                     s.getValueOfStocksAtPurchaseRate().doubleValue(),
                                     s.getValueOfStocksAtRetailSaleRate().doubleValue()},
                        totalOrange);
            }
        }
        rowNum++; // blank row

        // ═════════════════════════════════════════════════════════════════════
        // TRANSFERS  (cyan header, 8 columns)
        // ═════════════════════════════════════════════════════════════════════
        byte[] cyan = {(byte)0x0D,(byte)0xA2,(byte)0xB8};
        String[] transferCols = {"Transfer Type","Gross Value","","","Net Value",
                                 "Stock Value (Cost)","Stock Value (Purchase)","Stock Value (Retail)"};
        rowNum = writeSectionHeader(sheet, workbook, rowNum, "Transfer Transactions", transferCols, cyan);

        if (dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle() != null
                && dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle().getRows() != null) {
            for (var t : dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle().getRows()) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(t.getRowType());
                setCellNum(r, 1, t.getGrossTotal(), numStyle);
                setCellNum(r, 4, t.getNetTotal(), numStyle);
                setCellNum(r, 5, t.getValueOfStocksAtCostRate().doubleValue(), numStyle);
                setCellNum(r, 6, t.getValueOfStocksAtPurchaseRate().doubleValue(), numStyle);
                setCellNum(r, 7, t.getValueOfStocksAtRetailSaleRate().doubleValue(), numStyle);
            }
            var s = dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle().getSummaryRow();
            CellStyle totalCyan = makeSolidStyle(workbook, cyan, numFmt, true, false);
            if (s != null) {
                rowNum = writeTotalRow(sheet, rowNum, "Total Transfers",
                        new double[]{s.getGrossTotal(), 0, 0, s.getNetTotal(),
                                     s.getValueOfStocksAtCostRate().doubleValue(),
                                     s.getValueOfStocksAtPurchaseRate().doubleValue(),
                                     s.getValueOfStocksAtRetailSaleRate().doubleValue()},
                        totalCyan);
            }
        }
        rowNum++; // blank row

        // ═════════════════════════════════════════════════════════════════════
        // ADJUSTMENTS  (red header, 8 columns)
        // ═════════════════════════════════════════════════════════════════════
        byte[] red = {(byte)0xDC,(byte)0x35,(byte)0x45};
        String[] adjustCols = {"Adjustment Type","Gross Value","","","Net Value",
                               "Stock Value (Cost)","Stock Value (Purchase)","Stock Value (Retail)"};
        rowNum = writeSectionHeader(sheet, workbook, rowNum, "Adjustment Transactions", adjustCols, red);

        if (dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle() != null
                && dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle().getRows() != null) {
            for (var a : dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle().getRows()) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(a.getRowType());
                setCellNum(r, 1, a.getGrossTotal(), numStyle);
                setCellNum(r, 4, a.getNetTotal(), numStyle);
                setCellNum(r, 5, a.getValueOfStocksAtCostRate().doubleValue(), numStyle);
                setCellNum(r, 6, a.getValueOfStocksAtPurchaseRate().doubleValue(), numStyle);
                setCellNum(r, 7, a.getValueOfStocksAtRetailSaleRate().doubleValue(), numStyle);
            }
            var s = dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle().getSummaryRow();
            CellStyle totalRed = makeSolidStyle(workbook, red, numFmt, true, false);
            if (s != null) {
                rowNum = writeTotalRow(sheet, rowNum, "Total Adjustments",
                        new double[]{s.getGrossTotal(), 0, 0, s.getNetTotal(),
                                     s.getValueOfStocksAtCostRate().doubleValue(),
                                     s.getValueOfStocksAtPurchaseRate().doubleValue(),
                                     s.getValueOfStocksAtRetailSaleRate().doubleValue()},
                        totalRed);
            }
        }
        rowNum++; // blank row

        // ═════════════════════════════════════════════════════════════════════
        // CLOSING STOCK  (blue header, 3 value columns, mismatch highlighting)
        // ═════════════════════════════════════════════════════════════════════
        rowNum = writeSectionHeader(sheet, workbook, rowNum, "Closing Stock",
                new String[]{"Description","","","","","Cost Rate","Purchase Rate","Retail Rate"}, blue);
        Row closingRow = sheet.createRow(rowNum++);
        closingRow.createCell(0).setCellValue("Closing Stock Value");
        setCellNum(closingRow, 5, dailyStockBalanceReport.getClosingStockValueAtCostRate(),
                dailyStockBalanceReport.isClosingStockCostRateMismatch() ? mismatchStyle : numStyle);
        setCellNum(closingRow, 6, dailyStockBalanceReport.getClosingStockValueAtPurchaseRate(),
                dailyStockBalanceReport.isClosingStockPurchaseRateMismatch() ? mismatchStyle : numStyle);
        setCellNum(closingRow, 7, dailyStockBalanceReport.getClosingStockValue(),
                dailyStockBalanceReport.isClosingStockRetailRateMismatch() ? mismatchStyle : numStyle);

        // ── Auto-size all 8 columns ───────────────────────────────────────────
        for (int c = 0; c < 8; c++) {
            sheet.autoSizeColumn(c);
        }

        return workbook;
    }

    /** Writes a coloured section-header row followed by a column-header row.
     *  Returns the next available row index. */
    private int writeSectionHeader(Sheet sheet, XSSFWorkbook wb, int rowNum,
                                   String sectionTitle, String[] colHeaders, byte[] rgb) {
        CellStyle sectionStyle = makeSolidStyle(wb, rgb, (short) 0, true, true);
        Row sectionRow = sheet.createRow(rowNum++);
        Cell sectionCell = sectionRow.createCell(0);
        sectionCell.setCellValue(sectionTitle);
        sectionCell.setCellStyle(sectionStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));

        // lighter shade for column-header row
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

    /** Writes a bold total row across columns 0–7. Returns next row index. */
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

    /** Sets a numeric cell at the given column index with the provided style. */
    private void setCellNum(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    /** Creates a solid-fill cell style with the given RGB background. */
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

    /** Returns a lightened version of an RGB colour for sub-header rows. */
    private byte[] lighten(byte[] rgb) {
        byte[] out = new byte[3];
        for (int i = 0; i < 3; i++) {
            int v = (rgb[i] & 0xFF);
            out[i] = (byte) Math.min(255, v + 80);
        }
        return out;
    }

    /**
     * Prepares the data for printing and navigates to print page
     */
    public String navigateToPrintPage() {
        if (dailyStockBalanceReport == null || fromDate == null || department == null) {
            JsfUtil.addErrorMessage("Please generate the report first before printing");
            return null;
        }
        return "/pharmacy/reports/summary_reports/daily_stock_values_report_print?faces-redirect=true";
    }

    /**
     * Clears all filters and resets the form
     */
    public void clearFilters() {
        fromDate = new Date();
        institution = null;
        site = null;
        department = null;
        dailyStockBalanceReport = null;
        JsfUtil.addSuccessMessage("Filters cleared successfully");
    }

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = new Date();
        }
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

    public DailyStockBalanceReport getDailyStockBalanceReport() {
        if (dailyStockBalanceReport == null) {
            dailyStockBalanceReport = new DailyStockBalanceReport();
        }
        return dailyStockBalanceReport;
    }

    public void setDailyStockBalanceReport(DailyStockBalanceReport dailyStockBalanceReport) {
        this.dailyStockBalanceReport = dailyStockBalanceReport;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public DepartmentController getDepartmentController() {
        return departmentController;
    }
    // </editor-fold>
}

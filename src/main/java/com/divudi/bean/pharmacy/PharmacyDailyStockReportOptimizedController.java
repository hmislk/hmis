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
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
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
        System.out.println("==========================================");
        System.out.println("=== processDailyStockBalanceReportOptimized START ===");
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

        dailyStockBalanceReport = new DailyStockBalanceReport();
        dailyStockBalanceReport.setDate(fromDate);
        dailyStockBalanceReport.setDepartment(department);
        // Calculate Opening Stock Value at Retail Rate using optimized method

        // Calculate Opening Stock Value at Retail Rate using optimized method
        System.out.println(">>> Calculating OPENING stock for date: " + fromDate);
        double openingStockValueAtRetailRate = calculateStockValueAtRetailRateOptimized(fromDate, department);
        System.out.println(">>> Opening stock value returned: " + openingStockValueAtRetailRate);
        dailyStockBalanceReport.setOpeningStockValue(openingStockValueAtRetailRate);

        // Calculate toDate as fromDate + 1 day
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(Calendar.DATE, 1);
        toDate = cal.getTime();

        Date startOfTheDay = CommonFunctions.getStartOfDay(fromDate);
        Date endOfTheDay = CommonFunctions.getEndOfDay(fromDate);

        // These service calls are kept the same as original
        PharmacyBundle saleBundle = pharmacyService.fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionType(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacySalesByAdmissionTypeAndDiscountSchemeBundle(saleBundle);

        PharmacyBundle purchaseBundle = pharmacyService.fetchPharmacyStockPurchaseValueByBillTypeDto(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyPurchaseByBillTypeBundle(purchaseBundle);

        PharmacyBundle transferBundle = pharmacyService.fetchPharmacyTransferValueByBillTypeDto(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyTransferByBillTypeBundle(transferBundle);

        PharmacyBundle adjustmentBundle = pharmacyService.fetchPharmacyAdjustmentValueByBillTypeDto(
                startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyAdjustmentsByBillTypeBundle(adjustmentBundle);
        // Calculate Closing Stock Value at Retail Rate using optimized method

        // Calculate Closing Stock Value at Retail Rate using optimized method
        System.out.println(">>> Calculating CLOSING stock for date: " + toDate);
        double closingStockValueAtRetailRate = calculateStockValueAtRetailRateOptimized(toDate, department);
        System.out.println(">>> Closing stock value returned: " + closingStockValueAtRetailRate);
        dailyStockBalanceReport.setClosingStockValue(closingStockValueAtRetailRate);

        System.out.println("=== processDailyStockBalanceReportOptimized END ===");
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
        System.out.println("--- calculateStockValueAtRetailRateOptimized (Controller) ---");
        System.out.println("Date param: " + date);

        try {
            Long departmentId = (dept != null) ? dept.getId() : null;
            System.out.println("Extracted departmentId: " + departmentId);
            System.out.println("Calling facade method...");

            double result = stockHistoryFacade.calculateStockValueAtRetailRateOptimized(date, departmentId);

            System.out.println("Facade returned: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("!!! EXCEPTION in Controller calculateStockValueAtRetailRateOptimized !!!");
            e.printStackTrace();
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
     * Creates Excel workbook for the daily stock report
     */
    private Workbook createExcelReport() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Daily Stock Report");

        // Create styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(IndexedColors.LIGHT_GREY.getIndex());
        totalStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Daily Stock Value Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Empty row
        rowNum++;

        // Date and Department info
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Date:");
        dateRow.createCell(1).setCellValue(dateFormat.format(fromDate));

        Row deptRow = sheet.createRow(rowNum++);
        deptRow.createCell(0).setCellValue("Department:");
        deptRow.createCell(1).setCellValue(department != null ? department.getName() : "");

        // Empty row
        rowNum++;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        Cell descHeader = headerRow.createCell(0);
        descHeader.setCellValue("Description");
        descHeader.setCellStyle(headerStyle);

        Cell valueHeader = headerRow.createCell(1);
        valueHeader.setCellValue("Value");
        valueHeader.setCellStyle(headerStyle);

        // Opening Stock
        Row openingRow = sheet.createRow(rowNum++);
        openingRow.createCell(0).setCellValue("Opening Stock");
        Cell openingCell = openingRow.createCell(1);
        openingCell.setCellValue(dailyStockBalanceReport.getOpeningStockValue());
        openingCell.setCellStyle(numberStyle);

        // Sales
        if (dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle() != null
            && dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle().getRows() != null) {
            for (var sale : dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle().getRows()) {
                Row saleRow = sheet.createRow(rowNum++);
                saleRow.createCell(0).setCellValue(sale.getRowType());
                Cell saleCell = saleRow.createCell(1);
                saleCell.setCellValue(sale.getNetTotal());
                saleCell.setCellStyle(numberStyle);
            }

            // Total Sales
            Row totalSalesRow = sheet.createRow(rowNum++);
            Cell totalSalesDesc = totalSalesRow.createCell(0);
            totalSalesDesc.setCellValue("Total Sales");
            totalSalesDesc.setCellStyle(totalStyle);

            Cell totalSalesCell = totalSalesRow.createCell(1);
            totalSalesCell.setCellValue(dailyStockBalanceReport.getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle().getSummaryRow().getNetTotal());
            totalSalesCell.setCellStyle(totalStyle);
        }

        // Purchases
        if (dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle() != null
            && dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle().getRows() != null) {
            for (var purchase : dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle().getRows()) {
                Row purchaseRow = sheet.createRow(rowNum++);
                purchaseRow.createCell(0).setCellValue(purchase.getRowType());
                Cell purchaseCell = purchaseRow.createCell(1);
                purchaseCell.setCellValue(purchase.getValueOfStocksAtRetailSaleRate());
                purchaseCell.setCellStyle(numberStyle);
            }

            // Total Purchases
            Row totalPurchasesRow = sheet.createRow(rowNum++);
            Cell totalPurchasesDesc = totalPurchasesRow.createCell(0);
            totalPurchasesDesc.setCellValue("Total Purchases");
            totalPurchasesDesc.setCellStyle(totalStyle);

            Cell totalPurchasesCell = totalPurchasesRow.createCell(1);
            totalPurchasesCell.setCellValue(dailyStockBalanceReport.getPharmacyPurchaseByBillTypeBundle().getSummaryRow().getValueOfStocksAtRetailSaleRate());
            totalPurchasesCell.setCellStyle(totalStyle);
        }

        // Transfers
        if (dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle() != null
            && dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle().getRows() != null) {
            for (var transfer : dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle().getRows()) {
                Row transferRow = sheet.createRow(rowNum++);
                transferRow.createCell(0).setCellValue(transfer.getRowType());
                Cell transferCell = transferRow.createCell(1);
                transferCell.setCellValue(transfer.getValueOfStocksAtRetailSaleRate());
                transferCell.setCellStyle(numberStyle);
            }

            // Total Transfers
            Row totalTransfersRow = sheet.createRow(rowNum++);
            Cell totalTransfersDesc = totalTransfersRow.createCell(0);
            totalTransfersDesc.setCellValue("Total Transfers");
            totalTransfersDesc.setCellStyle(totalStyle);

            Cell totalTransfersCell = totalTransfersRow.createCell(1);
            totalTransfersCell.setCellValue(dailyStockBalanceReport.getPharmacyTransferByBillTypeBundle().getSummaryRow().getValueOfStocksAtRetailSaleRate());
            totalTransfersCell.setCellStyle(totalStyle);
        }

        // Adjustments
        if (dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle() != null
            && dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle().getRows() != null) {
            for (var adjustment : dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle().getRows()) {
                Row adjustmentRow = sheet.createRow(rowNum++);
                adjustmentRow.createCell(0).setCellValue(adjustment.getRowType());
                Cell adjustmentCell = adjustmentRow.createCell(1);
                adjustmentCell.setCellValue(adjustment.getValueOfStocksAtRetailSaleRate());
                adjustmentCell.setCellStyle(numberStyle);
            }

            // Total Adjustments
            Row totalAdjustmentsRow = sheet.createRow(rowNum++);
            Cell totalAdjustmentsDesc = totalAdjustmentsRow.createCell(0);
            totalAdjustmentsDesc.setCellValue("Total Adjustments");
            totalAdjustmentsDesc.setCellStyle(totalStyle);

            Cell totalAdjustmentsCell = totalAdjustmentsRow.createCell(1);
            totalAdjustmentsCell.setCellValue(dailyStockBalanceReport.getPharmacyAdjustmentsByBillTypeBundle().getSummaryRow().getValueOfStocksAtRetailSaleRate());
            totalAdjustmentsCell.setCellStyle(totalStyle);
        }

        // Closing Stock
        Row closingRow = sheet.createRow(rowNum++);
        closingRow.createCell(0).setCellValue("Closing Stock");
        Cell closingCell = closingRow.createCell(1);
        closingCell.setCellValue(dailyStockBalanceReport.getClosingStockValue());
        closingCell.setCellStyle(numberStyle);

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        return workbook;
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

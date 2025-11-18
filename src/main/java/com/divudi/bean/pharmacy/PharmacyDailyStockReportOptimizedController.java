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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Optimized controller for Daily Stock Balance Report
 * This is a performance-optimized version of the report that uses simplified queries
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

    /**
     * Optimized version of the daily stock balance report processing
     * Uses simplified queries with better performance
     */
    public void processDailyStockBalanceReportOptimized() {
        System.out.println("==========================================");
        System.out.println("=== processDailyStockBalanceReportOptimized START ===");
        System.out.println("From Date: " + fromDate);
        System.out.println("Department: " + department);
        System.out.println("Department ID: " + (department != null ? department.getId() : "null"));
        System.out.println("Department Name: " + (department != null ? department.getName() : "null"));

        if (department == null) {
            System.out.println("ERROR: Department is null");
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        if (fromDate == null) {
            System.out.println("ERROR: fromDate is null");
            JsfUtil.addErrorMessage("Please select a date");
            return;
        }

        dailyStockBalanceReport = new DailyStockBalanceReport();
        dailyStockBalanceReport.setDate(fromDate);
        dailyStockBalanceReport.setDepartment(department);

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
        System.out.println(">>> Calculating CLOSING stock for date: " + toDate);
        double closingStockValueAtRetailRate = calculateStockValueAtRetailRateOptimized(toDate, department);
        System.out.println(">>> Closing stock value returned: " + closingStockValueAtRetailRate);
        dailyStockBalanceReport.setClosingStockValue(closingStockValueAtRetailRate);

        System.out.println("=== processDailyStockBalanceReportOptimized END ===");
        System.out.println("==========================================");
    }

    /**
     * OPTIMIZED: Calculates the stock value at retail rate for a given date and department.
     * Delegates to the facade method for better encapsulation.
     *
     * @param date The date for which to calculate stock value
     * @param dept The department for which to calculate stock value
     * @return The total stock value at retail rate, or 0.0 if calculation fails
     */
    private double calculateStockValueAtRetailRateOptimized(Date date, Department dept) {
        System.out.println("--- calculateStockValueAtRetailRateOptimized (Controller) ---");
        System.out.println("Date param: " + date);
        System.out.println("Department param: " + dept);

        try {
            Long departmentId = (dept != null) ? dept.getId() : null;
            System.out.println("Extracted departmentId: " + departmentId);
            System.out.println("Calling facade method...");

            double result = stockHistoryFacade.calculateStockValueAtRetailRateOptimized(date, departmentId);

            System.out.println("Facade returned: " + result);
            System.out.println("--- End calculateStockValueAtRetailRateOptimized (Controller) ---");
            return result;
        } catch (Exception e) {
            System.err.println("!!! EXCEPTION in Controller calculateStockValueAtRetailRateOptimized !!!");
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
            JsfUtil.addErrorMessage(e, "Error calculating stock value at retail rate for date: " + date);
            return 0.0;
        }
    }

    /**
     * Alternative optimized method using a two-step approach.
     * Delegates to the facade method for better encapsulation.
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

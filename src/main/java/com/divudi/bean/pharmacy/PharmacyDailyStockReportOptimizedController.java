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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

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
        double openingStockValueAtRetailRate = calculateStockValueAtRetailRateOptimized(fromDate, department);
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
        double closingStockValueAtRetailRate = calculateStockValueAtRetailRateOptimized(toDate, department);
        dailyStockBalanceReport.setClosingStockValue(closingStockValueAtRetailRate);
    }

    /**
     * OPTIMIZED: Calculates the stock value at retail rate for a given date and department.
     * This method uses a simplified native SQL query for better performance.
     *
     * Performance improvements:
     * 1. Uses native SQL instead of complex JPQL with nested subqueries
     * 2. Leverages database indexing more efficiently
     * 3. Reduces the number of subquery levels
     *
     * @param date The date for which to calculate stock value
     * @param dept The department for which to calculate stock value
     * @return The total stock value at retail rate, or 0.0 if calculation fails
     */
    private double calculateStockValueAtRetailRateOptimized(Date date, Department dept) {
        try {
            // Use native SQL for better performance
            // This query finds the latest stock history record for each item batch before the given date
            // and calculates the total retail value
            String sql =
                "SELECT COALESCE(SUM(latest_stock.stock_qty * latest_stock.retail_rate), 0.0) AS total_value " +
                "FROM ( " +
                "    SELECT  " +
                "        sh.stock_qty, " +
                "        COALESCE(ib.retailsale_rate, 0.0) AS retail_rate " +
                "    FROM stock_history sh " +
                "    INNER JOIN ( " +
                "        SELECT  " +
                "            department_id, " +
                "            item_batch_id, " +
                "            MAX(id) AS max_id " +
                "        FROM stock_history " +
                "        WHERE retired = 0 " +
                "        AND created_at < ? " +
                (dept != null ? "        AND department_id = ? " : "") +
                "        GROUP BY department_id, item_batch_id " +
                "    ) AS latest ON sh.id = latest.max_id " +
                "    INNER JOIN item_batch ib ON sh.item_batch_id = ib.id " +
                "    WHERE sh.retired = 0 " +
                "    AND sh.stock_qty > 0 " +
                ") AS latest_stock";

            // Execute the native query
            javax.persistence.Query query = stockHistoryFacade.getEntityManager().createNativeQuery(sql);
            query.setParameter(1, date, TemporalType.TIMESTAMP);
            if (dept != null) {
                query.setParameter(2, dept.getId());
            }

            Object result = query.getSingleResult();
            if (result != null) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            }
            return 0.0;

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating stock value at retail rate for date: " + date);
            return 0.0;
        }
    }

    /**
     * Alternative optimized method using a two-step approach
     * This can be even faster for very large datasets as it breaks the complex query into simpler parts
     */
    private double calculateStockValueAtRetailRateTwoStep(Date date, Department dept) {
        try {
            // Step 1: Get the latest stock history IDs using a simplified query
            String findLatestIdsSql =
                "SELECT MAX(id) AS max_id " +
                "FROM stock_history " +
                "WHERE retired = 0 " +
                "AND created_at < ? " +
                (dept != null ? "AND department_id = ? " : "") +
                "GROUP BY department_id, item_batch_id";

            javax.persistence.Query idsQuery = stockHistoryFacade.getEntityManager().createNativeQuery(findLatestIdsSql);
            idsQuery.setParameter(1, date, TemporalType.TIMESTAMP);
            if (dept != null) {
                idsQuery.setParameter(2, dept.getId());
            }

            List<Object> latestIds = idsQuery.getResultList();

            if (latestIds == null || latestIds.isEmpty()) {
                return 0.0;
            }

            // Step 2: Calculate the sum of stock values for the latest records
            String calculateValueSql =
                "SELECT COALESCE(SUM(sh.stock_qty * COALESCE(ib.retailsale_rate, 0.0)), 0.0) " +
                "FROM stock_history sh " +
                "INNER JOIN item_batch ib ON sh.item_batch_id = ib.id " +
                "WHERE sh.id IN (:ids) " +
                "AND sh.retired = 0 " +
                "AND sh.stock_qty > 0";

            javax.persistence.Query valueQuery = stockHistoryFacade.getEntityManager().createNativeQuery(calculateValueSql);
            valueQuery.setParameter("ids", latestIds);

            Object result = valueQuery.getSingleResult();
            if (result != null) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            }
            return 0.0;

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

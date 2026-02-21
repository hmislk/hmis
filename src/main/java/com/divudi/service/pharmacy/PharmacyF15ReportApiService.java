package com.divudi.service.pharmacy;

import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.data.dto.PharmacyF15ReportDTO;
import com.divudi.core.data.dto.PharmacyF15ReportDTO.BalanceCheckDTO;
import com.divudi.core.data.dto.PharmacyF15ReportDTO.BundleDTO;
import com.divudi.core.data.dto.PharmacyF15ReportDTO.BundleRowDTO;
import com.divudi.core.data.dto.PharmacyF15ReportDTO.StockValueDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.PharmacyService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Service that generates the AF15 Daily Stock Values Report data for the API.
 *
 * Uses the same PharmacyService and StockHistoryFacade methods as the JSF
 * controller (PharmacyDailyStockReportOptimizedController), so any fix to
 * those service methods automatically updates both the UI and the API.
 *
 * Balance formula (verified on production data 2026-02-18, dept 485):
 *   expectedClosing = opening
 *                   + sales.totals.stockValueAtRetailRate       (negative: stock out)
 *                   + purchases.totals.stockValueAtRetailRate   (positive: stock in)
 *                   + transfers.totals.stockValueAtRetailRate   (signed per type)
 *                   + adjustments.totals.stockValueAtRetailRate (signed per type)
 *   discrepancy = actualClosing - expectedClosing
 */
@Stateless
public class PharmacyF15ReportApiService {

    @EJB
    private PharmacyService pharmacyService;

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    private static final double BALANCE_TOLERANCE = 1.0;

    /**
     * Generates the full F15 report for a given date and department.
     *
     * @param date         The report date (time portion ignored; uses full day)
     * @param departmentId The department ID (institution table, e.g. 485 = Main Pharmacy)
     * @return Fully populated F15 report DTO, or null if department not found
     */
    public PharmacyF15ReportDTO generateReport(Date date, Long departmentId) {
        Department department = departmentFacade.find(departmentId);
        if (department == null) {
            return null;
        }

        Date startOfDay = CommonFunctions.getStartOfDay(date);
        Date endOfDay = CommonFunctions.getEndOfDay(date);

        // Closing stock uses start of next day (same logic as the JSF controller)
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfDay);
        cal.add(Calendar.DATE, 1);
        Date nextDayStart = cal.getTime();

        // Opening stock (same as Step 2 in JSF controller)
        double openingRetail = stockHistoryFacade.calculateStockValueAtRetailRateOptimized(startOfDay, departmentId);
        double openingCost = stockHistoryFacade.calculateStockValueAtCostRateOptimized(startOfDay, departmentId);

        // Fetch transaction bundles (same as Steps 4-7 in JSF controller)
        PharmacyBundle salesBundle = pharmacyService
                .fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionTypeDto(
                        startOfDay, endOfDay, null, null, department, null, null, null);

        PharmacyBundle purchasesBundle = pharmacyService
                .fetchPharmacyStockPurchaseValueByBillTypeDtoCompleted(
                        startOfDay, endOfDay, null, null, department, null, null, null);

        PharmacyBundle transfersBundle = pharmacyService
                .fetchPharmacyTransferValueByBillTypeDto(
                        startOfDay, endOfDay, null, null, department, null, null, null);

        PharmacyBundle adjustmentsBundle = pharmacyService
                .fetchPharmacyAdjustmentValueByBillTypeDto(
                        startOfDay, endOfDay, null, null, department, null, null, null);

        // Pre-bill stock movements (informational section)
        // PRE_TO_SETTLE_AT_CASHIER + CANCELLED_PRE: stock that moved in the period
        // but whose financial settlement may fall on a different day.
        PharmacyBundle preBillBundle = pharmacyService
                .fetchPharmacyF15StockMovementBundleDto(
                        startOfDay, endOfDay, null, null, department, null, null, null);

        // Closing stock (same as Step 8 in JSF controller)
        double closingRetail = stockHistoryFacade.calculateStockValueAtRetailRateOptimized(nextDayStart, departmentId);
        double closingCost = stockHistoryFacade.calculateStockValueAtCostRateOptimized(nextDayStart, departmentId);

        // Build DTO
        PharmacyF15ReportDTO dto = new PharmacyF15ReportDTO();
        dto.setDate(new SimpleDateFormat("yyyy-MM-dd").format(date));
        dto.setDepartmentId(departmentId);
        dto.setDepartmentName(department.getName());
        dto.setOpeningStock(new StockValueDTO(openingRetail, openingCost));
        dto.setSales(buildBundleDTO(salesBundle));
        dto.setPurchases(buildBundleDTO(purchasesBundle));
        dto.setTransfers(buildBundleDTO(transfersBundle));
        dto.setAdjustments(buildBundleDTO(adjustmentsBundle));
        dto.setClosingStock(new StockValueDTO(closingRetail, closingCost));
        dto.setBalanceCheck(buildBalanceCheck(
                openingRetail, closingRetail,
                salesBundle, purchasesBundle, transfersBundle, adjustmentsBundle));
        dto.setPreBillStockMovements(buildBundleDTO(preBillBundle));

        return dto;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private BundleDTO buildBundleDTO(PharmacyBundle bundle) {
        BundleDTO bundleDTO = new BundleDTO();

        if (bundle == null) {
            bundleDTO.setRows(new ArrayList<>());
            bundleDTO.setTotals(new BundleRowDTO());
            return bundleDTO;
        }

        List<BundleRowDTO> rows = new ArrayList<>();
        if (bundle.getRows() != null) {
            for (PharmacyRow row : bundle.getRows()) {
                rows.add(buildRowDTO(row));
            }
        }
        bundleDTO.setRows(rows);

        if (bundle.getSummaryRow() != null) {
            bundleDTO.setTotals(buildRowDTO(bundle.getSummaryRow()));
        } else {
            bundleDTO.setTotals(new BundleRowDTO());
        }

        return bundleDTO;
    }

    private BundleRowDTO buildRowDTO(PharmacyRow row) {
        BundleRowDTO dto = new BundleRowDTO();
        dto.setType(row.getRowType());
        dto.setGrossTotal(row.getGrossTotal());
        dto.setDiscount(row.getDiscount());
        dto.setServiceCharge(row.getServiceCharge());
        dto.setNetTotal(row.getNetTotal());
        dto.setStockValueAtCostRate(
                row.getValueOfStocksAtCostRate() != null
                        ? row.getValueOfStocksAtCostRate().doubleValue()
                        : 0.0);
        dto.setStockValueAtRetailRate(
                row.getValueOfStocksAtRetailSaleRate() != null
                        ? row.getValueOfStocksAtRetailSaleRate().doubleValue()
                        : 0.0);
        return dto;
    }

    private double bundleTotalRetail(PharmacyBundle bundle) {
        if (bundle == null || bundle.getSummaryRow() == null) {
            return 0.0;
        }
        BigDecimal val = bundle.getSummaryRow().getValueOfStocksAtRetailSaleRate();
        return val != null ? val.doubleValue() : 0.0;
    }

    private BalanceCheckDTO buildBalanceCheck(
            double openingRetail, double closingRetail,
            PharmacyBundle sales, PharmacyBundle purchases,
            PharmacyBundle transfers, PharmacyBundle adjustments) {

        double salesRetail = bundleTotalRetail(sales);
        double purchasesRetail = bundleTotalRetail(purchases);
        double transfersRetail = bundleTotalRetail(transfers);
        double adjustmentsRetail = bundleTotalRetail(adjustments);

        double expectedClosing = round2(openingRetail
                + salesRetail
                + purchasesRetail
                + transfersRetail
                + adjustmentsRetail);

        double roundedClosing = round2(closingRetail);
        double discrepancy = round2(roundedClosing - expectedClosing);

        BalanceCheckDTO check = new BalanceCheckDTO();
        check.setExpectedClosingAtRetailRate(expectedClosing);
        check.setActualClosingAtRetailRate(roundedClosing);
        check.setDiscrepancyAtRetailRate(discrepancy);
        check.setBalanced(Math.abs(discrepancy) <= BALANCE_TOLERANCE);
        check.setToleranceUsed(BALANCE_TOLERANCE);
        check.setFormula(
                "expectedClosing = opening(" + round2(openingRetail) + ")"
                + " + sales(" + round2(salesRetail) + ")"
                + " + purchases(" + round2(purchasesRetail) + ")"
                + " + transfers(" + round2(transfersRetail) + ")"
                + " + adjustments(" + round2(adjustmentsRetail) + ")"
                + " = " + expectedClosing
                + " | actual=" + roundedClosing
                + " | discrepancy=" + discrepancy);

        return check;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}

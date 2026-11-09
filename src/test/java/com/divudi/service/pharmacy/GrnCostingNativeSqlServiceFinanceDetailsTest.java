package com.divudi.service.pharmacy;

import com.divudi.core.entity.BillFinanceDetails;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests verifying GRN native costing save persists full bill-level
 * finance details (#25390).
 */
public class GrnCostingNativeSqlServiceFinanceDetailsTest {

    @Test
    @DisplayName("copyFinanceDetails copies netTotal, grossTotal, line totals, and stock values (#25390)")
    void copyFinanceDetails_copiesAllFinanceFields() {
        BillFinanceDetails src = new BillFinanceDetails();
        src.setNetTotal(new BigDecimal("12500.50"));
        src.setGrossTotal(new BigDecimal("13000.00"));
        src.setLineNetTotal(new BigDecimal("12800.00"));
        src.setLineGrossTotal(new BigDecimal("13000.00"));
        src.setBillNetTotal(new BigDecimal("12500.50"));
        src.setBillGrossTotal(new BigDecimal("13000.00"));

        src.setTotalCostValue(new BigDecimal("9800.00"));
        src.setTotalCostValueFree(new BigDecimal("300.00"));
        src.setTotalCostValueNonFree(new BigDecimal("9500.00"));

        src.setTotalPurchaseValue(new BigDecimal("10500.00"));
        src.setTotalPurchaseValueFree(new BigDecimal("500.00"));
        src.setTotalPurchaseValueNonFree(new BigDecimal("10000.00"));

        src.setTotalRetailSaleValue(new BigDecimal("16000.00"));
        src.setTotalRetailSaleValueFree(new BigDecimal("800.00"));
        src.setTotalRetailSaleValueNonFree(new BigDecimal("15200.00"));

        src.setTotalWholesaleValue(new BigDecimal("14000.00"));
        src.setTotalWholesaleValueFree(new BigDecimal("700.00"));
        src.setTotalWholesaleValueNonFree(new BigDecimal("13300.00"));

        src.setTotalOfFreeItemValues(new BigDecimal("800.00"));
        src.setTotalQuantity(new BigDecimal("150"));
        src.setTotalFreeQuantity(new BigDecimal("15"));
        src.setTotalQuantityInAtomicUnitOfMeasurement(new BigDecimal("1500"));
        src.setTotalFreeQuantityInAtomicUnitOfMeasurement(new BigDecimal("150"));

        src.setBillDiscount(new BigDecimal("200.00"));
        src.setLineDiscount(new BigDecimal("100.00"));
        src.setTotalDiscount(new BigDecimal("300.00"));

        src.setBillExpense(new BigDecimal("150.00"));
        src.setLineExpense(new BigDecimal("50.00"));
        src.setTotalExpense(new BigDecimal("200.00"));

        src.setBillTaxValue(new BigDecimal("80.00"));
        src.setItemTaxValue(new BigDecimal("20.00"));
        src.setTotalTaxValue(new BigDecimal("100.00"));

        src.setBillCostValue(new BigDecimal("120.00"));
        src.setLineCostValue(new BigDecimal("9800.00"));

        src.setBillExpensesConsideredForCosting(new BigDecimal("150.00"));
        src.setBillExpensesNotConsideredForCosting(new BigDecimal("50.00"));
        src.setTotalBillValue(new BigDecimal("12550.50"));

        BillFinanceDetails dest = new BillFinanceDetails();
        GrnCostingNativeSqlService.copyFinanceDetails(src, dest);

        assertEquals(new BigDecimal("12500.50"), dest.getNetTotal());
        assertEquals(new BigDecimal("13000.00"), dest.getGrossTotal());
        assertEquals(new BigDecimal("12800.00"), dest.getLineNetTotal());
        assertEquals(new BigDecimal("13000.00"), dest.getLineGrossTotal());
        assertEquals(new BigDecimal("12500.50"), dest.getBillNetTotal());
        assertEquals(new BigDecimal("13000.00"), dest.getBillGrossTotal());

        assertEquals(new BigDecimal("9800.00"), dest.getTotalCostValue());
        assertEquals(new BigDecimal("300.00"), dest.getTotalCostValueFree());
        assertEquals(new BigDecimal("9500.00"), dest.getTotalCostValueNonFree());

        assertEquals(new BigDecimal("10500.00"), dest.getTotalPurchaseValue());
        assertEquals(new BigDecimal("500.00"), dest.getTotalPurchaseValueFree());
        assertEquals(new BigDecimal("10000.00"), dest.getTotalPurchaseValueNonFree());

        assertEquals(new BigDecimal("16000.00"), dest.getTotalRetailSaleValue());
        assertEquals(new BigDecimal("800.00"), dest.getTotalRetailSaleValueFree());
        assertEquals(new BigDecimal("15200.00"), dest.getTotalRetailSaleValueNonFree());

        assertEquals(new BigDecimal("14000.00"), dest.getTotalWholesaleValue());
        assertEquals(new BigDecimal("700.00"), dest.getTotalWholesaleValueFree());
        assertEquals(new BigDecimal("13300.00"), dest.getTotalWholesaleValueNonFree());

        assertEquals(new BigDecimal("800.00"), dest.getTotalOfFreeItemValues());
        assertEquals(new BigDecimal("150"), dest.getTotalQuantity());
        assertEquals(new BigDecimal("15"), dest.getTotalFreeQuantity());
        assertEquals(new BigDecimal("1500"), dest.getTotalQuantityInAtomicUnitOfMeasurement());
        assertEquals(new BigDecimal("150"), dest.getTotalFreeQuantityInAtomicUnitOfMeasurement());

        assertEquals(new BigDecimal("200.00"), dest.getBillDiscount());
        assertEquals(new BigDecimal("100.00"), dest.getLineDiscount());
        assertEquals(new BigDecimal("300.00"), dest.getTotalDiscount());

        assertEquals(new BigDecimal("150.00"), dest.getBillExpense());
        assertEquals(new BigDecimal("50.00"), dest.getLineExpense());
        assertEquals(new BigDecimal("200.00"), dest.getTotalExpense());

        assertEquals(new BigDecimal("80.00"), dest.getBillTaxValue());
        assertEquals(new BigDecimal("20.00"), dest.getItemTaxValue());
        assertEquals(new BigDecimal("100.00"), dest.getTotalTaxValue());

        assertEquals(new BigDecimal("120.00"), dest.getBillCostValue());
        assertEquals(new BigDecimal("9800.00"), dest.getLineCostValue());

        assertEquals(new BigDecimal("150.00"), dest.getBillExpensesConsideredForCosting());
        assertEquals(new BigDecimal("50.00"), dest.getBillExpensesNotConsideredForCosting());
        assertEquals(new BigDecimal("12550.50"), dest.getTotalBillValue());
    }

    @Test
    @DisplayName("copyFinanceDetails preserves existing fields when source fields are null (#25390)")
    void copyFinanceDetails_nullFieldsInSourceDoNotOverwriteExistingDestinationValues() {
        BillFinanceDetails dest = new BillFinanceDetails();
        dest.setNetTotal(new BigDecimal("5000.00"));
        dest.setGrossTotal(new BigDecimal("5200.00"));
        dest.setLineNetTotal(new BigDecimal("5000.00"));
        dest.setTotalPurchaseValue(new BigDecimal("4500.00"));

        BillFinanceDetails src = new BillFinanceDetails();
        src.setTotalDiscount(new BigDecimal("200.00"));
        src.setTotalExpense(new BigDecimal("150.00"));
        src.setTotalTaxValue(new BigDecimal("50.00"));
        src.setTotalBillValue(new BigDecimal("4950.00"));

        GrnCostingNativeSqlService.copyFinanceDetails(src, dest);

        // Retained fields
        assertEquals(new BigDecimal("5000.00"), dest.getNetTotal());
        assertEquals(new BigDecimal("5200.00"), dest.getGrossTotal());
        assertEquals(new BigDecimal("5000.00"), dest.getLineNetTotal());
        assertEquals(new BigDecimal("4500.00"), dest.getTotalPurchaseValue());

        // Updated fields
        assertEquals(new BigDecimal("200.00"), dest.getTotalDiscount());
        assertEquals(new BigDecimal("150.00"), dest.getTotalExpense());
        assertEquals(new BigDecimal("50.00"), dest.getTotalTaxValue());
        assertEquals(new BigDecimal("4950.00"), dest.getTotalBillValue());
    }

    @Test
    @DisplayName("copyFinanceDetails safely handles null inputs without exception")
    void copyFinanceDetails_nullSafety() {
        assertDoesNotThrow(() -> GrnCostingNativeSqlService.copyFinanceDetails(null, null));
        assertDoesNotThrow(() -> GrnCostingNativeSqlService.copyFinanceDetails(null, new BillFinanceDetails()));
        assertDoesNotThrow(() -> GrnCostingNativeSqlService.copyFinanceDetails(new BillFinanceDetails(), null));
    }
}


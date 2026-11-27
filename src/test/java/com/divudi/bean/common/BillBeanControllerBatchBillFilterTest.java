package com.divudi.bean.common;

import com.divudi.core.data.BillTypeAtomic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26216: payment-method totals in Book Keeping must not count an OPD
 * batch bill on top of its individual bills.
 */
class BillBeanControllerBatchBillFilterTest {

    @Test
    void skipListHasBatchSaleCollectionAndCancellationTypes() {
        assertEquals(6, BillBeanController.batchBillsToSkipInPaymentTotals().size());
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT));
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT));
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER));
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER));
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION));
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION));
    }

    @Test
    void skipListKeepsIndividualBillsAndTheirCancellations() {
        assertFalse(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BILL_WITH_PAYMENT));
        assertFalse(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER));
        assertFalse(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BILL_CANCELLATION));
        assertFalse(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION));
        assertFalse(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION));
    }

    @Test
    void cancelledCardBatchNetsToZero() {
        // Card batch of 1000: batch +1000, bills +1000, batch cancel -1000, bill cancels -1000.
        java.util.Map<BillTypeAtomic, Double> rows = new java.util.LinkedHashMap<>();
        rows.put(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT, 1000.0);
        rows.put(BillTypeAtomic.OPD_BILL_WITH_PAYMENT, 1000.0);
        rows.put(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION, -1000.0);
        rows.put(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION, -1000.0);
        double sum = 0;
        for (java.util.Map.Entry<BillTypeAtomic, Double> e : rows.entrySet()) {
            if (!BillBeanController.batchBillsToSkipInPaymentTotals().contains(e.getKey())) {
                sum += e.getValue();
            }
        }
        assertEquals(0.0, sum, 0.0001);
    }

    @Test
    void vatTotalQuerySumsNetTotalPlusVatWithSameFilter() {
        String jpql = BillBeanController.paymentMethodBillsWithoutBatchJpql(true, true);
        assertTrue(jpql.startsWith("select sum(b.netTotal+b.vat) from Bill b"));
        assertTrue(jpql.contains("b.billTypeAtomic not in :skipAtomic"));
    }

    @Test
    void listQueryFiltersBatchBillsAndKeepsOldBills() {
        String jpql = BillBeanController.paymentMethodBillsWithoutBatchJpql(false);
        assertTrue(jpql.startsWith("select b from Bill b"));
        assertTrue(jpql.contains("(b.billTypeAtomic is null or b.billTypeAtomic not in :skipAtomic)"));
        assertTrue(jpql.contains("order by b.id desc"));
    }

    @Test
    void totalQuerySumsNetTotalWithSameFilter() {
        String jpql = BillBeanController.paymentMethodBillsWithoutBatchJpql(true);
        assertTrue(jpql.startsWith("select sum(b.netTotal) from Bill b"));
        assertTrue(jpql.contains("b.billTypeAtomic not in :skipAtomic"));
        assertFalse(jpql.contains("order by"));
    }
}

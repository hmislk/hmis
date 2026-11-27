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
    void skipListHasBothBatchBillsWithPayment() {
        assertEquals(2, BillBeanController.batchBillsToSkipInPaymentTotals().size());
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT));
        assertTrue(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT));
    }

    @Test
    void skipListKeepsIndividualBills() {
        assertFalse(BillBeanController.batchBillsToSkipInPaymentTotals().contains(BillTypeAtomic.OPD_BILL_WITH_PAYMENT));
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

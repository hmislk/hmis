package com.divudi.bean.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26283: the surgery header total counts live children only (a cancelled
 * bill and any cancellation bill are both left out) and adds unlinked refunds.
 */
class BillBeanControllerHeaderTotalQueryTest {

    @Test
    void childQueryCountsOnlyLiveChildrenAndLeavesOutCancellationBills() {
        String jpql = BillBeanController.SURGERY_HEADER_CHILD_TOTAL_JPQL;
        assertTrue(jpql.contains("bf.forwardReferenceBill=:bill"));
        assertTrue(jpql.contains("bf.cancelled=false"));
        assertTrue(jpql.contains("not exists (select o.id from Bill o where o.cancelledBill=bf)"));
    }

    @Test
    void refundQuerySkipsRefundsAlreadyLinkedToTheHeader() {
        String jpql = BillBeanController.SURGERY_HEADER_REFUND_TOTAL_JPQL;
        assertTrue(jpql.contains("from RefundBill r"));
        assertTrue(jpql.contains("r.forwardReferenceBill is null"));
        assertTrue(jpql.contains("r.billedBill.forwardReferenceBill=:bill"));
        assertTrue(jpql.contains("r.billedBill.retired=false"));
        assertTrue(jpql.contains("r.billedBill.cancelled=false"));
    }
}

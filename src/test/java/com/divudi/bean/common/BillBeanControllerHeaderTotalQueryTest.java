package com.divudi.bean.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26283: the surgery header total must leave out a cancelled child whose
 * cancellation bill is not linked to the header, and must add unlinked refunds.
 */
class BillBeanControllerHeaderTotalQueryTest {

    @Test
    void childQueryNetsCancelledChildOnlyWhenCancellationIsNotLinked() {
        String jpql = BillBeanController.SURGERY_HEADER_CHILD_TOTAL_JPQL;
        assertTrue(jpql.contains("bf.forwardReferenceBill=:bill"));
        assertTrue(jpql.contains("left join bf.cancelledBill cb"));
        assertTrue(jpql.contains("bf.cancelled=false or"));
        assertTrue(jpql.contains("cb.forwardReferenceBill=:bill"));
    }

    @Test
    void refundQuerySkipsRefundsAlreadyLinkedToTheHeader() {
        String jpql = BillBeanController.SURGERY_HEADER_REFUND_TOTAL_JPQL;
        assertTrue(jpql.contains("from RefundBill r"));
        assertTrue(jpql.contains("r.forwardReferenceBill is null"));
        assertTrue(jpql.contains("r.billedBill.forwardReferenceBill=:bill"));
        assertTrue(jpql.contains("r.billedBill.retired=false"));
    }
}

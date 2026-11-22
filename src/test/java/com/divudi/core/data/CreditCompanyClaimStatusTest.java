package com.divudi.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreditCompanyClaimStatusTest {

    @Test
    void draftCanBeSubmittedOrCancelledOnly() {
        assertTrue(CreditCompanyClaimStatus.DRAFT.canMoveTo(CreditCompanyClaimStatus.SUBMITTED));
        assertTrue(CreditCompanyClaimStatus.DRAFT.canMoveTo(CreditCompanyClaimStatus.CANCELLED));
        assertFalse(CreditCompanyClaimStatus.DRAFT.canMoveTo(CreditCompanyClaimStatus.REJECTED));
        assertFalse(CreditCompanyClaimStatus.DRAFT.canMoveTo(CreditCompanyClaimStatus.SETTLED));
    }

    @Test
    void submittedAndResubmittedCanBeRejected() {
        assertTrue(CreditCompanyClaimStatus.SUBMITTED.canMoveTo(CreditCompanyClaimStatus.REJECTED));
        assertTrue(CreditCompanyClaimStatus.RESUBMITTED.canMoveTo(CreditCompanyClaimStatus.REJECTED));
        assertFalse(CreditCompanyClaimStatus.SUBMITTED.canMoveTo(CreditCompanyClaimStatus.DRAFT));
    }

    @Test
    void onlyRejectedCanBeResubmitted() {
        assertTrue(CreditCompanyClaimStatus.REJECTED.canMoveTo(CreditCompanyClaimStatus.RESUBMITTED));
        for (CreditCompanyClaimStatus s : CreditCompanyClaimStatus.values()) {
            if (s != CreditCompanyClaimStatus.REJECTED) {
                assertFalse(s.canMoveTo(CreditCompanyClaimStatus.RESUBMITTED), s.name());
            }
        }
    }

    @Test
    void finalStatusesMoveNowhere() {
        for (CreditCompanyClaimStatus s : new CreditCompanyClaimStatus[]{
            CreditCompanyClaimStatus.SETTLED, CreditCompanyClaimStatus.CLOSED, CreditCompanyClaimStatus.CANCELLED}) {
            assertTrue(s.getAllowedNext().isEmpty(), s.name());
        }
        assertFalse(CreditCompanyClaimStatus.DRAFT.canMoveTo(null));
    }

    @Test
    void settledClosedAndCancelledReleaseTheirBills() {
        assertEquals(5, CreditCompanyClaimStatus.openStatuses().length);
        assertTrue(CreditCompanyClaimStatus.PARTIALLY_SETTLED.isOpen());
        assertFalse(CreditCompanyClaimStatus.CANCELLED.isOpen());
    }

    @Test
    void onlyDraftAndRejectedCanBeCancelled() {
        for (CreditCompanyClaimStatus s : CreditCompanyClaimStatus.values()) {
            boolean expected = s == CreditCompanyClaimStatus.DRAFT || s == CreditCompanyClaimStatus.REJECTED;
            assertEquals(expected, CreditCompanyClaimStatus.canCancel(s), s.name());
            if (CreditCompanyClaimStatus.canCancel(s)) {
                assertTrue(s.canMoveTo(CreditCompanyClaimStatus.CANCELLED), s.name());
            }
        }
        assertFalse(CreditCompanyClaimStatus.canCancel(null));
    }
}

package com.divudi.service;

import static com.divudi.core.data.CreditCompanyClaimStatus.PARTIALLY_SETTLED;
import static com.divudi.core.data.CreditCompanyClaimStatus.RESUBMITTED;
import static com.divudi.core.data.CreditCompanyClaimStatus.SETTLED;
import static com.divudi.core.data.CreditCompanyClaimStatus.SUBMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.divudi.core.data.CreditCompanyClaimStatus;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The claim status recompute rule when settlements are saved or cancelled (#25847). */
class CreditCompanyClaimSettlementRuleTest {

    private static List<BigDecimal> dues(String... v) {
        return Arrays.stream(v).map(BigDecimal::new).collect(java.util.stream.Collectors.toList());
    }

    private static CreditCompanyClaimStatus after(CreditCompanyClaimStatus cur, int resub, List<BigDecimal> dues,
            String linked, boolean inAnother) {
        return CreditCompanyClaimService.statusAfterSettlement(cur, resub, dues, new BigDecimal(linked), inAnother);
    }

    @Test
    void allBillsAtOrBelowCutoffIsSettled() {
        assertEquals(SETTLED, after(SUBMITTED, 0, dues("0.00", "0.10"), "50", false));
    }

    @Test
    void oneBillAboveCutoffIsNotSettled() {
        assertEquals(PARTIALLY_SETTLED, after(SUBMITTED, 0, dues("0.00", "0.11"), "50", false));
    }

    @Test
    void someLinkedButNotAllIsPartiallySettled() {
        assertEquals(PARTIALLY_SETTLED, after(RESUBMITTED, 1, dues("10.00", "20.00"), "5", false));
    }

    @Test
    void nothingLinkedGoesBackToSubmitted() {
        assertEquals(SUBMITTED, after(PARTIALLY_SETTLED, 0, dues("10.00"), "0", false));
        assertEquals(SUBMITTED, after(SETTLED, 0, dues("10.00"), "0", false));
    }

    @Test
    void nothingLinkedGoesBackToResubmittedWhenResubmitted() {
        assertEquals(RESUBMITTED, after(PARTIALLY_SETTLED, 2, dues("10.00"), "0", false));
        assertEquals(RESUBMITTED, after(SETTLED, 1, dues("10.00"), "0", false));
    }

    @Test
    void settledClaimReopensToPartiallySettledWhenSomeStillLinked() {
        assertEquals(PARTIALLY_SETTLED, after(SETTLED, 0, dues("10.00", "0.00"), "40", false));
    }

    @Test
    void settledClaimStaysSettledWhenABillIsInAnotherOpenClaim() {
        assertEquals(SETTLED, after(SETTLED, 0, dues("10.00"), "0", true));
        assertEquals(SETTLED, after(SETTLED, 0, dues("10.00"), "5", true));
    }

    @Test
    void anotherOpenClaimDoesNotBlockOtherStatuses() {
        assertEquals(PARTIALLY_SETTLED, after(SUBMITTED, 0, dues("10.00"), "5", true));
    }

    @Test
    void statusUnchangedIsReturnedAsIs() {
        assertEquals(SUBMITTED, after(SUBMITTED, 0, dues("10.00"), "0", false));
        assertEquals(PARTIALLY_SETTLED, after(PARTIALLY_SETTLED, 0, dues("10.00"), "5", false));
        assertEquals(SETTLED, after(SETTLED, 0, dues("0.00"), "5", false));
    }

    @Test
    void claimsNotYetWithTheCompanyAreLeftAlone() {
        for (CreditCompanyClaimStatus s : new CreditCompanyClaimStatus[]{CreditCompanyClaimStatus.DRAFT,
            CreditCompanyClaimStatus.REJECTED, CreditCompanyClaimStatus.CLOSED, CreditCompanyClaimStatus.CANCELLED}) {
            assertEquals(s, after(s, 0, dues("0.00"), "5", false));
        }
    }

    @Test
    void emptyClaimIsLeftAlone() {
        assertEquals(SUBMITTED, after(SUBMITTED, 0, Collections.<BigDecimal>emptyList(), "0", false));
    }

    @Test
    void systemTransitionsAreSeparateFromUserTransitions() {
        assertTrue(SETTLED.canSystemMoveTo(SUBMITTED));
        assertFalse(SETTLED.canMoveTo(SUBMITTED));
        assertTrue(SUBMITTED.canSystemMoveTo(SETTLED));
        assertFalse(CreditCompanyClaimStatus.REJECTED.canSystemMoveTo(SETTLED));
        assertFalse(CreditCompanyClaimStatus.CLOSED.canSystemMoveTo(SETTLED));
        assertFalse(SUBMITTED.canSystemMoveTo(CreditCompanyClaimStatus.REJECTED));
    }

    @Test
    void userTransitionsAreUnchanged() {
        assertEquals(java.util.EnumSet.noneOf(CreditCompanyClaimStatus.class), SETTLED.getAllowedNext());
    }
}

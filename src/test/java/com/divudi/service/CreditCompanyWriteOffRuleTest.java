package com.divudi.service;

import static com.divudi.core.data.CreditCompanyWriteOffStatus.APPROVED;
import static com.divudi.core.data.CreditCompanyWriteOffStatus.CANCELLED;
import static com.divudi.core.data.CreditCompanyWriteOffStatus.REJECTED;
import static com.divudi.core.data.CreditCompanyWriteOffStatus.REQUESTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.CreditCompanyWriteOffStatus;
import com.divudi.core.entity.CreditCompanyWriteOff;
import com.divudi.core.entity.WebUser;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure rules of the credit bill write-off (#25847 part 3). */
class CreditCompanyWriteOffRuleTest {

    private static WebUser user(long id) {
        WebUser u = new WebUser();
        u.setId(id);
        return u;
    }

    private static CreditCompanyWriteOff writeOff(CreditCompanyWriteOffStatus st, long requester) {
        CreditCompanyWriteOff w = new CreditCompanyWriteOff();
        w.setStatus(st);
        w.setRequestedBy(user(requester));
        return w;
    }

    @Test
    void amountMustBeAboveZero() {
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkAmount(new BigDecimal("0.00"), new BigDecimal("100.00")));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkAmount(new BigDecimal("-5.00"), new BigDecimal("100.00")));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkAmount(null, new BigDecimal("100.00")));
    }

    @Test
    void amountCannotExceedDueButMayEqualIt() {
        CreditCompanyWriteOffService.checkAmount(new BigDecimal("100.00"), new BigDecimal("100.00"));
        CreditCompanyWriteOffService.checkAmount(new BigDecimal("0.01"), new BigDecimal("100.00"));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkAmount(new BigDecimal("100.01"), new BigDecimal("100.00")));
    }

    @Test
    void amountIsRoundedHalfUpToTwoPlaces() {
        assertEquals(new BigDecimal("10.01"), CreditCompanyWriteOffService.normalize(10.005));
        assertEquals(new BigDecimal("10.00"), CreditCompanyWriteOffService.normalize(10.004));
        // 1.10 - 1.00 is exactly 0.10 in decimal, not 0.10000000000000009
        assertEquals(new BigDecimal("0.10"), CreditCompanyClaimService.dueDecimalOf(1.10, 0, 1.00));
    }

    @Test
    void reasonIsRequiredAndLimited() {
        assertThrows(CreditCompanyClaimException.class, () -> CreditCompanyWriteOffService.checkReason(null, "the reason"));
        assertThrows(CreditCompanyClaimException.class, () -> CreditCompanyWriteOffService.checkReason("   ", "the reason"));
        CreditCompanyWriteOffService.checkReason("Company rejected the item", "the reason");
        char[] c = new char[501];
        Arrays.fill(c, 'x');
        assertThrows(CreditCompanyClaimException.class, () -> CreditCompanyWriteOffService.checkReason(new String(c), "the reason"));
    }

    @Test
    void requesterCannotDecideTheirOwnRequest() {
        CreditCompanyWriteOff w = writeOff(REQUESTED, 7);
        CreditCompanyClaimException e = assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkDecision(w, user(7), true, APPROVED));
        assertTrue(e.getMessage().contains("another user"));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkDecision(w, user(7), true, REJECTED));
    }

    @Test
    void differentUserWithPrivilegeCanDecide() {
        CreditCompanyWriteOffService.checkDecision(writeOff(REQUESTED, 7), user(8), true, APPROVED);
        CreditCompanyWriteOffService.checkDecision(writeOff(REQUESTED, 7), user(8), true, REJECTED);
    }

    @Test
    void decisionNeedsThePrivilegeAndALoggedInUser() {
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkDecision(writeOff(REQUESTED, 7), user(8), false, APPROVED));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkDecision(writeOff(REQUESTED, 7), null, true, APPROVED));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkDecision(writeOff(REQUESTED, 7), new WebUser(), true, APPROVED));
    }

    @Test
    void statusTransitions() {
        assertTrue(REQUESTED.canMoveTo(APPROVED));
        assertTrue(REQUESTED.canMoveTo(REJECTED));
        assertFalse(REQUESTED.canMoveTo(CANCELLED));
        assertTrue(APPROVED.canMoveTo(CANCELLED));
        assertFalse(APPROVED.canMoveTo(REJECTED));
        assertFalse(APPROVED.canMoveTo(APPROVED));
        for (CreditCompanyWriteOffStatus s : new CreditCompanyWriteOffStatus[]{REJECTED, CANCELLED}) {
            for (CreditCompanyWriteOffStatus n : CreditCompanyWriteOffStatus.values()) {
                assertFalse(s.canMoveTo(n));
            }
        }
    }

    @Test
    void decidedWriteOffCannotBeDecidedAgain() {
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkDecision(writeOff(APPROVED, 7), user(8), true, APPROVED));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkDecision(writeOff(REJECTED, 7), user(8), true, APPROVED));
    }

    @Test
    void cancelOnlyFromApproved() {
        CreditCompanyWriteOffService.checkTransition(writeOff(APPROVED, 7), CANCELLED);
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkTransition(writeOff(REQUESTED, 7), CANCELLED));
    }

    @Test
    void dueSubtractsTheWriteOff() {
        assertEquals(new BigDecimal("30.00"), CreditCompanyWriteOffService.dueAfterWriteOff(100, 10, 40, 20));
        assertEquals(new BigDecimal("0.00"), CreditCompanyWriteOffService.dueAfterWriteOff(100, 0, 60, 40));
        assertEquals(new BigDecimal("50.00"), CreditCompanyClaimService.dueDecimalOf(100, 0, 50));
    }

    @Test
    void fullyWrittenOffBillsMakeTheClaimSettled() {
        List<BigDecimal> dues = Arrays.asList(
                CreditCompanyWriteOffService.dueAfterWriteOff(100, 0, 60, 40),
                CreditCompanyWriteOffService.dueAfterWriteOff(50, 0, 50, 0));
        assertEquals(CreditCompanyClaimStatus.SETTLED, CreditCompanyClaimService.statusAfterSettlement(
                CreditCompanyClaimStatus.PARTIALLY_SETTLED, 0, dues, new BigDecimal("110"), false));
    }

    @Test
    void cancellingAWriteOffReopensASettledClaim() {
        List<BigDecimal> dues = Arrays.asList(CreditCompanyWriteOffService.dueAfterWriteOff(100, 0, 60, 0));
        assertEquals(CreditCompanyClaimStatus.PARTIALLY_SETTLED, CreditCompanyClaimService.statusAfterSettlement(
                CreditCompanyClaimStatus.SETTLED, 0, dues, new BigDecimal("60"), false));
    }

    @Test
    void requestAllowedOnlyForSubmittedResubmittedOrPartiallySettledClaims() {
        assertTrue(CreditCompanyWriteOffService.claimAllowsRequest(CreditCompanyClaimStatus.SUBMITTED));
        assertTrue(CreditCompanyWriteOffService.claimAllowsRequest(CreditCompanyClaimStatus.RESUBMITTED));
        assertTrue(CreditCompanyWriteOffService.claimAllowsRequest(CreditCompanyClaimStatus.PARTIALLY_SETTLED));
        assertFalse(CreditCompanyWriteOffService.claimAllowsRequest(CreditCompanyClaimStatus.DRAFT));
        assertFalse(CreditCompanyWriteOffService.claimAllowsRequest(CreditCompanyClaimStatus.REJECTED));
        assertFalse(CreditCompanyWriteOffService.claimAllowsRequest(CreditCompanyClaimStatus.SETTLED));
    }

    @Test
    void approvedWriteOffCannotBeCancelledByItsRequester() {
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkCancel(writeOff(APPROVED, 7), user(7)));
    }

    @Test
    void approvedWriteOffCanBeCancelledByAnotherUser() {
        CreditCompanyWriteOffService.checkCancel(writeOff(APPROVED, 7), user(8));
    }

    @Test
    void pendingRequestCannotBeCancelled() {
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyWriteOffService.checkCancel(writeOff(REQUESTED, 7), user(7)));
    }
}

package com.divudi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CreditCompanyClaim;
import com.divudi.core.entity.Institution;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreditCompanyClaimServiceTest {

    private static final List<BillTypeAtomic> ALLOWED = Arrays.asList(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);

    private static Institution company(long id) {
        Institution i = new Institution();
        i.setId(id);
        return i;
    }

    private static Bill creditBill(Institution company, double net, double refund, double paid) {
        Bill b = new Bill();
        b.setId(10L);
        b.setDeptId("D/26/1");
        b.setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        b.setPaymentMethod(PaymentMethod.Credit);
        b.setCreditCompany(company);
        b.setNetTotal(net);
        b.setRefundAmount(refund);
        b.setPaidAmount(paid);
        return b;
    }

    @Test
    void dueIsNetLessRefundLessPaid() {
        assertEquals(70.0, CreditCompanyClaimService.dueOf(creditBill(company(1), 100, 10, 20)), 0.0001);
    }

    @Test
    void dueUsesDecimalArithmetic() {
        Bill b = creditBill(company(1), 1.10, 0, 1.00);
        assertEquals(0.10, CreditCompanyClaimService.dueOf(b), 0.0);
        assertEquals(new java.math.BigDecimal("0.10"), CreditCompanyClaimService.dueDecimalOf(b));
    }

    @Test
    void refusesDueExactlyAtTheCutoff() {
        assertThrows(CreditCompanyClaimException.class, () -> CreditCompanyClaimService
                .checkBillEligible(creditBill(company(1), 1.10, 0, 1.00), company(1), ALLOWED));
        CreditCompanyClaimService.checkBillEligible(creditBill(company(1), 1.11, 0, 1.00), company(1), ALLOWED);
    }

    @Test
    void acceptsDueBillOfTheSameCompany() {
        CreditCompanyClaimService.checkBillEligible(creditBill(company(1), 100, 0, 0), company(1), ALLOWED);
    }

    @Test
    void refusesOtherCompany() {
        assertThrows(CreditCompanyClaimException.class, () -> CreditCompanyClaimService
                .checkBillEligible(creditBill(company(2), 100, 0, 0), company(1), ALLOWED));
    }

    @Test
    void refusesNothingDue() {
        assertThrows(CreditCompanyClaimException.class, () -> CreditCompanyClaimService
                .checkBillEligible(creditBill(company(1), 100, 40, 60), company(1), ALLOWED));
    }

    @Test
    void refusesCancelledNonCreditAndOtherTypes() {
        Bill cancelled = creditBill(company(1), 100, 0, 0);
        cancelled.setCancelled(true);
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyClaimService.checkBillEligible(cancelled, company(1), ALLOWED));

        Bill cash = creditBill(company(1), 100, 0, 0);
        cash.setPaymentMethod(PaymentMethod.Cash);
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyClaimService.checkBillEligible(cash, company(1), ALLOWED));

        Bill other = creditBill(company(1), 100, 0, 0);
        other.setBillTypeAtomic(BillTypeAtomic.INWARD_FINAL_BILL);
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyClaimService.checkBillEligible(other, company(1), ALLOWED));
    }

    @Test
    void grnWholesaleIsNotClaimableButSalesAre() {
        List<BillTypeAtomic> all = Arrays.asList(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT,
                BillTypeAtomic.PHARMACY_RETAIL_SALE, BillTypeAtomic.PHARMACY_WHOLESALE,
                BillTypeAtomic.PHARMACY_GRN_WHOLESALE);
        List<BillTypeAtomic> claimable = CreditCompanyClaimService.claimable(all);
        assertEquals(3, claimable.size());
        assertFalse(claimable.contains(BillTypeAtomic.PHARMACY_GRN_WHOLESALE));

        Bill grn = creditBill(company(1), 100, 0, 0);
        grn.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_WHOLESALE);
        CreditCompanyClaimException e = assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyClaimService.checkBillEligible(grn, company(1), all));
        assertTrue(e.getMessage().contains("GRN"));
    }

    @Test
    void transitionFollowsTheEnum() {
        CreditCompanyClaim c = new CreditCompanyClaim();
        c.setStatus(CreditCompanyClaimStatus.DRAFT);
        CreditCompanyClaimService.checkTransition(c, CreditCompanyClaimStatus.SUBMITTED);
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyClaimService.checkTransition(c, CreditCompanyClaimStatus.RESUBMITTED));
    }

    @Test
    void cancelTransitionIsAllowedForDraftAndRejectedOnly() {
        for (CreditCompanyClaimStatus s : CreditCompanyClaimStatus.values()) {
            CreditCompanyClaim c = new CreditCompanyClaim();
            c.setStatus(s);
            if (CreditCompanyClaimStatus.canCancel(s)) {
                CreditCompanyClaimService.checkTransition(c, CreditCompanyClaimStatus.CANCELLED);
            } else {
                assertThrows(CreditCompanyClaimException.class,
                        () -> CreditCompanyClaimService.checkTransition(c, CreditCompanyClaimStatus.CANCELLED));
            }
        }
    }

    @Test
    void openClaimMessageNamesTheClaim() {
        assertEquals("This bill is in open claim D/CLM/26/000001. Cancel or settle the claim first.",
                CreditCompanyClaimService.openClaimMessage("D/CLM/26/000001"));
    }

    @Test
    void partiallySettledClaimClosesOnlyWithAReason() {
        CreditCompanyClaim c = new CreditCompanyClaim();
        c.setStatus(CreditCompanyClaimStatus.PARTIALLY_SETTLED);
        CreditCompanyClaimService.checkCanClosePartiallySettled(c, "Company will not pay the rest");
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyClaimService.checkCanClosePartiallySettled(c, null));
        assertThrows(CreditCompanyClaimException.class,
                () -> CreditCompanyClaimService.checkCanClosePartiallySettled(c, "   "));
    }

    @Test
    void closeHereIsRefusedFromEveryOtherStatus() {
        for (CreditCompanyClaimStatus s : CreditCompanyClaimStatus.values()) {
            if (s == CreditCompanyClaimStatus.PARTIALLY_SETTLED) {
                continue;
            }
            CreditCompanyClaim c = new CreditCompanyClaim();
            c.setStatus(s);
            assertThrows(CreditCompanyClaimException.class,
                    () -> CreditCompanyClaimService.checkCanClosePartiallySettled(c, "reason"));
        }
    }
}

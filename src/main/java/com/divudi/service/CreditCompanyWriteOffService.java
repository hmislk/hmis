package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.CreditCompanyWriteOffStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CreditCompanyClaim;
import com.divudi.core.entity.CreditCompanyWriteOff;
import com.divudi.core.data.Privileges;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.CreditCompanyClaimFacade;
import com.divudi.core.facade.CreditCompanyClaimItemFacade;
import com.divudi.core.facade.CreditCompanyWriteOffFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Write-off of the due of an OPD credit bill (#25847 part 3). One user requests,
 * a different user with the approve privilege approves; only then does the bill's
 * {@code writtenOffAmount} (and so its due) change. Refusals throw
 * {@link CreditCompanyClaimException}, which rolls back.
 */
@Stateless
public class CreditCompanyWriteOffService {

    @EJB
    private CreditCompanyWriteOffFacade writeOffFacade;
    @EJB
    private CreditCompanyClaimFacade claimFacade;
    @EJB
    private CreditCompanyClaimItemFacade claimItemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private CreditCompanyClaimService claimService;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;

    /**
     * Checks the user holds the privilege in some department, so a future caller
     * cannot skip the controller's check. Called after the row locks, so the lock
     * stays the first database read of the transaction.
     */
    private void requirePrivilege(WebUser user, Privileges privilege, String action) {
        long n;
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("u", user);
            p.put("pri", privilege);
            n = webUserPrivilegeFacade.findLongByJpqlStrict("select count(wup) from WebUserPrivilege wup"
                    + " where wup.retired=false and wup.webUser=:u and wup.webUser.retired=false"
                    + " and wup.privilege=:pri", p);
        } catch (RuntimeException e) {
            throw new CreditCompanyClaimException("Could not check your privileges. Try again.");
        }
        if (n < 1) {
            throw new CreditCompanyClaimException("You do not have the privilege to " + action + ".");
        }
    }

    // ---- Pure rules (unit tested) ----

    /** Amount in decimal, 2 places half-up, as the claim service does for dues. */
    public static BigDecimal normalize(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    /** The amount asked for must be above zero and no more than the bill's due now. */
    public static void checkAmount(BigDecimal amount, BigDecimal dueNow) {
        if (amount == null || amount.signum() <= 0) {
            throw new CreditCompanyClaimException("Enter a write-off amount above zero.");
        }
        if (dueNow == null || amount.compareTo(dueNow) > 0) {
            throw new CreditCompanyClaimException("The write-off amount cannot be more than the amount due on the bill ("
                    + (dueNow == null ? "0.00" : dueNow.toPlainString()) + ").");
        }
    }

    public static final int MAX_TEXT = 500;

    public static void checkReason(String reason, String what) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new CreditCompanyClaimException("Enter " + what + ".");
        }
        if (reason.trim().length() > MAX_TEXT) {
            throw new CreditCompanyClaimException("Keep " + what + " within " + MAX_TEXT + " characters.");
        }
    }

    private static boolean sameUser(WebUser a, WebUser b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    /** Approve or reject: needs the privilege, a different user than the requester, and a requested write-off. */
    public static void checkDecision(CreditCompanyWriteOff w, WebUser decider, boolean hasApprovePrivilege,
            CreditCompanyWriteOffStatus next) {
        if (w == null || w.getStatus() == null) {
            throw new CreditCompanyClaimException("Write-off not found.");
        }
        if (decider == null || decider.getId() == null) {
            throw new CreditCompanyClaimException("You must be logged in to decide a write-off.");
        }
        if (!hasApprovePrivilege) {
            throw new CreditCompanyClaimException("You do not have the privilege to approve or reject write-offs.");
        }
        if (sameUser(w.getRequestedBy(), decider)) {
            throw new CreditCompanyClaimException("You requested this write-off, so another user must approve or reject it.");
        }
        checkTransition(w, next);
    }

    public static void checkTransition(CreditCompanyWriteOff w, CreditCompanyWriteOffStatus next) {
        if (!w.getStatus().canMoveTo(next)) {
            throw new CreditCompanyClaimException("A write-off that is " + w.getStatus().getLabel()
                    + " cannot be changed to " + next.getLabel() + ".");
        }
    }

    /** An approved write-off must be cancelled by a different user than the one who requested it. */
    public static void checkCancel(CreditCompanyWriteOff w, WebUser user) {
        checkTransition(w, CreditCompanyWriteOffStatus.CANCELLED);
        if (w.getStatus() == CreditCompanyWriteOffStatus.APPROVED && sameUser(w.getRequestedBy(), user)) {
            throw new CreditCompanyClaimException("Another user must cancel an approved write-off you requested.");
        }
    }

    /** Due left on a bill after its approved write-offs. */
    public static BigDecimal dueAfterWriteOff(double netTotal, double refundAmount, double paidAmount, double writtenOff) {
        return CreditCompanyClaimService.dueDecimalOf(netTotal, refundAmount, paidAmount, writtenOff);
    }

    /** True for claims in which a write-off may be requested from the claim page. */
    public static boolean claimAllowsRequest(CreditCompanyClaimStatus status) {
        return status == CreditCompanyClaimStatus.SUBMITTED || status == CreditCompanyClaimStatus.RESUBMITTED
                || status == CreditCompanyClaimStatus.PARTIALLY_SETTLED;
    }

    public static final String APPROVED_WRITE_OFF_MESSAGE = "This bill has an approved write-off. Cancel the write-off first.";

    // ---- Actions ----

    public CreditCompanyWriteOff request(Long billId, double amount, String reason, WebUser user, boolean hasRequestPrivilege) {
        if (!hasRequestPrivilege) {
            throw new CreditCompanyClaimException("You do not have the privilege to request write-offs.");
        }
        if (user == null || user.getId() == null) {
            throw new CreditCompanyClaimException("You must be logged in to request a write-off.");
        }
        checkReason(reason, "the reason for the write-off");
        if (billId == null) {
            throw new CreditCompanyClaimException("A selected bill was not found.");
        }
        // First database call: serialises requests and decisions on this bill.
        Bill bill = billFacade.findWithLock(billId);
        requirePrivilege(user, Privileges.OpdCreditWriteOffRequest, "request write-offs");
        CreditCompanyClaimService.checkBillEligible(bill, bill == null ? null : bill.getCreditCompany(),
                claimService.claimableBillTypeAtomics());
        BigDecimal due = CreditCompanyClaimService.dueDecimalOf(bill);
        BigDecimal wanted = normalize(amount);
        checkAmount(wanted, due);

        Map<String, Object> p = new HashMap<>();
        p.put("id", billId);
        p.put("st", CreditCompanyWriteOffStatus.REQUESTED);
        long pending;
        try {
            pending = writeOffFacade.findLongByJpqlStrict("select count(w) from CreditCompanyWriteOff w"
                    + " where w.retired=false and w.status=:st and w.bill.id=:id", p);
        } catch (RuntimeException e) {
            throw new CreditCompanyClaimException("Could not check earlier write-off requests. Try again.");
        }
        if (pending > 0) {
            throw new CreditCompanyClaimException("This bill already has a write-off waiting for approval.");
        }

        CreditCompanyClaim claim = openClaimOf(billId);
        if (claim != null && !claimAllowsRequest(claim.getStatus())) {
            throw new CreditCompanyClaimException("A write-off can be requested only for a claim that is submitted, resubmitted or partially settled. Claim "
                    + claim.getClaimNo() + " is " + claim.getStatus().getLabel() + ".");
        }

        CreditCompanyWriteOff w = new CreditCompanyWriteOff();
        w.setBill(bill);
        w.setClaim(claim);
        w.setAmount(wanted.doubleValue());
        w.setReason(reason.trim());
        w.setStatus(CreditCompanyWriteOffStatus.REQUESTED);
        w.setRequestedBy(user);
        w.setRequestedAt(new Date());
        writeOffFacade.create(w);
        return w;
    }

    public CreditCompanyWriteOff approve(Long writeOffId, String comment, WebUser approver, boolean hasApprovePrivilege) {
        // Row locks come first (write-off, then its bill); the privilege read and the approved-total
        // aggregate follow, so their snapshot is taken after the lock and sees every committed write-off.
        CreditCompanyWriteOff w = reload(writeOffId);
        Bill bill = billFacade.findWithLock(w.getBill().getId());
        requirePrivilege(approver, Privileges.OpdCreditWriteOffApprove, "approve or reject write-offs");
        checkDecision(w, approver, hasApprovePrivilege, CreditCompanyWriteOffStatus.APPROVED);
        if (bill == null || bill.isRetired() || bill.isCancelled()) {
            throw new CreditCompanyClaimException("The bill is cancelled, so its due cannot be written off.");
        }
        checkAmount(normalize(w.getAmount()), CreditCompanyClaimService.dueDecimalOf(bill));
        w.setStatus(CreditCompanyWriteOffStatus.APPROVED);
        w.setDecidedBy(approver);
        w.setDecidedAt(new Date());
        if (blankToNull(comment) != null) {
            checkReason(comment, "the comment");
        }
        w.setDecisionComment(blankToNull(comment));
        writeOffFacade.edit(w);
        recomputeBillAndClaims(bill, w);
        return w;
    }

    public CreditCompanyWriteOff reject(Long writeOffId, String comment, WebUser approver, boolean hasApprovePrivilege) {
        CreditCompanyWriteOff w = reload(writeOffId);
        requirePrivilege(approver, Privileges.OpdCreditWriteOffApprove, "approve or reject write-offs");
        checkDecision(w, approver, hasApprovePrivilege, CreditCompanyWriteOffStatus.REJECTED);
        checkReason(comment, "the reason for rejecting the write-off");
        w.setStatus(CreditCompanyWriteOffStatus.REJECTED);
        w.setDecidedBy(approver);
        w.setDecidedAt(new Date());
        w.setDecisionComment(comment.trim());
        writeOffFacade.edit(w);
        return w;
    }

    /** Cancels an approved write-off, restoring the bill's due. Needs the approve privilege. */
    public CreditCompanyWriteOff cancel(Long writeOffId, String reason, WebUser user, boolean hasApprovePrivilege) {
        if (!hasApprovePrivilege) {
            throw new CreditCompanyClaimException("You do not have the privilege to cancel write-offs.");
        }
        if (user == null || user.getId() == null) {
            throw new CreditCompanyClaimException("You must be logged in to cancel a write-off.");
        }
        // Row locks first (write-off, then its bill), before the privilege read and the aggregate.
        CreditCompanyWriteOff w = reload(writeOffId);
        Bill bill = billFacade.findWithLock(w.getBill().getId());
        requirePrivilege(user, Privileges.OpdCreditWriteOffApprove, "cancel write-offs");
        checkCancel(w, user);
        checkReason(reason, "the reason for cancelling the write-off");
        w.setStatus(CreditCompanyWriteOffStatus.CANCELLED);
        w.setCancelledBy(user);
        w.setCancelledAt(new Date());
        w.setCancelReason(reason.trim());
        writeOffFacade.edit(w);
        recomputeBillAndClaims(bill, w);
        return w;
    }

    /** Message when the bill (or the bill it points to) has an approved write-off, else null. */
    public String approvedWriteOffRefusal(Bill... bills) {
        if (bills == null) {
            return null;
        }
        for (Bill b : bills) {
            if (b == null) {
                continue;
            }
            for (Bill x : new Bill[]{b, b.getBackwardReferenceBill(), b.getBilledBill(), b.getReferenceBill()}) {
                if (x != null && x.getId() != null) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", x.getId());
                    p.put("st", CreditCompanyWriteOffStatus.APPROVED);
                    long n;
                    try {
                        n = writeOffFacade.findLongByJpqlStrict("select count(w) from CreditCompanyWriteOff w"
                                + " where w.retired=false and w.status=:st and w.bill.id=:id", p);
                    } catch (RuntimeException e) {
                        return CreditCompanyClaimService.LOOKUP_FAILED;
                    }
                    if (n > 0) {
                        return APPROVED_WRITE_OFF_MESSAGE;
                    }
                }
            }
        }
        return null;
    }

    // ---- Internals ----

    /**
     * Sets the bill's written-off amount to the sum of its approved write-offs (recomputed, not
     * incremented, on a fresh copy of the bill, so a payment saved meanwhile is not overwritten),
     * then recomputes the status of each claim that holds the bill.
     */
    private void recomputeBillAndClaims(Bill lockedBill, CreditCompanyWriteOff w) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", lockedBill.getId());
        p.put("st", CreditCompanyWriteOffStatus.APPROVED);
        List<?> sum = writeOffFacade.findLightsByJpqlStrict("select coalesce(sum(w.amount),0) from CreditCompanyWriteOff w"
                + " where w.retired=false and w.status=:st and w.bill.id=:id", p);
        BigDecimal total = sum.isEmpty() || sum.get(0) == null ? BigDecimal.ZERO
                : normalize(((Number) sum.get(0)).doubleValue());
        // Keep the stored balance (printed as outstanding balance) in step. It is recomputed from
        // its source of truth (net total less refunds, settlements and approved write-offs), never
        // moved by a delta, so approve and cancel are symmetric and idempotent.
        lockedBill.setWrittenOffAmount(total.doubleValue());
        lockedBill.setBalance(Math.max(0.0, CreditCompanyClaimService.dueDecimalOf(lockedBill).doubleValue()));
        billFacade.edit(lockedBill);

        Set<Long> claimIds = new TreeSet<>();
        if (w.getClaim() != null && w.getClaim().getId() != null) {
            claimIds.add(w.getClaim().getId());
        }
        CreditCompanyClaim open = openClaimOf(lockedBill.getId());
        if (open != null) {
            claimIds.add(open.getId());
        }
        // Claims are locked in id order, as the settlement link does, so overlapping requests cannot deadlock.
        for (Long claimId : claimIds) {
            CreditCompanyClaim claim = claimFacade.findWithLock(claimId);
            if (claim != null && !claim.isRetired()) {
                claimService.recomputeStatus(claim);
            }
        }
    }

    private CreditCompanyClaim openClaimOf(Long billId) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", billId);
        p.put("open", Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
        List<?> found;
        try {
            found = claimItemFacade.findLightsByJpqlStrict(
                    "select ci.claim.id from CreditCompanyClaimItem ci where ci.retired=false"
                    + " and ci.claim.retired=false and ci.claim.status in :open and ci.bill.id=:id", p);
        } catch (RuntimeException e) {
            throw new CreditCompanyClaimException(CreditCompanyClaimService.LOOKUP_FAILED);
        }
        return found == null || found.isEmpty() ? null : claimFacade.find((Long) found.get(0));
    }

    private CreditCompanyWriteOff reload(Long id) {
        if (id == null) {
            throw new CreditCompanyClaimException("Write-off not found.");
        }
        CreditCompanyWriteOff w = writeOffFacade.findWithLock(id);
        if (w == null || w.isRetired()) {
            throw new CreditCompanyClaimException("Write-off not found.");
        }
        return w;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}

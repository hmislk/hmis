package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.CreditCompanyClaim;
import com.divudi.core.entity.CreditCompanyClaimItem;
import com.divudi.core.entity.CreditCompanyClaimSettlement;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.CreditCompanyClaimFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.CreditCompanyClaimItemFacade;
import com.divudi.core.facade.CreditCompanyClaimSettlementFacade;
import com.divudi.ejb.BillNumberGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Claims sent to credit companies for OPD credit bills (#25847). Business
 * errors are thrown as {@link CreditCompanyClaimException}.
 */
@Stateless
public class CreditCompanyClaimService {

    /** Same cut-off as OPD Due Search: a bill is due above this amount. */
    public static final double MIN_DUE = 0.1;

    @EJB
    private CreditCompanyClaimFacade claimFacade;
    @EJB
    private CreditCompanyClaimItemFacade claimItemFacade;
    @EJB
    private CreditCompanyClaimSettlementFacade settlementFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @EJB
    private BillService billService;

    /** Due = net total less refunds less settlements (as OPD Due Search). */
    public static double dueOf(Bill bill) {
        return dueDecimalOf(bill).doubleValue();
    }

    private static final Logger LOGGER = Logger.getLogger(CreditCompanyClaimService.class.getName());

    /** Due in decimal arithmetic, 2 places half-up, so 1.10 less 1.00 is exactly 0.10. */
    public static BigDecimal dueDecimalOf(Bill bill) {
        return dueDecimalOf(bill.getNetTotal(), bill.getRefundAmount(), bill.getPaidAmount());
    }

    public static BigDecimal dueDecimalOf(double netTotal, double refundAmount, double paidAmount) {
        return BigDecimal.valueOf(netTotal)
                .subtract(BigDecimal.valueOf(refundAmount).add(BigDecimal.valueOf(paidAmount)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks one bill can go into a claim for the company. Does not check other claims.
     */
    /** Bill types left out of claims: pharmacy GRN wholesale is a purchase, not a sale to a company. */
    public static final List<BillTypeAtomic> NOT_CLAIMABLE = java.util.Collections.unmodifiableList(
            Arrays.asList(BillTypeAtomic.PHARMACY_GRN_WHOLESALE));

    /** The OPD Due Search bill types (OPD, package, pharmacy sales) minus {@link #NOT_CLAIMABLE}. */
    public List<BillTypeAtomic> claimableBillTypeAtomics() {
        return claimable(billService.fetchBillTypeAtomicsForOpdFinance());
    }

    public static List<BillTypeAtomic> claimable(List<BillTypeAtomic> opdFinanceTypes) {
        List<BillTypeAtomic> out = new ArrayList<>(opdFinanceTypes);
        out.removeAll(NOT_CLAIMABLE);
        return out;
    }

    public static void checkBillEligible(Bill bill, Institution company, Collection<BillTypeAtomic> allowedTypes) {
        if (bill == null) {
            throw new CreditCompanyClaimException("A selected bill was not found.");
        }
        String no = bill.getDeptId() != null ? bill.getDeptId() : String.valueOf(bill.getId());
        if (bill.isRetired() || bill.isCancelled()) {
            throw new CreditCompanyClaimException("Bill " + no + " is cancelled, so it cannot be claimed.");
        }
        if (NOT_CLAIMABLE.contains(bill.getBillTypeAtomic())) {
            throw new CreditCompanyClaimException("Bill " + no + " is a pharmacy GRN (purchase) bill and cannot be claimed.");
        }
        if (bill.getPaymentMethod() != PaymentMethod.Credit || bill.getCreditCompany() == null
                || allowedTypes == null || !allowedTypes.contains(bill.getBillTypeAtomic())) {
            throw new CreditCompanyClaimException("Bill " + no + " is not an OPD credit bill.");
        }
        if (company == null || company.getId() == null || !company.getId().equals(bill.getCreditCompany().getId())) {
            throw new CreditCompanyClaimException("All bills in a claim must be for the same credit company (bill " + no + ").");
        }
        if (dueDecimalOf(bill).compareTo(BigDecimal.valueOf(MIN_DUE)) <= 0) {
            throw new CreditCompanyClaimException("Bill " + no + " has nothing due.");
        }
    }

    /** Moves the claim to a new status or refuses. */
    public static void checkTransition(CreditCompanyClaim claim, CreditCompanyClaimStatus next) {
        if (claim == null || claim.getStatus() == null) {
            throw new CreditCompanyClaimException("Claim not found.");
        }
        if (!claim.getStatus().canMoveTo(next)) {
            throw new CreditCompanyClaimException("A claim that is " + claim.getStatus().getLabel()
                    + " cannot be changed to " + next.getLabel() + ".");
        }
    }

    public CreditCompanyClaim createClaim(List<Long> billIds, Institution company, Department department, WebUser user) {
        if (billIds == null || billIds.isEmpty()) {
            throw new CreditCompanyClaimException("Select at least one bill.");
        }
        if (company == null || company.getId() == null) {
            throw new CreditCompanyClaimException("Select the credit company.");
        }
        // Must be the first database call: serialises claim creation per company,
        // so two overlapping requests cannot put one bill in two open claims.
        // A read before the lock would fix the snapshot and miss the other request's rows.
        if (institutionFacade.findWithLock(company.getId()) == null) {
            throw new CreditCompanyClaimException("Select the credit company.");
        }
        if (department == null || department.getInstitution() == null) {
            throw new CreditCompanyClaimException("Select your department before creating a claim.");
        }
        Set<Long> ids = new HashSet<>(billIds);
        Map<String, Object> m = new HashMap<>();
        m.put("ids", ids);
        List<Bill> bills = billFacade.findByJpql("select b from Bill b where b.id in :ids", m);
        if (bills == null || bills.size() != ids.size()) {
            throw new CreditCompanyClaimException("A selected bill was not found.");
        }
        List<BillTypeAtomic> allowed = claimableBillTypeAtomics();
        BigDecimal total = BigDecimal.ZERO;
        for (Bill b : bills) {
            checkBillEligible(b, company, allowed);
            total = total.add(dueDecimalOf(b));
        }

        Map<String, Object> p = new HashMap<>();
        p.put("ids", ids);
        p.put("open", java.util.Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
        long inOtherClaim;
        try {
            inOtherClaim = claimItemFacade.findLongByJpqlStrict(
                    "select count(ci) from CreditCompanyClaimItem ci where ci.retired=false"
                    + " and ci.claim.retired=false and ci.claim.status in :open and ci.bill.id in :ids", p);
        } catch (RuntimeException e) {
            throw new CreditCompanyClaimException("Could not check the selected bills against open claims. Try again.");
        }
        if (inOtherClaim > 0) {
            throw new CreditCompanyClaimException("A selected bill is already in another open claim.");
        }

        Date now = new Date();
        CreditCompanyClaim claim = new CreditCompanyClaim();
        claim.setCreditCompany(company);
        claim.setDepartment(department);
        claim.setInstitution(department.getInstitution());
        claim.setStatus(CreditCompanyClaimStatus.DRAFT);
        claim.setClaimedTotal(total.doubleValue());
        claim.setCreater(user);
        claim.setCreatedAt(now);
        claim.setClaimNo(billNumberGenerator.departmentClaimNumberGeneratorYearlyInNewTransaction(
                department, BillTypeAtomic.OPD_CREDIT_COMPANY_CLAIM, "CLM"));
        claimFacade.create(claim);

        for (Bill b : bills) {
            CreditCompanyClaimItem item = new CreditCompanyClaimItem();
            item.setClaim(claim);
            item.setBill(b);
            item.setClaimedAmount(dueOf(b));
            item.setCreater(user);
            item.setCreatedAt(now);
            claimItemFacade.create(item);
        }
        return claim;
    }

    /** True when the bill is in a claim that is still open (draft, submitted, rejected or resubmitted). */
    public boolean isInOpenClaim(Long billId) {
        if (billId == null) {
            return false;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("id", billId);
        p.put("open", Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
        long n;
        try {
            n = claimItemFacade.findLongByJpqlStrict(
                    "select count(ci) from CreditCompanyClaimItem ci where ci.retired=false"
                    + " and ci.claim.retired=false and ci.claim.status in :open and ci.bill.id=:id", p);
        } catch (RuntimeException e) {
            throw new CreditCompanyClaimException(LOOKUP_FAILED);
        }
        return n > 0;
    }

    public static final String LOOKUP_FAILED = "Could not check claims for this bill. Try again.";

    /** Claim number of the open claim holding the bill, or null. Throws when the lookup fails. */
    public String openClaimNoFor(Long billId) {
        if (billId == null) {
            return null;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("id", billId);
        p.put("open", Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
        List<?> found;
        try {
            found = claimItemFacade.findLightsByJpqlStrict(
                    "select ci.claim.claimNo from CreditCompanyClaimItem ci where ci.retired=false"
                    + " and ci.claim.retired=false and ci.claim.status in :open and ci.bill.id=:id", p);
        } catch (RuntimeException e) {
            throw new CreditCompanyClaimException(LOOKUP_FAILED);
        }
        return found == null || found.isEmpty() ? null : String.valueOf(found.get(0));
    }

    public static String openClaimMessage(String claimNo) {
        return "This bill is in open claim " + claimNo + ". Cancel or settle the claim first.";
    }

    /**
     * Refusal text for a refund, return or cancel of these bills, or null when none is in
     * an open claim. Checks each bill and the bills it points to (the batch bill of an
     * individual bill, the original of a refund or cancellation), since the claim holds
     * the credit bill, which for OPD is the batch bill.
     */
    public String openClaimRefusal(Bill... bills) {
        if (bills == null) {
            return null;
        }
        for (Bill b : bills) {
            if (b == null) {
                continue;
            }
            for (Bill x : new Bill[]{b, b.getBackwardReferenceBill(), b.getBilledBill(), b.getReferenceBill()}) {
                if (x != null && x.getId() != null) {
                    String no;
                    try {
                        no = openClaimNoFor(x.getId());
                    } catch (CreditCompanyClaimException e) {
                        return LOOKUP_FAILED;
                    }
                    if (no != null) {
                        return openClaimMessage(no);
                    }
                }
            }
        }
        return null;
    }

    /** Cancels a draft or rejected claim, which frees its bills. */
    public CreditCompanyClaim cancel(CreditCompanyClaim claim, WebUser user) {
        CreditCompanyClaim c = reload(claim);
        if (!CreditCompanyClaimStatus.canCancel(c.getStatus())) {
            throw new CreditCompanyClaimException("Only a draft or rejected claim can be cancelled. This claim is "
                    + c.getStatus().getLabel() + ".");
        }
        checkTransition(c, CreditCompanyClaimStatus.CANCELLED);
        c.setStatus(CreditCompanyClaimStatus.CANCELLED);
        c.setCancelledAt(new Date());
        c.setCancelledBy(user);
        claimFacade.edit(c);
        return c;
    }

    public CreditCompanyClaim submit(CreditCompanyClaim claim, Date submittedOn, String companyReference, WebUser user) {
        CreditCompanyClaim c = reload(claim);
        checkTransition(c, CreditCompanyClaimStatus.SUBMITTED);
        if (submittedOn == null) {
            throw new CreditCompanyClaimException("Enter the date the claim was submitted.");
        }
        c.setStatus(CreditCompanyClaimStatus.SUBMITTED);
        c.setSubmittedAt(submittedOn);
        c.setSubmitter(user);
        c.setCompanyReference(blankToNull(companyReference));
        claimFacade.edit(c);
        // Bills may have been paid while the claim was a draft: start from the real state.
        recomputeStatus(c);
        return c;
    }

    public CreditCompanyClaim reject(CreditCompanyClaim claim, String reason, WebUser user) {
        CreditCompanyClaim c = reload(claim);
        checkTransition(c, CreditCompanyClaimStatus.REJECTED);
        if (blankToNull(reason) == null) {
            throw new CreditCompanyClaimException("Enter the reason the company rejected the claim.");
        }
        c.setStatus(CreditCompanyClaimStatus.REJECTED);
        c.setRejectReason(reason.trim());
        c.setRejectedAt(new Date());
        c.setRejecter(user);
        claimFacade.edit(c);
        return c;
    }

    /**
     * Closes a partially settled claim with a reason. The bills are freed from the
     * claim; what the company has not paid stays due on the bills.
     */
    public CreditCompanyClaim closePartiallySettled(CreditCompanyClaim claim, String reason, WebUser user) {
        CreditCompanyClaim c = reload(claim);
        checkCanClosePartiallySettled(c, reason);
        c.setStatus(CreditCompanyClaimStatus.CLOSED);
        c.setCloseReason(reason.trim());
        c.setClosedAt(new Date());
        c.setClosedBy(user);
        claimFacade.edit(c);
        return c;
    }

    /** The rule for closing: only a partially settled claim, and a reason is needed. */
    public static void checkCanClosePartiallySettled(CreditCompanyClaim claim, String reason) {
        if (claim == null || claim.getStatus() == null) {
            throw new CreditCompanyClaimException("Claim not found.");
        }
        if (claim.getStatus() != CreditCompanyClaimStatus.PARTIALLY_SETTLED) {
            throw new CreditCompanyClaimException("Only a partially settled claim can be closed here. This claim is "
                    + claim.getStatus().getLabel() + ".");
        }
        checkTransition(claim, CreditCompanyClaimStatus.CLOSED);
        if (blankToNull(reason) == null) {
            throw new CreditCompanyClaimException("Enter the reason for closing the claim.");
        }
    }

    public CreditCompanyClaim resubmit(CreditCompanyClaim claim, Date submittedOn, String companyReference, WebUser user) {
        CreditCompanyClaim c = reload(claim);
        checkTransition(c, CreditCompanyClaimStatus.RESUBMITTED);
        if (submittedOn == null) {
            throw new CreditCompanyClaimException("Enter the date the claim was resubmitted.");
        }
        c.setStatus(CreditCompanyClaimStatus.RESUBMITTED);
        c.setResubmitCount(c.getResubmitCount() + 1);
        c.setSubmittedAt(submittedOn);
        c.setSubmitter(user);
        if (blankToNull(companyReference) != null) {
            c.setCompanyReference(companyReference.trim());
        }
        claimFacade.edit(c);
        // Bills may have been paid while the claim was rejected: start from the real state.
        recomputeStatus(c);
        return c;
    }

    // ---- Settlement link (#25847 part 2) ----

    /**
     * Status of a claim after settlements change, or the current status when settlement
     * does not apply to it (draft, rejected, closed, cancelled). The one recompute rule:
     * every bill's due at or below {@link #MIN_DUE}: Settled. Otherwise some amount linked:
     * Partially Settled. Otherwise nothing linked: back to Submitted, or Resubmitted when
     * the claim was resubmitted. A Settled claim does not reopen while one of its bills
     * is in another open claim.
     *
     * @param dues due of each bill in the claim
     * @param linkedTotal sum of the active settlement links of the claim
     */
    public static CreditCompanyClaimStatus statusAfterSettlement(CreditCompanyClaimStatus current, int resubmitCount,
            Collection<BigDecimal> dues, BigDecimal linkedTotal, boolean aBillIsInAnotherOpenClaim) {
        if (current == null || !current.followsSettlement() || dues == null || dues.isEmpty()) {
            return current;
        }
        BigDecimal cutoff = BigDecimal.valueOf(MIN_DUE);
        boolean allSettled = true;
        for (BigDecimal d : dues) {
            if (d.compareTo(cutoff) > 0) {
                allSettled = false;
                break;
            }
        }
        CreditCompanyClaimStatus next;
        if (allSettled) {
            next = CreditCompanyClaimStatus.SETTLED;
        } else if (linkedTotal != null && linkedTotal.signum() > 0) {
            next = CreditCompanyClaimStatus.PARTIALLY_SETTLED;
        } else {
            next = resubmitCount > 0 ? CreditCompanyClaimStatus.RESUBMITTED : CreditCompanyClaimStatus.SUBMITTED;
        }
        if (next == current) {
            return current;
        }
        if (current == CreditCompanyClaimStatus.SETTLED && aBillIsInAnotherOpenClaim) {
            return current;
        }
        return current.canSystemMoveTo(next) ? next : current;
    }

    /**
     * Called when a credit collection bill has been saved. For each paid bill that is in an
     * open claim, links the settlement to the claim and recomputes the claim status.
     * Does not change how payment is recorded.
     */
    public void recordSettlement(Bill settlementBill, List<BillItem> items, WebUser user) {
        if (settlementBill == null || settlementBill.getId() == null || items == null || items.isEmpty()) {
            return;
        }
        Date now = new Date();
        // claim id -> link rows to create (claimed bill, amount)
        Map<Long, List<Object[]>> byClaim = new java.util.TreeMap<>();
        for (BillItem item : items) {
            if (item == null || item.getReferenceBill() == null || item.getReferenceBill().getId() == null) {
                continue;
            }
            Long billId = item.getReferenceBill().getId();
            Map<String, Object> p = new HashMap<>();
            p.put("id", billId);
            p.put("open", Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
            List<?> claimIds = claimItemFacade.findLightsByJpqlStrict(
                    "select ci.claim.id from CreditCompanyClaimItem ci where ci.retired=false"
                    + " and ci.claim.retired=false and ci.claim.status in :open and ci.bill.id=:id", p);
            for (Object cid : claimIds) {
                byClaim.computeIfAbsent((Long) cid, k -> new ArrayList<>())
                        .add(new Object[]{item.getReferenceBill(), Math.abs(item.getNetValue())});
            }
        }
        // Claims are locked in id order so overlapping requests cannot deadlock.
        for (Map.Entry<Long, List<Object[]>> e : byClaim.entrySet()) {
            CreditCompanyClaim claim = claimFacade.findWithLock(e.getKey());
            if (claim == null || claim.isRetired()) {
                continue;
            }
            for (Object[] row : e.getValue()) {
                CreditCompanyClaimSettlement link = new CreditCompanyClaimSettlement();
                link.setClaim(claim);
                link.setSettlementBill(settlementBill);
                link.setClaimedBill((Bill) row[0]);
                link.setAmount((Double) row[1]);
                link.setCreater(user);
                link.setCreatedAt(now);
                settlementFacade.create(link);
            }
            recomputeStatus(claim);
        }
    }

    /**
     * Called when a credit collection bill has been cancelled: retires its link rows and
     * recomputes each affected claim.
     */
    public void reverseSettlement(Bill settlementBill, WebUser user) {
        if (settlementBill == null || settlementBill.getId() == null) {
            return;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("sb", settlementBill.getId());
        List<?> found = settlementFacade.findLightsByJpqlStrict(
                "select s.id, s.claim.id from CreditCompanyClaimSettlement s where s.retired=false"
                + " and s.settlementBill.id=:sb", p);
        Set<Long> claimIds = new TreeSet<>();
        List<Long> linkIds = new ArrayList<>();
        for (Object o : found) {
            Object[] r = (Object[]) o;
            linkIds.add((Long) r[0]);
            claimIds.add((Long) r[1]);
        }
        Date now = new Date();
        for (Long claimId : claimIds) {
            CreditCompanyClaim claim = claimFacade.findWithLock(claimId);
            if (claim == null) {
                continue;
            }
            for (Long linkId : linkIds) {
                CreditCompanyClaimSettlement link = settlementFacade.find(linkId);
                if (link != null && !link.isRetired() && link.getClaim() != null
                        && claimId.equals(link.getClaim().getId())) {
                    link.setRetired(true);
                    link.setRetiredAt(now);
                    link.setRetirer(user);
                    settlementFacade.edit(link);
                }
            }
            recomputeStatus(claim);
        }
    }

    /** Recomputes the status of a claim (already locked by the caller) from the bills' current due and the active links. */
    public CreditCompanyClaimStatus recomputeStatus(CreditCompanyClaim claim) {
        if (claim == null || claim.getStatus() == null || !claim.getStatus().followsSettlement()) {
            return claim == null ? null : claim.getStatus();
        }
        Map<String, Object> p = new HashMap<>();
        p.put("claim", claim.getId());
        // Scalar queries read the database, so the due is current.
        List<?> rows = claimItemFacade.findLightsByJpqlStrict(
                "select b.id, b.netTotal, coalesce(b.refundAmount,0), coalesce(b.paidAmount,0)"
                + " from CreditCompanyClaimItem ci join ci.bill b where ci.retired=false and ci.claim.id=:claim", p);
        List<BigDecimal> dues = new ArrayList<>();
        Set<Long> billIds = new HashSet<>();
        for (Object o : rows) {
            Object[] r = (Object[]) o;
            billIds.add((Long) r[0]);
            dues.add(dueDecimalOf(((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue(), ((Number) r[3]).doubleValue()));
        }
        List<?> sum = settlementFacade.findLightsByJpqlStrict(
                "select coalesce(sum(s.amount),0) from CreditCompanyClaimSettlement s"
                + " where s.retired=false and s.claim.id=:claim", p);
        BigDecimal linked = sum.isEmpty() || sum.get(0) == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(((Number) sum.get(0)).doubleValue());
        boolean inAnother = false;
        if (claim.getStatus() == CreditCompanyClaimStatus.SETTLED && !billIds.isEmpty()) {
            Map<String, Object> q = new HashMap<>();
            q.put("claim", claim.getId());
            q.put("ids", billIds);
            q.put("open", Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
            inAnother = claimItemFacade.findLongByJpqlStrict(
                    "select count(ci) from CreditCompanyClaimItem ci where ci.retired=false and ci.claim.retired=false"
                    + " and ci.claim.id<>:claim and ci.claim.status in :open and ci.bill.id in :ids", q) > 0;
        }
        CreditCompanyClaimStatus next = statusAfterSettlement(claim.getStatus(), claim.getResubmitCount(),
                dues, linked, inAnother);
        if (next != claim.getStatus()) {
            claim.setStatus(next);
            claimFacade.edit(claim);
        } else if (inAnother) {
            // Reached only when the settled claim would reopen but is left Settled.
            BigDecimal cutoff = BigDecimal.valueOf(MIN_DUE);
            boolean wouldReopen = dues.stream().anyMatch(d -> d.compareTo(cutoff) > 0);
            if (wouldReopen) {
                LOGGER.log(Level.WARNING, "Claim {0} left Settled: a settlement was cancelled but one of its bills is in another open claim.",
                        claim.getClaimNo());
            }
        }
        return claim.getStatus();
    }

    /** Sum of the active settlement links of the claim. */
    public double settledAmountOf(CreditCompanyClaim claim) {
        if (claim == null || claim.getId() == null) {
            return 0;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("claim", claim.getId());
        List<?> sum = settlementFacade.findLightsByJpqlStrict(
                "select coalesce(sum(s.amount),0) from CreditCompanyClaimSettlement s"
                + " where s.retired=false and s.claim.id=:claim", p);
        return sum.isEmpty() || sum.get(0) == null ? 0 : ((Number) sum.get(0)).doubleValue();
    }

    /** Sum of what is still due on the claim's bills (never below zero for a bill). */
    public double outstandingOf(List<CreditCompanyClaimItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (CreditCompanyClaimItem i : items) {
                BigDecimal d = dueDecimalOf(i.getBill());
                if (d.signum() > 0) {
                    total = total.add(d);
                }
            }
        }
        return total.doubleValue();
    }

    private CreditCompanyClaim reload(CreditCompanyClaim claim) {
        if (claim == null || claim.getId() == null) {
            throw new CreditCompanyClaimException("Claim not found.");
        }
        CreditCompanyClaim c = claimFacade.findWithLock(claim.getId());
        if (c == null || c.isRetired()) {
            throw new CreditCompanyClaimException("Claim not found.");
        }
        return c;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}

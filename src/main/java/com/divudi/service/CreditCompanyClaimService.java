package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CreditCompanyClaim;
import com.divudi.core.entity.CreditCompanyClaimItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.CreditCompanyClaimFacade;
import com.divudi.core.facade.CreditCompanyClaimItemFacade;
import com.divudi.ejb.BillNumberGenerator;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @EJB
    private BillService billService;

    /** Due = net total less refunds less settlements (as OPD Due Search). */
    public static double dueOf(Bill bill) {
        return bill.getNetTotal() - (bill.getRefundAmount() + bill.getPaidAmount());
    }

    /**
     * Checks one bill can go into a claim for the company. Does not check other claims.
     */
    public static void checkBillEligible(Bill bill, Institution company, Collection<BillTypeAtomic> allowedTypes) {
        if (bill == null) {
            throw new CreditCompanyClaimException("A selected bill was not found.");
        }
        String no = bill.getDeptId() != null ? bill.getDeptId() : String.valueOf(bill.getId());
        if (bill.isRetired() || bill.isCancelled()) {
            throw new CreditCompanyClaimException("Bill " + no + " is cancelled, so it cannot be claimed.");
        }
        if (bill.getPaymentMethod() != PaymentMethod.Credit || bill.getCreditCompany() == null
                || allowedTypes == null || !allowedTypes.contains(bill.getBillTypeAtomic())) {
            throw new CreditCompanyClaimException("Bill " + no + " is not an OPD credit bill.");
        }
        if (company == null || company.getId() == null || !company.getId().equals(bill.getCreditCompany().getId())) {
            throw new CreditCompanyClaimException("All bills in a claim must be for the same credit company (bill " + no + ").");
        }
        if (dueOf(bill) <= MIN_DUE) {
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
        if (company == null) {
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
        List<BillTypeAtomic> allowed = billService.fetchBillTypeAtomicsForOpdFinance();
        double total = 0;
        for (Bill b : bills) {
            checkBillEligible(b, company, allowed);
            total += dueOf(b);
        }

        Map<String, Object> p = new HashMap<>();
        p.put("ids", ids);
        p.put("open", java.util.Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
        long inOtherClaim = claimItemFacade.findLongByJpql(
                "select count(ci) from CreditCompanyClaimItem ci where ci.retired=false"
                + " and ci.claim.retired=false and ci.claim.status in :open and ci.bill.id in :ids", p);
        if (inOtherClaim > 0) {
            throw new CreditCompanyClaimException("A selected bill is already in another open claim.");
        }

        Date now = new Date();
        CreditCompanyClaim claim = new CreditCompanyClaim();
        claim.setCreditCompany(company);
        claim.setDepartment(department);
        claim.setInstitution(department.getInstitution());
        claim.setStatus(CreditCompanyClaimStatus.DRAFT);
        claim.setClaimedTotal(total);
        claim.setCreater(user);
        claim.setCreatedAt(now);
        claim.setClaimNo(billNumberGenerator.departmentBillNumberGeneratorYearly(
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
        return c;
    }

    private CreditCompanyClaim reload(CreditCompanyClaim claim) {
        if (claim == null || claim.getId() == null) {
            throw new CreditCompanyClaimException("Claim not found.");
        }
        CreditCompanyClaim c = claimFacade.find(claim.getId());
        if (c == null || c.isRetired()) {
            throw new CreditCompanyClaimException("Claim not found.");
        }
        return c;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}

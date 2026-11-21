package com.divudi.bean.credit;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.dto.CreditCompanyClaimRowDto;
import com.divudi.core.entity.CreditCompanyClaim;
import com.divudi.core.entity.CreditCompanyClaimItem;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.CreditCompanyClaimFacade;
import com.divudi.core.facade.CreditCompanyClaimItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.CreditCompanyClaimException;
import com.divudi.service.CreditCompanyClaimService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Credit company claims for OPD credit bills: list, view, create, submit,
 * record rejection and resubmit (#25847). Every action re-checks the privilege
 * on the server.
 */
@Named
@SessionScoped
public class CreditCompanyClaimController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private CreditCompanyClaimService claimService;
    @EJB
    private CreditCompanyClaimFacade claimFacade;
    @EJB
    private CreditCompanyClaimItemFacade claimItemFacade;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    // List filters
    private Institution filterCompany;
    private CreditCompanyClaimStatus filterStatus;
    private Date fromDate;
    private Date toDate;
    private List<CreditCompanyClaimRowDto> rows = new ArrayList<>();

    // View page
    private CreditCompanyClaim claim;
    private List<CreditCompanyClaimItem> claimItems = new ArrayList<>();
    private Date submittedOn;
    private String companyReference;
    private String rejectReason;

    public boolean hasAnyClaimPrivilege() {
        return webUserController.hasPrivilege("OpdCreditClaimCreate")
                || webUserController.hasPrivilege("OpdCreditClaimSubmit")
                || webUserController.hasPrivilege("OpdCreditClaimReject")
                || webUserController.hasPrivilege("OpdCreditClaimResubmit");
    }

    private boolean deny(String privilege) {
        if (webUserController.hasPrivilege(privilege)) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege for this action.");
        return true;
    }

    /** Claim numbers of open claims that hold the given bills, keyed by bill id. */
    public Map<Long, String> fetchOpenClaimNumbersByBillId(List<Long> billIds) {
        Map<Long, String> out = new HashMap<>();
        if (billIds == null || billIds.isEmpty()) {
            return out;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("ids", billIds);
        m.put("open", java.util.Arrays.asList(CreditCompanyClaimStatus.openStatuses()));
        List<?> found = claimItemFacade.findLightsByJpql(
                "select ci.bill.id, ci.claim.claimNo from CreditCompanyClaimItem ci where ci.retired=false"
                + " and ci.claim.retired=false and ci.claim.status in :open and ci.bill.id in :ids", m);
        if (found != null) {
            for (Object row : found) {
                Object[] r = (Object[]) row;
                out.put((Long) r[0], (String) r[1]);
            }
        }
        return out;
    }

    /** Called from OPD Due Search with the selected bills. */
    public String createClaimFromBills(List<Long> billIds, Institution company) {
        if (deny("OpdCreditClaimCreate")) {
            return null;
        }
        try {
            CreditCompanyClaim created = claimService.createClaim(billIds, company,
                    sessionController.getDepartment(), sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Claim " + created.getClaimNo() + " created.");
            return openClaim(created);
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public String navigateToClaims() {
        if (!hasAnyClaimPrivilege()) {
            JsfUtil.addErrorMessage("You do not have the privilege for this page.");
            return null;
        }
        fromDate = CommonFunctions.getStartOfMonth();
        toDate = CommonFunctions.getEndOfDay();
        filterCompany = null;
        filterStatus = null;
        rows = new ArrayList<>();
        return "/credit/credit_company_claims?faces-redirect=true";
    }

    public void searchClaims() {
        if (!hasAnyClaimPrivilege()) {
            rows = new ArrayList<>();
            JsfUtil.addErrorMessage("You do not have the privilege for this page.");
            return;
        }
        StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.dto.CreditCompanyClaimRowDto("
                + "c.id, c.claimNo, c.creditCompany.name, c.status, c.claimedTotal, c.createdAt, c.submittedAt,"
                + " c.companyReference, c.resubmitCount) from CreditCompanyClaim c"
                + " where c.retired=false and c.createdAt between :fd and :td");
        Map<String, Object> m = new HashMap<>();
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (filterCompany != null) {
            jpql.append(" and c.creditCompany.id=:cc");
            m.put("cc", filterCompany.getId());
        }
        if (filterStatus != null) {
            jpql.append(" and c.status=:st");
            m.put("st", filterStatus);
        }
        jpql.append(" order by c.createdAt desc");
        rows = (List<CreditCompanyClaimRowDto>) claimFacade.findLightsByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
    }

    public String viewClaim(Long claimId) {
        if (!hasAnyClaimPrivilege()) {
            JsfUtil.addErrorMessage("You do not have the privilege for this page.");
            return null;
        }
        CreditCompanyClaim c = claimId == null ? null : claimFacade.find(claimId);
        if (c == null) {
            JsfUtil.addErrorMessage("Claim not found.");
            return null;
        }
        return openClaim(c);
    }

    /** Back to the list from the claim page, refreshed so it shows the new status. */
    public String backToClaims() {
        if (!hasAnyClaimPrivilege()) {
            JsfUtil.addErrorMessage("You do not have the privilege for this page.");
            return null;
        }
        searchClaims();
        return "/credit/credit_company_claims?faces-redirect=true";
    }

    private String openClaim(CreditCompanyClaim c) {
        claim = c;
        submittedOn = new Date();
        companyReference = c.getCompanyReference();
        rejectReason = null;
        loadItems();
        return "/credit/credit_company_claim?faces-redirect=true";
    }

    private void loadItems() {
        Map<String, Object> m = new HashMap<>();
        m.put("claim", claim);
        claimItems = claimItemFacade.findByJpql(
                "select ci from CreditCompanyClaimItem ci where ci.retired=false and ci.claim=:claim order by ci.id", m);
    }

    private void reloadClaim() {
        claim = claimFacade.find(claim.getId());
        loadItems();
    }

    public void submit() {
        if (deny("OpdCreditClaimSubmit")) {
            return;
        }
        try {
            claimService.submit(claim, submittedOn, companyReference, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Claim submitted.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim();
    }

    public boolean isCancellable() {
        return claim != null && CreditCompanyClaimStatus.canCancel(claim.getStatus());
    }

    public void cancelClaim() {
        if (deny("OpdCreditClaimCreate")) {
            return;
        }
        try {
            claimService.cancel(claim, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Claim cancelled. Its bills are free again.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim();
    }

    public void reject() {
        if (deny("OpdCreditClaimReject")) {
            return;
        }
        try {
            claimService.reject(claim, rejectReason, sessionController.getLoggedUser());
            rejectReason = null;
            JsfUtil.addSuccessMessage("Rejection recorded.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim();
    }

    public void resubmit() {
        if (deny("OpdCreditClaimResubmit")) {
            return;
        }
        try {
            claimService.resubmit(claim, submittedOn, companyReference, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Claim resubmitted.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim();
    }

    public CreditCompanyClaimStatus[] getStatuses() {
        return CreditCompanyClaimStatus.values();
    }

    public boolean isDraft() {
        return claim != null && claim.getStatus() == CreditCompanyClaimStatus.DRAFT;
    }

    public boolean isRejected() {
        return claim != null && claim.getStatus() == CreditCompanyClaimStatus.REJECTED;
    }

    public boolean isAwaitingCompany() {
        return claim != null && (claim.getStatus() == CreditCompanyClaimStatus.SUBMITTED
                || claim.getStatus() == CreditCompanyClaimStatus.RESUBMITTED);
    }

    public double dueOf(CreditCompanyClaimItem item) {
        return CreditCompanyClaimService.dueOf(item.getBill());
    }

    public Institution getFilterCompany() {
        return filterCompany;
    }

    public void setFilterCompany(Institution filterCompany) {
        this.filterCompany = filterCompany;
    }

    public CreditCompanyClaimStatus getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(CreditCompanyClaimStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<CreditCompanyClaimRowDto> getRows() {
        return rows;
    }

    public CreditCompanyClaim getClaim() {
        return claim;
    }

    public List<CreditCompanyClaimItem> getClaimItems() {
        return claimItems;
    }

    public Date getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(Date submittedOn) {
        this.submittedOn = submittedOn;
    }

    public String getCompanyReference() {
        return companyReference;
    }

    public void setCompanyReference(String companyReference) {
        this.companyReference = companyReference;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}

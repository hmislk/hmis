package com.divudi.bean.credit;

import com.divudi.bean.common.CashRecieveBillController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.CreditCompanyClaimStatus;
import com.divudi.core.data.dto.CreditCompanyClaimRowDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CreditCompanyClaim;
import com.divudi.core.entity.CreditCompanyClaimItem;
import com.divudi.core.entity.CreditCompanyClaimSettlement;
import com.divudi.core.entity.CreditCompanyWriteOff;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.CreditCompanyClaimFacade;
import com.divudi.core.facade.CreditCompanyClaimItemFacade;
import com.divudi.core.facade.CreditCompanyClaimSettlementFacade;
import com.divudi.core.facade.CreditCompanyWriteOffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.CreditCompanyClaimException;
import com.divudi.service.CreditCompanyClaimService;
import com.divudi.service.CreditCompanyWriteOffService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
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
    @EJB
    private CreditCompanyClaimSettlementFacade settlementFacade;
    @EJB
    private CreditCompanyWriteOffService writeOffService;
    @EJB
    private CreditCompanyWriteOffFacade writeOffFacade;
    @Inject
    private CashRecieveBillController cashRecieveBillController;
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
    /** Id of the claim the page was rendered for; posted back with every action (the bean is shared by browser tabs). */
    private Long claimIdOnPage;
    /** Department the claim page was opened in; actions are refused after a department change (#25873). */
    private Long openedInDepartmentId;
    private List<CreditCompanyClaimItem> claimItems = new ArrayList<>();
    private List<CreditCompanyClaimSettlement> settlements = new ArrayList<>();
    private double settledAmount;
    private double outstandingAmount;
    private Date submittedOn;
    private String companyReference;
    private String rejectReason;
    private String closeReason;
    // Write-off request (#25847 part 3)
    private List<CreditCompanyWriteOff> claimWriteOffs = new ArrayList<>();
    private CreditCompanyClaimItem writeOffItem;
    /** Claim item id posted from the open write-off form, to match the form to writeOffItem. */
    private Long writeOffItemIdOnPage;
    private Double writeOffAmount;
    private String writeOffReason;

    public boolean hasAnyClaimPrivilege() {
        return webUserController.hasPrivilege("OpdCreditClaimCreate")
                || webUserController.hasPrivilege("OpdCreditClaimSubmit")
                || webUserController.hasPrivilege("OpdCreditClaimReject")
                || webUserController.hasPrivilege("OpdCreditClaimResubmit")
                || webUserController.hasPrivilege("OpdCreditClaimSettle")
                || webUserController.hasPrivilege("OpdCreditWriteOffRequest")
                || webUserController.hasPrivilege("OpdCreditWriteOffApprove");
    }

    private boolean deny(String privilege) {
        if (webUserController.hasPrivilege(privilege)) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege for this action.");
        return true;
    }

    /**
     * The claim this request was posted for, read fresh by the id posted with this
     * request, not by the session copy that another tab of the same login may have
     * changed meanwhile (#25873). Null, with a message, when the page is out of date
     * or the department was changed after the claim was opened.
     */
    private CreditCompanyClaim claimForAction() {
        Long posted = postedClaimId();
        CreditCompanyClaim held = claim;
        if (held == null || held.getId() == null || posted == null || !held.getId().equals(posted)) {
            JsfUtil.addErrorMessage("This page is out of date. Open the claim again.");
            return null;
        }
        Department current = sessionController.getDepartment();
        Long currentDepartmentId = current == null ? null : current.getId();
        if (!java.util.Objects.equals(openedInDepartmentId, currentDepartmentId)) {
            JsfUtil.addErrorMessage("The department was changed after this claim was opened. Open the claim again.");
            return null;
        }
        CreditCompanyClaim c = claimFacade.find(posted);
        if (c == null) {
            JsfUtil.addErrorMessage("Claim not found.");
            return null;
        }
        return c;
    }

    /** The claim id posted by this request's form, or the held one when the form did not post it. */
    private Long postedClaimId() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            String value = fc.getExternalContext().getRequestParameterMap().get("claimForm:claimIdOnPage");
            if (value != null) {
                try {
                    return Long.valueOf(value.trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return claimIdOnPage;
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
        claim = null;
        claimIdOnPage = null;
        openedInDepartmentId = null;
        claimItems = new ArrayList<>();
        settlements = new ArrayList<>();
        clearWriteOffForm();
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
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        searchClaims();
        return "/credit/credit_company_claims?faces-redirect=true";
    }

    private String openClaim(CreditCompanyClaim c) {
        claim = c;
        claimIdOnPage = c.getId();
        Department current = sessionController.getDepartment();
        openedInDepartmentId = current == null ? null : current.getId();
        submittedOn = new Date();
        companyReference = c.getCompanyReference();
        rejectReason = null;
        closeReason = null;
        clearWriteOffForm();
        loadItems();
        return "/credit/credit_company_claim?faces-redirect=true";
    }

    private void loadItems() {
        Map<String, Object> m = new HashMap<>();
        m.put("claim", claim);
        claimItems = claimItemFacade.findByJpql(
                "select ci from CreditCompanyClaimItem ci where ci.retired=false and ci.claim=:claim order by ci.id", m);
        settlements = settlementFacade.findByJpql(
                "select s from CreditCompanyClaimSettlement s where s.retired=false and s.claim=:claim"
                + " order by s.createdAt, s.id", m);
        settledAmount = claimService.settledAmountOf(claim);
        outstandingAmount = claimService.outstandingOf(claimItems);
        claimWriteOffs = new ArrayList<>();
        List<Long> billIds = new ArrayList<>();
        for (CreditCompanyClaimItem i : claimItems) {
            if (i.getBill() != null && i.getBill().getId() != null) {
                billIds.add(i.getBill().getId());
            }
        }
        if (!billIds.isEmpty()) {
            Map<String, Object> w = new HashMap<>();
            w.put("ids", billIds);
            claimWriteOffs = writeOffFacade.findByJpql("select w from CreditCompanyWriteOff w where w.retired=false"
                    + " and w.bill.id in :ids order by w.requestedAt desc, w.id desc", w);
        }
    }

    // ---- Write-off request (#25847 part 3) ----

    /** True when write-offs can be requested from this claim page. */
    public boolean isWriteOffAllowed() {
        return claim != null && CreditCompanyWriteOffService.claimAllowsRequest(claim.getStatus())
                && webUserController.hasPrivilege("OpdCreditWriteOffRequest");
    }

    public boolean isWriteOffFormOpen() {
        return writeOffItem != null;
    }

    /** Opens the request form for one bill of the claim; amount defaults to the bill's due. */
    public void startWriteOff(Long itemId) {
        if (deny("OpdCreditWriteOffRequest")) {
            return;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return;
        }
        reloadClaim(c);
        clearWriteOffForm();
        if (!CreditCompanyWriteOffService.claimAllowsRequest(claim.getStatus())) {
            JsfUtil.addErrorMessage("A write-off can be requested only for a claim that is submitted, resubmitted or partially settled.");
            return;
        }
        for (CreditCompanyClaimItem i : claimItems) {
            if (i.getId() != null && i.getId().equals(itemId)) {
                writeOffItem = i;
                writeOffAmount = CreditCompanyClaimService.dueDecimalOf(i.getBill()).doubleValue();
                return;
            }
        }
        JsfUtil.addErrorMessage("Bill not found in this claim.");
    }

    public void cancelWriteOffForm() {
        clearWriteOffForm();
    }

    private void clearWriteOffForm() {
        writeOffItem = null;
        writeOffAmount = null;
        writeOffReason = null;
    }

    public void requestWriteOff() {
        if (deny("OpdCreditWriteOffRequest")) {
            return;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return;
        }
        if (writeOffItem == null || writeOffItem.getBill() == null) {
            JsfUtil.addErrorMessage("Choose the bill to write off.");
            return;
        }
        if (writeOffItem.getClaim() == null || !c.getId().equals(writeOffItem.getClaim().getId())) {
            JsfUtil.addErrorMessage("This form is out of date. Open the write-off request again.");
            return;
        }
        if (writeOffItemIdOnPage == null || !writeOffItemIdOnPage.equals(writeOffItem.getId())) {
            JsfUtil.addErrorMessage("This form is out of date. Open the write-off request again.");
            return;
        }
        try {
            writeOffService.request(writeOffItem.getBill().getId(), writeOffAmount == null ? 0 : writeOffAmount,
                    writeOffReason, sessionController.getLoggedUser(), true);
            JsfUtil.addSuccessMessage("Write-off requested. A different user with the approve privilege must approve it.");
            clearWriteOffForm();
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim(c);
    }

    public List<CreditCompanyWriteOff> getClaimWriteOffs() {
        return claimWriteOffs;
    }

    public Long getWriteOffItemIdOnPage() {
        return writeOffItem == null ? null : writeOffItem.getId();
    }

    public void setWriteOffItemIdOnPage(Long writeOffItemIdOnPage) {
        this.writeOffItemIdOnPage = writeOffItemIdOnPage;
    }

    public CreditCompanyClaimItem getWriteOffItem() {
        return writeOffItem;
    }

    public Double getWriteOffAmount() {
        return writeOffAmount;
    }

    public void setWriteOffAmount(Double writeOffAmount) {
        this.writeOffAmount = writeOffAmount;
    }

    public String getWriteOffReason() {
        return writeOffReason;
    }

    public void setWriteOffReason(String writeOffReason) {
        this.writeOffReason = writeOffReason;
    }

    /** Opens Outpatient Credit Collection with the claim's bills that still have a due pre-added. */
    public String settleClaim() {
        if (deny("OpdCreditClaimSettle")) {
            return null;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return null;
        }
        reloadClaim(c);
        if (!isSettleable()) {
            JsfUtil.addErrorMessage("Only a submitted, resubmitted or partially settled claim can be settled.");
            return null;
        }
        List<Bill> bills = new ArrayList<>();
        for (CreditCompanyClaimItem i : claimItems) {
            if (i.getBill() != null && CreditCompanyClaimService.dueDecimalOf(i.getBill())
                    .compareTo(java.math.BigDecimal.valueOf(CreditCompanyClaimService.MIN_DUE)) > 0) {
                bills.add(i.getBill());
            }
        }
        if (bills.isEmpty()) {
            JsfUtil.addErrorMessage("No bill in this claim has a due amount.");
            return null;
        }
        return cashRecieveBillController.navigateWithPreloadedBills(bills);
    }

    public boolean isSettleable() {
        return claim != null && (claim.getStatus() == CreditCompanyClaimStatus.SUBMITTED
                || claim.getStatus() == CreditCompanyClaimStatus.RESUBMITTED
                || claim.getStatus() == CreditCompanyClaimStatus.PARTIALLY_SETTLED);
    }

    public List<CreditCompanyClaimSettlement> getSettlements() {
        return settlements;
    }

    public double getSettledAmount() {
        return settledAmount;
    }

    public double getOutstandingAmount() {
        return outstandingAmount;
    }

    private void reloadClaim(CreditCompanyClaim c) {
        CreditCompanyClaim fresh = claimFacade.find(c.getId());
        claim = fresh == null ? c : fresh;
        claimIdOnPage = claim.getId();
        loadItems();
    }

    public void submit() {
        if (deny("OpdCreditClaimSubmit")) {
            return;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return;
        }
        try {
            claimService.submit(c, submittedOn, companyReference, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Claim submitted.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim(c);
    }

    public boolean isCancellable() {
        return claim != null && CreditCompanyClaimStatus.canCancel(claim.getStatus());
    }

    public void cancelClaim() {
        if (deny("OpdCreditClaimCreate")) {
            return;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return;
        }
        try {
            claimService.cancel(c, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Claim cancelled. Its bills are free again.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim(c);
    }

    public void reject() {
        if (deny("OpdCreditClaimReject")) {
            return;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return;
        }
        try {
            claimService.reject(c, rejectReason, sessionController.getLoggedUser());
            rejectReason = null;
            JsfUtil.addSuccessMessage("Rejection recorded.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim(c);
    }

    public void closePartial() {
        if (deny("OpdCreditClaimCreate")) {
            return;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return;
        }
        try {
            claimService.closePartiallySettled(c, closeReason, sessionController.getLoggedUser());
            closeReason = null;
            JsfUtil.addSuccessMessage("Claim closed. Its bills are free; what is unpaid stays due.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim(c);
    }

    public boolean isPartiallySettled() {
        return claim != null && claim.getStatus() == CreditCompanyClaimStatus.PARTIALLY_SETTLED;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public void resubmit() {
        if (deny("OpdCreditClaimResubmit")) {
            return;
        }
        CreditCompanyClaim c = claimForAction();
        if (c == null) {
            return;
        }
        try {
            claimService.resubmit(c, submittedOn, companyReference, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Claim resubmitted.");
        } catch (CreditCompanyClaimException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        }
        reloadClaim(c);
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

    public Long getClaimIdOnPage() {
        return claimIdOnPage;
    }

    public void setClaimIdOnPage(Long claimIdOnPage) {
        this.claimIdOnPage = claimIdOnPage;
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

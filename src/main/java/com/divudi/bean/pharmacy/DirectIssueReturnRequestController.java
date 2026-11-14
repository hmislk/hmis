package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.Privileges;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyCalculation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Return request for a direct issue to an inpatient (#25506, policy #25503).
 *
 * <p>A direct issue is cancelled or returned only by the department that issued
 * it. Any other department (typically the ward) raises a return request here;
 * the issuing pharmacy approves or rejects it. Raising a request changes no
 * stock and no bill value: the request is a
 * {@link BillTypeAtomic#REQUEST_DIRECT_ISSUE_RETURN_INWARD} bill whose items
 * point to the direct-issue bill items through {@code referanceBillItem} and
 * carry only the requested quantity (no pharmaceutical bill item, so no
 * quantity-netting query over returns counts it).</p>
 *
 * <p>Status of a request bill: pending = neither completed nor cancelled;
 * approved = {@code completed} (set when the issuing department settles the
 * return through {@link BhtIssueReturnController}, which also links the return
 * bill in {@code forwardReferenceBill}); rejected = {@code cancelled} with
 * {@code completedBy} set; withdrawn by the requester = {@code cancelled}
 * without {@code completedBy}.</p>
 */
@Named
@SessionScoped
public class DirectIssueReturnRequestController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final List<BillTypeAtomic> DIRECT_ISSUE_ATOMICS = Arrays.asList(
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE);

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @Inject
    private PharmacyCalculation pharmacyCalculation;
    @EJB
    private BillNumberGenerator billNumberBean;

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private BhtIssueReturnController bhtIssueReturnController;

    // Ward side
    private boolean allPatients;
    private PatientEncounter patientEncounter;
    private List<Bill> issueBills = new ArrayList<>();
    private Bill selectedIssueBill;
    private List<RequestLine> lines = new ArrayList<>();
    private String comment;
    private Bill submittedRequest;
    private boolean printPreview;
    private boolean submitting;
    private List<Bill> recentRequests = new ArrayList<>();

    // Pharmacy side
    private List<Bill> pendingRequests = new ArrayList<>();
    private Bill selectedRequest;
    private List<BillItem> selectedRequestItems = new ArrayList<>();
    private String rejectComment;

    /**
     * One line of a request: the issued bill item with its quantities and the
     * quantity the user asks to return.
     */
    public static class RequestLine implements Serializable {

        private static final long serialVersionUID = 1L;

        private BillItem issueItem;
        private double issuedQty;
        private double returnedQty;
        private double pendingRequestedQty;
        private double requestableQty;
        private double requestedQty;

        public BillItem getIssueItem() {
            return issueItem;
        }

        public void setIssueItem(BillItem issueItem) {
            this.issueItem = issueItem;
        }

        public double getIssuedQty() {
            return issuedQty;
        }

        public void setIssuedQty(double issuedQty) {
            this.issuedQty = issuedQty;
        }

        public double getReturnedQty() {
            return returnedQty;
        }

        public void setReturnedQty(double returnedQty) {
            this.returnedQty = returnedQty;
        }

        public double getPendingRequestedQty() {
            return pendingRequestedQty;
        }

        public void setPendingRequestedQty(double pendingRequestedQty) {
            this.pendingRequestedQty = pendingRequestedQty;
        }

        public double getRequestableQty() {
            return requestableQty;
        }

        public void setRequestableQty(double requestableQty) {
            this.requestableQty = requestableQty;
        }

        public double getRequestedQty() {
            return requestedQty;
        }

        public void setRequestedQty(double requestedQty) {
            this.requestedQty = requestedQty;
        }
    }

    // ------------------------------------------------------------------
    // Ward side: raise a request
    // ------------------------------------------------------------------
    /**
     * One patient: from the Inpatient Dashboard.
     */
    public String navigateToRequestForEncounter(PatientEncounter encounter) {
        if (encounter == null || encounter.getId() == null) {
            JsfUtil.addErrorMessage("Select an admission first.");
            return null;
        }
        resetWardState();
        allPatients = false;
        patientEncounter = encounter;
        loadIssueBills();
        loadRecentRequests();
        return "/ward/ward_direct_issue_return_request?faces-redirect=true";
    }

    /**
     * All patients of the logged department: from the Nursing Dashboard and
     * the main menu.
     */
    public String navigateToRequestAllPatients() {
        resetWardState();
        allPatients = true;
        loadIssueBills();
        loadRecentRequests();
        return "/ward/ward_direct_issue_return_request?faces-redirect=true";
    }

    public String navigateToNewRequest() {
        if (allPatients) {
            return navigateToRequestAllPatients();
        }
        return navigateToRequestForEncounter(patientEncounter);
    }

    private void resetWardState() {
        patientEncounter = null;
        issueBills = new ArrayList<>();
        selectedIssueBill = null;
        lines = new ArrayList<>();
        comment = null;
        submittedRequest = null;
        printPreview = false;
        recentRequests = new ArrayList<>();
    }

    /**
     * Non-cancelled direct issues that still have a quantity that can be
     * requested back. For all patients: admissions currently in the logged
     * department that are neither discharged nor nursing-discharged. The
     * candidate query is capped so the netting queries stay bounded.
     */
    public void loadIssueBills() {
        issueBills = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("btas", DIRECT_ISSUE_ATOMICS);
        String jpql;
        if (allPatients) {
            if (sessionController.getDepartment() == null) {
                return;
            }
            jpql = "SELECT b FROM Bill b JOIN b.patientEncounter pe "
                    + "WHERE b.billTypeAtomic IN :btas "
                    + "AND b.cancelled = false "
                    + "AND (b.retired = false OR b.retired IS NULL) "
                    + "AND (pe.discharged = false OR pe.discharged IS NULL) "
                    + "AND (pe.nursingDischarged = false OR pe.nursingDischarged IS NULL) "
                    + "AND pe.currentPatientRoom.roomFacilityCharge.department = :dept "
                    + "ORDER BY b.createdAt DESC";
            params.put("dept", sessionController.getDepartment());
        } else {
            if (patientEncounter == null || patientEncounter.getId() == null) {
                return;
            }
            jpql = "SELECT b FROM Bill b "
                    + "WHERE b.billTypeAtomic IN :btas "
                    + "AND b.patientEncounter.id = :encId "
                    + "AND b.cancelled = false "
                    + "AND (b.retired = false OR b.retired IS NULL) "
                    + "ORDER BY b.createdAt DESC";
            params.put("encId", patientEncounter.getId());
        }
        List<Bill> candidates = billFacade.findByJpql(jpql, params, 300);
        for (Bill b : candidates) {
            if (hasRequestableBalance(b)) {
                issueBills.add(b);
            }
        }
    }

    private boolean hasRequestableBalance(Bill issueBill) {
        for (RequestLine line : buildLines(issueBill)) {
            if (line.getRequestableQty() > 0.0001) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requestable quantity of an issued line = issued - already returned -
     * quantity in other open (pending) requests, floored at zero. A request can
     * therefore never ask for more than the line can still give back.
     */
    private List<RequestLine> buildLines(Bill issueBill) {
        List<RequestLine> result = new ArrayList<>();
        if (issueBill == null || issueBill.getId() == null) {
            return result;
        }
        for (PharmaceuticalBillItem pbi : pharmaceuticalBillItemFacade.getPharmaceuticalBillItems(issueBill)) {
            BillItem issueItem = pbi.getBillItem();
            if (issueItem == null) {
                continue;
            }
            double issued = Math.abs(pbi.getQtyInUnit());
            double returned = Math.abs(pharmacyCalculation.getTotalQty(issueItem, issueBill.getBillType()));
            double pending = fetchPendingRequestedQty(issueItem);
            RequestLine line = new RequestLine();
            line.setIssueItem(issueItem);
            line.setIssuedQty(issued);
            line.setReturnedQty(returned);
            line.setPendingRequestedQty(pending);
            line.setRequestableQty(Math.max(0.0, issued - returned - pending));
            line.setRequestedQty(0.0);
            result.add(line);
        }
        return result;
    }

    private double fetchPendingRequestedQty(BillItem issueItem) {
        if (issueItem == null || issueItem.getId() == null) {
            return 0.0;
        }
        String jpql = "SELECT SUM(bi.qty) FROM BillItem bi "
                + "WHERE bi.referanceBillItem.id = :itemId "
                + "AND bi.bill.billTypeAtomic = :bta "
                + "AND bi.bill.cancelled = false "
                + "AND bi.bill.completed = false "
                + "AND (bi.bill.retired = false OR bi.bill.retired IS NULL)";
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", issueItem.getId());
        params.put("bta", BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD);
        return Math.abs(billItemFacade.findDoubleByJpql(jpql, params));
    }

    public void selectIssueBill(Bill issueBill) {
        selectedIssueBill = issueBill;
        lines = buildLines(issueBill);
        if (lines.isEmpty()) {
            JsfUtil.addErrorMessage("No items on this bill.");
        }
    }

    public void submit() {
        if (!webUserController.hasPrivilege(Privileges.InwardPharmacyReturnSubmit.name())) {
            JsfUtil.addErrorMessage("You do not have the privilege to request a return.");
            return;
        }
        if (submitting) {
            return;
        }
        submitting = true;
        try {
            doSubmit();
        } finally {
            submitting = false;
        }
    }

    private void doSubmit() {
        printPreview = false;
        if (selectedIssueBill == null || selectedIssueBill.getId() == null) {
            JsfUtil.addErrorMessage("Select an issue to request a return for.");
            return;
        }
        Bill issue = billFacade.findWithoutCache(selectedIssueBill.getId());
        if (issue == null || issue.isCancelled() || issue.isRetired()) {
            JsfUtil.addErrorMessage("This issue is no longer available.");
            return;
        }
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Give the reason for the return request.");
            return;
        }
        PatientEncounter enc = issue.getPatientEncounter();
        if (enc == null) {
            JsfUtil.addErrorMessage("The issue has no admission.");
            return;
        }
        if (enc.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot request a return: nursing discharge has already been confirmed for this patient.");
            return;
        }
        if (enc.isDischarged() || enc.isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Cannot request a return: the patient is discharged.");
            return;
        }

        // Re-read the balances: another request or return may have happened since the page loaded.
        Map<Long, RequestLine> fresh = new HashMap<>();
        for (RequestLine l : buildLines(issue)) {
            fresh.put(l.getIssueItem().getId(), l);
        }
        List<RequestLine> toRequest = new ArrayList<>();
        for (RequestLine line : lines) {
            double qty = line.getRequestedQty();
            if (qty < 0) {
                JsfUtil.addErrorMessage("A return quantity cannot be negative.");
                return;
            }
            if (qty <= 0.0) {
                continue;
            }
            RequestLine latest = fresh.get(line.getIssueItem().getId());
            double max = latest == null ? 0.0 : latest.getRequestableQty();
            if (qty > max + 0.0001) {
                JsfUtil.addErrorMessage("Only " + max + " of " + line.getIssueItem().getItem().getName()
                        + " can be requested for return now.");
                return;
            }
            toRequest.add(line);
        }
        if (toRequest.isEmpty()) {
            JsfUtil.addErrorMessage("Enter a return quantity for at least one item.");
            return;
        }

        Department wardDept = sessionController.getDepartment();
        BilledBill request = new BilledBill();
        request.setBillType(BillType.InwardDirectIssueReturnRequest);
        request.setBillTypeAtomic(BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD);
        request.setInstitution(sessionController.getInstitution());
        request.setDepartment(wardDept);
        request.setFromDepartment(wardDept);
        request.setToDepartment(issue.getDepartment());
        request.setPatient(issue.getPatient());
        request.setPatientEncounter(enc);
        request.setReferenceBill(issue);
        request.setComments(comment.trim());
        request.setCreatedAt(new Date());
        request.setCreater(sessionController.getLoggedUser());
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(wardDept,
                BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD, "DIRQ");
        request.setDeptId(deptId);
        request.setInsId(deptId);
        billFacade.create(request);

        List<BillItem> saved = new ArrayList<>();
        int serial = 1;
        for (RequestLine line : toRequest) {
            BillItem bi = new BillItem();
            bi.setBill(request);
            bi.setItem(line.getIssueItem().getItem());
            bi.setQty(line.getRequestedQty());
            bi.setReferanceBillItem(line.getIssueItem());
            bi.setPatientEncounter(enc);
            bi.setSearialNo(serial++);
            bi.setCreatedAt(new Date());
            bi.setCreater(sessionController.getLoggedUser());
            billItemFacade.create(bi);
            saved.add(bi);
        }
        request.setBillItems(saved);
        submittedRequest = request;
        printPreview = true;
        JsfUtil.addSuccessMessage("Return request sent to the issuing pharmacy. No stock or bill changed until it is approved.");
    }

    public void loadRecentRequests() {
        recentRequests = new ArrayList<>();
        if (sessionController.getDepartment() == null) {
            return;
        }
        String jpql = "SELECT b FROM Bill b WHERE b.billTypeAtomic = :bta "
                + "AND b.fromDepartment = :dept "
                + "AND (b.retired = false OR b.retired IS NULL) "
                + "ORDER BY b.createdAt DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD);
        params.put("dept", sessionController.getDepartment());
        recentRequests = billFacade.findByJpql(jpql, params, 100);
    }

    /**
     * The ward withdraws its own request while it is still pending.
     */
    public void withdraw(Bill request) {
        if (!webUserController.hasPrivilege(Privileges.InwardPharmacyReturnSubmit.name())) {
            JsfUtil.addErrorMessage("You do not have the privilege to withdraw this request.");
            return;
        }
        Bill fresh = request == null || request.getId() == null ? null : billFacade.findWithoutCache(request.getId());
        if (fresh == null || fresh.getBillTypeAtomic() != BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD) {
            JsfUtil.addErrorMessage("Request not found.");
            return;
        }
        if (fresh.isCompleted() || fresh.isCancelled()) {
            JsfUtil.addErrorMessage("This request has already been handled by the pharmacy.");
            loadRecentRequests();
            return;
        }
        fresh.setCancelled(true);
        fresh.setComments(appendNote(fresh.getComments(), "Withdrawn by the ward"));
        billFacade.edit(fresh);
        JsfUtil.addSuccessMessage("Request withdrawn.");
        loadRecentRequests();
        loadIssueBills();
    }

    // ------------------------------------------------------------------
    // Pharmacy side: approve or reject
    // ------------------------------------------------------------------
    public String navigateToPendingRequests() {
        selectedRequest = null;
        selectedRequestItems = new ArrayList<>();
        rejectComment = null;
        loadPendingRequests();
        return "/pharmacy/pharmacy_direct_issue_return_request_list?faces-redirect=true";
    }

    public void loadPendingRequests() {
        pendingRequests = new ArrayList<>();
        if (sessionController.getDepartment() == null) {
            return;
        }
        String jpql = "SELECT b FROM Bill b WHERE b.billTypeAtomic = :bta "
                + "AND b.toDepartment = :dept "
                + "AND b.cancelled = false AND b.completed = false "
                + "AND (b.retired = false OR b.retired IS NULL) "
                + "ORDER BY b.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD);
        params.put("dept", sessionController.getDepartment());
        pendingRequests = billFacade.findByJpql(jpql, params, 300);
    }

    public void viewRequest(Bill request) {
        selectedRequest = request;
        selectedRequestItems = new ArrayList<>();
        rejectComment = null;
        if (request == null || request.getId() == null) {
            return;
        }
        String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill.id = :billId "
                + "AND (bi.retired = false OR bi.retired IS NULL) ORDER BY bi.id";
        Map<String, Object> params = new HashMap<>();
        params.put("billId", request.getId());
        selectedRequestItems = billItemFacade.findByJpql(jpql, params);
    }

    /**
     * Approving opens the existing direct-issue return page of the issuing
     * department with the requested quantities pre-filled (capped by the
     * balance); settling it there records the return and completes this request.
     */
    public String approve(Bill request) {
        if (!webUserController.hasPrivilege(Privileges.PharmacyBHTIssueAccept.name())) {
            JsfUtil.addErrorMessage("You do not have the privilege to approve this request.");
            return null;
        }
        Bill fresh = request == null || request.getId() == null ? null : billFacade.findWithoutCache(request.getId());
        if (fresh == null || fresh.getBillTypeAtomic() != BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD) {
            JsfUtil.addErrorMessage("Request not found.");
            return null;
        }
        if (fresh.isCompleted() || fresh.isCancelled()) {
            JsfUtil.addErrorMessage("This request has already been handled.");
            loadPendingRequests();
            return null;
        }
        Bill issue = fresh.getReferenceBill();
        if (issue == null || issue.isCancelled()) {
            JsfUtil.addErrorMessage("The issue this request refers to is no longer valid. Reject the request.");
            return null;
        }
        return bhtIssueReturnController.navigateToReturnForRequest(issue, fresh);
    }

    public void reject() {
        if (!webUserController.hasPrivilege(Privileges.PharmacyBHTIssueAccept.name())) {
            JsfUtil.addErrorMessage("You do not have the privilege to reject this request.");
            return;
        }
        if (rejectComment == null || rejectComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Give the reason for rejecting the request.");
            return;
        }
        Bill fresh = selectedRequest == null || selectedRequest.getId() == null
                ? null : billFacade.findWithoutCache(selectedRequest.getId());
        if (fresh == null || fresh.getBillTypeAtomic() != BillTypeAtomic.REQUEST_DIRECT_ISSUE_RETURN_INWARD) {
            JsfUtil.addErrorMessage("Request not found.");
            return;
        }
        if (fresh.isCompleted() || fresh.isCancelled()) {
            JsfUtil.addErrorMessage("This request has already been handled.");
            loadPendingRequests();
            return;
        }
        fresh.setCancelled(true);
        fresh.setCompletedBy(sessionController.getLoggedUser());
        fresh.setCompletedAt(new Date());
        fresh.setComments(appendNote(fresh.getComments(), "Rejected by pharmacy: " + rejectComment.trim()));
        billFacade.edit(fresh);
        selectedRequest = null;
        selectedRequestItems = new ArrayList<>();
        rejectComment = null;
        JsfUtil.addSuccessMessage("Request rejected.");
        loadPendingRequests();
    }

    private String appendNote(String existing, String note) {
        if (existing == null || existing.trim().isEmpty()) {
            return note;
        }
        return existing + " | " + note;
    }

    /**
     * Pending / Approved / Rejected / Withdrawn, as described on the class.
     */
    public String statusOf(Bill request) {
        if (request == null) {
            return "";
        }
        if (request.isCompleted()) {
            return "Approved";
        }
        if (request.isCancelled()) {
            return request.getCompletedBy() != null ? "Rejected" : "Withdrawn";
        }
        return "Pending";
    }

    public boolean isPending(Bill request) {
        return request != null && !request.isCompleted() && !request.isCancelled();
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------
    public boolean isAllPatients() {
        return allPatients;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public List<Bill> getIssueBills() {
        return issueBills;
    }

    public Bill getSelectedIssueBill() {
        return selectedIssueBill;
    }

    public List<RequestLine> getLines() {
        return lines;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Bill getSubmittedRequest() {
        return submittedRequest;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public List<Bill> getRecentRequests() {
        return recentRequests;
    }

    public List<Bill> getPendingRequests() {
        return pendingRequests;
    }

    public Bill getSelectedRequest() {
        return selectedRequest;
    }

    public List<BillItem> getSelectedRequestItems() {
        return selectedRequestItems;
    }

    public String getRejectComment() {
        return rejectComment;
    }

    public void setRejectComment(String rejectComment) {
        this.rejectComment = rejectComment;
    }
}

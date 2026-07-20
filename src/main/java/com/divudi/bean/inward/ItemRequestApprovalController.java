/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.inward.ItemRequestApiService;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSF controller backing the department-level Item/Service Request approval
 * queue (issue #21793). Lists pending {@link BillType#InwardServiceItemRequest}
 * bills routed to the logged-in user's current department and lets the user
 * Approve (charges the BHT / deducts stock via {@link ItemRequestApiService})
 * or Reject (records a reason) each request.
 *
 * @author Claude AI Assistant
 */
@Named("itemRequestApprovalController")
@SessionScoped
public class ItemRequestApprovalController implements Serializable {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @Inject
    private ItemRequestApiService itemRequestApiService;

    @Inject
    private com.divudi.bean.inward.BillBhtController billBhtController;

    @Inject
    private com.divudi.bean.pharmacy.InpatientDirectIssueNativeSqlController inpatientDirectIssueNativeSqlController;

    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    private List<Bill> pendingRequests;
    private Bill selectedRequest;
    private String rejectionReason;

    /**
     * Per-request-id cache of the remaining (still-pending) service and
     * inventory lines, populated once per {@link #loadPendingRequests()} call.
     * The pending queue page's datatable and per-row button {@code rendered}
     * attributes each re-evaluate these lists via EL, so without caching a
     * page with N requests x M lines re-runs the JPQL fulfillment check
     * (isLineFulfilled) several times per line per render.
     */
    private final Map<Long, List<BillItem>> remainingServiceLinesCache = new HashMap<>();
    private final Map<Long, List<BillItem>> remainingInventoryLinesCache = new HashMap<>();

    /**
     * Navigation method: loads the pending-request list for the current
     * department and returns the redirect outcome. Initialization happens
     * here (not in a f:viewAction) because this bean is @SessionScoped.
     */
    public String navigateToPendingRequests() {
        loadPendingRequests();
        return "/inward/item_request_pending_list?faces-redirect=true";
    }

    public void loadPendingRequests() {
        remainingServiceLinesCache.clear();
        remainingInventoryLinesCache.clear();

        Map<String, Object> params = new HashMap<>();
        String jpql = "select b from Bill b where b.retired=false and b.toDepartment=:toDep "
                + "and b.billType=:bTp and b.cancelled=false "
                + "order by b.createdAt desc";
        params.put("toDep", sessionController.getDepartment());
        params.put("bTp", BillType.InwardServiceItemRequest);

        List<Bill> results = billFacade.findByJpql(jpql, params);
        List<Bill> withRemainingLines = new ArrayList<>();
        for (Bill candidate : results != null ? results : new ArrayList<Bill>()) {
            if (!getRemainingServiceLines(candidate).isEmpty() || !getRemainingInventoryLines(candidate).isEmpty()) {
                withRemainingLines.add(candidate);
            }
        }
        pendingRequests = withRemainingLines;
    }

    public String processServices(Bill request) {
        if (!webUserController.hasPrivilege("InwardServiceItemRequestApproval")) {
            JsfUtil.addErrorMessage("You do not have privileges to process item/service requests.");
            return null;
        }
        if (request == null || !belongsToCurrentDepartment(request)) {
            JsfUtil.addErrorMessage("This request belongs to another department's queue.");
            loadPendingRequests();
            return null;
        }
        List<BillItem> remaining = getRemainingServiceLines(request);
        if (remaining.isEmpty()) {
            JsfUtil.addErrorMessage("No remaining service/investigation lines on this request.");
            return null;
        }
        return billBhtController.navigateToAddServicesFromItemRequest(request, remaining);
    }

    public String processInventory(Bill request) {
        if (!webUserController.hasPrivilege("InwardServiceItemRequestApproval")) {
            JsfUtil.addErrorMessage("You do not have privileges to process item/service requests.");
            return null;
        }
        if (request == null || !belongsToCurrentDepartment(request)) {
            JsfUtil.addErrorMessage("This request belongs to another department's queue.");
            loadPendingRequests();
            return null;
        }
        List<BillItem> remaining = getRemainingInventoryLines(request);
        if (remaining.isEmpty()) {
            JsfUtil.addErrorMessage("No remaining inventory lines on this request.");
            return null;
        }
        return inpatientDirectIssueNativeSqlController.navigateToDirectIssueFromItemRequest(request, remaining);
    }

    public void reject(Bill request, String reason) {
        if (!webUserController.hasPrivilege("InwardServiceItemRequestRejection")) {
            JsfUtil.addErrorMessage("You do not have privileges to reject item/service requests.");
            return;
        }
        if (request == null || request.getId() == null) {
            JsfUtil.addErrorMessage("No request selected.");
            return;
        }
        if (reason == null || reason.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Rejection reason is required");
            return;
        }
        if (!belongsToCurrentDepartment(request)) {
            JsfUtil.addErrorMessage("This request belongs to another department's queue.");
            loadPendingRequests();
            return;
        }
        List<BillItem> remaining = new ArrayList<>();
        remaining.addAll(getRemainingServiceLines(request));
        remaining.addAll(getRemainingInventoryLines(request));
        if (remaining.isEmpty()) {
            JsfUtil.addErrorMessage("This request has no remaining lines to reject.");
            return;
        }
        try {
            itemRequestApiService.rejectRemainingLines(request.getId(), remaining, reason, sessionController.getLoggedUser(), sessionController.getDepartment());
            JsfUtil.addSuccessMessage("Remaining lines rejected.");
        } catch (IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Rejection failed: " + e.getMessage());
        }
        setRejectionReason(null);
        loadPendingRequests();
    }

    /**
     * Fast-fail UI guard; the authoritative department scope check is enforced
     * again inside {@link ItemRequestApiService} before any status change.
     */
    private boolean belongsToCurrentDepartment(Bill request) {
        return sessionController.getDepartment() != null
                && sessionController.getDepartment().getId() != null
                && request.getToDepartment() != null
                && sessionController.getDepartment().getId().equals(request.getToDepartment().getId());
    }

    /**
     * Line items (requested items/services) for a given request bill, used by
     * the nested datatable in the approval queue page.
     */
    public List<BillItem> getRequestLines(Bill request) {
        if (request == null || request.getId() == null) {
            return new ArrayList<>();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("b", request);
        List<BillItem> lines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.bill=:b", params);
        return lines != null ? lines : new ArrayList<BillItem>();
    }

    public List<BillItem> getRemainingServiceLines(Bill request) {
        computeRemainingLinesIfAbsent(request);
        return request != null && request.getId() != null
                ? remainingServiceLinesCache.get(request.getId()) : new ArrayList<BillItem>();
    }

    public List<BillItem> getRemainingInventoryLines(Bill request) {
        computeRemainingLinesIfAbsent(request);
        return request != null && request.getId() != null
                ? remainingInventoryLinesCache.get(request.getId()) : new ArrayList<BillItem>();
    }

    /**
     * Splits a request's still-pending lines into service/investigation vs.
     * inventory buckets in a single pass and caches both, keyed by request id.
     */
    private void computeRemainingLinesIfAbsent(Bill request) {
        if (request == null || request.getId() == null || remainingServiceLinesCache.containsKey(request.getId())) {
            return;
        }
        List<BillItem> remainingServices = new ArrayList<>();
        List<BillItem> remainingInventory = new ArrayList<>();
        for (BillItem line : getRequestLines(request)) {
            if (itemRequestApiService.isLineFulfilled(line)) {
                continue;
            }
            if (ItemRequestApiService.isServiceItem(line.getItem())) {
                remainingServices.add(line);
            } else {
                remainingInventory.add(line);
            }
        }
        remainingServiceLinesCache.put(request.getId(), remainingServices);
        remainingInventoryLinesCache.put(request.getId(), remainingInventory);
    }

    public List<Bill> getPendingRequests() {
        if (pendingRequests == null) {
            pendingRequests = new ArrayList<>();
        }
        return pendingRequests;
    }

    public void setPendingRequests(List<Bill> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public Bill getSelectedRequest() {
        return selectedRequest;
    }

    public void setSelectedRequest(Bill selectedRequest) {
        this.selectedRequest = selectedRequest;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

}

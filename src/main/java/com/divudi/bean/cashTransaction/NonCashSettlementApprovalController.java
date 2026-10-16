package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.RequestStatus;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Request;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.NonCashSettlementException;
import com.divudi.service.PaymentSettlementService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Approve / reject pending Non-Cash Settlement requests (#24541).
 */
@Named
@ViewScoped
public class NonCashSettlementApprovalController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PRIVILEGE = Privileges.ApproveNonCashSettlement.name();

    @EJB
    private PaymentSettlementService paymentSettlementService;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    private RequestStatus status = RequestStatus.PENDING;
    private Department department;
    private Date fromDate;
    private Date toDate;
    private List<Request> requests = new ArrayList<>();
    private Request selected;
    private List<Payment> selectedPayments = new ArrayList<>();
    private String rejectReason;
    private boolean acting;
    private Long pendingCount;

    public String navigateToApprovals() {
        return "/cashier/non_cash_settlement_approvals?faces-redirect=true";
    }

    /** Called by f:viewAction on every entry. */
    public void init() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth(new Date());
        }
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        search();
    }

    public void search() {
        // Pending requests are always listed regardless of dates.
        boolean pendingOnly = status == RequestStatus.PENDING;
        requests = paymentSettlementService.findRequests(status, department,
                pendingOnly ? null : fromDate, pendingOnly ? null : toDate);
        pendingCount = null;
        selected = null;
        selectedPayments = new ArrayList<>();
        rejectReason = null;
    }

    public void select(Request r) {
        selected = r;
        selectedPayments = r == null ? new ArrayList<>()
                : paymentSettlementService.findPaymentsOfSettlementBill(r.getBill());
        rejectReason = null;
    }

    public boolean canAct(Request r) {
        return r != null
                && (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.UNDER_REVIEW)
                && sessionController.getLoggedUser() != null
                && webUserController.hasPrivilege(PRIVILEGE)
                && !Objects.equals(sessionController.getLoggedUser(), r.getRequester());
    }

    public String actionBlockedReason(Request r) {
        if (r == null) {
            return "";
        }
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            return "You need the Approve Non-Cash Settlement privilege.";
        }
        if (sessionController.getLoggedUser() == null
                || Objects.equals(sessionController.getLoggedUser(), r.getRequester())) {
            return "You cannot approve or reject your own request.";
        }
        if (r.getStatus() != RequestStatus.PENDING && r.getStatus() != RequestStatus.UNDER_REVIEW) {
            return "This request is no longer pending.";
        }
        return "";
    }

    public void approve() {
        if (acting || selected == null) {
            return;
        }
        if (!canAct(selected)) {
            JsfUtil.addErrorMessage(actionBlockedReason(selected));
            return;
        }
        acting = true;
        Long id = selected.getId();
        try {
            paymentSettlementService.approveSettlement(selected.getId(), sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Request " + selected.getRequestNo() + " approved. The payments are settled.");
            search();
        } catch (NonCashSettlementException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            refreshKeepingSelection(id);
        } finally {
            acting = false;
        }
    }

    public void reject() {
        if (acting || selected == null) {
            return;
        }
        if (!canAct(selected)) {
            JsfUtil.addErrorMessage(actionBlockedReason(selected));
            return;
        }
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter a reason to reject the request.");
            return;
        }
        acting = true;
        Long id = selected.getId();
        try {
            paymentSettlementService.rejectSettlement(selected.getId(), sessionController.getLoggedUser(), rejectReason);
            JsfUtil.addSuccessMessage("Request " + selected.getRequestNo() + " rejected. The payments are back with the requester.");
            search();
        } catch (NonCashSettlementException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            refreshKeepingSelection(id);
        } finally {
            acting = false;
        }
    }

    /** Re-runs the search and re-selects the request with the given id if it is still listed. */
    private void refreshKeepingSelection(Long id) {
        search();
        if (id == null) {
            return;
        }
        for (Request r : requests) {
            if (id.equals(r.getId())) {
                select(r);
                break;
            }
        }
    }

    public long getPendingCount() {
        if (pendingCount == null) {
            pendingCount = paymentSettlementService.countPendingRequests();
        }
        return pendingCount;
    }

    public double getSelectedTotal() {
        double t = 0.0;
        for (Payment p : selectedPayments) {
            t += p.getPaidValue();
        }
        return t;
    }

    public RequestStatus[] getStatuses() {
        return new RequestStatus[]{RequestStatus.PENDING, RequestStatus.APPROVED,
            RequestStatus.REJECTED, RequestStatus.CANCELLED};
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public List<Request> getRequests() {
        return requests;
    }

    public Request getSelected() {
        return selected;
    }

    public List<Payment> getSelectedPayments() {
        return selectedPayments;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}

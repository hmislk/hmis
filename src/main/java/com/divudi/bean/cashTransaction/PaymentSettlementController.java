package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Payment;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.PaymentSettlementService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Backing bean for the Settle Non-Cash page (issue #17964).
 *
 * @see com.divudi.service.PaymentSettlementService
 */
@Named
@ViewScoped
public class PaymentSettlementController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PRIVILEGE = "SettleNonCashPayments";

    @EJB
    private PaymentSettlementService paymentSettlementService;

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    private List<Department> availableDepartments = new ArrayList<>();
    private Department selectedDepartment;
    private List<Payment> pendingPayments = new ArrayList<>();
    private List<Payment> selectedPayments = new ArrayList<>();
    private String referenceNumber;
    private String comments;
    private Bill lastSettlementBill;

    /**
     * Loads the list of departments where the logged-in user has pending
     * non-cash payments. Called on initial page load.
     */
    public void loadPending() {
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            JsfUtil.addErrorMessage("You do not have the required privilege to settle non-cash payments.");
            availableDepartments = new ArrayList<>();
            pendingPayments = new ArrayList<>();
            return;
        }
        availableDepartments = paymentSettlementService.findPendingSettlementDepartments(
                sessionController.getLoggedUser());
        selectedDepartment = null;
        pendingPayments = new ArrayList<>();
        selectedPayments = new ArrayList<>();
        referenceNumber = null;
        comments = null;
        lastSettlementBill = null;
    }

    /**
     * Reloads the pending-payment table for the currently selected department.
     * Invoked when the user picks a department from the dropdown.
     */
    public void onDepartmentChange() {
        selectedPayments = new ArrayList<>();
        if (selectedDepartment == null) {
            pendingPayments = new ArrayList<>();
            return;
        }
        pendingPayments = paymentSettlementService.findPendingSettlementPayments(
                sessionController.getLoggedUser(), selectedDepartment);
    }

    public String settleSelected() {
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            JsfUtil.addErrorMessage("You do not have the required privilege to settle non-cash payments.");
            return "";
        }
        if (selectedDepartment == null) {
            JsfUtil.addErrorMessage("Select a department first.");
            return "";
        }
        if (selectedPayments == null || selectedPayments.isEmpty()) {
            JsfUtil.addErrorMessage("Select at least one payment to settle.");
            return "";
        }
        if (referenceNumber == null || referenceNumber.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Reference number is required.");
            return "";
        }
        try {
            lastSettlementBill = paymentSettlementService.settlePayments(
                    selectedPayments,
                    referenceNumber.trim(),
                    comments,
                    sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Settled " + selectedPayments.size() + " payment(s) for "
                    + selectedDepartment.getName() + ".");
            loadPending();
            return "";
        } catch (IllegalArgumentException | IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return "";
        }
    }

    public double getSelectedTotal() {
        if (selectedPayments == null) {
            return 0.0;
        }
        double t = 0.0;
        for (Payment p : selectedPayments) {
            t += p.getPaidValue();
        }
        return t;
    }

    public List<Department> getAvailableDepartments() {
        return availableDepartments;
    }

    public Department getSelectedDepartment() {
        return selectedDepartment;
    }

    public void setSelectedDepartment(Department selectedDepartment) {
        this.selectedDepartment = selectedDepartment;
    }

    public List<Payment> getPendingPayments() {
        return pendingPayments;
    }

    public List<Payment> getSelectedPayments() {
        return selectedPayments;
    }

    public void setSelectedPayments(List<Payment> selectedPayments) {
        this.selectedPayments = selectedPayments;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Bill getLastSettlementBill() {
        return lastSettlementBill;
    }
}

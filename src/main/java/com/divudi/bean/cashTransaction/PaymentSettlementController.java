package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.Privileges;
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
    private static final String PRIVILEGE = Privileges.SettleNonCashPayments.name();

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
    private List<Payment> lastSettledPayments = new ArrayList<>();
    private boolean settling;
    private boolean printPreview;

    /**
     * Loads the list of departments where the logged-in user has pending
     * non-cash payments. Called on initial page load.
     */
    public void loadPending() {
        // Always reset the print-preview flag on entry so a fresh navigation
        // (or back-button) lands on the form, not on a stale preview.
        printPreview = false;
        lastSettlementBill = null;
        lastSettledPayments = new ArrayList<>();
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
    }

    /**
     * Closes the print preview and returns the page to a fresh settlement form.
     */
    public void prepareNewSettlement() {
        loadPending();
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
        if (settling) {
            return "";
        }
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
            settling = true;
            // Snapshot the payments before settlement mutates currentHolder etc,
            // so the print preview shows what was actually settled.
            List<Payment> snapshot = new ArrayList<>(selectedPayments);
            lastSettlementBill = paymentSettlementService.settlePayments(
                    selectedPayments,
                    referenceNumber.trim(),
                    comments,
                    sessionController.getLoggedUser());
            lastSettledPayments = snapshot;
            JsfUtil.addSuccessMessage("Settled " + snapshot.size() + " payment(s) for "
                    + selectedDepartment.getName() + ".");
            printPreview = true;
            return "";
        } catch (IllegalArgumentException | IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return "";
        } finally {
            settling = false;
        }
    }

    /**
     * Navigation entry point — invoked from cashier/index.xhtml.
     * Ensures a fresh state so the page is never left on a stale print preview.
     */
    public String navigateToSettleNonCash() {
        printPreview = false;
        lastSettlementBill = null;
        lastSettledPayments = new ArrayList<>();
        return "/cashier/settle_non_cash?faces-redirect=true";
    }

    public boolean isSettling() {
        return settling;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public List<Payment> getLastSettledPayments() {
        return lastSettledPayments;
    }

    public double getLastSettledTotal() {
        if (lastSettledPayments == null) {
            return 0.0;
        }
        double t = 0.0;
        for (Payment p : lastSettledPayments) {
            t += p.getPaidValue();
        }
        return t;
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

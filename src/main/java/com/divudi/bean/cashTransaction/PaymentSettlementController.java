package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.Bill;
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

    private List<Payment> pendingPayments = new ArrayList<>();
    private List<Payment> selectedPayments = new ArrayList<>();
    private String referenceNumber;
    private String comments;
    private Bill lastSettlementBill;

    public void loadPending() {
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            JsfUtil.addErrorMessage("You do not have the required privilege to settle non-cash payments.");
            pendingPayments = new ArrayList<>();
            return;
        }
        pendingPayments = paymentSettlementService.findPendingSettlementPayments(
                sessionController.getLoggedUser());
        selectedPayments = new ArrayList<>();
        referenceNumber = null;
        comments = null;
        lastSettlementBill = null;
    }

    public String settleSelected() {
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            JsfUtil.addErrorMessage("You do not have the required privilege to settle non-cash payments.");
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
            JsfUtil.addSuccessMessage("Settled " + selectedPayments.size() + " payment(s).");
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

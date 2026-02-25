/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.RequestStatus;
import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Request;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.DrawerService;
import com.divudi.service.RequestService;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for drawer balance adjustment workflow.
 * Supports cashier self-request (creates a Request for supervisor approval)
 * and admin direct adjustment (bypasses approval, applies immediately).
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class DrawerAdjustmentController implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    WebUserController webUserController;
    @Inject
    RequestService requestService;

    @EJB
    private BillFacade billFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    DrawerService drawerService;

    private WebUser targetDrawerUser;
    private PaymentMethod paymentMethod;
    private Double adjustmentDelta;
    private String reason;

    public DrawerAdjustmentController() {
    }

    // <editor-fold defaultstate="collapsed" desc="Navigation">
    public String navigateToRequestAdjustment() {
        makeNull();
        targetDrawerUser = sessionController.getLoggedUser();
        return "/cashier/drawer_adjustment_request?faces-redirect=true";
    }

    public String navigateToAdminAdjustment() {
        if (!webUserController.hasPrivilege("DrawerAdjustmentDirect")) {
            JsfUtil.addErrorMessage("You are not authorized to perform direct drawer adjustments.");
            return "";
        }
        makeNull();
        return "/cashier/drawer_adjustment_admin?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Cashier Self-Request">
    public String submitAdjustmentRequest() {
        if (!validateInput(true)) {
            return "";
        }

        Bill bill = createAndPersistAdjustmentBill(sessionController.getLoggedUser());

        Request request = new Request();
        request.setRequestType(RequestType.DRAWER_ADJUSTMENT);
        request.setStatus(RequestStatus.PENDING);
        request.setBill(bill);
        request.setRequester(sessionController.getLoggedUser());
        request.setRequestAt(new Date());
        request.setRequestReason(reason);
        request.setTargetWebUser(targetDrawerUser);
        request.setPaymentMethod(paymentMethod);
        request.setInstitution(sessionController.getInstitution());
        request.setDepartment(sessionController.getDepartment());

        requestService.save(request, sessionController.getLoggedUser());
        request.setRequestNo("DRADJ-" + request.getId());
        requestService.save(request, sessionController.getLoggedUser());

        JsfUtil.addSuccessMessage("Drawer adjustment request submitted successfully. Request No: " + request.getRequestNo());
        makeNull();
        return "/cashier/index?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Admin Direct Adjustment">
    public String applyDirectAdjustment() {
        if (!webUserController.hasPrivilege("DrawerAdjustmentDirect")) {
            JsfUtil.addErrorMessage("You are not authorized to perform direct drawer adjustments.");
            return "";
        }

        if (targetDrawerUser == null) {
            JsfUtil.addErrorMessage("Please select a user.");
            return "";
        }

        if (!validateInput(false)) {
            return "";
        }

        Drawer drawer = drawerService.getUsersDrawer(targetDrawerUser);
        if (drawer == null) {
            JsfUtil.addErrorMessage("Drawer not found for the selected user.");
            return "";
        }

        Bill bill = createAndPersistAdjustmentBill(sessionController.getLoggedUser());
        drawerService.applyDrawerAdjustment(drawer, paymentMethod, adjustmentDelta, bill, sessionController.getLoggedUser());

        JsfUtil.addSuccessMessage("Drawer adjusted successfully. Bill No: " + bill.getDeptId());
        makeNull();
        return "/cashier/index?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Private Helpers">
    private boolean validateInput(boolean requireTargetUser) {
        if (requireTargetUser && targetDrawerUser == null) {
            JsfUtil.addErrorMessage("Target user is not set.");
            return false;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method.");
            return false;
        }
        if (adjustmentDelta == null) {
            JsfUtil.addErrorMessage("Please enter an adjustment amount.");
            return false;
        }
        if (reason == null || reason.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please provide a reason for the adjustment.");
            return false;
        }
        return true;
    }

    private Bill createAndPersistAdjustmentBill(WebUser creator) {
        BilledBill bill = new BilledBill();
        bill.setBillType(BillType.DrawerAdjustment);
        bill.setCreatedAt(Calendar.getInstance().getTime());
        bill.setCreater(creator);
        bill.setDeptId(billNumberBean.institutionBillNumberGenerator(
                sessionController.getDepartment(), BillType.DrawerAdjustment, BillClassType.BilledBill, BillNumberSuffix.DRADJ));
        bill.setInsId(billNumberBean.institutionBillNumberGenerator(
                sessionController.getInstitution(), BillType.DrawerAdjustment, BillClassType.BilledBill, BillNumberSuffix.DRADJ));
        bill.setDepartment(sessionController.getDepartment());
        bill.setInstitution(sessionController.getInstitution());
        bill.setFromDepartment(sessionController.getDepartment());
        bill.setFromInstitution(sessionController.getInstitution());
        bill.setNetTotal(adjustmentDelta);
        bill.setComments(reason);
        billFacade.create(bill);
        return bill;
    }
    // </editor-fold>

    public void makeNull() {
        targetDrawerUser = null;
        paymentMethod = null;
        adjustmentDelta = null;
        reason = null;
    }

    // <editor-fold defaultstate="collapsed" desc="Getters & Setters">
    public WebUser getTargetDrawerUser() {
        return targetDrawerUser;
    }

    public void setTargetDrawerUser(WebUser targetDrawerUser) {
        this.targetDrawerUser = targetDrawerUser;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getAdjustmentDelta() {
        return adjustmentDelta;
    }

    public void setAdjustmentDelta(Double adjustmentDelta) {
        this.adjustmentDelta = adjustmentDelta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    // </editor-fold>

}

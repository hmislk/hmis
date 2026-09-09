/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Refund-to-patient workflow for Appointment Deposit bills (Issue #23571).
 *
 * Appointment deposit bills (BillTypeAtomic.INWARD_APPOINTMENT_BILL) are
 * pre-admission: they carry {@code Bill.patient} and never a
 * {@code PatientEncounter}. {@code InwardRefundController} is deeply
 * PatientEncounter-centric (errorCheck/saveBill/pay all dereference
 * getCurrent().getPatientEncounter(), and its eligibility lookup filters on
 * BillType.InwardPaymentBill), so it cannot be reused here. This controller
 * mirrors its cancel -> refund pattern but is keyed off Bill.patient
 * instead, for exactly one bill at a time (no BHT-wide bill picker needed -
 * navigation always starts from one specific appointment deposit bill).
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class AppointmentRefundController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillService billService;
    @EJB
    private PaymentService paymentService;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private CashTransactionBean cashTransactionBean;

    @Inject
    private SessionController sessionController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private FinancialTransactionController financialTransactionController;
    @Inject
    private WebUserController webUserController;

    private Bill current;
    private Bill originalBillToRefund;
    private PaymentMethodData paymentMethodData;
    private boolean printPreview;

    public void makeNull() {
        current = null;
        originalBillToRefund = null;
        paymentMethodData = null;
        printPreview = false;
    }

    /**
     * Navigate to the appointment deposit refund page for a specific
     * appointment deposit bill (opened from the appointment bill receipt /
     * search results). Only an un-cancelled, not-yet-fully-refunded
     * INWARD_APPOINTMENT_BILL is eligible.
     */
    public String navigateToRefundFromAppointmentBill(Long appointmentBillId) {
        makeNull();

        if (!webUserController.hasPrivilege("InwardBilling")) {
            JsfUtil.addErrorMessage("You are not authorized to refund this bill");
            return "";
        }

        financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
        if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
            JsfUtil.addErrorMessage("Start Your Shift First !");
            return "/cashier/index?faces-redirect=true";
        }

        Bill bill = billService.reloadBill(appointmentBillId);

        if (bill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }

        if (bill.getBillTypeAtomic() != BillTypeAtomic.INWARD_APPOINTMENT_BILL) {
            JsfUtil.addErrorMessage("This bill is not an Appointment Deposit bill and cannot be refunded here.");
            return "";
        }

        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("This bill has been cancelled and cannot be refunded.");
            return "";
        }

        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("This bill has already been refunded.");
            return "";
        }

        originalBillToRefund = bill;

        double remainingRefundableAmount = computeRemainingRefundableAmount(bill);
        getCurrent().setTotal(remainingRefundableAmount);

        return "/inward/inward_appointment_bill_refund?faces-redirect=true";
    }

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();
    }

    private boolean errorCheck() {
        if (!webUserController.hasPrivilege("InwardBilling")) {
            JsfUtil.addErrorMessage("You are not authorized to refund this bill");
            return true;
        }

        if (getOriginalBillToRefund() == null) {
            JsfUtil.addErrorMessage("Select a Bill to Refund");
            return true;
        }

        if (getOriginalBillToRefund().isCancelled()) {
            JsfUtil.addErrorMessage("This bill has been cancelled and cannot be refunded.");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        if (getCurrent().getTotal() <= 0) {
            JsfUtil.addErrorMessage("Enter a valid refund amount");
            return true;
        }

        double remaining = computeRemainingRefundableAmount(getOriginalBillToRefund());

        if (Math.abs(remaining) < getCurrent().getTotal()) {
            double different = Math.abs(Math.abs(remaining) - Math.abs(getCurrent().getTotal()));

            if (different > 0.1) {
                JsfUtil.addErrorMessage("Check Refunding Amount");
                return true;
            }
        }

        return false;
    }

    public void pay() {
        if (errorCheck()) {
            return;
        }

        saveBill();
        saveBillItem();

        double remainingAfterThisRefund = computeRemainingRefundableAmount(getOriginalBillToRefund());
        if (Math.abs(remainingAfterThisRefund) <= 0.1) {
            getOriginalBillToRefund().setRefunded(true);
            getOriginalBillToRefund().setRefundedBill(getCurrent());
        }
        getBillFacade().edit(getOriginalBillToRefund());

        printPreview = true;

        List<Payment> payments = paymentService.createPayment(getCurrent(), paymentMethodData);
        paymentService.updateBalances(payments);

        WebUser wb = cashTransactionBean.saveBillCashOutTransaction(getCurrent(), sessionController.getLoggedUser());
        sessionController.setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Refund Saved");
    }

    private void saveBill() {
        billBean.setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setBillType(BillType.InwardAppointmentBill);
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setInstitution(sessionController.getInstitution());
        getCurrent().setDepartment(sessionController.getDepartment());
        getCurrent().setReferenceBill(getOriginalBillToRefund());
        getCurrent().setPatient(getOriginalBillToRefund().getPatient());
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_APPOINTMENT_BILL_REFUND);

        getCurrent().setDeptId(billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.INWARD_APPOINTMENT_BILL_REFUND));
        getCurrent().setInsId(billNumberBean.institutionBillNumberGeneratorYearly(sessionController.getInstitution(), BillTypeAtomic.INWARD_APPOINTMENT_BILL_REFUND));

        double dbl = Math.abs(getCurrent().getTotal());

        getCurrent().setTotal(0 - dbl);
        getCurrent().setNetTotal(0 - dbl);
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(sessionController.getLoggedUser());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }
    }

    private void saveBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getCurrent());
        temBi.setGrossValue(0 - getCurrent().getTotal());
        temBi.setNetValue(0 - getCurrent().getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(sessionController.getLoggedUser());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }

        saveBillFee(temBi);
    }

    private void saveBillFee(BillItem bt) {
        BillFee bf = new BillFee();
        bf.setBill(getCurrent());
        bf.setBillItem(bt);
        bf.setCreatedAt(new Date());
        bf.setCreater(sessionController.getLoggedUser());
        bf.setFeeGrossValue(0 - getCurrent().getTotal());
        bf.setFeeValue(0 - getCurrent().getTotal());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
    }

    /**
     * Sum of netTotal of every RefundBill already linked to originalBill via
     * referenceBill (always <= 0), added to the bill's own netTotal. Mirrors
     * InwardRefundController.computeRemainingRefundableAmount(Bill) - no
     * per-bill cache is needed here since navigation always deals with
     * exactly one appointment bill at a time.
     */
    private double computeRemainingRefundableAmount(Bill originalBill) {
        if (originalBill == null) {
            return 0.0;
        }
        String sql = "select sum(b.netTotal) from Bill b where b.referenceBill=:orig and b.retired=false";
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("orig", originalBill);
        double refundedSoFar = getBillFacade().findDoubleByJpql(sql, hm);
        return originalBill.getNetTotal() + refundedSoFar;
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new RefundBill();
            current.setBillType(BillType.InwardAppointmentBill);
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public Bill getOriginalBillToRefund() {
        return originalBillToRefund;
    }

    public void setOriginalBillToRefund(Bill originalBillToRefund) {
        this.originalBillToRefund = originalBillToRefund;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }
}

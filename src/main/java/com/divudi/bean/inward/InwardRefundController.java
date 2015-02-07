/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.inward;

import com.divudi.bean.memberShip.PaymentSchemeController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.bean.common.BillBeanController;
import com.divudi.data.BillClassType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class InwardRefundController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private SessionController sessionController;
    private double paidAmount;
    private Bill current;
    private PaymentMethodData paymentMethodData;
    @EJB
    private BillNumberGenerator billNumberBean;
    private boolean printPreview;
    @Inject
    private InwardBeanController inwardBean;

    public void makeNull() {
        current = null;
        paidAmount = 0.0;
        printPreview = false;
    }

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();
    }

    @Inject
    private PaymentSchemeController paymentSchemeController;

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            UtilityController.addErrorMessage("Select BHT");
            return true;
        }
        if (getCurrent().getPaymentMethod() == null) {
            UtilityController.addErrorMessage("Select Payment Method");
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        if ((getPaidAmount() < getCurrent().getTotal())) {
            double different = Math.abs((getPaidAmount() - Math.abs(getCurrent().getTotal())));

            if (different > 0.1) {
                UtilityController.addErrorMessage("Check Refuning Amount");
                return true;
            }
        }

        return false;
    }

    @EJB
    private CashTransactionBean cashTransactionBean;

    public void pay() {
        if (errorCheck()) {
            return;
        }

        saveBill();
        saveBillItem();
        printPreview = true;

        getBillBean().updateInwardDipositList(getCurrent().getPatientEncounter(), getCurrent());

        if (getCurrent().getPatientEncounter().isPaymentFinalized()) {
            getInwardBean().updateFinalFill(getCurrent().getPatientEncounter());
            if (getCurrent().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                getInwardBean().updateCreditDetail(getCurrent().getPatientEncounter(), getCurrent().getPatientEncounter().getFinalBill().getNetTotal());
            }
        }

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        UtilityController.addSuccessMessage("Payment Bill Saved");
    }

    @Inject
    private BillBeanController billBean;

    private void saveBill() {
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setBillType(BillType.InwardPaymentBill);
        getCurrent().setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getCurrent().setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setDepartment(getSessionController().getDepartment());
        // getCurrent().setForwardReferenceBill(getCurrent().getPatientEncounter().getFinalBill());
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getCurrent().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREF));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getCurrent().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREF));

        double dbl = Math.abs(getCurrent().getTotal());

        getCurrent().setTotal(0 - dbl);
        getCurrent().setNetTotal(0 - dbl);
        getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        }

    }

    private void saveBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getCurrent());
        temBi.setGrossValue(0 - getCurrent().getTotal());
        temBi.setNetValue(0 - getCurrent().getTotal());
        temBi.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temBi.setCreater(getSessionController().getLoggedUser());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }

        saveBillFee(temBi);

    }

    private void saveBillFee(BillItem bt) {
        BillFee bf = new BillFee();
        bf.setBill(getCurrent());
        bf.setBillItem(bt);
        bf.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setFeeGrossValue(0 - getCurrent().getTotal());
        bf.setFeeValue(0 - getCurrent().getTotal());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public Bill getCurrent() {
        if (current == null) {
            current = new RefundBill();
            current.setBillType(BillType.InwardPaymentBill);
        }

        return current;
    }

    public void setCurrent(RefundBill current) {
        this.current = current;
    }

    public void calculatePaidAmount() {

        HashMap map = new HashMap();
        String sql = "SELECT sum(bb.netTotal) FROM Bill bb where bb.retired=false "
                + " and bb.billType=:bType and bb.patientEncounter=:pe";
        map.put("bType", BillType.InwardPaymentBill);
        map.put("pe", getCurrent().getPatientEncounter());
        paidAmount = getBillFacade().findDoubleByJpql(sql, map);

    }

    public void selectBhtListener() {
        if (getCurrent().getPatientEncounter().isPaymentFinalized()) {
            calculteFinalBillMax();
        } else {
            calculatePaidAmount();
        }

    }

    public void calculteFinalBillMax() {
        String sql = "Select b From BilledBill b where"
                + " b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter=:pe"
                + " order by b.id desc";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);
        hm.put("pe", getCurrent().getPatientEncounter());

        Bill b = getBillFacade().findFirstBySQL(sql, hm);

        if (b == null) {
            paidAmount = 0;
            return;
        }

        paidAmount = b.getNetTotal() - (b.getPaidAmount() + getCurrent().getPatientEncounter().getCreditPaidAmount());

//        double paidByPatient = Math.abs(b.getPaidAmount());
//
//        if (getCurrent().getPatientEncounter().getPaymentMethod() == PaymentMethod.Cash) {
//            paidAmount = paidByPatient - Math.abs(b.getNetTotal());
//            return;
//        }
//
//        double creditUsedAmount = Math.abs(getCurrent().getPatientEncounter().getCreditUsedAmount());
//        double creditPaidAmount = Math.abs(getCurrent().getPatientEncounter().getCreditPaidAmount());
//        double netCredit = creditUsedAmount - creditPaidAmount;
//
//        if (getCurrent().getPatientEncounter().getCreditLimit() != 0) {
//            paidAmount = (paidByPatient + netCredit) - Math.abs(b.getNetTotal());
//        }
    }

    public double getPaidAmount() {

        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
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

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }
}

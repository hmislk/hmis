/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.facade.BillFacade;
import com.divudi.facade.WebUserFacade;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class DrawerAdjustmentController implements Serializable {

    @Inject
    SessionController sessionController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    CashTransactionBean cashTransactionBean;
    /////////////////////////
    private Bill adjustmentBill;
    Double value;
    String comment;
    private boolean printPreview;

    /**
     * Creates a new instance of PharmacySaleController
     */
    public DrawerAdjustmentController() {
    }

    public Bill getAdjustmentBill() {
        if (adjustmentBill == null) {
            adjustmentBill = new BilledBill();
            adjustmentBill.setBillType(BillType.DrawerAdjustment);
        }
        return adjustmentBill;
    }

    public void setAdjustmentPreBill(Bill adjustmentPreBill) {
        this.adjustmentBill = adjustmentPreBill;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void makeNull() {
        printPreview = false;
        comment = null;
        value = null;
        adjustmentBill = null;
    }

    private void saveAdjustmentBill() {
        getAdjustmentBill().setCreatedAt(Calendar.getInstance().getTime());
        getAdjustmentBill().setCreater(getSessionController().getLoggedUser());
        getAdjustmentBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getAdjustmentBill().getBillType(), BillClassType.BilledBill, BillNumberSuffix.DRADJ));
        getAdjustmentBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getAdjustmentBill().getBillType(), BillClassType.BilledBill, BillNumberSuffix.DRADJ));
        getAdjustmentBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getAdjustmentBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getAdjustmentBill().setToDepartment(null);
        getAdjustmentBill().setToInstitution(null);
        getAdjustmentBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getAdjustmentBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getAdjustmentBill().setComments(comment);
        if (getAdjustmentBill().getId() == null) {
            getBillFacade().create(getAdjustmentBill());
        } else {
            getBillFacade().edit(getAdjustmentBill());
        }
    }

    private boolean errorCheck() {
//        if (getSessionController().getLoggedUser().getDrawer() == null) {
//            return true;
//        }

        if (getValue() == null) {
            return true;
        }

        return false;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    @EJB
    WebUserFacade webUserFacade;

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    private void save(double ballance, PaymentMethod paymentMethod) {

        saveAdjustmentBill();

        double difference = ballance - getValue();

//        System.err.println("Cash Balance " + cashBallance);
//        System.err.println("Difference  " + difference);
        CashTransaction cashTransaction = new CashTransaction();
        cashTransaction.setCreatedAt(new Date());
        cashTransaction.setCreater(getSessionController().getLoggedUser());

        if (difference < 0) {
            //  System.err.println("Adding");
            switch (paymentMethod) {
                case Cash:
                    cashTransaction.setCashValue(0 - difference);
                    break;
                case Card:
                    cashTransaction.setCreditCardValue(0 - difference);
                    break;
                case Cheque:
                    cashTransaction.setChequeValue(0 - difference);
                    break;
                case Slip:
                    cashTransaction.setSlipValue(0 - difference);
                    break;
            }

//            getCashTransactionBean().saveCashAdjustmentTransactionIn(cashTransaction, adjustmentBill, getSessionController().getLoggedUser().getDrawer(), getSessionController().getLoggedUser());
            getAdjustmentBill().setCashTransaction(cashTransaction);
            getAdjustmentBill().setNetTotal(0 - difference);
            getBillFacade().edit(getAdjustmentBill());
//            getCashTransactionBean().addToBallance(getSessionController().getLoggedUser().getDrawer(), cashTransaction);

        } else {
            //System.err.println("Diduct");
            switch (paymentMethod) {
                case Cash:
                    cashTransaction.setCashValue(0 - difference);
                    break;
                case Card:
                    cashTransaction.setCreditCardValue(0 - difference);
                    break;
                case Cheque:
                    cashTransaction.setChequeValue(0 - difference);
                    break;
                case Slip:
                    cashTransaction.setSlipValue(0 - difference);
                    break;
            }

//            getCashTransactionBean().saveCashAdjustmentTransactionOut(cashTransaction, adjustmentBill, getSessionController().getLoggedUser().getDrawer(), getSessionController().getLoggedUser());
            getAdjustmentBill().setCashTransaction(cashTransaction);
            getAdjustmentBill().setNetTotal(0 - difference);
            getBillFacade().edit(getAdjustmentBill());
//            getCashTransactionBean().deductFromBallance(getSessionController().getLoggedUser().getDrawer(), cashTransaction);
        }

        WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
        getSessionController().setLoggedUser(wb);

        printPreview = true;
    }

    public void saveAdjustBillCash() {
        if (errorCheck()) {
            return;
        }
//        save(getSessionController().getLoggedUser().getDrawer().getRunningBallance(), PaymentMethod.Cash);
    }

    public void saveAdjustBillCheque() {
        if (errorCheck()) {
            return;
        }
//        save(getSessionController().getLoggedUser().getDrawer().getChequeBallance(), PaymentMethod.Cheque);
    }

    public void saveAdjustBillSlip() {
        if (errorCheck()) {
            return;
        }
//        save(getSessionController().getLoggedUser().getDrawer().getSlipBallance(), PaymentMethod.Slip);
    }

    public void saveAdjustBillCreditCard() {
        if (errorCheck()) {
            return;
        }
//        save(getSessionController().getLoggedUser().getDrawer().getCreditCardBallance(), PaymentMethod.Card);
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

}

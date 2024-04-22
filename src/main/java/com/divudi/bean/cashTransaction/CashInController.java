/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.facade.BillFacade;
import com.divudi.facade.CashTransactionFacade;
import com.divudi.facade.WebUserFacade;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class CashInController implements Serializable {

    private boolean printPreview;
    Bill bill;
    private Bill referenceBill;
    @EJB
    private CashTransactionBean cashTransactionBean;
    @EJB
    CashTransactionFacade cashTransactionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    private SessionController sessionController;

    public CashTransactionFacade getCashTransactionFacade() {
        return cashTransactionFacade;
    }

    public void setCashTransactionFacade(CashTransactionFacade cashTransactionFacade) {
        this.cashTransactionFacade = cashTransactionFacade;
    }

    private boolean errorCheck() {
        if (getBill().getCashTransaction() == null) {
            return true;
        }

        return false;
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setCashTransaction(new CashTransaction());
        }

        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    private void saveBill(CashTransaction ct) {
        double netTotal = 0;
        if (ct.getCashValue() != null) {
            netTotal += Math.abs(ct.getCashValue());
        }
        if (ct.getChequeValue() != null) {
            netTotal += Math.abs(ct.getChequeValue());
        }
        if (ct.getCreditCardValue() != null) {
            netTotal += Math.abs(ct.getCreditCardValue());
        }
        if (ct.getSlipValue() != null) {
            netTotal += Math.abs(ct.getSlipValue());
        }

        getBill().setNetTotal(netTotal);
        getBill().setBillType(BillType.CashIn);
        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setToWebUser(getSessionController().getLoggedUser());
        getBill().setBackwardReferenceBill(getReferenceBill());

        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.BilledBill, BillNumberSuffix.CSIN));
        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.BilledBill, BillNumberSuffix.CSIN));

        getBillFacade().create(getBill());

    }

    @EJB
    WebUserFacade webUserFacade;

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public void settle() {
        if (errorCheck()) {
            return;
        }

        if (getBill().getCashTransaction().getCashValue() == null) {
            calTotal();
        }

        CashTransaction ct = getBill().getCashTransaction();
        getBill().setCashTransaction(null);
        saveBill(ct);

        getCashTransactionBean().saveCashInTransaction(ct, getBill(), getSessionController().getLoggedUser());

        getBill().setCashTransaction(ct);
        getBillFacade().edit(getBill());

        if (getReferenceBill() != null) {
            getReferenceBill().getForwardReferenceBills().add(getBill());
            getBillFacade().edit(getReferenceBill());

        }

//        if (getBill().getFromWebUser() != null) {
//            getCashTransactionBean().deductFromBallance(getBill().getFromWebUser().getDrawer(), dbl, ct);
//        }
//        getCashTransactionBean().addToBallance(getSessionController().getLoggedUser().getDrawer(), ct);
        WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
        getSessionController().setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Succesfully Cash Inned ");
        printPreview = true;

    }

    public void makeNull() {
        printPreview = false;
        bill = null;
        referenceBill = null;
    }

    public void calTotal() {
        double dbl = getCashTransactionBean().calTotal(getBill().getCashTransaction());
        getBill().getCashTransaction().setCashValue(dbl);
    }

    /**
     * Creates a new instance of BulkCashierController
     */
    public CashInController() {
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
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

    public Bill getReferenceBill() {
        return referenceBill;
    }

    public void setReferenceBill(Bill referenceBill) {
        makeNull();
        this.referenceBill = referenceBill;
        getBill().getCashTransaction().copyQty(referenceBill.getCashTransaction());
        getBill().setFromWebUser(referenceBill.getFromWebUser());
        getBill().setNetTotal(referenceBill.getNetTotal());
    }

}

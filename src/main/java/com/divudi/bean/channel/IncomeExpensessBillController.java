/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.*;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class IncomeExpensessBillController implements Serializable {

    @Inject
    CommonController commonController;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    SearchController searchController;

    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;

    Bill current;
    Bill bill;
    PaymentMethodData paymentMethodData;
    PaymentMethod paymentMethod;
    private boolean printPreview = false;
    double amount;
    String comment;

    public IncomeExpensessBillController() {
    }

    private boolean errorCheck() {
        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select Payment Methord");
            return true;
        }

        if (getCurrent().getComments() == null || "".equals(getCurrent().getComments())) {
            JsfUtil.addErrorMessage("Please Enter Discription");
            return true;
        }

        if (getCurrent().getPaymentMethod() != null && getCurrent().getPaymentMethod() == PaymentMethod.Cheque) {
            if (getCurrent().getBank() == null || getCurrent().getChequeRefNo() == null || getCurrent().getChequeDate() == null) {
                JsfUtil.addErrorMessage("Please select Cheque Number,Bank and Cheque Date");
                return true;
            }

        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        if (getAmount() < 1) {
            JsfUtil.addErrorMessage("Type Amount");
            return true;
        }

        return false;
    }
    
    
    private boolean errorCheckCancel() {
        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select Payment Methord");
            return true;
        }

        if (getComment() == null || "".equals(getComment())) {
            JsfUtil.addErrorMessage("Please Enter Discription");
            return true;
        }

        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        return false;
    }

    private void saveBill(BillType billType, BillNumberSuffix suffix) {

        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.BilledBill, suffix));
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.BilledBill, suffix));

        getCurrent().setBillType(billType);

        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setInstitution(getSessionController().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setNetTotal(getCurrent().getNetTotal());

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getBillFacade().create(getCurrent());
    }

    public void settleIncomeBill() {
        settleBill(BillType.ChannelIncomeBill, BillNumberSuffix.I, getAmount());
    }

    public void settleExpencesBill() {
        settleBill(BillType.ChannelExpenesBill, BillNumberSuffix.E, 0 - getAmount());
    }

    public void settleBill(BillType billType, BillNumberSuffix suffix, double d) {

        if (errorCheck()) {
            return;
        }

        getCurrent().setTotal(d);
        getCurrent().setNetTotal(d);
        getCurrent().setVat(0.0);
        getCurrent().setVatPlusNetTotal(d);

        saveBill(billType, suffix);
        saveBillItem();

//        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
//        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    private void saveBillItem() {
        BillItem tmp = new BillItem();

        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setBill(getCurrent());
        tmp.setNetValue(getCurrent().getNetTotal());
        tmp.setVat(0.0);
        tmp.setVatPlusNetValue(getCurrent().getNetTotal());
        getBillItemFacade().create(tmp);

    }

    public void recreateModel() {
        current = null;
        printPreview = false;
        amount = 0.0;
        comment="";
        getSearchController().makeListNull();
    }

    public void prepareNewBill() {
        recreateModel();
    }
    
    public void cancelIncomeBill(){
        cancelBill(BillType.ChannelIncomeBill, BillNumberSuffix.ICAN);
    }
    
    public void cancelExpencesBill(){
        cancelBill(BillType.ChannelExpenesBill, BillNumberSuffix.ECAN);
    }

    public void cancelBill(BillType billType, BillNumberSuffix suffix) {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheckCancel()) {
                return;
            }

            CancelledBill cb = createCancelBill(billType, suffix);

            getBillFacade().create(cb);
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    private CancelledBill createCancelBill(BillType billType, BillNumberSuffix suffix) {
        CancelledBill cb = new CancelledBill();
        cb.copy(getBill());
        cb.invertValue(getBill());
        cb.setBilledBill(getBill());

        cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.CancelledBill, suffix));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.CancelledBill, suffix));

        cb.setCreatedAt(new Date());
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setPaymentMethod(paymentMethod);
        cb.setComments(comment);

        return cb;
    }

    private void cancelBillItems(Bill can) {
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);
            b.setReferenceBill(nB.getReferenceBill());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            getBillItemFacade().create(b);

        }
    }

    public List<BillItem> getBillItems() {
        List<BillItem> billItems = new ArrayList<>();
        if (getBill() != null) {
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billItems = getBillItemFacade().findByJpql(sql);
            if (billItems == null) {
                billItems = new ArrayList<BillItem>();
            }
        }

        return billItems;
    }

    //Getters And Setters
    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
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

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new Bill();
        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public SearchController getSearchController() {
        return searchController;
    }

    public void setSearchController(SearchController searchController) {
        this.searchController = searchController;
    }

}

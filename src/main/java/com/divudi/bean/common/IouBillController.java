/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Person;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.entity.BillFee;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.facade.PaymentFacade;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class IouBillController implements Serializable {

    @Inject
    CommonController commonController;
    @Inject
    BillController billController;
    private Bill current;
    private boolean printPreview = false;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    private SessionController sessionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    PaymentFacade paymentFacade;
    private Person newPerson;
    PaymentMethodData paymentMethodData;
    String comment;
    private double returnAmount;
    private double returnTotal;
    private Bill currentReturnBill;
    private PaymentMethod paymentMethod;
    private boolean printPriview;
    private List<Bill> billList;

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public IouBillController() {
    }

    public Title[] getTitle() {
        return Title.values();
    }

    @Inject
    PaymentSchemeController paymentSchemeController;

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }
    
    public void fillBillsReferredByCurrentBill(){
        billList=new ArrayList<>();
        String sql="Select b from Bill b where b.retired=:ret and b.billedBill=:cb";
        HashMap m=new HashMap();
        m.put("ret", false);
        m.put("cb", getCurrent());
        billList=getBillFacade().findByJpql(sql,m);
        System.out.println("billList = " + billList.size());
    }

    private boolean errorCheck() {
        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

//        //Edited 2014.10.04 p
//        if (getCurrent().getStaff() == null && getCurrent().getPerson() == null && getNewPerson() == null) {
//            JsfUtil.addErrorMessage("Can't settle without Person");
//            return true;
//        }
        if (getCurrent().getPaymentMethod() != null && getCurrent().getPaymentMethod() == PaymentMethod.Cheque) {
            if (getCurrent().getBank() == null || getCurrent().getChequeRefNo() == null || getCurrent().getChequeDate() == null) {
                JsfUtil.addErrorMessage("Please select Cheque Number,Bank and Cheque Date");
                return true;
            }

        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        if (getCurrent().getNetTotal() < 1) {
            JsfUtil.addErrorMessage("Type Amount");
            return true;
        }

        if (checkInvoice()) {
            JsfUtil.addErrorMessage("Invoice Number Already Exist");
            return true;
        }

        return false;
    }

    private boolean checkInvoice() {
        Calendar year = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.set(year.get(Calendar.YEAR), 3, 1, 0, 0, 0);
        Date fd = c.getTime();
        //// // System.out.println("d = " + fd);
        DecimalFormat df = new DecimalFormat("00000");
        String s = df.format(getCurrent().getIntInvoiceNumber());
        String inv = createInvoiceNumberSuffix() + s;
        String sql = "Select b From BilledBill b where "
                + " b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType= :btp "
                + " and b.createdAt > :fd "
                + " and b.invoiceNumber=:inv ";
//                + " and (b.invoiceNumber) like '%" + inv.trim().toUpperCase() + "%'";
        HashMap h = new HashMap();
        h.put("btp", BillType.IouIssue);
        h.put("fd", fd);
        h.put("inv", inv);
        List<Bill> tmp = getBillFacade().findByJpql(sql, h, TemporalType.TIMESTAMP);

        if (tmp.size() > 0) {
            return true;
        }

        return false;
    }

    public void checkInvoiceNumber() {
    }

    private String createInvoiceNumberSuffix() {

        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        String s1;
        String s2;
        if (m < 3) {
            s1 = Integer.toString(y - 1);
            s2 = Integer.toString(y);

        } else {
            s1 = Integer.toString(y);
            s2 = Integer.toString(y + 1);

        }
        String s = s1.substring(2, 4) + s2.substring(2, 4) + "-";

        return s;
    }

    @Inject
    BillBeanController billBean;

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private void saveBill() {

        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.IouIssue, BillClassType.BilledBill, BillNumberSuffix.PTYPAY));
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.IouIssue, BillClassType.BilledBill, BillNumberSuffix.PTYPAY));

        getCurrent().setBillType(BillType.IouIssue);

        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setInstitution(getSessionController().getInstitution());
//        getCurrent().setComments(comment);

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setTotal(0 - getCurrent().getNetTotal());
        getCurrent().setNetTotal(0 - getCurrent().getNetTotal());

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getBillFacade().create(getCurrent());
    }
    @EJB
    private PersonFacade personFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;
    
    public String navigateToIouReturnBill(){
        return "";
    }

    public void settleBill() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (errorCheck()) {
            return;
        }

        switch (getTabId()) {
            case "tabStaff":
                if (current.getStaff() == null) {
                    JsfUtil.addErrorMessage("Staff?");
                    return;
                }
                break;
            case "tabSearchPerson":
                if (current.getPerson() == null) {
                    JsfUtil.addErrorMessage("Person?");
                    return;
                }
                break;
            case "tabNew":
                if (getNewPerson().getName().trim().equals("")) {
                    JsfUtil.addErrorMessage("Person?");
                    return;
                }
                break;
            default:
                JsfUtil.addErrorMessage(getTabId());
                return;
        }

        if (getTabId().equals("tabNew")) {
            getPersonFacade().create(getNewPerson());
            getCurrent().setPerson(getNewPerson());
        }

        getCurrent().setTotal(getCurrent().getNetTotal());
        DecimalFormat df = new DecimalFormat("00000");
        String s = df.format(getCurrent().getIntInvoiceNumber());
        getCurrent().setInvoiceNumber(createInvoiceNumberSuffix() + s);

        saveBill();
        saveBillItem();

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleReturnBill() {
        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        if (getCurrent() != null && getCurrent().getId() != null && getCurrent().getId() != 0) {
            Bill rb = createIouCashReturnBill();
            Payment p = createPaymentForIouBillReturn(rb, paymentMethod);
            p.setPaidValue(returnAmount);
            paymentFacade.edit(p);
            getBillFacade().edit(getCurrent());
            saveIouReturnBill(rb);
            printPriview=true;
        }
    }

    public Payment createPaymentForIouBillReturn(Bill rb, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(rb);
        p.setPaidValue(0 - Math.abs(rb.getNetTotal()));
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        if (p.getBill().getBillType() == BillType.PaymentBill) {
            p.setPaidValue(p.getBill().getNetTotal());
        } else {
            p.setPaidValue(p.getBill().getCashPaid());
        }

        if (p.getId() == null) {
            paymentFacade.create(p);
        }

    }

    private Bill createIouCashReturnBill() {
        Bill rb = new RefundBill();
        rb.copy(getCurrent());
        rb.invertValue(getCurrent());
        rb.setBillType(BillType.IouReturn);
        rb.setBillTypeAtomic(BillTypeAtomic.IOU_CASH_RETURN);
        rb.setBilledBill(getCurrent());
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDiscount(0.00);
        rb.setDiscountPercent(0.0);
        rb.setComments(comment);
        rb.setPaymentMethod(paymentMethod);
        rb.setTotal(0 - returnAmount);
        getBillFacade().create(rb);

        return rb;

    }

    private boolean saveIouReturnBill(Bill rb) {
        if (rb == null) {
            JsfUtil.addErrorMessage("No bill");
            return false;
        }
        if (rb.getBilledBill() == null) {
            rb.setBilledBill(getCurrent());
        }
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setComments(comment);
        rb.setPaymentMethod(paymentMethod);
        rb.setReferenceBill(getCurrent());
        rb.setBilledBill(getCurrent());
        rb.setBillType(BillType.IouReturn);
        rb.setBillTypeAtomic(BillTypeAtomic.IOU_CASH_RETURN);
        billController.save(rb);
        currentReturnBill = rb;
        return true;
    }
    
    public void recreateModle(){
        returnAmount=0.0;
        printPreview=false;
        currentReturnBill=null;
    }

    private void saveBillItem() {
        BillItem tmp = new BillItem();

        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setBill(getCurrent());
        tmp.setNetValue(0 - getCurrent().getNetTotal());
        getBillItemFacade().create(tmp);

    }
    private String tabId = "tabStaff";

    public void recreateModel() {
        current = null;
        printPreview = false;
        newPerson = null;
        comment = null;

        tabId = "tabStaff";
    }

    public void onTabChange(TabChangeEvent event) {
        setTabId(event.getTab().getId());
    }

    public void prepareNewBill() {
        recreateModel();
    }

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

    public Person getNewPerson() {
        if (newPerson == null) {
            newPerson = new Person();
        }
        return newPerson;
    }

    public void setNewPerson(Person newPerson) {
        this.newPerson = newPerson;
    }

    public String getTabId() {
        return tabId;
    }

    public void setTabId(String tabId) {
        this.tabId = tabId;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
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

    public double getReturnAmount() {
        return returnAmount;
    }

    public void setReturnAmount(double returnAmount) {
        this.returnAmount = returnAmount;
    }

    public double getReturnTotal() {
        return returnTotal;
    }

    public void setReturnTotal(double returnTotal) {
        this.returnTotal = returnTotal;
    }

    public Bill getCurrentReturnBill() {
        return currentReturnBill;
    }

    public void setCurrentReturnBill(Bill currentReturnBill) {
        this.currentReturnBill = currentReturnBill;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isPrintPriview() {
        return printPriview;
    }

    public void setPrintPriview(boolean printPriview) {
        this.printPriview = printPriview;
    }

    public List<Bill> getBillList() {
        return billList;
    }

    public void setBillList(List<Bill> billList) {
        this.billList = billList;
    }

}

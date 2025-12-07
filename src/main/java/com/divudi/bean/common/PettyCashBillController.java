/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.service.PaymentService;
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
public class PettyCashBillController implements Serializable {

    @Inject
    BillController billController;
    @Inject
    WebUserController webUserController;
    @Inject
    DrawerController drawerController;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    BillBeanController billBean;

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
    @EJB
    PaymentService paymentService;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;

    private Bill current;
    private boolean printPreview = false;
    private Person newPerson;
    PaymentMethodData paymentMethodData;
    String comment;
    private double returnAmount;
    private double returnTotal;
    private Bill currentReturnBill;
    private PaymentMethod paymentMethod;
    private boolean printPriview;
    private List<Bill> billList;

    public PettyCashBillController() {
    }

    public void pettyCashCancelBillApprove() {
        if (current == null) {
            JsfUtil.addErrorMessage("Approved Bill Error");
        }
        Bill b = current.getReferenceBill();
        b.setApproveAt(new Date());
        b.setApproveUser(sessionController.getLoggedUser());
        billController.save(b);
    }

    public String navigatePettyAndIouReprint() {
        if (current.getBillType() == BillType.PettyCash) {
            fillBillsReferredByCurrentBill();
            return "petty_cash_bill_reprint?faces-redirect=true";
        }
        if (current.getBillType() == BillType.IouIssue) {
            return "iou_bill_reprint?faces-redirect=true";
        }
        if (current.getBillType() == BillType.PettyCashCancelApprove) {
            return "petty_cash_bill_reprint?faces-redirect=true";
        }
        return "";
    }

    public String navigateToPettyCashReturn() {
        returnAmount = Math.abs(getCurrent().getNetTotal()) - Math.abs(totalOfRedundedBills);
        if (returnAmount > 0.0) {
            printPriview = false;
            comment = null;
            paymentMethodData = null;
            return "petty_cash_bill_return?faces-redirect=true";
        } else {
            JsfUtil.addErrorMessage("The full amount has been refunded.");
            return "";
        }

    }

    private double totalOfRedundedBills;

    public void fillBillsReferredByCurrentBill() {
        billList = new ArrayList<>();
        totalOfRedundedBills = 0.0;
        String sql = "Select b from Bill b where b.retired=:ret and b.billedBill=:cb";
        HashMap m = new HashMap();
        m.put("ret", false);
        m.put("cb", getCurrent());
        billList = getBillFacade().findByJpql(sql, m);

        if (billList != null) {
            for (Bill b : billList) {
                totalOfRedundedBills += b.getNetTotal();

            }
        }
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
        h.put("btp", BillType.PettyCash);
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

    private void saveBill() {

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PETTY_CASH_ISSUE);

        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.PETTY_CASH_ISSUE);
        getCurrent().setBillType(BillType.PettyCash);

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

    public void approveBill(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No Bill");
        }
        b.setApproveAt(new Date());
        b.setApproveUser(sessionController.getLoggedUser());
        billFacade.edit(b);
        JsfUtil.addSuccessMessage("Approved");
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
            case "tabDepartment":
                if (getCurrent().getToDepartment().getId().equals(null)) {
                    JsfUtil.addErrorMessage("Department?");
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
        List<Payment> payments = createPaymentForPettyCashBill(getCurrent(), getCurrent().getPaymentMethod());
        drawerController.updateDrawerForOuts(payments);
        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public String settleReturnBill() {
        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }
        fillBillsReferredByCurrentBill();
        double maximumRefundedAmount = Math.abs(getCurrent().getNetTotal()) - Math.abs(totalOfRedundedBills);

        if(returnAmount > maximumRefundedAmount){
            String massage = "You can only refund a maximum amount of " + String.format("%.2f", maximumRefundedAmount);
            JsfUtil.addErrorMessage(massage);
            return "";
        }

        if (maximumRefundedAmount > 0.0) {
            if (getCurrent() != null && getCurrent().getId() != null && getCurrent().getId() != 0) {
                currentReturnBill = createPettyCashReturnBill();
                paymentService.createPayment(currentReturnBill, paymentMethodData);
                getBillFacade().edit(getCurrent());
                printPriview = true;
                current = null;
                return "/petty_cash_bill_return_print";
            }else{
                JsfUtil.addErrorMessage("NO Bill.");
                return "";
            }
        } else {
            JsfUtil.addErrorMessage("The full amount has been refunded.");
            return "";
        }
    }

    public Payment createPaymentForPettyCashBillReturn(Bill rb, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(rb);
        p.setPaidValue(0 - Math.abs(rb.getNetTotal()));
        setPaymentMethodData(p, pm);
        return p;
    }

    public Payment createPaymentForPettyCashBillCancellation(Bill cb, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(cb);
        p.setPaidValue(0 - Math.abs(cb.getNetTotal()));
        setPaymentMethodData(p, pm);
        return p;
    }

    public List<Payment> createPaymentForPettyCashBill(Bill b, PaymentMethod pm) {
        List<Payment> payments = new ArrayList<>();
        Payment p = new Payment();
        p.setBill(b);
        p.setPaidValue(0 - Math.abs(b.getNetTotal()));
        setPaymentMethodData(p, pm);
        payments.add(p);
        return payments;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        if (p.getBill().getBillType() == BillType.PettyCash) {
            p.setPaidValue(p.getBill().getNetTotal());
        } else {
            p.setPaidValue(p.getBill().getCashPaid());
        }
        if (p.getId() == null) {
            paymentFacade.create(p);
        }
    }

    public boolean checkForExpireofApproval(Bill b) {
        if (webUserController.hasPrivilege("PettyCashBillApprove")) {
            return false;
        } else {
            if (b == null || b.getId() == null) {
                return true;
            }
            Date now = new Date();
            long differenceInMillis = now.getTime() - b.getCreatedAt().getTime();

            // Check if the difference is more than one day (24 hours in milliseconds)
            long oneDayInMillis = 24 * 60 * 60 * 1000;
            if (differenceInMillis > oneDayInMillis) {
                return true;
            } else {
                return false;
            }
        }
    }

    private Bill createPettyCashReturnBill() {
        Bill rb = new RefundBill();
        rb.copy(getCurrent());

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PETTY_CASH_RETURN);

        rb.invertAndAssignValuesFromOtherBill(getCurrent());
        rb.setBillType(BillType.PettyCashReturn);
        rb.setBillTypeAtomic(BillTypeAtomic.PETTY_CASH_RETURN);
        rb.setBilledBill(getCurrent());
        rb.setReferenceBill(getCurrent());
        rb.setBillDate(new Date());
        rb.setInsId(deptId);
        rb.setDeptId(deptId);
        rb.setBillTime(new Date());
        rb.setCreatedAt(new Date());
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDiscount(0.00);
        rb.setDiscountPercent(0.0);
        rb.setComments(comment);
        rb.setPaymentMethod(paymentMethod);
        rb.setTotal(returnAmount);
        rb.setCashPaid(returnAmount);
        rb.setNetTotal(returnAmount);
        getBillFacade().create(rb);
        return rb;

    }

    @Deprecated
    private boolean savePettyCashReturnBill(Bill rb) {
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
        rb.setBillType(BillType.PettyCashReturn);
        rb.setBillTypeAtomic(BillTypeAtomic.PETTY_CASH_RETURN);
        billController.save(rb);
        currentReturnBill = rb;
        return true;
    }

    public void recreateModle() {
        returnAmount = 0.0;
        printPreview = false;
        currentReturnBill = null;
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

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Title[] getTitle() {
        return Title.values();
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public double getTotalOfRedundedBills() {
        return totalOfRedundedBills;
    }

    public void setTotalOfRedundedBills(double totalOfRedundedBills) {
        this.totalOfRedundedBills = totalOfRedundedBills;
    }

}

/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.PaymentController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
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
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.facade.PaymentFacade;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @EJB
    private PersonFacade personFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PaymentFacade paymentFacade;

    @Inject
    BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    private BillController billController;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    private DrawerController drawerController;
    @Inject
    private PaymentController paymentController;

    private Bill current;
    private List<Payment> myIousToSettle;
    private List<Payment> settlingIuos;
    private List<Payment> paymentsForsettlingIuos;
    private Payment currentPayment;
    private boolean printPreview = false;
    private Person newPerson;
    private PaymentMethodData paymentMethodData;
    private String comment;
    private double returnAmount;
    private double returnTotal;
    private Bill currentReturnBill;
    private PaymentMethod paymentMethod;
    private boolean printPriview;
    private List<Bill> billList;
    private String tabId = "tabStaff";

    private double settlingIouTotal = 0.0;
    private double paymentTotal = 0.0;

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

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public void fillBillsReferredByCurrentBill() {
        billList = new ArrayList<>();
        String sql = "Select b from Bill b where b.retired=:ret and b.billedBill=:cb";
        HashMap m = new HashMap();
        m.put("ret", false);
        m.put("cb", getCurrent());
        billList = getBillFacade().findByJpql(sql, m);
    }

    private boolean errorInIouCreateBill() {
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

    private boolean errorInSettlingIouBill() {
        if (settlingIuos == null) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return true;
        }
        if (settlingIuos.isEmpty()) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return true;
        }
        if (paymentsForsettlingIuos == null) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return true;
        }
        if (paymentsForsettlingIuos.isEmpty()) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return true;
        }
        if (Math.abs(settlingIouTotal) != Math.abs(paymentTotal)) {
            JsfUtil.addErrorMessage("Settling Total and Payment Totals do not match. Please check and retry");
            return true;
        }

        return false;
    }

    private void calculateTotalsForSettlingIouBill() {

        if (settlingIuos == null) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return;
        }
        if (settlingIuos.isEmpty()) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return;
        }
        if (paymentsForsettlingIuos == null) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return;
        }
        if (paymentsForsettlingIuos.isEmpty()) {
            JsfUtil.addErrorMessage("No IOUs selected. ");
            return;
        }
        settlingIouTotal = 0.0;
        paymentTotal = 0.0;

        for (Payment pout : settlingIuos) {
            settlingIouTotal += pout.getPaidValue();
        }
        for (Payment pin : paymentsForsettlingIuos) {
            paymentTotal += pin.getPaidValue();
        }

        getCurrent().setTotal(paymentTotal);
        getCurrent().setNetTotal(paymentTotal);

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

    public String navigateToCreateIou() {
        prepareNewBill();
        return "/cashier/iou_bill?faces-redirect=true";
    }

    public String navigateToTrackIous() {
        return "/cashier/iou_track?faces-redirect=true";
    }

    public String navigateToSettleIous() {
        if (!prepareNewIouSettleBill()) {
            return null;
        }
        return "/cashier/settle_iou?faces-redirect=true";
    }

    private void saveIouCreateBill() {
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(),
                BillTypeAtomic.IOU_CASH_ISSUE);
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.IouIssue);
        getCurrent().setBillClassType(BillClassType.BilledBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.IOU_CASH_ISSUE);
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setTotal(0 - getCurrent().getNetTotal());
        getCurrent().setNetTotal(0 - getCurrent().getNetTotal());

        getBillFacade().create(getCurrent());
    }

    private void saveSettleIouBill() {
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(),
                BillTypeAtomic.IOU_SETTLE);
        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.IouSettle);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.IOU_SETTLE);
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setTotal(0 - getCurrent().getNetTotal());
        getCurrent().setNetTotal(0 - getCurrent().getNetTotal());

        getBillFacade().create(getCurrent());
    }

    public String navigateToIouReturnBill() {
        return "";
    }

    public void addPaymentToSettlingIouBill() {
        if (current == null) {
            JsfUtil.addErrorMessage("Error - Current Null");
            return;
        }
        if (current.getBillType() != BillType.IouSettle) {
            JsfUtil.addErrorMessage("Error - Wrong Bill Type");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error - No current Payment");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        currentPayment.setDepartment(sessionController.getDepartment());
        currentPayment.setInstitution(sessionController.getInstitution());
        currentPayment.setBill(current);
        getPaymentsForsettlingIuos().add(currentPayment);
        calculateTotalsForSettlingIouBill();
        currentPayment = null;
        getCurrentPayment();
    }

    public void settleIouSettlingBill() {
        if (errorInSettlingIouBill()) {
            return;
        }
        saveIouCreateBill();

        for (Payment pi : getPaymentsForsettlingIuos()) {
            pi.setBill(current);
            pi.setCreatedAt(new Date());
            pi.setCreater(sessionController.getLoggedUser());
            pi.setCurrentHolder(sessionController.getLoggedUser());
            paymentController.save(pi);
        }
        for (Payment po : getSettlingIuos()) {
            po.setCancelledBill(current);
            po.setCancelled(true);
            po.setCancelledBy(sessionController.getLoggedUser());
            po.setCancelledAt(new Date());
            paymentController.save(po);
        }

        drawerController.updateDrawerForOuts(getSettlingIuos());
        drawerController.updateDrawerForIns(getPaymentsForsettlingIuos());
        JsfUtil.addSuccessMessage("IOU Settled Successfully");
        settlingIuos=null;
        myIousToSettle=null;
        printPreview = true;

    }

    public void settleIouCreateBill() {
        if (errorInIouCreateBill()) {
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
        saveIouCreateBill();
        saveBillItem();
        List<Payment> paymentOuts = getBillBean().createPaymentsForNonCreditOuts(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        PaymentMethodData pmd = new PaymentMethodData();
        pmd.setPaymentMethod(PaymentMethod.IOU);
        pmd.getIou().setReferenceNo(getCurrent().getReferenceNumber());
        List<Payment> paymentsIn = getBillBean().createPaymentsForNonCreditIns(getCurrent(),
                PaymentMethod.IOU,
                pmd);
        drawerController.updateDrawerForOuts(paymentOuts);
        drawerController.updateDrawerForIns(paymentsIn);
        paymentsForsettlingIuos=null;
        myIousToSettle=null;
        settlingIuos=null;
        JsfUtil.addSuccessMessage("IOU Created");
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
            printPriview = true;
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
        rb.invertAndAssignValuesFromOtherBill(getCurrent());
        rb.setBillType(BillType.IouSettle);
        rb.setBillTypeAtomic(BillTypeAtomic.IOU_SETTLE);
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
        rb.setBillType(BillType.IouSettle);
        rb.setBillTypeAtomic(BillTypeAtomic.IOU_SETTLE);
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
        current = new Bill();
        current.setBillType(BillType.IouIssue);
        current.setBillTypeAtomic(BillTypeAtomic.IOU_CASH_ISSUE);
        current.setDepartment(sessionController.getDepartment());
        current.setInstitution(sessionController.getInstitution());
        printPreview = false;

    }

    public boolean prepareNewIouSettleBill() {
        recreateModel();
        current = new Bill();
        current.setBillType(BillType.IouSettle);
        current.setBillTypeAtomic(BillTypeAtomic.IOU_SETTLE);
        current.setDepartment(sessionController.getDepartment());
        current.setInstitution(sessionController.getInstitution());
        printPreview = false;
        String jpql = "select p "
                + " from Payment p "
                + " where p.retired=:ret "
                + " and p.currentHolder=:user "
                + " and p.paymentMethod=:pm"
                + " and p.cancelled=:can ";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("can", false);
        params.put("pm", PaymentMethod.IOU);
        params.put("user", sessionController.getLoggedUser());
        myIousToSettle = paymentFacade.findByJpql(jpql, params);
        paymentsForsettlingIuos=new ArrayList<>();
        settlingIuos = new ArrayList<>();
        if (myIousToSettle == null) {
            JsfUtil.addErrorMessage("You do not have any IOUs to settle");
            return false;
        }

        return true;
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

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public DrawerController getDrawerController() {
        return drawerController;
    }

    public void setDrawerController(DrawerController drawerController) {
        this.drawerController = drawerController;
    }

    public List<Payment> getMyIousToSettle() {
        return myIousToSettle;
    }

    public void setMyIousToSettle(List<Payment> myIousToSettle) {
        this.myIousToSettle = myIousToSettle;
    }

    public List<Payment> getSettlingIuos() {
        return settlingIuos;
    }

    public void setSettlingIuos(List<Payment> settlingIuos) {
        this.settlingIuos = settlingIuos;
    }

    public List<Payment> getPaymentsForsettlingIuos() {
        if (paymentsForsettlingIuos == null) {
            paymentsForsettlingIuos = new ArrayList<>();
        }
        return paymentsForsettlingIuos;
    }

    public void setPaymentsForsettlingIuos(List<Payment> paymentsForsettlingIuos) {
        this.paymentsForsettlingIuos = paymentsForsettlingIuos;
    }

    public Payment getCurrentPayment() {
        if (currentPayment == null) {
            currentPayment = new Payment();
        }
        return currentPayment;
    }

    public void setCurrentPayment(Payment currentPayment) {
        this.currentPayment = currentPayment;
    }

    public double getSettlingIouTotal() {
        return settlingIouTotal;
    }

    public void setSettlingIouTotal(double settlingIouTotal) {
        this.settlingIouTotal = settlingIouTotal;
    }

    public double getPaymentTotal() {
        return paymentTotal;
    }

    public void setPaymentTotal(double paymentTotal) {
        this.paymentTotal = paymentTotal;
    }

}

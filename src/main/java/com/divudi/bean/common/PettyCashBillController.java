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
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PettyCashType;
import com.divudi.core.data.RequestStatus;
import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Request;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.service.PaymentService;
import com.divudi.service.RequestService;
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
    PaymentSchemeController paymentSchemeController;
    @Inject
    BillBeanController billBean;
    @Inject
    DrawerController drawerController;

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

    private PettyCashType currentBillType;
    private String financialYear;
    private Integer invoiceNo;

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
    
    private boolean duplicate;
    private boolean preBill;
    
    public String navigatePettyCashReprint(Bill selectedBill) {
        if(selectedBill == null){
            JsfUtil.addErrorMessage("BillType is Missing.");
            return "";
        }
        
        setCurrent(selectedBill);
        
        if (null == current.getBillTypeAtomic()){
            JsfUtil.addErrorMessage("BillType is Worng.");
            return "";
        }else switch (current.getBillTypeAtomic()) {
            case PETTY_CASH_PRE:
                printPreview = true;
                duplicate = true;
                preBill = true;
                return "petty_cash_prebill_reprint?faces-redirect=true";
            case PETTY_CASH_ISSUE:
                printPreview = true;
                duplicate = true;
                preBill = false;
                return "petty_cash_bill_reprint?faces-redirect=true";
            case PETTY_CASH_BILL_CANCELLATION:
                printPreview = true;
                duplicate = true;
                preBill = false;
                return "";
            default:
                JsfUtil.addErrorMessage("BillType is Worng.");
                return "";
        }
        
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

    private String getFullyInvoiceNo(String financialYear, Integer invoiceNo) {
        DecimalFormat df = new DecimalFormat("00000");
        System.out.println("Entered Financial Year = " + financialYear);
        System.out.println("Entered Integer Invoice Number = " + invoiceNo);

        String insNo = df.format(invoiceNo.intValue());
        System.out.println("s = " + insNo);

        return (financialYear + "-" + insNo);

    }

    private boolean checkInvoiceNo() {

        String fullInvoiceNumber = getFullyInvoiceNo(financialYear, invoiceNo);

        System.out.println("fullInvoiceNumber = " + fullInvoiceNumber);

        return checkValidInvoiceNumber(BillTypeAtomic.PETTY_CASH_PRE,fullInvoiceNumber);
    }

    public boolean checkValidInvoiceNumber(BillTypeAtomic type, String invoiceNumber) {
        
        Calendar year = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.set(year.get(Calendar.YEAR), 3, 1, 0, 0, 0);
        Date fd = c.getTime();
        
        String sql = "Select b From BilledBill b where "
                + " b.retired=false "
                + " and b.cancelled=false "
                + " and b.billTypeAtomic= :bta "
                + " and b.createdAt > :fd "
                + " and b.invoiceNumber =:inv ";
        HashMap h = new HashMap();
        h.put("bta", type);
        h.put("fd", fd);
        h.put("inv", invoiceNumber);
        Bill tmp = getBillFacade().findFirstByJpql(sql, h, TemporalType.TIMESTAMP);

        if (tmp != null) {
            return true;
        }

        return false;
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
        String s = s1.substring(2, 4) + s2.substring(2, 4);

        return s;
    }

    private void savePreBill() {

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PETTY_CASH_PRE);

        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.PETTY_CASH_PRE);
        getCurrent().setBillType(BillType.PettyCash);

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

    public void approveBill(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No Bill");
        }
        b.setApproveAt(new Date());
        b.setApproveUser(sessionController.getLoggedUser());
        billFacade.edit(b);
        JsfUtil.addSuccessMessage("Approved");
    }

    public void settlePreBill() {
        if(errorCheck()){
            return ;
        }

        if (currentBillType == PettyCashType.NEWPERSON) {
            personFacade.create(newPerson);
            System.out.println("New Person ID = " + newPerson.getId());
            getCurrent().setPerson(newPerson);
        }

        getCurrent().setTotal(getCurrent().getNetTotal());
        getCurrent().setInvoiceNumber(getFullyInvoiceNo(financialYear, invoiceNo));
        
        savePreBill();

        saveBillItem();
        
        Request newlyRequest = new Request();

        newlyRequest.setBill(getCurrent());
        newlyRequest.setRequester(sessionController.getLoggedUser());
        newlyRequest.setRequestAt(new Date());
        newlyRequest.setRequestType(RequestType.PETTYCASH_APROVEL);
        newlyRequest.setStatus(RequestStatus.PENDING);
        newlyRequest.setInstitution(sessionController.getInstitution());
        newlyRequest.setDepartment(sessionController.getDepartment());

        String reqNo = billNumberGenerator.departmentRequestNumberGeneratorYearly(sessionController.getDepartment(), RequestType.PETTYCASH_APROVEL);
        newlyRequest.setRequestNo(reqNo);

        requestService.save(newlyRequest, sessionController.getLoggedUser());
        
        getCurrent().setCurrentRequest(newlyRequest);
        billFacade.edit(current);

        //List<Payment> payments = createPaymentForPettyCashBill(getCurrent(), getCurrent().getPaymentMethod());
        //drawerController.updateDrawerForOuts(payments);
        printPreview = true;
        duplicate = false;
        preBill = true;
        
        JsfUtil.addSuccessMessage("Bill Saved");
        

    }
    
    private Request currentRequest;
    
    public void settleBill(Bill preBill) {

        //Create BilledBill
        Bill bill = new Bill();

        bill.setInvoiceNumber(preBill.getInvoiceNumber());
        bill.setPerson(preBill.getPerson());
        bill.setStaff(preBill.getStaff());
        bill.setToDepartment(preBill.getToDepartment());
        bill.setPaymentMethod(preBill.getPaymentMethod());
        bill.setTotal(preBill.getTotal());
        bill.setNetTotal(preBill.getNetTotal());
        bill.setReferenceBill(preBill);
        
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PETTY_CASH_ISSUE);
        System.out.println("deptId = " + deptId);
        bill.setInsId(deptId);
        bill.setDeptId(deptId);
        bill.setBillTypeAtomic(BillTypeAtomic.PETTY_CASH_ISSUE);
        bill.setBillType(BillType.PettyCash);

        bill.setDepartment(getSessionController().getDepartment());
        bill.setInstitution(getSessionController().getInstitution());

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());

        bill.setCreatedAt(new Date());
        bill.setCreater(getSessionController().getLoggedUser());

        getBillFacade().create(bill);
        
        //Updtae Pre Bill
        preBill.setReferenceBill(bill);
        getBillFacade().edit(preBill);
        
        System.out.println("Create new Bill / Id = " + bill + " / Bill Numbeer = " + bill.getDeptId());
        
        //create Payment
        List<Payment> payments = createPaymentForPettyCashBill(bill, bill.getPaymentMethod());
        System.out.println("payments = " + payments);
        
        //Update User Drawer
        drawerController.updateDrawerForOuts(payments);
        
    }

    @Inject
    RequestService requestService;

    public String settleReturnBill() {
        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }
        fillBillsReferredByCurrentBill();
        double maximumRefundedAmount = Math.abs(getCurrent().getNetTotal()) - Math.abs(totalOfRedundedBills);

        if (returnAmount > maximumRefundedAmount) {
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
            } else {
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
    
    public boolean errorCheck(){
        if (currentBillType == null) {
            JsfUtil.addErrorMessage("Petty-Cash Type is Missing.");
            return true;
        }

        switch (currentBillType) {
            case STAFF:
                if (current.getStaff() == null) {
                    JsfUtil.addErrorMessage("Staff is Missing.");
                    return true;
                }
                current.setToDepartment(null);
                current.setPerson(null);
                break;
            case DEPARTMENT:
                if (current.getToDepartment() == null) {
                    JsfUtil.addErrorMessage("Department is Missing.");
                    return true;
                }
                current.setStaff(null);
                current.setPerson(null);
                break;
            case PERSON:
                if (current.getPerson() == null) {
                    JsfUtil.addErrorMessage("Person is Missing.");
                    return true;
                }
                current.setToDepartment(null);
                current.setStaff(null);
                break;
            case NEWPERSON:
                if (newPerson == null) {
                    JsfUtil.addErrorMessage("Error in New Person.");
                    return true;
                }
                if (newPerson.getTitle() == null) {
                    JsfUtil.addErrorMessage("Title is Missing in New Person.");
                    return true;
                }
                if (newPerson.getName() == null || newPerson.getName().trim().isEmpty()) {
                    JsfUtil.addErrorMessage("Name is Missing in New Person.");
                    return true;
                }
                if (newPerson.getSex() == null) {
                    JsfUtil.addErrorMessage("Gender is Missing in New Person.");
                    return true;
                }
                if (newPerson.getArea() == null) {
                    JsfUtil.addErrorMessage("Address is Missing in New Person.");
                    return true;
                }
                if (newPerson.getPhone() == null) {
                    JsfUtil.addErrorMessage("Mobile is Missing in New Person.");
                    return true;
                }
                current.setToDepartment(null);
                current.setStaff(null);
                current.setPerson(null);
                break;
        }

        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select the PaymentMethod");
            return true;
        }

        if (getCurrent().getNetTotal() < 1) {
            JsfUtil.addErrorMessage("Type Amount");
            return true;
        }

        if (financialYear == null || financialYear.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Financial Year is Missing");
            return true;
        }

        if (invoiceNo == null) {
            JsfUtil.addErrorMessage("Invoice No is Missing.");
            return true;
        }

        if (checkInvoiceNo()) {
            JsfUtil.addErrorMessage("Invoice Number Already Exist");
            return true;
        }

        if (current != null && current.getId() != null) {
            JsfUtil.addErrorMessage("Bill already saved. Please start a new bill.");
            return true;
        }
        
        Drawer loggedUserDrawer = drawerController.getUsersDrawer(sessionController.getLoggedUser());

        System.out.println("loggedUserDrawer = " + loggedUserDrawer);

        if (loggedUserDrawer == null) {
            JsfUtil.addErrorMessage("Your Drawer have a Error.");
            return true;
        }
        System.out.println("loggedUserDrawer.getCashInHandValue() = " + loggedUserDrawer.getCashInHandValue());

        if (loggedUserDrawer != null && (loggedUserDrawer.getCashInHandValue() == null || loggedUserDrawer.getCashInHandValue() == 0)) {
            JsfUtil.addErrorMessage("There is no cash in your drawer.");
            return true;
        }

        if (loggedUserDrawer.getCashInHandValue() < getCurrent().getNetTotal()) {
            JsfUtil.addErrorMessage("There is not enough cash in your drawer.");
            return true;
        }
        
        return false;
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
        currentBillType = null;
        tabId = "tabStaff";
    }

    public void onTabChange(TabChangeEvent event) {
        setTabId(event.getTab().getId());
    }

    public void prepareNewBill() {
        recreateModel();
        duplicate = false;
        preBill = true;
        printPreview = false;
        financialYear = createInvoiceNumberSuffix();
        invoiceNo = null;
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

    public void listnerForPaymentMethodChange() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }

        if (current != null && current.getPaymentMethod() != null) {
            // Initialize payment method specific data based on selection
            switch (current.getPaymentMethod()) {
                case Card:
                    paymentMethodData.getCreditCard().setTotalValue(current.getNetTotal());
                    break;
                case Cheque:
                    paymentMethodData.getCheque().setTotalValue(current.getNetTotal());
                    break;
                case Slip:
                    paymentMethodData.getSlip().setTotalValue(current.getNetTotal());
                    break;
                case ewallet:
                    paymentMethodData.getEwallet().setTotalValue(current.getNetTotal());
                    break;
                case Cash:
                    paymentMethodData.getCash().setTotalValue(current.getNetTotal());
                    break;
                default:
                    break;
            }
        }
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

    public PettyCashType getCurrentBillType() {
        return currentBillType;
    }

    public void setCurrentBillType(PettyCashType currentBillType) {
        this.currentBillType = currentBillType;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public Integer getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(Integer invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Request getCurrentRequest() {
        return currentRequest;
    }

    public void setCurrentRequest(Request currentRequest) {
        this.currentRequest = currentRequest;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public boolean isPreBill() {
        return preBill;
    }

    public void setPreBill(boolean preBill) {
        this.preBill = preBill;
    }

}

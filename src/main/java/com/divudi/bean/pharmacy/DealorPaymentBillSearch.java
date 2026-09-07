/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.CreditBean;
import com.divudi.ejb.EjbApplication;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.core.facade.CancelledBillFacade;
import com.divudi.core.facade.RefundBillFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class DealorPaymentBillSearch implements Serializable {

    private boolean printPreview = false;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    String txtSearch;
    BilledBill bill;
    List<BillEntry> billEntrys;
    List<BillItem> billItems;
    List<BillComponent> billComponents;
    List<BillFee> billFees;
    List<Bill> bills;

    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    CancelledBillFacade cancelledBillFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    BilledBillFacade billedBillFacade;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private RefundBillFacade refundBillFacade;
    @Inject
    SessionController sessionController;

    @EJB
    EjbApplication ejbApplication;
    private List<BillItem> tempbillItems;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    private String comment;
    private double netTotal;
    WebUser user;

    public void approve() {
        if (getBill().getReferenceBill() != null && !getBill().isReactivated()) {
            JsfUtil.addErrorMessage("Already Approved");
            return;
        }
        BilledBill newBill = new BilledBill();
        newBill.copy(getBill());
        newBill.copyValue(getBill());
        newBill.setCreatedAt(new Date());
        newBill.setComments(comment);
        newBill.setCreater(sessionController.getLoggedUser());
        newBill.setInstitution(sessionController.getInstitution());
        newBill.setDepartment(sessionController.getDepartment());
        newBill.setBillType(BillType.GrnPayment);
        newBill.setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT);
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLIER_PAYMENT);
        newBill.setDeptId(deptId);
        newBill.setApproveAt(new Date());
        newBill.setApproveUser(sessionController.getLoggedUser());
        newBill.setApprovedAnyTest(true);
        billFacade.create(newBill);

        if(getBill().isReactivated()){
            getBill().setReactivated(false);
        }
        bill.setReferenceBill(newBill);
        billFacade.edit(bill);

        for (BillItem bi : getBillItems()) {
            BillItem newBi = new BillItem();
            newBi.copy(bi);
            newBi.setBill(newBill);
            newBi.setCreatedAt(new Date());
            newBi.setCreater(sessionController.getLoggedUser());
            newBi.setReferanceBillItem(bi);
            billItemFacede.create(newBi);
            bi.setReferanceBillItem(newBi);
            billItemFacede.edit(bi);
        }

        JsfUtil.addSuccessMessage("Succesfully Approved");
    }

    @Deprecated
    public void fillDealorPaymentDone() {
        bills = null;
        netTotal = 0.0;
        String jpql;
        Map params = new HashMap();

        jpql = "select b from Bill b "
                + " where b.retired=false "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.billType = :billTypes "
                + " and b.billTypeAtomic = :bTA ";

        params.put("billTypes", BillType.GrnPayment);
        params.put("bTA", BillTypeAtomic.SUPPLIER_PAYMENT);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        Iterator<Bill> iterator = bills.iterator();
        while (iterator.hasNext()) {
            Bill b = iterator.next();
            netTotal += b.getNetTotal();
        }
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        // recreateModel();
        this.user = user;
        recreateModel();
    }

    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public void setEjbApplication(EjbApplication ejbApplication) {
        this.ejbApplication = ejbApplication;
    }

//    public List<Bill> getUserBillsOwn() {
//        List<Bill> userBills;
//        if (getUser() == null) {
//            userBills = new ArrayList<>();
//        } else {
//            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);
//        }
//        if (userBills == null) {
//            userBills = new ArrayList<>();
//        }
//        return userBills;
//    }
//    public List<Bill> getBillsOwn() {
//        if (bills == null) {
//            if (txtSearch == null || txtSearch.trim().equals("")) {
//                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.CashRecieveBill);
//            } else {
//                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.CashRecieveBill);
//            }
//            if (bills == null) {
//                bills = new ArrayList<>();
//            }
//        }
//        return bills;
//    }
    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BilledBillFacade getBilledBillFacade() {
        return billedBillFacade;
    }

    public void setBilledBillFacade(BilledBillFacade billedBillFacade) {
        this.billedBillFacade = billedBillFacade;
    }

    public CancelledBillFacade getCancelledBillFacade() {
        return cancelledBillFacade;
    }

    public void setCancelledBillFacade(CancelledBillFacade cancelledBillFacade) {
        this.cancelledBillFacade = cancelledBillFacade;
    }

    public void recreateModel() {

        billFees = null;
//        billFees
        billComponents = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
    }

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private CancelledBill createCancelBill() {
        CancelledBill cb = new CancelledBill();

        cb.setBilledBill(bill);
        cb.copy(bill);
        cb.invertAndAssignValuesFromOtherBill(bill);
//        cb.setNetTotal(0 - Math.abs(cb.getNetTotal()));
        cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.CancelledBill, BillNumberSuffix.CRDCAN));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.CashRecieveBill, BillClassType.CancelledBill, BillNumberSuffix.CRDCAN));

        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT_CANCELLED);
        cb.setPaymentMethod(getBill().getPaymentMethod());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        cb.setComments(comment);

        if (cb.getId() == null) {
            getBillFacade().create(cb);
        }
        return cb;
    }

    public void returnBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                JsfUtil.addErrorMessage("Error in check, cannot proceed.");
                return;
            }

            RefundBill rb = createRefundBill();
            if (rb.getId() == null) {
                JsfUtil.addErrorMessage("Refund Bill creation failed.");
                return;
            }

            Payment p = pharmacyDealorBill.createPayment(rb, getBill().getPaymentMethod());
            if (p == null) {
                JsfUtil.addErrorMessage("Payment creation failed.");
                return;
            }

            returnBillItems(rb, p);
            getBill().setRefunded(true);
            getBill().setRefundedBill(rb);

            try {
                getBilledBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Returned");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Failed to update bill: " + e.getMessage());
                return;
            }

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(rb, getSessionController().getLoggedUser());
            if (wb != null) {
                getSessionController().setLoggedUser(wb);
                printPreview = true;
                JsfUtil.addSuccessMessage("Successfully Returned");
            } else {
                JsfUtil.addErrorMessage("Cash transaction saving failed.");
            }
            JsfUtil.addSuccessMessage("Successfully Returned");
        } else {
            JsfUtil.addErrorMessage("No Bill to return");
        }
    }

    private RefundBill createRefundBill() {
        RefundBill rb = new RefundBill();

        rb.setBilledBill(getBill());
        rb.copy(getBill());
        rb.invertAndAssignValuesFromOtherBill(getBill());
        rb.setNetTotal(0 - Math.abs(rb.getNetTotal()));
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLIER_PAYMENT_RETURNED);
        rb.setDeptId(deptId);
        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.GrnPayment, BillClassType.RefundBill, BillNumberSuffix.CRDCAN));

        rb.setBillType(BillType.GrnPayment);
        rb.setBillTypeAtomic(BillTypeAtomic.SUPPLIER_PAYMENT_RETURNED);

        rb.setBillDate(new Date());
        rb.setBillTime(new Date());
        rb.setCreatedAt(new Date());
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setPaymentMethod(getBill().getPaymentMethod());
        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        rb.setInstitution(getSessionController().getInstitution());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setComments(comment);

        if (rb.getId() == null) {
            getBillFacade().create(rb);
        } else {
            getBillFacade().edit(rb);
        }

        return rb;
    }

    private boolean errorCheck() {
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

        if (getComment() == null || getComment().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

//    if (getTabId().equals("tabBht")) {
//            savePayments();
//        }
//    private void savePayments() {
//        for (BillItem b : getBillItems()) {
//            Bill bil = saveBhtPaymentBill(b);
//            saveBhtBillItem(bil);
//        }
//    }
//      private Bill saveBhtPaymentBill(BillItem b) {
//        Bill tmp = new BilledBill();
//        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment()));
//        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment()));
//
//        tmp.setBillType(BillType.InwardPaymentBill);
//        tmp.setPatientEncounter(b.getPatientEncounter());
//        tmp.setPatient(b.getPatientEncounter().getPatient());
//        tmp.setPaymentScheme(getCurrent().getPaymentScheme());
//        tmp.setNetTotal(b.getNetValue());
//        tmp.setCreatedAt(new Date());
//        tmp.setCreater(getSessionController().getLoggedUser());
//        getBillFacade().create(tmp);
//
//        return tmp;
//    }
//
//       private void saveBhtBillItem(Bill b) {
//        BillItem temBi = new BillItem();
//        temBi.setBill(b);
//        temBi.setNetValue(b.getNetTotal());
//        temBi.setCreatedAt(new Date());
//        temBi.setCreater(getSessionController().getLoggedUser());
//        getBillItemFacade().create(temBi);
//    }
    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    @Inject
    SupplierPaymentController pharmacyDealorBill;

    public void cancelBill() {
        if (bill != null && bill.getId() != null && bill.getId() != 0) {
            if (errorCheck()) {
                return;
            }

            CancelledBill cb = createCancelBill();
            Payment p = pharmacyDealorBill.createPayment(cb, bill.getPaymentMethod());

            cancelBillItems(cb, p);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            JsfUtil.addSuccessMessage("Successfully Cancelled");
            getBilledBillFacade().edit(bill);

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }
    List<Bill> billsToApproveCancellation;
    List<Bill> billsApproving;
    private Bill billForCancel;

    public void approveCancellation() {

        if (billsApproving == null) {
            JsfUtil.addErrorMessage("Select Bill to Approve Cancell");
            return;
        }
        for (Bill b : billsApproving) {

            b.setApproveUser(getSessionController().getCurrent());
            b.setApproveAt(Calendar.getInstance().getTime());
            if (b.getId() == null) {
                getBillFacade().create(b);
            }

            cancelBillItems(b);
            b.getBilledBill().setCancelled(true);
            b.getBilledBill().setCancelledBill(b);

            getBilledBillFacade().edit(getBill());

            ejbApplication.getBillsToCancel().remove(b);

            JsfUtil.addSuccessMessage("Cancelled");

        }

        billForCancel = null;
    }

    public List<Bill> getBillsToApproveCancellation() {
        billsToApproveCancellation = ejbApplication.getBillsToCancel();
        return billsToApproveCancellation;
    }

    public void setBillsToApproveCancellation(List<Bill> billsToApproveCancellation) {
        this.billsToApproveCancellation = billsToApproveCancellation;
    }

    public List<Bill> getBillsApproving() {
        return billsApproving;
    }

    public void setBillsApproving(List<Bill> billsApproving) {
        this.billsApproving = billsApproving;
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

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            updateReferenceBill(b);
        }
    }

    private void cancelBillItems(Bill can, Payment p) {
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);
            b.setReferenceBill(nB.getReferenceBill());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }
            pharmacyDealorBill.saveBillFee(b, p);
            updateReferenceBill(b);
        }
    }

    private void returnBillItems(Bill can, Payment p) {
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);
            b.setReferenceBill(nB.getReferenceBill());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }
            pharmacyDealorBill.saveBillFee(b, p);
            updateReferenceBill(b);
        }
    }

    @EJB
    private CreditBean creditBean;

    private void updateReferenceBill(BillItem tmp) {
        double dbl = getCreditBean().getPaidAmount(tmp.getReferenceBill(), BillType.GrnPayment);

        tmp.getReferenceBill().setPaidAmount(0 - dbl);
        getBillFacade().edit(tmp.getReferenceBill());

    }

    @Inject
    private BillBeanController billBean;

    public List<Bill> getBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }
    @EJB
    private BillFacade billFacade;

    public List<Bill> getInstitutionPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();

            if (txtSearch == null || txtSearch.trim().isEmpty()) {
                sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";
            } else {
                sql = "select b from BilledBill b where b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
            }

            temMap.put("toDate", getToDate());
            temMap.put("fromDate", getFromDate());
            temMap.put("type", BillType.PaymentBill);

            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;

    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getOpdBills() {
        if (txtSearch == null || txtSearch.trim().isEmpty()) {
            bills = getBillBean().billsForTheDay(fromDate, toDate, BillType.OpdBill);
        } else {
            bills = getBillBean().billsFromSearch(txtSearch, fromDate, toDate, BillType.OpdBill);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
        recreateModel();
    }

    public BilledBill getBill() {
        //recreateModel();
        return bill;
    }

    public void setBill(BilledBill bill) {
        recreateModel();
        this.bill = bill;
    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        if (getBill() != null && billItems == null) {
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billItems = getBillItemFacede().findByJpql(sql);

        }
        if (billItems == null) {
            billItems = new ArrayList<BillItem>();
        }

        return billItems;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<BillComponent>();
            }
        }
        return billComponents;
    }

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
                if (billFees == null) {
                    billFees = new ArrayList<BillFee>();
                }
            }
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findByJpql(sql);
            if (billFees == null) {
                billFees = new ArrayList<BillFee>();
            }

        }

        return billFees;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    /**
     * Creates a new instance of BillSearch
     */
    public DealorPaymentBillSearch() {
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillItemFacade getBillItemFacede() {
        return billItemFacede;
    }

    public void setBillItemFacede(BillItemFacade billItemFacede) {
        this.billItemFacede = billItemFacede;
    }

    public BillComponentFacade getBillCommponentFacade() {
        return billCommponentFacade;
    }

    public void setBillCommponentFacade(BillComponentFacade billCommponentFacade) {
        this.billCommponentFacade = billCommponentFacade;
    }

    public RefundBillFacade getRefundBillFacade() {
        return refundBillFacade;
    }

    public void setRefundBillFacade(RefundBillFacade refundBillFacade) {
        this.refundBillFacade = refundBillFacade;
    }

    public Bill getBillForCancel() {
        return billForCancel;
    }

    public void setBillForCancel(CancelledBill billForCancel) {
        this.billForCancel = billForCancel;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<BillItem> getTempbillItems() {
        if (tempbillItems == null) {
            tempbillItems = new ArrayList<BillItem>();
        }
        return tempbillItems;
    }

    public void setTempbillItems(List<BillItem> tempbillItems) {
        this.tempbillItems = tempbillItems;
    }

    public void resetLists() {
        recreateModel();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        resetLists();
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        resetLists();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }
}

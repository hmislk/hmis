/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.common.WebUserController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.CreditBean;
import com.divudi.ejb.EjbApplication;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BilledBillFacade;
import com.divudi.facade.CancelledBillFacade;
import com.divudi.facade.RefundBillFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
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
    String txtSearch;
    BilledBill bill;
    List<BillEntry> billEntrys;
    List<BillItem> billItems;
    List<BillComponent> billComponents;
    List<BillFee> billFees;
    List<Bill> bills;

    private CommonFunctions commonFunctions;
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
    @Inject
    private WebUserController webUserController;
    @EJB
    EjbApplication ejbApplication;
    private List<BillItem> tempbillItems;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    private String comment;
    WebUser user;

    public void approve() {
        if (getBill().getReferenceBill() != null) {
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
        billFacade.create(newBill);

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
//            //////// // System.out.println("user is null");
//        } else {
//            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);
//            //////// // System.out.println("user ok");
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

        cb.setBilledBill(getBill());
        cb.copy(getBill());
        cb.invertValue(getBill());

        cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.CancelledBill, BillNumberSuffix.CRDCAN));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.CashRecieveBill, BillClassType.CancelledBill, BillNumberSuffix.CRDCAN));

        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setPaymentMethod(getBill().getPaymentMethod());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        cb.setComments(comment);
        
        if (cb.getId()==null) {
            getBillFacade().create(cb);
        }

        return cb;
    }

    private boolean errorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

        if (getComment() == null || getComment().trim().equals("")) {
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
    PharmacyDealorBill pharmacyDealorBill;

    public void cancelBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }

            CancelledBill cb = createCancelBill();
            Payment p = pharmacyDealorBill.createPayment(cb, getBill().getPaymentMethod());

            //Copy & paste
            //  if (webUserController.hasPrivilege("LabBillCancelling")) {
            if (true) {
//                if (cb.getId() == null) {
//                    getCancelledBillFacade().create(cb);
//                }
                cancelBillItems(cb, p);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBilledBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");

                WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
                getSessionController().setLoggedUser(wb);
                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

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
        //////// // System.out.println("1");
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
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<Bill>();
            }
        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<Bill>();
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
            if (bills == null) {
                if (txtSearch == null || txtSearch.trim().equals("")) {
                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";
                    temMap.put("toDate", getToDate());
                    temMap.put("fromDate", getFromDate());
                    temMap.put("type", BillType.PaymentBill);
                    bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                } else {
                    sql = "select b from BilledBill b where b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
                    temMap.put("toDate", getToDate());
                    temMap.put("fromDate", getFromDate());
                    temMap.put("type", BillType.PaymentBill);
                    bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);
                }
                if (bills == null) {
                    bills = new ArrayList<Bill>();
                }
            }
        }
        return bills;

    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        //////// // System.out.println("getting user bills");
        if (getUser() == null) {
            userBills = new ArrayList<Bill>();
            //////// // System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
            //////// // System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<Bill>();
        }
        return userBills;
    }

    public List<Bill> getOpdBills() {
        if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay(fromDate, toDate, BillType.OpdBill);
        } else {
            bills = getBillBean().billsFromSearch(txtSearch, fromDate, toDate, BillType.OpdBill);
        }

        if (bills == null) {
            bills = new ArrayList<Bill>();
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

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
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
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        resetLists();
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        resetLists();
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
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
}

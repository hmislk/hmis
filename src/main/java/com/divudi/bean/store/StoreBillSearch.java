/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.common.WebUserController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.EjbApplication;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
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
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.LazyDataModel;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class StoreBillSearch implements Serializable {

    private boolean printPreview = false;
    private double refundAmount;
    String txtSearch;
    Bill bill;
    PaymentMethod paymentMethod;
    PaymentScheme paymentScheme;
    BillItem currentBillItem;
    private RefundBill billForRefund;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    //  private String comment;
    WebUser user;
    StoreBean storeBean;
    ////////////////
    List<BillItem> refundingItems;
    List<Bill> bills;
    private List<Bill> filteredBill;
    private List<Bill> selectedBills;
    List<BillEntry> billEntrys;
    List<BillItem> billItems;
    List<BillComponent> billComponents;
    List<BillFee> billFees;
    private List<BillItem> tempbillItems;
    List<Bill> searchRetaiBills;

    //////////////////
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    /////////
    private CommonFunctions commonFunctions;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    StoreBean StoreBean;
    @EJB
    EjbApplication ejbApplication;
    ///////////////////
    @Inject
    SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    public void calBillItemsTotal() {
        if (billItems == null) {
            return;
        }
        for (BillItem b : billItems) {
            b.setTmpQty(b.getPharmaceuticalBillItem().getQty());
        }
    }

    public void editBill() {
        if (errorCheckForEdit()) {
            return;
        }
        getBillFacade().edit(getBill());
    }

    public void editBill(Bill bill) {

        getBillFacade().edit(bill);
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            PharmaceuticalBillItem cuPharmaceuticalBillItem = new PharmaceuticalBillItem();
            currentBillItem.setPharmaceuticalBillItem(cuPharmaceuticalBillItem);
            cuPharmaceuticalBillItem.setBillItem(currentBillItem);
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    private boolean errorCheckForEdit() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        return false;
    }

    public void editBillItem(BillItem billItem) {
        if (errorCheckForEdit()) {
            return;
        }

        getBillItemFacede().edit(billItem);
        getPharmaceuticalBillItemFacade().edit(billItem.getPharmaceuticalBillItem());

        calTotalAndUpdate(billItem.getBill());
    }

    public void editBillItem2(BillItem billItem) {
        getBillItemFacede().edit(billItem);
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - billItem.getQty()));
        getPharmaceuticalBillItemFacade().edit(billItem.getPharmaceuticalBillItem());

        calTotalAndUpdate2(billItem.getBill());
    }

    private void calTotalAndUpdate2(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            double tmp2 = (b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate());
            tmp += tmp2;
        }

        bill.setTotal(tmp);
        bill.setNetTotal(tmp);
        getBillFacade().edit(bill);
    }

    private void calTotalAndUpdate(Bill bill) {
        double tmp = 0;
        for (BillItem b : bill.getBillItems()) {
            if (b.getPharmaceuticalBillItem() == null) {
                continue;
            }
            double tmp2 = (b.getPharmaceuticalBillItem().getQtyInUnit() * b.getPharmaceuticalBillItem().getPurchaseRateInUnit());
            tmp += tmp2;
        }

        bill.setTotal(0 - tmp);
        bill.setNetTotal(0 - tmp);
        getBillFacade().edit(bill);
    }

    public WebUser getUser() {
        return user;
    }

    public void onEdit(RowEditEvent event) {

        BillFee tmp = (BillFee) event.getObject();

        if (tmp.getPaidValue() != 0.0) {
            JsfUtil.addErrorMessage("Already Staff FeePaid");
            return;
        }

        getBillFeeFacade().edit(tmp);

    }

    public void setUser(WebUser user) {
        // recreateModel();
        this.user = user;
        recreateModel();
    }

    private LazyDataModel<Bill> lazyBills;

   
    public LazyDataModel<Bill> getSearchSaleBills() {
        return lazyBills;
    }

  
    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public void setEjbApplication(EjbApplication ejbApplication) {
        this.ejbApplication = ejbApplication;
    }

    public boolean calculateRefundTotal() {
        Double d = 0.0;
        //billItems=null;
        tempbillItems = null;
        for (BillItem i : getRefundingItems()) {
            if (checkPaidIndividual(i)) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Refund Bill");
                return false;
            }

            if (i.isRefunded() == null) {
                d = d + i.getNetValue();
                getTempbillItems().add(i);
            }

        }
        refundAmount = d;
        return true;
    }

    public List<Bill> getUserBillsOwn() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
            //////System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);
            //////System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getBillsOwn() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<BillItem> getRefundingItems() {
        return refundingItems;
    }

    public void setRefundingItems(List<BillItem> refundingItems) {
        this.refundingItems = refundingItems;
    }

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

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String refundBill() {
        if (refundingItems.isEmpty()) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";

        }
        if (refundAmount == 0.0) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";
        }
        if (getBill().getComments() == null || getBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
                return "";
            }
            if (!calculateRefundTotal()) {
                return "";
            }

            RefundBill rb = (RefundBill) createRefundBill();

            refundBillItems(rb);

            calculateRefundBillFees(rb);

            getBill().setRefunded(true);
            getBill().setRefundedBill(rb);
            getBillFacade().edit(getBill());

            printPreview = true;
            //JsfUtil.addSuccessMessage("Refunded");

        } else {
            JsfUtil.addErrorMessage("No Bill to refund");
            return "";
        }
        //  recreateModel();
        return "";
    }

    private Bill createRefundBill() {
        RefundBill rb = new RefundBill();
        rb.setBilledBill(getBill());
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setBillType(getBill().getBillType());
        rb.setBilledBill(getBill());
        rb.setCatId(getBill().getCatId());
        rb.setCollectingCentre(getBill().getCollectingCentre());
        rb.setCreatedAt(bd);
        rb.setComments(getBill().getComments());
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setCreditCompany(getBill().getCreditCompany());
        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        rb.setDiscount(0.00);
        rb.setDiscountPercent(0.0);

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), rb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RF));
        rb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), rb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RF));

        rb.setToDepartment(getBill().getToDepartment());
        rb.setToInstitution(getBill().getToInstitution());

        rb.setFromDepartment(getBill().getFromDepartment());
        rb.setFromInstitution(getBill().getFromInstitution());

        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDepartment(getSessionController().getDepartment());

        rb.setNetTotal(refundAmount);
        rb.setPatient(getBill().getPatient());
        rb.setPatientEncounter(getBill().getPatientEncounter());
        rb.setPaymentScheme(paymentScheme);
        rb.setPaymentMethod(paymentMethod);
        rb.setReferredBy(getBill().getReferredBy());
        rb.setTotal(0 - refundAmount);
        rb.setNetTotal(0 - refundAmount);

        getBillFacade().create(rb);

        return rb;

    }

    public String returnBill() {
        if (refundingItems.isEmpty()) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";

        }
        if (refundAmount == 0.0) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";
        }
        if (getBill().getComments() == null || getBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
                return "";
            }
            if (!calculateRefundTotal()) {
                return "";
            }

            RefundBill rb = (RefundBill) createReturnBill();

            refundBillItems(rb);

            getBill().setRefunded(true);
            getBill().setRefundedBill(rb);
            getBillFacade().edit(getBill());

            printPreview = true;
            //JsfUtil.addSuccessMessage("Refunded");

        } else {
            JsfUtil.addErrorMessage("No Bill to refund");
            return "";
        }
        //  recreateModel();
        return "";
    }

    private Bill createReturnBill() {
        RefundBill rb = new RefundBill();
        rb.setBilledBill(getBill());
        Date bd = Calendar.getInstance().getTime();
        rb.setBillType(getBill().getBillType());
        rb.setBilledBill(getBill());
        rb.setCreatedAt(bd);
        rb.setComments(getBill().getComments());
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setCreditCompany(getBill().getCreditCompany());
        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));
        rb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));

        rb.setToDepartment(getBill().getToDepartment());
        rb.setToInstitution(getBill().getToInstitution());

        rb.setFromDepartment(getBill().getFromDepartment());
        rb.setFromInstitution(getBill().getFromInstitution());

        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDepartment(getSessionController().getDepartment());

        rb.setNetTotal(refundAmount);
        rb.setPatient(getBill().getPatient());
        rb.setPatientEncounter(getBill().getPatientEncounter());
        rb.setPaymentScheme(paymentScheme);
        rb.setPaymentMethod(paymentMethod);
        rb.setReferredBy(getBill().getReferredBy());
        rb.setTotal(0 - refundAmount);
        rb.setNetTotal(0 - refundAmount);

        getBillFacade().create(rb);

        return rb;

    }

    public void calculateRefundBillFees(RefundBill rb) {
        double s = 0.0;
        double b = 0.0;
        double p = 0.0;
        for (BillItem bi : refundingItems) {
            String sql = "select c from BillFee c where c.billItem.id = " + bi.getId();
            List<BillFee> rbf = getBillFeeFacade().findByJpql(sql);
            for (BillFee bf : rbf) {

                if (bf.getFee().getStaff() == null) {
                    p = p + bf.getFeeValue();
                } else {
                    s = s + bf.getFeeValue();
                }
            }

        }
        rb.setStaffFee(0 - s);
        rb.setPerformInstitutionFee(0 - p);
        getBillFacade().edit(rb);
    }

    public void refundBillItems(RefundBill rb) {
        for (BillItem bi : refundingItems) {
            //set Bill Item as Refunded

            BillItem rbi = new BillItem();
            rbi.setBill(rb);
            rbi.setCreatedAt(Calendar.getInstance().getTime());
            rbi.setCreater(getSessionController().getLoggedUser());
            rbi.setItem(bi.getItem());
            rbi.setNetValue(0 - bi.getNetValue());
            rbi.setRefunded(Boolean.TRUE);
            rbi.setReferanceBillItem(bi);
            getBillItemFacede().create(rbi);

            bi.setRefunded(Boolean.TRUE);
            getBillItemFacede().edit(bi);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);

            returnBillFee(rb, rbi, tmp);

        }
    }

    public void recreateModel() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
//        billFees
        lazyBills = null;
        billComponents = null;
        billForRefund = null;
        filteredBill = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        //  comment = null;
    }

    private void cancelBillComponents(Bill can, BillItem bt) {
        for (BillComponent nB : getBillComponents()) {
            BillComponent bC = new BillComponent();
            bC.setCatId(nB.getCatId());
            bC.setDeptId(nB.getDeptId());
            bC.setInsId(nB.getInsId());
            bC.setDepartment(nB.getDepartment());
            bC.setDeptId(nB.getDeptId());
            bC.setInstitution(nB.getInstitution());
            bC.setItem(nB.getItem());
            bC.setName(nB.getName());
            bC.setPackege(nB.getPackege());
            bC.setSpeciality(nB.getSpeciality());
            bC.setStaff(nB.getStaff());

            bC.setBill(can);
            bC.setBillItem(bt);
            bC.setCreatedAt(new Date());
            bC.setCreater(getSessionController().getLoggedUser());
            getBillCommponentFacade().create(bC);
        }

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

    private boolean checkPaidIndividual(BillItem bi) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
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

        if (getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

        if (checkCollectedReported()) {
            JsfUtil.addErrorMessage("Sample Already collected can't cancel or report already issued");
            return true;
        }

        if (getBill().getBillType() != BillType.LabBill && getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment scheme.");
            return true;
        }
        if (getBill().getComments() == null || getBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    private boolean pharmacyErrorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().getBillType() == BillType.StoreOrderApprove) {
            if (checkGrn()) {
                JsfUtil.addErrorMessage("Grn already head been Come u can't bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.StoreGrnBill) {
            if (checkGrnReturn()) {
                JsfUtil.addErrorMessage("Grn had been Returned u can't cancell bill ");
                return true;
            }
        }

        if (getBill().getBillType() == BillType.StoreTransferIssue) {
            if (getBill().checkActiveForwardReference()) {
                JsfUtil.addErrorMessage("Item for this bill already recieve");
                return true;
            }
        }

        if (getBill().getComments() == null || getBill().getComments().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    private boolean checkGrn() {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref and b.referenceBill.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("ref", getBill());
        hm.put("btp", BillType.StoreGrnBill);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean checkGrnReturn() {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref and b.referenceBill.cancelled=false";
        HashMap hm = new HashMap();
        hm.put("ref", getBill());
        hm.put("btp", BillType.StoreGrnReturn);
        List<Bill> tmp = getBillFacade().findByJpql(sql, hm);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    private CancelledBill pharmacyCreateCancelBill() {
        CancelledBill cb = new CancelledBill();

        cb.setBilledBill(getBill());
        cb.copy(getBill());
        cb.setReferenceBill(getBill().getReferenceBill());
        cb.invertValue(getBill());

        cb.setPaymentScheme(getBill().getPaymentScheme());
        cb.setPaymentMethod(getBill().getPaymentMethod());
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setComments(getBill().getComments());

        return cb;
    }

    private void pharmacyCancelBillItemsAddStock(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.StoreGrnBill || can.getBillType() == BillType.StoreGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacede().create(b);

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getStoreBean().addToStock(ph.getStock(),
                    Math.abs(qty),
                    ph, getSessionController().getDepartment());

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    public void unitCancell() {

        Bill prebill = getStoreBean().reAddToStock(getBill(), getSessionController().getLoggedUser(),
                getSessionController().getDepartment(), BillNumberSuffix.ISSCAN);

        if (prebill != null) {
            getBill().setCancelled(true);
            getBill().setCancelledBill(prebill);
            getBillFacade().edit(getBill());

            printPreview = true;
        }
    }

    private void pharmacyCancelBillItemsReduceStock(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.StoreGrnBill || can.getBillType() == BillType.StoreGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacede().create(b);

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getStoreBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }
        
        for (BillItem i : can.getBilledBill().getBillExpenses()) {
            BillItem b = new BillItem();
            b.copy(i);
            b.invertValue(i);
            b.setExpenseBill(can);
            getBillItemFacede().create(b);
            can.getBillExpenses().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelIssuedItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            b.setReferanceBillItem(nB);

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            try {
                getBillItemFacede().create(b);
            } catch (Exception e) {
            }

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            boolean returnFlag = getStoreBean().deductFromStockWithoutHistory(ph.getStaffStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (returnFlag) {
                getStoreBean().addToStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());
            } else {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReceivedItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            b.setReferanceBillItem(nB);

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacede().create(b);

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //    updateRemainingQty(nB);
            //  b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            double qty = ph.getFreeQtyInUnit() + ph.getQtyInUnit();
            //System.err.println("Updating QTY " + qty);
            getStoreBean().addToStockWithoutHistory(ph.getStaffStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            boolean returnFlag = getStoreBean().deductFromStock(ph.getStock(), Math.abs(qty), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelBillItems(CancelledBill can) {
        for (BillItem nB : getBill().getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            if (can.getBillType() == BillType.StoreGrnBill || can.getBillType() == BillType.StoreGrnReturn) {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            } else {
                b.setReferanceBillItem(nB);
            }

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB.getPharmaceuticalBillItem());
            ph.invertValue(nB.getPharmaceuticalBillItem());

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacede().create(b);

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReturnBillItems(Bill can) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());

            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacede().create(b);

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

//            //System.err.println("Updating QTY " + ph.getQtyInUnit());
//            getStoreBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());
//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

    private void pharmacyCancelReturnBillItemsWithReducingStock(Bill can) {
        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB.getBillItem());
            b.invertValue(nB.getBillItem());

            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(nB);
            ph.invertValue(nB);

            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacede().create(b);

            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().edit(ph);

            //System.err.println("Updating QTY " + ph.getQtyInUnit());
            boolean returnFlag = getStoreBean().deductFromStock(ph.getStock(), Math.abs(ph.getQtyInUnit()), ph, getSessionController().getDepartment());

            if (!returnFlag) {
                b.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(b.getPharmaceuticalBillItem());
            }

//
//            //    updateRemainingQty(nB);
            // if (b.getReferanceBillItem() != null) {
            //    b.setPharmaceuticalBillItem(b.getReferanceBillItem().getPharmaceuticalBillItem());
            //  }
            getBillItemFacede().edit(b);

            can.getBillItems().add(b);
        }

        getBillFacade().edit(can);
    }

//    private void pharmacyCancelReturnBillItems(Bill can) {
//        for (PharmaceuticalBillItem nB : getPharmacyBillItems()) {
//            BillItem b = new BillItem();
//            b.setBill(can);
//            b.copy(nB.getBillItem());
//            b.invertValue(nB.getBillItem());
//
//            b.setReferanceBillItem(nB.getBillItem().getReferanceBillItem());
//            b.setCreatedAt(new Date());
//            b.setCreater(getSessionController().getLoggedUser());
//
//            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
//            ph.copy(nB);
//            ph.invertValue(nB);
//
//            getPharmaceuticalBillItemFacade().create(ph);
//
//            b.setPharmaceuticalBillItem(ph);
//            getBillItemFacede().create(b);
//
//            ph.setBillItem(b);
//            getPharmaceuticalBillItemFacade().edit(ph);
//   
//            getBillItemFacede().edit(b);
//
//            can.getBillItems().add(b);
//        }
//
//        getBillFacade().edit(can);
//    }
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    CashTransactionBean cashTransactionBean;

    public LazyDataModel<Bill> getLazyBills() {
        return lazyBills;
    }

    public void setLazyBills(LazyDataModel<Bill> lazyBills) {
        this.lazyBills = lazyBills;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void pharmacyRetailCancelBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));

            getBillFacade().create(cb);

            pharmacyCancelBillItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            if (getBill().getReferenceBill() != null) {
                getBill().getReferenceBill().setReferenceBill(null);
                getBillFacade().edit(getBill().getReferenceBill());
            }

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private boolean checkDepartment(Bill bill) {
        if (bill == null) {
            return true;

        }

        if (bill.getDepartment() == null) {
            return true;
        }

        if (bill.getDepartment().getId() != getSessionController().getDepartment().getId()) {
            JsfUtil.addErrorMessage("Billed Department Is Defferent than Logged Department");
            return true;
        }

        return false;
    }

    public void pharmacyRetailCancelBillWithStock() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().getReferenceBill() == null) {
                return;
            }

            if (getBill().getReferenceBill().getBillType() != BillType.StorePre) {
                return;
            }

            if (checkDepartment(getBill().getReferenceBill())) {
                return;
            }

            getStoreBean().reAddToStock(getBill().getReferenceBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), BillNumberSuffix.PRECAN);

            CancelledBill cb = pharmacyCreateCancelBill();

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.SALCAN));

            getBillFacade().create(cb);

            pharmacyCancelBillItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

//            if (getBill().getReferenceBill() != null) {
//                getBill().getReferenceBill().setReferenceBill(null);
//                getBillFacade().edit(getBill().getReferenceBill());
//            }
            getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());

            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyRetailCancelBillWithStockBht() {
        if (getBill().getBillType() != BillType.StoreBhtPre) {
            return;
        }

        CancelBillWithStockBht(BillNumberSuffix.PHISSCAN);
    }

    public void storeRetailCancelBillWithStockBht() {
        if (getBill().getBillType() != BillType.StoreBhtPre) {
            ////System.out.println("Bill Type incorrect");
            return;
        }

        CancelBillWithStockBht(BillNumberSuffix.STTISSUECAN);
    }

    public void storeRetailCancelBillWithStockBhtIssue() {
        ////System.out.println("In");
        ///////bht cancel BillType.StoreIssue to BillType.StoreBhtPre
        if (getBill().getBillType() != BillType.StoreBhtPre) {
            ////System.out.println("Bill Type incorrect");
            return;
        }

        CancelBillWithStockBht(BillNumberSuffix.STTISSUECAN);
    }

    public void cancelPreBillFees(List<BillItem> list) {
        for (BillItem b : list) {
            List<BillFee> bfs = getBillFees(b.getReferanceBillItem());
            for (BillFee bf : bfs) {
                BillFee nBillFee = new BillFee();
                nBillFee.copy(bf);
                nBillFee.invertValue(bf);
                nBillFee.setBill(b.getBill());
                nBillFee.setBillItem(b);
                nBillFee.setCreatedAt(new Date());
                nBillFee.setCreater(getSessionController().getLoggedUser());
                getBillFeeFacade().create(nBillFee);
            }
        }
    }

    private void CancelBillWithStockBht(BillNumberSuffix billNumberSuffix) {
        ////System.out.println("In CancelBillWithStockBht");
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (getBill().checkActiveReturnBhtIssueBills()) {
                JsfUtil.addErrorMessage("There some return Bill for this please cancel that bills first");
                return;
            }

            if (checkDepartment(getBill())) {
                return;
            }

            Bill cb = getStoreBean().reAddToStock(getBill(), getSessionController().getLoggedUser(), getSessionController().getDepartment(), billNumberSuffix);
            cb.setForwardReferenceBill(getBill().getForwardReferenceBill());
            getBillFacade().edit(cb);

            cancelPreBillFees(cb.getBillItems());

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            //   //System.out.print(getBill().isCancelled());

//            if (getBill().getReferenceBill() != null) {
//                getBill().getReferenceBill().setReferenceBill(null);
//                getBillFacade().edit(getBill().getReferenceBill());
//            }
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPoCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.POCAN));

            getBillFacade().create(cb);
            pharmacyCancelBillItems(cb);

            getBill().getReferenceBill().setReferenceBill(null);
            getBillFacade().edit(getBill().getReferenceBill());

            getBill().setReferenceBill(null);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            //       //System.err.println("Bill : "+getBill().getBillType());
//            //System.err.println("Reference Bill : "+getBill().getReferenceBill().getBillType());
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPoRequestCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().getReferenceBill() != null && !getBill().getReferenceBill().isCancelled()) {
                JsfUtil.addErrorMessage("Sorry You cant Cancell Approved Bill");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PORCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PORCAN));

            getBillFacade().create(cb);
            pharmacyCancelBillItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            //       //System.err.println("Bill : "+getBill().getBillType());
//            //System.err.println("Reference Bill : "+getBill().getReferenceBill().getBillType());
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private boolean checkStock(PharmaceuticalBillItem pharmaceuticalBillItem) {
        //System.err.println("Batch " + pharmaceuticalBillItem.getItemBatch());
        double stockQty = getStoreBean().getStockQty(pharmaceuticalBillItem.getItemBatch(), getBill().getDepartment());
        //System.err.println("Stock Qty" + stockQty);
        //System.err.println("Ph Qty" + pharmaceuticalBillItem.getQtyInUnit());
        if (Math.abs(pharmaceuticalBillItem.getQtyInUnit()) > stockQty) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkBillItemStock() {
        //System.err.println("Checking Item Stock");
        for (BillItem bi : getBill().getBillItems()) {
            if (checkStock(bi.getPharmaceuticalBillItem())) {
                return true;
            }
        }

        return false;
    }

    public void markAsChecked() {
        if (bill == null) {
            return;
        }

        if (bill.getPatientEncounter() == null) {
            return;
        }

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            return;
        }

        bill.setCheckeAt(new Date());
        bill.setCheckedBy(getSessionController().getLoggedUser());

        getBillFacade().edit(bill);
    }

    public void pharmacyReturnBhtCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (checkDepartment(getBill())) {
                return;
            }

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            RefundBill cb = pharmacyCreateRefundCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.RefundBill, BillNumberSuffix.RETCAN));

            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }

            pharmacyCancelReturnBillItemsWithReducingStock(cb);

            // cancelPreBillFees(cb.getBillItems());
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private RefundBill pharmacyCreateRefundCancelBill() {
        RefundBill cb = new RefundBill();
        cb.invertQty();
        cb.copy(getBill());
        cb.invertValue(getBill());
        cb.setRefundedBill(getBill());
        cb.setReferenceBill(getBill().getReferenceBill());
        cb.setForwardReferenceBill(getBill().getForwardReferenceBill());

        cb.setPaymentMethod(paymentMethod);
        cb.setPaymentScheme(getBill().getPaymentScheme());
        cb.setBalance(0.0);
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setComments(getBill().getComments());
        cb.setPaymentMethod(getPaymentMethod());

        return cb;
    }

    public void pharmacyGrnCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("ITems for this GRN Already issued so you can't cancel ");
                return;
            }

            if (getBill().getPaidAmount() != 0) {
                JsfUtil.addErrorMessage("Payments for this GRN Already Given ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNCAN));

            getBillFacade().create(cb);

            pharmacyCancelBillItemsReduceStock(cb);
//
//            List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//
//            for (PharmaceuticalBillItem ph : tmp) {
//                double qty = ph.getQtyInUnit() + ph.getFreeQtyInUnit();
//                getStoreBean().deductFromStock(ph.getStock(), qty);
//
//                getStoreBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getStoreBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyTransferIssueCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTICAN));

            getBillFacade().create(cb);

            pharmacyCancelIssuedItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

//            getBill().getBackwardReferenceBill().setForwardReferenceBill(null);
//            getBillFacade().edit(getBill().getBackwardReferenceBill());
//
//            getBill().setBackwardReferenceBill(null);
//            getBill().setForwardReferenceBill(null);
//            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyTransferReceiveCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("Items for this Note Already issued so you can't cancel ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTRCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PHTRCAN));

            getBillFacade().create(cb);

            pharmacyCancelReceivedItems(cb);

            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void pharmacyPurchaseCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            if (checkBillItemStock()) {
                JsfUtil.addErrorMessage("ITems for this GRN Already issued so you can't cancel ");
                return;
            }

            if (getBill().getPaidAmount() != 0) {
                JsfUtil.addErrorMessage("Payments for this GRN Already Given ");
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PURCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PURCAN));

            getBillFacade().create(cb);

            pharmacyCancelBillItemsReduceStock(cb);

//            //   List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//            for (BillItem bi : getBill().getBillItems()) {
//                double qty = bi.getPharmaceuticalBillItem().getQtyInUnit() + bi.getPharmaceuticalBillItem().getFreeQtyInUnit();
//                getStoreBean().deductFromStock(bi.getPharmaceuticalBillItem().getStock(), qty);
//
//                getStoreBean().reSetPurchaseRate(bi.getPharmaceuticalBillItem().getItemBatch(), getBill().getDepartment());
//                getStoreBean().reSetRetailRate(bi.getPharmaceuticalBillItem().getItemBatch(), getSessionController().getDepartment());
//            }
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

    public void pharmacyGrnReturnCancel() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (pharmacyErrorCheck()) {
                return;
            }

            CancelledBill cb = pharmacyCreateCancelBill();
            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNRETCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.GRNRETCAN));

            getBillFacade().create(cb);

            pharmacyCancelBillItemsAddStock(cb);

            //        List<PharmaceuticalBillItem> tmp = getPharmaceuticalBillItemFacade().findByJpql("Select p from PharmaceuticalBillItem p where p.billItem.bill.id=" + getBill().getId());
//            for (PharmaceuticalBillItem ph : tmp) {
//                getStoreBean().addToStock(ph.getStock(), ph.getQtyInUnit());
//
//                getStoreBean().reSetPurchaseRate(ph.getItemBatch(), getBill().getDepartment());
//                getStoreBean().reSetRetailRate(ph.getItemBatch(), getSessionController().getDepartment());
//            }
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    private void returnBillFee(Bill b, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.setFee(nB.getFee());
            bf.setPatienEncounter(nB.getPatienEncounter());
            bf.setPatient(nB.getPatient());
            bf.setDepartment(nB.getDepartment());
            bf.setInstitution(nB.getInstitution());
            bf.setSpeciality(nB.getSpeciality());
            bf.setStaff(nB.getStaff());

            bf.setBill(b);
            bf.setBillItem(bt);
            bf.setFeeValue(0 - nB.getFeeValue());
            bf.setFeeGrossValue(0 - nB.getFeeGrossValue());
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

    @Inject
    private BillBeanController billBean;

    boolean showAllBills;

    public boolean isShowAllBills() {
        return showAllBills;
    }

    public void setShowAllBills(boolean showAllBills) {
        this.showAllBills = showAllBills;
    }

    public void allBills() {
        showAllBills = true;
    }

    public List<Bill> getBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getColBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.LabBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.LabBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getPos() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.StoreOrder);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.StoreOrder);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getSales() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.StoreSale);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.StoreSale);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getPreBills() {
        if (bills == null) {
//            if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay2(getFromDate(), getToDate(), BillType.StorePre);
//            } else {
//                bills = getBillBean().billsFromSearch2(txtSearch, getFromDate(), getToDate(), BillType.PharmacySale);
//            }
//            if (bills == null) {
//                bills = new ArrayList<>();
//            }
        }
        return bills;
    }

  
    public void updatePhIem() {
        if (currentBillItem == null) {
            return;
        }

        if (currentBillItem.getPharmaceuticalBillItem() == null) {
            return;
        }

        pharmaceuticalBillItemFacade.edit(currentBillItem.getPharmaceuticalBillItem());
        //Update Successfull

    }

    public void addDetailItemListener(BillItem bi) {

        currentBillItem = null;
        currentBillItem = bi;
        currentBillItem.setPharmaceuticalBillItem(bi.getPharmaceuticalBillItem());

    }

    public void makeNull() {
        refundAmount = 0;
        txtSearch = null;
        bill = null;
        paymentMethod = null;
        paymentScheme = null;
        billForRefund = null;
        fromDate = null;
        toDate = null;
        user = null;
        ////////////////
        refundingItems = null;
        bills = null;
        filteredBill = null;
        selectedBills = null;
        billEntrys = null;
        billItems = null;
        billComponents = null;
        billFees = null;
        tempbillItems = null;
        searchRetaiBills = null;
        lazyBills = null;

    }
    
    public String viewBill() {

        if (bill != null) {
            switch (bill.getBillType()) {
//                case PharmacyPre:
//                    return "store_reprint_bill_sale";
//                case PharmacyBhtPre:
//                    return "store_reprint_bill_sale";
                case StoreIssue:
                    return "store_reprint_bill_unit_issue";
                case StoreTransferIssue:
                    return "store_reprint_transfer_isssue";
                case StoreTransferReceive:
                    return "store_reprint_transfer_receive";
                case StorePurchase:
                    return "store_reprint_purchase";
                case StoreGrnBill:
                    return "store_reprint_grn";
                case StoreGrnReturn:
                    return "store_reprint_grn_return";
                case StorePurchaseReturn:
                    return "store_reprint_purchase_return";
                case StoreAdjustment:
                    return "store_reprint_adjustment";
//                case PharmacyWholesalePre:
//                    return "store_reprint_bill_sale";
                default:
                    return "store_reprint_bill_sale";
            }
        } else {

            return "";
        }

    }

    public List<Bill> getPreRefundBills() {
        if (bills == null) {
            //   List<Bill> lstBills;
            String sql;
            Map temMap = new HashMap();
            sql = "select b from RefundBill b where b.billType = :billType"
                    + " and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc ";

            temMap.put("billType", BillType.StorePre);
            // temMap.put("refBillType", BillType.PharmacySale);
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);

            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
            //   bills = getBillBean().billsRefundForTheDay(getFromDate(), getToDate(), BillType.PharmacyPre);

        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.StoreTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.StoreTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.StoreGrnBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.StoreGrnBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getInstitutionPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                if (txtSearch == null || txtSearch.trim().equals("")) {
                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";

                } else {
                    sql = "select b from BilledBill b where b.retired=false and"
                            + " b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) "
                            + "and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
                }

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public List<Bill> getChannelPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                        + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType=:btp"
                        + " or bt.referenceBill.billType=:btp2) and b.billType=:type and b.createdAt "
                        + "between :fromDate and :toDate order by b.id";

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        //////System.out.println("getting user bills");
        if (getUser() == null) {
            userBills = new ArrayList<>();
            //////System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
            //////System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
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

    public Bill getBill() {
        //recreateModel();
        return bill;
    }

    public void setBill(Bill bill) {
        recreateModel();
        this.bill = bill;
        //System.err.println("Setting BIll " + bill);
        //////System.out.println("bill.getBillItems() = " + bill.getBillItems());
        bill = getBillFacade().find(bill.getId());
        //////System.out.println("bill.getBillItems() = " + bill.getBillItems());
        double tmp = 0;
        if (bill.getBillType() == BillType.StoreTransferIssue) {
            for (BillItem b : bill.getBillItems()) {
                tmp += (b.getPharmaceuticalBillItem().getStock().getItemBatch().getPurcahseRate() * b.getPharmaceuticalBillItem().getQtyInUnit());
            }
            //System.err.println("Inside" + tmp);
            if (bill.getNetTotal() == 0) {
                bill.setNetTotal(tmp);
                getBillFacade().edit(bill);
            }
        }

        createBillItems();

    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void createBillItems() {
        HashMap hm = new HashMap();
        String sql = "SELECT b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill=:b";
        hm.put("b", getBill());
        billItems = billItemFacede.findByJpqlWithoutCache(sql, hm);

    }

    public List<PharmaceuticalBillItem> getPharmacyBillItems() {
        List<PharmaceuticalBillItem> tmp = new ArrayList<>();
        if (getBill() != null) {
            String sql = "SELECT b FROM PharmaceuticalBillItem b WHERE b.billItem.retired=false and b.billItem.bill.id=" + getBill().getId();
            tmp = getPharmaceuticalBillItemFacade().findByJpql(sql);
        }

        return tmp;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null || billForRefund == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getBillFees(BillItem bi) {

        String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.billItem=:b ";
        HashMap hm = new HashMap();
        hm.put("b", bi);
        List<BillFee> ls = getBillFeeFacade().findByJpql(sql, hm);

        if (ls == null) {
            ls = new ArrayList<>();
        }

        return ls;
    }

    public List<BillFee> getBillFees2() {
        if (getBill() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findByJpql(sql);
            if (billFees == null) {
                billFees = new ArrayList<>();
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
    public StoreBillSearch() {
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

    private void setRefundAttribute() {
        billForRefund.setBalance(getBill().getBalance());

        billForRefund.setBillDate(Calendar.getInstance().getTime());
        billForRefund.setBillTime(Calendar.getInstance().getTime());
        billForRefund.setCreater(getSessionController().getLoggedUser());
        billForRefund.setCreatedAt(Calendar.getInstance().getTime());

        billForRefund.setBillType(getBill().getBillType());
        billForRefund.setBilledBill(getBill());

        billForRefund.setCatId(getBill().getCatId());
        billForRefund.setCollectingCentre(getBill().getCollectingCentre());
        billForRefund.setCreditCardRefNo(getBill().getCreditCardRefNo());
        billForRefund.setCreditCompany(getBill().getCreditCompany());

        billForRefund.setDepartment(getBill().getDepartment());
        billForRefund.setDeptId(getBill().getDeptId());
        billForRefund.setDiscount(getBill().getDiscount());

        billForRefund.setDiscountPercent(getBill().getDiscountPercent());
        billForRefund.setFromDepartment(getBill().getFromDepartment());
        billForRefund.setFromInstitution(getBill().getFromInstitution());
        billForRefund.setFromStaff(getBill().getFromStaff());

        billForRefund.setInsId(getBill().getInsId());
        billForRefund.setInstitution(getBill().getInstitution());

        billForRefund.setPatient(getBill().getPatient());
        billForRefund.setPatientEncounter(getBill().getPatientEncounter());
        billForRefund.setPaymentScheme(getBill().getPaymentScheme());
        billForRefund.setPaymentMethod(paymentMethod);
        billForRefund.setPaymentSchemeInstitution(getBill().getPaymentSchemeInstitution());

        billForRefund.setReferredBy(getBill().getReferredBy());
        billForRefund.setReferringDepartment(getBill().getReferringDepartment());

        billForRefund.setStaff(getBill().getStaff());

        billForRefund.setToDepartment(getBill().getToDepartment());
        billForRefund.setToInstitution(getBill().getToInstitution());
        billForRefund.setToStaff(getBill().getToStaff());
        billForRefund.setTotal(calTot());
        //Need To Add Net Total Logic
        billForRefund.setNetTotal(billForRefund.getTotal());
    }

    public double calTot() {
        if (getBillFees() == null) {
            return 0.0f;
        }
        double tot = 0.0f;
        for (BillFee f : getBillFees()) {
            //////System.out.println("Tot" + f.getFeeValue());
            tot += f.getFeeValue();
        }
        getBillForRefund().setTotal(tot);
        return tot;
    }

    public RefundBill getBillForRefund() {

        if (billForRefund == null) {
            billForRefund = new RefundBill();
            setRefundAttribute();
        }

        return billForRefund;
    }

    public void setBillForRefund(RefundBill billForRefund) {
        this.billForRefund = billForRefund;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<BillItem> getTempbillItems() {
        if (tempbillItems == null) {
            tempbillItems = new ArrayList<>();
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
        resetLists();
        this.toDate = toDate;

    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        resetLists();
        this.fromDate = fromDate;

    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public StoreBean getStoreBean() {
        return StoreBean;
    }

    public void setStoreBean(StoreBean StoreBean) {
        this.StoreBean = StoreBean;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public List<Bill> getFilteredBill() {
        return filteredBill;
    }

    public void setFilteredBill(List<Bill> filteredBill) {
        this.filteredBill = filteredBill;
    }

    private boolean checkCollectedReported() {
        return false;
    }

    public List<Bill> getSearchRetaiBills() {
        return searchRetaiBills;
    }

    public void setSearchRetaiBills(List<Bill> searchRetaiBills) {
        this.searchRetaiBills = searchRetaiBills;
    }

    public List<Bill> getSelectedBills() {
        if (selectedBills == null) {
            selectedBills = new ArrayList<>();
        }
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

}

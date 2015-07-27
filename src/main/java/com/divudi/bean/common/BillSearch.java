/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.EjbApplication;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.LazyBill;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.WebUserFacade;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
public class BillSearch implements Serializable {

    private boolean printPreview = false;
    private double refundAmount;
    String txtSearch;
    Bill bill;
    Bill printingBill;
    PaymentMethod paymentMethod;
    private RefundBill billForRefund;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    private String comment;
    WebUser user;
    private BillType billType;
    ////////////////
    List<BillItem> refundingItems;
    List<Bill> bills;
    private List<Bill> filteredBill;
    List<BillEntry> billEntrys;
    List<BillItem> billItems;//////////
    List<BillComponent> billComponents;
    List<BillFee> billFees;
    private List<BillItem> tempbillItems;
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
    @EJB
    PaymentFacade paymentFacade;
    ////////////////////
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    EjbApplication ejbApplication;
    ///////////////////
    @Inject
    SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    OpdPreSettleController opdPreSettleController;
    private SearchKeyword searchKeyword;
    Institution creditCompany;
    PatientInvestigation patientInvestigation;

    public BillSearch() {
    }

    private LazyDataModel<BillItem> searchBillItems;
    @EJB
    private BillItemFacade billItemFacade;

    public void updateBill() {

        if (bill.getPatientEncounter() == null) {
            bill.setCreditCompany(creditCompany);
        } else {
            bill.getPatientEncounter().setCreditCompany(creditCompany);
        }
        bill.setEditedAt(new Date());
        bill.setEditor(sessionController.getLoggedUser());
        billFacade.edit(bill);
        UtilityController.addSuccessMessage("Bill Upadted");

    }

    private double roundOff(double d, int position) {
//        BigDecimal bd = new BigDecimal(x).setScale(position, RoundingMode.HALF_EVEN);
//        return bd.doubleValue();

        DecimalFormat newFormat = new DecimalFormat("#.##");
        return Double.valueOf(newFormat.format(d));

    }

    public void updateValue() {

        for (BillFee bf : billFeesList) {
            bf.setFeeGrossValue(roundOff(bf.getFeeGrossValue(), 2));
            bf.setFeeDiscount(roundOff(bf.getFeeDiscount(), 2));
            bf.setFeeValue(roundOff(bf.getFeeValue(), 2));
            billFeeFacade.edit(bf);
        }

        for (BillItem bt : billItemList) {
            String sql = "select sum(b.feeGrossValue)  "
                    + " from BillFee b "
                    + " where b.retired=false"
                    + " and b.billItem.id=" + bt.getId();
            bt.setGrossValue(billItemFacade.findDoubleByJpql(sql));

            sql = "select sum(b.feeDiscount)  "
                    + " from BillFee b "
                    + " where b.retired=false"
                    + " and b.billItem.id=" + bt.getId();

            bt.setDiscount(billItemFacade.findDoubleByJpql(sql));

            sql = "select sum(b.feeValue)  "
                    + " from BillFee b "
                    + " where b.retired=false"
                    + " and b.billItem.id=" + bt.getId();
            bt.setNetValue(billItemFacade.findDoubleByJpql(sql));
        }

        String sql = "select sum(b.grossValue)  "
                + " from BillItem b "
                + " where b.retired=false"
                + " and b.bill.id=" + bill.getId();
        bill.setTotal(billItemFacade.findDoubleByJpql(sql));

        sql = "select sum(b.discount)  "
                + " from BillItem b "
                + " where b.retired=false"
                + " and b.bill.id=" + bill.getId();

        bill.setDiscount(billItemFacade.findDoubleByJpql(sql));

        sql = "select sum(b.netValue)  "
                + " from BillItem b "
                + " where b.retired=false"
                + " and b.bill.id=" + bill.getId();
        bill.setNetTotal(billItemFacade.findDoubleByJpql(sql));
        billFacade.edit(bill);

        UtilityController.addSuccessMessage("Bill Upadted");

    }

//    public void updateBillRetierd() {
//
//        bill.setRetiredAt(new Date());
//        bill.setRetirer(sessionController.getLoggedUser());
//        bill.setEditedAt(new Date());
//        bill.setEditor(sessionController.getLoggedUser());
//        billFacade.edit(bill);
//        UtilityController.addSuccessMessage("Bill Retired");
//
//    }
    public void updateBillFeeRetierd(BillFee bf) {
        if (bf.isRetired()) {
            bf.setRetiredAt(new Date());
            bf.setRetirer(sessionController.getLoggedUser());
            getBillFeeFacade().edit(bf);
            UtilityController.addSuccessMessage("Bill Fee Retired");
        }
    }

    public void updateBillItemRetierd(BillItem bi) {

        if (bi.isRetired()) {
            bi.setRetiredAt(new Date());
            bi.setRetirer(sessionController.getLoggedUser());
            getBillItemFacade().edit(bi);
            UtilityController.addSuccessMessage("Bill Item Retired");
        }
    }

    public void updateBillfee(BillFee bf) {

        getBillFeeFacade().edit(bf);
        UtilityController.addSuccessMessage("Bill Item Retired");
    }

    private void createBillFees() {
        String sql = "SELECT b FROM BillFee b WHERE b.bill.id=" + getBillSearch().getId();
        billFeesList = getBillFeeFacade().findBySQL(sql);
    }

    private List<BillItem> billItemList;

    private void createBillItemsAll() {
        String sql = "SELECT b FROM BillItem b WHERE b.bill.id=" + getBillSearch().getId();
        billItemList = billItemFacade.findBySQL(sql);
    }

    public void createCashReturnBills() {
        bills = null;
        Map m = new HashMap();
        m.put("bt", BillType.PharmacySale);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false and b.institution=:ins and"
                + " b.createdAt between :fd and :td and b.billType=:bt ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.id desc  ";

        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void makeNull() {
        printPreview = false;
        refundAmount = 0;
        txtSearch = "";
        bill = null;
        paymentMethod = null;
        billForRefund = null;
        comment = "";
        user = null;
        refundingItems = null;
        bills = null;
        filteredBill = null;
        billEntrys = null;
        billItems = null;
        billComponents = null;
        billFees = null;
        tempbillItems = null;
        lazyBills = null;
        searchKeyword = null;
    }

    public void update() {
        getBillFacade().edit(getBill());
    }

    public WebUser getUser() {
        return user;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public void onEdit(RowEditEvent event) {

        BillFee tmp = (BillFee) event.getObject();

        tmp.setEditedAt(new Date());
        tmp.setEditor(sessionController.getLoggedUser());
       
        if (tmp.getPaidValue() != 0.0) {
            UtilityController.addErrorMessage("Already Staff FeePaid");
            return;
        }

        getBillFeeFacade().edit(tmp);

    }

    public void onEditItem(RowEditEvent event) {

        BillItem tmp = (BillItem) event.getObject();

        tmp.setEditedAt(new Date());
        tmp.setEditor(sessionController.getLoggedUser());

        //System.out.println("1.tmp = " + tmp.getPaidForBillFee().getPaidValue());
        getBillFeeFacade().edit(tmp.getPaidForBillFee());
        //System.out.println("2.tmp = " + tmp.getPaidForBillFee().getPaidValue());
//        if (tmp.getPaidValue() != 0.0) {
//            UtilityController.addErrorMessage("Already Staff FeePaid");
//            return;
//        }
        getBillItemFacade().edit(tmp);

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

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
    }
    
    

    private double refundTotal = 0;
    private double refundDiscount = 0;
    private double refundMargin = 0;

    public boolean calculateRefundTotal() {
        refundAmount = 0;
        refundDiscount = 0;
        refundTotal = 0;
        refundMargin = 0;
        //billItems=null;
        tempbillItems = null;
        for (BillItem i : getRefundingItems()) {
            if (checkPaidIndividual(i)) {
                UtilityController.addErrorMessage("Doctor Payment Already Paid So Cant Refund Bill");
                return false;
            }
            //Add for check refund is already done
            String sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + i.getId();
            BillItem rbi = getBillItemFacade().findFirstBySQL(sql);

            if (rbi != null) {
                System.err.println("rbi = " + rbi.getId());
                System.out.println("rbi.getBill().getInsId() = " + rbi.getBill().getInsId());
                System.out.println("rbi.getBill().getDeptId() = " + rbi.getBill().getDeptId());
                System.err.println("rbi = " + rbi.getId());
                UtilityController.addErrorMessage("This Bill Item Already Refunded");
                return false;
            }
            //

//            if (!i.isRefunded()) {
            refundTotal += i.getGrossValue();
            refundAmount += i.getNetValue();
            refundMargin += i.getMarginValue();
            refundDiscount += i.getDiscount();
            getTempbillItems().add(i);
//            }

        }

        return true;
    }

    public List<Bill> getUserBillsOwn() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
            ////System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);

            ////System.out.println("user ok");
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

    public void recreateModel2() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
//        billFees
        fromDate = null;
        toDate = null;
        billComponents = null;
        billForRefund = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
        lazyBills = null;
    }
//
//    public List<Bill> getSearchBills() {
//        if (bills == null) {
//            // List<Bill> lstBills;
//            String sql;
//            Map temMap = new HashMap();
//            sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins and b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc  ";
//
//            temMap.put("billType", BillType.OpdBill);
//            temMap.put("toDate", toDate);
//            temMap.put("fromDate", fromDate);
//            temMap.put("ins", getSessionController().getInstitution());
//            bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//        }
//        return bills;
//    }

    private LazyDataModel<Bill> lazyBills;

    public LazyDataModel<Bill> getSearchBills() {
        return lazyBills;
    }

    public void createTableByKeyword2() {
        lazyBills = null;
        String sql;
        Map temMap = new HashMap();

        if (txtSearch == null || txtSearch.trim().equals("")) {
            UtilityController.addErrorMessage("Please Enter Bill Id ,Billed Value,Patient Name or Phone Number & Search");
            return;
        }

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and  (upper(b.patient.person.name) like :str "
                + " or upper(b.patient.person.phone) like :str "
                + "  or upper(b.insId) like :str "
                + " or upper(b.netTotal) like :str "
                + " or upper(b.total) like :str )  and"
                + " b.createdAt between :fromDate and :toDate and b.retired=false "
                + " order by b.id desc  ";

        temMap.put("str", "%" + txtSearch.toUpperCase() + "%");

        temMap.put("billType", BillType.OpdBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        List<Bill> lst = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        //System.err.println("SIZE : " + lst.size());

        lazyBills = new LazyBill(lst);
    }

    public void createDealorPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :ins )";
            temMap.put("ins", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bank.name) like :bnk )";
            temMap.put("bnk", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.chequeRefNo) like :chck )";
            temMap.put("chck", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.id desc  ";

        temMap.put("billType", BillType.GrnPayment);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("dept", getSessionController().getInstitution());
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);
        //     //System.err.println("SIZE : " + lst.size());

    }

    public void makeKeywodNull() {
        searchKeyword = null;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String refundBill() {
        if (refundingItems.isEmpty()) {
            UtilityController.addErrorMessage("There is no item to Refund");
            return "";

        }
//        if (refundAmount == 0.0) {
//            UtilityController.addErrorMessage("There is no item to Refund");
//            return "";
//        }
        if (comment == null || comment.trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return "";
        }

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().isCancelled()) {
                UtilityController.addErrorMessage("Already Cancelled. Can not Refund again");
                return "";
            }

            if (getBill().getBillType() == BillType.InwardBill) {
                if (getBill().getCheckedBy() != null) {
                    UtilityController.addErrorMessage("Please Uncheck Bill");
                    return "";
                }
            }

            if (!calculateRefundTotal()) {
                return "";
            }

            if (!getWebUserController().hasPrivilege("LabBillRefundSpecial")) {
                for (BillItem trbi : refundingItems) {
                    if (patientInvestigationController.sampledForBillItem(trbi)) {
                        UtilityController.addErrorMessage("One or more bill Item has been already undersone process at the Lab. Can not return.");
                        return "";
                    }
                }
            }

            RefundBill rb = (RefundBill) createRefundBill();
            Payment p = getOpdPreSettleController().createPayment(rb, paymentMethod);
            refundBillItems(rb, p);
            System.out.println("getOpdPreSettleController().calBillPaidValue(rb) = " + getOpdPreSettleController().calBillPaidValue(rb));
            System.out.println("1p.getPaidValue() = " + p.getPaidValue());
            p.setPaidValue(getOpdPreSettleController().calBillPaidValue(rb));
            System.out.println("2p.getPaidValue() = " + p.getPaidValue());
            paymentFacade.edit(p);

            calculateRefundBillFees(rb);

            getBill().setRefunded(true);
            getBill().setRefundedBill(rb);
            getBillFacade().edit(getBill());

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(rb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            
            printingBill=getBillFacade().find(rb.getId());

            printPreview = true;
            //UtilityController.addSuccessMessage("Refunded");

        } else {
            UtilityController.addErrorMessage("No Bill to refund");
            return "";
        }
        //  recreateModel();
        return "";
    }

    private Bill createRefundBill() {
        RefundBill rb = new RefundBill();
        rb.copy(getBill());
        rb.invertValue(getBill());

        rb.setBilledBill(getBill());
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

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));
        rb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));

        rb.setTotal(0 - refundTotal);
        rb.setDiscount(0 - refundDiscount);
        rb.setNetTotal(0 - refundAmount);

        getBillFacade().create(rb);

        return rb;

    }

    public String returnBill() {
        if (refundingItems.isEmpty()) {
            UtilityController.addErrorMessage("There is no item to Refund");
            return "";

        }
        if (refundAmount == 0.0) {
            UtilityController.addErrorMessage("There is no item to Refund");
            return "";
        }
        if (comment == null || comment.trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return "";
        }

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().isCancelled()) {
                UtilityController.addErrorMessage("Already Cancelled. Can not Refund again");
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
            //UtilityController.addSuccessMessage("Refunded");

        } else {
            UtilityController.addErrorMessage("No Bill to refund");
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
        rb.setComments(comment);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setCreditCompany(getBill().getCreditCompany());
        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));
        rb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));

        rb.setToDepartment(getBill().getToDepartment());
        rb.setToInstitution(getBill().getToInstitution());

        rb.setFromDepartment(getBill().getFromDepartment());
        rb.setFromInstitution(getBill().getFromInstitution());

        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDepartment(getSessionController().getDepartment());

        rb.setNetTotal(refundAmount);
        rb.setPatient(getBill().getPatient());
        rb.setPatientEncounter(getBill().getPatientEncounter());
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
            List<BillFee> rbf = getBillFeeFacade().findBySQL(sql);
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
            rbi.copy(bi);
            rbi.invertValue(bi);
            rbi.setBill(rb);
            rbi.setCreatedAt(Calendar.getInstance().getTime());
            rbi.setCreater(getSessionController().getLoggedUser());
            rbi.setReferanceBillItem(bi);
            getBillItemFacede().create(rbi);

            bi.setRefunded(Boolean.TRUE);
            getBillItemFacede().edit(bi);
            System.out.println("bi.getRefunded() = " + bi.getRefunded());
            BillItem bbb = getBillItemFacade().find(bi.getId());
            System.out.println("bbb.getRefunded() = " + bbb.getRefunded());

            String sql = "Select bf From BillFee bf where "
                    + " bf.retired=false and bf.billItem.id=" + bi.getId();
            List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);

            returnBillFee(rb, rbi, tmp);

        }
    }

    public void refundBillItems(RefundBill rb, Payment p) {
        for (BillItem bi : refundingItems) {
            System.out.println("refundingItems = " + refundingItems);
            //set Bill Item as Refunded

            BillItem rbi = new BillItem();
            rbi.copy(bi);
            rbi.invertValue(bi);
            rbi.setBill(rb);
            rbi.setCreatedAt(Calendar.getInstance().getTime());
            rbi.setCreater(getSessionController().getLoggedUser());
            rbi.setReferanceBillItem(bi);
            getBillItemFacede().create(rbi);

            bi.setRefunded(Boolean.TRUE);
            getBillItemFacede().edit(bi);
            System.out.println("bi.getRefunded() = " + bi.getRefunded());
            BillItem bbb = getBillItemFacade().find(bi.getId());
            System.out.println("bbb.getRefunded() = " + bbb.getRefunded());

            String sql = "Select bf From BillFee bf where "
                    + " bf.retired=false and bf.billItem.id=" + bi.getId();
            List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);

            returnBillFee(rb, rbi, tmp);

            //create BillFeePayments For Refund
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + rbi.getId();
            List<BillFee> tmpC = getBillFeeFacade().findBySQL(sql);
            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(rb, tmpC, p);
            //
            
            rb.getBillItems().add(rbi);

        }
    }

    public void recreateModel() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
        refundingItems = null;
//        billFees
        billComponents = null;
        billForRefund = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
        lazyBills = null;
        searchKeyword = null;
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
            bC.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            bC.setCreater(getSessionController().getLoggedUser());
            getBillCommponentFacade().create(bC);
        }

    }

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findBySQL(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean checkPaidIndividual(BillItem bi) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tempFe = getBillFeeFacade().findBySQL(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private CancelledBill createCancelBill() {
        CancelledBill cb = new CancelledBill();
        if (getBill() != null) {
            cb.copy(getBill());
            cb.invertValue(getBill());

            if (getBill().getBillType() == BillType.PaymentBill) {
                cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PROCAN));
                cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PROCAN));
            } else {
                cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getToDepartment(), getBill().getBillType(), BillClassType.CancelledBill, BillNumberSuffix.CAN));
                cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getToDepartment(), getBill().getBillType(), BillClassType.CancelledBill, BillNumberSuffix.CAN));
            }

        }
        cb.setBalance(0.0);
        cb.setPaymentMethod(paymentMethod);
        cb.setBilledBill(getBill());
        cb.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

        return cb;
    }

    private CancelledBill createCahsInOutCancelBill(BillNumberSuffix billNumberSuffix) {
        CancelledBill cb = new CancelledBill();
        if (getBill() != null) {
            cb.copy(getBill());
            cb.invertValue(getBill());

            cb.setBilledBill(getBill());

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, billNumberSuffix));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, billNumberSuffix));

        }

        cb.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setPaymentMethod(paymentMethod);
        cb.setComments(comment);

        return cb;
    }

    private boolean errorCheck() {
        if (getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getPaymentMethod() == PaymentMethod.Credit && getBill().getPaidAmount() != 0.0) {
            UtilityController.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            UtilityController.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

//        if (getBill().getBillType() == BillType.LabBill && patientInvestigation.getCollected()== true) {
//            UtilityController.addErrorMessage("You can't cancell mark as collected");
//            return true;
//        }
        if (!getWebUserController().hasPrivilege("LabBillCancelSpecial")) {

            //System.out.println("patientInvestigationController.sampledForAnyItemInTheBill(bill) = " + patientInvestigationController.sampledForAnyItemInTheBill(bill));
            if (patientInvestigationController.sampledForAnyItemInTheBill(bill)) {
                UtilityController.addErrorMessage("Sample Already collected can't cancel");
                return true;
            }
        }

        if (getBill().getBillType() != BillType.LabBill && getPaymentMethod() == null) {
            UtilityController.addErrorMessage("Please select a payment scheme.");
            return true;
        }

        if (getComment() == null || getComment().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void cancelBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }

            CancelledBill cb = createCancelBill();
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            Calendar created = Calendar.getInstance();
            created.setTime(cb.getBilledBill().getCreatedAt());

            //Copy & paste
            if ((now.get(Calendar.DATE) == created.get(Calendar.DATE))
                    || (getBill().getBillType() == BillType.LabBill && getWebUserController().hasPrivilege("LabBillCancelling"))
                    || (getBill().getBillType() == BillType.OpdBill && getWebUserController().hasPrivilege("OpdCancel"))) {

                getBillFacade().create(cb);
                Payment p = getOpdPreSettleController().createPayment(cb, paymentMethod);
                List<BillItem> list = cancelBillItems(cb, p);
                cb.setBillItems(list);
                billFacade.edit(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit(getBill());
                UtilityController.addSuccessMessage("Cancelled");

                WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
                getSessionController().setLoggedUser(wb);

                if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
                    //   //System.out.println("getBill().getPaymentMethod() = " + getBill().getPaymentMethod());
                    //   //System.out.println("getBill().getToStaff() = " + getBill().getToStaff());
                    if (getBill().getToStaff() != null) {
                        //   //System.out.println("getBill().getNetTotal() = " + getBill().getNetTotal());
                        staffBean.updateStaffCredit(getBill().getToStaff(), 0 - getBill().getNetTotal());
                        UtilityController.addSuccessMessage("Staff Credit Updated");
                        cb.setFromStaff(getBill().getToStaff());
                        getBillFacade().edit(cb);
                    }
                }

                bill = billFacade.find(bill.getId());
                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                UtilityController.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            UtilityController.addErrorMessage("No Bill to cancel");
        }

    }

    @EJB
    private StaffBean staffBean;
    @EJB
    WebUserFacade webUserFacade;

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public void cancelCashInBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            CancelledBill cb = createCahsInOutCancelBill(BillNumberSuffix.CSINCAN);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            Calendar created = Calendar.getInstance();
            created.setTime(cb.getBilledBill().getCreatedAt());

            //Copy & paste
            if ((now.get(Calendar.DATE) == created.get(Calendar.DATE))
                    || (getBill().getBillType() == BillType.LabBill && getWebUserController().hasPrivilege("LabBillCancelling"))
                    || (getBill().getBillType() == BillType.OpdBill && getWebUserController().hasPrivilege("OpdCancel"))) {

                getBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit(getBill());
                UtilityController.addSuccessMessage("Cancelled");

                CashTransaction newCt = new CashTransaction();
                newCt.invertQty(getBill().getCashTransaction());

                CashTransaction tmp = getCashTransactionBean().saveCashOutTransaction(newCt, cb, getSessionController().getLoggedUser());
                cb.setCashTransaction(tmp);
                getBillFacade().edit(cb);

//                getCashTransactionBean().deductFromBallance(getSessionController().getLoggedUser().getDrawer(), tmp);
                WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
                getSessionController().setLoggedUser(wb);
                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                UtilityController.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            UtilityController.addErrorMessage("No Bill to cancel");
        }

    }

    public void cancelCashOutBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            CancelledBill cb = createCahsInOutCancelBill(BillNumberSuffix.CSOUTCAN);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            Calendar created = Calendar.getInstance();
            created.setTime(cb.getBilledBill().getCreatedAt());

            //Copy & paste
            if ((now.get(Calendar.DATE) == created.get(Calendar.DATE))
                    || (getBill().getBillType() == BillType.LabBill && getWebUserController().hasPrivilege("LabBillCancelling"))
                    || (getBill().getBillType() == BillType.OpdBill && getWebUserController().hasPrivilege("OpdCancel"))) {

                getBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit(getBill());
                UtilityController.addSuccessMessage("Cancelled");

                CashTransaction newCt = new CashTransaction();
                newCt.invertQty(getBill().getCashTransaction());

                CashTransaction tmp = getCashTransactionBean().saveCashInTransaction(newCt, cb, getSessionController().getLoggedUser());
                cb.setCashTransaction(tmp);
                getBillFacade().edit(cb);

//                getCashTransactionBean().addToBallance(getSessionController().getLoggedUser().getDrawer(), tmp);
                WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
                getSessionController().setLoggedUser(wb);

                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                UtilityController.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            UtilityController.addErrorMessage("No Bill to cancel");
        }

    }

    @EJB
    private ItemBatchFacade itemBatchFacade;

    private void returnBillFee(Bill rb, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(rb);
            bf.setBillItem(bt);
            bf.setSettleValue(0 - nB.getSettleValue());
            bf.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

    public void cancelPaymentBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }
            CancelledBill cb = createCancelBill();
            //Copy & paste

            getBillFacade().create(cb);
            Payment p = getOpdPreSettleController().createPayment(cb, paymentMethod);
//            cancelBillItems(cb);
            cancelBillItems(cb,p);
            cancelPaymentItems(bill);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            UtilityController.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            UtilityController.addErrorMessage("No Bill to cancel");
            return;
        }
    }

    private void cancelPaymentItems(Bill pb) {
        List<BillItem> pbis;
        pbis = getBillItemFacede().findBySQL("SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + pb.getId());
        for (BillItem pbi : pbis) {
            if (pbi.getPaidForBillFee() != null) {
                pbi.getPaidForBillFee().setPaidValue(0.0);
                getBillFeeFacade().edit(pbi.getPaidForBillFee());
            }
        }
    }

    List<Bill> billsToApproveCancellation;
    List<Bill> billsApproving;
    private Bill billForCancel;

    public void approveCancellation() {

        if (billsApproving == null) {
            UtilityController.addErrorMessage("Select Bill to Approve Cancell");
            return;
        }
        for (Bill b : billsApproving) {

            b.setApproveUser(getSessionController().getCurrent());
            b.setApproveAt(Calendar.getInstance().getTime());
            getBillFacade().create(b);

            cancelBillItems(b);
            b.getBilledBill().setCancelled(true);
            b.getBilledBill().setCancelledBill(b);

            getBillFacade().edit(b);

            ejbApplication.getBillsToCancel().remove(b);

            UtilityController.addSuccessMessage("Cancelled");

        }

        billForCancel = null;
    }

    public List<Bill> getOpdBillsToApproveCancellation() {
        ////System.out.println("1");
        billsToApproveCancellation = ejbApplication.getOpdBillsToCancel();
        return billsToApproveCancellation;
    }

    public List<Bill> getBillsToApproveCancellation() {
        ////System.out.println("1");
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

    private List<BillItem> cancelBillItems(Bill can) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);

            if (can.getBillType() != BillType.PaymentBill) {
                b.setItem(nB.getItem());
            } else {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            }

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            getBillItemFacede().create(b);

            cancelBillComponents(can, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);
////////////////////////

            cancelBillFee(can, b, tmp);

            list.add(b);

        }

        return list;
    }

    private List<BillItem> cancelBillItems(Bill can, Payment p) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);

            if (can.getBillType() != BillType.PaymentBill) {
                b.setItem(nB.getItem());
            } else {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            }

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            getBillItemFacede().create(b);

            cancelBillComponents(can, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);
////////////////////////
            System.out.println("tmp = " + tmp.size());
            cancelBillFee(can, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findBySQL(sql);
            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(can, tmpC, p);
            //

            list.add(b);

        }

        return list;
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.setFee(nB.getFee());
            bf.setPatienEncounter(nB.getPatienEncounter());
            bf.setPatient(nB.getPatient());
            bf.setDepartment(nB.getDepartment());
            bf.setInstitution(nB.getInstitution());
            bf.setSpeciality(nB.getSpeciality());
            bf.setStaff(nB.getStaff());

            bf.setBill(can);
            bf.setBillItem(bt);
            bf.setFeeValue(0 - nB.getFeeValue());
            bf.setSettleValue(0 - nB.getSettleValue());

            bf.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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

    public List<Bill> getPos() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyOrder);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyOrder);
            }
            if (bills == null) {
                bills = new ArrayList<>();
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
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrnReturns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnReturn);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnReturn);
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
                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                            + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";

                } else {
                    sql = "select b from BilledBill b where b.retired=false and"
                            + " b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) "
                            + "and b.billType=:type and b.createdAt between :fromDate and :toDate and (upper(b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or upper(b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or upper(b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
                }

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        ////System.out.println("getting user bills");
        if (getUser() == null) {
            userBills = new ArrayList<>();
            ////System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
            ////System.out.println("user ok");
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
        //    recreateModel();
    }

    public Bill getBill() {
        //recreateModel();
        if (bill == null) {
            bill = new Bill();
        }
        return bill;
    }

    @Inject
    private BillController billController;

    public void setBill(Bill bill) {
        //recreateModel();
        System.err.println("Bill " + bill);
        this.bill = bill;
        paymentMethod = bill.getPaymentMethod();
        createBillItems();

        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

//    public List<BillItem> getBillItems() {
//        String sql = "";
//        if (getBill() != null) {
//            if (getBill().getRefundedBill() == null) {
//                sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getId();
//            } else {
//                sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getRefundedBill().getId();
//            }
//            billItems = getBillItemFacede().findBySQL(sql);
//            // ////System.out.println("sql for bill item search is " + sql);
//            // ////System.out.println("results for bill item search is " + billItems);
//            if (billItems == null) {
//                billItems = new ArrayList<>();
//            }
//        }
//
//        return billItems;
//    }
    public List<BillItem> getBillItems() {
        return billItems;
    }

    private void createBillItems() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b"
                + "  WHERE b.retired=false "
                + " and b.bill=:b";
        hm.put("b", getBillSearch());
        billItems = getBillItemFacede().findBySQL(sql, hm);

    }

    private void createBillItemsForRetire() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b WHERE "
                + " b.bill=:b";
        hm.put("b", getBillSearch());
        billItems = getBillItemFacede().findBySQL(sql, hm);

    }

    public List<PharmaceuticalBillItem> getPharmacyBillItems() {
        List<PharmaceuticalBillItem> tmp = new ArrayList<>();
        if (getBill() != null) {
            String sql = "SELECT b FROM PharmaceuticalBillItem b WHERE b.billItem.retired=false and b.billItem.bill.id=" + getBill().getId();
            tmp = getPharmaceuticalBillItemFacade().findBySQL(sql);
        }

        return tmp;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findBySQL(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    private List<BillFee> billFeesList;

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null || billForRefund == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findBySQL(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getBillFees2() {
        if (billFees == null) {
            if (getBill() != null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findBySQL(sql);
            }

            if (getBillSearch() != null) {
                String sql = "SELECT b FROM BillFee b WHERE b.bill.id=" + getBillSearch().getId();
                billFees = getBillFeeFacade().findBySQL(sql);
            }

            if (billFees == null) {
                billFees = new ArrayList<>();
            }
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findBySQL(sql);
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
     *
     * @return
     */
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
        billForRefund.setPaymentMethod(getBill().getPaymentMethod());
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
            return 0.0;
        }
        double tot = 0.0;
        for (BillFee f : getBillFees()) {
            ////System.out.println("Tot" + f.getFeeValue());
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

    public Bill getBillForCancel() {
        return billForCancel;
    }

    public void setBillForCancel(Bill billForCancel) {
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
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        //  resetLists();
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        //resetLists();
        this.fromDate = fromDate;

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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
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

    @Inject
    private PatientInvestigationController patientInvestigationController;

    private boolean checkCollectedReported() {
        return patientInvestigationController.sampledForAnyItemInTheBill(bill);
    }

    public PharmacyPreSettleController getPharmacyPreSettleController() {
        return pharmacyPreSettleController;
    }

    public void setPharmacyPreSettleController(PharmacyPreSettleController pharmacyPreSettleController) {
        this.pharmacyPreSettleController = pharmacyPreSettleController;
    }

    public LazyDataModel<Bill> getLazyBills() {
        return lazyBills;
    }

    public void setLazyBills(LazyDataModel<Bill> lazyBills) {
        this.lazyBills = lazyBills;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public LazyDataModel<BillItem> getSearchBillItems() {
        return searchBillItems;
    }

    public void setSearchBillItems(LazyDataModel<BillItem> searchBillItems) {
        this.searchBillItems = searchBillItems;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public Bill getBillSearch() {
        return bill;
    }

    public Institution getCreditCompany() {
        if (getBillSearch().getPatientEncounter() == null) {
            creditCompany = getBillSearch().getCreditCompany();
        } else {
            creditCompany = getBillSearch().getPatientEncounter().getCreditCompany();
        }

        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public void setBillSearch(Bill bill) {

        recreateModel();
        System.err.println("Bill " + bill);
        this.bill = bill;
        paymentMethod = bill.getPaymentMethod();
        createBillItemsForRetire();
        createBillFees();
        createBillItemsAll();
    }

    public void fillBillTypeIncomeRecords() {

        String jpql;
        jpql = "select new com.divudi.bean.common.BillTypeIncomeRecord(b.billType,sum(b.netTotal)) from Bill b "
                + " where b.billType in :bts "
                + " group by b.billType "
                + " order by b.billType";
    }

    public double getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(double refundTotal) {
        this.refundTotal = refundTotal;
    }

    public double getRefundDiscount() {
        return refundDiscount;
    }

    public void setRefundDiscount(double refundDiscount) {
        this.refundDiscount = refundDiscount;
    }

    public double getRefundMargin() {
        return refundMargin;
    }

    public void setRefundMargin(double refundMargin) {
        this.refundMargin = refundMargin;
    }

    public StaffBean getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffBean staffBean) {
        this.staffBean = staffBean;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public List<BillFee> getBillFeesList() {
        return billFeesList;
    }

    public void setBillFeesList(List<BillFee> billFeesList) {
        this.billFeesList = billFeesList;
    }

    public PatientInvestigationController getPatientInvestigationController() {
        return patientInvestigationController;
    }

    public void setPatientInvestigationController(PatientInvestigationController patientInvestigationController) {
        this.patientInvestigationController = patientInvestigationController;
    }

    public List<BillItem> getBillItemList() {
        return billItemList;
    }

    public void setBillItemList(List<BillItem> billItemList) {
        this.billItemList = billItemList;
    }

    public OpdPreSettleController getOpdPreSettleController() {
        return opdPreSettleController;
    }

    public void setOpdPreSettleController(OpdPreSettleController opdPreSettleController) {
        this.opdPreSettleController = opdPreSettleController;
    }

    public class BillTypeIncomeRecord {

        BillType billType;
        Double netTotal;

        public BillTypeIncomeRecord(BillType billType, Double netTotal) {
            this.billType = billType;
            this.netTotal = netTotal;
        }

        public BillType getBillType() {
            return billType;
        }

        public void setBillType(BillType billType) {
            this.billType = billType;
        }

        public Double getNetTotal() {
            return netTotal;
        }

        public void setNetTotal(Double netTotal) {
            this.netTotal = netTotal;
        }

    }

}

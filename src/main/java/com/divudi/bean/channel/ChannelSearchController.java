/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.WebUserController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ChannelSearchController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillItemFacade billItemFacede;
    @EJB
    BillComponentFacade billCommponentFacade;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    BillNumberGenerator billNumberBean;

    /**
     * Controllers
     */
    @Inject
    private BookingController bookingController;
    @Inject
    private ChannelBillController channelBillController;
    @Inject
    SessionController sessionController;
    @Inject
    WebUserController webUserController;
    @Inject
    BillController billController;
    @Inject
    CommonController commonController;
    /**
     * Properties
     */
    Bill bill;
    private Date date;
    Date fromDate;
    Date toDate;

    List<BillItem> billItems;
    List<BillComponent> billComponents;
    private List<BillSession> billSessions;
    private List<BillSession> searchedBillSessions;
    private List<BillSession> filteredbillSessions;

    PaymentMethod paymentMethod;
    String comment;
    String txtSearch;
    String txtSearchRef;
    String txtSearchPhone;
    boolean printPreview = false;

    /**
     * Creates a new instance of ChannelSearchController
     */
    public ChannelSearchController() {
    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        billSessions = null;
        filteredbillSessions = null;
    }

    public void searchForBillSessions() {
         //System.out.println("getFromDate() = " + getFromDate());
         //System.out.println("getToDate() = " + getToDate());
        //// // System.out.println("txtSearch = " + txtSearch);
        //// // System.out.println("txtSearchRef = " + txtSearchRef);
        if (getFromDate() == null && getToDate() == null 
                && (txtSearch == null || txtSearch.trim().equals("")) 
                && (txtSearchRef == null || txtSearchRef.trim().equals("")) 
                && (txtSearchPhone == null || txtSearchPhone.trim().equals(""))) {
            JsfUtil.addErrorMessage("Please Select From To Dates or BillNo Or Agent Referane No. or Telephone No");
            return;
        }
        if ((getFromDate() == null && getToDate() != null) || (getFromDate() != null && getToDate() == null)) {
            JsfUtil.addErrorMessage("Please Check From,TO Dates");
            return;
        }
        if (getFromDate() != null && getToDate() != null) {
            double count = commonController.dateDifferenceInMinutes(getFromDate(), getToDate()) / (60 * 24);
            if (count > 1) {
                JsfUtil.addErrorMessage("Please Selected Date Range To Long.(Date Range limit for 1 day)");
                return;
            }
        }

        String sql;
        HashMap m = new HashMap();

        sql = "Select bs From BillSession bs "
                + " where bs.retired=false "
                + " and type(bs.bill)=:class ";

        if (txtSearch != null && !txtSearch.trim().equals("")) {
            sql += " and  ((bs.bill.insId like :ts ) "
                    + " or (bs.bill.deptId like :ts ))";
            m.put("ts", "%" + txtSearch.trim().toUpperCase() + "%");
        }

        if (txtSearchRef != null && !txtSearchRef.trim().equals("")) {
            sql += " and bs.billItem.agentRefNo like :ts2 ";
            m.put("ts2", "%" + txtSearchRef.trim().toUpperCase() + "%");
        }
        
        if (txtSearchPhone != null && !txtSearchPhone.trim().equals("")) {
            sql += " and bs.bill.patient.person.phone like :ts3";
            m.put("ts3", "%" + txtSearchPhone.trim().toUpperCase() + "%");
        }

        if (getFromDate() != null && getToDate() != null) {
            sql += " and bs.sessionDate between :fd and :td ";
            m.put("fd", getFromDate());
            m.put("td", getToDate());
        }

        sql += " order by bs.id desc";

        m.put("class", BilledBill.class);

        if (getFromDate() != null && getToDate() != null) {
            searchedBillSessions = getBillSessionFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            searchedBillSessions = getBillSessionFacade().findByJpql(sql, m);
        }

    }

    public List<BillSession> getBillSessions() {

        if (billSessions == null) {
            if (getDate() != null) {
                String sql = "Select bs From BillSession bs "
                        + " where bs.retired=false "
                        + " and bs.sessionDate= :ssDate "
                        + " order by  bs.serviceSession.staff.speciality.name,"
                        + " bs.serviceSession.staff.person.name,"
                        + " bs.serviceSession.id,"
                        + " bs.serialNo";
                HashMap hh = new HashMap();
                hh.put("ssDate", getDate());
                billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

            }
        }

        return billSessions;
    }

    public void cancelPaymentBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }
            CancelledBill cb = createCancelBill();
            //Copy & paste

            getBillFacade().create(cb);
            cancelBillItems(cb);
            cancelPaymentItems(bill);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }
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

        if (getPaymentMethod() == PaymentMethod.Credit && getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

//        if (getBill().getBillType() == BillType.LabBill && patientInvestigation.getCollected()== true) {
//            JsfUtil.addErrorMessage("You can't cancell mark as collected");
//            return true;
//        }
//        if (!getWebUserController().hasPrivilege("LabBillCancelSpecial")) {
//
//            ////// // System.out.println("patientInvestigationController.sampledForAnyItemInTheBill(bill) = " + patientInvestigationController.sampledForAnyItemInTheBill(bill));
//            if (patientInvestigationController.sampledForAnyItemInTheBill(bill)) {
//                JsfUtil.addErrorMessage("Sample Already collected can't cancel");
//                return true;
//            }
//        }
        if (getBill().getBillType() != BillType.LabBill && getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment scheme.");
            return true;
        }

        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
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
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

        return cb;
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

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            getBillItemFacede().create(b);

            cancelBillComponents(can, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
////////////////////////

            cancelBillFee(can, b, tmp);

            list.add(b);

        }

        return list;
    }

    private void cancelPaymentItems(Bill pb) {
        List<BillItem> pbis;
        pbis = getBillItemFacede().findByJpql("SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + pb.getId());
        for (BillItem pbi : pbis) {
            if (pbi.getPaidForBillFee() != null) {
                pbi.getPaidForBillFee().setPaidValue(0.0);
                getBillFeeFacade().edit(pbi.getPaidForBillFee());
            }
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

            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
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

    private void createBillItems() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b"
                + "  WHERE b.retired=false "
                + " and b.bill=:b";
        hm.put("b", getBill());
        billItems = getBillItemFacede().findByJpql(sql, hm);

    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public List<BillSession> getFilteredbillSessions() {
        return filteredbillSessions;
    }

    public void setFilteredbillSessions(List<BillSession> filteredbillSessions) {
        this.filteredbillSessions = filteredbillSessions;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public BookingController getBookingController() {
        return bookingController;
    }

    public void setBookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }

    public ChannelBillController getChannelBillController() {
        return channelBillController;
    }

    public void setChannelBillController(ChannelBillController channelBillController) {
        this.channelBillController = channelBillController;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
        paymentMethod = bill.getPaymentMethod();
        createBillItems();

        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
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

    public BillItemFacade getBillItemFacede() {
        return billItemFacede;
    }

    public void setBillItemFacede(BillItemFacade billItemFacede) {
        this.billItemFacede = billItemFacede;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillComponentFacade getBillCommponentFacade() {
        return billCommponentFacade;
    }

    public void setBillCommponentFacade(BillComponentFacade billCommponentFacade) {
        this.billCommponentFacade = billCommponentFacade;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
    }

    public List<BillSession> getSearchedBillSessions() {
        return searchedBillSessions;
    }

    public void setSearchedBillSessions(List<BillSession> searchedBillSessions) {
        this.searchedBillSessions = searchedBillSessions;
    }

    public String getTxtSearchRef() {
        return txtSearchRef;
    }

    public void setTxtSearchRef(String txtSearchRef) {
        this.txtSearchRef = txtSearchRef;
    }

    public String getTxtSearchPhone() {
        return txtSearchPhone;
    }

    public void setTxtSearchPhone(String txtSearchPhone) {
        this.txtSearchPhone = txtSearchPhone;
    }

}

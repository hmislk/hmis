/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.channel;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.WebUserController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillSession;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillSessionFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;

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
    @EJB
    private PaymentFacade paymentFacade;

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
    DrawerController drawerController;
    @Inject
    private BillBeanController billBean;
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
                && (txtSearch == null || txtSearch.trim().isEmpty())
                && (txtSearchRef == null || txtSearchRef.trim().isEmpty())
                && (txtSearchPhone == null || txtSearchPhone.trim().isEmpty())) {
            JsfUtil.addErrorMessage("Please Select From To Dates or BillNo Or Agent Referane No. or Telephone No");
            return;
        }
        if ((getFromDate() == null && getToDate() != null) || (getFromDate() != null && getToDate() == null)) {
            JsfUtil.addErrorMessage("Please Check From,TO Dates");
            return;
        }
        if (getFromDate() != null && getToDate() != null) {
            double count = CommonFunctions.dateDifferenceInMinutes(getFromDate(), getToDate()) / (60 * 24);
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

        if (txtSearch != null && !txtSearch.trim().isEmpty()) {
            sql += " and  ((bs.bill.insId like :ts ) "
                    + " or (bs.bill.deptId like :ts ))";
            m.put("ts", "%" + txtSearch.trim().toUpperCase() + "%");
        }

        if (txtSearchRef != null && !txtSearchRef.trim().isEmpty()) {
            sql += " and bs.billItem.agentRefNo like :ts2 ";
            m.put("ts2", "%" + txtSearchRef.trim().toUpperCase() + "%");
        }

        if (txtSearchPhone != null && !txtSearchPhone.trim().isEmpty()) {
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
        if (bill != null && bill.getId() != null && bill.getId() != 0) {
            if (errorCheck()) {
                return;
            }
            CancelledBill newlyCreatedCancellationProfessionalPaymentBill = createCancelBill(bill);
            getBillFacade().create(newlyCreatedCancellationProfessionalPaymentBill);
            Payment cancellationPayment = createPaymentForCancellationsAndRefunds(newlyCreatedCancellationProfessionalPaymentBill, paymentMethod);
            //cancelBillItems(cb);
            drawerController.updateDrawerForIns(cancellationPayment);
            cancelProfessionalPaymentBillItems(newlyCreatedCancellationProfessionalPaymentBill, bill);
            cancelPaymentItems(bill);
            getBill().setCancelled(true);
            getBill().setCancelledBill(newlyCreatedCancellationProfessionalPaymentBill);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(newlyCreatedCancellationProfessionalPaymentBill, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            bill = newlyCreatedCancellationProfessionalPaymentBill;
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }
    }

    private List<BillItem> cancelProfessionalPaymentBillItems(Bill newlyCreatedCancellationProfessionalPaymentBill, Bill originalPaymentBill) {
        List<BillItem> originalProfessionalPaymentBillItems = billBean.fetchBillItems(originalPaymentBill);
        List<BillItem> newlyCreatedCancellationBillItems = new ArrayList<>();
        if (originalProfessionalPaymentBillItems == null) {
            return newlyCreatedCancellationBillItems;
        }
        for (BillItem originalProfessionalPaymentBillItem : originalProfessionalPaymentBillItems) {
            BillItem newlyCreatedCancellationProfessionalBillItem = new BillItem();
            newlyCreatedCancellationProfessionalBillItem.setBill(newlyCreatedCancellationProfessionalPaymentBill);

            newlyCreatedCancellationProfessionalBillItem.setReferanceBillItem(originalProfessionalPaymentBillItem.getReferanceBillItem());

            newlyCreatedCancellationProfessionalBillItem.setNetValue(0 - originalProfessionalPaymentBillItem.getNetValue());
            newlyCreatedCancellationProfessionalBillItem.setGrossValue(0 - originalProfessionalPaymentBillItem.getGrossValue());
            newlyCreatedCancellationProfessionalBillItem.setRate(0 - originalProfessionalPaymentBillItem.getRate());
            newlyCreatedCancellationProfessionalBillItem.setVat(0 - originalProfessionalPaymentBillItem.getVat());
            newlyCreatedCancellationProfessionalBillItem.setVatPlusNetValue(0 - originalProfessionalPaymentBillItem.getVatPlusNetValue());

            newlyCreatedCancellationProfessionalBillItem.setCatId(originalProfessionalPaymentBillItem.getCatId());
            newlyCreatedCancellationProfessionalBillItem.setDeptId(originalProfessionalPaymentBillItem.getDeptId());
            newlyCreatedCancellationProfessionalBillItem.setInsId(originalProfessionalPaymentBillItem.getInsId());
            newlyCreatedCancellationProfessionalBillItem.setDiscount(0 - originalProfessionalPaymentBillItem.getDiscount());
            newlyCreatedCancellationProfessionalBillItem.setQty(1.0);
            newlyCreatedCancellationProfessionalBillItem.setRate(originalProfessionalPaymentBillItem.getRate());
            newlyCreatedCancellationProfessionalBillItem.setCreatedAt(new Date());
            newlyCreatedCancellationProfessionalBillItem.setCreater(getSessionController().getLoggedUser());
            newlyCreatedCancellationProfessionalBillItem.setPaidForBillFee(originalProfessionalPaymentBillItem.getPaidForBillFee());

            getBillItemFacede().create(newlyCreatedCancellationProfessionalBillItem);
            List<BillFee> originalProfessionalPaymentFeesForBillItem = billBean.fetchBillFees(originalProfessionalPaymentBillItem);
            cancelBillFee(newlyCreatedCancellationProfessionalPaymentBill, newlyCreatedCancellationProfessionalBillItem, originalProfessionalPaymentFeesForBillItem);
            newlyCreatedCancellationBillItems.add(newlyCreatedCancellationProfessionalBillItem);

        }

        return newlyCreatedCancellationBillItems;
    }

    public Payment createPaymentForCancellationsAndRefunds(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        double valueToSet = Math.abs(bill.getNetTotal());
        p.setPaidValue(valueToSet);
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
        getPaymentFacade().edit(p);
    }

    public String navigateTocancelPaymentBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        printPreview = false;
        return "/channel/channel_payment_staff_bill_cancel?faces-redirect=true";
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

    private CancelledBill createCancelBill(Bill originalBill) {
        if (originalBill == null) {
            JsfUtil.addErrorMessage("No Bill to Cancel");
            return null;
        }
        CancelledBill cb = new CancelledBill();
        cb.copy(originalBill);
        cb.copyValue(originalBill);
        cb.invertAndAssignValuesFromOtherBill(originalBill);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN);
        cb.setDeptId(deptId);
        cb.setInsId(deptId);
        cb.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN);
        cb.setBalance(0.0);
        cb.setPaymentMethod(paymentMethod);
        cb.setBilledBill(originalBill);
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

    private void cancelPaymentItems(Bill originalBill) {
        List<BillItem> originalBillItems;
        originalBillItems = getBillItemFacede().findByJpql("SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + originalBill.getId());
        for (BillItem originalBillItem : originalBillItems) {
            if (originalBillItem.getPaidForBillFee() != null) {
                originalBillItem.getPaidForBillFee().setPaidValue(0.0);
                getBillFeeFacade().edit(originalBillItem.getPaidForBillFee());
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

    private void cancelBillFee(Bill newlyCreatedCancellationProfessionalPaymentBill, BillItem newlyCreatedCancellationProfessionalBillItem, List<BillFee> originalProfessionalPaymentFeesForBillItem) {
        for (BillFee originalProfessionalPaymentFeeForBillItem : originalProfessionalPaymentFeesForBillItem) {
            BillFee bf = new BillFee();
            bf.setFee(originalProfessionalPaymentFeeForBillItem.getFee());
            bf.setPatienEncounter(originalProfessionalPaymentFeeForBillItem.getPatienEncounter());
            bf.setPatient(originalProfessionalPaymentFeeForBillItem.getPatient());
            bf.setDepartment(originalProfessionalPaymentFeeForBillItem.getDepartment());
            bf.setInstitution(originalProfessionalPaymentFeeForBillItem.getInstitution());
            bf.setSpeciality(originalProfessionalPaymentFeeForBillItem.getSpeciality());
            bf.setStaff(originalProfessionalPaymentFeeForBillItem.getStaff());

            bf.setBill(newlyCreatedCancellationProfessionalPaymentBill);
            bf.setBillItem(newlyCreatedCancellationProfessionalBillItem);
            bf.setFeeValue(0 - originalProfessionalPaymentFeeForBillItem.getFeeValue());

            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);

            originalProfessionalPaymentFeeForBillItem.setPaidValue(0);
            getBillFeeFacade().edit(originalProfessionalPaymentFeeForBillItem);
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

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberController;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.ServiceSession;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ChannelBillController implements Serializable {

    private BillSession billSession;
    private Bill current;
    private String patientTabId = "tabNewPt";
    private Patient newPatient;
    private Patient searchPatient;
    private String agentRefNo;
    private double amount;
    private boolean foriegn = false;
    ///////////////////////////////////
    private List<BillFee> billFee;
    private List<BillFee> refundBillFee;
    /////////////////////////////////
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    //////////////////////////////////
    @EJB
    private ServiceSessionBean serviceSessionBean;
    //////////////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private BookingController bookingController;
    @Inject
    private BillNumberController billNumberBean;
    @EJB
    private ChannelBean channelBean;
    List<BillItem> billItems;

    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            newPatient.setPerson(new Person());
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Patient getSearchPatient() {
        return searchPatient;
    }

    public void setSearchPatient(Patient searchPatient) {
        this.searchPatient = searchPatient;
    }

    public void settleCredit() {
        if (errorCheckForSettle()) {
            return;
        }

        Bill b = saveBill();
        BillItem bi = saveBillItem(b);
        saveBillFee(b, bi);

        // getBillSession().getBill().setReferenceBill(b);
        getBillFacade().edit(getBillSession().getBill());

        editBillSession(b, bi);

        UtilityController.addSuccessMessage("Channel Booking Added");

    }

//
//    private void deductBallance() {
//        double tmp = getBilledTotalFee() - getAgentPay().getBilledFee().getFeeValue();
//        getBillSession().getBill().getFromInstitution().setBallance(getBillSession().getBill().getFromInstitution().getBallance() - tmp);
//        getInstitutionFacade().edit(getBillSession().getBill().getFromInstitution());
//    }
    private Bill saveBill() {
        Bill temp = new BilledBill();

        temp.setReferenceBill(getBillSession().getBill());
        temp.setFromInstitution(getBillSession().getBill().getFromInstitution());
        temp.setStaff(getBillSession().getBill().getStaff());

        if (getBillSession().getBill().getFromInstitution() != null) {
            temp.setPaymentMethod(PaymentMethod.Agent);
        } else {
            temp.setPaymentMethod(PaymentMethod.Cash);
        }

        temp.setBookingId(getBillSession().getBill().getBookingId());

        temp.setBillType(BillType.ChannelPaid);

//        double amt = getBilledTotalFee();
//        temp.setTotal(amt);
//        temp.setNetTotal(amt - getAgentPay().getBilledFee().getFeeValue());
//
//        if (getBillSession().getBill().getFromInstitution() != null) {
//            deductBallance();
//        }
        temp.setPatient(getBillSession().getBill().getPatient());

        temp.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());

        temp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(temp);

        return temp;
    }

    private BillItem saveBillItem(Bill b) {
        BillItem bi = new BillItem();
        bi.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setBill(b);

//        double amt = getBilledTotalFee() - getAgentPay().getBilledFee().getFeeValue();
//        bi.setNetValue(amt);
        bi.setSessionDate(getBillSession().getSessionDate());
        getBillItemFacade().create(bi);

        return bi;
    }

    private void saveBillFee(Bill b, BillItem bi) {

        for (BillFee f : getBillFee()) {

            BillFee bf = new BillFee();

            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(getSessionController().getLoggedUser());

            bf.setStaff(f.getStaff());

            bf.setPatient(b.getPatient());
            bf.setBill(b);
            bf.setBillItem(bi);
            bf.setFee(f.getFee());
            bf.setFeeValue(f.getFeeValue());
            bf.setFeeGrossValue(f.getFeeValue());
            getBillFeeFacade().create(bf);
        }
    }

    private void editBillSession(Bill b, BillItem bi) {
        getBillSession().setBill(b);
        getBillSession().setBillItem(bi);

        getBillSessionFacade().edit(getBillSession());
    }

    private boolean errorCheckForSettle() {

        if (getBillSession().getBill().getPaymentMethod() == PaymentMethod.Credit) {

            if (getBillSession().getBill().getFromInstitution() != null
                    && getBillSession().getBill().getFromInstitution().getBallance()
                    - getBillSession().getBill().getTotal() < -getBillSession().getBill().getFromInstitution().getAllowedCredit()) {
                UtilityController.addErrorMessage("Agency Ballance is Not Enough");
                return true;
            }

        }

        return false;
    }

    private boolean errorCheckRefunding() {
        if (getBillSession().getBill().getBillType() == BillType.ChannelCredit) {
            UtilityController.addSuccessMessage("Credit Bill Cant be Refunded");
            return true;
        }

//        if (getDoctorFee().getBilledFee().getFeeValue() < getDoctorFee().getRepayment().getFeeValue()
//                || getHospitalFee().getBilledFee().getFeeValue() < getHospitalFee().getRepayment().getFeeValue()
//                || getTax().getBilledFee().getFeeValue() < getTax().getRepayment().getFeeValue()
//                || getAgentPay().getBilledFee().getFeeValue() < getAgentPay().getRepayment().getFeeValue()) {
//            UtilityController.addSuccessMessage("You can't refund mor than paid fee");
//            return true;
//        }
        return false;
    }

//    public void refund() {
//        if (errorCheckRefunding()) {
//            return;
//        }
//
//        RefundBill rb = (RefundBill) createRefundBill();
//
//        refundBillItems(rb);
//
//        getBillSession().getBill().setRefunded(true);
//        getBillSession().getBill().setRefundedBill(rb);
//        getBillFacade().edit(getBillSession().getBill());
//
//    }
    List<BillFee> listBillFees;

    public void createBillfees(SelectEvent event) {
        BillSession bs = ((BillSession) event.getObject());
        System.err.println("LISTED");
        System.err.println("BIS " + bs);
        String sql;
        HashMap hm = new HashMap();
        sql = "Select bf From BillFee bf where bf.retired=false"
                + " and bf.billItem=:bt ";
        hm.put("bt", bs.getBillItem());

        listBillFees = billFeeFacade.findBySQL(sql, hm);
        billSession = bs;

    }

    public BookingController getBookingController() {
        return bookingController;
    }

    public void setBookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }

    public List<BillFee> getListBillFees() {
        return listBillFees;
    }

    public void setListBillFees(List<BillFee> listBillFees) {
        this.listBillFees = listBillFees;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    private boolean errorCheckCancelling() {

        return false;
    }

    public void cancel() {
        if (errorCheckCancelling()) {
            return;
        }

        CancelledBill cb = createCancelBill();

        cancelBillItems(cb);

        getBillSession().getBill().setCancelled(true);
        getBillSession().getBill().setCancelledBill(cb);
        getBillFacade().edit(getBillSession().getBill());

        if (getBillSession().getBill().getReferenceBill() != null) {
            getBillSession().setBill(getBillSession().getBill().getReferenceBill());
            getBillSessionFacade().edit(getBillSession());
        }

        UtilityController.addSuccessMessage("Cancelled");

    }

//    private void cancelBillItemsOld(CancelledBill can) {
//        BillItem bi = getBillSession().getBillItem();
//
//        BillItem b = new BillItem();
//        b.setBill(can);
//
//        b.setNetValue(0 - bi.getNetValue());
//        b.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
//        b.setCreater(getSessionController().getLoggedUser());
//
//        getBillItemFacade().create(b);
//
//        cancelBillFeeOld(can, b);
//    }
    private void cancelBillItems(CancelledBill can) {
        BillItem bi = billSession.getBillItem();

        BillItem b = new BillItem();
        b.setBill(can);
        b.copy(bi);
        b.invertValue(bi);
        b.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        b.setCreater(getSessionController().getLoggedUser());

        getBillItemFacade().create(b);
        String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);
        cancelBillFee(can, b, tmp);

    }

//    private void cancelBillSession(CancelledBill can) {
//
//        BillSession bs = new BillSession();
//        bs.setBill(can);
//        bs.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
//        bs.setCreater(getSessionController().getLoggedUser());
//
//        getBillSessionFacade().create(bs);
//
//    }
    private CancelledBill createCancelBill() {
        CancelledBill cb = new CancelledBill();

        cb.setBilledBill(getBillSession().getBill());
        cb.setBillType(getBillSession().getBill().getBillType());

        cb.setBookingId(getBillSession().getBill().getBookingId());

        cb.setFromInstitution(getBillSession().getBill().getFromInstitution());

        cb.setPaymentMethod(getBillSession().getBill().getPaymentMethod());

        cb.setNetTotal(0 - getBillSession().getBill().getNetTotal());
        cb.setTotal(0 - getBillSession().getBill().getTotal());

        cb.setPatient(getBillSession().getBill().getPatient());
        cb.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        getBillFacade().create(cb);

//        if (cb.getPaymentMethod() == PaymentMethod.Agent) {
//            updateBallance(cb.getFromInstitution(), cb.getNetTotal(), HistoryType.ChannelBookingCancel,
//                    cb, null, billSession);
//        }
        return cb;
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(can);
            bf.setBillItem(bt);

            bf.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

//    private void addBallance(Bill cb, double amt) {
//        cb.getFromInstitution().setBallance(cb.getFromInstitution().getBallance() + amt);
//        getInstitutionFacade().edit(cb.getFromInstitution());
//    }
//
//    private void cancelBillFeeOld(Bill b, BillItem bt) {
//        if (getDoctorFee().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getDoctorFee(), getDoctorFee().getBilledFee().getFeeValue());
//        }
//
//        if (getHospitalFee().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getHospitalFee(), getHospitalFee().getBilledFee().getFeeValue());
//        }
//
//        if (getTax().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getTax(), getTax().getBilledFee().getFeeValue());
//        }
//
//        if (getAgentPay().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getAgentPay(), getAgentPay().getBilledFee().getFeeValue());
//        }
//
//    }
//    public void refundBillItems(RefundBill rb) {
//        BillItem bi = getBillSession().getBillItem();
//
//        BillItem rbi = new BillItem();
//        rbi.setBill(rb);
//        rbi.setCreatedAt(Calendar.getInstance().getTime());
//        rbi.setCreater(getSessionController().getLoggedUser());
//        rbi.setItem(bi.getItem());
//
//        double netValue = getRefundedTotalFee();
//
//        rbi.setNetValue(-netValue);
//        rbi.setRefunded(Boolean.TRUE);
//        rbi.setReferanceBillItem(bi);
//        getBillItemFacade().create(rbi);
//
//        bi.setRefunded(Boolean.TRUE);
//        getBillItemFacade().edit(bi);
//
//        returnBillFee(rb, rbi);
//
//    }
//    private void returnBillFee(Bill b, BillItem bt) {
//        if (getDoctorFee().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getDoctorFee(), getDoctorFee().getRepayment().getFeeValue());
//        }
//
//        if (getHospitalFee().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getHospitalFee(), getHospitalFee().getRepayment().getFeeValue());
//        }
//
//        if (getTax().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getTax(), getTax().getRepayment().getFeeValue());
//        }
//
//        if (getAgentPay().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getAgentPay(), getAgentPay().getRepayment().getFeeValue());
//        }
//    }
    private void createBillFee(Bill b, BillItem bt, double feeValue) {

//        cf.getRepayment().setFee(cf.getBilledFee().getFee());
//        cf.getRepayment().setPatient(cf.getBilledFee().getPatient());
//        cf.getRepayment().setDepartment(cf.getBilledFee().getDepartment());
//        cf.getRepayment().setInstitution(cf.getBilledFee().getInstitution());
//        cf.getRepayment().setSpeciality(cf.getBilledFee().getSpeciality());
//        cf.getRepayment().setStaff(cf.getBilledFee().getStaff());
//
//        cf.getRepayment().setBill(b);
//        cf.getRepayment().setBillItem(bt);
//        cf.getRepayment().setFeeValue(0-feeValue);
//
//        cf.getRepayment().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
//        cf.getRepayment().setCreater(getSessionController().getLoggedUser());
//
//        getBillFeeFacade().create(cf.getRepayment());
    }

//    private Bill createRefundBill() {
//        RefundBill rb = new RefundBill();
//
//        Date bd = Calendar.getInstance().getTime();
//        rb.setBillDate(bd);
//        rb.setBillTime(bd);
//        rb.setCreatedAt(bd);
//        rb.setBillType(getBillSession().getBill().getBillType());
//        rb.setBilledBill(getBillSession().getBill());
//        rb.setCreater(getSessionController().getLoggedUser());
//        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());
//
//        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
//        rb.setDepartment(getSessionController().getDepartment());
//
//        rb.setFromInstitution(getBillSession().getBill().getFromInstitution());
//        rb.setStaff(getBillSession().getBill().getStaff());
//
//        rb.setPaymentMethod(getBillSession().getBill().getPaymentMethod());
//
//        double total = getRefundedTotalFee();
//        rb.setNetTotal(-(total - getAgentPay().getRepayment().getFeeValue()));
//        rb.setTotal(-total);
//        rb.setPatient(getBillSession().getBill().getPatient());
//
//        getBillFacade().create(rb);
//
//        if (rb.getPaymentMethod() == PaymentMethod.Agent) {
//            double amt = getRefundedTotalFee() - getAgentPay().getRepayment().getFeeValue();
//            addBallance(rb, amt);
//        }
//
//        return rb;
//
//    }
    public void onTabChange(TabChangeEvent event) {
        //System.out.println("event : " + event.getTab().getId());
        setPatientTabId(event.getTab().getId());
    }

    public ChannelBillController() {
    }

    public ServiceSession getSs() {
        if (getbookingController().getSelectedServiceSession() != null) {
            return getServiceSessionFacade().findFirstBySQL("Select s From ServiceSession s where s.retired=false and s.id=" + getbookingController().getSelectedServiceSession().getId());
        } else {
            return new ServiceSession();
        }
    }

    public void updateBallance(Institution institution, double transactionValue, HistoryType historyType, Bill bill, BillItem billItem, BillSession billSession) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setBeforeBallance(institution.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        getCurrent().getFromInstitution().setBallance(getCurrent().getFromInstitution().getBallance() + transactionValue);
        getInstitutionFacade().edit(getCurrent().getFromInstitution());
    }

    public double getAmount() {
        amount = 0.0;
        if (!foriegn) {
            amount = getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFee();
        } else {
            amount = getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee();
        }
        return amount;
    }

//    private List<Fee> getServiceSessionFee() {
//        List<Fee> tmp = new ArrayList<Fee>();
//
//        String sql = "select f From Fee f where f.retired=false and f.serviceSession.id=" + getbookingController().getSelectedServiceSession().getId();
//        if (getbookingController().getSelectedServiceSession() != null) {
//            tmp = getFeeFacade().findBySQL(sql);
//        }
//
//        return tmp;
//    }
//    private void saveBilledFee(Bill b, BillItem bi) {
//
//        for (Fee f : getServiceSessionFee()) {
//            if (f.getFee() == 0.0 && f.getFfee() == 0.0) {
//                continue;
//            }
//
//            if (getCurrent().getFromInstitution() == null && f.getFeeType() == FeeType.OtherInstitution) {
//                continue;
//            }
//
//            BillFee bf = new BillFee();
//
//            bf.setCreatedAt(Calendar.getInstance().getTime());
//            bf.setCreater(getSessionController().getLoggedUser());
//
//            if (bf.getPatienEncounter() != null) {
//                bf.setPatienEncounter(b.getPatientEncounter());
//            }
//
//            bf.setPatient(b.getPatient());
//            bf.setBill(b);
//            bf.setBillItem(bi);
//            bf.setFee(f);
//
//            if (f.getFeeType() == FeeType.Staff) {
//                bf.setStaff(f.getStaff());
//            }
//
//            if (f.getFeeType() == FeeType.OtherInstitution) {
//                bf.setInstitution(getCurrent().getFromInstitution());
//            }
//
//            if (foriegn) {
//                bf.setFeeValue(f.getFfee());
//            } else {
//                bf.setFeeValue(f.getFee());
//            }
//
//            getBillFeeFacade().create(bf);
//        }
//    }
    public void makeNull() {
        amount = 0.0;
        foriegn = false;
        billFee = null;
        refundBillFee = null;
        newPatient = null;
        searchPatient = null;
        current = null;
        agentRefNo = "";
        billSession = null;
        patientTabId = "tabNewPt";
        billFee = null;
        refundBillFee = null;
        billItems = null;
    }

    private boolean errorCheck() {
        if (patientTabId.equals("tabNewPt")) {
            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("")) {
                UtilityController.addErrorMessage("Can not bill without Patient ");
                return true;
            }
        }

        if (patientTabId.equals("tabSearchPt")) {
            if (getSearchPatient() == null) {
                UtilityController.addErrorMessage("Please select Patient");
                return true;
            }
        }

        if (getCurrent().getPaymentMethod() == null) {
            UtilityController.addErrorMessage("Please select Paymentmethod");
            return true;
        }

        if (getCurrent().getPaymentMethod() == PaymentMethod.Agent) {
            if (getCurrent().getFromInstitution() == null) {
                UtilityController.addErrorMessage("Please select Agency");
                return true;
            }

            if (getCurrent().getFromInstitution().getBallance() - amount < 0 - getCurrent().getFromInstitution().getAllowedCredit()) {
                UtilityController.addErrorMessage("Agency Ballance is Not Enough");
                return true;
            }

        }

        if (getCurrent().getFromInstitution() != null) {
            if (getAgentRefNo().trim().isEmpty()) {
                UtilityController.addErrorMessage("Please Enter Agent Ref No");
                return true;
            }
        }

        if (getSs().getMaxNo() != 0.0 && getbookingController().getSelectedServiceSession().getDisplayCount() >= getSs().getMaxNo()) {
            UtilityController.addErrorMessage("No Space to Book");
            return true;
        }

        return false;
    }

    private void savePatient() {
        getPersonFacade().create(getNewPatient().getPerson());
        getPatientFacade().create(getNewPatient());
    }

//    private BillItem saveBilledItem(Bill b) {
//        BillItem bi = new BillItem();
//        bi.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
//        bi.setCreater(getSessionController().getLoggedUser());
//        bi.setBill(b);
//        bi.setAgentRefNo(agentRefNo);
//        bi.setNetValue(amount);
//        bi.setSessionDate(getbookingController().getSelectedServiceSession().getSessionAt());
//        getBillItemFacade().create(bi);
//
//        return bi;
//    }
//    private void saveBillSession(Bill b, BillItem bi) {
//        BillSession bs = new BillSession();
//        bs.setBill(b);
//        bs.setBillItem(bi);
//        bs.setCreatedAt(Calendar.getInstance().getTime());
//        bs.setCreater(getSessionController().getLoggedUser());
//        bs.setServiceSession(getbookingController().getSelectedServiceSession());
//        bs.setSessionDate(getbookingController().getSelectedServiceSession().getSessionAt());
//        bs.setSessionTime(getChannelBean().calSessionTime(getbookingController().getSelectedServiceSession()));
//
//        int count = getServiceSessionBean().getSessionNumber(getbookingController().getSelectedServiceSession(), getbookingController().getSelectedServiceSession().getSessionAt());
//        bs.setSerialNo(count);
//
//        getBillSessionFacade().create(bs);
//
//    }
    public void add() {
        if (errorCheck()) {
            return;
        }

        savePatient();

        current = saveBilledBill();

        current = getBillFacade().find(current.getId());

        UtilityController.addSuccessMessage("Channel Booking Added.");

    }

    private BillSession createBillSession(Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        bs.setDepartment(getbookingController().getSelectedServiceSession().getOriginatingSession().getDepartment());
        bs.setInstitution(getbookingController().getSelectedServiceSession().getOriginatingSession().getInstitution());
        bs.setItem(getbookingController().getSelectedServiceSession().getOriginatingSession());
//        bs.setPresent(true);

        System.out.println("getbookingController().getSelectedServiceSession().getOriginatingSession() = " + getbookingController().getSelectedServiceSession().getOriginatingSession());

        bs.setServiceSession(getbookingController().getSelectedServiceSession().getOriginatingSession());
        bs.setSessionDate(getbookingController().getSelectedServiceSession().getSessionDate());
        bs.setSessionTime(getbookingController().getSelectedServiceSession().getSessionTime());
        bs.setStaff(getbookingController().getSelectedServiceSession().getStaff());

        int count = getServiceSessionBean().getSessionNumber(getbookingController().getSelectedServiceSession().getOriginatingSession(), getbookingController().getSelectedServiceSession().getSessionAt());
        System.err.println("count" + count);
        bs.setSerialNo(count);

        getBillSessionFacade().create(bs);

        return bs;
    }

    private BillItem createBillItem(Bill bill) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agentRefNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setGrossValue(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setItem(getbookingController().getSelectedServiceSession().getOriginatingSession());
        bi.setNetRate(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setNetValue(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setQty(1.0);
        bi.setRate(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setSessionDate(getbookingController().getSelectedServiceSession().getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }

    private List<BillFee> createBillFee(Bill bill, BillItem billItem) {
        List<BillFee> billFeeList = new ArrayList<>();

        for (ItemFee f : getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees()) {
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());
            bf.setDepartment(getSessionController().getDepartment());
            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setFeeDiscount(0.0);
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());
            bf.setSpeciality(f.getSpeciality());
            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(bill.getPatientEncounter());
            }

            bf.setPatient(bill.getPatient());

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
            }

            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(getCurrent().getFromInstitution());
            }

            if (foriegn) {
                bf.setFeeValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
            }

            if (getCurrent().getPaymentMethod().equals(PaymentMethod.Credit)) {
                bf.setPaidValue(0.0);
            } else {
                bf.setPaidValue(bf.getFeeValue());
            }

            billFeeFacade.create(bf);
            billFeeList.add(bf);
            System.out.println("billFees = " + billFeeList);
        }

        return billFeeList;

    }

    private Bill createBill() {
        Bill bill = new BilledBill();
        bill.setBookingId(getBillNumberBean().bookingIdGenerator(sessionController.getInstitution()));
        bill.setStaff(getbookingController().getSelectedServiceSession().getOriginatingSession().getStaff());
        bill.setAppointmentAt(getbookingController().getSelectedServiceSession().getSessionAt());
        bill.setTotal(getAmount());
        bill.setNetTotal(getAmount());

        if (getPatientTabId().equals("tabNewPt")) {
            bill.setPatient(newPatient);
        } else {
            bill.setPatient(searchPatient);
        }

        if (getCurrent().getPaymentMethod().equals(PaymentMethod.OnCall)
                || getCurrent().getPaymentMethod().equals(PaymentMethod.Credit)) {
            bill.setBillType(BillType.ChannelCredit);
        } else if (getCurrent().getPaymentMethod().equals(PaymentMethod.Agent)) {
            bill.setBillType(BillType.ChannelAgent);
        } else if (getCurrent().getPaymentMethod().equals(PaymentMethod.Cash)) {
            bill.setBillType(BillType.ChannelPaid);
        }

        bill.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bill.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bill.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bill.setCreater(getSessionController().getLoggedUser());
        bill.setDepartment(getSessionController().getDepartment());
        bill.setInstitution(sessionController.getInstitution());
        getBillFacade().create(bill);

        return bill;
    }

    @Inject
    BillBeanController billBeanController;

    private Bill saveBilledBill() {
        Bill savingBill = createBill();
        BillItem savingBillItem = createBillItem(savingBill);
        BillSession savingBillSession = createBillSession(savingBill, savingBillItem);
        List<BillFee> savingBillFees = createBillFee(savingBill, savingBillItem);
        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItem);

        getAmount();

        getBillItemFacade().edit(savingBillItem);

        //Update Bill Session
        savingBillItem.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItem));
        savingBillItem.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItem));
        savingBillItem.setBillSession(savingBillSession);
        getBillSessionFacade().edit(savingBillSession);

        //Update Bill
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);
        getBillFacade().edit(savingBill);

        if (savingBill.getPaymentMethod() == PaymentMethod.Agent) {
            updateBallance(savingBill.getInstitution(), savingBill.getNetTotal(), HistoryType.ChannelBooking, savingBill, savingBillItem, savingBillSession);
        }

        return savingBill;
    }

    public List<BillFee> getBillFee() {

        billFee = new ArrayList<>();
        if (billSession != null) {
            String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + billSession.getBill().getId();
            billFee = getBillFeeFacade().findBySQL(sql);
        }

        return billFee;
    }

    public List<BillFee> getRefundBillFee() {
        if (refundBillFee == null) {
            refundBillFee = new ArrayList<BillFee>();
            if (billSession != null) {
                //String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + billSession.getBill().getId();
                String sql = "Select s From BillFee s where s.retired=false and s.bill.billedBill.id=" + billSession.getBill().getId();
                refundBillFee = getBillFeeFacade().findBySQL(sql);
            }
        }
        return refundBillFee;
    }

    public void setBillFee(List<BillFee> billFee) {
        this.billFee = billFee;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillSession getBillSession() {
        return billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
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

    public void setRefundBillFee(List<BillFee> refundBillFee) {
        this.refundBillFee = refundBillFee;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
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

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public String getPatientTabId() {
        //System.out.println("Getting Tab Id : " + patientTabId);
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
        //System.out.println("Setting Tab Id : " + patientTabId);
    }

    public boolean isForiegn() {
        return foriegn;
    }

    public void setForiegn(boolean foriegn) {
        this.foriegn = foriegn;
    }

    public BookingController getbookingController() {
        return bookingController;
    }

    public void setbookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public BillNumberController getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberController billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getAgentRefNo() {
        return agentRefNo;
    }

    public void setAgentRefNo(String agentRefNo) {
        this.agentRefNo = agentRefNo;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }
}

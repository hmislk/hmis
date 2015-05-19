/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Area;
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
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
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
    private String patientTabId = "tabNewPt";
    private Patient newPatient;
    private Area area;
    private Patient searchPatient;
    private String agentRefNo;
    private String settleAgentRefNo;
    private double amount;
    private boolean foriegn = false;
    PaymentMethod paymentMethod;
    PaymentMethod settlePaymentMethod;
    PaymentMethodData paymentMethodData;
    Institution institution;
    Institution settleInstitution;
    Bill printingBill;
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
    @EJB
    private BillNumberGenerator billNumberBean;
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

    private BillSession savePaidBillSession(Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.copy(getBillSession());
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bs.setCreater(getSessionController().getLoggedUser());

        getBillSessionFacade().create(bs);
        return bs;

    }

    public void settleCredit() {
        if (errorCheckForSettle()) {
            return;
        }

        Bill b = savePaidBill();

        BillItem bi = savePaidBillItem(b);
        savePaidBillFee(b, bi);
        BillSession bs = savePaidBillSession(b, bi);
        getBillSession().setPaidBillSession(bs);
        getBillSessionFacade().edit(bs);
        System.out.println("bs = " + bs);
        System.out.println("getBillSession().getPaidBillSession() = " + getBillSession().getPaidBillSession());

        getBillSession().getBill().setPaidAmount(b.getPaidAmount());
        getBillSession().getBill().setBalance(0.0);
        getBillSession().getBill().setPaidBill(b);
        getBillFacade().edit(getBillSession().getBill());

        b.setSingleBillItem(bi);
        b.setSingleBillSession(bs);
        getBillFacade().edit(b);

//        editBillSession(b, bi);
        UtilityController.addSuccessMessage("Channel Booking Added");        

    }

//
//    private void deductBallance() {
//        double tmp = getBilledTotalFee() - getAgentPay().getBilledFee().getFeeValue();
//        getBillSession().getBill().getFromInstitution().setBallance(getBillSession().getBill().getFromInstitution().getBallance() - tmp);
//        getInstitutionFacade().edit(getBillSession().getBill().getFromInstitution());
//    }
    private Bill savePaidBill() {
        Bill temp = new BilledBill();
        temp.copy(getBillSession().getBill());
        temp.copyValue(getBillSession().getBill());
        temp.setPaidAmount(getBillSession().getBill().getNetTotal());
        temp.setBalance(0.0);
        temp.setPaymentMethod(settlePaymentMethod);
        temp.setReferenceBill(getBillSession().getBill());
        temp.setInsId(getBillSession().getBill().getInsId());
        temp.setBookingId(billNumberBean.bookingIdGenerator(sessionController.getInstitution(), temp));
        temp.setBillType(BillType.ChannelPaid);
        temp.setDepartment(getSessionController().getDepartment());
        temp.setInstitution(getSessionController().getInstitution());

        temp.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(temp);

        return temp;
    }

    private BillItem savePaidBillItem(Bill b) {
        BillItem bi = new BillItem();
        bi.copy(billSession.getBillItem());
        bi.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setBill(b);
        getBillItemFacade().create(bi);

        return bi;
    }

    private void savePaidBillFee(Bill b, BillItem bi) {

        for (BillFee f : billSession.getBill().getBillFees()) {

            BillFee bf = new BillFee();
            bf.copy(f);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(getSessionController().getLoggedUser());
            bf.setBill(b);
            bf.setBillItem(bi);
            getBillFeeFacade().create(bf);
        }
    }

    private void editBillSession(Bill b, BillItem bi) {
//        getBillSession().setBill(b);
//        getBillSession().setBillItem(bi);
//
//        getBillSessionFacade().edit(getBillSession());
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

        if (settlePaymentMethod == PaymentMethod.Agent && settleInstitution == null) {
            UtilityController.addErrorMessage("Please select Agency");
            return true;
        }

        return false;
    }

    private boolean errorCheckRefunding() {
//        if (getBillSession().getBill().getBillType().getParent() == BillType.ChannelCreditFlow) {
//            UtilityController.addSuccessMessage("Credit Bill Cant be Refunded");
//            return true;
//        }

//        if (getDoctorFee().getBilledFee().getFeeValue() < getDoctorFee().getRepayment().getFeeValue()
//                || getHospitalFee().getBilledFee().getFeeValue() < getHospitalFee().getRepayment().getFeeValue()
//                || getTax().getBilledFee().getFeeValue() < getTax().getRepayment().getFeeValue()
//                || getAgentPay().getBilledFee().getFeeValue() < getAgentPay().getRepayment().getFeeValue()) {
//            UtilityController.addSuccessMessage("You can't refund mor than paid fee");
//            return true;
//        }
        return false;
    }

    public void refundCashFlowBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }

        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
    }

    public void refundCreditPaidBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            UtilityController.addErrorMessage("No Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }

        refund(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession().getPaidBillSession());
        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        

    }

    public void refund(Bill bill, BillItem billItem, List<BillFee> billFees, BillSession billSession) {
        calRefundTotal();

        RefundBill rb = (RefundBill) createRefundBill(bill);
        BillItem rBilItm = refundBillItems(billItem, rb);
        createReturnBillFee(billFees, rb, rBilItm);
        BillSession rSession = refundBillSession(billSession, rb, rBilItm);

        billSession.setReferenceBillSession(rSession);
        billSessionFacade.edit(billSession);

        bill.setRefunded(true);
        bill.setRefundedBill(rb);
        getBillFacade().edit(getBillSession().getBill());

    }
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

    public void cancelCashFlowBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }

        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());

    }

    public void cancelCreditPaidBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill() == null) {
            UtilityController.addErrorMessage("No Paid BillSession");
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            UtilityController.addErrorMessage("No Paid Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill() == null) {
            UtilityController.addErrorMessage("No Paid Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        System.out.println("getBillSession().getPaidBillSession().getBill() = " + getBillSession().getPaidBillSession().getBill());
        System.out.println("getBillSession().getPaidBillSession().getBillItem() = " + getBillSession().getPaidBillSession().getBill());
        System.out.println("getBillSession().getPaidBillSession() = " + getBillSession().getPaidBillSession().getBill());

        System.out.println("getBillSession().getBill() = " + getBillSession().getPaidBillSession().getBill());
        System.out.println("getBillSession().getBillItem() = " + getBillSession().getPaidBillSession().getBill());
        System.out.println("getBillSession() = " + getBillSession().getPaidBillSession().getBill());

        cancel(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getPaidBillSession());
        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());

    }

    public void cancel(Bill bill, BillItem billItem, BillSession billSession) {
        if (errorCheckCancelling()) {
            return;
        }

        CancelledBill cb = createCancelBill(bill);

        BillItem cItem = cancelBillItems(billItem, cb);
        BillSession cbs = cancelBillSession(billSession, cb, cItem);

        bill.setCancelled(true);
        bill.setCancelledBill(cb);
        getBillFacade().edit(bill);

        //Update BillSession        
        billSession.setReferenceBillSession(cbs);
        billSessionFacade.edit(billSession);

//        if (getBillSession().getBill().getReferenceBill() != null) {
//            getBillSession().setBill(getBillSession().getBill().getReferenceBill());
//            getBillSessionFacade().edit(getBillSession());
//        }
        if (cb.getPaymentMethod() == PaymentMethod.Agent) {
            updateBallance(cb.getFromInstitution(),
                    0 - cb.getNetTotal(),
                    HistoryType.ChannelBookingCancel,
                    cb, cItem, cbs, cItem.getAgentRefNo());
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
    private BillItem cancelBillItems(BillItem bi, CancelledBill can) {

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

        return b;
    }

    private BillSession cancelBillSession(BillSession billSession, CancelledBill can, BillItem canBillItem) {
        BillSession bs = new BillSession();
        bs.copy(billSession);
        bs.setBill(can);
        bs.setBillItem(canBillItem);
        bs.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bs.setCreater(getSessionController().getLoggedUser());
        getBillSessionFacade().create(bs);

        can.setSingleBillSession(bs);
        getBillFacade().edit(can);

        return bs;
    }

    private BillSession refundBillSession(BillSession billSession, Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.copy(billSession);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        bs.setCreater(getSessionController().getLoggedUser());
        getBillSessionFacade().create(bs);

        bill.setSingleBillSession(bs);
        getBillFacade().edit(bill);

        return bs;
    }

    private CancelledBill createCancelBill(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);
        
        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);
        getBillFacade().create(cb);

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
    public BillItem refundBillItems(BillItem bi, RefundBill rb) {
        BillItem rbi = new BillItem();
        rbi.copy(bi);
        rbi.resetValue();
        rbi.setBill(rb);
        rbi.setCreatedAt(Calendar.getInstance().getTime());
        rbi.setCreater(getSessionController().getLoggedUser());
        rbi.setItem(bi.getItem());
        rbi.setQty(0 - 1.0);
        rbi.setGrossValue(0 - getRefundableTotal());
        rbi.setNetValue(0 - getRefundableTotal());
        rbi.setReferanceBillItem(bi);
        getBillItemFacade().create(rbi);

        bi.setRefunded(Boolean.TRUE);
        getBillItemFacade().edit(bi);

        return rbi;

    }

    private void createReturnBillFee(List<BillFee> billFees, Bill b, BillItem bt) {
        for (BillFee bf : billFees) {
            System.err.println("Change Val " + bf.getTmpChangedValue());
            if (bf.getTmpChangedValue() != null && bf.getTmpChangedValue() != 0) {
                BillFee newBf = new BillFee();
                newBf.copy(bf);
                newBf.setFeeGrossValue(0 - bf.getTmpChangedValue());
                newBf.setFeeValue(0 - bf.getTmpChangedValue());
                newBf.setBill(b);
                newBf.setBillItem(bt);
                newBf.setCreatedAt(new Date());
                newBf.setCreater(sessionController.getLoggedUser());
                billFeeFacade.create(newBf);

                if (bf.getFee().getFeeType() == FeeType.Staff) {
                    bt.setStaffFee(0 - bf.getTmpChangedValue());
                }

                if (bf.getFee().getFeeType() == FeeType.OwnInstitution) {
                    bt.setHospitalFee(0 - bf.getTmpChangedValue());
                }

            }
        }

        billItemFacade.edit(bt);
    }

    double refundableTotal = 0;

    public double getRefundableTotal() {
        return refundableTotal;
    }

    public void setRefundableTotal(double refundableTotal) {
        this.refundableTotal = refundableTotal;
    }

    public void calRefundTotal() {
        refundableTotal = 0;
        for (BillFee bf : billSession.getBill().getBillFees()) {
            if (bf.getTmpChangedValue() != null) {
                refundableTotal += bf.getTmpChangedValue();
            }
        }
    }

    private Bill createRefundBill(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getInstitution());

        rb.setNetTotal(0 - getRefundableTotal());
        rb.setTotal(0 - getRefundableTotal());
        rb.setPaidAmount(0 - getRefundableTotal());
        
        String insId = generateBillNumberInsId(rb);
        
        if (insId.equals("")) {
            return null;
        }
        rb.setInsId(insId);

        getBillFacade().create(rb);

//Need To Update Agent BAllance
        return rb;

    }

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

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill, BillItem billItem, BillSession billSession, String refNo) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setBill(bill);
        agentHistory.setBillItem(billItem);
        agentHistory.setBillSession(billSession);
        agentHistory.setBeforeBallance(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setReferenceNo(refNo);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);
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
        printingBill = null;
        agentRefNo = "";
        billSession = null;
        patientTabId = "tabNewPt";
        billFee = null;
        refundBillFee = null;
        billItems = null;
        paymentMethod = null;
        institution = null;
        refundableTotal = 0;
    }

    @Inject
    AgentReferenceBookController agentReferenceBookController;

    private boolean errorCheck() {
        if (getbookingController().getSelectedServiceSession().getOriginatingSession() == null) {
            UtilityController.addErrorMessage("Please Select Session");
            return true;
        }

//        if (patientTabId.equals("tabNewPt")) {
//            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("")) {
//                UtilityController.addErrorMessage("Can not bill without Patient ");
//                return true;
//            }
//            if (area == null){
//                UtilityController.addErrorMessage("Select Area");
//                return true;
//            }
//        }
        if (patientTabId.equals("tabSearchPt")) {
            if (getSearchPatient() == null) {
                UtilityController.addErrorMessage("Please select Patient");
                return true;
            }
        }

        if (paymentMethod == null) {
            UtilityController.addErrorMessage("Please select Paymentmethod");
            return true;
        }

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                UtilityController.addErrorMessage("Please select Agency");
                return true;
            }

//            if (institution.getBallance() - amount < 0 - institution.getAllowedCredit()) {
//                UtilityController.addErrorMessage("Agency Ballance is Not Enough");
//                return true;
//            }
        }
        //System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (institution != null) {
            if (getAgentRefNo().trim().isEmpty() && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                UtilityController.addErrorMessage("Please Enter Agent Ref No");
                return true;
            }
        }
        //System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (getSs().getMaxNo() != 0.0 && getbookingController().getSelectedServiceSession().getDisplayCount() >= getSs().getMaxNo()) {
            UtilityController.addErrorMessage("No Space to Book");
            return true;
        }
        //System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (getAgentReferenceBookController().checkAgentReferenceNumber(institution, getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
            UtilityController.addErrorMessage("This Reference Number is Blocked Or This channel Book is Not Issued.");
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

        printingBill = saveBilledBill();

        printingBill = getBillFacade().find(printingBill.getId());

        UtilityController.addSuccessMessage("Channel Booking Added.");

    }

    public void addOnCall() {
        if (errorCheck()) {
            return;
        }

        if (printingBill == null) {
            printingBill = new Bill();
        }

        printingBill.setPaymentMethod(PaymentMethod.OnCall);
        add();

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

        int count = getServiceSessionBean().getSessionNumber(getbookingController().getSelectedServiceSession().getOriginatingSession(), getbookingController().getSelectedServiceSession().getSessionAt(), bs);
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
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(institution);
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
                bf.setStaff(f.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setFeeDiscount(0.0);
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());

            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(bill.getPatientEncounter());
            }

            bf.setPatient(bill.getPatient());

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
            }

            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(sessionController.getInstitution());
            }

            if (foriegn) {
                bf.setFeeValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
            }

//            if (paymentMethod.equals(PaymentMethod.Credit)) {
//                bf.setPaidValue(0.0);
//            } else {
//                bf.setPaidValue(bf.getFeeValue());
//            }
            billFeeFacade.create(bf);
            billFeeList.add(bf);
            System.out.println("billFees = " + billFeeList);
        }
        if (paymentMethod != PaymentMethod.Agent) {
            changeAgentFeeToHospitalFee();
        }

        return billFeeList;

    }

    public void changeAgentFeeToHospitalFee() {
        List<ItemFee> itemFees = getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees();
        double agentFee = 0.0;
        double agentFfee = 0.0;
        for (ItemFee ifl : itemFees) {
            if (ifl.getFeeType() == FeeType.OtherInstitution) {
                agentFee = ifl.getFee();
                System.out.println("agentFee = " + agentFee);
                agentFfee = ifl.getFfee();
                System.out.println("agentFfee = " + agentFfee);

                ifl.setFee(0.0);
                ifl.setFfee(0.0);
            }
        }
        for (ItemFee ifl : itemFees) {
            if (ifl.getFeeType() == FeeType.OwnInstitution) {

                System.out.println("1.agentFee = " + agentFee);
                System.out.println("1.agentFfee = " + agentFfee);
                agentFee += ifl.getFee();
                agentFfee += ifl.getFfee();
                System.out.println("2.agentFee = " + agentFee);
                System.out.println("2.agentFfee = " + agentFfee);

                ifl.setFee(agentFee);
                ifl.setFfee(agentFfee);
            }
        }
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
    }

    private Bill createBill() {
        Bill bill = new BilledBill();
        bill.setStaff(getbookingController().getSelectedServiceSession().getOriginatingSession().getStaff());
        bill.setAppointmentAt(getbookingController().getSelectedServiceSession().getSessionAt());
        bill.setTotal(getAmount());
        bill.setNetTotal(getAmount());
        bill.setPaymentMethod(paymentMethod);

        if (getPatientTabId().equals("tabNewPt")) {
            bill.setPatient(newPatient);
        } else {
            bill.setPatient(searchPatient);
        }

        switch (paymentMethod) {
            case OnCall:
                bill.setBillType(BillType.ChannelOnCall);
                break;
            case Cash:
                bill.setBillType(BillType.ChannelCash);
                break;

            case Card:
                bill.setBillType(BillType.ChannelCash);
                break;

            case Cheque:
                bill.setBillType(BillType.ChannelCash);
                break;

            case Slip:
                bill.setBillType(BillType.ChannelCash);
                break;
            case Agent:
                bill.setBillType(BillType.ChannelAgent);
                break;
            case Staff:
                bill.setBillType(BillType.ChannelStaff);
                break;
            case Credit:
                bill.setBillType(BillType.ChannelCredit);
                break;
        }

//        bill.setInsId(getBillNumberBean().institutionChannelBillNumberGenerator(sessionController.getInstitution(), bill));
        String insId = generateBillNumberInsId(bill);
        
        if (insId.equals("")) {
            return null;
        }
        bill.setInsId(insId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
            bill.setBookingId(getBillNumberBean().bookingIdGenerator(sessionController.getInstitution(), new BilledBill()));
            bill.setPaidAmount(getAmount());
            bill.setPaidAt(new Date());
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

        if (savingBill.getBillType() == BillType.ChannelAgent) {
            updateBallance(savingBill.getInstitution(), 0 - savingBill.getNetTotal(), HistoryType.ChannelBooking, savingBill, savingBillItem, savingBillSession, savingBillItem.getAgentRefNo());
            savingBill.setBalance(0.0);
        } else if (savingBill.getBillType() == BillType.ChannelCash) {
            savingBill.setBalance(0.0);
        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
            savingBill.setBalance(savingBill.getNetTotal());
        }

        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);

        getBillFacade().edit(savingBill);

        return savingBill;
    }

    private String generateBillNumberInsId(Bill bill) {
        String suffix = "";
        BillClassType billClassType = null;
        if (bill instanceof BilledBill) {
            suffix = "CHANN";
            billClassType=BillClassType.BilledBill;
        }

        if (bill instanceof CancelledBill) {
            suffix = "CHANNCAN";
            billClassType=BillClassType.CancelledBill;
        }

        if (bill instanceof RefundBill) {
            suffix = "CHANNREF";
            billClassType=BillClassType.RefundBill;
        }
        
        System.out.println("billClassType = " + billClassType);
        
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);

        String insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType,suffix);
        System.out.println("insId = " + insId);
        return insId;
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

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
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

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSettleAgentRefNo() {
        return settleAgentRefNo;
    }

    public void setSettleAgentRefNo(String settleAgentRefNo) {
        this.settleAgentRefNo = settleAgentRefNo;
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

    public PaymentMethod getSettlePaymentMethod() {
        return settlePaymentMethod;
    }

    public void setSettlePaymentMethod(PaymentMethod settlePaymentMethod) {
        this.settlePaymentMethod = settlePaymentMethod;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Institution getSettleInstitution() {
        return settleInstitution;
    }

    public void setSettleInstitution(Institution settleInstitution) {
        this.settleInstitution = settleInstitution;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public AgentReferenceBookController getAgentReferenceBookController() {
        return agentReferenceBookController;
    }

    public void setAgentReferenceBookController(AgentReferenceBookController agentReferenceBookController) {
        this.agentReferenceBookController = agentReferenceBookController;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

}

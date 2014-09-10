/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.ChannelFee;
import com.divudi.ejb.BillNumberController;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Fee;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Person;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.channel.AgentsFees;
import com.divudi.facade.AgentsFeesFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.inject.Inject;
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
    private ChannelFee doctorFee;
    private ChannelFee hospitalFee;
    private ChannelFee tax;
    private ChannelFee agentPay;
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
    private AgentsFeesFacade agentsFeesFacade;
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

    private double getBilledTotalFee() {
        return getDoctorFee().getBilledFee().getFeeValue()
                + getHospitalFee().getBilledFee().getFeeValue()
                + getTax().getBilledFee().getFeeValue()
                + getAgentPay().getBilledFee().getFeeValue();
    }

    private double getRefundedTotalFee() {
        return getDoctorFee().getRepayment().getFeeValue()
                + getHospitalFee().getRepayment().getFeeValue()
                + getTax().getRepayment().getFeeValue()
                + getAgentPay().getRepayment().getFeeValue();
    }

    private void deductBallance() {
        double tmp = getBilledTotalFee() - getAgentPay().getBilledFee().getFeeValue();
        getBillSession().getBill().getFromInstitution().setBallance(getBillSession().getBill().getFromInstitution().getBallance() - tmp);
        getInstitutionFacade().edit(getBillSession().getBill().getFromInstitution());
    }

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

        double amt = getBilledTotalFee();
        temp.setTotal(amt);
        temp.setNetTotal(amt - getAgentPay().getBilledFee().getFeeValue());

        if (getBillSession().getBill().getFromInstitution() != null) {
            deductBallance();
        }

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

        double amt = getBilledTotalFee() - getAgentPay().getBilledFee().getFeeValue();
        bi.setNetValue(amt);

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

        if (getDoctorFee().getBilledFee().getFeeValue() < getDoctorFee().getRepayment().getFeeValue()
                || getHospitalFee().getBilledFee().getFeeValue() < getHospitalFee().getRepayment().getFeeValue()
                || getTax().getBilledFee().getFeeValue() < getTax().getRepayment().getFeeValue()
                || getAgentPay().getBilledFee().getFeeValue() < getAgentPay().getRepayment().getFeeValue()) {
            UtilityController.addSuccessMessage("You can't refund mor than paid fee");
            return true;
        }

        return false;
    }

    public void refund() {
        if (errorCheckRefunding()) {
            return;
        }

        RefundBill rb = (RefundBill) createRefundBill();

        refundBillItems(rb);

        getBillSession().getBill().setRefunded(true);
        getBillSession().getBill().setRefundedBill(rb);
        getBillFacade().edit(getBillSession().getBill());

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

    private void cancelBillItems(CancelledBill can) {
        BillItem bi = getBillSession().getBillItem();

        BillItem b = new BillItem();
        b.setBill(can);

        b.setNetValue(-bi.getNetValue());
        b.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        b.setCreater(getSessionController().getLoggedUser());

        getBillItemFacade().create(b);

        cancelBillFee(can, b);
    }

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

        if (cb.getPaymentMethod() == PaymentMethod.Agent) {
            double amt = getBilledTotalFee() - getAgentPay().getBilledFee().getFeeValue();
            addBallance(cb, amt);
        }

        return cb;
    }

    private void addBallance(Bill cb, double amt) {
        cb.getFromInstitution().setBallance(cb.getFromInstitution().getBallance() + amt);
        getInstitutionFacade().edit(cb.getFromInstitution());
    }

    private void cancelBillFee(Bill b, BillItem bt) {
        if (getDoctorFee().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getDoctorFee(), getDoctorFee().getBilledFee().getFeeValue());
        }

        if (getHospitalFee().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getHospitalFee(), getHospitalFee().getBilledFee().getFeeValue());
        }

        if (getTax().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getTax(), getTax().getBilledFee().getFeeValue());
        }

        if (getAgentPay().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getAgentPay(), getAgentPay().getBilledFee().getFeeValue());
        }

    }

    public void refundBillItems(RefundBill rb) {
        BillItem bi = getBillSession().getBillItem();

        BillItem rbi = new BillItem();
        rbi.setBill(rb);
        rbi.setCreatedAt(Calendar.getInstance().getTime());
        rbi.setCreater(getSessionController().getLoggedUser());
        rbi.setItem(bi.getItem());

        double netValue = getRefundedTotalFee();

        rbi.setNetValue(-netValue);
        rbi.setRefunded(Boolean.TRUE);
        rbi.setReferanceBillItem(bi);
        getBillItemFacade().create(rbi);

        bi.setRefunded(Boolean.TRUE);
        getBillItemFacade().edit(bi);

        returnBillFee(rb, rbi);

    }

    private void returnBillFee(Bill b, BillItem bt) {
        if (getDoctorFee().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getDoctorFee(), getDoctorFee().getRepayment().getFeeValue());
        }

        if (getHospitalFee().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getHospitalFee(), getHospitalFee().getRepayment().getFeeValue());
        }

        if (getTax().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getTax(), getTax().getRepayment().getFeeValue());
        }

        if (getAgentPay().getRepayment().getFeeValue() != 0.0) {
            createBillFee(b, bt, getAgentPay(), getAgentPay().getRepayment().getFeeValue());
        }
    }

    private void createBillFee(Bill b, BillItem bt, ChannelFee cf, double feeValue) {

        cf.getRepayment().setFee(cf.getBilledFee().getFee());
        cf.getRepayment().setPatient(cf.getBilledFee().getPatient());
        cf.getRepayment().setDepartment(cf.getBilledFee().getDepartment());
        cf.getRepayment().setInstitution(cf.getBilledFee().getInstitution());
        cf.getRepayment().setSpeciality(cf.getBilledFee().getSpeciality());
        cf.getRepayment().setStaff(cf.getBilledFee().getStaff());

        cf.getRepayment().setBill(b);
        cf.getRepayment().setBillItem(bt);
        cf.getRepayment().setFeeValue(-feeValue);

        cf.getRepayment().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        cf.getRepayment().setCreater(getSessionController().getLoggedUser());

        getBillFeeFacade().create(cf.getRepayment());

    }

    private Bill createRefundBill() {
        RefundBill rb = new RefundBill();

        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBillType(getBillSession().getBill().getBillType());
        rb.setBilledBill(getBillSession().getBill());
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());

        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDepartment(getSessionController().getDepartment());

        rb.setFromInstitution(getBillSession().getBill().getFromInstitution());
        rb.setStaff(getBillSession().getBill().getStaff());

        rb.setPaymentMethod(getBillSession().getBill().getPaymentMethod());

        double total = getRefundedTotalFee();
        rb.setNetTotal(-(total - getAgentPay().getRepayment().getFeeValue()));
        rb.setTotal(-total);
        rb.setPatient(getBillSession().getBill().getPatient());

        getBillFacade().create(rb);

        if (rb.getPaymentMethod() == PaymentMethod.Agent) {
            double amt = getRefundedTotalFee() - getAgentPay().getRepayment().getFeeValue();
            addBallance(rb, amt);
        }

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

    private void updateBallance() {
        AgentsFees agentFee = getAgentFee();
        double tmp = 0.0;
        if (agentFee.getFee() != null) {
            if (!foriegn) {
                tmp = getSs().getHospitalFee() + getSs().getProfessionalFee();
            } else {
                tmp = getSs().getHospitalFfee() + getSs().getProfessionalFfee();
            }
        }

        getCurrent().getFromInstitution().setBallance(getCurrent().getFromInstitution().getBallance() - tmp);
        getInstitutionFacade().edit(getCurrent().getFromInstitution());
    }

    private AgentsFees createAgentFee() {
        AgentsFees agentFee = new AgentsFees();
        agentFee.setFee(new Fee());

        return agentFee;

    }

    private AgentsFees getAgentFee() {
        AgentsFees agentFee = createAgentFee();

        if (getbookingController().getSelectedServiceSession() != null) {
            if (getCurrent().getFromInstitution() != null) {
                agentFee = getAgentsFeesFacade().findFirstBySQL("Select i From AgentsFees i where "
                        + "i.serviceSession.id=" + getbookingController().getSelectedServiceSession().getId()
                        + " and i.agent.id=" + getCurrent().getFromInstitution().getId());

                if (agentFee == null) {
                    agentFee = getAgentsFeesFacade().findFirstBySQL("Select i From AgentsFees i where "
                            + "i.agent.id=" + getCurrent().getFromInstitution().getId());
                }
            }
        }

        if (agentFee == null) {
            agentFee = createAgentFee();
        }

        return agentFee;
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
        doctorFee = null;
        hospitalFee = null;
        agentPay = null;
        tax = null;
        newPatient = null;
        searchPatient = null;
        current = null;
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

            if (getCurrent().getFromInstitution().getBallance() - amount < 0-getCurrent().getFromInstitution().getAllowedCredit()) {
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

    private Bill saveBilledBill() {
        Bill savingBill = getCurrent();

        savingBill.setBookingId(getBillNumberBean().bookingIdGenerator());
        System.out.println("getbookingController() = " + getbookingController());
        System.out.println("getbookingController().getSelectedServiceSession() = " + getbookingController().getSelectedServiceSession());
        System.out.println("getbookingController().getSelectedServiceSession().getOriginatingSession() = " + getbookingController().getSelectedServiceSession().getOriginatingSession());
        System.out.println("getbookingController().getSelectedServiceSession().getOriginatingSession().getStaff() = " + getbookingController().getSelectedServiceSession().getOriginatingSession().getStaff());
        System.out.println("getbookingController().getSelectedServiceSession().getSessionDate() = " + getbookingController().getSelectedServiceSession().getSessionDate());
        savingBill.setStaff(getbookingController().getSelectedServiceSession().getOriginatingSession().getStaff());

        List<BillItem> billItems = new ArrayList<>();
        List<BillFee> billFees = new ArrayList<>();

        BillItem bi = new BillItem();
        BillSession bs = new BillSession();

        getBillSessionFacade().create(bs);

        System.out.println("getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees() = " + getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees());

        for (ItemFee f : getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees()) {
            BillFee bf = new BillFee();
            bf.setBill(savingBill);
            bf.setBillItem(bi);
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());
            bf.setDepartment(f.getDepartment());
            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setFeeDiscount(0.0);
            bf.setOrderNo(0);
            bf.setPatient(savingBill.getPatient());
            bf.setSpeciality(f.getSpeciality());
            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(savingBill.getPatientEncounter());
            }

            bf.setPatient(savingBill.getPatient());

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

            billFees.add(bf);
            System.out.println("billFees = " + billFees);
        }

        bs.setAbsent(false);
        bs.setBill(savingBill);
        bs.setBillItem(bi);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        bs.setDepartment(getbookingController().getSelectedServiceSession().getOriginatingSession().getDepartment());
        bs.setInstitution(getbookingController().getSelectedServiceSession().getOriginatingSession().getInstitution());
        bs.setItem(getbookingController().getSelectedServiceSession().getOriginatingSession());
        bs.setPresent(true);

        System.out.println("getbookingController().getSelectedServiceSession().getOriginatingSession() = " + getbookingController().getSelectedServiceSession().getOriginatingSession());

        bs.setServiceSession(getbookingController().getSelectedServiceSession().getOriginatingSession());
        bs.setSessionDate(getbookingController().getSelectedServiceSession().getSessionDate());
        bs.setSessionTime(getbookingController().getSelectedServiceSession().getSessionTime());
        bs.setStaff(getbookingController().getSelectedServiceSession().getStaff());

        int count = getServiceSessionBean().getSessionNumber(getbookingController().getSelectedServiceSession().getOriginatingSession(), getbookingController().getSelectedServiceSession().getSessionAt());
        System.err.println("count" + count);
        bs.setSerialNo(count);
        

        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agentRefNo);
        bi.setBill(savingBill);
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

        billItems.add(bi);

        
        getAmount();

        
        if (getCurrent().getPaymentMethod().equals(PaymentMethod.Credit)) {
            savingBill.setBillType(BillType.ChannelCredit);
        } else {
            savingBill.setBillType(BillType.ChannelPaid);
            savingBill.setTotal(amount);
            savingBill.setNetTotal(amount);
        }

        
        if (getCurrent().getPaymentMethod().equals(PaymentMethod.Agent)) {
            updateBallance();
        }

        
        if (getPatientTabId().equals("tabNewPt")) {
            savingBill.setPatient(newPatient);
        } else {
            savingBill.setPatient(searchPatient);
        }

        
        savingBill.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        savingBill.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());

        savingBill.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        savingBill.setCreater(getSessionController().getLoggedUser());

        
        getBillFacade().create(savingBill);
        
        getBillItemFacade().create(bi);
        
        for (BillFee bf : billFees) {
            getBillFeeFacade().create(bf);
        }
        
        

        billItems.add(bi);
        savingBill.setBillItems(billItems);
        savingBill.setBillFees(billFees);
        bi.setBillSession(bs);

        
        getBillItemFacade().edit(bi);
        
        
        getBillSessionFacade().edit(bs);
//        System.err.println("L12");
//        getBillSessionFacade().edit(bs);
        
        getBillFacade().edit(savingBill);
        

        return savingBill;
    }

    public List<BillFee> getBillFee() {

        billFee = new ArrayList<BillFee>();
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

    public ChannelFee getDoctorFee() {
        if (doctorFee == null) {
            doctorFee = new ChannelFee();

            for (BillFee bf : getBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.Staff) {
                    doctorFee.setBilledFee(bf);
                }
            }

            for (BillFee bf : getRefundBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.Staff) {
                    doctorFee.setPrevFee(bf);
                }
            }

            doctorFee.setRepayment(new BillFee());

        }

        return doctorFee;
    }

    public void setDoctorFee(ChannelFee doctorFee) {
        this.doctorFee = doctorFee;
    }

    public ChannelFee getHospitalFee() {
        if (hospitalFee == null) {
            hospitalFee = new ChannelFee();
            for (BillFee bf : getBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.OwnInstitution) {
                    hospitalFee.setBilledFee(bf);
                }
            }

            for (BillFee bf : getRefundBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.OwnInstitution) {
                    hospitalFee.setPrevFee(bf);
                }
            }

            hospitalFee.setRepayment(new BillFee());
        }

        return hospitalFee;
    }

    public void setHospitalFee(ChannelFee hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public ChannelFee getTax() {
        if (tax == null) {
            tax = new ChannelFee();
            for (BillFee bf : getBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.Tax) {
                    tax.setBilledFee(bf);
                }
            }

            for (BillFee bf : getRefundBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.Tax) {
                    tax.setPrevFee(bf);
                }
            }

        }
        return tax;
    }

    public void setTax(ChannelFee tax) {
        this.tax = tax;
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

    public ChannelFee getAgentPay() {
        if (agentPay == null) {
            agentPay = new ChannelFee();
            for (BillFee bf : getBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.OtherInstitution) {
                    agentPay.setBilledFee(bf);
                }
            }

            for (BillFee bf : getRefundBillFee()) {
                if (bf.getFee().getFeeType() == FeeType.OtherInstitution) {
                    agentPay.setPrevFee(bf);
                }
            }

        }

        return agentPay;
    }

    public void setAgentPay(ChannelFee agentPay) {
        this.agentPay = agentPay;
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

    public AgentsFeesFacade getAgentsFeesFacade() {
        return agentsFeesFacade;
    }

    public void setAgentsFeesFacade(AgentsFeesFacade agentsFeesFacade) {
        this.agentsFeesFacade = agentsFeesFacade;
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

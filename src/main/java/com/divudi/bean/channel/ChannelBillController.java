/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.DoctorSpecialityController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.MessageType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.ejb.SmsManagerEjb;
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
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Sms;
import com.divudi.entity.Staff;
import com.divudi.entity.UserPreference;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.entity.membership.PaymentSchemeDiscount;
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
import com.divudi.facade.SmsFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Payment;
import com.divudi.facade.PaymentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    @EJB
    private PaymentFacade paymentFacade;

    private BillSession billSession;
    private String patientTabId = "tabNewPt";
    private Patient newPatient;
    private Area area;
    private Patient searchPatient;
    private String agentRefNo;
    private String settleAgentRefNo;
    private double amount;
    private boolean foriegn = false;
    boolean settleSucessFully = false;
    private boolean printPreview;

    private boolean printPreviewC;
    private boolean printPreviewR;

    PaymentMethod paymentMethod;
    PaymentMethod settlePaymentMethod;
    PaymentMethod cancelPaymentMethod;
    PaymentMethod refundPaymentMethod;
    PaymentMethodData paymentMethodData;
    Institution institution;
    Institution settleInstitution;
    private Institution creditCompany;
    Bill printingBill;
    Staff toStaff;
    String errorText;
    PaymentScheme paymentScheme;
    double creditLimit;
    boolean activeCreditLimitPannel = false;
    String comment;
    String commentR;
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
    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    //////////////////////////////////
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    private SmsFacade smsFacade;
    @EJB
    SmsManagerEjb smsManagerEjb;
    @EJB
    StaffBean staffBean;
    //////////////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private BookingController bookingController;
    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    DoctorSpecialityController doctorSpecialityController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    private BillBeanController billBean;
    //////////////////////////////
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private ChannelBean channelBean;
    @Inject
    BillBeanController billBeanController;
    List<BillItem> billItems;
    int patientSearchTab;
    private boolean disableRefund;

    private UserPreference pf;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public String navigateToSettleBooking() {
        printPreview = false;
        return "/channel/settle_channel_booking?faces-redirect=true";
    }

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
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());

        getBillSessionFacade().create(bs);
        return bs;

    }

    public void settleCreditWithCash() {
        settlePaymentMethod = PaymentMethod.Cash;
        settleCredit();
    }

    public void settleCreditWithCard() {
        settlePaymentMethod = PaymentMethod.Card;
        settleCredit();
    }

    public void settleCreditWithOnlinePayment() {
        settlePaymentMethod = PaymentMethod.OnlineSettlement;
        settleCredit();
    }

    public void settleCreditWithCredit() {
        settlePaymentMethod = PaymentMethod.Credit;
        settleCredit();
    }

    public void settleCredit() {
        if (errorCheckForSettle()) {
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Channel Credit Booking Settle Requires Additional Information")) {

            if (settlePaymentMethod == PaymentMethod.Card) {
                if (paymentMethodData.getCreditCard().getInstitution() == null) {
                    JsfUtil.addErrorMessage("Please Enter Bank Details");
                    return;
                }
                if (paymentMethodData.getCreditCard().getNo() == null || paymentMethodData.getCreditCard().getNo().isEmpty()) {
                    JsfUtil.addErrorMessage("Please Enter Reference No.");
                    return;
                }
            }
            if (settlePaymentMethod == PaymentMethod.Credit) {
                if (toStaff == null && creditCompany == null) {
                    JsfUtil.addErrorMessage("Please Select the Staff or Credit Company");
                    return;
                }
            }
            if (errorChecksettle()) {
                return;
            }

        }

        Bill b = savePaidBill();
        BillItem bi = savePaidBillItem(b);
        savePaidBillFee(b, bi);
        BillSession bs = savePaidBillSession(b, bi);

        getBillSession().setPaidBillSession(bs);
        getBillSessionFacade().edit(bs);
        getBillSessionFacade().edit(getBillSession());

        getBillSession().getBill().setPaidAmount(b.getPaidAmount());
        getBillSession().getBill().setBalance(0.0);
        getBillSession().getBill().setPaidBill(b);
        getBillFacade().edit(getBillSession().getBill());

        b.setSingleBillItem(bi);
        b.setSingleBillSession(bs);
        getBillFacade().edit(b);

        createPayment(b, settlePaymentMethod);

        if (toStaff != null && settlePaymentMethod == PaymentMethod.Staff_Welfare) {
            staffBean.updateStaffWelfare(toStaff, getBillSession().getBill().getTotal());
            JsfUtil.addSuccessMessage("User Credit Updated");
        } else if (toStaff != null && settlePaymentMethod == PaymentMethod.Staff) {
            staffBean.updateStaffCredit(toStaff, getBillSession().getBill().getTotal());
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
        }

        if (settlePaymentMethod == PaymentMethod.PatientDeposit) {
            if (getBillSession().getBill().getPatient().getRunningBalance() != null) {
                getBillSession().getBill().getPatient().setRunningBalance(getBillSession().getBill().getPatient().getRunningBalance() - getBillSession().getBill().getTotal());
            } else {
                getBillSession().getBill().getPatient().setRunningBalance(0.0 - getBillSession().getBill().getTotal());
            }
            getPatientFacade().edit(getBillSession().getBill().getPatient());
        }

        printPreview = true;
        creditCompany = null;
        toStaff = null;
        paymentMethodData = null;
        
        JsfUtil.addSuccessMessage("On Call Channel Booking Settled");
    }

    private boolean errorChecksettle() {
        if (getPaymentSchemeController().checkPaymentMethodError(settlePaymentMethod, getPaymentMethodData())) {
            return true;
        }

        if (settlePaymentMethod == PaymentMethod.PatientDeposit) {
            if (!getBillSession().getBill().getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(getBillSession().getBill().getPatient().getCreditLimit());
            double runningBalance;
            if (getBillSession().getBill().getPatient().getRunningBalance() != null) {
                runningBalance = getBillSession().getBill().getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (getBillSession().getBill().getTotal() > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

        if (settlePaymentMethod == PaymentMethod.Credit) {
            if (creditCompany == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company.");
                return true;
            }
        }

        if (settlePaymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }
            if (toStaff.getCurrentCreditValue() + getBillSession().getBill().getTotal() > toStaff.getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (settlePaymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + getBillSession().getBill().getTotal() > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (settlePaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
            }
            double differenceOfBillTotalAndPaymentValue = getBillSession().getBill().getTotal() - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
            if (getBillSession().getBill().getCashPaid() == 0.0) {
                getBillSession().getBill().setCashPaid(multiplePaymentMethodTotalValue);
            }

        }
        return false;
    }

    public BillSession settleCreditForOnlinePayments(BillSession bookingBillSession) {
        settlePaymentMethod = PaymentMethod.OnlineSettlement;
//        Bill b = savePaidBill();
        Bill newSettleBill = new BilledBill();
        newSettleBill.copy(bookingBillSession.getBill());
        newSettleBill.copyValue(bookingBillSession.getBill());
        newSettleBill.setPaidAmount(bookingBillSession.getBill().getNetTotal());
        newSettleBill.setBalance(0.0);
        newSettleBill.setPaymentMethod(settlePaymentMethod);
        newSettleBill.setReferenceBill(bookingBillSession.getBill());
        newSettleBill.setBillType(BillType.ChannelPaid);
        newSettleBill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT_ONLINE);
        newSettleBill.setDepartment(bookingBillSession.getSessionInstance().getDepartment());
        newSettleBill.setInstitution(bookingBillSession.getSessionInstance().getInstitution());
        String deptId = generateBillNumberDeptIdForPatientPortal(newSettleBill);
        newSettleBill.setInsId(deptId);
        newSettleBill.setDeptId(deptId);
        newSettleBill.setBookingId(deptId);
        newSettleBill.setBillDate(new Date());
        newSettleBill.setBillTime(new Date());
        newSettleBill.setCreatedAt(new Date());
//        newSettleBill.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(newSettleBill);

        BillItem newSettleBillItem = new BillItem();
        newSettleBillItem.copy(bookingBillSession.getBillItem());
        newSettleBillItem.setCreatedAt(new Date());
//        bi.setCreater(getSessionController().getLoggedUser());
        newSettleBillItem.setBill(newSettleBill);
        getBillItemFacade().create(newSettleBillItem);

        for (BillFee bookingBillFees : bookingBillSession.getBill().getBillFees()) {

            BillFee newlyCreatedSettlingBillFee = new BillFee();
            newlyCreatedSettlingBillFee.copy(bookingBillFees);
            newlyCreatedSettlingBillFee.setCreatedAt(Calendar.getInstance().getTime());
            newlyCreatedSettlingBillFee.setCreater(getSessionController().getLoggedUser());
            newlyCreatedSettlingBillFee.setBill(newSettleBill);
            newlyCreatedSettlingBillFee.setBillItem(newSettleBillItem);
            getBillFeeFacade().create(newlyCreatedSettlingBillFee);
        }

        BillSession newlyCreatedSettlingBillSession = new BillSession();
        newlyCreatedSettlingBillSession.copy(bookingBillSession);
        newlyCreatedSettlingBillSession.setBill(newSettleBill);
        newlyCreatedSettlingBillSession.setBillItem(newSettleBillItem);
        newlyCreatedSettlingBillSession.setCreatedAt(new Date());
        getBillSessionFacade().create(newlyCreatedSettlingBillSession);

        bookingBillSession.setPaidBillSession(newlyCreatedSettlingBillSession);
        getBillSessionFacade().edit(newlyCreatedSettlingBillSession);
        getBillSessionFacade().edit(bookingBillSession);

        bookingBillSession.getBill().setPaidAmount(newSettleBill.getPaidAmount());
        bookingBillSession.getBill().setBalance(0.0);
        bookingBillSession.getBill().setPaidBill(newSettleBill);
        getBillFacade().edit(bookingBillSession.getBill());

        newSettleBill.setSingleBillItem(newSettleBillItem);
        newSettleBill.setSingleBillSession(newlyCreatedSettlingBillSession);
        getBillFacade().edit(newSettleBill);

        createPaymentForOnlinePortal(newSettleBill, settlePaymentMethod);

        return newlyCreatedSettlingBillSession;

    }

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:

                    case Agent:
                    case Credit:
                        p.setInstitution(creditCompany);
                    case PatientDeposit:
                    case Slip:
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);
            p.setPaidValue(bill.getNetTotal());

            switch (pm) {
                case Card:
                    p.setBank(getPaymentMethodData().getCreditCard().getInstitution());
                    p.setCreditCardRefNo(getPaymentMethodData().getCreditCard().getNo());
                    break;
                case Cheque:
                    p.setChequeDate(getPaymentMethodData().getCheque().getDate());
                    p.setChequeRefNo(getPaymentMethodData().getCheque().getNo());
                    break;
                case Cash:
                    break;
                case ewallet:

                case Agent:
                case Credit:
                    p.setInstitution(creditCompany);
                    break;
                case PatientDeposit:
                case Slip:
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    public List<Payment> createPaymentForOnlinePortal(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();

        Payment p = new Payment();
        p.setBill(bill);
        p.setInstitution(bill.getInstitution());
        p.setDepartment(bill.getDepartment());
        p.setCreatedAt(new Date());
        p.setPaymentMethod(pm);
        p.setPaidValue(bill.getNetTotal());

        p.setPaidValue(p.getBill().getNetTotal());
        paymentFacade.create(p);

        ps.add(p);

        return ps;
    }

    private Bill savePaidBill() {
        Bill temp = new BilledBill();
        temp.copy(getBillSession().getBill());
        temp.copyValue(getBillSession().getBill());
        temp.setPaidAmount(getBillSession().getBill().getNetTotal());
        temp.setBalance(0.0);
        temp.setPaymentMethod(settlePaymentMethod);
        temp.setReferenceBill(getBillSession().getBill());
        temp.setBillType(BillType.ChannelPaid);
        temp.setBillTypeAtomic(BillTypeAtomic.CHANNEL_PAYMENT_FOR_BOOKING_BILL);
        String deptId = generateBillNumberDeptId(temp);
        temp.setInsId(deptId);
        temp.setDeptId(deptId);
        temp.setBookingId(deptId);
        temp.setDepartment(getSessionController().getDepartment());
        temp.setInstitution(getSessionController().getInstitution());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());

        getBillFacade().create(temp);

        return temp;
    }

    private BillItem savePaidBillItem(Bill b) {
        BillItem bi = new BillItem();
        bi.copy(billSession.getBillItem());
        bi.setCreatedAt(new Date());
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
        
        
        if (settlePaymentMethod == PaymentMethod.OnCall ) {
            JsfUtil.addErrorMessage("You Can't Settle On-Call Bill with OnCall Payment");
            return true;
        }

        if (getBillSession().getBill().getPaymentMethod() == PaymentMethod.Credit) {
            if (getBillSession().getBill().getFromInstitution() != null
                    && getBillSession().getBill().getFromInstitution().getBallance()
                    - getBillSession().getBill().getTotal() < -getBillSession().getBill().getFromInstitution().getAllowedCredit()) {
                JsfUtil.addErrorMessage("Agency Balance is Not Enough");
                return true;
            }
        }

        if (settlePaymentMethod == PaymentMethod.Agent && settleInstitution == null) {
            JsfUtil.addErrorMessage("Please select Agency");
            return true;
        }

        return false;
    }

    private boolean errorCheckRefunding() {
//        if (getBillSession().getBill().getBillType().getParent() == BillType.ChannelCreditFlow) {
//            JsfUtil.addSuccessMessage("Credit Bill Cant be Refunded");
//            return true;
//        }

//        if (getDoctorFee().getBilledFee().getFeeValue() < getDoctorFee().getRepayment().getFeeValue()
//                || getHospitalFee().getBilledFee().getFeeValue() < getHospitalFee().getRepayment().getFeeValue()
//                || getTax().getBilledFee().getFeeValue() < getTax().getRepayment().getFeeValue()
//                || getAgentPay().getBilledFee().getFeeValue() < getAgentPay().getRepayment().getFeeValue()) {
//            JsfUtil.addSuccessMessage("You can't refund mor than paid fee");
//            return true;
//        }
        return false;
    }

    public void channelBookingRefund() {
        if (refundableTotal > 0.0) {
            if (billSession.getBillItem().getBill().getPaymentMethod() == PaymentMethod.Agent) {
                refundAgentBill();
                return;
            }
            if (billSession.getBill().getBillType().getParent() == BillType.ChannelCashFlow && billSession.getBillItem().getBill().getPaymentMethod() != PaymentMethod.Agent) {
                refundCashFlowBill();
                return;
            }
            if (billSession.getBill().getBillType().getParent() == BillType.ChannelCreditFlow) {
                if (billSession.getBill().getPaidAmount() == 0) {
                    JsfUtil.addErrorMessage("Can't Refund. No Payments");
                } else {
                    refundCreditPaidBill();
                }
            }
        } else {
            JsfUtil.addErrorMessage("Nothing to Refund");
        }
    }

    public void refundCashFlowBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        commentR = null;
        printPreviewR = true;
    }

    public void refundAgentBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (refundPaymentMethod == null) {
            JsfUtil.addErrorMessage("Select Refund Payment Method");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());

        refundPaymentMethod = null;
        commentR = null;
        printPreviewR = true;
    }

    public void refundCreditPaidBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            JsfUtil.addErrorMessage("No Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        if (getBillSession().getBill().getBillFees() != null) {

            for (BillFee bf : getBillSession().getBill().getBillFees()) {

                if (bf.getTmpChangedValue() == null) {
                    continue;
                }

                switch (bf.getFee().getFeeType()) {
                    case OtherInstitution:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Agency Fee");
                            return;
                        }
                        break;
                    case Staff:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Doctor Fee");
                            return;
                        }
                        break;

                    case Service:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Scan Fee");
                            return;
                        }
                        break;

                    case OwnInstitution:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            JsfUtil.addErrorMessage("Enter Lesser Amount for Hospital Fee");
                            return;
                        }
                        break;

                    default:
                        JsfUtil.addErrorMessage("Enter Refund Amount");
                        break;

                }
            }

        }

        refund1(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession().getPaidBillSession());
        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        commentR = null;
        printPreviewR = true;
    }

    public void refund(Bill bill, BillItem billItem, List<BillFee> billFees, BillSession billSession) {
        calRefundTotal();

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        }

        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            RefundBill rb = (RefundBill) createCashRefundBill(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            Payment p = createPaymentForCancellationsAndRefunds(rb, refundPaymentMethod);
            System.out.println("p = " + p.getPaidValue());
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

        } else {
            RefundBill rb = (RefundBill) createRefundBill(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            Payment p = createPaymentForCancellationsAndRefunds(rb, bill.getPaidBill().getPaymentMethod());
            System.out.println("p = " + p.getPaidValue());
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

            RefundBill rpb = (RefundBill) createRefundBill(bill.getPaidBill());
            BillItem rpBilItm = refundBillItems(bill.getSingleBillItem(), rb);
            BillSession rpSession = refundBillSession(billSession.getPaidBillSession(), rpb, rpBilItm);

            billSession.getPaidBillSession().setReferenceBillSession(rpSession);
            billSessionFacade.edit(billSession.getPaidBillSession());

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.getPaidBill().setRefunded(true);
            bill.getPaidBill().setRefundedBill(rpb);
            getBillFacade().edit(bill.getPaidBill());
            JsfUtil.addSuccessMessage("Successfully Refunded");
        }

    }

    public void refund1(Bill bill, BillItem billItem, List<BillFee> billFees, BillSession billSession) {
        calRefundTotal();

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        }

        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            RefundBill rb = (RefundBill) createCashRefundBill1(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

        } else {
            RefundBill rb = (RefundBill) createRefundBill1(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

            RefundBill rpb = (RefundBill) createRefundBill1(bill.getPaidBill());
            BillItem rpBilItm = refundBillItems(bill.getSingleBillItem(), rb);
            BillSession rpSession = refundBillSession(billSession.getPaidBillSession(), rpb, rpBilItm);
           
            billSession.getPaidBillSession().setReferenceBillSession(rpSession);
            billSessionFacade.edit(billSession.getPaidBillSession());

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.getPaidBill().setRefunded(true);
            bill.getPaidBill().setRefundedBill(rpb);
            getBillFacade().edit(bill.getPaidBill());
            JsfUtil.addSuccessMessage("Successfully Refunded");
        }

    }

    List<BillFee> listBillFees;
    
      public Payment createPaymentForCancellationsAndRefunds(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        double valueToSet = 0 - Math.abs(bill.getNetTotal());
        System.out.println("valueToSet = " + valueToSet);
        p.setPaidValue(valueToSet);
        if(pm == null){
            pm = bill.getPaymentMethod();
        }
        setPaymentMethodData(p, pm);
        return p;
    }
      
     public void setPaymentMethodData(Payment p, PaymentMethod pm) {
        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        System.out.println("paid value Channeling bill refund = " + p.getPaidValue());
        if (p.getId() == null) {
            getPaymentFacade().create(p);
        }
        getPaymentFacade().edit(p);
    }

    public void createBillfees(SelectEvent event) {
        BillSession bs = ((BillSession) event.getObject());
        String sql;
        HashMap hm = new HashMap();
        sql = "Select bf From BillFee bf where bf.retired=false"
                + " and bf.billItem=:bt ";
        hm.put("bt", bs.getBillItem());

        listBillFees = billFeeFacade.findByJpql(sql, hm);
        billSession = bs;

        for (BillFee bf : billSession.getBill().getBillFees()) {
            if (bf.getFee().getFeeType() == FeeType.Staff && getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                bf.setTmpChangedValue(bf.getFeeValue());
            }
        }
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

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBillSession().getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    public void channelBookingCancel() {
        if (billSession.getBill().getBillType() == BillType.ChannelAgent) {
            cancelAgentPaidBill();
            return;
        }
        if (billSession.getBill().getBillType().getParent() == BillType.ChannelCashFlow && billSession.getBill().getBillType() != BillType.ChannelAgent) {
            cancelCashFlowBill();
            return;
        }
        if ((billSession.getBill().getBillType() == BillType.ChannelOnCall || billSession.getBill().getBillType() == BillType.ChannelStaff) && billSession.getBill().getPaidBill() == null) {
            cancelBookingBill();
            return;
        }
        if (billSession.getBill().getBillType().getParent() == BillType.ChannelCreditFlow && billSession.getBill().getBillType() != BillType.ChannelAgent) {
            if (billSession.getBill().getPaidAmount() == 0) {
                JsfUtil.addErrorMessage("Can't Cancel. No Payments");
            } else {
                cancelCreditPaidBill();
                return;
            }
        }
    }

    private boolean errorCheckCancelling() {
        if (getBillSession() == null) {
            return true;
        }

        if (getBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return true;
        }

        if (getBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment has paid");
            return true;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }
        return false;
    }

    public void cancelCashFlowBill() {
        if (errorCheckCancelling()) {
            return;
        }

        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        comment = null;
        printPreviewC = true;
    }

    public void cancelBookingBill() {
        if (errorCheckCancelling()) {
            return;
        }

        CancelledBill cb = createCancelBill1(getBillSession().getBill());
        BillItem cItem = cancelBillItems(getBillSession().getBillItem(), cb);
        BillSession cbs = cancelBillSession(getBillSession(), cb, cItem);
        getBillSession().getBill().setCancelled(true);
        getBillSession().getBill().setCancelledBill(cb);
        getBillFacade().edit(getBillSession().getBill());
        getBillSession().setReferenceBillSession(cbs);
        billSessionFacade.edit(billSession);

        comment = null;
        printPreviewC = true;
    }

    public void cancelAgentPaidBill() {
        if (getBillSession() == null) {
            JsfUtil.addErrorMessage("No BillSession");
            return;
        }

        if (getBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Bill To Cancel");
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            if (getBillSession().getBillItem().getBill().getBalance() == 0.0) {
                getBillSession().setPaidBillSession(getBillSession());
            } else {
                JsfUtil.addErrorMessage("No Paid. Can not cancel.");
                return;
            }
        }

        if (getBillSession().getPaidBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Paid Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }

        if (getCancelPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        //cancel(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getPaidBillSession());
        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        cancelPaymentMethod = null;
        comment = null;
        printPreviewC = true;
    }

    public void cancelCreditPaidBill() {
        if (getBillSession() == null) {
            JsfUtil.addErrorMessage("No BillSession");
            return;
        }
        if (getBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Bill To Cancel");
            return;
        }
        if (getBillSession().getPaidBillSession() == null) {
            JsfUtil.addErrorMessage("No Paid Paid Bill Session");
            return;
        }
        if (getBillSession().getPaidBillSession().getBill() == null) {
            JsfUtil.addErrorMessage("No Paid Paid Bill Session");
            return;
        }
        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return;
        }
        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }
        cancel1(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getPaidBillSession());
        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        comment = null;
        printPreviewC = true;

    }

    public void cancel(Bill bill, BillItem billItem, BillSession billSession) {
        if (errorCheckCancelling()) {
            return;
        }

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        } else if (bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
        }

        //dr. buddhika said
        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            CancelledBill cb = createCancelCashBill(bill);
            createPaymentForCancellationsAndRefunds(cb,cb.getPaymentMethod());
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(billSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

            //Update BillSession        
            billSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(billSession);

        } else {
            CancelledBill cb = createCancelBill(bill);
            createPaymentForCancellationsAndRefunds(cb,bill.getPaidBill().getPaymentMethod());
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(billSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);
            billSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(billSession);

            CancelledBill cpb = createCancelBill(bill.getPaidBill());
            BillItem cpItem = cancelBillItems(bill.getPaidBill().getSingleBillItem(), cb);
            BillSession cpbs = cancelBillSession(billSession.getPaidBillSession(), cpb, cpItem);
            bill.getPaidBill().setCancelled(true);
            bill.getPaidBill().setCancelledBill(cpb);
            getBillFacade().edit(bill.getPaidBill());
            billSession.getPaidBillSession().setReferenceBillSession(cpbs);
            billSessionFacade.edit(billSession.getPaidBillSession());
            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

        }

        JsfUtil.addSuccessMessage("Channel Cancelled");

    }

    public void cancel1(Bill bill, BillItem billItem, BillSession billSession) {
        if (errorCheckCancelling()) {
            return;
        }

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        } else if (bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
        }

        //dr. buddhika said
        if (bill.getPaidBill() == null) {
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            CancelledBill cb = createCancelCashBill1(bill);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(billSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

            //Update BillSession        
            billSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(billSession);

        } else {
            CancelledBill cb = createCancelBill1(bill);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(billSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);
            billSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(billSession);

            CancelledBill cpb = createCancelBill(bill.getPaidBill());
            BillItem cpItem = cancelBillItems(bill.getPaidBill().getSingleBillItem(), cb);
            BillSession cpbs = cancelBillSession(billSession.getPaidBillSession(), cpb, cpItem);
            bill.getPaidBill().setCancelled(true);
            bill.getPaidBill().setCancelledBill(cpb);
            getBillFacade().edit(bill.getPaidBill());
            billSession.getPaidBillSession().setReferenceBillSession(cpbs);
            billSessionFacade.edit(billSession.getPaidBillSession());
            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

        }

        JsfUtil.addSuccessMessage("Cancelled");

    }

//    private void cancelBillItemsOld(CancelledBill can) {
//        BillItem bi = getBillSession().getBillItem();
//
//        BillItem b = new BillItem();
//        b.setBill(can);
//
//        b.setNetValue(0 - bi.getNetValue());
//        b.setCreatedAt(new Date());
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
        b.setCreatedAt(new Date());
        b.setCreater(getSessionController().getLoggedUser());

        getBillItemFacade().create(b);
        String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
        cancelBillFee(can, b, tmp);

        return b;
    }

    private BillSession cancelBillSession(BillSession billSession, CancelledBill can, BillItem canBillItem) {
        BillSession bs = new BillSession();
        bs.copy(billSession);
        bs.setBill(can);
        bs.setBillItem(canBillItem);
        bs.setCreatedAt(new Date());
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
        bs.setCreatedAt(new Date());
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
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        cb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private CancelledBill createCancelCashBill(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        cb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private CancelledBill createCancelBill1(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private CancelledBill createCancelCashBill1(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(can);
            bf.setBillItem(bt);

            bf.setCreatedAt(new Date());
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
        double hf = 0.0;
        double sf = 0.0;
        for (BillFee bf : billFees) {
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
                    sf += bt.getStaffFee();
                }

                if (bf.getFee().getFeeType() == FeeType.OwnInstitution) {
                    bt.setHospitalFee(0 - bf.getTmpChangedValue());
                    hf += bt.getHospitalFee();
                }

            }
        }
        b.setHospitalFee(hf);
        b.setStaffFee(sf);
        billFacade.edit(b);

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

    public void checkRefundTotal() {
        refundableTotal = 0;
        for (BillFee bf : billSession.getBill().getBillFees()) {
            if (bf.getTmpChangedValue() != null) {
                if (bf.getTmpChangedValue() > bf.getFeeValue()) {
                    bf.setTmpChangedValue(bf.getFeeValue());
                }
            }
        }

        calRefundTotal();
    }

    private Bill createRefundBill(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
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

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);
        rb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    private Bill createCashRefundBill(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
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

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);
        rb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    private Bill createRefundBill1(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
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

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    private Bill createCashRefundBill1(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(comment);
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

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());
    }

    public ChannelBillController() {
    }

    public ServiceSession getSs() {
        if (getbookingController().getSelectedServiceSession() != null) {
            return getServiceSessionFacade().findFirstByJpql("Select s From ServiceSession s where s.retired=false and s.id=" + getbookingController().getSelectedServiceSession().getId());
        } else {
            return new ServiceSession();
        }
    }

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill, BillItem billItem, BillSession billSession, String refNo) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBill(bill);
        agentHistory.setBillItem(billItem);
        agentHistory.setBillSession(billSession);
        agentHistory.setBeforeBallance(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setReferenceNumber(refNo);
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
//            tmp = getFeeFacade().findByJpql(sql);
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
    public String startNewChannelBooking() {
        makeNull();
        printPreview = false;
        return bookingController.navigateBackToBookings();
    }

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
        patientSearchTab = 0;
        billFee = null;
        refundBillFee = null;
        billItems = null;
        paymentMethod = null;
        institution = null;
        refundableTotal = 0;
        toStaff = null;
        paymentScheme = null;
        doctorSpecialityController.setSelectText("");
        bookingController.setSelectTextSpeciality("");
        bookingController.setSelectTextConsultant("");
        bookingController.setSelectTextSession("");
        printPreview = false;
    }

    public void resetVariablesFromBooking() {
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
        patientSearchTab = 0;
        billFee = null;
        refundBillFee = null;
        billItems = null;
        paymentMethod = null;
        institution = null;
        refundableTotal = 0;
        toStaff = null;
        paymentScheme = null;
    }

    @Inject
    AgentReferenceBookController agentReferenceBookController;

    private boolean errorCheck() {
        if (getbookingController().getSelectedServiceSession() == null) {
            errorText = "Please Select Specility and Doctor.";
            JsfUtil.addErrorMessage("Please Select Specility and Doctor.");
            return true;
        }

        if (getbookingController().getSelectedServiceSession().isDeactivated()) {
            errorText = "******** Doctor Leave day Can't Channel ********";
            JsfUtil.addErrorMessage("Doctor Leave day Can't Channel.");
            return true;
        }

        if (getbookingController().getSelectedServiceSession().getOriginatingSession() == null) {
            errorText = "Please Select Session.";
            JsfUtil.addErrorMessage("Please Select Session");
            return true;
        }
        if (patientTabId.equals("tabNewPt")) {
            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("")) {
                errorText = "Can not bill without Patient.";
                JsfUtil.addErrorMessage("Can't Settle Without Patient.");
                return true;
            }
            if ((getNewPatient().getPerson().getPhone() == null || getNewPatient().getPerson().getPhone().trim().equals("")) && !getSessionController().getInstitutionPreference().isChannelSettleWithoutPatientPhoneNumber()) {
                errorText = "Can not bill without Patient Contact Number.";
                JsfUtil.addErrorMessage("Can't Settle Without Patient Contact Number.");
                return true;
            }
        }
        if (patientTabId.equals("tabSearchPt")) {
            if (getSearchPatient() == null) {
                errorText = "Please select Patient";
                JsfUtil.addErrorMessage("Please select Patient");
                return true;
            }
        }

        if (paymentMethod == null) {
            errorText = "Please select Paymentmethod";
            JsfUtil.addErrorMessage("Please select Paymentmethod");
            return true;
        }

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                errorText = "Please select Agency";
                JsfUtil.addErrorMessage("Please select Agency");
                return true;
            }

            if (institution.getBallance() - amount < 0 - institution.getAllowedCredit()) {
                errorText = "Agency Balance is Not Enough";
                JsfUtil.addErrorMessage("Agency Balance is Not Enough");
                return true;
            }
            if (getAgentReferenceBookController().checkAgentReferenceNumber(getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "Invaild Reference Number.";
                JsfUtil.addErrorMessage("Invaild Reference Number.");
                return true;
            }
            if (getAgentReferenceBookController().checkAgentReferenceNumberAlredyExsist(getAgentRefNo(), institution, BillType.ChannelAgent, PaymentMethod.Agent) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "This Reference Number( " + getAgentRefNo() + " ) is alredy Given.";
                JsfUtil.addErrorMessage("This Reference Number is alredy Given.");
                setAgentRefNo("");
                return true;
            }
            if (getAgentReferenceBookController().checkAgentReferenceNumber(institution, getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "This Reference Number is Blocked Or This channel Book is Not Issued.";
                JsfUtil.addErrorMessage("This Reference Number is Blocked Or This channel Book is Not Issued.");
                return true;
            }
        }
        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                errorText = "Please Select Staff.";
                JsfUtil.addErrorMessage("Please Select Staff.");
                return true;
            }
        }
        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (institution != null) {
            if (getAgentRefNo().trim().isEmpty() && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "Please Enter Agent Ref No";
                JsfUtil.addErrorMessage("Please Enter Agent Ref No.");
                return true;
            }
        }
        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (getSs().getMaxNo() != 0.0 && getbookingController().getSelectedServiceSession().getTransDisplayCountWithoutCancelRefund() >= getSs().getMaxNo()) {
            errorText = "No Space to Book.";
            JsfUtil.addErrorMessage("No Space to Book");
            return true;
        }

        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        return false;
    }

    private boolean errorCheckAgentValidate() {
        if (getbookingController().getSelectedServiceSession() == null) {
            errorText = "Please Select Specility and Doctor.";
            JsfUtil.addErrorMessage("Please Select Specility and Doctor.");
            return true;
        }

        if (getbookingController().getSelectedServiceSession().isDeactivated()) {
            errorText = "******** Doctor Leave day Can't Channel ********";
            JsfUtil.addErrorMessage("Doctor Leave day Can't Channel.");
            return true;
        }

        if (getbookingController().getSelectedServiceSession().getOriginatingSession() == null) {
            errorText = "Please Select Session.";
            JsfUtil.addErrorMessage("Please Select Session");
            return true;
        }

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                errorText = "Please select Agency";
                JsfUtil.addErrorMessage("Please select Agency");
                return true;
            }

            if (institution.getBallance() - amount < 0 - institution.getAllowedCredit()) {
                errorText = "Agency Balance is Not Enough";
                JsfUtil.addErrorMessage("Agency Balance is Not Enough");
                return true;
            }

        }

        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        return false;
    }

    private void savePatient() {
        switch (getPatientTabId()) {
            case "tabNewPt":
                getNewPatient().setCreater(getSessionController().getLoggedUser());
                getNewPatient().setCreatedAt(new Date());
                getNewPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getNewPatient().getPerson().setCreatedAt(new Date());
                getPersonFacade().create(getNewPatient().getPerson());
                getPatientFacade().create(getNewPatient());
                break;
            case "tabSearchPt":
                break;
        }
    }

//    private BillItem saveBilledItem(Bill b) {
//        BillItem bi = new BillItem();
//        bi.setCreatedAt(new Date());
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
        errorText = "";
        if (errorCheck()) {
            settleSucessFully = false;
            return;
        }
        savePatient();
        printingBill = saveBilledBill();
        printingBill = getBillFacade().find(printingBill.getId());
        bookingController.fillBillSessions();
        bookingController.generateSessions();
        sendSmsAfterBooking();
        settleSucessFully = true;
        printPreview = true;
        JsfUtil.addSuccessMessage("Channel Booking Added.");
    }

    public void sendSmsAfterBooking() {
        try {
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setBill(printingBill);
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(printingBill.getPatient().getPerson().getPhone());
            e.setSendingMessage(chanellBookingSms(printingBill));
            e.setDepartment(getSessionController().getLoggedUser().getDepartment());
            e.setInstitution(getSessionController().getLoggedUser().getInstitution());
            e.setSmsType(MessageType.ChannelBooking);
            getSmsFacade().create(e);
            boolean suc = smsManagerEjb.sendSms(e);
        } catch (Exception e) {
        }
    }

    private String chanellBookingSms(Bill b) {
        String s;
        String date = CommonController.getDateFormat(b.getSingleBillSession().getSessionDate(),
                "dd MMM");
        //System.out.println("date = " + date);
        String time = CommonController.getDateFormat(
                b.getSingleBillSession().getSessionTime(),
                "hh:mm a");
        //System.out.println("time = " + time);
        ServiceSession ss = null;
        if (b != null && b.getSingleBillSession() != null && b.getSingleBillSession().getServiceSession() != null
                && b.getSingleBillSession().getServiceSession().getOriginatingSession() != null) {
            ss = b.getSingleBillSession().getServiceSession().getOriginatingSession();
        }
//        if (b != null) {
//            System.out.println("b = " + b);
//            if (b.getSingleBillSession() != null) {
//                System.out.println("b.getSingleBillSession() = " + b.getSingleBillSession());
//                if(b.getSingleBillSession().getServiceSession()!=null){
//                    System.out.println("b.getSingleBillSession().getServiceSession() = " + b.getSingleBillSession().getServiceSession());
//                    if(b.getSingleBillSession().getServiceSession().getOriginatingSession()!=null){
//                        System.out.println("b.getSingleBillSession().getServiceSession().getOriginatingSession() = " + b.getSingleBillSession().getServiceSession().getOriginatingSession());
//                    }
//                }
//            }
//        }
        if (ss != null && ss.getStartingTime() != null) {
            time = CommonController.getDateFormat(
                    ss.getStartingTime(),
                    "hh:mm a");
        } else {
            //System.out.println("Null Error");
        }
        String doc = b.getSingleBillSession().getStaff().getPerson().getNameWithTitle();
        s = "Your Appointment with "
                + ""
                + doc
                + " @ Baddegama Medical Services - "
                + "No "
                + b.getSingleBillSession().getSerialNo()
                + " at "
                + time
                + " on "
                + date
                + ". 0912293700";

        return s;
    }

    private String chanellReminderSms(Bill b) {
        String s;
        String date = CommonController.getDateFormat(b.getSingleBillSession().getSessionDate(),
                "dd MMM");
        String time = CommonController.getDateFormat(
                b.getSingleBillSession().getSessionTime(),
                "hh:mm a");
        String doc = b.getSingleBillSession().getStaff().getPerson().getNameWithTitle();
        s = "Your Appointment with "
                + ""
                + doc
                + " @ Baddegama Medical Services - "
                + "No "
                + b.getSingleBillSession().getSerialNo()
                + " at "
                + time
                + " on "
                + date
                + ". 0912293700";

        return s;
    }

    public void loadUserPreferances() {
        UserPreference pf = null;
        //System.out.println("pf = " + pf);
        if (getSessionController().getApplicationPreference() != null) {
            pf = getSessionController().getApplicationPreference();
        } else if (getSessionController().getUserPreference() != null) {
            pf = getSessionController().getUserPreference();
        } else {
            pf = null;
        }
    }

    public void clearBillValues() {
        patientSearchTab = 0;
        paymentMethod = sessionController.getInstitutionPreference().getChannellingPaymentMethod();
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
        bs.setItem(getbookingController().getSelectedServiceSession());
//        bs.setPresent(true);
//        bs.setPresent(true);
//        bs.setItem(getbookingController().getSelectedServiceSession().getOriginatingSession());

        bs.setServiceSession(getbookingController().getSelectedServiceSession());
//        bs.setServiceSession(getbookingController().getSelectedServiceSession().getOriginatingSession());
        bs.setSessionDate(getbookingController().getSelectedServiceSession().getSessionDate());
        bs.setSessionTime(getbookingController().getSelectedServiceSession().getSessionTime());
        bs.setStaff(getbookingController().getSelectedServiceSession().getStaff());

        int count = getServiceSessionBean().getSessionNumber(getbookingController().getSelectedServiceSession(), getbookingController().getSelectedServiceSession().getSessionDate(), bs);
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
        bi.setItem(getbookingController().getSelectedServiceSession());
//        bi.setItem(getbookingController().getSelectedServiceSession().getOriginatingSession());
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
        double tmpTotal = 0;
        double tmpDiscount = 0;
        double tmpGrossTotal = 0.0;
        for (ItemFee f : getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees()) {
            if (paymentMethod != PaymentMethod.Agent) {
                if (f.getFeeType() == FeeType.OtherInstitution) {
                    continue;
                }
            }
            if (paymentMethod != PaymentMethod.OnCall) {
                if (f.getFeeType() == FeeType.OwnInstitution && f.getName().equalsIgnoreCase("On-Call Fee")) {
                    continue;
                }
            }
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

            PaymentSchemeDiscount paymentSchemeDiscount = priceMatrixController.fetchPaymentSchemeDiscount(paymentScheme, paymentMethod);

            double d = 0;
            if (foriegn) {
                bf.setFeeValue(f.getFfee());
                bf.setFeeGrossValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
                bf.setFeeGrossValue(f.getFee());
            }

            if (f.getFeeType() == FeeType.OwnInstitution && paymentSchemeDiscount != null) {
                d = bf.getFeeValue() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                bf.setFeeDiscount(d);
                bf.setFeeGrossValue(bf.getFeeGrossValue());
                bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                tmpDiscount += d;
            } else if (bill.getPatient().getPerson().getMembershipScheme() != null && f.getFeeType() == FeeType.OwnInstitution) {
//                MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(bill.getPatient());

                MembershipScheme membershipScheme = bill.getPatient().getPerson().getMembershipScheme();

                PriceMatrix priceMatrix = getPriceMatrixController().getChannellingDisCount(paymentMethod, membershipScheme, f.getDepartment());
//                priceMatrix.getDiscountPercent();
//                //System.out.println("priceMatrix.getDiscountPercent() = " + priceMatrix.getDiscountPercent());

                if (priceMatrix != null) {

                    d = bf.getFeeValue() * (priceMatrix.getDiscountPercent() / 100);
                    bf.setFeeDiscount(d);
                    bf.setFeeGrossValue(bf.getFeeGrossValue());
                    bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                    tmpDiscount += d;
                }
            }

            tmpGrossTotal += bf.getFeeGrossValue();
            tmpTotal += bf.getFeeValue();
            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }
        bill.setDiscount(tmpDiscount);
        bill.setNetTotal(tmpTotal);
        bill.setTotal(tmpGrossTotal);
        getBillFacade().edit(bill);

        billItem.setDiscount(tmpDiscount);
        billItem.setNetValue(tmpTotal);
        getBillItemFacade().edit(billItem);

//        if (paymentMethod != PaymentMethod.Agent) {
//            changeAgentFeeToHospitalFee();
//        }
        return billFeeList;

    }

    public void changeAgentFeeToHospitalFee() {
        List<ItemFee> itemFees = getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees();
        double agentFee = 0.0;
        double agentFfee = 0.0;
        for (ItemFee ifl : itemFees) {
            if (ifl.getFeeType() == FeeType.OtherInstitution) {
                agentFee = ifl.getFee();
                agentFfee = ifl.getFfee();

                ifl.setFee(0.0);
                ifl.setFfee(0.0);
            }
        }
        for (ItemFee ifl : itemFees) {
            if (ifl.getFeeType() == FeeType.OwnInstitution) {

                agentFee += ifl.getFee();
                agentFfee += ifl.getFfee();

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
        bill.setToStaff(toStaff);
        bill.setAppointmentAt(getbookingController().getSelectedServiceSession().getSessionDate());
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
                bill.setCreditCompany(institution);
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

        String deptId = generateBillNumberDeptId(bill);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
//            bill.setBookingId(getBillNumberBean().bookingIdGenerator(sessionController.getInstitution(), new BilledBill()));
            bill.setPaidAmount(getAmount());
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
        bill.setCreater(getSessionController().getLoggedUser());
        bill.setDepartment(getSessionController().getDepartment());
        bill.setInstitution(sessionController.getInstitution());
        if (getbookingController() != null) {

            if (getbookingController().getSelectedServiceSession() != null) {

                if (getbookingController().getSelectedServiceSession().getDepartment() != null) {
                }
            }
        }

        bill.setToDepartment(getbookingController().getSelectedServiceSession().getDepartment());
        bill.setToInstitution(getbookingController().getSelectedServiceSession().getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) {
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }

        return bill;
    }

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
            updateBallance(savingBill.getCreditCompany(), 0 - savingBill.getNetTotal(), HistoryType.ChannelBooking, savingBill, savingBillItem, savingBillSession, savingBillItem.getAgentRefNo());
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelCash) {
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
            savingBill.setBalance(savingBill.getNetTotal());
        } else if (savingBill.getBillType() == BillType.ChannelStaff) {
            savingBill.setBalance(savingBill.getNetTotal());
        }

        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBill;
    }

    private String generateBillNumberInsId(Bill bill) {
        String suffix = getSessionController().getInstitution().getInstitutionCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String insId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        return insId;
    }

    private String generateBillNumberDeptId(Bill bill) {
        String suffix = getSessionController().getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        return deptId;
    }

    private String generateBillNumberDeptIdForPatientPortal(Bill bill) {
        String suffix = bill.getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                deptId = getBillNumberBean().departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = getBillNumberBean().departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        return deptId;
    }

    public List<BillFee> getBillFee() {

        billFee = new ArrayList<>();
        if (billSession != null) {
            String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + billSession.getBill().getId();
            billFee = getBillFeeFacade().findByJpql(sql);
        }

        return billFee;
    }

    public List<BillFee> getRefundBillFee() {
        if (refundBillFee == null) {
            refundBillFee = new ArrayList<BillFee>();
            if (billSession != null) {
                //String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + billSession.getBill().getId();
                String sql = "Select s From BillFee s where s.retired=false and s.bill.billedBill.id=" + billSession.getBill().getId();
                refundBillFee = getBillFeeFacade().findByJpql(sql);
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
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public boolean isForiegn() {
        return foriegn;
    }

    public void setForiegn(boolean foriegn) {
        this.foriegn = foriegn;
    }

    public void validateAgentBalance() {
        fetchRecentChannelBooks(institution);
        activeCreditLimitPannel = false;

        if (errorCheckAgentValidate()) {
            activeCreditLimitPannel = true;
            return;
        }

        if (isForiegn()) {
            if (bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFfee() > (institution.getBallance() + institution.getAllowedCredit())) {
                JsfUtil.addErrorMessage("Please Increase Credit Limit or Balance");
                activeCreditLimitPannel = true;
                return;
            }
        }

        if (!isForiegn()) {
            if (bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFee() > (institution.getBallance() + institution.getAllowedCredit())) {
                JsfUtil.addErrorMessage("Please Increase Credit Limit or Balance");
                activeCreditLimitPannel = true;
                return;
            }
        }
        setAgentRefNo("");
    }

    public void fetchRecentChannelBooks(Institution ins) {
        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a "
                + " where a.retired=false "
                + " and a.institution=:ins"
                + " and a.deactivate=false "
                + " order by a.id desc ";

        m.put("ins", ins);

        List<AgentReferenceBook> agentReferenceBooks = agentReferenceBookFacade.findByJpql(sql, m, 5);
        if (agentReferenceBooks.size() > 0) {
            ins.setAgentReferenceBooks(agentReferenceBooks);
        }
    }

//    public void updateCreditLimit() {
//        if (institution == null) {
//            JsfUtil.addErrorMessage("Please Select a Agency");
//            return;
//        }
//
//        if (institution.getMaxCreditLimit() == 0.0) {
//            JsfUtil.addErrorMessage("Please Enter Maximum Credit Limit.");
//            return;
//        }
//
//        if (institution.getMaxCreditLimit() < creditLimit) {
//            JsfUtil.addErrorMessage("Please Enter less than Maximum Credit Limit");
//            return;
//        }
//
//        createAgentCreditLimitUpdateHistory(institution, creditLimit, HistoryType.AgentBalanceUpdateBill);
//        creditLimit = 0.0;
//    }
//
//    public void createAgentCreditLimitUpdateHistory(Institution ins, double transactionValue, HistoryType historyType) {
//        AgentHistory agentHistory = new AgentHistory();
//        agentHistory.setCreatedAt(new Date());
//        agentHistory.setCreater(getSessionController().getLoggedUser());
//        agentHistory.setBeforeBallance(ins.getBallance());
//        agentHistory.setTransactionValue(transactionValue);
//        agentHistory.setHistoryType(historyType);
//        agentHistory.setInstitution(institution);
//        agentHistoryFacade.create(agentHistory);
//    }
    public void changeListener() {
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFee(0.0);
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFfee(0.0);
        for (ItemFee f : bookingController.getSelectedServiceSession().getOriginatingSession().getItemFees()) {
            bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFee(bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFee() + f.getFee());
            bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFfee(bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFfee() + f.getFfee());
        }
        PaymentSchemeDiscount paymentSchemeDiscount = priceMatrixController.fetchPaymentSchemeDiscount(paymentScheme, paymentMethod);
        double d = 0;
        if (paymentSchemeDiscount != null) {
            for (ItemFee itmf : bookingController.getSelectedServiceSession().getOriginatingSession().getItemFees()) {
                if (itmf.getFeeType() == FeeType.OwnInstitution) {
                    if (foriegn) {
                        d += itmf.getFfee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    } else {
                        d += itmf.getFee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    }

                }
            }
        }
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFee(bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFee() - d);
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFfee(bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFfee() - d);
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
        if (paymentMethod == null) {
            paymentMethod = sessionController.getInstitutionPreference().getChannellingPaymentMethod();
        }
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

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public PaymentMethod getCancelPaymentMethod() {
        return cancelPaymentMethod;
    }

    public void setCancelPaymentMethod(PaymentMethod cancelPaymentMethod) {
        this.cancelPaymentMethod = cancelPaymentMethod;
    }

    public PaymentMethod getRefundPaymentMethod() {
        return refundPaymentMethod;
    }

    public void setRefundPaymentMethod(PaymentMethod refundPaymentMethod) {
        this.refundPaymentMethod = refundPaymentMethod;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public boolean isSettleSucessFully() {
        return settleSucessFully;
    }

    public void setSettleSucessFully(boolean settleSucessFully) {
        this.settleSucessFully = settleSucessFully;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(double creditLimit) {
        this.creditLimit = creditLimit;
    }

    public boolean isActiveCreditLimitPannel() {
        return activeCreditLimitPannel;
    }

    public void setActiveCreditLimitPannel(boolean activeCreditLimitPannel) {
        this.activeCreditLimitPannel = activeCreditLimitPannel;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCommentR() {
        return commentR;
    }

    public void setCommentR(String commentR) {
        this.commentR = commentR;
    }

    public UserPreference getPf() {
        if (pf == null) {
            loadUserPreferances();
        }
        return pf;
    }

    public void setPf(UserPreference pf) {
        this.pf = pf;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public boolean isDisableRefund() {
        if (billSession.getBill().getBillType().getParent() == BillType.ChannelCreditFlow) {
            if (billSession.getBill().getPaidAmount() == 0) {
                disableRefund = true;
            } else {
                disableRefund = false;
            }
        }
        return disableRefund;
    }

    public void setDisableRefund(boolean disableRefund) {
        this.disableRefund = disableRefund;
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

    public boolean isPrintPreviewC() {
        return printPreviewC;
    }

    public void setPrintPreviewC(boolean printPreviewC) {
        this.printPreviewC = printPreviewC;
    }


    public boolean isPrintPreviewR() {
        return printPreviewR;
    }

    public void setPrintPreviewR(boolean printPreviewR) {
        this.printPreviewR = printPreviewR;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

}

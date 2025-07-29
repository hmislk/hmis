package com.divudi.bean.channel;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.InstitutionController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.OnlineBookingStatus;
import com.divudi.core.data.PaymentMethod;
import static com.divudi.core.data.PaymentMethod.Agent;
import static com.divudi.core.data.PaymentMethod.Card;
import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.Cheque;
import static com.divudi.core.data.PaymentMethod.Credit;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.core.data.PaymentMethod.OnCall;
import static com.divudi.core.data.PaymentMethod.OnlineSettlement;
import static com.divudi.core.data.PaymentMethod.PatientDeposit;
import static com.divudi.core.data.PaymentMethod.Slip;
import static com.divudi.core.data.PaymentMethod.Staff;
import static com.divudi.core.data.PaymentMethod.YouOweMe;
import static com.divudi.core.data.PaymentMethod.ewallet;
import com.divudi.core.data.PaymentType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillNumber;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.OnlineBooking;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.OnlineBookingFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.ChannelService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Chinthaka Prasad
 */
@Named
@SessionScoped
public class OnlineBookingAgentController implements Serializable {

    private Institution current;
    private List<Institution> allAgents;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date toDate;

    private Institution institutionForBookings;
    private Institution agentForBookings;
    private Boolean paidStatus;
    private List<OnlineBooking> paidToHospitalList;
    private List<OnlineBookingStatus> onlineBookingStatusList;
    private boolean printOriginal;

    @EJB
    private InstitutionFacade institutionFacade;

    @Inject
    private SessionController sessionController;

    @EJB
    private ChannelService channelService;
    private List<OnlineBooking> onlineBookingList;
    private List<OnlineBooking> bookinsToAgenHospitalPayementCancellation;

    @EJB
    private OnlineBookingFacade onlineBookingFacade;
    @EJB
    private BillFacade billFacade;

    private PaymentMethod paidToHospitalPaymentMethod;
    private Bill paidToHospitalBill;
    private Bill paidToHospitalDirectFundBill;
    private PaymentMethodData paymentMethodData;
    private double paidToHospitalTotal;
    private OnlineBookingStatus onlineBookingStatus;
    private Bill printBill;
    private PaymentMethod cancelPaymentMethod;
    private Bill cancelBill;
    private String billStatus;
    private String comment;

    @EJB
    private PaymentFacade paymentFacade;

    @Inject
    private DrawerController drawerController;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Bill getPaidToHospitalDirectFundBill() {
        if (paidToHospitalDirectFundBill == null) {
            paidToHospitalDirectFundBill = new BilledBill();
            paidToHospitalDirectFundBill.setBillType(BillType.ChannelOnlineBookingAgentPaidToHospital);
            paidToHospitalDirectFundBill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_DIRECT_FUND_FOR_ONLINE_BOOKINGS_BILL);
        }
        return paidToHospitalDirectFundBill;
    }

    public void setPaidToHospitalDirectFundBill(Bill paidToHospitalDirectFundBill) {
        this.paidToHospitalDirectFundBill = paidToHospitalDirectFundBill;
    }

    public String getBillStatus() {
        return billStatus;
    }

    public void setBillStatus(String billStatus) {
        this.billStatus = billStatus;
    }

    public List<OnlineBooking> getBookinsToAgenHospitalPayementCancellation() {
        if (bookinsToAgenHospitalPayementCancellation == null) {
            bookinsToAgenHospitalPayementCancellation = new ArrayList<>();
        }
        return bookinsToAgenHospitalPayementCancellation;
    }

    public void setBookinsToAgenHospitalPayementCancellation(List<OnlineBooking> bookinsToAgenHospitalPayementCancellation) {
        this.bookinsToAgenHospitalPayementCancellation = bookinsToAgenHospitalPayementCancellation;
    }

    public PaymentMethod getCancelPaymentMethod() {
        return cancelPaymentMethod;
    }

    @Inject
    private InstitutionController institutionController;

    public List<Institution> getInstitutionForOnlineBookingManegement() {
        List<Institution> list = institutionController.getItems();

        list = list.stream().filter(ins -> ins.getInstitutionType() == InstitutionType.Company).collect(Collectors.toList());

        return list;
    }

    public Bill getCancelBill() {
        if (cancelBill == null) {
            cancelBill = new CancelledBill();
            cancelBill.setBillType(BillType.ChannelOnlineBookingAgentPaidToHospitalBillCancellation);
            cancelBill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_FOR_ONLINE_BOOKINGS_BILL_CANCELLATION);
        }
        return cancelBill;
    }
    
    private boolean errorCheck(Bill origianlBill) {
        if (origianlBill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (origianlBill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }
        if (paidToHospitalPaymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method.");
            return true;
        }
        if (paidToHospitalPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            JsfUtil.addErrorMessage("Multiple Payment Methods are NOT allowed.");
            return true;
        }
        if (paidToHospitalPaymentMethod.getPaymentType() == PaymentType.CREDIT) {
            JsfUtil.addErrorMessage("Credit Payment Methods are NOT allowed.");
            return true;
        }

        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }
    
    @EJB
    private DrawerFacade drawerFacade;
    
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    
    @Inject
    private BillBeanController billBeanController;
    @EJB
    private BillItemFacade billItemFacade;
    
    public void cancelAgentDirectFundBill(){
        if(paidToHospitalPaymentMethod == null){
            JsfUtil.addErrorMessage("Select Payment method to cancel");
            return;
        }
        
        if(printBill == null){
            JsfUtil.addErrorMessage("No bill to cancel");
            return;
        }
        
        if(printBill instanceof CancelledBill){
            JsfUtil.addErrorMessage("Cancelled Bill can't Cancel");
            return;
        }
        
        if(errorCheck(printBill)){
            return;
        }
        
        if (paidToHospitalPaymentMethod == PaymentMethod.Cash) {
            Drawer userDrawer = drawerFacade.find(sessionController.getLoggedUserDrawer().getId());
            if (userDrawer.getCashInHandValue() < printBill.getNetTotal()) {
                if (configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
                    JsfUtil.addErrorMessage("Drawer cash in hand value is not enough to cancel the bill");
                    return;
                }
            }
        }
        CancelledBill cancelBill = generateCancelBillForAgentDirectFundBill(printBill);
        
        printBill.setCancelled(true);
        printBill.setCancelledBill(cancelBill);
        getBillFacade().editAndCommit(printBill);
        
        List<Payment> payments = createPayment(printBill, paidToHospitalPaymentMethod, true);
        
        cancelBillItems(cancelBill, printBill);
        
        printBill = cancelBill;
        printOriginal = true;
        
        JsfUtil.addSuccessMessage("Bill Cancellation is successful");
 
    }
    
    private void cancelBillItems(CancelledBill cancelBill, Bill originalBill) {
        List<BillItem> bis = billBeanController.fetchBillItems(originalBill);
        if (bis == null) {
            return;
        }
        for (BillItem originalBillItem : bis) {
            BillItem newBillItemForCancelBill = new BillItem();
            newBillItemForCancelBill.setBill(cancelBill);
            newBillItemForCancelBill.copy(originalBillItem);
            newBillItemForCancelBill.invertValue();
//            newBillItemForCancelBill.setNetValue(-originalBillItem.getNetValue());
            newBillItemForCancelBill.setReferenceBill(originalBillItem.getReferenceBill());
            newBillItemForCancelBill.setCatId(originalBillItem.getCatId());
            newBillItemForCancelBill.setDeptId(originalBillItem.getDeptId());
            newBillItemForCancelBill.setInsId(originalBillItem.getInsId());
            newBillItemForCancelBill.setCreatedAt(new Date());
            newBillItemForCancelBill.setCreater(getSessionController().getLoggedUser());
            newBillItemForCancelBill.setReferanceBillItem(originalBillItem);
            billItemFacade.create(newBillItemForCancelBill);

        }
    }

    
    @EJB
    private BillNumberGenerator billNumberGenerator;
    
    public CancelledBill generateCancelBillForAgentDirectFundBill(Bill paidBill) {
        if (paidBill == null) {
            return null;
        }
        CancelledBill cb = new CancelledBill();
        cb.copy(paidBill);
        cb.invertAndAssignValuesFromOtherBill(paidBill);
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);
        cb.setDeptId(deptId);
        cb.setInsId(deptId);
        cb.setBillType(BillType.ChannelOnlineBookingAgentPaidToHospitalBillCancellation);
        cb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_DIRECT_FUND_FOR_ONLINE_BOOKINGS_BILL_CANCELLATION);
        cb.setBilledBill(printBill);
        cb.setPaymentMethod(paidToHospitalPaymentMethod);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(printBill.getInstitution());
        cb.setToInstitution(printBill.getToInstitution());
        cb.setFromInstitution(printBill.getFromInstitution());
        cb.setComments(comment);
        billFacade.create(cb);
        return cb;
        
    }

    public void setCancelBill(Bill cancelBill) {
        this.cancelBill = cancelBill;
    }

    public void setCancelPaymentMethod(PaymentMethod cancelPaymentMethod) {
        this.cancelPaymentMethod = cancelPaymentMethod;
    }

    public List<OnlineBookingStatus> getOnlineBookingStatusList() {
        if (onlineBookingStatusList == null || onlineBookingStatusList.isEmpty()) {
            onlineBookingStatusList = fetchAllOnlineBookingStatus();
        }
        return onlineBookingStatusList;
    }

    public void setOnlineBookingStatusList(List<OnlineBookingStatus> onlineBookingStatusList) {
        this.onlineBookingStatusList = onlineBookingStatusList;
    }

    public OnlineBookingStatus getOnlineBookingStatus() {
        return onlineBookingStatus;
    }

    public void setOnlineBookingStatus(OnlineBookingStatus onlineBookingStatus) {
        this.onlineBookingStatus = onlineBookingStatus;
    }

    public double getPaidToHospitalTotal() {
        return paidToHospitalTotal;
    }

    public void setPaidToHospitalTotal(double paidToHospitalTotal) {
        this.paidToHospitalTotal = paidToHospitalTotal;
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

    public Bill getPaidToHospitalBill() {
        if (paidToHospitalBill == null) {
            paidToHospitalBill = new BilledBill();
            paidToHospitalBill.setBillType(BillType.ChannelOnlineBookingAgentPaidToHospital);
            paidToHospitalBill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_FOR_ONLINE_BOOKINGS_BILL);
        }
        return paidToHospitalBill;
    }

    public void setPaidToHospitalBill(Bill paidToHospitalBill) {
        this.paidToHospitalBill = paidToHospitalBill;
    }

    public PaymentMethod getPaidToHospitalPaymentMethod() {
        return paidToHospitalPaymentMethod;
    }

    public List fetchAllOnlineBookingStatus() {
        return Arrays.stream(OnlineBookingStatus.values())
                .filter(status -> status != OnlineBookingStatus.PENDING)
                .collect(Collectors.toList());
    }

    public void setPaidToHospitalPaymentMethod(PaymentMethod paidToHospitalPaymentMethod) {
        this.paidToHospitalPaymentMethod = paidToHospitalPaymentMethod;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public OnlineBookingFacade getOnlineBookingFacade() {
        return onlineBookingFacade;
    }

    public void setOnlineBookingFacade(OnlineBookingFacade onlineBookingFacade) {
        this.onlineBookingFacade = onlineBookingFacade;
    }

    public void setTotalForPaymentMethodData() {
        if (paidToHospitalPaymentMethod != null && paidToHospitalBill.getNetTotal() > 0) {
            switch (paidToHospitalPaymentMethod) {
                case Cash:
                    break;
                case Card:
                    getPaymentMethodData().getCreditCard().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                case Cheque:
                    getPaymentMethodData().getCheque().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                case Slip:
                    getPaymentMethodData().getSlip().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                case ewallet:
                    getPaymentMethodData().getEwallet().setTotalValue(paidToHospitalBill.getNetTotal());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported payment method: " + paidToHospitalPaymentMethod);
            }
        }
    }

    public void setTotalForPaymentMethodData(PaymentMethod paymentMethod, Bill bill) {
        if (paymentMethod != null && (bill != null && bill.getNetTotal() > 0)) {
            switch (paymentMethod) {
                case Cash:
                    break;
                case Card:
                    getPaymentMethodData().getCreditCard().setTotalValue(bill.getNetTotal());
                    break;
                case Cheque:
                    getPaymentMethodData().getCheque().setTotalValue(bill.getNetTotal());
                    break;
                case Slip:
                    getPaymentMethodData().getSlip().setTotalValue(bill.getNetTotal());
                    break;
                case ewallet:
                    getPaymentMethodData().getEwallet().setTotalValue(bill.getNetTotal());
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }

    public void fetchOnlineBookingsFromAgentPaidBill(Bill bill) {
        if (bill == null) {
            return;
        }

        String sql = "Select ob from OnlineBooking ob "
                + " where ob.paidToHospitalBill = :bill "
                + " and ob.retired = :ret "
                + " and ob.onlineBookingStatus <> :status";

        Map params = new HashMap();
        params.put("bill", bill);
        params.put("ret", false);
        params.put("status", OnlineBookingStatus.PENDING);

        List<OnlineBooking> bookings = getOnlineBookingFacade().findByJpql(sql, params, TemporalType.TIMESTAMP);

        if (bookings != null) {
            onlineBookingList = null;
            getOnlineBookingList().addAll(bookings);
            bookinsToAgenHospitalPayementCancellation = onlineBookingList;
        }
        prepareCancellationAgentPaidToHospitalBills();
    }

    private double calculateMultiplePaymentMethodTotal() {

        double total = 0;
        if (paymentMethodData != null) {

            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                switch (cd.getPaymentMethod()) {
                    case Card:
                        total += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                        break;

                    case Cash:
                        total += cd.getPaymentMethodData().getCash().getTotalValue();
                        break;

                    case Cheque:
                        total += cd.getPaymentMethodData().getCheque().getTotalValue();
                        break;

                    case Slip:
                        total += cd.getPaymentMethodData().getSlip().getTotalValue();
                        break;
                    default:
                        throw new AssertionError();
                }
            }
        }

        return total;
    }

    public List<Payment> createPayment(Bill bill, PaymentMethod pm, boolean isCancellation) {
        List<Payment> ps = new ArrayList<>();
        if (pm == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                double paidValue = 0;

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        paidValue = cd.getPaymentMethodData().getCreditCard().getTotalValue();
                        p.setPaidValue(isCancellation ? -paidValue : paidValue);
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        paidValue = cd.getPaymentMethodData().getCheque().getTotalValue();
                        p.setPaidValue(isCancellation ? -paidValue : paidValue);
                        break;
                    case Cash:
                        paidValue = cd.getPaymentMethodData().getCash().getTotalValue();
                        p.setPaidValue(isCancellation ? -paidValue : paidValue);
                        break;
                    case ewallet:

                    case Agent:
                    case Credit:
                    case PatientDeposit:
                    case Slip:
                        paidValue = cd.getPaymentMethodData().getSlip().getTotalValue();
                        p.setPaidValue(isCancellation ? -paidValue : paidValue);
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        break;
                    case OnCall:
                    case OnlineSettlement:
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

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    break;
                case Cash:
                    break;
                case ewallet:
                    break;
                case Credit:
                    break;
                case PatientDeposit:
                    break;
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    break;
                case OnCall:
                    break;
                case OnlineSettlement:
                    break;
                case Staff:
                    break;
                case YouOweMe:
                    break;
                case MultiplePaymentMethods:
                    break;
            }

            if (isCancellation) {
                p.setPaidValue(-Math.abs(p.getBill().getNetTotal()));
            } else {
                p.setPaidValue(Math.abs(p.getBill().getNetTotal()));
            }

            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    @Inject
    private BookingControllerViewScope bookingControllerViewScope;

    public BookingControllerViewScope getBookingControllerViewScope() {
        return bookingControllerViewScope;
    }

    public void setBookingControllerViewScope(BookingControllerViewScope bookingControllerViewScope) {
        this.bookingControllerViewScope = bookingControllerViewScope;
    }

    public Bill createHospitalPaymentBill(List<OnlineBooking> bookings) {
        Bill paidBill = getPaidToHospitalBill();
        paidBill.setCreatedAt(new Date());
        paidBill.setCreater(getSessionController().getLoggedUser());
        paidBill.setToInstitution(institutionForBookings);
        paidBill.setToDepartment(getSessionController().getDepartment());
        paidBill.setDepartment(getSessionController().getDepartment());
        paidBill.setFromInstitution(agentForBookings);
        paidBill.setCreditCompany(agentForBookings);
        paidBill.setTotal(createTotalAmountToPay());
        paidBill.setBalance(0d);
        paidBill.setBillDate(new Date());
        paidBill.setBillTime(new Date());
        paidBill.setPaymentMethod(paidToHospitalPaymentMethod);

        String billNo = bookingControllerViewScope.generateBillNumberInsId(paidBill);

        if (billNo != null && !billNo.isEmpty()) {
            paidBill.setDeptId(billNo);
        }

        if (paidBill.getId() == null) {
            getBillFacade().create(paidBill);
        } else {
            getBillFacade().edit(paidBill);
        }

        return paidBill;

    }

    public void paidToHospitalBillCancellation() {
        if (printBill == null) {
            return;
        }

        prepareCancellationAgentPaidToHospitalBills();

        if (bookinsToAgenHospitalPayementCancellation == null || bookinsToAgenHospitalPayementCancellation.isEmpty()) {
            return;
        }

        double totalToCancel = 0;
        for (OnlineBooking ob : bookinsToAgenHospitalPayementCancellation) {
            totalToCancel += ob.getAppoinmentTotalAmount();
        }

        if (getCancelBill().getNetTotal() != totalToCancel) {
            JsfUtil.addErrorMessage("Bill total and value is different.");
            return;
        }

        Bill cancelBill = createCancelBillForAgentPaidToHospitalCancellation(totalToCancel);

        if (cancelBill != null) {
            List<Payment> payments = createPayment(cancelBill, cancelPaymentMethod, true);
            drawerController.updateDrawerForOuts(payments);
        }

        Bill paidBill = getPrintBill();

        if (onlineBookingList.size() == bookinsToAgenHospitalPayementCancellation.size()) {
            paidBill.setCancelled(true);
            paidBill.setCancelledBill(cancelBill);
            getBillFacade().edit(paidBill);
        } else if (onlineBookingList.size() != bookinsToAgenHospitalPayementCancellation.size()) {
            paidBill.setRefunded(true);
            if (paidBill.getRefundedBill() != null) {
                paidBill.getRefundBills().add(cancelBill);
            } else {
                paidBill.setRefundedBill(cancelBill);
            }
            getBillFacade().edit(paidBill);
        }

        String remark = "Cancelled OB ref Nos - (";

        for (OnlineBooking ob : bookinsToAgenHospitalPayementCancellation) {
            ob.setPaidToHospitalBillCancelledAt(new Date());
            ob.setPaidToHospital(false);
            ob.setPaidToHospitalBill(null);
            remark += ob.getReferenceNo() + " / ";
            ob.setPaidToHospitalBillCancelledBy(getSessionController().getLoggedUser());
            ob.setPaidToHospitalCancelledBill(cancelBill);
            getOnlineBookingFacade().edit(ob);
        }

        remark += ") from " + paidBill.getDeptId() + " (bill no) Bill ";
        cancelBill.setComments(cancelBill.getComments() + " - " + remark);
        getBillFacade().edit(cancelBill);

        printBill = cancelBill;
        printOriginal = true;

        JsfUtil.addSuccessMessage("Bill cancellation is Successful.");

    }

    public Bill createCancelBillForAgentPaidToHospitalCancellation(double totalToCancel) {
        Bill bill = getCancelBill();
        bill.setNetTotal(-totalToCancel);
        bill.setTotal(-totalToCancel);
        bill.setCreatedAt(new Date());
        bill.setPaymentMethod(cancelPaymentMethod);
        bill.setCreater(getSessionController().getLoggedUser());
        bill.setToInstitution(printBill.getToInstitution());
        bill.setToDepartment(getSessionController().getDepartment());
        bill.setDepartment(getSessionController().getDepartment());
        bill.setFromInstitution(printBill.getFromInstitution());
        bill.setReferenceBill(printBill);

        String billNo = getBookingControllerViewScope().generateBillNumberInsId(bill);

        if (billNo != null && !billNo.isEmpty()) {
            bill.setDeptId(billNo);
        }

        if (bill.getId() == null) {
            getBillFacade().create(bill);
        } else {
            getBillFacade().edit(bill);
        }

        return bill;
    }

    public String createPaymentForHospital() {
        if (paidToHospitalList == null || paidToHospitalList.isEmpty()) {
            JsfUtil.addErrorMessage("No Bookings are selected to proceed");
            return null;
        }

        if (createTotalAmountToPay() != getPaidToHospitalBill().getNetTotal()) {
            JsfUtil.addErrorMessage("Value is different than total amount.");
            return null;
        }

        if (agentForBookings == null) {
            JsfUtil.addErrorMessage("No agent is selected.");
            return null;
        }

        Bill paidBill = createHospitalPaymentBill(paidToHospitalList);

        if (paidBill != null) {
            List<Payment> payments = createPayment(paidBill, paidToHospitalPaymentMethod, false);
//            drawerController.updateDrawerForIns(payments);
        }
        
        createBillItemForPaidToHospitalBill(paidBill);
        
        if (paidBill != null) {
            for (OnlineBooking ob : paidToHospitalList) {
                if (!ob.isPaidToHospital()) {
                    ob.setPaidToHospital(true);
                    ob.setPaidToHospitalDate(new Date());
                    ob.setPaidToHospitalProcessedBy(getSessionController().getLoggedUser());
                    ob.setPaidToHospitalBill(paidBill);
                    getOnlineBookingFacade().edit(ob);
                }
            }
        }

        printBill = paidBill;
        printOriginal = true;
        return "OB_agent_paid_bill_reprint?faces-redirect=true";

    }

    private double totalForPaidToHospitalBillCancellation;

    public void setTotalForPaidToHospitalBillCancellation(double totalForPaidToHospitalBillCancellation) {
        this.totalForPaidToHospitalBillCancellation = totalForPaidToHospitalBillCancellation;
    }

    public double getTotalForPaidToHospitalBillCancellation() {
        double totalAmountToCancellation = 0;

        for (OnlineBooking ob : bookinsToAgenHospitalPayementCancellation) {
            totalAmountToCancellation += ob.getAppoinmentTotalAmount();
        }

        return totalAmountToCancellation;
    }

    public void prepareCancellationAgentPaidToHospitalBills() {

        double totalAmountToCancellation = 0;

        if (bookinsToAgenHospitalPayementCancellation == null || bookinsToAgenHospitalPayementCancellation.isEmpty()) {

            getCancelBill().setTotal(totalAmountToCancellation);
            getCancelBill().setNetTotal(totalAmountToCancellation);
            return;

        }

        for (OnlineBooking ob : bookinsToAgenHospitalPayementCancellation) {
            totalAmountToCancellation += ob.getAppoinmentTotalAmount();
        }

        getCancelBill().setTotal(totalAmountToCancellation);
        getCancelBill().setNetTotal(totalAmountToCancellation);

        setTotalForPaymentMethodData(cancelPaymentMethod, getCancelBill());

    }

    public void onRowSelect(SelectEvent<OnlineBooking> event) {
        OnlineBooking selected = event.getObject();

        if (selected.isPaidToHospital()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Selection is not allowed", "This Booking is already paid."));
            getPaidToHospitalList().remove(selected); // manually remove it
        } else {
            getPaidToHospitalList().add(selected);
        }

    }

    public void onRowUnselect(SelectEvent<OnlineBooking> event) {
        OnlineBooking selected = event.getObject();
        getPaidToHospitalList().remove(selected);
    }

    public void onRowUnselectForCancellation(SelectEvent<OnlineBooking> event) {
        OnlineBooking selected = event.getObject();
        bookinsToAgenHospitalPayementCancellation.remove(selected);
        prepareCancellationAgentPaidToHospitalBills();
    }
    
    public void createBillItemForPaidToHospitalBill(Bill bill) {
        BillItem bi = new BillItem();
        bi.setNetValue(bill.getNetTotal());
        bi.setGrossValue(bill.getNetTotal());
        bi.setBillSession(null);
        bi.setDiscount(0.0);
        bi.setItem(null);
        bi.setQty(1.0);
        bi.setRate(bill.getNetTotal());
        bi.setBill(bill);
        
        billItemFacade.create(bi);
        
    }

    public void settleDirectFundBill() {
        if (paidToHospitalPaymentMethod == null) {
            JsfUtil.addErrorMessage("Please Select a Payment Method");
            return;
        }
        
        if (agentForBookings == null) {
            JsfUtil.addErrorMessage("Please Select a Booking Agent");
            return;
        }
        
        if (institutionForBookings == null) {
            JsfUtil.addErrorMessage("Please Select a Institution");
            return;
        }
        
        double total = 0;
        try {
            switch (paidToHospitalPaymentMethod) {
                case Cash:
                    total = paidToHospitalDirectFundBill.getNetTotal();
                    break;

                case Card:
                    total = getPaymentMethodData().getCreditCard().getTotalValue();
                    break;

                case Cheque:
                    total = getPaymentMethodData().getCheque().getTotalValue();
                    break;

                case Slip:
                    total = getPaymentMethodData().getSlip().getTotalValue();
                    break;
                default:
                    throw new AssertionError();
            }
            if (total <= 0) {
                JsfUtil.addErrorMessage("Please add valid amount to pay.");
                return;
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Please add valid amount to pay.");
            return;
        }
        
        Bill paidBill = createHospitalPaymentDirectFundBill(total);
        
        if(paidBill != null){
            createPayment(paidBill, paidToHospitalPaymentMethod, false);
        }
        createBillItemForPaidToHospitalBill(paidBill);
        
        printBill = paidBill;
        
        JsfUtil.addSuccessMessage("Direct Fund is deposited successfully.");
        
        
    }
    
    public Bill createHospitalPaymentDirectFundBill(double total) {
        Bill paidBill = getPaidToHospitalDirectFundBill();
        paidBill.setCreatedAt(new Date());
        paidBill.setCreater(getSessionController().getLoggedUser());
        paidBill.setToInstitution(institutionForBookings);
        paidBill.setToDepartment(getSessionController().getDepartment());
        paidBill.setDepartment(getSessionController().getDepartment());
        paidBill.setFromInstitution(agentForBookings);
        paidBill.setCreditCompany(agentForBookings);
        paidBill.setTotal(total);
        paidBill.setBalance(0d);
        paidBill.setBillDate(new Date());
        paidBill.setBillTime(new Date());
        paidBill.setPaymentMethod(paidToHospitalPaymentMethod);

        String billNo = bookingControllerViewScope.generateBillNumberInsId(paidBill);

        if (billNo != null && !billNo.isEmpty()) {
            paidBill.setDeptId(billNo);
        }

        if (paidBill.getId() == null) {
            getBillFacade().create(paidBill);
        } else {
            getBillFacade().edit(paidBill);
        }

        return paidBill;

    }

    public void clearPreviousValues() {
        institutionForBookings = null;
        paidToHospitalDirectFundBill = null;
        paidToHospitalBill = null;
        paidToHospitalPaymentMethod = null;
        paidToHospitalTotal = 0;
        paidToHospitalList = null;
        onlineBookingList = null;
        cancelBill = null;
        paymentMethodData = null;
        printBill = null;
        agentPaidToHospitalBills = null;
        printOriginal = false;

    }

    public double createTotalAmountToPay() {
        if (paidToHospitalList == null || paidToHospitalList.isEmpty()) {
            return 0;
        }

        double totalForPay = 0;

        for (OnlineBooking ob : paidToHospitalList) {
            if (!ob.isPaidToHospital()) {
                totalForPay += ob.getAppoinmentTotalAmount();
            }
        }
        paidToHospitalTotal = totalForPay;
        if (paidToHospitalBill != null) {
            paidToHospitalBill.setNetTotal(totalForPay);
        }

        return totalForPay;
    }

    public List<OnlineBooking> getPaidToHospitalList() {
        if (paidToHospitalList == null) {
            paidToHospitalList = new ArrayList<>();
        }

        return paidToHospitalList;
    }

    public void setPaidToHospitalList(List<OnlineBooking> paidToHospitalList) {
        this.paidToHospitalList = paidToHospitalList;
    }

    public Boolean getPaidStatus() {
        return paidStatus;
    }

    public void setPaidStatus(Boolean paidStatus) {
        this.paidStatus = paidStatus;
    }

    public List<OnlineBooking> getOnlineBookingList() {
        if (onlineBookingList == null) {
            onlineBookingList = new ArrayList<>();
        }
        return onlineBookingList;
    }

    public void setOnlineBookingList(List<OnlineBooking> onlineBookingList) {
        this.onlineBookingList = onlineBookingList;
    }

    public List<Institution> getAllAgents() {
        return allAgents;
    }

    public void setAllAgents(List<Institution> allAgents) {
        this.allAgents = allAgents;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public void prepareAdd() {
        current = new Institution();
        current.setInstitutionType(InstitutionType.OnlineBookingAgent);
    }

    public void fetchAllOnlineBookings() {
        if (institutionForBookings == null) {
            JsfUtil.addErrorMessage("Please Select Hospital.");
            return;
        }

        if (agentForBookings == null) {
            JsfUtil.addErrorMessage("Please Select the Agent.");
            return;
        }

        List<OnlineBookingStatus> statusToSearch = new ArrayList();

        if (onlineBookingStatus == null) {
            statusToSearch = fetchAllOnlineBookingStatus();
        } else {
            statusToSearch.add(onlineBookingStatus);
        }

        List<OnlineBooking> bookingList = channelService.fetchAllOnlineBookings(fromDate, toDate, agentForBookings, institutionForBookings, paidStatus, statusToSearch);

        if (bookingList != null) {
            onlineBookingList = bookingList;
        }
    }

    private List<Bill> agentPaidToHospitalBills;

    public List<Bill> getAgentPaidToHospitalBills() {
        return agentPaidToHospitalBills;
    }

    public void setAgentPaidToHospitalBills(List<Bill> agentPaidToHospitalBills) {
        this.agentPaidToHospitalBills = agentPaidToHospitalBills;
    }

    public void fetchAgentPaidToHospitalBills() {

        if (institutionForBookings == null) {
            JsfUtil.addErrorMessage("Please Select Hospital.");
            return;
        }

        if (agentForBookings == null) {
            JsfUtil.addErrorMessage("Please Select the Agent.");
            return;
        }

        List<Bill> bills = channelService.fetchOnlineBookingsAgentPaidToHospitalBills(fromDate, toDate, institutionForBookings, agentForBookings, billStatus);

        if (bills != null) {
            agentPaidToHospitalBills = bills;
        }
    }

    public void fetchOnlineBookingsForManagement() {
        if (institutionForBookings == null) {
            JsfUtil.addErrorMessage("Please Select Hospital.");
            return;
        }

        if (agentForBookings == null) {
            JsfUtil.addErrorMessage("Please Select the Agent.");
            return;
        }
        List<OnlineBookingStatus> status = new ArrayList<>();
        status.add(OnlineBookingStatus.COMPLETED);
        status.add(OnlineBookingStatus.DOCTOR_CANCELED);

        List<OnlineBooking> bookingList = channelService.fetchOnlineBookings(fromDate, toDate, agentForBookings, institutionForBookings, false, status);

        if (bookingList != null) {
            onlineBookingList = bookingList;
        }

        paidToHospitalList = null;
    }

    public void delete() {

        if (getCurrent() != null && getCurrent().getId() != null) {
            getCurrent().setRetired(true);
            getCurrent().setRetiredAt(new Date());
            getCurrent().setRetirer(getSessionController().getLoggedUser());
            getInstitutionFacade().edit(getCurrent());
            current = null;
            fillAllAgents();
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            current = null;
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
    }

    @PostConstruct
    public void init() {
        fillAllAgents();
    }

    public boolean checkDuplicateCodeAvailability(Institution institution) {
        fillAllAgents();
        for (Institution ins : allAgents) {
            if (institution.getId() == null) {
                if (ins.getCode().equalsIgnoreCase(institution.getCode())) {
                    return true;
                }
            } else if (institution.getId() != null) {
                if (ins.getId() != institution.getId()) {
                    if (ins.getCode().equalsIgnoreCase(institution.getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void saveSelected(boolean isNew) {
        if (getCurrent().getName() == null || getCurrent().getName().isEmpty()) {
            JsfUtil.addErrorMessage("Agent Name is missing.");
            return;
        }

        if (getCurrent().getCode() == null || getCurrent().getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Agent Code is missing.");
            return;
        }

        if (checkDuplicateCodeAvailability(current)) {
            JsfUtil.addErrorMessage("Agent Code is already taken. Use different one.");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0 && !isNew) {
            getInstitutionFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else if (isNew && getCurrent().getId() == null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getInstitutionFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        } else if (isNew && getCurrent().getId() != null) {
            JsfUtil.addErrorMessage("Please use update Button to edit Agent.");
        } else if (!isNew && getCurrent().getId() == null) {
            JsfUtil.addErrorMessage("Please Save the Agent.");
        }
        fillAllAgents();
    }

    public void fillAllAgents() {
        String j;
        j = "select i "
                + " from Institution i "
                + " where i.retired=:ret"
                + " and i.institutionType = :type"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("type", InstitutionType.OnlineBookingAgent);
        allAgents = getInstitutionFacade().findByJpql(j, m);
    }

    public List<Institution> completeAgent(String params) {
        if (params == null) {
            return null;
        }
        String j;
        j = "select i "
                + " from Institution i "
                + " where i.retired=:ret"
                + " and i.institutionType = :type"
                + " and (LOWER(i.name) like  LOWER(CONCAT('%', :params, '%')) or LOWER(i.code) like  LOWER(CONCAT('%', :params, '%')))"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("params", params);
        m.put("type", InstitutionType.OnlineBookingAgent);
        return getInstitutionFacade().findByJpql(j, m);
    }

    public Institution getCurrent() {
        if (current == null) {
            current = new Institution();
            current.setInstitutionType(InstitutionType.OnlineBookingAgent);
        }
        return current;
    }

    public void setCurrent(Institution current) {
        this.current = current;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitutionForBookings() {
        return institutionForBookings;
    }

    public void setInstitutionForBookings(Institution institutionForBookings) {
        this.institutionForBookings = institutionForBookings;
    }

    public Institution getAgentForBookings() {
        return agentForBookings;
    }

    public void setAgentForBookings(Institution agentForBookings) {
        this.agentForBookings = agentForBookings;
    }

    public ChannelService getChannelService() {
        return channelService;
    }

    public void setChannelService(ChannelService channelService) {
        this.channelService = channelService;
    }

    public Bill getPrintBill() {
        return printBill;
    }

    public void setPrintBill(Bill printBill) {
        this.printBill = printBill;
    }

    public boolean isPrintOriginal() {
        return printOriginal;
    }

    public void setPrintOriginal(boolean printOriginal) {
        this.printOriginal = printOriginal;
    }

}

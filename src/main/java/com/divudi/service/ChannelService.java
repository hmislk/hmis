package com.divudi.service;

import com.divudi.bean.channel.BookingControllerViewScope;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.ApiKeyType;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.ApiKey;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Consultant;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.Payment;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.ApiKeyFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.ConsultantFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.SessionInstanceFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.java.CommonFunctions;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Chinthaka Prasad
 */
@Stateless
public class ChannelService {

    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private SessionInstanceFacade sessionInstanceFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @EJB
    private ConsultantFacade consultantFacade;
    @EJB
    private StaffFacade staffFacade;

    @Inject
    private BookingControllerViewScope bookingControllerViewScope;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;


    public void retireNonSettledOnlineBills() {
        String jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret ";
        jpql += " and b.billTypeAtomic=:bta ";
        jpql += " and b.createdAt < :createdAt ";
        jpql += " and b.paid=:paid ";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("paid", false);
        Long minutesToCancelOnlineBooking = configOptionApplicationController.getLongValueByKey("Number of Minutes to keep online channel bookings active without finalizing the payment", 20L);
        params.put("createdAt", CommonFunctions.deductMinutesFromCurrentTime(minutesToCancelOnlineBooking.intValue()));
        params.put("bta", BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        List<Bill> unpaidBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        if (unpaidBills == null) {
            return;
        }
        if (unpaidBills.isEmpty()) {
            return;
        }
        for (Bill b : unpaidBills) {
            b.setRetired(true);
            b.setRetireComments("Online Agent Payment NOT completed");
            b.setRetiredAt(new Date());
            billFacade.edit(b);
            b.getSingleBillSession().setRetired(true);
            b.getSingleBillSession().setRetireComments("Online Agent Payment NOT completed");
            b.getSingleBillSession().setRetiredAt(new Date());
            billSessionFacade.edit(b.getSingleBillSession());
            b.getSingleBillItem().setRetired(true);
            b.getSingleBillItem().setRetireComments("Online Agent Payment NOT completed");
            billItemFacade.edit(b.getSingleBillItem());
            if(b.getBillFees() != null){
                for(BillFee bf : b.getBillFees()){
                    bf.setRetired(true);
                    bf.setRetireComments("Online Agent Payment NOT completed");
                    billFeeFacade.edit(bf);
                }
            }
        }
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public SessionInstanceFacade getSessionInstanceFacade() {
        return sessionInstanceFacade;
    }

    public void setSessionInstanceFacade(SessionInstanceFacade sessionInstanceFacade) {
        this.sessionInstanceFacade = sessionInstanceFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public BookingControllerViewScope getBookingControllerViewScope() {
        return bookingControllerViewScope;
    }

    public void setBookingControllerViewScope(BookingControllerViewScope bookingControllerViewScope) {
        this.bookingControllerViewScope = bookingControllerViewScope;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PersonFacade personFacade;

    @Inject
    BillBeanController billBeanController;

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

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public void saveOrUpdatePatientDetails(Patient p) {
        if (p == null) {
            return;
        }
        if (p.getPerson() == null) {
            return;
        }
        if (p.getPerson().getId() == null) {
            p.getPerson().setCreatedAt(new Date());
            personFacade.create(p.getPerson());
        } else {
            personFacade.edit(p.getPerson());
        }
        if (p.getId() == null) {
            p.setCreatedAt(new Date());
            //     p.setCreatedInstitution(sessionController.getInstitution());
            patientFacade.create(p);
        } else {
            patientFacade.edit(p);
        }
    }

    public Bill addToReserveAgentBookingThroughApi(boolean forReservedNumbers, Patient patient, SessionInstance session, String refNo, WebUser user, Institution creditCompany) {
        saveOrUpdatePatientDetails(patient);
        Bill savingTemporaryBill = createAgentInitialBookingBill(patient, session);
        BillItem savingBillItemForSession = createSessionItem(savingTemporaryBill, refNo, session);
        savingTemporaryBill.setAgentRefNo(refNo);
        savingTemporaryBill.setCreditCompany(creditCompany);

        BillSession savingTemporaryBillSession = createBillSession(savingTemporaryBill, savingBillItemForSession, forReservedNumbers, session);
        System.out.println(savingTemporaryBillSession);

        List<BillFee> savingBillFees = new ArrayList<>();

        List<BillFee> savingBillFeesFromSession = createBillFeeForSessions(savingTemporaryBill, savingBillItemForSession, false, null);

        List<BillFee> savingBillFeesFromAdditionalItems = new ArrayList<>();

//        if (!additionalBillItems.isEmpty()) {
//            for (BillItem abi : additionalBillItems) {
//                List<BillFee> blf = createBillFeeForSessions(savingBill, abi, addedItemFees, priceMatrix);
//                for (BillFee bf : blf) {
//                    savingBillFeesFromAdditionalItems.add(bf);
//                }
//            }
//        }
        if (savingBillFeesFromSession != null) {
            savingBillFees.addAll(savingBillFeesFromSession);
        }
        if (!savingBillFeesFromAdditionalItems.isEmpty()) {
            savingBillFees.addAll(savingBillFeesFromAdditionalItems);
        }

        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItemForSession);
        getBillItemFacade().edit(savingBillItemForSession);
        savingBillItemForSession.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItemForSession));
        savingBillItemForSession.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItemForSession));
        savingBillItemForSession.setBillSession(savingTemporaryBillSession);

        getBillSessionFacade().edit(savingTemporaryBillSession);
        savingTemporaryBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingTemporaryBill));
        savingTemporaryBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingTemporaryBill));
        savingTemporaryBill.setSingleBillItem(savingBillItemForSession);
        savingTemporaryBill.setSingleBillSession(savingTemporaryBillSession);
        savingTemporaryBill.setBillItems(savingBillItems);
        savingTemporaryBill.setBillFees(savingBillFees);
        // savingBill.setCashPaid(cashPaid);
        // savingBill.setCashBalance(cashBalance);
        savingTemporaryBill.setBalance(savingTemporaryBill.getNetTotal());

//        if (savingBill.getBillType() == BillType.ChannelAgent) {
//            updateBallance(savingBill.getCreditCompany(), 0 - savingBill.getNetTotal(), HistoryType.ChannelBooking, savingBill, savingBillItemForSession, savingBillSession, savingBillItemForSession.getAgentRefNo());
//            savingBill.setBalance(0.0);
//            savingBillSession.setPaidBillSession(savingBillSession);
//        } else if (savingBill.getBillType() == BillType.ChannelCash) {
//            savingBill.setBalance(0.0);
//            savingBillSession.setPaidBillSession(savingBillSession);
//        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
//            savingBill.setBalance(savingBill.getNetTotal());
//        } else if (savingBill.getBillType() == BillType.ChannelStaff) {
//            savingBill.setBalance(0.0);
//            savingBillSession.setPaidBillSession(savingBillSession);
//        } 
//        if (referredBy != null) {
//            savingBill.setReferredBy(referredBy);
//        }
//        if (referredByInstitution != null) {
//            savingBill.setReferenceInstitution(referredByInstitution);
//        }
//        if (collectingCentre != null) {
//            savingBill.setCollectingCentre(collectingCentre);
//        }
//        if (savingBill.getPaidAmount() == 0.0) {
//            if (!(savingBill.getPaymentMethod() == PaymentMethod.OnCall)) {
//                savingBill.setPaidAmount(feeNetTotalForSelectedBill);
//            } else {
//                if (feeNetTotalForSelectedBill != null) {
//                    savingBill.setNetTotal(feeNetTotalForSelectedBill);
//                }
//            }
//        }

        calculateBillTotalsFromBillFees(savingTemporaryBill, savingBillFees);

        getBillFacade().edit(savingTemporaryBill);
        getBillSessionFacade().edit(savingTemporaryBillSession);
        return savingTemporaryBill;
    }

    public List<ItemFee> findServiceSessionFees(ServiceSession ss) {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and (f.serviceSession=:ses "
                + " or f.item=:ses )"
                + " order by f.id";
        m.put("ses", ss);
        List<ItemFee> tfs = itemFeeFacade.findByJpql(sql, m);
        return tfs;
    }

    private void calculateBillTotalsFromBillFees(Bill billToCaclculate, List<BillFee> billfeesAvailable) {
//        System.out.println("calculateBillTotalsFromBillFees");
//        System.out.println("billToCaclculate = " + billToCaclculate);
//        System.out.println("billfeesAvailable = " + billfeesAvailable);
        double calculatingGrossBillTotal = 0.0;
        double calculatingNetBillTotal = 0.0;

        for (BillFee iteratingBillFee : billfeesAvailable) {
//            System.out.println("iteratingBillFee = " + iteratingBillFee);
            if (iteratingBillFee.getFee() == null) {
                continue;
            }

            calculatingGrossBillTotal += iteratingBillFee.getFeeGrossValue();
//            System.out.println("calculatingGrossBillTotal = " + calculatingGrossBillTotal);
            calculatingNetBillTotal += iteratingBillFee.getFeeValue();
//            System.out.println("calculatingNetBillTotal = " + calculatingNetBillTotal);

        }
        billToCaclculate.setDiscount(calculatingGrossBillTotal - calculatingNetBillTotal);
        billToCaclculate.setNetTotal(calculatingNetBillTotal);
        System.out.println(calculatingNetBillTotal + " g " + calculatingGrossBillTotal);
        billToCaclculate.setTotal(calculatingGrossBillTotal);
        getBillFacade().edit(billToCaclculate);
    }

    @Deprecated
    private String generateBillNumberDeptId(Bill bill) {
        String suffix = bill.getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
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
                deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = billNumberBean.departmentBillNumberGenerator(bill.getInstitution(), bill.getDepartment(), bts, billClassType, suffix);
        }

        return deptId;
    }

    public Institution findCreditCompany(String code, InstitutionType type) {

        String jpql = "Select i from Institution i where i.retired = false "
                + " and UPPER(i.code) = UPPER(:code) "
                + " and i.institutionType = :type";

        Map params = new HashMap();
        params.put("code", code);
        params.put("type", type);

        return institutionFacade.findFirstByJpql(jpql, params);
    }

    private Bill createAgentInitialBookingBill(Patient patient, SessionInstance session) {
        Bill bill = new BilledBill();
        bill.setStaff(session.getOriginatingSession().getStaff());
        //bill.setToStaff(toStaff);
        bill.setAppointmentAt(session.getSessionDate());
        bill.setTotal(session.getOriginatingSession().getTotal());
        bill.setNetTotal(session.getOriginatingSession().getTotal());
        bill.setPaymentMethod(PaymentMethod.OnCall);
        bill.setPatient(patient);
        bill.setPaid(false);
        bill.setPaidAmount(0.0);
        bill.setPaidBill(null);
        bill.setBillType(BillType.ChannelOnCall);
        bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        System.out.println(bill);
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(session.getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());

        bill.setToDepartment(session.getDepartment());
        bill.setToInstitution(session.getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent || bill.getBillType() == BillType.ChannelStaff) {
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }
        return bill;
    }

    public List<BillItem> findBillItemFromRefNo(String refNo) {
        Map params = new HashMap();
        String jpql = "Select bi from BillItem bi"
                + " where bi.AgentRefNo = :ref"
                + " And bi.retired = false";

        params.put("ref", refNo);
        return billItemFacade.findByJpql(jpql, params);

    }

    private BillItem createSessionItem(Bill bill, String refNo, SessionInstance session) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(refNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        // bi.setCreater(getSessionController().getLoggedUser());
        bi.setGrossValue(session.getOriginatingSession().getTotal());
        bi.setItem(session.getOriginatingSession());
//        bi.setItem(getSelectedSessionInstance().getOriginatingSession());
        bi.setNetRate(session.getOriginatingSession().getTotal());
        bi.setNetValue(session.getOriginatingSession().getTotal());
        bi.setQty(1.0);
        bi.setRate(session.getOriginatingSession().getTotal());
        bi.setSessionDate(session.getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }
    
    private List<BillSession> getAllBillSessionForSessionInstance(SessionInstance ss){
        List<BillSession> allBillSessions = new ArrayList<>();
        BillType[] billTypes = {
            BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelCredit,
            BillType.ChannelResheduleWithPayment,
            BillType.ChannelResheduleWithOutPayment,};

        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select bs "
                + " From BillSession bs "
                + " where bs.retired=false"
                + " and bs.bill.billType in :bts"
                + " and type(bs.bill)=:class "
                + " and bs.sessionInstance=:ss "
                + " order by bs.serialNo ";
        HashMap<String, Object> hh = new HashMap<>();

        Bill b = new Bill();
        b.getBillTypeAtomic();
        hh.put("bts", bts);
        hh.put("class", BilledBill.class);
        hh.put("ss", ss);
        return getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
    }
    
    public List getReleasedAppoinmentNumbersForApiBookings(SessionInstance ss) {
        long nextNumber = 1L;
        
        if(ss.getNextAvailableAppointmentNumber() != null){
            nextNumber = ss.getNextAvailableAppointmentNumber();
        }
        
        List releasedNumberList = new ArrayList();
        
        List<BillSession> allBillSessions = getAllBillSessionForSessionInstance(ss);

        List<Integer> reservedSerialNumbers = allBillSessions.stream()
                .map(BillSession::getSerialNo)
                .collect(Collectors.toList());

        for (int i = 1; i < nextNumber; ++i) {
            boolean isAssign = false;
            for (Integer number : reservedSerialNumbers) {
                if (i == number) {
                    isAssign = true;
                    
                }
            }

            if (!isAssign) {
                releasedNumberList.add(i);
            }
        }
        return releasedNumberList;
    }

    private BillSession createBillSession(Bill bill, BillItem billItem, boolean forReservedNumbers, SessionInstance session) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setReservedBooking(forReservedNumbers);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        // bs.setCreater(getSessionController().getLoggedUser());
        bs.setDepartment(session.getOriginatingSession().getDepartment());
        bs.setInstitution(session.getOriginatingSession().getInstitution());
        bs.setItem(session.getOriginatingSession());
//        bs.setPresent(true);
//        bs.setPresent(true);
//        bs.setItem(getSelectedSessionInstance().getOriginatingSession());

        bs.setServiceSession(session.getOriginatingSession());
        bs.setSessionInstance(session);
//        bs.setServiceSession(getSelectedSessionInstance().getOriginatingSession());
        bs.setSessionDate(session.getSessionDate());
        bs.setSessionTime(session.getSessionTime());
        bs.setStaff(session.getStaff());

        List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(session.getOriginatingSession().getReserveNumbers());
        Integer count = null;
        
        List<Integer> availableReleasedApoinmentNumbers = getReleasedAppoinmentNumbersForApiBookings(session); 
        Random rand = new Random();
        if(availableReleasedApoinmentNumbers != null && !availableReleasedApoinmentNumbers.isEmpty()){
            count = availableReleasedApoinmentNumbers.get(rand.nextInt(availableReleasedApoinmentNumbers.size()));
        }
        

        if (forReservedNumbers) {
//            // Pass the selectedReservedBookingNumber to the service method
//            count = serviceSessionBean.getNextAvailableReservedNumber(session, reservedNumbers, selectedReserverdBookingNumber);
//            if (count == null) {
//                count = serviceSessionBean.getNextNonReservedSerialNumber(session, reservedNumbers);
//                JsfUtil.addErrorMessage("No reserved numbers available. Normal number is given");
//            }
        } else if(count == null){
            count = serviceSessionBean.getNextNonReservedSerialNumber(session, reservedNumbers);
        }

        if (count != null) {
            bs.setSerialNo(count);
        } else {
            bs.setSerialNo(1);
        }
        System.out.println(bs);
        getBillSessionFacade().create(bs);

        return bs;
    }

    public List<ItemFee> findItemFees(Item i) {
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:i "
                + " order by f.id";
        m.put("i", i);
        return itemFeeFacade.findByJpql(sql, m);
    }

    public List<Bill> findBillFromRefNo(String refNo, Institution creditCompany, BillClassType b) {
        Map params = new HashMap();
        String jpql = "Select b from Bill b "
                + " where b.agentRefNo = :ref"
                + " and b.retired = false";

        if (creditCompany != null) {
            jpql += " and b.creditCompany = :cc";
            params.put("cc", creditCompany);
        }

        if (b != null) {
            jpql += " and b.billClassType = :bb";
            params.put("bb", b);
        }

        params.put("ref", refNo);

        return billFacade.findByJpql(jpql, params);

    }

    public List<SessionInstance> findSessionInstanceFromId(String id) {
        String jpql = "Select ss from SessionInstance ss "
                + " Where ss.completed = false "
                + " and ss.id = :id";

        Long idLong = Long.parseLong(id);
        Map parms = new HashMap();
        parms.put("id", idLong);

        return sessionInstanceFacade.findByJpql(jpql, parms);

    }

    private BillSession cancelBillSession(BillSession selectedBillSession, CancelledBill can, BillItem canBillItem) {
        BillSession bs = new BillSession();
        bs.copy(selectedBillSession);
        bs.setBill(can);
        bs.setBillItem(canBillItem);
        bs.setCreatedAt(new Date());
        //  bs.setCreater(getSessionController().getLoggedUser());
        getBillSessionFacade().create(bs);
        System.out.println(can);
        can.setSingleBillSession(bs);
        getBillFacade().edit(can);

        return bs;
    }

    public List<Bill> viewBookingHistorybyDate(String fromDate, String toDate, Institution cc, BillClassType b) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            Date startDate = formatter.parse(fromDate);
            Date endDate = formatter.parse(toDate);
            System.out.println(startDate + "" + endDate);

            Map params = new HashMap();

            BillTypeAtomic billType = BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT;
            String jpql = "Select b from Bill b "
                    + " where b.billDate between :fd And :td "
                    + " and b.creditCompany = :cc "
                    + " and b.billTypeAtomic = :bt";

            if (b != null) {
                jpql += " and b.billClassType = :bc";
                params.put("bc", b);
            }

            params.put("fd", startDate);
            params.put("td", endDate);
            params.put("cc", cc);
            params.put("bt", billType);

            return billFacade.findByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        } catch (ParseException ex) {
            Logger.getLogger(ChannelService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public BillSession cancelBookingBill(Bill bill) {

        BillSession bs = bill.getSingleBillSession();
        CancelledBill cb = createCancelBill1(bill);
        BillItem cItem = cancelBillItems(bs.getBillItem(), cb);
        BillSession cbs = cancelBillSession(bs, cb, cItem);
        //  bill.getSingleBillSession().getBill().setCancelled(true);
        bill.setCancelled(true);
        System.out.println(bill.getBillClass());
        System.out.println(bs.getBill().getBillClass());

        if (bill.getPaidBill() != null) {
            System.out.println("inside");
            bill.getPaidBill().setCancelled(true);
        }
        bs.getBill().setCancelledBill(cb);
        getBillFacade().edit(bill);
        bs.setReferenceBillSession(cbs);
        billSessionFacade.edit(bs);
        return cbs;

        //  sendSmsOnChannelCancellationBookings();
    }

    private BillItem cancelBillItems(BillItem bi, CancelledBill can) {

        BillItem b = new BillItem();
        b.setBill(can);
        b.copy(bi);
        b.invertValue(bi);
        b.setCreatedAt(new Date());
        // b.setCreater(getSessionController().getLoggedUser());

        getBillItemFacade().create(b);
        String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
        cancelBillFee(can, b, tmp);

        return b;
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(can);
            bf.setBillItem(bt);
            System.out.println(nB.getBill().getBillClass() + "bf");
            bf.setCreatedAt(new Date());
            //   bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

    private CancelledBill createCancelBill1(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertAndAssignValuesFromOtherBill(bill);
        cb.setAgentRefNo(bill.getAgentRefNo());
        cb.setCreditCompany(bill.getCreditCompany());
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());

        // cb.setCreater(getSessionController().getLoggedUser());
        // cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        //  cb.setInstitution(getSessionController().getInstitution());
        // cb.setComments(comment);
        //     cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(bill.getInstitution(), cb));
        // String insId = bookingControllerViewScope.generateBillNumberInsId(cb);
        String insId = billNumberBean.institutionChannelBillNumberGenerator(bill.getSingleBillSession().getSessionInstance().getOriginatingSession().getInstitution(), cb);
        System.out.println(insId);
        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(bill.getSingleBillSession().getSessionInstance().getOriginatingSession().getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        // String deptId = generateBillNumberDeptId(cb);
        System.out.println(deptId);
        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        getBillFacade().create(cb);
        cb.setPaymentMethod(bill.getPaymentMethod());

//        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
//            cb.setPaymentMethod(cancelPaymentMethod);
////            if (cancelPaymentMethod == PaymentMethod.Agent) {
////                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, selectedBillSession.getBillItem(), selectedBillSession, selectedBillSession.getBill().getReferralNumber());
////            }
//        } else {
//            cb.setPaymentMethod(bill.getPaymentMethod());
//        }
        getBillFacade().edit(cb);
        return cb;
    }

    public List<Institution> findHospitals() {
        Map params = new HashMap();
        String jpql = "select c from Institution c "
                + " where c.retired=false "
                + " and c.institutionType =:type";

        params.put("type", InstitutionType.Company);

        return institutionFacade.findByJpql(jpql, params);
    }

    public List<SessionInstance> findSessionInstancesForDoctor(Institution institution, Long id) {
        String jpql = "Select i from SessionInstance i where i.retired = false"
                + " and i.originatingSession.institution=:ins "
                + " and i.cancelled = false and i.completed = false "
                + " and i.originatingSession.staff.id = :id";

        Map params = new HashMap();
        params.put("ins", institution);
        params.put("id", id);
        return sessionInstanceFacade.findByJpql(jpql, params);

    }

    public List<Institution> findInstitutionFromId(Long id) {
        String jpql = "Select i from Institution i where i.retired = false ";
        Map params = new HashMap();

        if (id != null && !id.toString().isEmpty()) {
            jpql += "and i.id = :id";
            params.put("id", id);

        }

        return institutionFacade.findByJpql(jpql, params);
    }

    public boolean checkHospitalId(Long id) {
        List<Institution> allHospitals = findInstitutionFromId(null);
        boolean availableHospitalWithId = false;
        for (Institution i : allHospitals) {
            if (i.getId() == id) {
                availableHospitalWithId = true;
            }
        }
        return availableHospitalWithId;
    }

    public Long checkSafeParseLong(String value) {
        try {
            Long data = Long.valueOf(value);
            return data;
        } catch (Exception e) {
            return null;
        }

    }

    public List<Speciality> findSpecilityFromId(Long id) {
        String jpql = "select s from Speciality s where s.retired=:ret ";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);

        if (id != null && !id.toString().isEmpty()) {
            jpql += " and s.id=:id";
            params.put("id", id);
        }

        return specialityFacade.findByJpql(jpql, params);
    }

    public boolean checkSpecialityId(Long id) {
        List<Speciality> specialities = findSpecilityFromId(null);
        boolean availableSpecialityWithId = false;
        for (Speciality s : specialities) {
            if (s.getId() == id) {
                availableSpecialityWithId = true;
            }
        }
        return availableSpecialityWithId;
    }

    public List<Consultant> findConsultantFromName(String name, Long id) {
        StringBuffer jpql = new StringBuffer("select c from Consultant c where c.retired=:ret");
        Map m = new HashMap();
        m.put("ret", false);

        if (name != null && !name.isEmpty()) {
            jpql.append(" and c.person.name like :name");
            m.put("name", "%" + name + "%");
        }
        if (id != null && !id.toString().isEmpty()) {
            jpql.append(" and c.id =:id");
            m.put("id", id);
        }

        return consultantFacade.findByJpql(jpql.toString(), m);
    }
    public List<Speciality> findAllSpecilities(){
        String jpql;
        Map params = new HashMap();
        jpql = " select c  "
                + " from DoctorSpeciality c "
                + " where c.retired=:ret "
                + " order by c.name";
        params.put("ret", false);
        return staffFacade.findByJpql(jpql, params);
    }

    public List<Doctor> findDoctorsFromName(String name, Long id) {
        StringBuffer jpql = new StringBuffer("select c from Doctor c where c.retired=:ret");
        Map m = new HashMap();
        m.put("ret", false);

        if (name != null && !name.isEmpty()) {
            jpql.append(" and c.person.name like :name");
            m.put("name", "%" + name + "%");
        }
        if (id != null && !id.toString().isEmpty()) {
            jpql.append(" and c.id =:id");
            m.put("id", id);
        }

        return consultantFacade.findByJpql(jpql.toString(), m);
    }

    public List<SessionInstance> findSessionInstance(List<Institution> institution, List<Speciality> specialities, List<Doctor> doctorList, Date sessionDate) {
        List<SessionInstance> sessionInstances;
        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select i from SessionInstance i where i.retired=:ret and i.originatingSession.retired=:ret "
                + " and i.cancelled = false"
                + " and i.completed = false");

        // Handle sessionDate equality check
        if (sessionDate != null) {
            jpql.append(" and i.sessionDate = :sd ");
            m.put("sd", sessionDate);
            System.out.println(sessionDate);
        } else if (sessionDate == null) {
            jpql.append(" and i.sessionDate >= :sd ");
            m.put("sd", new Date());
        }
//         
//        if(fromDate != null){
//            jpql.append(" and i.sessionDate >= :fd");
//            m.put("fd", fromDate);
//        }

        // Additional conditions for consultant, institution, and specialities
        if (doctorList != null && !doctorList.isEmpty()) {
            jpql.append(" and i.originatingSession.staff in :os");
            m.put("os", doctorList);
        }
        if (institution != null && !institution.isEmpty()) {
            jpql.append(" and i.originatingSession.institution in :ins");
            m.put("ins", institution);
        }
        if (specialities != null && !specialities.isEmpty()) {
            jpql.append(" and i.originatingSession.staff.speciality in :spe ");
            m.put("spe", specialities);
        }

        m.put("ret", false);

        sessionInstances = sessionInstanceFacade.findByJpql(jpql.toString(), m, TemporalType.TIMESTAMP);
        // System.out.println(jpql.toString()+"\n"+sessionInstances.size()+"\n"+m.values());
        return sessionInstances;
    }
    
    @EJB
    WebUserFacade webUserFacade;
    @Inject
    SecurityController securityController;
    
    public WebUser checkUserCredentialForApi(String temUserName, String temPassword) {
     
        String temSQL; 
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and (u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = webUserFacade.findFirstByJpql(temSQL, m);

        if (u == null) {
            return null;
        }

        if (securityController.matchPassword(temPassword, u.getWebUserPassword())) {

            return u;
        }
      
        return null;
    }
    
    @EJB
    ApiKeyFacade apiKeyFacade;
    
    public List<ApiKey> listApiKeysForUser(WebUser user) {
        String j;
        j = "select a "
                + " from ApiKey a "
                + " where a.retired=false "
                + " and a.webUser=:wu "
                + " and a.dateOfExpiary > :ed "
                + " order by a.dateOfExpiary";
        Map m = new HashMap();
        m.put("wu", user);
        m.put("ed", new Date());
        return apiKeyFacade.findByJpql(j, m, TemporalType.DATE);
    }
    
     public ApiKey createNewApiKeyForApiResponse(WebUser user) {
        UUID uuid = UUID.randomUUID();
        ApiKey newOne = new ApiKey();
        newOne.setWebUser(user);
        newOne.setKeyType(ApiKeyType.Token);
        newOne.setKeyValue(uuid.toString());
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 12);
        newOne.setDateOfExpiary(c.getTime());
        apiKeyFacade.create(newOne);
        return newOne;
    }

    public SessionInstance findNextSessionInstance(List<Institution> institution, List<Speciality> specialities, List<Doctor> doctorList, Date sessionDate) {
        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select i from SessionInstance i where i.retired=:ret and i.originatingSession.retired=:ret "
                + " and i.cancelled = false"
                + " and i.completed = false");

        // Handle sessionDate equality check
        if (sessionDate != null) {
            jpql.append(" and i.sessionDate > :sd ");
            m.put("sd", sessionDate);
           // System.out.println(sessionDate);
        }
//         
//        if(fromDate != null){
//            jpql.append(" and i.sessionDate >= :fd");
//            m.put("fd", fromDate);
//        }

        // Additional conditions for consultant, institution, and specialities
        if (doctorList != null && !doctorList.isEmpty()) {
            jpql.append(" and i.originatingSession.staff in :os");
            m.put("os", doctorList);
        }
        if (institution != null && !institution.isEmpty()) {
            jpql.append(" and i.originatingSession.institution in :ins");
            m.put("ins", institution);
        }
        if (specialities != null && !specialities.isEmpty()) {
            jpql.append(" and i.originatingSession.staff.speciality in :spe ");
            m.put("spe", specialities);
        }
        jpql.append(" order by i.sessionDate asc");

        m.put("ret", false);
        System.out.println(jpql.toString() + "\n" + m);
        return sessionInstanceFacade.findFirstByJpql(jpql.toString(), m, TemporalType.DATE);
        // System.out.println(jpql.toString()+"\n"+sessionInstances.size()+"\n"+m.values());

    }

    public Bill settleOnlineAgentInitialBooking(BillSession preBillSession, String refNo) {
        Bill paidBill = saveAgentOnlinePaymentCompletionBill(preBillSession, refNo);
        BillItem paidBillItem = savePaidBillItem(paidBill, preBillSession);
        savePaidBillFee(paidBill, paidBillItem, preBillSession);
        BillSession paidBillSession = savePaidBillSession(paidBill, paidBillItem, preBillSession);
        preBillSession.setPaidBillSession(paidBillSession);
        getBillSessionFacade().edit(paidBillSession);
        getBillSessionFacade().edit(preBillSession);

        preBillSession.getBill().setPaidAmount(paidBill.getPaidAmount());
        preBillSession.getBill().setBalance(0.0);
        preBillSession.getBill().setPaidBill(paidBill);
        preBillSession.getBill().setPaid(true);

        getBillFacade().edit(preBillSession.getBill());

        paidBill.setSingleBillItem(paidBillItem);
        paidBill.setSingleBillSession(paidBillSession);
        getBillFacade().editAndCommit(paidBill);

        List<Payment> p = createPayment(paidBill, PaymentMethod.OnlineSettlement);
        return paidBill;
        // drawerController.updateDrawerForIns(p);
    }

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        Payment p = new Payment();
        p.setBill(bill);
        // p.setInstitution(getSessionController().getInstitution());
        // p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        // p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);
        p.setPaidValue(p.getBill().getNetTotal());
        paymentFacade.create(p);

        ps.add(p);

        return ps;

    }

    private BillItem savePaidBillItem(Bill b, BillSession bs) {
        BillItem bi = new BillItem();
        bi.copy(bs.getBillItem());
        bi.setCreatedAt(new Date());
        // bi.setCreater(getSessionController().getLoggedUser());
        bi.setBill(b);
        getBillItemFacade().create(bi);

        return bi;
    }

    private void savePaidBillFee(Bill b, BillItem bi, BillSession bs) {

        for (BillFee f : bs.getBill().getBillFees()) {

            BillFee bf = new BillFee();
            bf.copy(f);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            // bf.setCreater(getSessionController().getLoggedUser());
            bf.setBill(b);
            bf.setBillItem(bi);
            getBillFeeFacade().create(bf);
        }
    }

    private BillSession savePaidBillSession(Bill paidBill, BillItem paidBillItem, BillSession preBillSession) {
        BillSession paidBillSession = new BillSession();
        paidBillSession.copy(preBillSession);
        paidBillSession.setBill(paidBill);
        paidBillSession.setBillItem(paidBillItem);
        paidBillSession.setCreatedAt(new Date());
        //  bs.setCreater(getSessionController().getLoggedUser());

        getBillSessionFacade().create(paidBillSession);
        return paidBillSession;

    }

    private Bill saveAgentOnlinePaymentCompletionBill(BillSession bs, String refNo) {
        Bill newlyCreatedAgentOnlinePaymentCompletionBill = new BilledBill();
        newlyCreatedAgentOnlinePaymentCompletionBill.setAgentRefNo(refNo);
        newlyCreatedAgentOnlinePaymentCompletionBill.copy(bs.getBill());
        newlyCreatedAgentOnlinePaymentCompletionBill.copyValue(bs.getBill());
        newlyCreatedAgentOnlinePaymentCompletionBill.setPaidAmount(bs.getBill().getNetTotal());
        newlyCreatedAgentOnlinePaymentCompletionBill.setBalance(0.0);
        newlyCreatedAgentOnlinePaymentCompletionBill.setPaymentMethod(PaymentMethod.Agent);
        newlyCreatedAgentOnlinePaymentCompletionBill.setReferenceBill(bs.getBill());
        newlyCreatedAgentOnlinePaymentCompletionBill.setBillType(BillType.ChannelAgent);
        newlyCreatedAgentOnlinePaymentCompletionBill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT);
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(bs.getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT);
        // String deptId = generateBillNumberDeptId(temp);
        System.out.println(deptId);
        newlyCreatedAgentOnlinePaymentCompletionBill.setInsId(deptId);
        newlyCreatedAgentOnlinePaymentCompletionBill.setDeptId(deptId);
        newlyCreatedAgentOnlinePaymentCompletionBill.setBookingId(deptId);
        newlyCreatedAgentOnlinePaymentCompletionBill.setDepartment(bs.getDepartment());
        newlyCreatedAgentOnlinePaymentCompletionBill.setInstitution(bs.getInstitution());
        newlyCreatedAgentOnlinePaymentCompletionBill.setBillDate(new Date());
        newlyCreatedAgentOnlinePaymentCompletionBill.setBillTime(new Date());
        newlyCreatedAgentOnlinePaymentCompletionBill.setCreatedAt(new Date());
        // temp.setCreater(getSessionController().getLoggedUser());

        getBillFacade().create(newlyCreatedAgentOnlinePaymentCompletionBill);

        return newlyCreatedAgentOnlinePaymentCompletionBill;
    }

    private List<BillFee> createBillFeeForSessions(Bill bill, BillItem billItem, boolean thisIsAnAdditionalFee, PriceMatrix priceMatrix) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpDiscount = 0;
        double tmpGrossTotal = 0.0;

        List<ItemFee> sessionsFees = null;

        if (thisIsAnAdditionalFee) {
            sessionsFees = findItemFees(billItem.getItem());
        } else {
            if (billItem.getItem() instanceof ServiceSession) {
                sessionsFees = findServiceSessionFees((ServiceSession) billItem.getItem());
            }
        }

        if (sessionsFees == null) {
            return billFeeList;
        }
        for (ItemFee f : sessionsFees) {
            if (bill.getPaymentMethod() != PaymentMethod.Agent) {
                if (f.getFeeType() == FeeType.OtherInstitution) {
                    continue;
                }
            }
            if (bill.getPaymentMethod() != PaymentMethod.OnCall) {
                if (f.getFeeType() == FeeType.OwnInstitution && f.getName().equalsIgnoreCase("On-Call Fee")) {
                    continue;
                }
            }
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
            // bf.setCreater(getSessionController().getLoggedUser());
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(bill.getInstitution());
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
                bf.setStaff(f.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
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
                bf.setInstitution(bill.getInstitution());
            }

            double d = 0;
            if (bill.getPatient().getPerson().isForeigner()) {
                bf.setFeeValue(f.getFfee());
                bf.setFeeGrossValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
                bf.setFeeGrossValue(f.getFee());
            }

//            priceMatrix = priceMatrixController.fetchChannellingMemberShipDiscount(paymentMethod, paymentScheme, selectedSessionInstance.getOriginatingSession().getCategory());
            if (priceMatrix != null) {
                if (f.getFeeType() == FeeType.OwnInstitution) {
                    d = bf.getFeeValue() * (priceMatrix.getDiscountPercent() / 100);
                    bf.setFeeDiscount(d);
                } else if (f.getFeeType() == FeeType.Staff) {
                    bf.setFeeDiscount(0.0);
                } else {
                    bf.setFeeDiscount(0.0);
                }

                bf.setFeeGrossValue(bf.getFeeGrossValue());
                bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                tmpDiscount += d;
            }

            tmpGrossTotal += bf.getFeeGrossValue();
            tmpTotal += bf.getFeeValue();
            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }

        billItem.setDiscount(tmpDiscount);
        billItem.setNetValue(tmpTotal);
        getBillItemFacade().edit(billItem);

        return billFeeList;

    }

}

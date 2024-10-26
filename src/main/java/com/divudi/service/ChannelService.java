package com.divudi.service;

import com.divudi.bean.channel.BookingControllerViewScope;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.util.JsfUtil;
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
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Consultant;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
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

    @Inject
    private BookingControllerViewScope bookingControllerViewScope;

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

    public void saveSelected(Patient p) {
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

    public Bill saveBilledBill(boolean forReservedNumbers, Patient patient, SessionInstance session, String refNo, WebUser user, Institution creditCompany) {
        saveSelected(patient);
        Bill savingBill = createBill(patient, session);
        BillItem savingBillItemForSession = createSessionItem(savingBill, refNo, session);
        savingBill.setAgentRefNo(refNo);   
        savingBill.setCreditCompany(creditCompany);
       
//        PriceMatrix priceMatrix;
//        if (itemsAddedToBooking != null || itemsAddedToBooking.isEmpty()) {
//            for (Item ai : itemsAddedToBooking) {
//                BillItem aBillItem = createAdditionalItem(savingBill, ai);
//                additionalBillItems.add(aBillItem);
//            }
//        }
        BillSession savingBillSession;

        savingBillSession = createBillSession(savingBill, savingBillItemForSession, forReservedNumbers, session);
        System.out.println(savingBillSession);

        List<BillFee> savingBillFees = new ArrayList<>();

//        priceMatrix = priceMatrixController.fetchChannellingMemberShipDiscount(paymentMethod, paymentScheme, selectedSessionInstance.getOriginatingSession().getCategory());
////        System.out.println("priceMatrix = " + priceMatrix);
        List<BillFee> savingBillFeesFromSession = createBillFeeForSessions(savingBill, savingBillItemForSession, false, null);

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
        savingBillItemForSession.setBillSession(savingBillSession);

        getBillSessionFacade().edit(savingBillSession);
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItemForSession);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);
        // savingBill.setCashPaid(cashPaid);
        // savingBill.setCashBalance(cashBalance);
        savingBill.setBalance(savingBill.getNetTotal());

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
        savingBill.setSingleBillItem(savingBillItemForSession);
        savingBill.setSingleBillSession(savingBillSession);
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

        calculateBillTotalsFromBillFees(savingBill, savingBillFees);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBill;
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
    
    public Institution findCreditCompany(String code, InstitutionType type){
    
        String jpql = "Select i from Institution i where i.retired = false "
                + " and i.code = :code "
                + " and i.institutionType = :type";
        
        Map params = new HashMap();
        params.put("code", code);
        params.put("type", type);
        
        return institutionFacade.findFirstByJpql(jpql, params);      
    }

    private Bill createBill(Patient patient, SessionInstance session) {
        Bill bill = new BilledBill();
        bill.setStaff(session.getOriginatingSession().getStaff());      
        //bill.setToStaff(toStaff);
        bill.setAppointmentAt(session.getSessionDate());
        bill.setTotal(session.getOriginatingSession().getTotal());
        bill.setNetTotal(session.getOriginatingSession().getTotal());
        bill.setPaymentMethod(PaymentMethod.OnCall);
        bill.setPatient(patient);

        bill.setBillType(BillType.ChannelOnCall);
        bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        System.out.println(bill);
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(session.getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
            bill.setBookingId(billNumberBean.bookingIdGenerator(session.getInstitution(), new BilledBill()));
            bill.setPaidAmount(session.getOriginatingSession().getTotal());
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
        // bill.setCreater(getSessionController().getLoggedUser());
        // bill.setDepartment(getSessionController().getDepartment());
        //bill.setInstitution(session.getInstitution());

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

        if (forReservedNumbers) {
//            // Pass the selectedReservedBookingNumber to the service method
//            count = serviceSessionBean.getNextAvailableReservedNumber(session, reservedNumbers, selectedReserverdBookingNumber);
//            if (count == null) {
//                count = serviceSessionBean.getNextNonReservedSerialNumber(session, reservedNumbers);
//                JsfUtil.addErrorMessage("No reserved numbers available. Normal number is given");
//            }
        } else {
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

    public List<Bill> findBillFromRefNo(String refNo, Institution creditCompany) {
        String jpql = "Select b from Bill b "
                + " where b.creditcompany = :cc"
                + " and b.agentRefNo = :ref"
                + " and b.cancelled = false"
                + " and b.retired = false";

        
        Map params = new HashMap();
        params.put("ref", refNo);
        params.put("cc", creditCompany);
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

    public List<Bill> viewBookingHistorybyDate(String fromDate, String toDate, Institution cc) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = formatter.parse(fromDate);
            Date endDate = formatter.parse(toDate);
            Bill b = new Bill();
            b.getBillTypeAtomic();

            BillTypeAtomic billType = BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT;
            String jpql = "Select b from Bill b "
                    + " where b.billDate between :fd And :td "
                    + " b.creditCompany = :cc " 
                    + " and b.billTypeAtomic = :bt";

            Map params = new HashMap();
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
        bill.getSingleBillSession().getBill().setCancelled(true);
        bs.getBill().setCancelledBill(cb);
        getBillFacade().edit(bs.getBill());
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

            bf.setCreatedAt(new Date());
            //   bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

    private CancelledBill createCancelBill1(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
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
    
    public List<SessionInstance> findSessionInstancesForDoctor(Institution institution, Long id){
        String jpql = "Select i from SessionInstance i where i.retired = false"
                + " and i.originatingSession.institution=:ins "
                + " and i.cancelled = false and i.completed = false "
                + " and i.originatingSession.staff.id = :id";
        
        Map params = new HashMap();
        params.put("ins", institution);
        params.put("id", id);
        return sessionInstanceFacade.findByJpql(jpql, params);
        
    }
    
    public Institution findInstitutionFromId(Long id) {
        String jpql = "Select i from Institution i where i.retired = false and i.id = :id";
        Map params = new HashMap();
        params.put("id", id);
        return institutionFacade.findFirstByJpql(jpql, params);
    }

    public Speciality findSpecilityFromId(Long id) {
        String jpql = "select s from Speciality s where s.retired=:ret and s.id=:id";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("id", id);
        return specialityFacade.findFirstByJpql(jpql, params);
    }

    public Consultant findConsultantFromName(String name) {
        StringBuffer jpql = new StringBuffer("select c from Consultant c where c.retired=:ret");
        Map m = new HashMap();
        m.put("ret", false);
        
        if(name != null && !name.isEmpty()){
            jpql.append(" and c.person.name like :name");
            m.put("name", name);
        }

        return consultantFacade.findFirstByJpql(jpql.toString(), m);
    }

    public Bill settleCredit(BillSession preBillSession, String refNo) {
        Bill paidBill = savePaidBill(preBillSession, refNo);
        BillItem paidBillItem = savePaidBillItem(paidBill, preBillSession);
        savePaidBillFee(paidBill, paidBillItem, preBillSession);
        BillSession paidBillSession = savePaidBillSession(paidBill, paidBillItem, preBillSession);
        preBillSession.setPaidBillSession(paidBillSession);
        getBillSessionFacade().edit(paidBillSession);
        getBillSessionFacade().edit(preBillSession);

        preBillSession.getBill().setPaidAmount(paidBill.getPaidAmount());
        preBillSession.getBill().setBalance(0.0);
        preBillSession.getBill().setPaidBill(paidBill);

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

    private Bill savePaidBill(BillSession bs, String refNo) {
        Bill temp = new BilledBill();
        temp.setAgentRefNo(refNo);
        temp.copy(bs.getBill());
        temp.copyValue(bs.getBill());
        temp.setPaidAmount(bs.getBill().getNetTotal());
        temp.setBalance(0.0);
        temp.setPaymentMethod(PaymentMethod.OnlineSettlement);
        temp.setReferenceBill(bs.getBill());
        temp.setBillType(BillType.ChannelPaid);
        temp.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(bs.getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        // String deptId = generateBillNumberDeptId(temp);
        System.out.println(deptId);
        temp.setInsId(deptId);
        temp.setDeptId(deptId);
        temp.setBookingId(deptId);
        temp.setDepartment(bs.getDepartment());
        temp.setInstitution(bs.getInstitution());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setCreatedAt(new Date());
        // temp.setCreater(getSessionController().getLoggedUser());

        getBillFacade().create(temp);

        return temp;
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

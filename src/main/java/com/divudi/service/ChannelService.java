package com.divudi.service;

import com.divudi.bean.channel.ChannelReportController;
import com.divudi.bean.channel.OnlineBookingAgentController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.ApiKeyType;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.OnlineBookingStatus;
import com.divudi.core.data.PaymentMethod;
import static com.divudi.core.data.PaymentMethod.Agent;
import static com.divudi.core.data.PaymentMethod.Card;
import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.Credit;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.ServiceType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillSession;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.OnlineBooking;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.ServiceSession;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.facade.ApiKeyFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillSessionFacade;
import com.divudi.core.facade.ConsultantFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFeeFacade;
import com.divudi.core.facade.OnlineBookingFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.ServiceSessionFacade;
import com.divudi.core.facade.SessionInstanceFacade;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.google.common.collect.HashBiMap;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import javax.transaction.Transactional;
import com.divudi.service.WebSocketService;

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
    @EJB
    BillService billService;
    @EJB
    private WebUserFacade webUserFacade;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    private static final Logger LOGGER = Logger.getLogger(ChannelService.class.getName());

    @PermitAll //TODO: Fix this to appropriate roles .
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
            b.setRetireComments("Online Booking is NOT completed.");
            b.setRetiredAt(new Date());
            billFacade.edit(b);

            b.getSingleBillSession().setRetired(true);
            b.getSingleBillSession().setRetireComments("Online Booking is NOT completed.");
            b.getSingleBillSession().setRetiredAt(new Date());
            billSessionFacade.edit(b.getSingleBillSession());

            b.getSingleBillItem().setRetired(true);
            b.getSingleBillItem().setRetireComments("Online Booking is NOT completed.");
            billItemFacade.edit(b.getSingleBillItem());

            b.getOnlineBooking().setRetired(true);
            b.getOnlineBooking().setRetiredAt(new Date());
            b.getOnlineBooking().setRetireComments("Online Booking is NOT completed.");
            getOnlineBookingFacade().edit(b.getOnlineBooking());

            try {
                WebSocketService.broadcastToSessions("Temporary Booking Retired - " + b.getSingleBillSession().getSessionInstance().getId());
            } catch (Exception e) {
                LOGGER.severe("Web socket communication error at retire method " + e.getMessage());
            }

            if (b.getBillFees() != null) {
                for (BillFee bf : b.getBillFees()) {
                    bf.setRetired(true);
                    bf.setRetireComments("Online Booking is NOT completed.");
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

    public Map nextAvailableAppoinmentNumberForSession(SessionInstance session) {
        BillType[] billTypes = {
            BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelCredit,
            BillType.ChannelResheduleWithPayment,
            BillType.ChannelResheduleWithOutPayment,};

        BillTypeAtomic[] billTypeAtomics = {
            BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT,
            BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT
        };

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
        //hh.put("bta", Arrays.asList(billTypeAtomics));
        hh.put("class", BilledBill.class);
        hh.put("ss", session);
        List<BillSession> billSessionList = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

        int nextNumber = 0;
        int activePatientCount = 0;

        if (billSessionList != null && !billSessionList.isEmpty()) {
            for (BillSession bs : billSessionList) {
                if (bs.getBill().getBillTypeAtomic() != BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT) {
                    if (!bs.getBill().isCancelled()) {
                        activePatientCount++;
                    }
                }
                if (Integer.parseInt(bs.getBill().getSingleBillSession().getSerialNoStr()) > nextNumber) {
                    nextNumber = Integer.parseInt(bs.getBill().getSingleBillSession().getSerialNoStr());
                }
            }
        }

        Map data = new HashMap();
        data.put("nextNumber", ++nextNumber);
        data.put("activePatients", activePatientCount);

        return data;

    }

    public Map getForeignFeesForDoctorAndInstitutionFromServiceSession(ServiceSession ss) {

        String sql = "Select fee From ItemFee fee "
                + " where fee.retired = false "
                + " and fee.serviceSession = :ss ";

        Map params = new HashMap<>();
        params.put("ss", ss);

        List<ItemFee> itemFeeList = itemFeeFacade.findAggregates(sql, params);

        double docForeignFee = 0;
        double hosForeignFee = 0;

        for (ItemFee f : itemFeeList) {
            if (f.getFeeType() == FeeType.OwnInstitution && f.getFee() > 0) {
                hosForeignFee += f.getFfee();
            } else if (f.getFeeType() == FeeType.Staff && f.getFee() > 0) {
                docForeignFee += f.getFfee();
            }
        }

        Map<String, Double> fees = new HashMap<>();

        fees.put("docForeignFee", docForeignFee);
        fees.put("hosForeignFee", hosForeignFee);

        return fees;

//        ItemFee fee = new ItemFee();
//        fee.isRetired();
//        fee.getServiceSession();
//        fee.getFeeType();
//        fee.getName();
    }

    public Map getLocalFeesForDoctorAndInstitutionFromServiceSession(ServiceSession ss) {

        String sql = "Select fee From ItemFee fee "
                + " where fee.retired = false "
                + " and fee.serviceSession = :ss ";

        Map params = new HashMap<>();
        params.put("ss", ss);

        List<ItemFee> itemFeeList = itemFeeFacade.findAggregates(sql, params);

        double docFee = 0;
        double hosFee = 0;

        for (ItemFee f : itemFeeList) {
            if (f.getFeeType() == FeeType.OwnInstitution && f.getFee() > 0) {
                hosFee += f.getFee();
            } else if (f.getFeeType() == FeeType.Staff && f.getFee() > 0) {
                docFee += f.getFee();
            }
        }

        Map<String, Double> fees = new HashMap<>();

        fees.put("docFee", docFee);
        fees.put("hosFee", hosFee);

        return fees;

    }
    @EJB
    private OnlineBookingFacade onlineBookingFacade;

    public void updateAndSaveOnlineBooking(OnlineBooking newBooking, SessionInstance session) {
        newBooking.setHospital(session.getInstitution());
        newBooking.setDepartment(session.getDepartment());
        newBooking.setPaid(false);
        newBooking.setOnlineBookingStatus(OnlineBookingStatus.PENDING);

        if (newBooking.getId() == null) {
            onlineBookingFacade.create(newBooking);
        } else {
            onlineBookingFacade.edit(newBooking);
        }
    }

    public Bill addToReserveAgentBookingThroughApi(boolean forReservedNumbers, OnlineBooking newBooking, SessionInstance session, String refNo, WebUser user, Institution creditCompany) {
        //saveOrUpdatePatientDetails(patient);
        updateAndSaveOnlineBooking(newBooking, session);
        Bill savingTemporaryBill = createAgentInitialBookingBill(newBooking, session);
        if (savingTemporaryBill == null) {
            return null;
        }
        BillItem savingBillItemForSession = createSessionItem(savingTemporaryBill, refNo, session);
        savingTemporaryBill.setAgentRefNo(refNo);
        savingTemporaryBill.setCreditCompany(creditCompany);

        BillSession savingTemporaryBillSession = createBillSession(savingTemporaryBill, savingBillItemForSession, forReservedNumbers, session);

        List<BillFee> savingBillFees = new ArrayList<>();

        List<BillFee> savingBillFeesFromSession = createBillFeeForSessions(savingTemporaryBill, savingBillItemForSession, false, null);

        List<BillFee> savingBillFeesFromAdditionalItems = new ArrayList<>();

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

        newBooking.setHospitalFee(savingTemporaryBill.getHospitalFee());
        newBooking.setDoctorFee(savingTemporaryBill.getStaffFee());

        savingTemporaryBill.setBalance(savingTemporaryBill.getNetTotal());
        calculateBillTotalsFromBillFees(savingTemporaryBill, savingBillFees);

        getBillFacade().edit(savingTemporaryBill);
        getBillSessionFacade().edit(savingTemporaryBillSession);
        getOnlineBookingFacade().edit(newBooking);

        return savingTemporaryBill;
    }

    public OnlineBookingFacade getOnlineBookingFacade() {
        return onlineBookingFacade;
    }

    public void setOnlineBookingFacade(OnlineBookingFacade onlineBookingFacade) {
        this.onlineBookingFacade = onlineBookingFacade;
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
        double calculatingGrossBillTotal = 0.0;
        double calculatingNetBillTotal = 0.0;

        for (BillFee iteratingBillFee : billfeesAvailable) {
            if (iteratingBillFee.getFee() == null) {
                continue;
            }

            calculatingGrossBillTotal += iteratingBillFee.getFeeGrossValue();
            calculatingNetBillTotal += iteratingBillFee.getFeeValue();

        }
        billToCaclculate.setDiscount(calculatingGrossBillTotal - calculatingNetBillTotal);
        billToCaclculate.setNetTotal(calculatingNetBillTotal);
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

    public List<Bill> fetchOnlineBookingsAgentPaidToHospitalBills(Date fromDate, Date toDate, Institution hospital, Institution agent, String billStatus) {

        String sql = "select bill from Bill bill "
                + " where bill.retired = :ret "
                + " and bill.toInstitution = :toIns"
                + " and bill.createdAt between :fromDate and :toDate";

        Map params = new HashMap();

        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("ret", false);
        params.put("toIns", hospital);

        if (agent != null) {
            sql += " and bill.fromInstitution = :fromIns";
            params.put("fromIns", agent);
        }
        if (billStatus != null && !billStatus.isEmpty()) {
            switch (billStatus) {
                case "Completed":
                    sql += " and bill.billType = :type";
                    params.put("type", BillType.ChannelOnlineBookingAgentPaidToHospital);
                    break;

                case "Cancelled":
                    sql += " and bill.billType = :type";
                    params.put("type", BillType.ChannelOnlineBookingAgentPaidToHospitalBillCancellation);
                    break;

                default:
                    sql += " and bill.billType in :type";
                    BillType[] bts = {BillType.ChannelOnlineBookingAgentPaidToHospital, BillType.ChannelOnlineBookingAgentPaidToHospitalBillCancellation};
                    params.put("type", Arrays.asList(bts));
                    break;
            }
        }

        sql += " order by bill.createdAt desc";

        return getBillFacade().findByJpql(sql, params, TemporalType.TIMESTAMP);
    }

    public List<OnlineBooking> fetchOnlineBookings(Date fromDate, Date toDate, Institution agent, Institution hospital, boolean paid, List<OnlineBookingStatus> status) {
        String sql = "Select ob from OnlineBooking ob"
                + " where ob.onlineBookingStatus in :status "
                + " and ob.retired = :retire "
                + " and ob.paidToHospital = :paid "
                + " and ob.createdAt between :from and :to";

        Map params = new HashMap<>();
        params.put("status", status);
        params.put("retire", false);
        params.put("from", fromDate);
        params.put("to", toDate);
        params.put("paid", paid);

        if (agent != null) {
            sql += " and ob.agency = :agent";
            params.put("agent", agent);
        }
        if (hospital != null) {
            sql += " and ob.hospital = :hospital";
            params.put("hospital", hospital);
        }

        sql += " order by ob.createdAt desc";

        List<OnlineBooking> list = getOnlineBookingFacade().findByJpql(sql, params, TemporalType.TIMESTAMP);
        List<OnlineBooking> listNew = new ArrayList<>();

        for (OnlineBooking ob : list) {
            if (ob.getOnlineBookingStatus() == OnlineBookingStatus.DOCTOR_CANCELED) {
                if (findBillFromOnlineBooking(ob).getPaidBill() != null && findBillFromOnlineBooking(ob).getPaidBill().getCancelledBill().getPaymentMethod() != PaymentMethod.OnlineBookingAgent) {
                    listNew.add(ob);
                }
            } else {
                listNew.add(ob);
            }
        }

        return listNew;
    }

    public List<OnlineBooking> fetchAllOnlineBookings(Date fromDate, Date toDate, Institution agent, Institution hospital, Boolean paid, List<OnlineBookingStatus> status) {
        String sql = "Select ob from OnlineBooking ob"
                + " where ob.onlineBookingStatus in :status "
                + " and ob.retired = :retire "
                + " and ob.createdAt between :from and :to";

        Map params = new HashMap<>();
        params.put("status", status);
        params.put("retire", false);
        params.put("from", fromDate);
        params.put("to", toDate);

        if (agent != null) {
            sql += " and ob.agency = :agent";
            params.put("agent", agent);
        }
        if (hospital != null) {
            sql += " and ob.hospital = :hospital";
            params.put("hospital", hospital);
        }
        if (paid != null) {
            sql += " and ob.paidToHospital = :paid ";
            params.put("paid", paid);
        }

        sql += " order by ob.createdAt desc";

        return getOnlineBookingFacade().findByJpql(sql, params, TemporalType.TIMESTAMP);
    }

    public Institution findCreditCompany(String code, String name, InstitutionType type) {
        Map params = new HashMap();

        String jpql = "Select i from Institution i where i.retired = false "
                + " and UPPER(i.code) = UPPER(:code) "
                + " and i.institutionType = :type";

        if (name != null && !name.isEmpty()) {
            jpql += " and i.name = :name ";
            params.put("name", name.trim());
        }

        params.put("code", code);
        params.put("type", type);

        return institutionFacade.findFirstByJpql(jpql, params);
    }

    private Bill createAgentInitialBookingBill(OnlineBooking newBooking, SessionInstance session) {
        Bill bill = new BilledBill();
        bill.setStaff(session.getOriginatingSession().getStaff());
        //bill.setToStaff(toStaff);
        bill.setAppointmentAt(session.getSessionDate());

        if (newBooking.isForeignStatus()) {
            bill.setTotal(session.getOriginatingSession().getTotalFfee());
            bill.setNetTotal(session.getOriginatingSession().getTotalFfee());
        } else {
            bill.setTotal(session.getOriginatingSession().getTotal());
            bill.setNetTotal(session.getOriginatingSession().getTotal());
        }

        bill.setPaymentMethod(PaymentMethod.OnCall);
        bill.setOnlineBooking(newBooking);
        bill.setPaid(false);
        bill.setPaidAmount(0.0);
        bill.setPaidBill(null);
        bill.setBillType(BillType.ChannelOnCall);
        bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
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
        bill.setDepartment(session.getDepartment());
        bill.setToInstitution(session.getInstitution());
        bill.setInstitution(session.getInstitution());

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

    //this is for physical agentBookings not for online bookings
    public boolean checkDuplicateAgentRefNo(Institution creditCompany, String refNo) {

        Map params = new HashMap();
        params.put("type", BillType.ChannelAgent);
        params.put("bta", BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
        params.put("retire", false);
        params.put("refNo", refNo);

        StringBuilder sql = new StringBuilder("Select count(bill) from Bill bill where "
                + " bill.billType = :type and bill.billTypeAtomic = :bta and bill.agentRefNo = :refNo and"
                + " bill.retired = :retire");

        if (creditCompany != null) {
            sql.append(" and bill.creditCompany = :company");
            params.put("company", creditCompany);
        }

        Long count = getBillFacade().countByJpql(sql.toString(), params);

        if (count != null && count > 0) {
            return true;
        }

        return false;
    }

    private BillItem createSessionItem(Bill bill, String refNo, SessionInstance session) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(refNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setItem(session.getOriginatingSession());
        bi.setQty(1.0);
        bi.setSessionDate(session.getSessionAt());

        if (bill.getOnlineBooking() != null && bill.getOnlineBooking().isForeignStatus()) {
            bi.setGrossValue(session.getOriginatingSession().getTotalFfee());
            bi.setNetRate(session.getOriginatingSession().getTotalFfee());
            bi.setNetValue(session.getOriginatingSession().getTotalFfee());
            bi.setRate(session.getOriginatingSession().getTotalFfee());
        } else {
            bi.setGrossValue(session.getOriginatingSession().getTotal());
            bi.setNetRate(session.getOriginatingSession().getTotal());
            bi.setNetValue(session.getOriginatingSession().getTotal());
            bi.setRate(session.getOriginatingSession().getTotal());
        }

        billItemFacade.create(bi);
        return bi;
    }

    private List<BillSession> getAllBillSessionForSessionInstance(SessionInstance ss) {
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

        if (ss.getNextAvailableAppointmentNumber() != null) {
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

    public void fillBillSessionsAndUpdateBookingsCountInSessionInstance(SessionInstance session) {

        if (session == null) {
            return;
        }

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
                + " and bs.bill.billTypeAtomic != :bta"
                + " and bs.sessionInstance=:ss "
                + " order by bs.serialNo ";
        HashMap<String, Object> hh = new HashMap<>();

        Bill b = new Bill();
        b.getBillTypeAtomic();
        hh.put("bts", bts);
        hh.put("bta", BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);
        hh.put("class", BilledBill.class);
        hh.put("ss", session);

        List<BillSession> billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

        // Initialize counts
        long bookedPatientCount = 0;
        long paidPatientCount = 0;
        long completedPatientCount = 0;
        long cancelPatientCount = 0;
        long refundedPatientCount = 0;
        long onCallPatientCount = 0;
        long reservedBookingCount = 0;
        long sessionStartingNumber = 0;
        long nextAvailableAppointmentNumber = 0;

        if (session.getOriginatingSession()
                .getSessionStartingNumber() != null
                && !session.getOriginatingSession().getSessionStartingNumber().trim().equals("")) {

            int ssn = Integer.parseInt(session.getOriginatingSession().getSessionStartingNumber().trim());
            sessionStartingNumber = ssn;
        } else {
            sessionStartingNumber = 1; // Use 1 instead of 01 since it's an integer
        }

        if (billSessions == null) {
            session.setBookedPatientCount(0l);
            session.setPaidPatientCount(0l);
            session.setCompletedPatientCount(0l);
            session.setRemainingPatientCount(0l);
            session.setNextAvailableAppointmentNumber(sessionStartingNumber);
            sessionInstanceFacade.edit(session);
            return;
        }
        List<Integer> serialnumbersBySelectedSessionInstance = new ArrayList<>();
        // Loop through billSessions to calculate counts
        for (BillSession bs : billSessions) {
            if (bs != null) {
                bookedPatientCount++; // Always increment if bs is not null
                serialnumbersBySelectedSessionInstance.add(bs.getSerialNo());
                // Additional check for reserved status
                try {
                    if (bs.isReservedBooking()) {
                        reservedBookingCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking completion status

                }

                // Additional check for completion status
                try {
                    if (bs.isCompleted()) {
                        completedPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking completion status

                }

                // Additional check for paid status
                try {
                    if (bs.getBill().getBillTypeAtomic() == BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT) {
                        paidPatientCount++;
                    }
                    if (bs.getPaidBillSession() != null) {
                        paidPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }
                // Additional check for cancel status
                try {
                    if (bs.getBill().isCancelled()) {
                        cancelPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }

                // Additional check for refund status
                try {
                    if (bs.getBill().isRefunded()) {
                        refundedPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }

                // Additional check for Oncall status
                try {
                    if (bs.getPaidBillSession() == null && !bs.getBill().isCancelled() && bs.getBill().getBillTypeAtomic() != BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT) {
                        onCallPatientCount++;
                    }
                } catch (NullPointerException npe) {
                    // Log or handle the fact that there was an NPE checking paid status

                }
            }
        }

        // Set calculated counts to selectedSessionInstance
        session.setBookedPatientCount(bookedPatientCount);

        session.setPaidPatientCount(paidPatientCount);

        session.setCompletedPatientCount(completedPatientCount);

        session.setCancelPatientCount(cancelPatientCount);

        session.setRefundedPatientCount(refundedPatientCount);

        session.setOnCallPatientCount(onCallPatientCount);

        session.setReservedBookingCount(reservedBookingCount);

        session.setNextAvailableAppointmentNumber(generateNextAvailableAppointmentNumberBySessionInstance(session, serialnumbersBySelectedSessionInstance));

        // Assuming remainingPatientCount is calculated as booked - completed
        session.setRemainingPatientCount(bookedPatientCount
                - completedPatientCount);
        sessionInstanceFacade.edit(session);
    }

    public long generateNextAvailableAppointmentNumberBySessionInstance(SessionInstance ssi, List<Integer> serialNumberArray) {
        long nextAvailable = 0;

        if (ssi == null || serialNumberArray == null) {
            return nextAvailable;
        }

        List<Integer> reservedNumbersBySessionInstance = CommonFunctions.convertStringToIntegerList(ssi.getReserveNumbers());

        if (reservedNumbersBySessionInstance != null && !reservedNumbersBySessionInstance.isEmpty()) {
            serialNumberArray.removeAll(reservedNumbersBySessionInstance);
        }

        int maxNumber = 0;
        if (!serialNumberArray.isEmpty()) {
            maxNumber = serialNumberArray.stream().max(Integer::compareTo).orElse(0);
        }

        nextAvailable = maxNumber + 1;

        while (reservedNumbersBySessionInstance.contains((int) nextAvailable)) {
            nextAvailable++;
        }

        return nextAvailable;
    }

    private BillSession createBillSession(Bill bill, BillItem billItem, boolean forReservedNumbers, SessionInstance session) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setReservedBooking(forReservedNumbers);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setDepartment(session.getOriginatingSession().getDepartment());
        bs.setInstitution(session.getOriginatingSession().getInstitution());
        bs.setItem(session.getOriginatingSession());
        bs.setServiceSession(session.getOriginatingSession());
        bs.setSessionInstance(session);
        bs.setSessionDate(session.getSessionDate());
        bs.setSessionTime(session.getSessionTime());
        bs.setStaff(session.getStaff());

        // List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(session.getOriginatingSession().getReserveNumbers());
        Integer count = null;

        List<Integer> availableReleasedApoinmentNumbers = getReleasedAppoinmentNumbersForApiBookings(session);
        Random rand = new Random();
        if (availableReleasedApoinmentNumbers != null && !availableReleasedApoinmentNumbers.isEmpty()) {
            count = availableReleasedApoinmentNumbers.get(rand.nextInt(availableReleasedApoinmentNumbers.size()));
        }

        if (count == null) {
            count = serviceSessionBean.getNextNonReservedSerialNumber(session, Collections.EMPTY_LIST);
        }

        if (count != null) {
            bs.setSerialNo(count);
        } else {
            bs.setSerialNo(1);
        }
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

    public OnlineBooking editOnlineBooking(OnlineBooking bookingForEdit, String phoneNo, String title, String patientName, String nic, String email, String address) {
        bookingForEdit.setPhoneNo(phoneNo);
        bookingForEdit.setPatientName(patientName);
        bookingForEdit.setTitle(title);
        bookingForEdit.setNic(nic);
        bookingForEdit.setEmail(email);
        bookingForEdit.setAddress(address);

        getOnlineBookingFacade().edit(bookingForEdit);

        return bookingForEdit;
    }

    public Bill findBillFromOnlineBooking(OnlineBooking booking) {
        String sql = "select bill from Bill bill"
                + " where bill.onlineBooking = :ob"
                + " and bill.retired = false";

        Map params = new HashMap();
        params.put("ob", booking);

        return getBillFacade().findFirstByJpql(sql, params);

    }

    public OnlineBooking findOnlineBookingFromRefNo(String refNo, boolean retiredStatus, Institution agency) {

        String sql = "Select ob from OnlineBooking ob "
                + " where ob.referenceNo  = :refNo "
                + " and ob.retired = :retired "
                + " and ob.agency = :agency "
                + " and ob.bill is not null";

        Map params = new HashMap();
        params.put("refNo", refNo);
        params.put("agency", agency);
        params.put("retired", retiredStatus);

        return getOnlineBookingFacade().findFirstByJpql(sql, params);

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

    public List<Payment> fetchCardPaymentsFromChannelIncome(Date fromDate, Date toDate, Institution institution, String reportStatus) {
        String jpql = "Select p from Payment p where "
                + " p.bill.billType in :bt and p.bill.billTypeAtomic in :bta "
                + " and p.paymentMethod = :type"
                + " and p.bill.retired = false "
                + " and p.bill.createdAt between :fromDate and :toDate ";

        Map params = new HashMap();
        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelPaid);
        params.put("bt", bts);

        List<BillTypeAtomic> bta = new ArrayList<>();
        bta.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
        bta.add(BillTypeAtomic.CHANNEL_PAYMENT_FOR_BOOKING_BILL);

        if (reportStatus != null && reportStatus.equalsIgnoreCase("Details")) {
            bta.add(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT);
        }

        params.put("bta", bta);
        params.put("type", PaymentMethod.Card);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            jpql += "and p.bill.institution = :ins";
            params.put("ins", institution);
        }

        jpql += " order by p.bill.createdAt desc";

        List<Payment> list = paymentFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        return list;

    }

    public ChannelReportController.WrapperDtoForChannelFutureIncome fetchChannelIncomeByUser(Date fromDate, Date toDate, Institution institution, WebUser user, List<Category> categoryList, String reportStatus, String paidStatus) {
        String sql = "select new com.divudi.bean.channel.ChannelReportController.ChannelIncomeDetailDto(bs.id, "
                + "bill.id, "
                + "session.sessionDate, "
                + "bill.createdAt, "
                + "bill.creater.name, "
                + "person.name, "
                + "person.phone, "
                + "bill.paymentMethod, "
                + "COALESCE(bill.staffFee, 0), "
                + "COALESCE(bill.hospitalFee, 0), "
                + "COALESCE(bill.netTotal, 0), "
                + "bill.comments, "
                + "bill.cancelled, "
                + "bill.refunded ) "
                + "from BillSession bs "
                + "join bs.bill bill "
                + "join bs.sessionInstance session "
                + "join bill.patient patient "
                + "left join patient.person person "
                + "where bs.createdAt between :fromDate and :todate "
                + "and bill.billTypeAtomic in :bta "
                + "and bill.billType <> :bt ";

        List<BillTypeAtomic> btaList = new ArrayList<>();

        btaList.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
        btaList.add(BillTypeAtomic.CHANNEL_PAYMENT_FOR_BOOKING_BILL);
        btaList.add(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT);
        btaList.add(BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT);

        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", fromDate);
        params.put("todate", toDate);
        params.put("bta", btaList);
        params.put("bt", BillType.ChannelAgent);

        if (user != null) {
            sql += "and bill.creater = :user ";
            params.put("user", user);
        }

        if (institution != null) {
            sql += "and bill.institution = :ins ";
            params.put("ins", institution);
        }

        if (categoryList != null && !categoryList.isEmpty()) {
            System.out.println("line 1302");
            for (Category c : categoryList) {
                System.out.println(c.getName() + categoryList.size());
            }
            sql += "and session.originatingSession.category in :category ";
            params.put("category", categoryList);
        }

        sql += "order by bill.createdAt desc";

        List<ChannelReportController.ChannelIncomeDetailDto> dtoList = (List<ChannelReportController.ChannelIncomeDetailDto>) billSessionFacade.findLightsByJpql(sql, params, TemporalType.TIMESTAMP);

        if (dtoList == null || dtoList.isEmpty()) {
            return null;
        }

        ChannelReportController.WrapperDtoForChannelFutureIncome wrapperDto = new ChannelReportController.WrapperDtoForChannelFutureIncome();
        wrapperDto.setIncomeDtos(dtoList);
        wrapperDto.setProcessDate(new Date());

        if (reportStatus != null && reportStatus.equalsIgnoreCase("summery")) {

            List<ChannelReportController.ChannelIncomeSummeryDto> summeryDtoList = new ArrayList<>();

            for (ChannelReportController.ChannelIncomeDetailDto dto : dtoList) {
                if (summeryDtoList.isEmpty()) {
                    ChannelReportController.ChannelIncomeSummeryDto summery1 = new ChannelReportController.ChannelIncomeSummeryDto();
                    summery1.setAppoimentDate(dto.getAppoinmentDate());
                    summeryDtoList.add(summery1);
                    fillPaymentsDataToDto(summeryDtoList, dto);
                    continue;
                } else {
                    fillPaymentsDataToDto(summeryDtoList, dto);
                }

            }
            wrapperDto.setSummeryDtos(summeryDtoList);

            for (ChannelReportController.ChannelIncomeSummeryDto summery : wrapperDto.getSummeryDtos()) {
                wrapperDto.setAllCashTotal(wrapperDto.getAllCashTotal() + summery.getCashTotal());
                wrapperDto.setAllCardTotal(wrapperDto.getAllCardTotal() + summery.getCardTotal());
                wrapperDto.setAllCreditTotal(wrapperDto.getAllCreditTotal() + summery.getCreditTotal());
                wrapperDto.setAllCancelTotal(wrapperDto.getAllCancelTotal() + summery.getCancelTotal());
                wrapperDto.setAllRefundTotal(wrapperDto.getAllRefundTotal() + summery.getRefundTotal());
                wrapperDto.setAllCancelAppoinments(wrapperDto.getAllCancelAppoinments() + summery.getTotalCancelAppoinments());
                wrapperDto.setAllRefundAppoinments(wrapperDto.getAllRefundAppoinments() + summery.getTotalRefundAppoinments());
                wrapperDto.setTotalValidAppoinments(wrapperDto.getTotalValidAppoinments() + summery.getTotalActiveAppoinments());
            }
        }

        return wrapperDto;

//        ChannelReportController.ChannelIncomeDetailDto dto = new ChannelReportController.ChannelIncomeDetailDto(0, fromDate, sql, sql, PaymentMethod.PatientDeposit, 0, 0, 0, sql);
//        BillSession bs = new BillSession();
//        bs.getSessionInstance().getOriginatingSession().getCategory();
//        bs.getBill().getPatient().getPerson().getSmsNumber();
//        bs.getBill().getHospitalFee();
//        bs.getBill().getStaffFee();
//        bs.getBill().getNetTotal();
//        bs.getBill().getComments();
//        bs.getBill().getInstitution();
//        bs.getBill().getCreater();
    }

    public void fillPaymentsDataToDto(List<ChannelReportController.ChannelIncomeSummeryDto> summeryDtoList, ChannelReportController.ChannelIncomeDetailDto dto) {

        boolean availableSummery = false;

        for (ChannelReportController.ChannelIncomeSummeryDto summeryDto : summeryDtoList) {
            if (dto.getAppoinmentDate().equals(summeryDto.getAppoimentDate())) {
                availableSummery = true;
                switch (dto.getPaymentMethod()) {
                    case Cash:
                        summeryDto.setCashTotal(summeryDto.getCashTotal() + dto.getTotalAppoinmentFee());
                        break;
                    case Card:
                        summeryDto.setCardTotal(summeryDto.getCardTotal() + dto.getTotalAppoinmentFee());
                        break;
                    case MultiplePaymentMethods:
                        Bill bill = billFacade.find(dto.getBillId());
                        List<Payment> payments = new ArrayList<>();
                        if (bill != null) {
                            payments = billService.fetchBillPayments(bill);
                        }
                        for (Payment p : payments) {
                            switch (p.getPaymentMethod()) {
                                case Cash:
                                    summeryDto.setCashTotal(summeryDto.getCashTotal() + p.getPaidValue());
                                    break;
                                case Card:
                                    summeryDto.setCardTotal(summeryDto.getCardTotal() + p.getPaidValue());
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    case Agent:
                        summeryDto.setAgentTotal(summeryDto.getAgentTotal() + dto.getTotalAppoinmentFee());
                        break;

                    case Credit:
                        summeryDto.setCreditTotal(summeryDto.getCreditTotal() + dto.getTotalAppoinmentFee());
                        break;

                    default:
                        break;
                }
                if (dto.isIsCancelled()) {
                    summeryDto.setTotalCancelAppoinments(summeryDto.getTotalCancelAppoinments() + 1);
                    summeryDto.setTotalActiveAppoinments(summeryDto.getTotalActiveAppoinments() - 1);
                    summeryDto.setCancelTotal(summeryDto.getCancelTotal() + dto.getTotalAppoinmentFee());
                } else if (dto.isIsRefunded()) {
                    summeryDto.setTotalActiveAppoinments(summeryDto.getTotalActiveAppoinments() - 1);
                    summeryDto.setTotalRefundAppoinments(summeryDto.getTotalRefundAppoinments() + 1);
                    summeryDto.setRefundTotal(summeryDto.getRefundTotal() + dto.getTotalAppoinmentFee());
                } else {

                    summeryDto.setTotalActiveAppoinments(summeryDto.getTotalActiveAppoinments() + 1);

                }
                summeryDto.setTotalDocFee(summeryDto.getTotalDocFee() + dto.getDoctorFee());
                summeryDto.setTotalHosFee(summeryDto.getTotalHosFee() + dto.getHosFee());
                summeryDto.setTotalAmount(summeryDto.getTotalAmount() + dto.getTotalAppoinmentFee());

            }

//            }else if (dto.getAppoinmentDate().equals(summeryDto.getAppoimentDate()) && (dto.isIsCancelled() || dto.isIsRefunded())) {
//                System.out.println("line 1405");
//                availableSummery = true;
//                switch (dto.getPaymentMethod()) {
//                    case Cash:
//                        summeryDto.setCashTotal(summeryDto.getCashTotal() - Math.abs(dto.getTotalAppoinmentFee()));
//                        break;
//                    case Card:
//                        summeryDto.setCardTotal(summeryDto.getCardTotal() - Math.abs(dto.getTotalAppoinmentFee()));
//                        break;
//                    case MultiplePaymentMethods:
//                        Bill bill = billFacade.find(dto.getBillId());
//                        List<Payment> payments = new ArrayList<>();
//                        if (bill != null) {
//                            payments = billService.fetchBillPayments(bill);
//                        }
//                        for (Payment p : payments) {
//                            switch (p.getPaymentMethod()) {
//                                case Cash:
//                                    summeryDto.setCashTotal(summeryDto.getCashTotal() - Math.abs(p.getPaidValue()));
//                                    break;
//                                case Card:
//                                    summeryDto.setCardTotal(summeryDto.getCardTotal() - Math.abs(p.getPaidValue()));
//                                    break;
//                                default:
//                                    break;
//                            }
//                        }
//                    case Agent:
//                        summeryDto.setAgentTotal(summeryDto.getAgentTotal() - Math.abs(dto.getTotalAppoinmentFee()));
//                        break;
//
//                    case Credit:
//                        summeryDto.setCreditTotal(summeryDto.getCreditTotal() - Math.abs(dto.getTotalAppoinmentFee()));
//                        break;
//
//                    default:
//                        break;
//                }
//                summeryDto.setTotalDocFee(summeryDto.getTotalDocFee() + dto.getDoctorFee());
//                summeryDto.setTotalHosFee(summeryDto.getTotalHosFee() + dto.getHosFee());
//                summeryDto.setTotalActiveAppoinments(summeryDto.getTotalActiveAppoinments() + 1);
//                summeryDto.setTotalAmount(summeryDto.getTotalAmount() + dto.getTotalAppoinmentFee());
//
//            }
        }

        if (!availableSummery) {
            ChannelReportController.ChannelIncomeSummeryDto newSummery = new ChannelReportController.ChannelIncomeSummeryDto();
            newSummery.setAppoimentDate(dto.getAppoinmentDate());

            switch (dto.getPaymentMethod()) {
                case Cash:
                    newSummery.setCashTotal(newSummery.getCashTotal() + dto.getTotalAppoinmentFee());
                    break;
                case Card:
                    newSummery.setCardTotal(newSummery.getCardTotal() + dto.getTotalAppoinmentFee());
                    break;
                case MultiplePaymentMethods:
                    Bill bill = billFacade.find(dto.getBillId());
                    List<Payment> payments = new ArrayList<>();
                    if (bill != null) {
                        payments = billService.fetchBillPayments(bill);
                    }
                    for (Payment p : payments) {
                        switch (p.getPaymentMethod()) {
                            case Cash:
                                newSummery.setCashTotal(newSummery.getCashTotal() + p.getPaidValue());
                                break;
                            case Card:
                                newSummery.setCardTotal(newSummery.getCardTotal() + p.getPaidValue());
                                break;
                            default:
                                break;
                        }
                    }
                    break;

                case Agent:
                    newSummery.setAgentTotal(newSummery.getAgentTotal() + dto.getTotalAppoinmentFee());
                    break;

                case Credit:
                    newSummery.setCreditTotal(newSummery.getCreditTotal() + dto.getTotalAppoinmentFee());
                    break;

                default:
                    break;
            }

            if (dto.isIsCancelled()) {
                newSummery.setTotalCancelAppoinments(newSummery.getTotalCancelAppoinments() + 1);
                newSummery.setTotalActiveAppoinments(newSummery.getTotalActiveAppoinments() - 1);
                newSummery.setCancelTotal(newSummery.getCancelTotal() + dto.getTotalAppoinmentFee());
            } else if (dto.isIsRefunded()) {
                newSummery.setTotalActiveAppoinments(newSummery.getTotalActiveAppoinments() - 1);
                newSummery.setTotalRefundAppoinments(newSummery.getTotalRefundAppoinments() + 1);
                newSummery.setRefundTotal(newSummery.getRefundTotal() + dto.getTotalAppoinmentFee());
            } else {

                newSummery.setTotalActiveAppoinments(newSummery.getTotalActiveAppoinments() + 1);

            }
            newSummery.setTotalDocFee(dto.getDoctorFee());
            newSummery.setTotalHosFee(dto.getHosFee());
            newSummery.setTotalAmount(dto.getTotalAppoinmentFee());

            summeryDtoList.add(newSummery);
        }
    }

    public List<Bill> fetchAgentDirectFundBills(SearchKeyword searchKeyword, Date fromDate, Date toDate, Institution institution) {

        String sql = "select bill from Bill bill where "
                + " bill.billType in :bt "
                + " and bill.retired = false"
                + " and bill.billTypeAtomic in :bta "
                + " and bill.createdAt BETWEEN :fd AND :td";

        Map params = new HashMap();
        params.put("fd", fromDate);
        params.put("td", toDate);
        List<BillType> btList = new ArrayList<>();
        btList.add(BillType.ChannelOnlineBookingAgentPaidToHospital);
        btList.add(BillType.ChannelOnlineBookingAgentPaidToHospitalBillCancellation);
        params.put("bt", btList);

        List<BillTypeAtomic> btaList = new ArrayList<>();
        btaList.add(BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_DIRECT_FUND_FOR_ONLINE_BOOKINGS_BILL);
        btaList.add(BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_DIRECT_FUND_FOR_ONLINE_BOOKINGS_BILL_CANCELLATION);
        params.put("bta", btaList);

        if (institution != null) {
            sql += " and bill.toInstitution = :ins ";
            params.put("ins", institution);
        }

        if (searchKeyword != null) {

            if (searchKeyword.getBillNo() != null && !searchKeyword.getBillNo().trim().equals("")) {
                sql += " and  ((bill.insId) like :billNo )";
                params.put("billNo", "%" + searchKeyword.getBillNo().trim().toUpperCase() + "%");
            }

            if (searchKeyword.getNetTotal() != null && !searchKeyword.getNetTotal().trim().equals("")) {
                sql += " and  ((bill.netTotal) = :netTotal )";
                params.put("netTotal", "%" + searchKeyword.getNetTotal().trim().toUpperCase() + "%");
            }

            if (searchKeyword.getFromInstitution() != null && !searchKeyword.getFromInstitution().trim().equals("")) {
                sql += " and  ((bill.fromInstitution.name) like :frmIns )";
                params.put("frmIns", "%" + searchKeyword.getFromInstitution().trim().toUpperCase() + "%");
            }

            if (searchKeyword.getNumber() != null && !searchKeyword.getNumber().trim().equals("")) {
                sql += " and  ((bill.fromInstitution.institutionCode) like :num )";
                params.put("num", "%" + searchKeyword.getNumber().trim().toUpperCase() + "%");
            }

        }

        sql += " order by bill.createdAt desc";

        List<Bill> list = billFacade.findByJpql(sql, params, TemporalType.TIMESTAMP);
        System.out.println("end " + list.size());

        return list;
    }

    public ReportTemplateRowBundle generateChannelIncomeSummeryForSessions(Date fromDate, Date toDate, Institution institution, Department department, Staff staff, String status, String reportStatus) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow("
                + "bill, "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Cash THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Card THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.MultiplePaymentMethods THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Staff THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Credit THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Staff_Welfare THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Voucher THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.IOU THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Agent THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Cheque THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.Slip THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.ewallet THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.PatientDeposit THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.PatientPoints THEN p.paidValue ELSE 0 END), "
                + "SUM(CASE WHEN p.paymentMethod = com.divudi.core.data.PaymentMethod.OnlineSettlement THEN p.paidValue ELSE 0 END)) "
                + "FROM Payment p "
                + "JOIN p.bill bill "
                + "WHERE p.retired <> :bfr AND bill.retired <> :br ";

        List<BillTypeAtomic> bts = new ArrayList<>();

        if (status != null && status.equalsIgnoreCase("Scanning")) {
            jpql += "and bill.singleBillSession.sessionInstance.originatingSession.category.name = :catogery ";
            parameters.put("catogery", "Scanning");

            bts = BillTypeAtomic.findByServiceType(ServiceType.CHANNELLING);
            bts.remove(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT_PENDING_ONLINE);

        } else if (status != null && status.equalsIgnoreCase("Agent")) {
            jpql += " and bill.billType = :type ";
            parameters.put("type", BillType.ChannelAgent);

            bts.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
            bts.add(BillTypeAtomic.CHANNEL_REFUND_WITH_PAYMENT);
            bts.add(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT);
        }

        parameters.put("bfr", true);
        parameters.put("br", true);

        //List<BillTypeAtomic> bts = BillTypeAtomic.findByServiceType(ServiceType.CHANNELLING);
        jpql += "AND bill.billTypeAtomic IN :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }
        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (staff != null) {
            jpql += "AND bill.staff = :stf ";
            parameters.put("stf", staff);
        }

        jpql += "AND p.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        // Ensure proper grouping
        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        bundle = new ReportTemplateRowBundle();
        bundle.setReportTemplateRows(rs);
        bundle.createRowValuesFromBill();
        bundle.calculateTotals();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            if (bundle.getLong1() != null) { //long1 for billTotals
                bundle.setLong1(bundle.getLong1() + (long) row.getBill().getTotal());
            } else {
                bundle.setLong1((long) row.getBill().getTotal());
            }

            if (bundle.getLong2() != null) { //long2 for hospitalfee totals
                bundle.setLong2(bundle.getLong2() + (long) row.getBill().getHospitalFee());
            } else {
                bundle.setLong2((long) row.getBill().getHospitalFee());
            }

            if (bundle.getLong3() != null) { //long3 for stafffee totals
                bundle.setLong3(bundle.getLong3() + (long) row.getBill().getStaffFee());
            } else {
                bundle.setLong3((long) row.getBill().getStaffFee());
            }
        }

        if (reportStatus != null && reportStatus.equalsIgnoreCase("Summery")) {
            List<ReportTemplateRow> newList = removeCancelAndREfundBillsFromDTO(bundle.getReportTemplateRows());
            bundle.setReportTemplateRows(newList);
            return bundle;
        } else {
            return bundle;
        }

    }

    public OnlineBookingAgentController.ChannelAnalyticDto generateChannelAnaliticsData(Date fromDate, Date toDate, Institution institution, Institution agency, OnlineBookingAgentController.ChannelAnalyticDto dto) {
        StringBuilder sql = new StringBuilder("select ob from OnlineBooking ob where "
                + " ob.onlineBookingStatus in :status "
                + " and ob.retired = :ret"
                + " and ob.agency = :agent"
                + " and ob.createdAt between :fromDate and :toDate ");

        Map<String, Object> params = new HashMap();
        List<OnlineBookingStatus> status = Arrays.stream(OnlineBookingStatus.values())
                .filter(stat -> stat != OnlineBookingStatus.PENDING)
                .collect(Collectors.toList());

        params.put("status", status);
        params.put("ret", false);
        params.put("agent", agency);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            sql.append(" and ob.hospital = :hos");
            params.put("hos", institution);
        }

        sql.append(" order by ob.createdAt desc");

        List<OnlineBooking> bookingList = onlineBookingFacade.findByJpql(sql.toString(), params, TemporalType.TIMESTAMP);

        if (bookingList == null || bookingList.isEmpty()) {
            return null;
        }

        double totalBookings;

        totalBookings = bookingList.size();

        double hospitalCancelBookings = 0;
        double agentCancelBookings = 0;
        double absentPatientBookings = 0;
        double activeBookings = 0;
        double completedBookings = 0;
        double totalEarningForOnlineBooking = 0;
        double totalAgencyDeposits = 0;
        double bookingsCancelByagentThroughHospital = 0;
        double remainAmountNeedForHospitalCancelAndRepaidBills = 0;
        double activeBookingEarning = 0;
        double absentBookingPaymentTotal = 0;

        for (OnlineBooking ob : bookingList) {
            if (ob.getOnlineBookingStatus() == OnlineBookingStatus.COMPLETED) {
                totalEarningForOnlineBooking += ob.getAppoinmentTotalAmount();
                completedBookings++;
            } else if (ob.getOnlineBookingStatus() == OnlineBookingStatus.ACTIVE) {
                activeBookingEarning += ob.getAppoinmentTotalAmount();
                activeBookings++;
            } else if (ob.getOnlineBookingStatus() == OnlineBookingStatus.DOCTOR_CANCELED) {
                if (ob.getBill().isCancelled()) {
                    if (ob.getBill().getCancelledBill().getPaymentMethod() == PaymentMethod.OnlineBookingAgent) {
                        bookingsCancelByagentThroughHospital++;
                    } else {
                        hospitalCancelBookings++;
                        remainAmountNeedForHospitalCancelAndRepaidBills += ob.getAppoinmentTotalAmount();
                    }
                }

            } else if (ob.getOnlineBookingStatus() == OnlineBookingStatus.PATIENT_CANCELED) {
                agentCancelBookings++;
            } else if (ob.getOnlineBookingStatus() == OnlineBookingStatus.ABSENT) {
                absentPatientBookings++;
                absentBookingPaymentTotal += ob.getAppoinmentTotalAmount();
            }
        }

        SearchKeyword searchKeyword = new SearchKeyword();
        searchKeyword.setFromInstitution(agency.getName());

        List<Bill> billList = fetchAgentDirectFundBills(searchKeyword, fromDate, toDate, institution);

        for (Bill b : billList) {
            if (b.getBillType() == BillType.ChannelOnlineBookingAgentPaidToHospital) {
                totalAgencyDeposits += b.getNetTotal();
            } else if (b.getBillType() == BillType.ChannelOnlineBookingAgentPaidToHospitalBillCancellation) {
                totalAgencyDeposits -= b.getNetTotal();
            }
        }

        dto.setActiveBookings(activeBookings);
        dto.setAgentCancelBookings(agentCancelBookings);
        dto.setCompletedBookings(completedBookings);
        dto.setHospitalCancelBookings(hospitalCancelBookings);
        dto.setTotalAgencyDeposits(totalAgencyDeposits);
        dto.setTotalBookings(totalBookings);
        dto.setTotalEarningForOnlineBooking(totalEarningForOnlineBooking);
        dto.setAbsentPatientBookings(absentPatientBookings);
        dto.setBookingsCancelByagentThroughHospital(bookingsCancelByagentThroughHospital);
        dto.setRemainAmountNeedForHospitalCancelAndRepaidBills(remainAmountNeedForHospitalCancelAndRepaidBills);
        dto.setAbsentBookingPaymentTotal(absentBookingPaymentTotal);
        dto.setActiveBookingEarning(activeBookingEarning);

        String sql2 = "select bill from Bill bill where"
                + " bill.billType = :type "
                + " and bill.retired = :retire"
                + " order by bill.createdAt desc ";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", BillType.ChannelOnlineBookingAgentPaidToHospital);
        parameters.put("retire", false);

        Bill bill = billFacade.findFirstByJpql(sql2, parameters, TemporalType.DATE);

        if (bill != null) {

            dto.setLastPaidToHospitalDate(bill.getCreatedAt());
        }

        return dto;

    }

    public List<ReportTemplateRow> removeCancelAndREfundBillsFromDTO(List<ReportTemplateRow> dto) {
        List<ReportTemplateRow> newList = new ArrayList<>();

        if (dto != null && !dto.isEmpty()) {
            for (ReportTemplateRow row : dto) {
                if ((row.getBill() instanceof CancelledBill) || (row.getBill() instanceof RefundBill)) {
                    continue;
                } else {
                    newList.add(row);
                }
            }
        }

        return newList;
    }

    public List<BillSession> fetchScanningSessionBillSessions(Date fromDate, Date toDate, Institution institution) {

        StringBuilder sql = new StringBuilder("Select bs from BillSession bs where "
                + " bs.bill.billType <> :type "
                + " and bs.sessionInstance.originatingSession.category.name = :category "
                + " and bs.bill.retired = :state "
                + " and bs.createdAt between :fromDate and :toDate");

        Map params = new HashMap();
        params.put("type", BillType.ChannelOnCall);
        params.put("category", "Scanning");
        params.put("state", false);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (institution != null) {
            sql.append(" and bs.institution = :ins");
            params.put("ins", institution);
        }

        sql.append(" order by bs.createdAt desc");

        List<BillSession> list = billSessionFacade.findByJpql(sql.toString(), params, TemporalType.TIMESTAMP);
        return list;
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
        can.setSingleBillSession(bs);
        getBillFacade().edit(can);

        return bs;
    }

    public List<OnlineBooking> fetchOnlineBookingWithingDateRange(Date fromDate, Date toDate, Institution agency) {
        Map params = new HashMap();

        List<OnlineBookingStatus> requiredStatus = Arrays.asList(
                OnlineBookingStatus.COMPLETED,
                OnlineBookingStatus.DOCTOR_CANCELED,
                OnlineBookingStatus.PATIENT_CANCELED,
                OnlineBookingStatus.ACTIVE
        );

        String sql = "Select ob from OnlineBooking ob "
                + " where ob.createdAt between :fromDate and :toDate"
                + " and ob.retired = :ret"
                + " and ob.onlineBookingStatus in :requiredStatus";

        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("ret", false);
        params.put("requiredStatus", requiredStatus);

        if (agency != null) {
            sql += " and ob.agency = :agency";
            params.put("agency", agency);
        }

        return getOnlineBookingFacade().findByJpql(sql, params, TemporalType.DATE);

    }

    public List<Bill> viewBookingHistorybyDate(String fromDate, String toDate, Institution cc, BillClassType b) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            Date startDate = formatter.parse(fromDate);
            Date endDate = formatter.parse(toDate);

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

    public void cancelOnlineBooking(OnlineBooking booking) {
        booking.setOnlineBookingStatus(OnlineBookingStatus.PATIENT_CANCELED);
        booking.setCanceled(true);
        booking.setCancelledBy("From API :" + booking.getAgency().getName());
        getOnlineBookingFacade().edit(booking);

    }

    @EJB
    private ServiceSessionFacade serviceSessionFacade;

    @Transactional
    public void makeAllSessionsAvailableForOnlineBookings(boolean accept) throws Exception {

        String sqlForServiceSession = "";
        String sqlForSessionInstace = "";
        if (accept) {
            sqlForSessionInstace = "update SessionInstance set acceptOnlineBookings = true";
            sqlForServiceSession = "update Item set acceptOnlineBookings = true where Dtype = 'ServiceSession'";
        } else {
            sqlForSessionInstace = "update SessionInstance set acceptOnlineBookings = false";
            sqlForServiceSession = "update Item set acceptOnlineBookings = false where Dtype = 'ServiceSession'";
        }

        getSessionInstanceFacade().executeNativeSql(sqlForSessionInstace);
        serviceSessionFacade.executeNativeSql(sqlForServiceSession);
        getSessionInstanceFacade().flush();
        serviceSessionFacade.flush();

    }

    public SessionInstance findActiveChannelSession(Long id) {

        String sql = " select session from SessionInstance session"
                + " where session.id = :id "
                + " and session.cancelled = :cancel "
                + " and session.completed = :complete"
                + " and session.retired = :retire "
                + " and session.originatingSession.total <> :total";

        Map params = new HashMap();
        params.put("id", id);
        params.put("cancel", false);
        params.put("complete", false);
        params.put("retire", false);
        params.put("total", 0);

        if (!configOptionApplicationController.getBooleanValueByKey("Enable add online bookings(API) for past sessions that are still not completed", false)) {
            sql += " and session.sessionDate >= :date";
            params.put("date", new Date());
        }

        List<SessionInstance> sessions = getSessionInstanceFacade().findByJpqlWithoutCache(sql, params);

        if (sessions == null || sessions.isEmpty()) {
            return null;
        }

        return (SessionInstance) sessions.get(0);

    }

    public BillSession cancelBookingBill(Bill bill, OnlineBooking bookingDetails) {

        BillSession bs = bill.getSingleBillSession();
        CancelledBill cb = createCancelBill1(bill);

        BillItem cItem = cancelBillItems(bs.getBillItem(), cb);
        BillSession cbs = cancelBillSession(bs, cb, cItem);
        bill.setCancelled(true);
        bill.getReferenceBill().setCancelled(true);

        if (bill.getPaidBill() != null) {
            bill.getPaidBill().setCancelled(true);
        }

        List<Payment> payments = createPayment(cb, PaymentMethod.Agent);
        bs.getBill().setCancelledBill(cb);
        getBillFacade().edit(bill);
        getBillFacade().edit(bill.getReferenceBill());
        bs.setReferenceBillSession(cbs);
        billSessionFacade.edit(bs);

        cancelOnlineBooking(bill.getReferenceBill().getOnlineBooking());

        fillBillSessionsAndUpdateBookingsCountInSessionInstance(bill.getSingleBillSession().getSessionInstance());

        return cbs;

        //  sendSmsOnChannelCancellationBookings();
    }

    private BillItem cancelBillItems(BillItem bi, CancelledBill can) {

        BillItem b = new BillItem();
        b.setBill(can);
        b.copy(bi);
        b.invertValue(bi);
        b.setCreatedAt(new Date());

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
        cb.setBillTypeAtomic(BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING);
        cb.setInstitution(bill.getToInstitution());
        cb.setDepartment(bill.getToDepartment());

        String insId = billNumberBean.institutionChannelBillNumberGenerator(bill.getSingleBillSession().getSessionInstance().getOriginatingSession().getInstitution(), cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(bill.getSingleBillSession().getSessionInstance().getOriginatingSession().getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        getBillFacade().create(cb);
        cb.setPaymentMethod(bill.getPaymentMethod());

        createPaymentForCancellations(cb, cb.getPaymentMethod());

        getBillFacade().edit(cb);
        return cb;
    }

    public List<Payment> createPaymentForChannelAppoinmentCancellation(Bill cancellationBill, PaymentMethod cancelPaymentMethod, PaymentMethodData paymentMethodData, SessionController loggedSession) {
        List<Payment> ps = new ArrayList<>();
        if (cancelPaymentMethod == null) {
            List<Payment> originalBillPayments = billService.fetchBillPayments(cancellationBill.getBilledBill());
            if (originalBillPayments != null) {
                for (Payment originalBillPayment : originalBillPayments) {
                    Payment p = originalBillPayment.clonePaymentForNewBill();
                    p.invertValues();
                    p.setReferancePayment(originalBillPayment);
                    p.setBill(cancellationBill);
                    p.setInstitution(loggedSession.getInstitution());
                    p.setDepartment(loggedSession.getDepartment());
                    p.setCreatedAt(new Date());
                    p.setCreater(loggedSession.getLoggedUser());
                    paymentFacade.create(p);
                    ps.add(p);
                }
            }
        } else if (cancelPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(cancellationBill);
                p.setInstitution(loggedSession.getInstitution());
                p.setDepartment(loggedSession.getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(loggedSession.getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCreditCard().getComment());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCheque().getComment());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCash().getComment());
                        break;
                    case ewallet:
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
                    case Agent:
                    case Credit:
                    case PatientDeposit:
                    case Slip:
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }
                p.setPaidValue(0 - Math.abs(p.getPaidValue()));
                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(cancellationBill);
            p.setInstitution(loggedSession.getInstitution());
            p.setDepartment(loggedSession.getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(loggedSession.getLoggedUser());
            p.setPaymentMethod(cancelPaymentMethod);
            p.setPaidValue(cancellationBill.getNetTotal());

            switch (cancelPaymentMethod) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setComments(paymentMethodData.getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setComments(paymentMethodData.getCheque().getComment());
                    break;
                case Cash:
                    p.setComments(paymentMethodData.getCash().getComment());
                    break;
                case ewallet:
                    p.setComments(paymentMethodData.getEwallet().getComment());
                    break;

                case Agent:
                case Credit:
                case PatientDeposit:
                case Slip:
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(0 - Math.abs(p.getBill().getNetTotal()));
            paymentFacade.create(p);
            ps.add(p);
        }
        return ps;
    }
    public List<SessionInstance> getSessionsFromDoctor(Date fromDate, Date toDate, List<Staff> staff, Institution institution){
        String sql = "Select s From SessionInstance s "
                + " where s.retired=false "
                + " and s.originatingSession.institution = :ins "
                + " and s.originatingSession.staff in :staff"
                + " and s.sessionDate between :fromDate and :toDate "
                + " order by s.sessionWeekday,s.startingTime ";
        
        Map params = new HashMap();
        params.put("ins", institution);
        params.put("staff", staff);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        
        return sessionInstanceFacade.findByJpql(sql, params, TemporalType.TIMESTAMP);
        
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

    public List<Speciality> findAllSpecilities() {
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

    public boolean isFullyBookedSession(SessionInstance ss) {
        if (ss.getMaxNo() != 0 && configOptionApplicationController.getBooleanValueByKey("Limited appoinments session can't get appoinement more than max amount.")) {
            if (ss.getBookedPatientCount() != null) {
                int maxNo = ss.getMaxNo();
                long bookedPatientCount = ss.getBookedPatientCount();
                long totalPatientCount;

                List<Integer> reservedNumbers = CommonFunctions.convertStringToIntegerList(ss.getReserveNumbers());
                if (false) {
                    bookedPatientCount = bookedPatientCount;
                } else {
                    bookedPatientCount = bookedPatientCount + reservedNumbers.size();
                }

                if (ss.getCancelPatientCount() != null) {
                    long canceledPatientCount = ss.getCancelPatientCount();
                    totalPatientCount = bookedPatientCount - canceledPatientCount;
                } else {
                    totalPatientCount = bookedPatientCount;
                }
                if (maxNo <= totalPatientCount) {
                    return true;
                }
            }
            return false;
        }
        return false;

    }

    public List<SessionInstance> findSessionInstanceForDoctorSessions(List<Institution> institution, List<Speciality> specialities, List<Doctor> doctorList, Date sessionDate) {
        List<SessionInstance> sessionInstances;
        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select i from SessionInstance i where i.retired=:ret and i.originatingSession.retired=:ret "
                + " and i.cancelled = false"
                + " and i.completed = false");

        // Handle sessionDate equality check
        if (sessionDate != null) {
            jpql.append(" and i.sessionDate >= :sd ");
            m.put("sd", sessionDate);
        } else if (sessionDate == null) {
            Date today = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            cal.add(Calendar.DAY_OF_YEAR, (configOptionApplicationController.getLongValueByKey("How Many days sessions need to share with online booking agent through API", 14L)).intValue());
            Date toDate = cal.getTime();

            jpql.append(" and i.sessionDate between :sd and :td ");
            m.put("sd", new Date());
            m.put("td", toDate);

        }

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

        jpql.append(" order by i.sessionDate, i.sessionTime asc");

        m.put("ret", false);

        sessionInstances = sessionInstanceFacade.findByJpqlWithoutCache(jpql.toString(), m, TemporalType.TIMESTAMP);
        return sessionInstances;
    }

    public List<SessionInstance> findSessionInstance(List<Institution> institution, List<Speciality> specialities, List<Doctor> doctorList, Date sessionDate) {
        List<SessionInstance> sessionInstances;
        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select i from SessionInstance i where i.retired=:ret and i.originatingSession.retired=:ret "
                + " and i.cancelled = false"
                + " and i.completed = false");

        // Handle sessionDate equality check
        if (sessionDate != null) {
            jpql.append(" and i.sessionDate >= :sd ");
            m.put("sd", sessionDate);
        } else if (sessionDate == null) {
            Date today = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            cal.add(Calendar.DAY_OF_YEAR, configOptionApplicationController.getIntegerValueByKey("How Many days sessions need to share with online booking agent", 14));
            Date toDate = cal.getTime();

            jpql.append(" and i.sessionDate between :sd and :td ");
            m.put("sd", new Date());
            m.put("td", toDate);

        }

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

        sessionInstances = sessionInstanceFacade.findByJpqlWithoutCache(jpql.toString(), m, TemporalType.TIMESTAMP);
        return filterAndfindUniqueNextImmidiateSessions(sessionInstances);
    }

    public List<SessionInstance> filterAndfindUniqueNextImmidiateSessions(List<SessionInstance> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return sessions;
        }

        Map<String, SessionInstance> uniqueSessions = new HashMap<>();

        for (SessionInstance session : sessions) {
            String insId = String.valueOf(session.getOriginatingSession().getInstitution().getId());
            String staffId = String.valueOf(session.getOriginatingSession().getStaff().getId());
            String key = insId + staffId;

            SessionInstance existingSession = uniqueSessions.get(key);

            if (existingSession == null) {
                uniqueSessions.put(key, session);
            } else if (existingSession.getSessionDate().after(session.getSessionDate())) {
                uniqueSessions.put(key, session);
            } else if (existingSession.getSessionDate().equals(session.getSessionDate()) && existingSession.getSessionTime().after(session.getSessionTime())) {
                uniqueSessions.put(key, session);
            }

        }

        return new ArrayList<>(uniqueSessions.values());
    }

    public WebUser checkUserCredentialForApi(String temUserName, String temPassword) {

        String temSQL;
        temSQL = "SELECT u FROM WebUser u WHERE u.retired = false and (u.name)=:n order by u.id desc";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        WebUser u = webUserFacade.findFirstByJpql(temSQL, m);

        if (u == null) {
            return null;
        }

        if (SecurityController.matchPassword(temPassword, u.getWebUserPassword())) {

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
        return sessionInstanceFacade.findFirstByJpql(jpql.toString(), m, TemporalType.DATE);
        // System.out.println(jpql.toString()+"\n"+sessionInstances.size()+"\n"+m.values());

    }

    public Bill settleOnlineAgentInitialBooking(BillSession preBillSession, String refNo, double agencyCharge) {
        Bill paidBill = saveAgentOnlinePaymentCompletionBill(preBillSession, refNo);
        BillItem paidBillItem = savePaidBillItem(paidBill, preBillSession);
        savePaidBillFee(paidBill, paidBillItem, preBillSession);
        BillSession paidBillSession = savePaidBillSession(paidBill, paidBillItem, preBillSession);
        paidBillItem.setBillSession(paidBillSession);

        preBillSession.setPaidBillSession(paidBillSession);
        preBillSession.getBill().setPaidAmount(paidBill.getPaidAmount());
        preBillSession.getBill().setBalance(0.0);
        preBillSession.getBill().setPaidBill(paidBill);
        preBillSession.getBill().setPaid(true);

        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(paidBillItem);
        paidBill.setBillItems(savingBillItems);

        getBillFacade().edit(preBillSession.getBill());
        getBillSessionFacade().edit(paidBillSession);
        getBillSessionFacade().edit(preBillSession);

        paidBill.setSingleBillItem(paidBillItem);
        paidBill.setSingleBillSession(paidBillSession);
        getBillFacade().editAndCommit(paidBill);
        getBillItemFacade().edit(paidBillItem);

        List<Payment> p = createPayment(paidBill, paidBill.getPaymentMethod());

        OnlineBooking bookingDetails = paidBill.getReferenceBill().getOnlineBooking();
        bookingDetails.setOnlineBookingPayment(agencyCharge);
        bookingDetails.setHospitalFee(paidBill.getHospitalFee());
        bookingDetails.setDoctorFee(paidBill.getStaffFee());
        bookingDetails.setNetTotalForOnlineBooking(paidBill.getNetTotal() + agencyCharge);
        bookingDetails.setOnlineBookingStatus(OnlineBookingStatus.ACTIVE);
        bookingDetails.setAppoinmentTotalAmount(paidBill.getNetTotal());

        getOnlineBookingFacade().edit(paidBill.getReferenceBill().getOnlineBooking());

        fillBillSessionsAndUpdateBookingsCountInSessionInstance(preBillSession.getSessionInstance());

        return paidBill;
    }

    public Payment createPaymentForCancellations(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        double valueToSet = 0 - Math.abs(bill.getNetTotal());
        p.setPaidValue(valueToSet);
        p.setCreatedAt(new Date());
        p.setPaymentMethod(pm);
        if (pm == null) {
            pm = bill.getPaymentMethod();
        }
        if (p.getId() == null) {
            paymentFacade.create(p);
        }
        return p;
    }

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        Payment p = new Payment();
        p.setBill(bill);
        p.setInstitution(bill.getInstitution());
        p.setDepartment(bill.getDepartment());
        p.setCreatedAt(new Date());
        p.setPaymentMethod(pm);
        p.setPaidValue(bill.getNetTotal());
        paymentFacade.create(p);

        ps.add(p);
        return ps;

    }

    private BillItem savePaidBillItem(Bill b, BillSession bs) {
        BillItem bi = new BillItem();
        bi.copy(bs.getBillItem());
        bi.setCreatedAt(new Date());
        bi.setBill(b);
        getBillItemFacade().create(bi);
        return getBillItemFacade().find(bi.getId());
    }

    private void savePaidBillFee(Bill b, BillItem bi, BillSession bs) {

        for (BillFee f : bs.getBill().getBillFees()) {

            BillFee bf = new BillFee();
            bf.copy(f);
            bf.setCreatedAt(Calendar.getInstance().getTime());
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

        getBillSessionFacade().create(paidBillSession);
        return paidBillSession;

    }

    private Bill saveAgentOnlinePaymentCompletionBill(BillSession bs, String refNo) {
        Bill completedOnlineBookingBill = new BilledBill();
        completedOnlineBookingBill.setAgentRefNo(refNo);
        completedOnlineBookingBill.copy(bs.getBill());
        completedOnlineBookingBill.setDepartment(bs.getBill().getDepartment());
        completedOnlineBookingBill.setInstitution(bs.getBill().getInstitution());
        completedOnlineBookingBill.copyValue(bs.getBill());
        completedOnlineBookingBill.setPaidAmount(bs.getBill().getNetTotal());
        completedOnlineBookingBill.setBalance(0.0);
        completedOnlineBookingBill.setPaymentMethod(PaymentMethod.Agent);
        completedOnlineBookingBill.setReferenceBill(bs.getBill());
        completedOnlineBookingBill.setBillType(BillType.ChannelAgent);
        completedOnlineBookingBill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(bs.getDepartment(), BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT);

        completedOnlineBookingBill.setInsId(deptId);
        completedOnlineBookingBill.setDeptId(deptId);
        completedOnlineBookingBill.setBookingId(deptId);
        completedOnlineBookingBill.setDepartment(bs.getDepartment());
        completedOnlineBookingBill.setInstitution(bs.getInstitution());
        completedOnlineBookingBill.setBillDate(new Date());
        completedOnlineBookingBill.setBillTime(new Date());
        completedOnlineBookingBill.setCreatedAt(new Date());

        getBillFacade().create(completedOnlineBookingBill);

        return completedOnlineBookingBill;
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
            if (bill.getOnlineBooking().isForeignStatus()) {
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

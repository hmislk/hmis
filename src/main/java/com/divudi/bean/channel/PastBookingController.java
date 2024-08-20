/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.DoctorSpecialityController;
import com.divudi.bean.common.ItemForItemController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.data.channel.ChannelScheduleEvent;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.ArrivalRecord;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.entity.membership.PaymentSchemeDiscount;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.entity.Fee;
import com.divudi.entity.Payment;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.SessionInstanceFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class PastBookingController implements Serializable, ControllerWithPatient {

    /**
     * EJBs
     */
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    ItemFeeFacade ItemFeeFacade;
    @EJB
    private ChannelBean channelBean;
    @EJB
    FingerPrintRecordFacade fpFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    SmsManagerEjb smsManager;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    SessionInstanceFacade sessionInstanceFacade;
    /**
     * Controllers
     */
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    PatientController patientController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ChannelBillController channelCancelController;
    @Inject
    private ChannelReportController channelReportController;
    @Inject
    private ChannelSearchController channelSearchController;
    @Inject
    ServiceSessionLeaveController serviceSessionLeaveController;
    @Inject
    ChannelBillController channelBillController;
    @Inject
    DoctorSpecialityController doctorSpecialityController;
    @Inject
    ChannelStaffPaymentBillController channelStaffPaymentBillController;
    @Inject
    AgentReferenceBookController agentReferenceBookController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    BillController billController;
    @Inject
    SessionInstanceController sessionInstanceController;
    @Inject
    ItemForItemController itemForItemController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    /**
     * Properties
     */
    private Speciality speciality;
    private Staff staff;
    private Staff toStaff;
    private boolean foriegn;
    private PaymentScheme paymentScheme;
    boolean settleSucessFully;
    Bill printingBill;

    @EJB
    private SmsFacade smsFacade;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date channelDay;
    @Deprecated
    private ServiceSession selectedServiceSession;
    private SessionInstance selectedSessionInstance;
    private List<ItemFee> selectedItemFees;
    private List<ItemFee> sessionFees;
    private List<ItemFee> addedItemFees;
    private BillSession selectedBillSession;
    @Deprecated
    private BillSession managingBillSession;
    private List<SessionInstance> sessionInstances;
    private List<BillSession> billSessions;
    List<Staff> consultants;
    List<BillSession> getSelectedBillSession;
    boolean printPreview;
    double absentCount;
    int serealNo;
    Date date;
    Date sessionStartingDate;
    String selectTextSpeciality = "";
    String selectTextConsultant = "";
    String selectTextSession = "";
    ArrivalRecord arrivalRecord;
    private String errorText;
    private Patient patient;
    private PaymentMethod paymentMethod;
    PaymentMethodData paymentMethodData;

    private ScheduleModel eventModel;
    boolean patientDetailsEditable;

    private Item itemToAddToBooking;
    private List<Item> itemsAvailableToAddToBooking;
    private Institution institution;
    private String agentRefNo;
    private List<BillFee> listBillFees;
    private BillSession billSession;
    private List<Date> dates;

    private ChannelScheduleEvent event = new ChannelScheduleEvent();

    private Double feeTotalForSelectedBill;

    public boolean chackNull(String template) {
        boolean chack;
        chack = template.isEmpty();
        return chack;
    }

    public String fillDataForChannelingBillHeader(String template, Bill bill) {
        String output;

        output = template
                .replace("{from_department}", bill.getDepartment().getName())
                .replace("{from_department_address}", bill.getDepartment().getAddress())
                .replace("{telephone1}", bill.getDepartment().getTelephone1())
                .replace("{telephone2}", bill.getDepartment().getTelephone2())
                .replace("{email}", bill.getDepartment().getEmail())
                .replace("{fax}", bill.getDepartment().getFax());
        return output;
    }

    

  
    public void fillFees() {
        selectedItemFees = new ArrayList<>();
        sessionFees = new ArrayList<>();
        addedItemFees = new ArrayList<>();
        if (selectedSessionInstance == null) {
            return;
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            return;
        }
        String sql;
        Map m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and f.serviceSession=:ses "
                + " order by f.id";
        m.put("ses", selectedSessionInstance.getOriginatingSession());

        sessionFees = itemFeeFacade.findByJpql(sql, m);
        m = new HashMap();
        sql = "Select f from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:item "
                + " order by f.id";
        m.put("item", itemToAddToBooking);
        addedItemFees = itemFeeFacade.findByJpql(sql, m);
        if (sessionFees != null) {
            selectedItemFees.addAll(sessionFees);
        }
        if (addedItemFees != null) {
            selectedItemFees.addAll(addedItemFees);
        }
        feeTotalForSelectedBill = 0.0;
        for (ItemFee tbf : selectedItemFees) {
            if (foriegn) {
                feeTotalForSelectedBill += tbf.getFfee();
            } else {
                feeTotalForSelectedBill += tbf.getFee();
            }
        }
    }

    public String navigateToPastChannelBookingFromMenu() {
        prepareForNewChannellingBill();
        return "/channel/past_channel_booking?faces-redirect=true";
    }

    public String navigateToChannelQueueFromMenu() {
        sessionInstances = channelBean.listTodaysSesionInstances();
        return "/channel/channel_queue?faces-redirect=true";
    }

    public String navigateToChannelDisplayFromMenu() {
        sessionInstances = channelBean.listTodaysSessionInstances(true, false, false);
        return "/channel/channel_display?faces-redirect=true";
    }

    public String navigateToChannelQueueFromConsultantRoom() {
        sessionInstances = channelBean.listTodaysSesionInstances();
        return "/channel/channel_queue?faces-redirect=true";
    }

    public void listTodaysAllSesionInstances() {
        sessionInstances = channelBean.listTodaysSessionInstances(null, null, null);
    }

    public void listTodaysOngoingSesionInstances() {
        sessionInstances = channelBean.listTodaysSessionInstances(true, null, null);
    }

    public void listTodaysCompletedSesionInstances() {
        sessionInstances = channelBean.listTodaysSessionInstances(null, true, null);
    }

    public void listTodaysPendingSesionInstances() {
        sessionInstances = channelBean.listTodaysSessionInstances(null, null, true);
    }

    public void prepareForNewChannellingBill() {
        listnerStaffListForRowSelect();
    }

    public String navigateToViewSessionData() {
        return "/channel/session_data?faces-redirect=true";
    }

    public String navigateToConsultantRoom() {
        return "/channel/consultant_room?faces-redirect=true";
    }

    public void loadSessionInstance() {
        sessionInstances = channelBean.listTodaysSessionInstances(true, false, false);
    }

    public String navigateToManageBooking() {
        if (selectedBillSession == null) {
            JsfUtil.addErrorMessage("Please select a Patient");
            return "";
        }
        if (selectedBillSession.getBill().getBillItems() == null) {
            selectedBillSession.getBill().setBillItems(billController.billItemsOfBill(selectedBillSession.getBill()));
        }
        channelBillController.setBillSession(selectedBillSession);
        if (selectedBillSession.getBill().getBillItems() == null) {
            JsfUtil.addErrorMessage("Bill Items Null");
            return "";
        }

        if (selectedBillSession.getBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Items");
            return "";
        }

        if (selectedBillSession.getBill().getBillFees() == null) {
            selectedBillSession.getBill().setBillFees(billController.billFeesOfBill(selectedBillSession.getBill()));
        }

        if (selectedBillSession.getBill().getBillFees() == null) {
            JsfUtil.addErrorMessage("Bill Fees Null");
            return "";
        }

        if (selectedBillSession.getBill().getBillFees().isEmpty()) {
            JsfUtil.addErrorMessage("No Bill Fees");
            return "";
        }

        if (configOptionApplicationController.getBooleanValueByKey("Channel Past Booking Can Not Be Refunded")) {
        }

        if (configOptionApplicationController.getBooleanValueByKey("Channel Past Booking Can Not Be Canceled")) {
        }

        return "/channel/manage_booking_past?faces-redirect=true";
    }

    public String navigateBackToPastBookings() {
        return "/channel/past_channel_booking?faces-redirect=true";
    }

    public String navigateToManageSessionQueueAtConsultantRoom() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Not Selected");
            return null;
        }
        fillBillSessions();
        return "/channel/channel_queue_session?faces-redirect=true";
    }

    public String navigateToDisplatSessionQueueAtConsultantRoom() {
        if (selectedSessionInstance == null) {
            JsfUtil.addErrorMessage("Not Selected");
            return null;
        }
        fillBillSessions();
        return "/channel/channel_queue_display?faces-redirect=true";
    }

    public String navigateToNurseView() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "/channel/channel_nurse_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToDoctorView() {
        if (preSet()) {
            getChannelReportController().fillDoctorView();
            return "/channel/channel_doctor_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToSessionView() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "/channel/channel_session_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToPhoneView() {
        if (preSet()) {
            getChannelReportController().fillNurseView();
            return "/channel/channel_phone_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToUserView() {
        if (preSet()) {
            getChannelReportController().fillDoctorView();
            return "/channel/channel_user_view?faces-redirect=true";
        } else {
            return "";
        }
    }

    public String navigateToAllDoctorView() {
        return "/channel/channel_patient_view_today?faces-redirect=true";
    }

    public String navigateToAllPatientView() {
        return "/channel/channel_user_view?faces-redirect=true";
    }

    public List<BillSession> getGetSelectedBillSession() {
        return getSelectedBillSession;
    }

    public void setGetSelectedBillSession(List<BillSession> getSelectedBillSession) {
        this.getSelectedBillSession = getSelectedBillSession;
    }

    public boolean errorCheckForSerial() {
        boolean alreadyExists = false;
        for (BillSession bs : billSessions) {
            //System.out.println("billSessions" + bs.getId());

            if (selectedBillSession.equals(bs)) {

            } else {
                if (bs.getSerialNo() == selectedBillSession.getSerialNo()) {
                    alreadyExists = true;
                }
            }
            if (!bs.equals(selectedBillSession)) {
                for (BillItem bi : bs.getBill().getBillItems()) {
                    if (serealNo == bi.getBillSession().getSerialNo()) {
                        alreadyExists = true;
                        JsfUtil.addErrorMessage("This Number Is Alredy Exsist");
                    }
                }
            }

        }

        return alreadyExists;
    }

    public String startNewChannelBookingForSelectingSpeciality() {
        resetToStartFromSelectingSpeciality();
        printPreview = false;
        return navigateBackToPastBookings();
    }

    public String startNewChannelBookingFormSelectingConsultant() {
        resetToStartFromSelectingConsultant();
        generateSessions();
        printPreview = false;
        return navigateBackToPastBookings();
    }

    public String startNewChannelBookingForSelectingSession() {
        resetToStartFromSameSessionInstance();
        fillBillSessions();
        printPreview = false;
        return navigateBackToPastBookings();
    }

    public String startNewChannelBookingForSameSession() {
        resetToStartFromSameSessionInstance();
        printPreview = false;
        return "";
    }

    public String navigateToManageBookingForSameSession() {
        printPreview = false;
        if (printingBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        selectedBillSession = printingBill.getSingleBillSession();
        return navigateToManageBooking();
    }

    public boolean billSessionErrorPresent() {
        boolean flag = false;
        for (BillSession bs : getBillSessions()) {
            if (selectedBillSession != null) {
                if (!bs.equals(selectedBillSession)) {
                    for (BillItem bi : bs.getBill().getBillItems()) {
                        if (serealNo == bi.getBillSession().getSerialNo()) {
                            JsfUtil.addErrorMessage("This Number Is Alredy Exsist");
                            flag = true;
                        }
                    }
                }
            }
        }
        return flag;
    }

    public double getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(double absentCount) {
        this.absentCount = absentCount;
    }

//    public void errorCheckChannelNumber() {
//
//        for (BillSession bs : billSessions) {
//            //System.out.println("billSessions" + bs.getName());
//            for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
//                //System.out.println("billitem" + bi.getId());
//                if (bs.getSerialNo() == bi.getBillSession().getSerialNo()) {
//                    JsfUtil.addErrorMessage("Number you entered already exist");
//                    setSelectedBillSession(bs);
//
//                }
//
//            }
//        }
//
//    }
    public void updatePatient() {
        getPersonFacade().edit(getSelectedBillSession().getBill().getPatient().getPerson());
        JsfUtil.addSuccessMessage("Patient Updated");
    }

    public boolean patientErrorPresent(Patient p) {
        if (p == null) {
            JsfUtil.addErrorMessage("No Current. Error. NOT SAVED");
            return true;
        }

        if (p.getPerson() == null) {
            JsfUtil.addErrorMessage("No Person. Not Saved");
            return true;
        }

        if (p.getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please enter a name");
            return true;
        }

        if (p.getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a name");
            return true;
        }

        return false;
    }

   
   


    public void updateSerial() {
        if (errorCheckForSerial()) {
            return;
        }
        if (billSessionErrorPresent()) {
            return;
        }

        for (BillItem bi : getSelectedBillSession().getBill().getBillItems()) {
            bi.getBillSession().setSerialNo(serealNo);
            getBillItemFacade().edit(bi);
        }

        getBillSessionFacade().edit(getSelectedBillSession());
        //System.out.println(getSelectedBillSession().getBill().getPatient());
        JsfUtil.addSuccessMessage("Serial Updated");
    }

    public void resetToStartFromSelectingSpeciality() {
        speciality = null;
        staff = null;
        sessionInstances = null;
        billSessions = null;
        sessionStartingDate = null;
        itemsAvailableToAddToBooking = new ArrayList<>();
        itemToAddToBooking = null;
        patient = new Patient();
    }

    public void resetToStartFromSelectingConsultant() {
        staff = null;
        sessionInstances = null;
        billSessions = null;
        sessionStartingDate = null;
        itemToAddToBooking = null;
        itemsAvailableToAddToBooking = new ArrayList<>();
        patient = new Patient();
    }

    public void resetToStartFromSelectingSessionInstance() {
        sessionInstances = null;
        billSessions = null;
        sessionStartingDate = null;
        itemToAddToBooking = null;
        patient = new Patient();
    }

    public void resetToStartFromSameSessionInstance() {
        patient = new Patient();
        itemToAddToBooking = null;
    }

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getSpeciality() != null) {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            ////System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void fillConsultants() {
        String sql;
        Map m = new HashMap();
        m.put("sp", getSpeciality());
        if (getSpeciality() != null) {
            if (getSessionController().getInstitutionPreference().isShowOnlyMarkedDoctors()) {

                sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                        + " and pi.type=:typ "
                        + " and pi.institution=:ins "
                        + " and pi.staff.speciality=:sp "
                        + " order by pi.staff.person.name ";

                m.put("ins", getSessionController().getInstitution());
                m.put("typ", PersonInstitutionType.Channelling);

            } else {
                sql = "select p from Staff p where p.retired=false and p.speciality=:sp order by p.person.name";
            }

            consultants = getStaffFacade().findByJpql(sql, m);
        } else {
            sql = "select p from Staff p where p.retired=false order by p.person.name";
            consultants = getStaffFacade().findByJpql(sql);
        }
//        //System.out.println("consultants = " + consultants);
        setStaff(null);
    }

    public List<Staff> getSelectedConsultants() {
        String sql;
        Map m = new HashMap();
        if (getSpeciality() == null) {
            sql = " select s "
                    + " from Staff s "
                    + " where s.retired=:ret "
                    + " and s.speciality in :sps "
                    + " order by s.person.name ";
            m.put("ret", false);
            m.put("sps", doctorSpecialityController.getSelectedItems());
            consultants = getStaffFacade().findByJpql(sql, m);
        } else {
            sql = "select s "
                    + " from Staff s "
                    + " where s.retired=:ret "
                    + " and s.speciality=:sp "
                    + "order by s.person.name";
            m.put("ret", false);
            m.put("sp", getSpeciality());
            consultants = getStaffFacade().findByJpql(sql, m);
        }
        if (consultants == null) {
            consultants = new ArrayList<>();
        }
        return consultants;
    }

    public List<Staff> getConsultants() {
        if (consultants == null) {
            consultants = new ArrayList<>();
        }
        return consultants;
    }

    public void setConsultants(List<Staff> consultants) {
        this.consultants = consultants;
    }

    /**
     * Creates a new instance of BookingController
     */
    public PastBookingController() {
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

//    public void setSpeciality(Speciality speciality) {
//        this.speciality = speciality;
//        fillConsultants();
//        setStaff(null);
//    }
    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

//    public void setStaff(Staff staff) {
////        System.err.println("CLIKED");
//        this.staff = staff;
//        //generateSessions();
//        setSelectedServiceSession(null);
//        serviceSessionLeaveController.setSelectedServiceSession(null);
//        serviceSessionLeaveController.setCurrentStaff(staff);
//    }
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    private Double[] fetchFee(Item item, FeeType feeType) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select sum(f.fee),sum(f.ffee) "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses "
                + " and f.feeType=:ftp";
        m.put("ses", item);
        m.put("ftp", feeType);
        Object[] obj = getItemFeeFacade().findAggregateModified(jpql, m, TemporalType.TIMESTAMP);

        if (obj == null) {
            Double[] dbl = new Double[2];
            dbl[0] = 0.0;
            dbl[1] = 0.0;
            return dbl;
        }

        Double[] dbl = Arrays.copyOf(obj, obj.length, Double[].class);
//        System.err.println("Fetch Fee Values " + dbl);
        return dbl;
    }

    private double fetchLocalFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.fee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private double fetchForiegnFee(Item item, PaymentMethod paymentMethod) {
        String jpql;
        Map m = new HashMap();
        FeeType[] fts = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff};
        List<FeeType> feeTypes = Arrays.asList(fts);
        jpql = "Select sum(f.ffee)"
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";

        if (paymentMethod == PaymentMethod.Agent) {
            FeeType[] fts1 = {FeeType.Service, FeeType.OwnInstitution, FeeType.Staff, FeeType.OtherInstitution};
            feeTypes = Arrays.asList(fts1);
            jpql += " and f.feeType in :fts1 "
                    + " and f.name!=:name";
            m.put("name", "On-Call Fee");
            m.put("fts1", feeTypes);
        } else {
            if (paymentMethod == PaymentMethod.OnCall) {
                jpql += " and f.feeType in :fts2 ";
                m.put("fts2", feeTypes);
            } else {
                jpql += " and f.feeType in :fts3 "
                        + " and f.name!=:name";
                m.put("name", "On-Call Fee");
                m.put("fts3", feeTypes);
            }
        }
        m.put("ses", item);
        Double obj = getItemFeeFacade().findDoubleByJpql(jpql, m);

        if (obj == null) {
            return 0;
        }

        return obj;
    }

    private List<ItemFee> fetchFee(Item item) {
        String jpql;
        Map m = new HashMap();
        jpql = "Select f "
                + " from ItemFee f "
                + " where f.retired=false "
                + " and f.item=:ses ";
        m.put("ses", item);
        List<ItemFee> list = getItemFeeFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetch Fess " + list.size());
        return list;
    }

    public void calculateFee(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss, FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            dbl = fetchFee(ss, FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
//            System.err.println("1111");
            dbl = fetchFee(ss, FeeType.Tax);
//            System.err.println("2222");
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            ss.setTotalFee(fetchLocalFee(ss, paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss, paymentMethod));
            ss.setItemFees(fetchFee(ss));
        }
    }

    @Deprecated
    public void calculateFeeBooking(List<ServiceSession> lstSs, PaymentMethod paymentMethod) {
        for (ServiceSession ss : lstSs) {
            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void calculateFeeForSessionInstances(List<SessionInstance> lstSs, PaymentMethod paymentMethod) {
        for (SessionInstance ss : lstSs) {
            Double[] dbl = fetchFee(ss.getOriginatingSession(), FeeType.OwnInstitution);
            ss.setHospitalFee(dbl[0]);
            ss.setHospitalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setHospitalFee(dbl[0]);
            ss.getOriginatingSession().setHospitalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Staff);
            ss.setProfessionalFee(dbl[0]);
            ss.setProfessionalFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setProfessionalFee(dbl[0]);
            ss.getOriginatingSession().setProfessionalFfee(dbl[1]);
            //For Settle bill
            dbl = fetchFee(ss.getOriginatingSession(), FeeType.Tax);
            ss.setTaxFee(dbl[0]);
            ss.setTaxFfee(dbl[1]);
            //For Settle bill
            ss.getOriginatingSession().setTaxFee(dbl[0]);
            ss.getOriginatingSession().setTaxFfee(dbl[1]);
            //For Settle bill
            ss.setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
            ss.getOriginatingSession().setTotalFee(fetchLocalFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setTotalFfee(fetchForiegnFee(ss.getOriginatingSession(), paymentMethod));
            ss.getOriginatingSession().setItemFees(fetchFee(ss.getOriginatingSession()));
            //For Settle bill
        }
    }

    public void generateSessions() {
        sessionInstances = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        m.put("staff", getStaff());
        m.put("class", ServiceSession.class);
        if (staff != null) {
            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.originatingSession is null"
                    + " and type(s)=:class "
                    + " order by s.sessionWeekday,s.startingTime ";
            List<ServiceSession> selectedDoctorsServiceSessions = getServiceSessionFacade().findByJpql(sql, m);
            calculateFee(selectedDoctorsServiceSessions, channelBillController.getPaymentMethod());
            try {
                sessionInstances = getChannelBean().generateSesionInstancesFromServiceSessions(selectedDoctorsServiceSessions, sessionStartingDate);
            } catch (Exception e) {
            }
            generateSessionEvents(sessionInstances);
        }

    }

    public void fillSessionInstance() {
        sessionInstances = new ArrayList<>();
        String jpql = "select i "
                + " from SessionInstance i "
                + " where i.originatingSession.staff=:os "
                + " and i.retired=:ret"
                + " and i.sessionDate=:date ";

        Map m = new HashMap();
        m.put("ret", false);
        m.put("os", getStaff());
        m.put("date", getDate());

        sessionInstances = sessionInstanceFacade.findByJpql(jpql, m, TemporalType.DATE);
    }

    public void generateSessionEvents(List<SessionInstance> sss) {
        eventModel = new DefaultScheduleModel();
        for (SessionInstance s : sss) {
            ChannelScheduleEvent e = new ChannelScheduleEvent();
            e.setSessionInstance(s);
            e.setTitle(s.getName());
            e.setStartDate(CommonFunctions.convertDateToLocalDateTime(s.getTransStartTime()));
            e.setEndDate(CommonFunctions.convertDateToLocalDateTime(s.getTransEndTime()));
            eventModel.addEvent(e);
        }
    }

    public void onEventSelect(SelectEvent selectEvent) {
        event = (ChannelScheduleEvent) selectEvent.getObject();
        selectedServiceSession = event.getServiceSession();
        fillBillSessions();
    }

    public void generateSessionsFutureBooking(SelectEvent event) {
        date = null;
        date = ((Date) event.getObject());
        sessionInstances = new ArrayList<>();
        Map m = new HashMap();

        Date currenDate = new Date();
        if (getDate().before(currenDate)) {
            JsfUtil.addErrorMessage("Please Select Future Date");
            return;
        }

        String sql = "";

        if (staff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);

            sql = "Select s From ServiceSession s "
                    + " where s.retired=false "
                    + " and s.staff=:staff "
                    + " and s.sessionWeekday=:wd ";

            m.put("staff", getStaff());
            m.put("wd", wd);
            List<ServiceSession> tmp = getServiceSessionFacade().findByJpql(sql, m);
            calculateFee(tmp, channelBillController.getPaymentMethod());//check work future bokking
            sessionInstances = getChannelBean().generateSesionInstancesFromServiceSessions(tmp, date);
        }

        billSessions = new ArrayList<>();
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<SessionInstance> getServiceSessions() {
        return sessionInstances;
    }

    public void setServiceSessions(List<SessionInstance> serviceSessions) {
        this.sessionInstances = serviceSessions;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public List<BillSession> getBillSessions() {
        if (billSessions == null) {
            billSessions = new ArrayList<>();
        }
        return billSessions;
    }

//    public void fillBillSessions(SelectEvent event) {
//        selectedBillSession = null;
//        selectedServiceSession = ((ServiceSession) event.getObject());
//
//        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
//        List<BillType> bts = Arrays.asList(billTypes);
//
//        String sql = "Select bs From BillSession bs "
//                + " where bs.retired=false"
//                + " and bs.serviceSession=:ss "
//                + " and bs.bill.billType in :bt"
//                + " and type(bs.bill)=:class "
//                + " and bs.sessionDate= :ssDate "
//                + " order by bs.serialNo ";
//        HashMap hh = new HashMap();
//        hh.put("bt", bts);
//        hh.put("class", BilledBill.class);
//        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
//        hh.put("ss", getSelectedServiceSession());
//        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
//        System.out.println("hh = " + hh);
//        System.out.println("getSelectedServiceSession().isTransLeave() = " + getSelectedServiceSession().isTransLeave());
//        if (getSelectedServiceSession().isTransLeave()) {
//            billSessions=null;
//        }
//        System.out.println("billSessions" + billSessions);
//
//    }
    public void findArrivals() {

        String sql = "Select bs From ArrivalRecord bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.sessionDate= :ssDate ";
        HashMap hh = new HashMap();
        hh.put("ssDate", getSelectedServiceSession().getSessionDate());
        hh.put("ss", getSelectedServiceSession());
        arrivalRecord = (ArrivalRecord) fpFacade.findFirstByJpql(sql, hh);
    }

    public void markAsArrived() {
        if (selectedServiceSession == null) {
            return;
        }
        if (selectedServiceSession.getSessionDate() == null) {
            return;
        }
        if (arrivalRecord == null) {
            arrivalRecord = new ArrivalRecord();
            arrivalRecord.setSessionDate(selectedServiceSession.getSessionDate());
            arrivalRecord.setServiceSession(selectedServiceSession);
            arrivalRecord.setCreatedAt(new Date());
            arrivalRecord.setCreater(sessionController.getLoggedUser());
            fpFacade.create(arrivalRecord);
        }
        arrivalRecord.setRecordTimeStamp(new Date());
        arrivalRecord.setApproved(false);
        fpFacade.edit(arrivalRecord);
    }

    public void markCurrentCompleteAndCallNext() {
        BillSession lastCompletedSession = null;
        BillSession currentSession = null;
        BillSession nextSession = null;

        // Iterate through the billSessions list
        for (int i = 0; i < billSessions.size(); i++) {
            BillSession bs = billSessions.get(i);
            if (Boolean.TRUE.equals(bs.getCurrentlyConsulted())) {
                // Mark the currently consulted session as completed
                bs.setCompleted(true);
                bs.setCurrentlyConsulted(false);
                lastCompletedSession = bs; // This becomes the last completed session
                billSessionFacade.edit(bs);

                // Check for the next session in the list
                if (i + 1 < billSessions.size()) {
                    nextSession = billSessions.get(i + 1);
                    nextSession.setCurrentlyConsulted(true);
                    nextSession.setNextInLine(false);
                    currentSession = nextSession; // This is now the currently consulting session
                    billSessionFacade.edit(nextSession);
                }
                break;
            }
        }

        // Set the next in line session if there is one
        if (nextSession != null && billSessions.size() > billSessions.indexOf(nextSession) + 1) {
            BillSession newNextInLine = billSessions.get(billSessions.indexOf(nextSession) + 1);
            newNextInLine.setNextInLine(true);
            billSessionFacade.edit(newNextInLine); // Update the next in line session
            selectedSessionInstance.setNextInLineBillSession(newNextInLine);
        } else {
            selectedSessionInstance.setNextInLineBillSession(null);
        }

        // Update the SessionInstance with the new lastCompleted, currentlyConsulting, and nextInLine sessions
        selectedSessionInstance.setLastCompletedBillSession(lastCompletedSession);
        selectedSessionInstance.setCurrentlyConsultingBillSession(currentSession);
        // Note: The next in line has already been set above if available

        // Persist changes to the SessionInstance
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void callNextSessionToCurrent() {
        BillSession lastCompletedSession = null;
        BillSession currentSession = null;
        BillSession nextSession = null;
        boolean currentFound = false;

        // Check if there is currently a session being consulted
        for (BillSession bs : billSessions) {
            if (Boolean.TRUE.equals(bs.getCurrentlyConsulted())) {
                currentFound = true;
                break;
            }
        }

        // If no current session is being consulted
        if (!currentFound) {
            for (int i = 0; i < billSessions.size(); i++) {
                BillSession bs = billSessions.get(i);
                if (Boolean.TRUE.equals(bs.getNextInLine())) {
                    // This session becomes the currently consulted session
                    bs.setCurrentlyConsulted(true);
                    bs.setNextInLine(false);
                    currentSession = bs; // Set as the currently consulting session
                    billSessionFacade.edit(bs);

                    // Find and update the next in line session
                    if (i + 1 < billSessions.size()) {
                        nextSession = billSessions.get(i + 1);
                        nextSession.setNextInLine(true);
                        billSessionFacade.edit(nextSession);
                        selectedSessionInstance.setNextInLineBillSession(nextSession);
                    } else {
                        selectedSessionInstance.setNextInLineBillSession(null);
                        JsfUtil.addErrorMessage("You have to srat the session to call for Patients");
                        return;
                    }

                    // Update the last completed session if needed
                    if (i - 1 >= 0 && billSessions.get(i - 1).isCompleted()) {
                        lastCompletedSession = billSessions.get(i - 1);
                        selectedSessionInstance.setLastCompletedBillSession(lastCompletedSession);
                    }

                    break;
                }
            }
        }

        // Update the SessionInstance with the new currentlyConsulting session and possibly the nextInLine session
        selectedSessionInstance.setCurrentlyConsultingBillSession(currentSession);
        // Persist changes to the SessionInstance
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void reverseCurrentCompleteAndCallPrevious() {
        BillSession currentSession = null;
        BillSession previousSession = null;

        // Find the index of the currently consulting session
        int currentIndex = -1;
        for (int i = 0; i < billSessions.size(); i++) {
            if (Boolean.TRUE.equals(billSessions.get(i).getCurrentlyConsulted())) {
                currentIndex = i;
                break;
            }
        }

        // If a currently consulting session is found
        if (currentIndex != -1) {
            // Reverse the completion of the current session
            currentSession = billSessions.get(currentIndex);
            currentSession.setCompleted(false);
            billSessionFacade.edit(currentSession);

            // There is a session before the current one
            if (currentIndex - 1 >= 0) {
                previousSession = billSessions.get(currentIndex - 1);

                // Reverse the last completed session to the previous session
                previousSession.setCurrentlyConsulted(true);
                if (!previousSession.isCompleted()) {
                    selectedSessionInstance.setLastCompletedBillSession(null);
                } else {
                    // If the previous session was completed, update the lastCompletedBillSession accordingly
                    selectedSessionInstance.setLastCompletedBillSession(previousSession);
                }
                billSessionFacade.edit(previousSession);

                // Update currently consulting session to previous session
                selectedSessionInstance.setCurrentlyConsultingBillSession(previousSession);

                // Set the nextInLineBillSession to the currentSession
                currentSession.setNextInLine(true);
                billSessionFacade.edit(currentSession);
                selectedSessionInstance.setNextInLineBillSession(currentSession);
            } else {
                // If there's no previous session, handle accordingly, perhaps clearing the currentlyConsulted status
                selectedSessionInstance.setCurrentlyConsultingBillSession(null);
                selectedSessionInstance.setNextInLineBillSession(null);
            }
        }

        // If a previous session is found and set as the current session
        if (previousSession != null) {
            // Update the currentlyConsulted status for the session that was just reversed
            currentSession.setCurrentlyConsulted(false);
            billSessionFacade.edit(currentSession);
        }

        // Update changes to SessionInstance
        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void resetAndSetSelectedAsCurrentlyConsulted() {
        if (selectedBillSession == null) {
            // Handle the case where no session is selected
            return;
        }

        BillSession nextInLineSession = null;
        BillSession lastCompletedSession = null;

        // Reset the status for all sessions and identify nextInLine and lastCompleted sessions
        for (int i = 0; i < billSessions.size(); i++) {
            BillSession bs = billSessions.get(i);
            bs.setNextInLine(false);
            bs.setCurrentlyConsulted(false);

            // Do not reset the completed status; determine nextInLine and lastCompleted sessions instead
            // If selectedBillSession is found, look for the next session that is not completed to be the nextInLine
            if (selectedBillSession.equals(bs) && i + 1 < billSessions.size()) {
                for (int j = i + 1; j < billSessions.size(); j++) {
                    if (!billSessions.get(j).isCompleted()) {
                        nextInLineSession = billSessions.get(j);
                        break;
                    }
                }
            }
            // Find the last completed session before the selectedBillSession
            if (selectedBillSession.equals(bs) && i - 1 >= 0) {
                for (int k = i - 1; k >= 0; k--) {
                    if (Boolean.TRUE.equals(billSessions.get(k).isCompleted())) {
                        lastCompletedSession = billSessions.get(k);
                        break;
                    }
                }
            }

            billSessionFacade.edit(bs);
        }

        // Set the selectedBillSession as currentlyConsulted
        selectedBillSession.setCurrentlyConsulted(true);
        billSessionFacade.edit(selectedBillSession);

        // Update the SessionInstance with nextInLine and lastCompleted sessions
        selectedSessionInstance.setCurrentlyConsultingBillSession(selectedBillSession);
        selectedSessionInstance.setLastCompletedBillSession(lastCompletedSession);
        selectedSessionInstance.setNextInLineBillSession(nextInLineSession);

        // If there's a nextInLine session, mark it accordingly
        if (nextInLineSession != null) {
            nextInLineSession.setNextInLine(true);
            billSessionFacade.edit(nextInLineSession);
        }
        // Similarly, update lastCompletedSession if necessary
        if (lastCompletedSession != null) {
            lastCompletedSession.setCompleted(true); // Ensure it's marked as completed
            billSessionFacade.edit(lastCompletedSession);
        }

        sessionInstanceFacade.edit(selectedSessionInstance);
    }

    public void fillBillSessions() {
        selectedBillSession = null;
        BillType[] billTypes = {
            BillType.ChannelAgent,
            BillType.ChannelCash,
            BillType.ChannelOnCall,
            BillType.ChannelStaff,
            BillType.ChannelCredit
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
        hh.put("bts", bts);
        hh.put("class", BilledBill.class);
        hh.put("ss", getSelectedSessionInstance());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);

        // Initialize counts
        long bookedPatientCount = 0;
        long paidPatientCount = 0;
        long completedPatientCount = 0;

        // Loop through billSessions to calculate counts
        for (BillSession bs : billSessions) {
            if (bs != null) {
                // Booked count increments for every BillSession instance
                bookedPatientCount++;

                // Check if the bill session is completed
                if (bs.isCompleted()) {
                    completedPatientCount++;
                }

                // Check if the bill session is paid
                if (bs.getPaidBillSession() != null) {
                    paidPatientCount++;
                }
            }
        }

        // Set calculated counts to selectedSessionInstance
        selectedSessionInstance.setBookedPatientCount(bookedPatientCount);
        selectedSessionInstance.setPaidPatientCount(paidPatientCount);
        selectedSessionInstance.setCompletedPatientCount(completedPatientCount);

        // Assuming remainingPatientCount is calculated as booked - completed
        selectedSessionInstance.setRemainingPatientCount(bookedPatientCount - completedPatientCount);
        sessionInstanceController.save(selectedSessionInstance);
    }

    private boolean errorCheckForAddingNewBooking() {
        if (getSelectedSessionInstance() == null) {
            errorText = "Please Select Specility and Doctor.";
            JsfUtil.addErrorMessage("Please Select Specility and Doctor.");
            return true;
        }

        if (getSelectedSessionInstance().isDeactivated()) {
            errorText = "******** Doctor Leave day Can't Channel ********";
            JsfUtil.addErrorMessage("Doctor Leave day Can't Channel.");
            return true;
        }

        if (getSelectedSessionInstance().getOriginatingSession() == null) {
            errorText = "Please Select Session.";
            JsfUtil.addErrorMessage("Please Select Session");
            return true;
        }

        if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().equals("")) {
            errorText = "Can not bill without Patient.";
            JsfUtil.addErrorMessage("Can't Settle Without Patient.");
            return true;
        }
        if ((getPatient().getPerson().getPhone() == null || getPatient().getPerson().getPhone().trim().equals("")) && !getSessionController().getInstitutionPreference().isChannelSettleWithoutPatientPhoneNumber()) {
            errorText = "Can not bill without Patient Contact Number.";
            JsfUtil.addErrorMessage("Can't Settle Without Patient Contact Number.");
            return true;
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

            if (institution.getBallance() - getSelectedSessionInstance().getOriginatingSession().getTotal() < 0 - institution.getAllowedCredit()) {
                errorText = "Agency Balance is Not Enough";
                JsfUtil.addErrorMessage("Agency Balance is Not Enough");
                return true;
            }
            if (agentReferenceBookController.checkAgentReferenceNumber(getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "Invaild Reference Number.";
                JsfUtil.addErrorMessage("Invaild Reference Number.");
                return true;
            }
            if (agentReferenceBookController.checkAgentReferenceNumberAlredyExsist(getAgentRefNo(), institution, BillType.ChannelAgent, PaymentMethod.Agent) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "This Reference Number( " + getAgentRefNo() + " ) is alredy Given.";
                JsfUtil.addErrorMessage("This Reference Number is alredy Given.");
                setAgentRefNo("");
                return true;
            }
            if (agentReferenceBookController.checkAgentReferenceNumber(institution, getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
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
        if (getSelectedSessionInstance().getOriginatingSession().getMaxNo() != 0.0 && getSelectedSessionInstance().getTransDisplayCountWithoutCancelRefund() >= getSelectedSessionInstance().getOriginatingSession().getMaxNo()) {
            errorText = "No Space to Book.";
            JsfUtil.addErrorMessage("No Space to Book");
            return true;
        }

        ////System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        return false;
    }

    public void fillAbsentBillSessions(SelectEvent event) {
        selectedBillSession = null;
        selectedServiceSession = ((ServiceSession) event.getObject());

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
        List<BillType> bts = Arrays.asList(billTypes);

        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt "
                + " and bs.absent=true "
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", getSelectedServiceSession().getSessionAt());
        hh.put("ss", getSelectedServiceSession());
        billSessions = getBillSessionFacade().findByJpql(sql, hh, TemporalType.DATE);
        //absentCount=billSessions.size();

    }

    public String paySelectedDoctor() {
        if (getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Specility And Staff");
            return "";
        }
        if (getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return "";
        }
        channelStaffPaymentBillController.setSpeciality(getSpeciality());
        channelStaffPaymentBillController.setCurrentStaff(getStaff());
        channelStaffPaymentBillController.setConsiderDate(true);
        channelStaffPaymentBillController.calculateDueFees();

        return "/channel/channel_payment_staff_bill?faces-redirect=true";

    }

    public String paySelectedSession() {
        if (getSelectedSessionInstance() == null) {
            JsfUtil.addErrorMessage("Please Select Session Instance");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession() == null) {
            JsfUtil.addErrorMessage("Please Select Session");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return "";
        }
        if (selectedSessionInstance.getOriginatingSession().getStaff().getSpeciality() == null) {
            JsfUtil.addErrorMessage("Please Select Speciality");
            return "";
        }
        channelStaffPaymentBillController.setSessionInstance(selectedSessionInstance);
        channelStaffPaymentBillController.calculateSessionDueFees();
        return "/channel/channel_payment_session?faces-redirect=true";

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

    private List<BillFee> createBillFeeForSessions(Bill bill, BillItem billItem, boolean thisIsAnAdditionalFee) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpDiscount = 0;
        double tmpGrossTotal = 0.0;

        List<ItemFee> sessionsFees = null;
//        sessionsFees = findServiceSessionFees(getSelectedSessionInstance().getOriginatingSession());

        if (thisIsAnAdditionalFee) {
            sessionsFees = findItemFees(billItem.getItem());
        } else {
            if (billItem.getItem() instanceof ServiceSession) {
                sessionsFees = findServiceSessionFees((ServiceSession) billItem.getItem());
            }
        }

//        if (billItem.getItem() != null) {
//            if (billItem.getItem() instanceof ServiceSession) {
//                sessionsFees = findServiceSessionFees((ServiceSession) billItem.getItem());
//            } else if (billItem.getItem() instanceof Item) {
//                sessionsFees = findItemFees(billItem.getItem());
//            }
//        } else {
//            sessionsFees = findServiceSessionFees(getSelectedSessionInstance().getOriginatingSession());
//        }
        if (sessionsFees == null) {
            return billFeeList;
        }
        for (ItemFee f : sessionsFees) {
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

                PriceMatrix priceMatrix = priceMatrixController.getChannellingDisCount(paymentMethod, membershipScheme, f.getDepartment());
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

        billItem.setDiscount(tmpDiscount);
        billItem.setNetValue(tmpTotal);
        getBillItemFacade().edit(billItem);

        return billFeeList;

    }

    private void calculateBillTotalsFromBillFees(Bill billToCaclculate, List<BillFee> billfeesAvailable) {
        double calculatingGrossBillTotal = 0.0;
        double calculatingNetBillTotal = 0.0;

        for (BillFee iteratingBillFee : billfeesAvailable) {
            Fee currentItemFee;
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

    private Bill createBill() {
        Bill bill = new BilledBill();
        bill.setStaff(getSelectedSessionInstance().getOriginatingSession().getStaff());
        bill.setToStaff(toStaff);
        bill.setAppointmentAt(getSelectedSessionInstance().getSessionDate());
        bill.setTotal(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bill.setNetTotal(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bill.setPaymentMethod(paymentMethod);
        bill.setPatient(getPatient());
        switch (paymentMethod) {
            case OnCall:
                bill.setBillType(BillType.ChannelOnCall);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITHOUT_PAYMENT);
                break;
            case Cash:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;

            case Card:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;

            case Cheque:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;

            case Slip:
                bill.setBillType(BillType.ChannelCash);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;
            case Agent:
                bill.setBillType(BillType.ChannelAgent);
                bill.setCreditCompany(institution);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;
            case Staff:
                bill.setBillType(BillType.ChannelStaff);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;
            case Credit:
                bill.setBillType(BillType.ChannelCredit);
                bill.setBillTypeAtomic(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
                break;
        }
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
//        String insId = generateBillNumberInsId(bill);
//
//        if (insId.equals("")) {
//            return null;
//        }
//        bill.setInsId(insId);
        String deptId = generateBillNumberDeptId(bill);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
//            bill.setBookingId(billNumberBean.bookingIdGenerator(sessionController.getInstitution(), new BilledBill()));
            bill.setPaidAmount(getSelectedSessionInstance().getOriginatingSession().getTotal());
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
        bill.setCreater(getSessionController().getLoggedUser());
        bill.setDepartment(getSessionController().getDepartment());
        bill.setInstitution(sessionController.getInstitution());

        bill.setToDepartment(getSelectedSessionInstance().getDepartment());
        bill.setToInstitution(getSelectedSessionInstance().getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) {
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }
        return bill;
    }

    private String generateBillNumberDeptId(Bill bill) {
        String suffix = getSessionController().getDepartment().getDepartmentCode();
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
                deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            deptId = billNumberBean.departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        return deptId;
    }

    private BillItem createSessionItem(Bill bill) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agentRefNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setGrossValue(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setItem(getSelectedSessionInstance().getOriginatingSession());
//        bi.setItem(getSelectedSessionInstance().getOriginatingSession());
        bi.setNetRate(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setNetValue(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setQty(1.0);
        bi.setRate(getSelectedSessionInstance().getOriginatingSession().getTotal());
        bi.setSessionDate(getSelectedSessionInstance().getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }

    private BillItem createAdditionalItem(Bill bill, Item i) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agentRefNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        if (i != null) {
            bi.setGrossValue(i.getDblValue());
            bi.setItem(i);
            bi.setNetRate(i.getDblValue());
            bi.setNetValue(i.getDblValue());
            bi.setQty(1.0);
            bi.setRate(i.getDblValue());
        }
        bi.setSessionDate(getSelectedSessionInstance().getSessionAt());
        billItemFacade.create(bi);
        return bi;
    }

 
    private String generateBillNumberInsId(Bill bill) {
        String suffix = getSessionController().getInstitution().getInstitutionCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff, BillType.ChannelCredit};
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
                insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), billType, billClassType, suffix);
            } else {
                suffix += "CHANN";
                insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            suffix += "CHANNCAN";
            billClassType = BillClassType.CancelledBill;
            insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            suffix += "CHANNREF";
            billClassType = BillClassType.RefundBill;
            insId = billNumberBean.institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        return insId;
    }

    public void onEditItem(RowEditEvent event) {
        ServiceSession tmp = (ServiceSession) event.getObject();
        ServiceSession ss = getServiceSessionFacade().find(tmp.getId());
        if (ss.getMaxNo() != tmp.getMaxNo()) {
            tmp.setEditedAt(new Date());
            tmp.setEditer(getSessionController().getLoggedUser());
        }
        getServiceSessionFacade().edit(tmp);
    }

    public void listnerStaffListForRowSelect() {
        getSelectedConsultants();
        setStaff(null);
        sessionInstances = new ArrayList<>();
        selectedBillSession = null;
    }

    public void listnerStaffRowSelect() {
        getSelectedConsultants();
        setSelectedServiceSession(null);
        serviceSessionLeaveController.setSelectedServiceSession(null);
        serviceSessionLeaveController.setCurrentStaff(staff);
    }

    public void listnerSessionRowSelect() {
        if (sessionInstances == null) {
            selectedServiceSession = null;
            return;
        }
        for (SessionInstance ss : sessionInstances) {
            if (ss.getSessionText().toLowerCase().contains(selectTextSession.toLowerCase())) {
                selectedSessionInstance = ss;
            }
        }
    }

    public void listnerStaffListForSpecilitySelectedText() {
        if (doctorSpecialityController.getSelectedItems().size() > 0) {
            setSpeciality(doctorSpecialityController.getSelectedItems().get(0));
            listnerStaffListForRowSelect();
        }
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    @Deprecated
    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    @Deprecated
    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;

    }

    public void makeBillSessionNull() {
        billSessions = null;
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

    public BillSession getSelectedBillSession() {
        if (selectedBillSession == null) {
            selectedBillSession = new BillSession();
            Bill b = new BilledBill();
            Patient p = new Patient();
            p.setPerson(new Person());
            b.setPatient(p);
            selectedBillSession.setBill(b);
        }
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
        if (selectedBillSession != null) {
            Bill bill = selectedBillSession.getBill();
            if (bill != null) {
                if (bill.getBillItems() != null) {
                    bill.getBillItems().size();
                }
                if (bill.getBillFees() != null) {
                    bill.getBillFees().size();
                }
            }
        }
        getChannelCancelController().resetVariablesFromBooking();
        getChannelCancelController().setBillSession(selectedBillSession);
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            Person p = new Person();
            patientDetailsEditable = true;

            patient.setPerson(p);
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public ChannelBillController getChannelCancelController() {
        return channelCancelController;
    }

    public void setChannelCancelController(ChannelBillController channelCancelController) {
        this.channelCancelController = channelCancelController;
    }

    public ChannelReportController getChannelReportController() {
        return channelReportController;
    }

    public void setChannelReportController(ChannelReportController channelReportController) {
        this.channelReportController = channelReportController;
    }

    public Boolean preSet() {
        if (getSelectedSessionInstance() == null) {
            JsfUtil.addErrorMessage("Please select Service Session");
            return false;
        }
        getChannelReportController().setSessionInstance(selectedSessionInstance);
        return true;
    }

    public ChannelSearchController getChannelSearchController() {
        return channelSearchController;
    }

    public void setChannelSearchController(ChannelSearchController channelSearchController) {
        this.channelSearchController = channelSearchController;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return ItemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade ItemFeeFacade) {
        this.ItemFeeFacade = ItemFeeFacade;
    }

    public Date getChannelDay() {
        return channelDay;
    }

    public void setChannelDay(Date channelDay) {
        this.channelDay = channelDay;
    }

    public int getSerealNo() {
        return serealNo;
    }

    public void setSerealNo(int serealNo) {
        this.serealNo = serealNo;
    }

    public ScheduleModel getEventModel() {
        if (eventModel == null) {
            eventModel = new DefaultScheduleModel();
        }
        return eventModel;
    }

    public void setEventModel(ScheduleModel eventModel) {
        this.eventModel = eventModel;
    }

    public ChannelScheduleEvent getEvent() {
        if (event == null) {
            event = new ChannelScheduleEvent();
        }
        return event;
    }

    public void setEvent(ChannelScheduleEvent event) {
        this.event = event;
    }

    public Date getSessionStartingDate() {
        if (sessionStartingDate == null) {
            sessionStartingDate = new Date();
        }
        return sessionStartingDate;
    }

    public void setSessionStartingDate(Date sessionStartingDate) {
        this.sessionStartingDate = sessionStartingDate;
    }

    public String getSelectTextSpeciality() {
        return selectTextSpeciality;
    }

    public void setSelectTextSpeciality(String selectTextSpeciality) {
        this.selectTextSpeciality = selectTextSpeciality;
    }

    public String getSelectTextConsultant() {
        return selectTextConsultant;
    }

    public void setSelectTextConsultant(String selectTextConsultant) {
        this.selectTextConsultant = selectTextConsultant;
    }

    public String getSelectTextSession() {
        return selectTextSession;
    }

    public void setSelectTextSession(String selectTextSession) {
        this.selectTextSession = selectTextSession;
    }

    public ArrivalRecord getArrivalRecord() {
        return arrivalRecord;
    }

    public void setArrivalRecord(ArrivalRecord arrivalRecord) {
        this.arrivalRecord = arrivalRecord;
    }

    public FingerPrintRecordFacade getFpFacade() {
        return fpFacade;
    }

    public ServiceSessionLeaveController getServiceSessionLeaveController() {
        return serviceSessionLeaveController;
    }

    public ChannelBillController getChannelBillController() {
        return channelBillController;
    }

    public DoctorSpecialityController getDoctorSpecialityController() {
        return doctorSpecialityController;
    }

    public ChannelStaffPaymentBillController getChannelStaffPaymentBillController() {
        return channelStaffPaymentBillController;
    }

    @Deprecated
    public BillSession getManagingBillSession() {
        return managingBillSession;
    }

    @Deprecated
    public void setManagingBillSession(BillSession managingBillSession) {
        if (managingBillSession != null) {
            Bill bill = managingBillSession.getBill();
            if (bill != null) {
                if (bill.getBillItems() != null) {
                    bill.getBillItems().size();
                }
                if (bill.getBillFees() != null) {
                    bill.getBillFees().size();
                }
            }
        }
        this.managingBillSession = managingBillSession;
    }

    public SessionInstance getSelectedSessionInstance() {
        return selectedSessionInstance;
    }

    public void setSelectedSessionInstance(SessionInstance selectedSessionInstance) {
        this.selectedSessionInstance = selectedSessionInstance;
    }

    public List<SessionInstance> getSessionInstances() {
        return sessionInstances;
    }

    public void setSessionInstances(List<SessionInstance> sessionInstances) {
        this.sessionInstances = sessionInstances;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getAgentRefNo() {
        return agentRefNo;
    }

    public void setAgentRefNo(String agentRefNo) {
        this.agentRefNo = agentRefNo;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public boolean isForiegn() {
        return foriegn;
    }

    public void setForiegn(boolean foriegn) {
        this.foriegn = foriegn;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public List<BillFee> getListBillFees() {
        return listBillFees;
    }

    public void setListBillFees(List<BillFee> listBillFees) {
        this.listBillFees = listBillFees;
    }

    public BillSession getBillSession() {
        return billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
    }

    public boolean isSettleSucessFully() {
        return settleSucessFully;
    }

    public void setSettleSucessFully(boolean settleSucessFully) {
        this.settleSucessFully = settleSucessFully;
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
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

    public void changeListener() {
        getSelectedSessionInstance().getOriginatingSession().setTotalFee(0.0);
        getSelectedSessionInstance().getOriginatingSession().setTotalFfee(0.0);
        for (ItemFee f : getSelectedSessionInstance().getOriginatingSession().getItemFees()) {
            getSelectedSessionInstance().getOriginatingSession().setTotalFee(getSelectedSessionInstance().getOriginatingSession().getTotalFee() + f.getFee());
            getSelectedSessionInstance().getOriginatingSession().setTotalFfee(getSelectedSessionInstance().getOriginatingSession().getTotalFfee() + f.getFfee());
        }
        PaymentSchemeDiscount paymentSchemeDiscount = priceMatrixController.fetchPaymentSchemeDiscount(paymentScheme, paymentMethod);
        double d = 0;
        if (paymentSchemeDiscount != null) {
            for (ItemFee itmf : getSelectedSessionInstance().getOriginatingSession().getItemFees()) {
                if (itmf.getFeeType() == FeeType.OwnInstitution) {
                    if (foriegn) {
                        d += itmf.getFfee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    } else {
                        d += itmf.getFee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    }

                }
            }
        }
        getSelectedSessionInstance().getOriginatingSession().setTotalFee(getSelectedSessionInstance().getOriginatingSession().getTotalFee() - d);
        getSelectedSessionInstance().getOriginatingSession().setTotalFfee(getSelectedSessionInstance().getOriginatingSession().getTotalFfee() - d);
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
    }

    public List<ItemFee> getSelectedItemFees() {
        return selectedItemFees;
    }

    public List<ItemFee> getFilteredSelectedItemFees() {
        return selectedItemFees.stream()
                .filter(i -> (foriegn ? i.getFfee() != 0 : i.getFee() != 0))
                .collect(Collectors.toList());
    }

    public void setSelectedItemFees(List<ItemFee> selectedItemFees) {
        this.selectedItemFees = selectedItemFees;
    }

    public Item getItemToAddToBooking() {
        return itemToAddToBooking;
    }

    public void setItemToAddToBooking(Item itemToAddToBooking) {
        this.itemToAddToBooking = itemToAddToBooking;
    }

    public List<Item> getItemsAvailableToAddToBooking() {
        return itemsAvailableToAddToBooking;
    }

    public void setItemsAvailableToAddToBooking(List<Item> itemsAvailableToAddToBooking) {
        this.itemsAvailableToAddToBooking = itemsAvailableToAddToBooking;
    }

    private void fillItemAvailableToAdd() {
        itemsAvailableToAddToBooking = itemForItemController.getItemsForParentItem(selectedSessionInstance.getOriginatingSession());
    }

    public List<ItemFee> getSessionFees() {
        return sessionFees;
    }

    public void setSessionFees(List<ItemFee> sessionFees) {
        this.sessionFees = sessionFees;
    }

    public List<ItemFee> getAddedItemFees() {
        return addedItemFees;
    }

    public void setAddedItemFees(List<ItemFee> addedItemFees) {
        this.addedItemFees = addedItemFees;
    }

    public Double getFeeTotalForSelectedBill() {
        return feeTotalForSelectedBill;
    }

    public void setFeeTotalForSelectedBill(Double feeTotalForSelectedBill) {
        this.feeTotalForSelectedBill = feeTotalForSelectedBill;
    }

    public List<Date> getDates() {
        return dates;
    }

    public void setDates(List<Date> dates) {
        this.dates = dates;
    }

}

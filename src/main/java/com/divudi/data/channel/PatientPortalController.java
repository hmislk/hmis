package com.divudi.data.channel;

import com.divudi.bean.channel.BookingController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.DoctorController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.SmsController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillType;
import com.divudi.data.MessageType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.SmsSentResponse;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.BillSession;
import com.divudi.entity.Consultant;
import com.divudi.entity.Patient;
import com.divudi.entity.Payment;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Sms;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.UserPreference;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.SessionInstanceFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.StaffFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.primefaces.model.ScheduleModel;

/**
 *
 * @author acer
 */
@Named(value = "patientPortalController")
@ApplicationScoped
public class PatientPortalController {

    private String PatientphoneNumber;
    private boolean bookDoctor;
    private Staff selectedConsultant;
    private List<Bill> patientBills;
    private List<Payment> PatientPayments;
    private List<Payment> channelPayments;
    private List<ServiceSession> docotrSessions;
    private List<Staff> consultants;
    private List<ServiceSession> channelSessions;
    private Speciality selectedSpeciality;
    private ServiceSession selectedChannelSession;
    private Date date;
    private List<SessionInstance> sessionInstances;
    private Date sessionStartingDate;
    private String messageForSms;
    private String otp;
    private String patientEnteredOtp;
    private boolean otpVerify;
    private List<Patient> searchedPatients;
    private Patient patient;
    private boolean searchedPatientIsNull;
    private SessionInstance selectedSessionInstance;
    private boolean selectPatient;
    private boolean addNewProfile;
    private boolean addNewPatient;
    private boolean bookingCompleted;

    private List<BillSession> pastBookings;
    private List<Payment> pastPayments;

    ScheduleModel eventModel;
    Staff staff;
    ServiceSession serviceSession;

    @EJB
    private StaffFacade staffFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    SessionInstanceFacade sessionInstanceFacade;
    @EJB
    private SmsFacade SmsFacade;
    @EJB
    SmsManagerEjb smsManager;
    @EJB
    PatientFacade patientFacade;
    @EJB
    SmsFacade smsFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillSessionFacade billSessionFacade;

    @Inject
    private SessionController sessionController;
    @Inject
    DoctorController doctorController;
    @Inject
    SmsController smsController;
    @Inject
    PatientController patientController;
    @Inject
    BookingController bookingController;
    @Inject
    CommonController commonController;
    private ChannelBean channelBean;

    public List<BillSession> fillPastBookings() {
        pastBookings = null;
        Map m = new HashMap();
        String sql = "select bs from BillSession bs where bs.bill.billtype=:btype and bs.bill.patient=:pt and b.retired=:ret";
        m.put("btype", BillType.ChannelCredit);
        m.put("pt", patient);
        m.put("ret", false);
        pastBookings = billSessionFacade.findByJpql(sql, m);
        return pastBookings;
    }

    public List<Payment> fillPastPayments() {
        pastPayments = null;
        Map m = new HashMap();
        String sql = "select p from Payment p where retired=:ret and p.bill.patient=:pt";
        m.put("ret", false);
        m.put("pt", patient);
        pastPayments = paymentFacade.findByJpql(sql, m);
        return pastPayments;
    }

    public void saveNewPatient() {
        patientController.save(patient);
        addNewPatient = false;
    }

    public void reset() {
        patient = null;
        searchedPatients = null;
        otpVerify = false;
        searchedPatientIsNull = false;
        addNewProfile = false;
        bookingCompleted = false;
        addNewPatient = false;
    }

    public List<Staff> fillConsultants() {
        consultants = null;
        Map m = new HashMap();
        String sql = "select p from Staff p where p.retired=false and type(p)=:stype";
        m.put("stype", Consultant.class);
        if (selectedSpeciality != null) {
            sql += " and p.speciality= :sp";
            m.put("sp", selectedSpeciality);
        }
        consultants = staffFacade.findByJpql(sql, m);
        return consultants;
    }

    public void fillChannelSessions() {
        if (selectedConsultant != null) {
            Map m = new HashMap();
            String sql = "select s from ServiceSession s where s.retired=false and s.staff=:sd ";
            m.put("sd", selectedConsultant);
            // m.put("wd", 10);
            channelSessions = serviceSessionFacade.findByJpql(sql, m);
            System.out.println("channelSessions = " + channelSessions.size());
        }

    }

    public void addNewPatientAction() {
        addNewPatient = true;
    }

    public void GoBackfromPatientAddAction() {
        addNewPatient = false;
    }

//    public void addNewPatientAction() {
//        addNewPatient = true;
//    }
//
//    public void GoBackfromPatientAddAction() {
//        addNewPatient = false;
//    }

    public void fillSessionInstance() {
        System.out.println("working");
        if (channelSessions != null) {
            sessionInstances = new ArrayList<>();
            sessionStartingDate = new Date();
            System.out.println("selectedConsultant = " + selectedConsultant.getName());
            String jpql = "select i "
                    + " from SessionInstance i "
                    + " where i.originatingSession.staff=:os "
                    + " and i.retired=:ret ";

            Map m = new HashMap();
            m.put("ret", false);
            m.put("os", selectedConsultant);

            sessionInstances = sessionInstanceFacade.findByJpql(jpql, m, TemporalType.DATE);
            System.out.println("sessionInstances = " + sessionInstances.size());
        }
    }

    public void otpCodeConverter() {
        int codeSize = 0;
        if (sessionController.getCurrentPreference().getLengthOfOTPIndexes()== null || sessionController.getCurrentPreference().getLengthOfOTPIndexes()== "0") {
            codeSize = 4;
        }
        codeSize = Integer.parseInt(sessionController.getCurrentPreference().getLengthOfOTPIndexes());

        String numbers = "0123456789";
        Random random = new Random();
        StringBuilder otpBuilder = new StringBuilder();

        for (int i = 0; i < codeSize; i++) {
            int index = random.nextInt(numbers.length());
            otpBuilder.append(numbers.charAt(index));
        }
        otp = otpBuilder.toString();
        System.out.println("otp = " + otp);
    }

    public void sendOtp() {
        otpCodeConverter();
        if (PatientphoneNumber == null) {
            JsfUtil.addErrorMessage("Pleace Enter Phone Number");
            return;
        }

        Sms e = new Sms();
        e.setCreatedAt(new Date());
        e.setSmsType(MessageType.LabReport);
        e.setCreater(sessionController.getLoggedUser());
        e.setReceipientNumber(PatientphoneNumber);
        e.setSendingMessage("Your authentication code is " + otp);
        e.setDepartment(getSessionController().getDepartment());
        e.setInstitution(getSessionController().getInstitution());
        e.setPending(false);
        e.setOtp(otp);
        getSmsFacade().create(e);
        SmsSentResponse sent = smsManager.sendSmsByApplicationPreference(e.getReceipientNumber(), e.getSendingMessage(), sessionController.getApplicationPreference());
        e.setSentSuccessfully(sent.isSentSuccefully());
        e.setReceivedMessage(sent.getReceivedMessage());
        getSmsFacade().edit(e);
        JsfUtil.addSuccessMessage("SMS Sent");
    }

    public void findPatients() {
        if (patient == null) {
            patient = new Patient();
        }
        if (otpVerify) {
            searchedPatients = new ArrayList<>();
            String j;
            Long PatientphoneNumberLong = commonController.convertStringToLong(PatientphoneNumber);
            Map m = new HashMap();
            j = "select p from Patient p where p.retired=false and p.patientPhoneNumber=:pp";
            m.put("pp", PatientphoneNumberLong);
            searchedPatients = patientFacade.findByJpql(j, m);
            System.out.println("searchedPatients = " + searchedPatients.size());

            if (searchedPatients == null || searchedPatients.isEmpty()) {
                selectPatient = false;

                addNewPatient = true;
            }
            selectPatient = true;
            addNewPatient = false;

        }
    }

    public void otpVerification() {
        System.out.println("patientEnteredOtp = " + patientEnteredOtp);
        List<Sms> smss = new ArrayList<>();
        String j;
        Map m = new HashMap();
        j = "select s from Sms s where s.otp=:oc";
        m.put("oc", patientEnteredOtp);
        smss = smsFacade.findByJpql(j, m);
        System.out.println("smss = " + smss.size());
        if (smss.isEmpty() || smss.size() > 1) {
            JsfUtil.addErrorMessage("Enter correct authentication code");
            return;
        } else {
            otpVerify = true;
            findPatients();
        }
    }

    public void addBooking() {
        System.out.println("this = " + patient.getPerson().getName());
        bookingController.setPatient(patient);
        bookingController.setPaymentMethod(PaymentMethod.Credit);
        bookingController.setStaff(selectedConsultant);
        bookingController.setSelectedSessionInstance(selectedSessionInstance);
        bookingController.setSelectedServiceSession(selectedChannelSession);
        bookingController.add();
        bookingController.sendSmsAfterBooking();
        bookingCompleted = true;
        
        
    }

    public String getPatientphoneNumber() {
        return PatientphoneNumber;
    }

    public void setPatientphoneNumber(String PatientphoneNumber) {
        this.PatientphoneNumber = PatientphoneNumber;
    }

    public boolean isBookDoctor() {
        return bookDoctor;
    }

    public void setBookDoctor(boolean bookDoctor) {
        this.bookDoctor = bookDoctor;
    }

    public List<Bill> getPatientBills() {
        return patientBills;
    }

    public void setPatientBills(List<Bill> patientBills) {
        this.patientBills = patientBills;
    }

    public List<Payment> getPatientPayments() {
        return PatientPayments;
    }

    public void setPatientPayments(List<Payment> PatientPayments) {
        this.PatientPayments = PatientPayments;
    }

    public List<Payment> getChannelPayments() {
        return channelPayments;
    }

    public void setChannelPayments(List<Payment> channelPayments) {
        this.channelPayments = channelPayments;
    }

    public List<ServiceSession> getDocotrSessions() {
        return docotrSessions;
    }

    public void setDocotrSessions(List<ServiceSession> channels) {
        this.docotrSessions = channels;
    }

    public Staff getSelectedConsultant() {
        return selectedConsultant;
    }

    public void setSelectedConsultant(Staff selectedConsultant) {
        this.selectedConsultant = selectedConsultant;
    }

    public List<Staff> getConsultants() {
        return consultants;
    }

    public void setConsultants(List<Staff> consultants) {
        this.consultants = consultants;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Speciality getSelectedSpeciality() {
        return selectedSpeciality;
    }

    public void setSelectedSpeciality(Speciality selectedSpeciality) {
        this.selectedSpeciality = selectedSpeciality;
    }

    public List<ServiceSession> getChannelSessions() {
        return channelSessions;
    }

    public void setChannelSessions(List<ServiceSession> channelSessions) {
        this.channelSessions = channelSessions;
    }

    public ServiceSession getSelectedChannelSession() {
        return selectedChannelSession;
    }

    public void setSelectedChannelSession(ServiceSession selectedChannelSession) {
        this.selectedChannelSession = selectedChannelSession;
    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
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

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public List<SessionInstance> getSessionInstances() {
        return sessionInstances;
    }

    public void setSessionInstances(List<SessionInstance> sessionInstances) {
        this.sessionInstances = sessionInstances;
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

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getMessageForSms() {
        return messageForSms;
    }

    public void setMessageForSms(String messageForSms) {
        this.messageForSms = messageForSms;
    }

    public SmsFacade getSmsFacade() {
        return SmsFacade;
    }

    public void setSmsFacade(SmsFacade SmsFacade) {
        this.SmsFacade = SmsFacade;
    }

    public String getPatientEnteredOtp() {
        return patientEnteredOtp;
    }

    public void setPatientEnteredOtp(String patientEnteredOtp) {
        this.patientEnteredOtp = patientEnteredOtp;
    }

    public boolean isOtpVerify() {
        return otpVerify;
    }

    public void setOtpVerify(boolean otpVerify) {
        this.otpVerify = otpVerify;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public SessionInstance getSelectedSessionInstance() {
        return selectedSessionInstance;
    }

    public void setSelectedSessionInstance(SessionInstance selectedSessionInstance) {
        this.selectedSessionInstance = selectedSessionInstance;
    }

    public List<Patient> getSearchedPatients() {
        return searchedPatients;
    }

    public void setSearchedPatients(List<Patient> searchedPatients) {
        this.searchedPatients = searchedPatients;
    }

    public boolean isSearchedPatientIsNull() {
        return searchedPatientIsNull;
    }

    public void setSearchedPatientIsNull(boolean searchedPatientIsNull) {
        this.searchedPatientIsNull = searchedPatientIsNull;
    }

    public boolean isSelectPatient() {
        return selectPatient;
    }

    public void setSelectPatient(boolean selectPatient) {
        this.selectPatient = selectPatient;
    }

    public boolean isAddNewProfile() {
        return addNewProfile;
    }

    public void setAddNewProfile(boolean addNewProfile) {
        this.addNewProfile = addNewProfile;
    }

    public boolean isAddNewPatient() {
        return addNewPatient;
    }

    public void setAddNewPatient(boolean addNewPatient) {
        this.addNewPatient = addNewPatient;
    }

    public boolean isBookingCompleted() {
        return bookingCompleted;
    }

    public void setBookingCompleted(boolean bookingCompleted) {
        this.bookingCompleted = bookingCompleted;
    }

    public List<BillSession> getPastBookings() {
        return pastBookings;
    }

    public void setPastBookings(List<BillSession> pastBookings) {
        this.pastBookings = pastBookings;
    }

    public List<Payment> getPastPayments() {
        return pastPayments;
    }

    public void setPastPayments(List<Payment> pastPayments) {
        this.pastPayments = pastPayments;
    }

}

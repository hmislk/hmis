package com.divudi.bean.channel;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PatientController;
import com.divudi.bean.common.PaymentGatewayController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.hr.StaffController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.MessageType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillSession;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentGatewayTransaction;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.ServiceSession;
import com.divudi.core.entity.Sms;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.facade.BillSessionFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.ServiceSessionFacade;
import com.divudi.core.facade.SessionInstanceFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.facade.StaffFacade;
import java.io.Serializable;
import java.util.*;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.persistence.TemporalType;

import com.divudi.core.util.CommonFunctions;

/**
 *
 * @author acer
 */
@Named
@ViewScoped
public class PatientPortalController implements Serializable {

    
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
    BillSessionFacade billSessionFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;
    @Inject
    PatientController patientController;
    @Inject
    BookingController bookingController;
    @Inject
    PaymentGatewayController paymentGatewayController;
    @Inject
    ChannelBillController channelBillController;
    @Inject
    StaffController staffController;
    @Inject
    BookingControllerViewScope bookingControllerViewScope;

    private String patientphoneNumber;
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
    private boolean patientSelected;
    private boolean addNewProfile;
    private boolean addNewPatient;
    private boolean otpSendSuccess = false;
    private boolean bookingCompleted;
    private Date otpSentTime;

    private List<BillSession> pastBookings;
    private List<Payment> pastPayments;

    private BillSession channelBookingBillSession;
    private BillSession channelSettlingBillSession;

    private PaymentGatewayTransaction currentPaymentGatewayTransaction;

    private ChannelBean channelBean;

    private boolean allowLabaratory;
    private boolean allowChannellinig;
    boolean allowNavigation;

    public String navigateBookingMenue() {
        bookingControllerViewScope.fillBillSessions(Collections.singletonList(selectedSessionInstance));
        sessionInstances = null;
        selectedConsultant = null;
        selectedSpeciality = null;
        selectedSessionInstance = null;
        String oldURLMethord = CommonFunctions.getBaseUrl() + "faces/channel/patient_portal.xhtml";
        return "/channel/patient_portal.xhtml";
    }

    public String navigateToPayBooking() {
        if (selectedSessionInstance != null) {

            bookingController.setPatient(patient);
            bookingController.setPaymentMethod(PaymentMethod.OnlineSettlement);
            bookingController.setStaff(selectedConsultant);
            bookingController.setSelectedSessionInstance(selectedSessionInstance);
            bookingController.setSelectedServiceSession(selectedChannelSession);

            double amount = selectedSessionInstance.getOriginatingSession().getTotal();
            paymentGatewayController.setOrderAmount(String.valueOf(amount));
            paymentGatewayController.setOrderId(String.valueOf(selectedSessionInstance.getId()));
            paymentGatewayController.setPatient(patient);
            paymentGatewayController.generateTemplateForOrderDescription();
            paymentGatewayController.setSelectedSessioninstance(selectedSessionInstance);
            return paymentGatewayController.createCheckoutSession();
        }
        return null;

    }

    public void booking() {
        if (selectedSessionInstance != null) {
            channelBookingBillSession = bookingController.addChannelBookingForOnlinePayment();
            bookingCompleted = false;
        }
    }

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
        if (patient == null || patient.getPerson() == null) {
            com.divudi.core.util.JsfUtil.addErrorMessage("Error in Development.");
            return;
        }

        if (patient.getPerson().getTitle() == null) {
            com.divudi.core.util.JsfUtil.addErrorMessage("Title is Required.");
            return;
        }

        if (patient.getPerson().getName() == null || patient.getPerson().getName().trim().isEmpty()) {
            com.divudi.core.util.JsfUtil.addErrorMessage("Patient Name is Required.");
            return;
        }

        if (patient.getPerson().getSex() == null) {
            com.divudi.core.util.JsfUtil.addErrorMessage("Patient Gender is Required.");
            return;
        }

        if (patient.getPerson().getDob() == null) {
            com.divudi.core.util.JsfUtil.addErrorMessage("Patient DOB is Required.");
            return;
        }

        if (!Person.checkAgeSex(patient.getPerson().getDob(), patient.getPerson().getSex(), patient.getPerson().getTitle())) {
            JsfUtil.addErrorMessage("Mismatch in Title and Gender. Please Check the Title, Age and Sex");
            return;
        }

        Long phoneAsLong = com.divudi.core.util.CommonFunctions.convertStringToLongOrZero(patientphoneNumber);
        patient.setPatientPhoneNumber(phoneAsLong);
        patient.setPatientMobileNumber(phoneAsLong);
        patient.getPerson().setPhone(patientphoneNumber);
        patient.getPerson().setMobile(patientphoneNumber);
        patient.setSelfRegistered(true);
        patientController.save(patient);
        addNewPatient = false;
        patientSelected = true;
        Long phoneAsLongForSearch = com.divudi.core.util.CommonFunctions.convertStringToLongOrZero(patientphoneNumber);
        java.util.Map<String, Object> searchMap = new java.util.HashMap<>();
        searchMap.put("pp", phoneAsLongForSearch);
        searchedPatients = patientFacade.findByJpql("select p from Patient p where p.retired=false and p.patientPhoneNumber=:pp", searchMap);
        
    }

    public void selectPatientProfile(Patient selectedPt) {
        this.patient = selectedPt;
        this.patientSelected = true;
        this.selectPatient = false;
        this.addNewPatient = false;
    }

    public void reset() {
        patient = null;
        searchedPatients = null;
        otpVerify = false;
        searchedPatientIsNull = false;
        addNewProfile = false;
        bookingCompleted = false;
        addNewPatient = false;
        patientSelected = false;
        sessionInstances = null;
        otpSendSuccess = false;
        patientEnteredOtp = null;
        otpSentTime = null;
        allowLabaratory = false;
        allowChannellinig = false;
        allowNavigation = false;
        patientphoneNumber = null;
        otp = null;
    }

    public void navigateToLabaratory() {
        patientSelected = false;
        allowLabaratory = true;
        allowChannellinig = false;
        allowNavigation = true;
    }

    public void navigateToChannellinig() {
        patientSelected = false;
        allowLabaratory = false;
        allowChannellinig = true;
        allowNavigation = true;
    }

    public void navigateToBackProfile() {
        patientSelected = true;
        allowLabaratory = false;
        allowChannellinig = false;
        allowNavigation = false;
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
        }

    }

    public void addNewPatientAction() {
        com.divudi.core.entity.Person person = new com.divudi.core.entity.Person();
        person.setPhone(patientphoneNumber);
        person.setMobile(patientphoneNumber);
        patient = new Patient();
        patient.setPerson(person);
        addNewPatient = true;
        patientSelected = false;
    }

    public void GoBackfromPatientAddAction() {
        addNewPatient = false;
        patient = null;
    }

//    public void addNewPatientAction() {
//        addNewPatient = true;
//    }
//
//    public void GoBackfromPatientAddAction() {
//        addNewPatient = false;
//    }
    public void fillSessionInstance() {
        sessionInstances = new ArrayList<>();
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select i from SessionInstance i where i.retired=:ret and i.originatingSession.excludeFromPatientPortal=:epp and i.sessionDate BETWEEN :cd AND :nextTwoDays");

        if (selectedConsultant != null) {
            jpql.append(" and i.originatingSession.staff=:os");
            m.put("os", selectedConsultant);
        }

        if (selectedSpeciality != null) {
            List<Staff> staffListBySelectedSpeciality = staffController.getSpecialityStaff(selectedSpeciality);
            jpql.append(" and i.originatingSession.staff in :staffs");
            m.put("staffs", staffListBySelectedSpeciality);
        }

        m.put("ret", false);
        m.put("epp", false);
        m.put("cd", currentDate);
        m.put("nextTwoDays", calendar.getTime());

        sessionInstances = sessionInstanceFacade.findByJpql(jpql.toString(), m, TemporalType.DATE);

        bookingControllerViewScope.fillBillSessions(sessionInstances);
    }

    public void otpCodeConverter() {
        Long codeSize = configOptionApplicationController.getLongValueByKey("Patient Portal - OTP Length", 6L);
        String numbers = "0123456789";
        Random random = new Random();
        StringBuilder otpBuilder = new StringBuilder();

        for (int i = 0; i < codeSize; i++) {
            int index = random.nextInt(numbers.length());
            otpBuilder.append(numbers.charAt(index));
        }
        otp = otpBuilder.toString();
    }

    public void sendOtp() {
        java.util.Iterator<javax.faces.application.FacesMessage> iter = javax.faces.context.FacesContext.getCurrentInstance().getMessages();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        if (patientphoneNumber == null || patientphoneNumber.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Mobile number is required.");
        } else if (!patientphoneNumber.trim().matches("^(07[0-9]{8}|\\+[1-9][0-9]{6,14})$")) {
            JsfUtil.addErrorMessage("Invalid mobile number. Enter a local number (e.g. 0712345678) or international format with country code (e.g. +447911123456).");
        } else {
            otpCodeConverter();
            otpSendSuccess = false;
            Sms e = new Sms();
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            e.setReceipientNumber(patientphoneNumber);
            e.setSendingMessage(smsBody(otp));
            e.setPending(false);
            e.setSmsType(MessageType.PatientPortalOTP);
            e.setOtp(otp);
            getSmsFacade().create(e);
            Boolean sent = smsManager.sendSms(e);
            if (sent) {
                otpSendSuccess = true;
                otpSentTime = new Date();
                System.out.println("otpSendSuccess = " + otpSendSuccess);
                System.out.println("Successfuly OTP Send");
                JsfUtil.addErrorMessage("Successfuly OTP Send");
            } else {
                // for tempory use
                otpSendSuccess = true;

                // for original
//            otpSendSuccess = false;
                System.out.println("otpSendSuccess = " + otpSendSuccess);
                System.out.println("OTP SMS Failed");
                JsfUtil.addErrorMessage("OTP SMS Failed");

            }
            e.setSentSuccessfully(sent);
            getSmsFacade().edit(e);
            otpSentTime = new Date();
        }
    }

    public String smsBody(String otp) {
        String smsBody = "";
        String otpSendingTemplate = configOptionApplicationController.getLongTextValueByKey("Patient Portal - Custom SMS Body Massage for Send OTP");
        if (!otpSendingTemplate.equalsIgnoreCase("")) {
            smsBody = replaceOTPSMSBody(otpSendingTemplate, otp);
        } else {
            smsBody = "Your authentication code is " + otp;
        }
        return smsBody;
    }

    public String replaceOTPSMSBody(String template, String otp) {
        String output;
        String processedTemplate = template.replace("\\n", "\n");
        output = processedTemplate.replace("{otp}", otp);
        return output;
    }

    public Date getSessionStartDateTime(SessionInstance session) {
        Calendar sessionDateTimeCal = Calendar.getInstance();
        sessionDateTimeCal.setTime(session.getSessionDate()); // Assuming session has a getSessionDate method

        Calendar sessionTimeCal = Calendar.getInstance();
        sessionTimeCal.setTime(session.getSessionTime()); // Assuming session has a getSessionTime method

        // Combine session date and time
        sessionDateTimeCal.set(Calendar.HOUR_OF_DAY, sessionTimeCal.get(Calendar.HOUR_OF_DAY));
        sessionDateTimeCal.set(Calendar.MINUTE, sessionTimeCal.get(Calendar.MINUTE));
        sessionDateTimeCal.set(Calendar.SECOND, sessionTimeCal.get(Calendar.SECOND));

        return sessionDateTimeCal.getTime();
    }

    public void findPatients() {
        System.out.println("Run Start findPatients()");

        patient = new Patient();

        if (otpVerify) {
            searchedPatients = new ArrayList<>();
            String j;
            Long PatientphoneNumberLong = CommonFunctions.convertStringToLongOrZero(patientphoneNumber);
            Map m = new HashMap();
            j = "select p from Patient p where p.retired=false and p.patientPhoneNumber=:pp";
            m.put("pp", PatientphoneNumberLong);
            searchedPatients = patientFacade.findByJpql(j, m);

            System.out.println("searchedPatients = " + searchedPatients);

            if (searchedPatients == null || searchedPatients.isEmpty()) {
                patient.setPerson(new Person());
                System.out.println("Patient not Found ( Active - Add New Patinet)");
                selectPatient = false;
                addNewPatient = true;
            } else if (searchedPatients.size() == 1) {
                System.out.println("Found one Patient ( Active - Set Found Patient)");
                setPatient(searchedPatients.get(0));
                patientSelected = true;
                selectPatient = false;
                addNewPatient = false;
            } else {
                System.out.println("Found Multiple Patients ( Active - Select Patient from List)");
                selectPatient = true;
                addNewPatient = false;
            }
        }
    }

    public int getOtpTimeoutMinutes() {
        return configOptionApplicationController.getIntegerValueByKey("Patient Portal - OTP Timeout Minutes", 2);
    }

    public long getOtpExpiryEpochMs() {
        if (otpSentTime == null) {
            return 0;
        }
        return otpSentTime.getTime() + ((long) getOtpTimeoutMinutes() * 60 * 1000L);
    }

    public void otpVerification() {
        System.out.println("OTP Verifycation Start");

        if (otpSentTime != null && System.currentTimeMillis() > getOtpExpiryEpochMs()) {
            System.out.println("OTP Expired.");
            JsfUtil.addErrorMessage("OTP has expired. Please request a new OTP.");
            otp = null;
            patientEnteredOtp = null;
            otpSentTime = null;
            return;
        }

        String Jpql;
        Map m = new HashMap();
        Jpql = "select s from Sms s where s.retired =:ret and s.receipientNumber =:mobile and s.smsType =:type order by s.id desc";
        m.put("ret", false);
        m.put("type", MessageType.PatientPortalOTP);
        m.put("mobile", patientphoneNumber);

        Sms sms = smsFacade.findFirstByJpql(Jpql, m);

        System.out.println("Last Sended SMS = " + sms);

        if (sms == null) {
            System.out.println("Authentication Request SMS not Found.");
            otpVerify = false;
            patientEnteredOtp = null;
            JsfUtil.addErrorMessage("Authentication Request SMS Fail.");
        } else {
            if (patientEnteredOtp == null || patientEnteredOtp.trim().isEmpty()) {
                System.out.println("---> OTP Code is Missing. <---");
                JsfUtil.addErrorMessage("Enter the authentication code.");
            }else{
                if (patientEnteredOtp.equalsIgnoreCase(sms.getOtp())) {
                    System.out.println("---> OTP Authentication Pass. <---");
                    otpVerify = true;
                    findPatients();
                } else {
                    System.out.println("<--- OTP Authentication Fail. --->");
                    otpVerify = false;
                    patientEnteredOtp = null;
                    JsfUtil.addErrorMessage("Enter correct authentication code");
                }
            }
            
        }
    }

    public void completeBooking() {
        if (channelBookingBillSession == null) {
            JsfUtil.addErrorMessage("No Chanel Booking Session. Please contact system administator");
            return;
        }
        if (channelBookingBillSession.getPaidBillSession() != null) {
            JsfUtil.addErrorMessage("This is already Paid");
            return;
        }
        channelSettlingBillSession = channelBillController.settleCreditForOnlinePayments(channelBookingBillSession);
        bookingCompleted = true;
    }

    public String getPatientphoneNumber() {
        return patientphoneNumber;
    }

    public void setPatientphoneNumber(String patientphoneNumber) {
        this.patientphoneNumber = patientphoneNumber;
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

    public boolean isPatientSelected() {
        return patientSelected;
    }

    public void setPatientSelected(boolean patientSelected) {
        this.patientSelected = patientSelected;
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

    public BillSession getChannelBookingBillSession() {
        return channelBookingBillSession;
    }

    public void setChannelBookingBillSession(BillSession channelBookingBillSession) {
        this.channelBookingBillSession = channelBookingBillSession;
    }

    public BillSession getChannelSettlingBillSession() {
        return channelSettlingBillSession;
    }

    public void setChannelSettlingBillSession(BillSession channelSettlingBillSession) {
        this.channelSettlingBillSession = channelSettlingBillSession;
    }

    public PaymentGatewayTransaction getCurrentPaymentGatewayTransaction() {
        return currentPaymentGatewayTransaction;
    }

    public void setCurrentPaymentGatewayTransaction(PaymentGatewayTransaction currentPaymentGatewayTransaction) {
        this.currentPaymentGatewayTransaction = currentPaymentGatewayTransaction;
    }

    public boolean isOtpSendSuccess() {
        return otpSendSuccess;
    }

    public void setOtpSendSuccess(boolean otpSendSuccess) {
        this.otpSendSuccess = otpSendSuccess;
    }

    public boolean isAllowLabaratory() {
        return allowLabaratory;
    }

    public void setAllowLabaratory(boolean allowLabaratory) {
        this.allowLabaratory = allowLabaratory;
    }

    public boolean isAllowChannellinig() {
        return allowChannellinig;
    }

    public void setAllowChannellinig(boolean allowChannellinig) {
        this.allowChannellinig = allowChannellinig;
    }

    public boolean isAllowNavigation() {
        return allowNavigation;
    }

    public void setAllowNavigation(boolean allowNavigation) {
        this.allowNavigation = allowNavigation;
    }

}

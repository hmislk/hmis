package com.divudi.bean.clientportal;

import com.divudi.bean.common.ApplicationController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.ClientAccountCreationChannel;
import com.divudi.core.data.MessageType;
import com.divudi.core.data.PatientRegistrationSource;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.ClientAccount;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.Sms;
import com.divudi.core.facade.ClientAccountFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.util.ClientPortalIpAllowlist;
import com.divudi.core.util.ClientPortalMatcher;
import com.divudi.core.util.ClientPortalOtpGenerator;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.ejb.SmsManagerEjb;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * Kiosk registration channel (design spec &sect;2 item 4): IP-restricted, on-premises
 * terminal. Like the phone/email self-service channels, still requires OTP to the
 * entered contact. Unlike them, a zero-match result does not reject the visitor —
 * it offers to create a brand-new Patient + Person record on the spot.
 *
 * Issue: hmislk/hmis#22369
 */
@Named
@ViewScoped
public class ClientPortalKioskRegistrationController implements Serializable {

    public enum ContactType {
        PHONE, EMAIL
    }

    @EJB
    private SmsFacade smsFacade;
    @EJB
    private SmsManagerEjb smsManager;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private ClientAccountFacade clientAccountFacade;
    @EJB
    private InstitutionFacade institutionFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;
    @Inject
    private SecurityController securityController;
    @Inject
    private ApplicationController applicationController;

    private boolean ipAllowed;

    private ContactType contactType = ContactType.PHONE;
    private String phoneNumber;
    private String emailAddress;
    private String enteredOtp;
    private String otp;
    private Date otpSentTime;
    private boolean otpSendSuccess;
    private boolean otpVerified;

    private List<Patient> matchedPatients;
    private ClientPortalMatcher.MatchResult matchResult;
    private Patient selectedPatient;

    private boolean creatingNewPatient;
    private Patient newPatient;

    private String password;
    private String passwordConfirm;
    private boolean registrationComplete;
    private boolean duplicateAccountBlocked;

    @PostConstruct
    public void init() {
        String requestIp = resolveRequestIp();
        String allowedIpsCsv = configOptionApplicationController.getLongTextValueByKey("Client Portal - Kiosk Allowed IPs", "");
        ipAllowed = ClientPortalIpAllowlist.isAllowed(requestIp, allowedIpsCsv);
    }

    private String resolveRequestIp() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null || context.getExternalContext() == null) {
            return null;
        }
        Object request = context.getExternalContext().getRequest();
        if (!(request instanceof HttpServletRequest)) {
            return null;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String forwardedFor = httpRequest.getHeader("X-FORWARDED-FOR");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.trim();
        }
        return httpRequest.getRemoteAddr();
    }

    private void clearMessages() {
        java.util.Iterator<javax.faces.application.FacesMessage> iter = FacesContext.getCurrentInstance().getMessages();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    public void switchContactType(ContactType type) {
        clearMessages();
        this.contactType = type;
        otpSendSuccess = false;
        otpVerified = false;
    }

    public void sendOtp() {
        clearMessages();
        registrationComplete = false;
        duplicateAccountBlocked = false;
        creatingNewPatient = false;
        newPatient = null;

        if (!ipAllowed) {
            JsfUtil.addErrorMessage("This terminal is not authorized for kiosk registration.");
            return;
        }

        if (contactType == ContactType.PHONE) {
            sendPhoneOtp();
        } else {
            sendEmailOtp();
        }
    }

    private void sendPhoneOtp() {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Mobile number is required.");
            return;
        }
        if (!phoneNumber.trim().matches("^(07[0-9]{8}|\\+[1-9][0-9]{6,14})$")) {
            JsfUtil.addErrorMessage("Invalid mobile number. Enter a local number (e.g. 0712345678) or international format with country code (e.g. +447911123456).");
            return;
        }

        otp = ClientPortalOtpGenerator.generate(getOtpLength());
        otpSendSuccess = false;

        Sms sms = new Sms();
        sms.setCreatedAt(new Date());
        sms.setCreater(sessionController.getLoggedUser());
        sms.setReceipientNumber(phoneNumber);
        sms.setSendingMessage(smsBody(otp));
        sms.setPending(false);
        sms.setSmsType(MessageType.ClientPortalRegistrationOTP);
        sms.setOtp(otp);
        smsFacade.create(sms);

        Boolean sent = smsManager.sendSms(sms);
        // As in ClientPortalPhoneRegistrationController: no SMS gateway configured
        // locally always returns false; advance regardless so local dev/testing works.
        otpSendSuccess = true;
        otpSentTime = new Date();
        if (Boolean.TRUE.equals(sent)) {
            JsfUtil.addSuccessMessage("OTP sent successfully.");
        } else {
            JsfUtil.addErrorMessage("OTP SMS failed to send.");
        }
        sms.setSentSuccessfully(sent);
        smsFacade.edit(sms);
    }

    private void sendEmailOtp() {
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Email address is required.");
            return;
        }
        if (!emailAddress.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            JsfUtil.addErrorMessage("Invalid email address. Enter a valid email address (e.g. name@example.com).");
            return;
        }

        otp = ClientPortalOtpGenerator.generate(getOtpLength());
        otpSendSuccess = false;

        AppEmail email = new AppEmail();
        email.setReceipientEmail(emailAddress);
        email.setMessageBody(emailBody(otp));
        email.setMessageSubject(emailSubject());
        email.setMessageType(MessageType.ClientPortalEmailRegistrationOTP);
        email.setOtp(otp);
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setPending(false);
        emailFacade.create(email);

        boolean sent = emailManagerEjb.sendEmail(Collections.singletonList(emailAddress), emailBody(otp), emailSubject(), false);
        otpSendSuccess = true;
        otpSentTime = new Date();
        if (sent) {
            JsfUtil.addSuccessMessage("OTP sent successfully.");
        } else {
            JsfUtil.addErrorMessage("OTP email failed to send.");
        }
        email.setSentSuccessfully(sent);
        if (sent) {
            email.setSentAt(new Date());
        }
        emailFacade.edit(email);
    }

    public String smsBody(String otpCode) {
        String template = configOptionApplicationController.getLongTextValueByKey("Client Portal - Custom SMS Body Message for Send OTP");
        if (template != null && !template.trim().isEmpty()) {
            return template.replace("\\n", "\n").replace("{otp}", otpCode);
        }
        return "Your Client Portal registration code is " + otpCode;
    }

    public String emailBody(String otpCode) {
        String template = configOptionApplicationController.getLongTextValueByKey("Client Portal - Custom Email Body Message for Send OTP");
        if (template != null && !template.trim().isEmpty()) {
            return template.replace("\\n", "\n").replace("{otp}", otpCode);
        }
        return "Your Client Portal registration code is " + otpCode;
    }

    public String emailSubject() {
        String subject = configOptionApplicationController.getLongTextValueByKey("Client Portal - Custom Email Subject for Send OTP");
        if (subject != null && !subject.trim().isEmpty()) {
            return subject;
        }
        return "Your Client Portal Registration Code";
    }

    public void verifyOtp() {
        clearMessages();
        if (!ipAllowed) {
            JsfUtil.addErrorMessage("This terminal is not authorized for kiosk registration.");
            return;
        }
        if (contactType == ContactType.PHONE) {
            verifyPhoneOtp();
        } else {
            verifyEmailOtp();
        }
    }

    private void verifyPhoneOtp() {
        String jpql = "select s from Sms s where s.retired=false and s.receipientNumber=:mobile and s.smsType=:type order by s.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("mobile", phoneNumber);
        params.put("type", MessageType.ClientPortalRegistrationOTP);
        Sms lastSms = smsFacade.findFirstByJpql(jpql, params);

        if (lastSms == null) {
            JsfUtil.addErrorMessage("No OTP request found. Please request a new OTP.");
            return;
        }

        long ageMs = System.currentTimeMillis() - lastSms.getCreatedAt().getTime();
        if (ageMs > getOtpTimeoutMinutes() * 60_000L) {
            JsfUtil.addErrorMessage("OTP has expired. Please request a new OTP.");
            resetOtpState();
            return;
        }

        if (enteredOtp == null || enteredOtp.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter the authentication code.");
            return;
        }
        if (lastSms.getOtp() == null || !enteredOtp.equalsIgnoreCase(lastSms.getOtp())) {
            JsfUtil.addErrorMessage("Enter correct authentication code.");
            enteredOtp = null;
            return;
        }

        // Single-use: consume the OTP so a captured/observed code can't be replayed.
        lastSms.setOtp(null);
        smsFacade.edit(lastSms);

        otpVerified = true;
        findMatchingPatientsByPhone();
    }

    private void verifyEmailOtp() {
        String jpql = "select e from AppEmail e where e.retired=false and e.receipientEmail=:email and e.messageType=:type order by e.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("email", emailAddress);
        params.put("type", MessageType.ClientPortalEmailRegistrationOTP);
        AppEmail lastEmail = emailFacade.findFirstByJpql(jpql, params);

        if (lastEmail == null) {
            JsfUtil.addErrorMessage("No OTP request found. Please request a new OTP.");
            return;
        }

        long ageMs = System.currentTimeMillis() - lastEmail.getCreatedAt().getTime();
        if (ageMs > getOtpTimeoutMinutes() * 60_000L) {
            JsfUtil.addErrorMessage("OTP has expired. Please request a new OTP.");
            resetOtpState();
            return;
        }

        if (enteredOtp == null || enteredOtp.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter the authentication code.");
            return;
        }
        if (lastEmail.getOtp() == null || !enteredOtp.equalsIgnoreCase(lastEmail.getOtp())) {
            JsfUtil.addErrorMessage("Enter correct authentication code.");
            enteredOtp = null;
            return;
        }

        // Single-use: consume the OTP so a captured/observed code can't be replayed.
        lastEmail.setOtp(null);
        emailFacade.edit(lastEmail);

        otpVerified = true;
        findMatchingPatientsByEmail();
    }

    private void resetOtpState() {
        otp = null;
        enteredOtp = null;
        otpSentTime = null;
        otpSendSuccess = false;
    }

    private void findMatchingPatientsByPhone() {
        Long phoneAsLong = CommonFunctions.convertStringToLongOrZero(phoneNumber);
        String jpql = "select p from Patient p where p.retired=false and p.patientPhoneNumber=:pp";
        Map<String, Object> params = new HashMap<>();
        params.put("pp", phoneAsLong);
        matchedPatients = patientFacade.findByJpql(jpql, params);
        classifyMatches();
    }

    private void findMatchingPatientsByEmail() {
        String jpql = "select p from Patient p where p.retired=false and p.person.email=:email";
        Map<String, Object> params = new HashMap<>();
        params.put("email", emailAddress);
        matchedPatients = patientFacade.findByJpql(jpql, params);
        classifyMatches();
    }

    private void classifyMatches() {
        matchResult = ClientPortalMatcher.classify(matchedPatients);
        switch (matchResult) {
            case NO_MATCH:
                startNewPatient();
                break;
            case SINGLE_MATCH:
                selectPatientProfile(matchedPatients.get(0));
                break;
            case MULTIPLE_MATCH:
                // rendered as a list for selectPatientProfile(Patient)
                break;
            default:
                break;
        }
    }

    // Unlike phone/email self-service, a zero-match result is not rejected here —
    // the kiosk is the one channel that may onboard someone with no prior record.
    private void startNewPatient() {
        creatingNewPatient = true;
        newPatient = new Patient();
        newPatient.setPerson(new Person());
        if (contactType == ContactType.PHONE) {
            newPatient.getPerson().setPhone(phoneNumber);
            newPatient.getPerson().setMobile(phoneNumber);
        } else {
            newPatient.getPerson().setEmail(emailAddress);
        }
    }

    public void updateSexByTitle() {
        if (newPatient == null || newPatient.getPerson() == null || newPatient.getPerson().getTitle() == null) {
            return;
        }
        Title title = newPatient.getPerson().getTitle();
        switch (title) {
            case Mrs:
            case Ms:
            case Miss:
            case DrMrs:
            case DrMs:
            case DrMiss:
            case ProfMrs:
                newPatient.getPerson().setSex(Sex.Female);
                break;
            case Mr:
            case Master:
                newPatient.getPerson().setSex(Sex.Male);
                break;
            default:
                break;
        }
    }

    public void saveNewPatient() {
        clearMessages();
        if (newPatient == null || newPatient.getPerson() == null) {
            JsfUtil.addErrorMessage("Unexpected error. Please start over.");
            return;
        }
        Person person = newPatient.getPerson();
        if (person.getTitle() == null) {
            JsfUtil.addErrorMessage("Title is required.");
            return;
        }
        if (person.getName() == null || person.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Full name is required.");
            return;
        }
        if (person.getSex() == null) {
            JsfUtil.addErrorMessage("Gender is required.");
            return;
        }
        if (person.getDob() == null) {
            JsfUtil.addErrorMessage("Date of birth is required.");
            return;
        }
        if (!Person.checkAgeSex(person.getDob(), person.getSex(), person.getTitle())) {
            JsfUtil.addErrorMessage("Title and Gender do not match. Please check Title, Age and Sex.");
            return;
        }

        if (contactType == ContactType.PHONE) {
            Long phone = CommonFunctions.convertStringToLongOrZero(phoneNumber);
            newPatient.setPatientPhoneNumber(phone);
            newPatient.setPatientMobileNumber(phone);
        }

        // Mirrors PatientController.saveSelected()'s new-Person-then-new-Patient save
        // path (issue #22369 description). No staff creater/institution: the kiosk is
        // an unauthenticated, IP-gated public terminal, same as the phone/email
        // self-service channels.
        person.setCreatedAt(new Date());
        personFacade.create(person);

        newPatient.setRegistrationSource(PatientRegistrationSource.KIOSK);
        newPatient.setCreatedAt(new Date());
        String phn = applicationController.createNewPersonalHealthNumber(institutionFacade.findDefaultInstitution());
        if (phn != null) {
            newPatient.setPhn(phn);
        }
        patientFacade.createAndFlush(newPatient);

        creatingNewPatient = false;
        selectPatientProfile(newPatient);
    }

    public void selectPatientProfile(Patient patient) {
        this.selectedPatient = patient;
        checkDuplicateAccount();
    }

    private void checkDuplicateAccount() {
        if (selectedPatient == null || selectedPatient.getPerson() == null) {
            return;
        }
        ClientAccount existing = clientAccountFacade.findByPerson(selectedPatient.getPerson().getId());
        duplicateAccountBlocked = existing != null;
    }

    public void register() {
        clearMessages();
        if (selectedPatient == null || selectedPatient.getPerson() == null) {
            JsfUtil.addErrorMessage("Error in Development.");
            return;
        }
        if (password == null || password.length() < 6) {
            JsfUtil.addErrorMessage("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(passwordConfirm)) {
            JsfUtil.addErrorMessage("Password and confirmation do not match.");
            return;
        }

        Person person = selectedPatient.getPerson();

        ClientAccount account = new ClientAccount();
        account.setPerson(person);
        account.setPasswordHash(securityController.hashAndCheck(password));
        if (contactType == ContactType.PHONE) {
            account.setVerifiedPhone(phoneNumber);
            account.setPhoneVerified(true);
        } else {
            account.setVerifiedEmail(emailAddress);
            account.setEmailVerified(true);
        }
        account.setCreatedVia(ClientAccountCreationChannel.KIOSK);
        account.setCreatedAt(new Date());
        account.setRetired(false);

        ClientAccount created = clientAccountFacade.createIfNoActiveAccount(person.getId(), account);
        if (created == null) {
            duplicateAccountBlocked = true;
            JsfUtil.addErrorMessage("A portal account already exists for this person. Please log in or reset your password instead.");
            return;
        }

        registrationComplete = true;
    }

    public int getOtpLength() {
        Long configured = configOptionApplicationController.getLongValueByKey("Client Portal - OTP Length", 6L);
        if (configured == null || configured < 4L || configured > 12L) {
            return 6;
        }
        return configured.intValue();
    }

    public int getOtpTimeoutMinutes() {
        return configOptionApplicationController.getIntegerValueByKey("Client Portal - OTP Timeout Minutes", 2);
    }

    public long getOtpExpiryEpochMs() {
        if (otpSentTime == null) {
            return 0;
        }
        return otpSentTime.getTime() + ((long) getOtpTimeoutMinutes() * 60 * 1000L);
    }

    public boolean isIpAllowed() {
        return ipAllowed;
    }

    public ContactType getContactType() {
        return contactType;
    }

    public void setContactType(ContactType contactType) {
        this.contactType = contactType;
    }

    public boolean isPhoneChannel() {
        return contactType == ContactType.PHONE;
    }

    public boolean isEmailChannel() {
        return contactType == ContactType.EMAIL;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEnteredOtp() {
        return enteredOtp;
    }

    public void setEnteredOtp(String enteredOtp) {
        this.enteredOtp = enteredOtp;
    }

    public boolean isOtpSendSuccess() {
        return otpSendSuccess;
    }

    public void setOtpSendSuccess(boolean otpSendSuccess) {
        this.otpSendSuccess = otpSendSuccess;
    }

    public boolean isOtpVerified() {
        return otpVerified;
    }

    public void setOtpVerified(boolean otpVerified) {
        this.otpVerified = otpVerified;
    }

    public List<Patient> getMatchedPatients() {
        if (matchedPatients == null) {
            return new ArrayList<>();
        }
        return matchedPatients;
    }

    public void setMatchedPatients(List<Patient> matchedPatients) {
        this.matchedPatients = matchedPatients;
    }

    public ClientPortalMatcher.MatchResult getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(ClientPortalMatcher.MatchResult matchResult) {
        this.matchResult = matchResult;
    }

    public boolean isMultipleMatch() {
        return matchResult == ClientPortalMatcher.MatchResult.MULTIPLE_MATCH;
    }

    public boolean isCreatingNewPatient() {
        return creatingNewPatient;
    }

    public void setCreatingNewPatient(boolean creatingNewPatient) {
        this.creatingNewPatient = creatingNewPatient;
    }

    public Patient getNewPatient() {
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Title[] getTitles() {
        return Title.values();
    }

    public Sex[] getSexValues() {
        return Sex.values();
    }

    public Patient getSelectedPatient() {
        return selectedPatient;
    }

    public void setSelectedPatient(Patient selectedPatient) {
        this.selectedPatient = selectedPatient;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public boolean isRegistrationComplete() {
        return registrationComplete;
    }

    public void setRegistrationComplete(boolean registrationComplete) {
        this.registrationComplete = registrationComplete;
    }

    public boolean isDuplicateAccountBlocked() {
        return duplicateAccountBlocked;
    }

    public void setDuplicateAccountBlocked(boolean duplicateAccountBlocked) {
        this.duplicateAccountBlocked = duplicateAccountBlocked;
    }
}

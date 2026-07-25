package com.divudi.bean.clientportal;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.ClientAccountCreationChannel;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.ClientAccount;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.ClientAccountFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.util.ClientPortalMatcher;
import com.divudi.core.util.ClientPortalOtpGenerator;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.EmailManagerEjb;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ViewScoped
public class ClientPortalEmailRegistrationController implements Serializable {

    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private ClientAccountFacade clientAccountFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;
    @Inject
    private SecurityController securityController;

    private String emailAddress;
    private String enteredOtp;
    private String otp;
    private Date otpSentTime;
    private boolean otpSendSuccess;
    private boolean otpVerified;

    private List<Patient> matchedPatients;
    private ClientPortalMatcher.MatchResult matchResult;
    private Patient selectedPatient;
    private boolean noMatchRejected;

    private String password;
    private String passwordConfirm;
    private boolean registrationComplete;
    private boolean duplicateAccountBlocked;

    private void clearMessages() {
        java.util.Iterator<javax.faces.application.FacesMessage> iter = javax.faces.context.FacesContext.getCurrentInstance().getMessages();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    public void sendOtp() {
        clearMessages();
        registrationComplete = false;
        duplicateAccountBlocked = false;
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
        // Matches ClientPortalPhoneRegistrationController.sendOtp(): when no email gateway is configured
        // (e.g. local dev), sendEmail() always returns false. The OTP is still persisted
        // on the AppEmail row above, so advance to the verify step regardless of delivery
        // status rather than permanently blocking the flow when no gateway is set up.
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

        String jpql = "select e from AppEmail e where e.retired=false and e.receipientEmail=:email and e.messageType=:type order by e.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("email", emailAddress);
        params.put("type", MessageType.ClientPortalEmailRegistrationOTP);
        AppEmail lastEmail = emailFacade.findFirstByJpql(jpql, params);

        if (lastEmail == null) {
            JsfUtil.addErrorMessage("No OTP request found. Please request a new OTP.");
            return;
        }

        // Checked against the persisted AppEmail.createdAt, not the view-scoped otpSentTime,
        // so a page refresh mid-flow (which resets otpSentTime) can't bypass expiry.
        long ageMs = System.currentTimeMillis() - lastEmail.getCreatedAt().getTime();
        if (ageMs > getOtpTimeoutMinutes() * 60_000L) {
            JsfUtil.addErrorMessage("OTP has expired. Please request a new OTP.");
            otp = null;
            enteredOtp = null;
            otpSentTime = null;
            otpSendSuccess = false;
            return;
        }

        if (enteredOtp == null || enteredOtp.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter the authentication code.");
            return;
        }
        if (!enteredOtp.equalsIgnoreCase(lastEmail.getOtp())) {
            JsfUtil.addErrorMessage("Enter correct authentication code.");
            enteredOtp = null;
            return;
        }

        otpVerified = true;
        findMatchingPatients();
    }

    private void findMatchingPatients() {
        String jpql = "select p from Patient p where p.retired=false and p.person.email=:email";
        Map<String, Object> params = new HashMap<>();
        params.put("email", emailAddress);
        matchedPatients = patientFacade.findByJpql(jpql, params);

        matchResult = ClientPortalMatcher.classify(matchedPatients);
        switch (matchResult) {
            case NO_MATCH:
                noMatchRejected = true;
                JsfUtil.addErrorMessage("No existing patient record matches this email address. Please use the kiosk or ask staff to create your portal account.");
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
        account.setVerifiedEmail(emailAddress);
        account.setEmailVerified(true);
        account.setCreatedVia(ClientAccountCreationChannel.SELF_EMAIL);
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

    public boolean isNoMatchRejected() {
        return noMatchRejected;
    }

    public void setNoMatchRejected(boolean noMatchRejected) {
        this.noMatchRejected = noMatchRejected;
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

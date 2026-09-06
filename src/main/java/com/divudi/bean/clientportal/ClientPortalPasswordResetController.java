package com.divudi.bean.clientportal;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SecurityController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.ClientAccount;
import com.divudi.core.entity.Sms;
import com.divudi.core.facade.ClientAccountFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.util.ClientPortalOtpGenerator;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.SmsManagerEjb;
import java.io.Serializable;
import java.util.ArrayList;
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
public class ClientPortalPasswordResetController implements Serializable {

    @EJB
    private SmsFacade smsFacade;
    @EJB
    private SmsManagerEjb smsManager;
    @EJB
    private ClientAccountFacade clientAccountFacade;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;
    @Inject
    private SecurityController securityController;

    private String identifier;
    private List<ClientAccount> matchedAccounts;
    private ClientAccount selectedAccount;
    private boolean noAccountFound;
    private boolean emailResetUnsupported;

    private String enteredOtp;
    private String otp;
    private Date otpSentTime;
    private boolean otpSendSuccess;
    private boolean otpVerified;
    private Long verifiedSmsId;

    private String newPassword;
    private String newPasswordConfirm;
    private boolean resetComplete;

    private void clearMessages() {
        java.util.Iterator<javax.faces.application.FacesMessage> iter = javax.faces.context.FacesContext.getCurrentInstance().getMessages();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
    }

    public void findAccount() {
        clearMessages();
        resetComplete = false;
        noAccountFound = false;
        emailResetUnsupported = false;
        selectedAccount = null;
        otpSendSuccess = false;
        otpVerified = false;
        verifiedSmsId = null;
        matchedAccounts = null;

        if (identifier == null || identifier.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter the phone number or email on your account.");
            return;
        }
        String trimmed = identifier.trim();
        List<ClientAccount> candidates = clientAccountFacade.findByVerifiedPhoneOrEmail(trimmed);
        if (candidates == null || candidates.isEmpty()) {
            noAccountFound = true;
            JsfUtil.addErrorMessage("No Client Portal account found for that phone number or email. Please register first.");
            return;
        }

        // Which account(s) matched, and their names, are NOT revealed here — only
        // after OTP verification below, mirroring the registration flow's
        // OTP-then-disambiguation order. Revealing account holder names to anyone
        // who types a phone/email, before proving they control that phone/email,
        // would be a pre-authentication PII disclosure.
        boolean matchesPhone = candidates.stream().anyMatch(a -> trimmed.equals(a.getVerifiedPhone()));
        if (!matchesPhone) {
            emailResetUnsupported = true;
            JsfUtil.addErrorMessage("Password reset by email isn't available yet. Please use the phone number on your account, or contact support.");
            return;
        }

        matchedAccounts = candidates;
        sendOtp();
    }

    public void selectAccount(ClientAccount account) {
        clearMessages();
        this.selectedAccount = account;
    }

    public void sendOtp() {
        clearMessages();
        if (identifier == null || identifier.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Error in Development.");
            return;
        }
        String trimmed = identifier.trim();

        otp = ClientPortalOtpGenerator.generate(getOtpLength());
        otpSendSuccess = false;

        Sms sms = new Sms();
        sms.setCreatedAt(new Date());
        sms.setCreater(sessionController.getLoggedUser());
        sms.setReceipientNumber(trimmed);
        sms.setSendingMessage(smsBody(otp));
        sms.setPending(false);
        sms.setSmsType(MessageType.ClientPortalPasswordResetOTP);
        sms.setOtp(otp);
        smsFacade.create(sms);

        Boolean sent = smsManager.sendSms(sms);
        // Matches PatientPortalController.sendOtp(): when no SMS gateway is configured
        // (e.g. local dev), sendSms() always returns false. The OTP is still persisted
        // on the Sms row above, so advance to the verify step regardless of delivery
        // status rather than permanently blocking the flow when no gateway is set up.
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

    public String smsBody(String otpCode) {
        String template = configOptionApplicationController.getLongTextValueByKey("Client Portal - Custom SMS Body Message for Password Reset OTP");
        if (template != null && !template.trim().isEmpty()) {
            return template.replace("\\n", "\n").replace("{otp}", otpCode);
        }
        return "Your Client Portal password reset code is " + otpCode;
    }

    public void verifyOtp() {
        clearMessages();

        if (identifier == null || identifier.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Error in Development.");
            return;
        }
        String trimmed = identifier.trim();

        String jpql = "select s from Sms s where s.retired=false and s.receipientNumber=:mobile and s.smsType=:type order by s.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("mobile", trimmed);
        params.put("type", MessageType.ClientPortalPasswordResetOTP);
        Sms lastSms = smsFacade.findFirstByJpql(jpql, params);

        if (lastSms == null) {
            JsfUtil.addErrorMessage("No OTP request found. Please request a new OTP.");
            return;
        }

        // Checked against the persisted Sms.createdAt, not the view-scoped otpSentTime,
        // so a page refresh mid-flow (which resets otpSentTime) can't bypass expiry.
        long ageMs = System.currentTimeMillis() - lastSms.getCreatedAt().getTime();
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
        if (!enteredOtp.equalsIgnoreCase(lastSms.getOtp())) {
            JsfUtil.addErrorMessage("Enter correct authentication code.");
            enteredOtp = null;
            return;
        }

        otpVerified = true;
        verifiedSmsId = lastSms.getId();

        // Only now — after OTP proves control of the phone number — do we reveal
        // which account(s) matched. A single match auto-selects; multiple matches
        // (shared household phone) render a picker (see isMultipleMatch()) for the
        // user to pick their own name, same as the registration flow's post-OTP
        // disambiguation step.
        if (matchedAccounts != null && matchedAccounts.size() == 1) {
            selectedAccount = matchedAccounts.get(0);
        }
    }

    public void resetPassword() {
        clearMessages();
        if (!otpVerified || selectedAccount == null) {
            JsfUtil.addErrorMessage("Error in Development.");
            return;
        }
        if (newPassword == null || newPassword.length() < 6) {
            JsfUtil.addErrorMessage("Password must be at least 6 characters.");
            return;
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            JsfUtil.addErrorMessage("Password and confirmation do not match.");
            return;
        }

        // Re-bind the OTP proof at the moment of the credential change, rather than
        // trusting the view-scoped otpVerified flag indefinitely: re-fetch the latest
        // Sms for this identifier and confirm it's still the exact row that was
        // verified (not a newer OTP requested since, which would leave a stale
        // verification usable against a fresh code) and still within the expiry
        // window (in case the view has been open longer than the OTP timeout).
        String trimmed = identifier == null ? null : identifier.trim();
        String jpql = "select s from Sms s where s.retired=false and s.receipientNumber=:mobile and s.smsType=:type order by s.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("mobile", trimmed);
        params.put("type", MessageType.ClientPortalPasswordResetOTP);
        Sms latestSms = smsFacade.findFirstByJpql(jpql, params);
        boolean proofStillValid = latestSms != null
                && latestSms.getId().equals(verifiedSmsId)
                && (System.currentTimeMillis() - latestSms.getCreatedAt().getTime()) <= getOtpTimeoutMinutes() * 60_000L;
        if (!proofStillValid) {
            otpVerified = false;
            JsfUtil.addErrorMessage("Your verification has expired or a newer code was requested. Please verify your OTP again.");
            return;
        }

        String passwordHash = securityController.hashAndCheck(newPassword);
        if (passwordHash == null) {
            JsfUtil.addErrorMessage("Unable to reset the password. Please try again.");
            return;
        }
        selectedAccount.setPasswordHash(passwordHash);
        clientAccountFacade.edit(selectedAccount);
        resetComplete = true;
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

    public boolean isMultipleMatch() {
        return otpVerified && matchedAccounts != null && matchedAccounts.size() > 1 && selectedAccount == null;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<ClientAccount> getMatchedAccounts() {
        if (matchedAccounts == null) {
            return new ArrayList<>();
        }
        return matchedAccounts;
    }

    public void setMatchedAccounts(List<ClientAccount> matchedAccounts) {
        this.matchedAccounts = matchedAccounts;
    }

    public ClientAccount getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(ClientAccount selectedAccount) {
        this.selectedAccount = selectedAccount;
    }

    public boolean isNoAccountFound() {
        return noAccountFound;
    }

    public void setNoAccountFound(boolean noAccountFound) {
        this.noAccountFound = noAccountFound;
    }

    public boolean isEmailResetUnsupported() {
        return emailResetUnsupported;
    }

    public void setEmailResetUnsupported(boolean emailResetUnsupported) {
        this.emailResetUnsupported = emailResetUnsupported;
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

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirm() {
        return newPasswordConfirm;
    }

    public void setNewPasswordConfirm(String newPasswordConfirm) {
        this.newPasswordConfirm = newPasswordConfirm;
    }

    public boolean isResetComplete() {
        return resetComplete;
    }

    public void setResetComplete(boolean resetComplete) {
        this.resetComplete = resetComplete;
    }
}

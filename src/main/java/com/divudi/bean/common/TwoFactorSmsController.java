package com.divudi.bean.common;

import com.divudi.core.data.OtpPurpose;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.otp.OtpCodeUtil;
import com.divudi.service.otp.OtpIssueResult;
import com.divudi.service.otp.OtpService;
import com.divudi.service.otp.OtpVerifyResult;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * SMS code as the other two-factor login option, next to the authenticator
 * app.
 *
 * At login (after the password is accepted, see
 * {@link TwoFactorAuthController#startChallenge(WebUser)}) the user can ask
 * for a code by SMS; a correct code completes the login through
 * {@link TwoFactorAuthController#completeLoginAfterSecondFactor(WebUser, String)}.
 * Wrong codes count on the same per-user counter as wrong authenticator codes,
 * so 5 wrong codes of either kind lock the account.
 *
 * SMS codes are used only after an administrator allows them for the user
 * (Manage User). The first SMS login sets the mobile up on the login screen
 * itself: the user types the mobile saved in the system (staff record). A
 * number that does not match sends nothing and counts as a failed attempt
 * (5 lock the account); a match sends the code to the saved number, and a
 * correct code both logs in and marks that number verified. Later logins
 * send codes to that number.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class TwoFactorSmsController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private OtpService otpService;
    @EJB
    private WebUserFacade webUserFacade;

    @Inject
    private TwoFactorAuthController twoFactorAuthController;
    @Inject
    private SessionController sessionController;
    @Inject
    private SecurityController securityController;

    // Login challenge
    private boolean appLinked;
    private boolean smsUsable;
    // First SMS login: allowed but no verified mobile yet. The user types the
    // mobile saved in the system (staff record); only on a match is the code
    // sent, to that saved number. A correct code logs in and verifies it.
    private boolean mobileSetup;
    // The entered number matched the mobile saved in the system (staff record)
    private boolean mobileMatched;
    private String loginMobileInput;
    private boolean smsMode;
    private boolean loginCodeSent;
    // The last login code expired before it was used: the screen shows a "Code expired" banner
    private boolean loginCodeExpired;
    private String maskedMobile;
    private String code;
    private long codeExpiresAtMs;
    private long resendAvailableAtMs;

    // Mobile verification from My Authenticator App (change number)
    private boolean mobileVerificationInProgress;
    private boolean mobileCodeSent;
    // Masked number the last verification code went to (shown in the "Code sent" banner)
    private String mobileSentToMasked;
    private String mobileInput;
    private String mobileCode;
    private long mobileCodeExpiresAtMs;
    private long mobileResendAvailableAtMs;

    // <editor-fold defaultstate="collapsed" desc="Login challenge">
    /**
     * Called by TwoFactorAuthController when the code screen opens. A user
     * without a linked app goes straight to SMS mode: with a verified mobile
     * a code is sent at once; without one the mobile number field is shown.
     */
    public void startChallenge(boolean appLinked, boolean smsUsable, boolean mobileSetup) {
        clear();
        cancelMobileVerification();
        this.appLinked = appLinked;
        this.smsUsable = smsUsable;
        this.mobileSetup = mobileSetup;
        WebUser u = pendingUser();
        maskedMobile = u == null ? "" : OtpCodeUtil.maskMobile(u.getTwoFactorMobile());
        if (mobileSetup) {
            // The staff record number is filled in; the user can change it
            // The field starts empty: the user must type the number saved in the system
            loginMobileInput = null;
            smsMode = !appLinked;
        } else if (smsUsable && !appLinked) {
            smsMode = true;
            sendLoginCode();
        }
    }

    public void clear() {
        appLinked = false;
        smsUsable = false;
        mobileSetup = false;
        mobileMatched = false;
        loginMobileInput = null;
        smsMode = false;
        loginCodeSent = false;
        loginCodeExpired = false;
        maskedMobile = null;
        code = null;
        codeExpiresAtMs = 0;
        resendAvailableAtMs = 0;
    }

    /**
     * "Send code by SMS" under the authenticator code: sends at once to a
     * verified mobile, or opens the mobile number field for a first SMS login.
     */
    public void chooseSms() {
        if (mobileSetup) {
            smsMode = true;
            code = null;
        } else {
            sendLoginCode();
        }
    }

    /**
     * Sends (or re-sends) the login code: to the verified mobile, or, on a
     * first SMS login, to the mobile saved in the system once the user has
     * typed a matching number.
     */
    public void sendLoginCode() {
        WebUser u = twoFactorAuthController.findPendingUser();
        if (u == null) {
            return;
        }
        String sendTo;
        if (twoFactorAuthController.isSmsUsableFor(u)) {
            sendTo = u.getTwoFactorMobile();
        } else if (mobileSetup && twoFactorAuthController.isSmsAllowedFor(u)) {
            // First SMS login: the code only ever goes to the mobile saved in the system
            String systemMobile = otpService.normalizeMobile(staffMobileOf(u));
            if (systemMobile == null) {
                JsfUtil.addErrorMessage("No valid mobile number is saved in the system for your account. Contact your administrator.");
                return;
            }
            if (!mobileMatched) {
                if (loginMobileInput == null || loginMobileInput.trim().isEmpty()) {
                    JsfUtil.addErrorMessage("Enter your mobile number.");
                    return;
                }
                String entered = otpService.normalizeMobile(loginMobileInput);
                loginMobileInput = null;
                if (entered == null || !entered.equals(systemMobile)) {
                    // No SMS; counts on the same lock counter as wrong codes (5 locks the account)
                    twoFactorAuthController.recordFailedSecondFactor(u, TwoFactorAuthController.EVENT_MOBILE_VERIFICATION_FAILED,
                            "Your mobile number does not match the mobile number saved in the system.");
                    return;
                }
                mobileMatched = true;
            }
            sendTo = systemMobile;
        } else {
            JsfUtil.addErrorMessage("SMS codes are not available for your account.");
            return;
        }
        smsMode = true;
        code = null;
        maskedMobile = OtpCodeUtil.maskMobile(sendTo);
        OtpIssueResult r = otpService.issueSmsCode(u, OtpPurpose.STAFF_LOGIN_2FA, sendTo,
                signingKey(), requestIp(), userAgent());
        handleIssueResult(u, r, true, sendTo);
    }

    public String verifyLoginCode() {
        WebUser u = twoFactorAuthController.findPendingUser();
        if (u == null) {
            return "";
        }
        boolean verified = twoFactorAuthController.isSmsUsableFor(u);
        if (!verified && !(mobileSetup && twoFactorAuthController.isSmsAllowedFor(u))) {
            code = null;
            JsfUtil.addErrorMessage("SMS codes are not available for your account.");
            return "";
        }
        if (OtpCodeUtil.cleanCode(code) == null) {
            code = null;
            JsfUtil.addErrorMessage("Enter the 6-digit code from the SMS.");
            return "";
        }
        OtpVerifyResult r = otpService.verifyCode(u, OtpPurpose.STAFF_LOGIN_2FA, code, signingKey());
        code = null;
        switch (r.getStatus()) {
            case VERIFIED:
                if (!verified) {
                    // The code reached the saved number: it is now the verified mobile
                    u.setTwoFactorMobile(r.getIdentifier());
                    u.setTwoFactorMobileVerifiedAt(new Date());
                    Map<String, Object> details = new HashMap<>();
                    details.put("mobile", OtpCodeUtil.maskMobile(r.getIdentifier()));
                    details.put("setAtLogin", true);
                    twoFactorAuthController.logSmsEvent(u, u, TwoFactorAuthController.EVENT_MOBILE_VERIFIED, details, null);
                }
                // Same path as a valid authenticator code: the user gets access
                return twoFactorAuthController.completeLoginAfterSecondFactor(u, TwoFactorAuthController.EVENT_SMS_LOGIN_SUCCESS);
            case INVALID:
                twoFactorAuthController.recordFailedSecondFactor(u, TwoFactorAuthController.EVENT_SMS_LOGIN_FAILED, "Invalid SMS code.");
                return "";
            case EXPIRED:
                loginCodeSent = false;
                // No pop-up message: the screen shows a "Code expired" banner
                loginCodeExpired = true;
                return "";
            case TOO_MANY_ATTEMPTS:
                loginCodeSent = false;
                JsfUtil.addErrorMessage("Too many wrong entries for this code. Send a new code.");
                return "";
            default:
                loginCodeSent = false;
                JsfUtil.addErrorMessage("No active code. Send a new code.");
                return "";
        }
    }

    public void useAuthenticator() {
        if (appLinked) {
            smsMode = false;
            code = null;
        }
    }

    /**
     * The SMS option on the authenticator screen: shown whenever SMS codes
     * are allowed for the user, verified or not.
     */
    public boolean isSmsOptionAvailable() {
        return twoFactorAuthController.isPending() && (smsUsable || mobileSetup);
    }

    /**
     * The authenticator option on the SMS screen: only for a user whose app
     * is already linked. A user without a linked app sees SMS only; the app
     * can be set up later from My Authenticator App.
     */
    public boolean isAuthenticatorOptionAvailable() {
        return twoFactorAuthController.isPending() && appLinked;
    }

    /**
     * The mobile number field is shown (first SMS login, before the code is sent).
     */
    public boolean isMobileEntryShown() {
        return mobileSetup && !loginCodeSent;
    }

    /**
     * True when the mobile saved in the user's staff record is a valid number
     * from an allowed country, so a first SMS login can send a code to it.
     */
    public boolean hasUsableStaffMobile(WebUser u) {
        return otpService.normalizeMobile(staffMobileOf(u)) != null;
    }

    private static String staffMobileOf(WebUser u) {
        if (u == null || u.getWebUserPerson() == null || u.getWebUserPerson().getMobile() == null) {
            return null;
        }
        String m = u.getWebUserPerson().getMobile().trim();
        return m.isEmpty() ? null : m;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Mobile verification (My Authenticator App)">
    public boolean isLoggedUserSmsAllowed() {
        return twoFactorAuthController.isSmsAllowedFor(sessionController.getLoggedUser());
    }

    public boolean isLoggedUserMobileVerified() {
        WebUser u = sessionController.getLoggedUser();
        return u != null && u.isTwoFactorMobileVerified();
    }

    public String getLoggedUserMaskedMobile() {
        WebUser u = sessionController.getLoggedUser();
        return u == null ? "" : OtpCodeUtil.maskMobile(u.getTwoFactorMobile());
    }

    public Date getLoggedUserMobileVerifiedAt() {
        WebUser u = sessionController.getLoggedUser();
        return u == null ? null : u.getTwoFactorMobileVerifiedAt();
    }

    public void startMobileVerification() {
        WebUser u = loggedUserFromDb();
        if (u == null || !twoFactorAuthController.isSmsAllowedFor(u)) {
            JsfUtil.addErrorMessage("SMS codes are not enabled for your account. Contact your administrator.");
            return;
        }
        if (u.isTwoFactorMobileVerified()) {
            // Changing a verified number is done by an administrator, not by the user
            JsfUtil.addErrorMessage("Your mobile number is already verified. If it has changed, inform your system administrator.");
            return;
        }
        mobileVerificationInProgress = true;
        mobileCodeSent = false;
        mobileCode = null;
        if (u.getTwoFactorMobile() != null && !u.getTwoFactorMobile().trim().isEmpty()) {
            mobileInput = u.getTwoFactorMobile();
        } else if (u.getWebUserPerson() != null) {
            mobileInput = u.getWebUserPerson().getMobile();
        } else {
            mobileInput = null;
        }
    }

    public void cancelMobileVerification() {
        mobileVerificationInProgress = false;
        mobileCodeSent = false;
        mobileSentToMasked = null;
        mobileCode = null;
        mobileInput = null;
        mobileCodeExpiresAtMs = 0;
        mobileResendAvailableAtMs = 0;
    }

    public void sendMobileVerificationCode() {
        WebUser u = loggedUserFromDb();
        if (u == null || !twoFactorAuthController.isSmsAllowedFor(u)) {
            JsfUtil.addErrorMessage("SMS codes are not enabled for your account. Contact your administrator.");
            return;
        }
        if (u.isTwoFactorMobileVerified()) {
            // Another session may have verified a number since this one started; only an administrator changes it
            cancelMobileVerification();
            JsfUtil.addErrorMessage("Your mobile number is already verified. If it has changed, inform your system administrator.");
            return;
        }
        if (mobileInput == null || mobileInput.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter your mobile number.");
            return;
        }
        if (!otpService.isMobileAcceptable(mobileInput)) {
            JsfUtil.addErrorMessage("Enter a valid mobile number (e.g. 0771234567).");
            return;
        }
        mobileCode = null;
        OtpIssueResult r = otpService.issueSmsCode(u, OtpPurpose.STAFF_MOBILE_VERIFICATION, mobileInput.trim(),
                signingKey(), requestIp(), userAgent());
        handleIssueResult(u, r, false, mobileInput);
    }

    public void confirmMobileVerification() {
        WebUser u = loggedUserFromDb();
        if (u == null || !twoFactorAuthController.isSmsAllowedFor(u)) {
            JsfUtil.addErrorMessage("SMS codes are not enabled for your account. Contact your administrator.");
            return;
        }
        if (u.isTwoFactorMobileVerified()) {
            // Another session may have verified a number since this one started; only an administrator changes it
            cancelMobileVerification();
            JsfUtil.addErrorMessage("Your mobile number is already verified. If it has changed, inform your system administrator.");
            return;
        }
        if (OtpCodeUtil.cleanCode(mobileCode) == null) {
            mobileCode = null;
            JsfUtil.addErrorMessage("Enter the 6-digit code from the SMS.");
            return;
        }
        OtpVerifyResult r = otpService.verifyCode(u, OtpPurpose.STAFF_MOBILE_VERIFICATION, mobileCode, signingKey());
        mobileCode = null;
        if (!r.isVerified()) {
            Map<String, Object> details = new HashMap<>();
            details.put("result", r.getStatus().name());
            twoFactorAuthController.logSmsEvent(u, u, TwoFactorAuthController.EVENT_MOBILE_VERIFICATION_FAILED, details, "Failed");
            switch (r.getStatus()) {
                case INVALID:
                    JsfUtil.addErrorMessage("Invalid code. Check the SMS and try again.");
                    break;
                case EXPIRED:
                    mobileCodeSent = false;
                    JsfUtil.addErrorMessage("The code has expired. Send a new code.");
                    break;
                default:
                    mobileCodeSent = false;
                    JsfUtil.addErrorMessage("Too many wrong entries or no active code. Send a new code.");
            }
            return;
        }
        boolean wasForcedSetup = twoFactorAuthController.isSetupRequiredNow();
        u.setTwoFactorMobile(r.getIdentifier());
        u.setTwoFactorMobileVerifiedAt(new Date());
        webUserFacade.edit(u);
        // Keep the session copy in step so a later edit(loggedUser) does not drop the verified number
        TwoFactorAuthController.copyEnrollment(u, sessionController.getLoggedUser());
        Map<String, Object> details = new HashMap<>();
        details.put("mobile", OtpCodeUtil.maskMobile(r.getIdentifier()));
        twoFactorAuthController.logSmsEvent(u, u, TwoFactorAuthController.EVENT_MOBILE_VERIFIED, details, null);
        cancelMobileVerification();
        if (wasForcedSetup) {
            // Same as a forced app setup: sign in again, this time with an SMS code
            twoFactorAuthController.markReloginRequired();
            return;
        }
        JsfUtil.addSuccessMessage("Mobile number verified. You can now use SMS codes to log in.");
    }
    // </editor-fold>

    private void handleIssueResult(WebUser u, OtpIssueResult r, boolean login, String sentTo) {
        Map<String, Object> details = new HashMap<>();
        details.put("purpose", login ?OtpPurpose.STAFF_LOGIN_2FA.name() : OtpPurpose.STAFF_MOBILE_VERIFICATION.name());
        details.put("mobile", OtpCodeUtil.maskMobile(sentTo));
        details.put("ip", requestIp());
        long nowMs = System.currentTimeMillis();
        switch (r.getStatus()) {
            case SENT:
                if (login) {
                    loginCodeSent = true;
                    loginCodeExpired = false;
                    codeExpiresAtMs = r.getExpiresAt().getTime();
                    resendAvailableAtMs = nowMs + r.getRetryAfterSeconds() * 1000L;
                } else {
                    mobileCodeSent = true;
                    mobileSentToMasked = OtpCodeUtil.maskMobile(sentTo);
                    mobileCodeExpiresAtMs = r.getExpiresAt().getTime();
                    mobileResendAvailableAtMs = nowMs + r.getRetryAfterSeconds() * 1000L;
                }
                twoFactorAuthController.logSmsEvent(u, u, TwoFactorAuthController.EVENT_SMS_CODE_SENT, details, null);
                // No pop-up message: the screen shows a "Code sent" banner while the code is active
                break;
            case RATE_LIMITED:
                if (login) {
                    resendAvailableAtMs = nowMs + r.getRetryAfterSeconds() * 1000L;
                } else {
                    mobileResendAvailableAtMs = nowMs + r.getRetryAfterSeconds() * 1000L;
                }
                details.put("retryAfterSeconds", r.getRetryAfterSeconds());
                twoFactorAuthController.logSmsEvent(u, u, TwoFactorAuthController.EVENT_SMS_RATE_LIMITED, details, "Failed");
                JsfUtil.addErrorMessage("Too many code requests. Try again in " + formatWait(r.getRetryAfterSeconds()) + ".");
                break;
            case INVALID_MOBILE:
                JsfUtil.addErrorMessage("The mobile number is not valid or not allowed for SMS codes.");
                break;
            default:
                twoFactorAuthController.logSmsEvent(u, u, TwoFactorAuthController.EVENT_SMS_CODE_SEND_FAILED, details, "Failed");
                JsfUtil.addErrorMessage("The SMS could not be sent. Try again later"
                        + (login && appLinked ? " or use your authenticator app." : "."));
        }
    }

    /**
     * Masked mobile number for display (+9477****567).
     */
    public String mask(String mobile) {
        return OtpCodeUtil.maskMobile(mobile);
    }

    private static String formatWait(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        }
        long minutes = (seconds + 59) / 60;
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }

    private WebUser pendingUser() {
        Long id = twoFactorAuthController.getPendingUserId();
        return id == null ? null : webUserFacade.find(id);
    }

    private WebUser loggedUserFromDb() {
        WebUser lu = sessionController.getLoggedUser();
        if (lu == null || lu.getId() == null) {
            return null;
        }
        return webUserFacade.find(lu.getId());
    }

    private String signingKey() {
        return securityController.obtainHmacSigningKey(sessionController);
    }

    private static HttpServletRequest request() {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc == null ? null : (HttpServletRequest) fc.getExternalContext().getRequest();
    }

    private static String requestIp() {
        HttpServletRequest req = request();
        if (req == null) {
            return null;
        }
        String forwarded = req.getHeader("X-FORWARDED-FOR");
        return forwarded == null ? req.getRemoteAddr() : forwarded;
    }

    private static String userAgent() {
        HttpServletRequest req = request();
        return req == null ? null : req.getHeader("User-Agent");
    }

    // <editor-fold defaultstate="collapsed" desc="Getters and setters">
    public boolean isSmsMode() {
        return smsMode;
    }

    public boolean isLoginCodeExpired() {
        return loginCodeExpired;
    }

    public boolean isLoginCodeSent() {
        return loginCodeSent;
    }

    public String getMaskedMobile() {
        return maskedMobile;
    }

    public boolean isMobileSetup() {
        return mobileSetup;
    }

    public String getLoginMobileInput() {
        return loginMobileInput;
    }

    public void setLoginMobileInput(String loginMobileInput) {
        this.loginMobileInput = loginMobileInput;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    // Seconds are worked out on the server so a wrong clock on the user's PC does not matter
    public long getCodeSecondsLeft() {
        return secondsUntil(codeExpiresAtMs);
    }

    public long getResendSecondsLeft() {
        return secondsUntil(resendAvailableAtMs);
    }

    public long getMobileCodeSecondsLeft() {
        return secondsUntil(mobileCodeExpiresAtMs);
    }

    public long getMobileResendSecondsLeft() {
        return secondsUntil(mobileResendAvailableAtMs);
    }

    private static long secondsUntil(long epochMs) {
        long ms = epochMs - System.currentTimeMillis();
        return ms <= 0 ? 0 : (ms + 999) / 1000;
    }

    public boolean isMobileVerificationInProgress() {
        return mobileVerificationInProgress;
    }

    public String getMobileSentToMasked() {
        return mobileSentToMasked;
    }

    public boolean isMobileCodeSent() {
        return mobileCodeSent;
    }

    public String getMobileInput() {
        return mobileInput;
    }

    public void setMobileInput(String mobileInput) {
        this.mobileInput = mobileInput;
    }

    public String getMobileCode() {
        return mobileCode;
    }

    public void setMobileCode(String mobileCode) {
        this.mobileCode = mobileCode;
    }

    // </editor-fold>
}

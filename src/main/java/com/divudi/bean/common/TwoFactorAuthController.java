package com.divudi.bean.common;

import com.divudi.core.data.Privileges;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.TotpUtil;
import com.divudi.core.util.TwoFactorSecretCipher;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Authenticator app (TOTP) two-factor login.
 *
 * Config options: "Enable authenticator app two-factor login" (master switch),
 * "Require authenticator app two-factor login for all users" (true = everyone,
 * false = only users with WebUser.twoFactorRequired) and "Authenticator app
 * issuer name" (label shown in the app).
 *
 * After SessionController has verified the password it calls
 * {@link #startChallenge(WebUser)}; the login composites then show the code
 * screen and {@link #verifyLoginCode()} completes the login through
 * {@link SessionController#completeTwoFactorLogin(WebUser, boolean)}.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class TwoFactorAuthController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int QR_SIZE_PX = 200;
    public static final String LOCKED_MESSAGE = "Your account is locked after " + MAX_FAILED_ATTEMPTS
            + " invalid verification codes. Please contact an administrator to unlock it.";
    public static final String NOT_SET_UP_MESSAGE = "Two-factor login is not set up on this server. Ask the administrator.";
    public static final String NO_STAFF_MOBILE_MESSAGE = "Your staff record has no valid mobile number, so two-factor set-up cannot be done. "
            + "Ask an administrator to add your mobile number to your staff record.";
    public static final String SETUP_WINDOW_EXPIRED_MESSAGE = "The 24-hour period to set up two-factor login has ended. "
            + "Ask an administrator to reset your authenticator, then set it up again.";
    // A user who must set up two-factor login has this long after an admin requires it or resets it
    public static final long SETUP_WINDOW_MS = 24L * 60 * 60 * 1000;
    // A password-accepted login that waits for a code is dropped after this long
    private static final long PENDING_LOGIN_TTL_MS = 10L * 60 * 1000;

    // Audit events: entityType is AUDIT_ENTITY_TYPE, objectId is the affected user, webUserId is who did it
    public static final String AUDIT_ENTITY_TYPE = "TwoFactorAuth";
    public static final String EVENT_REQUIRED_ON = "2FA Required Turned On";
    public static final String EVENT_REQUIRED_OFF = "2FA Required Turned Off";
    public static final String EVENT_SELF_SETUP = "2FA Self Setup";
    public static final String EVENT_FIRST_LOGIN_SETUP = "2FA First Login Setup";
    public static final String EVENT_ACCOUNT_LOCKED = "2FA Account Locked";
    public static final String EVENT_ACCOUNT_UNLOCKED = "2FA Account Unlocked";
    public static final String EVENT_AUTHENTICATOR_RESET = "2FA Authenticator Reset";
    public static final String EVENT_LOGIN_SUCCESS = "2FA Login Success";
    public static final String EVENT_LOGIN_FAILED = "2FA Login Failed";
    public static final String EVENT_SETUP_FAILED = "2FA Setup Failed";
    public static final String EVENT_SETUP_QR_RENEWED = "2FA Setup QR Renewed";
    public static final String EVENT_SETUP_CANCELLED_FAILED_ATTEMPTS = "2FA Setup Cancelled After Failed Attempts";
    public static final String EVENT_SMS_CODE_SENT = "2FA SMS Code Sent";
    public static final String EVENT_SMS_CODE_SEND_FAILED = "2FA SMS Code Send Failed";
    public static final String EVENT_SMS_RATE_LIMITED = "2FA SMS Code Rate Limited";
    public static final String EVENT_SMS_LOGIN_SUCCESS = "2FA SMS Login Success";
    public static final String EVENT_SMS_LOGIN_FAILED = "2FA SMS Login Failed";
    public static final String EVENT_MOBILE_VERIFIED = "2FA Mobile Verified";
    public static final String EVENT_MOBILE_VERIFICATION_FAILED = "2FA Mobile Verification Failed";
    public static final String EVENT_MOBILE_RESET = "2FA Mobile Reset";
    public static final String EVENT_SMS_ALLOWED_ON = "2FA SMS Code Allowed";
    public static final String EVENT_SMS_ALLOWED_OFF = "2FA SMS Code Not Allowed";
    public static final String EVENT_SETUP_PROOF_VERIFIED = "2FA Setup SMS Check Passed";
    public static final String EVENT_SETUP_PROOF_FAILED = "2FA Setup SMS Check Failed";
    public static final String EVENT_LOGIN_REFUSED_NO_KEY = "2FA Login Refused (server key)";
    public static final String EVENT_LOGIN_TIMED_OUT = "2FA Login Timed Out";
    public static final String EVENT_SETUP_WINDOW_OPENED = "2FA Setup Window Opened";
    private static final int AUTH_LOG_LIMIT = 15;

    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private AuditEventFacade auditEventFacade;
    @EJB
    private AuditService auditService;

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private TwoFactorSmsController twoFactorSmsController;
    @Inject
    private WebUserController webUserController;

    // Login challenge state (password already verified, code pending)
    private Long pendingUserId;
    private String pendingUserName;
    private long pendingStartedAt;
    // Set when a waiting login was dropped for taking too long; the login card shows a notice
    private boolean loginTimedOutNotice;
    private boolean channelingTabView;
    // Shown on the login card when the account is locked (a growl disappears too quickly)
    private boolean accountLockedNotice;
    private String lockedUserName;
    private String code;

    // Self-service setup / reset from the user's menu
    private String selfSetupSecret;
    private String selfSetupCode;
    // Invalid codes against the current setup QR; the QR is replaced when it reaches MAX_FAILED_ATTEMPTS
    private int selfSetupFailedAttempts;
    // Set when a required setup is confirmed; the user must log out and sign in again with a code
    private boolean reloginRequired;
    // This session's login passed a second step (app or SMS code)
    private boolean secondFactorVerified;
    // A code sent to the staff-record mobile was entered in this session (see TwoFactorSmsController)
    private boolean setupProofed;

    // Authentication log popup on Manage User
    private List<AuditEvent> authLogs;

    public boolean isTwoFactorEnabled() {
        return configOptionApplicationController.isAuthenticatorAppTwoFactorLoginEnabled();
    }

    public boolean isTwoFactorRequiredFor(WebUser u) {
        if (u == null || !isTwoFactorEnabled()) {
            return false;
        }
        if (u.isTwoFactorRequired()) {
            return true;
        }
        // "All users" cannot apply to a user who has no linked app and no valid staff-record mobile: such a user
        // could never set up (the first proof is an SMS to that mobile). They are listed when the switch is saved
        // (see namesOfUsersWithoutStaffMobile) and are not held out of the system.
        return configOptionApplicationController.isAuthenticatorAppTwoFactorLoginRequiredForAllUsers()
                && (isAppLinked(u) || twoFactorSmsController.hasUsableStaffMobile(u));
    }

    /**
     * Names of active users the "all users" switch cannot cover, because
     * neither an app is linked nor a valid mobile number is on the staff
     * record. Shown as a warning when the switch is saved.
     */
    public List<String> namesOfUsersWithoutStaffMobile() {
        List<String> names = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        List<WebUser> all = webUserFacade.findByJpql("select u from WebUser u where u.retired=:ret and u.activated=true order by u.name", m);
        if (all == null) {
            return names;
        }
        for (WebUser u : all) {
            if (!u.isTwoFactorRequired() && !isAppLinked(u) && !twoFactorSmsController.hasUsableStaffMobile(u)) {
                names.add(u.getName());
            }
        }
        return names;
    }

    /**
     * Called after the "all users" switch is saved as on: warns, naming up to
     * 25 users that it cannot cover for lack of a staff-record mobile.
     */
    public void warnUsersWithoutStaffMobile() {
        List<String> names = namesOfUsersWithoutStaffMobile();
        if (names.isEmpty()) {
            return;
        }
        String shown = String.join(", ", names.size() > 25 ? names.subList(0, 25) : names);
        JsfUtil.addWarningMessage(names.size() + " user(s) have no valid mobile number on the staff record, so this switch does not cover them yet: "
                + shown + (names.size() > 25 ? ", ..." : "") + ". Add the mobile number to their staff record.");
    }

    /**
     * Whether the staff record of the user holds a valid mobile number.
     */
    public boolean hasUsableStaffMobile(WebUser u) {
        return twoFactorSmsController.hasUsableStaffMobile(u);
    }

    // <editor-fold defaultstate="collapsed" desc="Set-up window">
    /**
     * When the 24 h set-up window of the user began: the latest of the time an
     * admin required 2FA for the user or reset it, and the time a 2FA switch
     * was turned on. Null when no time is recorded.
     */
    public Date setupWindowStartOf(WebUser u) {
        Date a = u == null ? null : u.getTwoFactorSetupWindowStartedAt();
        Date b = configOptionApplicationController.getTwoFactorSwitchesOnAt();
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.after(b) ? a : b;
    }

    /**
     * When the set-up window of the user ends; null when it never began.
     */
    public Date setupWindowEndOf(WebUser u) {
        Date start = setupWindowStartOf(u);
        return start == null ? null : new Date(start.getTime() + SETUP_WINDOW_MS);
    }

    /**
     * Whether the user may still set up: a user for whom 2FA is required has
     * the window; a user setting up voluntarily has no limit. No recorded
     * start counts as ended, so an admin reset is needed.
     */
    public boolean isSetupWindowOpenFor(WebUser u) {
        if (!isTwoFactorRequiredFor(u)) {
            return true;
        }
        Date end = setupWindowEndOf(u);
        return end != null && new Date().before(end);
    }

    /**
     * Starts a new set-up window for a saved user with one UPDATE and
     * records it in the audit log.
     */
    public void openSetupWindowInDb(WebUser subject) {
        if (subject == null || subject.getId() == null) {
            return;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("id", subject.getId());
        p.put("t", new Date());
        webUserFacade.updateByJpql("update WebUser u set u.twoFactorSetupWindowStartedAt=:t where u.id=:id", p);
        logTwoFactorEvent(sessionController.getLoggedUser(), subject, EVENT_SETUP_WINDOW_OPENED);
        copyEnrollment(freshUser(subject.getId()), subject);
    }

    /**
     * Writes the two flags an admin sets on Manage User (Required, Allow
     * SMS) with one UPDATE, because they are insert-only on the entity.
     */
    public void saveAdminFlags(WebUser subject) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", subject.getId());
        p.put("r", subject.isTwoFactorRequired());
        p.put("s", subject.isTwoFactorSmsAllowed());
        webUserFacade.updateByJpql("update WebUser u set u.twoFactorRequired=:r, u.twoFactorSmsAllowed=:s where u.id=:id", p);
    }

    /**
     * Why the logged user cannot set up an authenticator right now, or null
     * when set-up can go ahead. Read from the database so an admin reset
     * made after login is seen.
     */
    public String getSetupBlockedMessage() {
        if (!TwoFactorSecretCipher.isKeyConfigured()) {
            return NOT_SET_UP_MESSAGE;
        }
        WebUser u = loggedUserFromDb();
        if (u == null) {
            return null;
        }
        if (u.isTwoFactorLocked()) {
            return LOCKED_MESSAGE;
        }
        if (!isSetupWindowOpenFor(u)) {
            return SETUP_WINDOW_EXPIRED_MESSAGE;
        }
        if (!isSetupProofSatisfied() && !twoFactorSmsController.hasUsableStaffMobile(u)) {
            return NO_STAFF_MOBILE_MESSAGE;
        }
        return null;
    }

    public Date getLoggedUserSetupWindowEnd() {
        return setupWindowEndOf(loggedUserFromDb());
    }
    // </editor-fold>

    /**
     * True when a password-accepted login must be refused because the
     * second step cannot work on this server: no key is set, and the user
     * either has an app saved under that key or must set one up. Nobody is
     * let through without the second step, and nothing is counted as a
     * failed code.
     */
    public boolean isLoginRefusedForMissingKey(WebUser u) {
        if (u == null || !isTwoFactorEnabled() || TwoFactorSecretCipher.isKeyConfigured() || isSmsUsableFor(u)) {
            return false;
        }
        if (isAppLinked(u)) {
            return TwoFactorSecretCipher.isAnyGcmFormat(u.getTwoFactorSecret());
        }
        return isTwoFactorRequiredFor(u);
    }

    /**
     * Records the refusal in the audit log and shows the message.
     */
    public void refuseLoginForMissingKey(WebUser u) {
        logTwoFactorEvent(u, u, EVENT_LOGIN_REFUSED_NO_KEY, null, null, "Refused");
        JsfUtil.addErrorMessage(NOT_SET_UP_MESSAGE);
    }

    /**
     * Whether the login must stop for a second-step code: every user with a
     * linked app, and every user allowed SMS codes (one without a verified
     * mobile sets it up there, see TwoFactorSmsController). A required user
     * with neither logs in and is held on the setup screen instead (see
     * {@link #isSetupRequiredNow()}).
     */
    public boolean isCodeNeededAtLoginFor(WebUser u) {
        if (u == null || !isTwoFactorEnabled()) {
            return false;
        }
        return isAppLinked(u) || isSmsUsableFor(u) || isFirstSmsLoginPossibleFor(u);
    }

    /**
     * Allowed SMS codes but no verified mobile yet: the first SMS login can
     * only send a code when the staff record holds a valid mobile. Without one
     * the user is not stopped at an SMS screen that can never send a code.
     */
    private boolean isFirstSmsLoginPossibleFor(WebUser u) {
        return isSmsAllowedFor(u) && !u.isTwoFactorMobileVerified() && twoFactorSmsController.hasUsableStaffMobile(u);
    }

    private static boolean isAppLinked(WebUser u) {
        return u != null && u.isTwoFactorEnrolled() && u.getTwoFactorSecret() != null;
    }

    /**
     * Whether the user can receive login codes by SMS: the option is on in
     * the configuration, an administrator allowed it for the user, and the
     * user has verified the mobile number.
     */
    public boolean isSmsUsableFor(WebUser u) {
        return isSmsAllowedFor(u) && u.isTwoFactorMobileVerified();
    }

    /**
     * Whether the user may set up SMS codes (verify a mobile number).
     */
    public boolean isSmsAllowedFor(WebUser u) {
        return u != null
                && isTwoFactorEnabled()
                && configOptionApplicationController.isTwoFactorSmsLoginEnabled()
                && u.isTwoFactorSmsAllowed();
    }

    /**
     * True while the logged user must use 2FA but has not linked an app, and
     * after that required setup until the user logs out; the templates then
     * show only the setup screen, whatever page is opened.
     */
    public boolean isSetupRequiredNow() {
        if (reloginRequired) {
            return true;
        }
        WebUser u = sessionController.getLoggedUser();
        return u != null && !u.isTwoFactorEnrolled() && !isSmsUsableFor(u) && isTwoFactorRequiredFor(u);
    }

    public boolean isReloginRequired() {
        return reloginRequired;
    }

    /**
     * Called at the start of every password login and at logout: drops a
     * half-finished code challenge, an unfinished self setup and the
     * "sign in again" mark, so none of it carries into the next login on the
     * same browser session.
     */
    public void resetForNewLogin() {
        clearChallenge();
        loginTimedOutNotice = false;
        reloginRequired = false;
        secondFactorVerified = false;
        setupProofed = false;
        twoFactorSmsController.cancelSetupProof();
        twoFactorSmsController.cancelMobileVerification();
        selfSetupSecret = null;
        selfSetupCode = null;
        selfSetupFailedAttempts = 0;
    }

    /**
     * Called when a required setup is completed by verifying a mobile number
     * for SMS codes; the user logs out and signs in again with a code.
     */
    public void markReloginRequired() {
        reloginRequired = true;
    }

    /**
     * Opens the code screen for a user whose password is accepted. No QR
     * code is ever shown here: a password alone must not be enough to link
     * an app. A user without a linked app logs in by SMS (to the staff-record
     * mobile) and links the app later from My Authenticator App.
     */
    public void startChallenge(WebUser u) {
        pendingUserId = u.getId();
        pendingUserName = u.getName();
        pendingStartedAt = System.currentTimeMillis();
        code = null;
        boolean appLinked = isAppLinked(u);
        boolean smsUsable = isSmsUsableFor(u);
        // Allowed SMS but no verified mobile yet: the mobile is set up on this screen
        boolean mobileSetup = !smsUsable && isFirstSmsLoginPossibleFor(u);
        twoFactorSmsController.startChallenge(appLinked, smsUsable, mobileSetup);
    }

    public String verifyLoginCode() {
        WebUser u = findPendingUser();
        if (u == null) {
            return "";
        }
        if (!isAppLinked(u)) {
            code = null;
            JsfUtil.addErrorMessage("No authenticator app is linked to your account. Use the SMS code instead.");
            return "";
        }
        String secret = decryptSecret(u);
        if (secret == null) {
            // The stored secret cannot be read: this server has no key, or a different one. Not the user's
            // mistake, so no failed attempt is counted and the account is never locked.
            code = null;
            logTwoFactorEvent(u, u, EVENT_LOGIN_REFUSED_NO_KEY, null, null, "Refused");
            JsfUtil.addErrorMessage(NOT_SET_UP_MESSAGE);
            return "";
        }
        long step = TotpUtil.verify(secret, code, u.getTwoFactorLastUsedStep());
        code = null;
        if (step < 0) {
            recordFailedSecondFactor(u, EVENT_LOGIN_FAILED, "Invalid authenticator code.");
            return "";
        }
        // One atomic write claims the time step: a second request with the same code (sent at the same moment)
        // finds the step already taken and is refused
        if (!claimTimeStep(u.getId(), step)) {
            recordFailedSecondFactor(u, EVENT_LOGIN_FAILED, "That code has already been used. Wait for the next code.");
            return "";
        }
        if (!TwoFactorSecretCipher.isCurrentFormat(u.getTwoFactorSecret())) {
            // A gcm1 value (read above) is stored again as gcm2, only if the value is unchanged
            String stored = TwoFactorSecretCipher.encrypt(secret, u.getId());
            if (stored != null) {
                Map<String, Object> p = new HashMap<>();
                p.put("id", u.getId());
                p.put("newSecret", stored);
                p.put("oldSecret", u.getTwoFactorSecret());
                webUserFacade.updateByJpql("update WebUser u set u.twoFactorSecret=:newSecret"
                        + " where u.id=:id and u.twoFactorSecret=:oldSecret", p);
            }
        }
        return completeLoginAfterSecondFactor(u, EVENT_LOGIN_SUCCESS);
    }

    /**
     * Stores the used time step only if it is newer than the one stored, in
     * one UPDATE; false when another request got there first.
     */
    private boolean claimTimeStep(Long userId, long step) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", userId);
        p.put("s", step);
        return webUserFacade.updateByJpql("update WebUser u set u.twoFactorLastUsedStep=:s"
                + " where u.id=:id and (u.twoFactorLastUsedStep is null or u.twoFactorLastUsedStep < :s)", p) == 1;
    }

    /**
     * The user read from the database now, bypassing the cache.
     */
    public WebUser freshUser(Long id) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", id);
        return webUserFacade.findFirstByJpql("select u from WebUser u where u.id=:id", p, true);
    }

    /**
     * The pending login user, freshly read; null (with the login cancelled
     * and a message shown) when there is no pending login or the user is
     * gone or locked.
     */
    public WebUser findPendingUser() {
        if (!isPending()) {
            return null;
        }
        // Read from the database, not the cache: a stale copy must not decide a second-step check
        WebUser u = freshUser(pendingUserId);
        if (u == null || u.isRetired()) {
            cancelLogin();
            JsfUtil.addErrorMessage("Login Failure. Please try again");
            return null;
        }
        if (u.isTwoFactorLocked()) {
            cancelLogin();
            showLockedNotice(u.getName());
            return null;
        }
        return u;
    }

    /**
     * Counts a wrong second-step code (authenticator or SMS). One counter is
     * kept on the user for both methods, so switching method, resending a
     * code or logging in again never gives fresh attempts; reaching the
     * limit locks the account until an administrator unlocks it.
     */
    public void recordFailedSecondFactor(WebUser u, String auditTrigger, String message) {
        boolean locked = countFailedCode(u, auditTrigger);
        if (locked) {
            cancelLogin();
            showLockedNotice(u.getName());
        } else {
            JsfUtil.addErrorMessage(message + " " + (MAX_FAILED_ATTEMPTS - u.getTwoFactorFailedAttempts()) + " attempt(s) left.");
        }
    }

    /**
     * Adds one wrong code to the user's counter with one UPDATE, locks the
     * account at the limit with another, and records both in the audit log.
     * Used for every wrong code (login, SMS set-up proof, mobile check), so
     * all of them share one counter. Parallel requests cannot each read the
     * old count and slip extra guesses past the limit.
     *
     * @return true when the account is locked now
     */
    public boolean countFailedCode(WebUser u, String auditTrigger) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", u.getId());
        webUserFacade.updateByJpql("update WebUser u set u.twoFactorFailedAttempts = u.twoFactorFailedAttempts + 1"
                + " where u.id=:id", p);
        WebUser fresh = freshUser(u.getId());
        int failedAttempts = fresh == null ? MAX_FAILED_ATTEMPTS : fresh.getTwoFactorFailedAttempts();
        u.setTwoFactorFailedAttempts(failedAttempts);
        Map<String, Object> attempt = new HashMap<>();
        attempt.put("failedAttempts", failedAttempts);
        // The user trying is recorded as the actor
        logTwoFactorEvent(u, u, auditTrigger, null, attempt, "Failed");
        if (failedAttempts < MAX_FAILED_ATTEMPTS) {
            return false;
        }
        Map<String, Object> lock = new HashMap<>();
        lock.put("id", u.getId());
        lock.put("max", MAX_FAILED_ATTEMPTS);
        lock.put("now", new Date());
        int locked = webUserFacade.updateByJpql("update WebUser u set u.twoFactorLocked=true, u.twoFactorLockedAt=:now"
                + " where u.id=:id and u.twoFactorLocked=false and u.twoFactorFailedAttempts >= :max", lock);
        if (locked > 0) {
            logTwoFactorEvent(u, u, EVENT_ACCOUNT_LOCKED);
        }
        return true;
    }

    /**
     * A correct second-step code (authenticator or SMS): clears the failure
     * count and completes the login, giving the user access.
     */
    public String completeLoginAfterSecondFactor(WebUser u, String auditTrigger) {
        // Clear the counter with one UPDATE (never on a locked account), then continue with a fresh copy so
        // the login does not write an old copy of this user back
        Map<String, Object> p = new HashMap<>();
        p.put("id", u.getId());
        webUserFacade.updateByJpql("update WebUser u set u.twoFactorFailedAttempts=0"
                + " where u.id=:id and u.twoFactorLocked=false", p);
        WebUser fresh = freshUser(u.getId());
        if (fresh == null || fresh.isTwoFactorLocked()) {
            cancelLogin();
            showLockedNotice(u.getName());
            return "";
        }
        u = fresh;
        boolean channeling = channelingTabView;
        clearChallenge();
        // A full password + code login ends any "sign in again" state from an earlier forced setup
        reloginRequired = false;
        String outcome = sessionController.completeTwoFactorLogin(u, channeling);
        if (outcome != null && !outcome.isEmpty()) {
            secondFactorVerified = true;
            logTwoFactorEvent(u, u, auditTrigger);
        }
        return outcome;
    }

    /**
     * Saves the mobile number confirmed by the first SMS login, writing only
     * those two columns.
     */
    public void saveVerifiedLoginMobile(WebUser u, String mobile) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", u.getId());
        p.put("m", mobile);
        p.put("t", new Date());
        webUserFacade.updateByJpql("update WebUser u set u.twoFactorMobile=:m, u.twoFactorMobileVerifiedAt=:t"
                + " where u.id=:id", p);
    }

    /**
     * Records an SMS-related event (code sent, send failed, mobile verified
     * ...).
     */
    public void logSmsEvent(WebUser actor, WebUser subject, String trigger, Map<String, Object> details, String status) {
        logTwoFactorEvent(actor, subject, trigger, null, details, status);
    }

    public String cancelLogin() {
        clearChallenge();
        return "";
    }

    private void clearChallenge() {
        pendingUserId = null;
        pendingUserName = null;
        code = null;
        channelingTabView = false;
        twoFactorSmsController.clear();
    }

    public boolean isPending() {
        if (pendingUserId != null && isPendingExpired()) {
            // Waited too long for the code: the half-finished login is dropped on its own
            WebUser expired = webUserFacade.find(pendingUserId);
            logTwoFactorEvent(expired, expired, EVENT_LOGIN_TIMED_OUT, null, null, "Failed");
            clearChallenge();
            loginTimedOutNotice = true;
        }
        return pendingUserId != null;
    }

    private boolean isPendingExpired() {
        return System.currentTimeMillis() - pendingStartedAt > PENDING_LOGIN_TTL_MS;
    }

    /**
     * Whether this session may link an authenticator app: its login passed a
     * second step, or a code sent to the staff-record mobile was entered.
     * A password alone is never enough.
     */
    public boolean isSetupProofSatisfied() {
        return secondFactorVerified || setupProofed;
    }

    /**
     * Called by TwoFactorSmsController when a code sent to the staff-record
     * mobile is entered correctly.
     */
    public void markSetupProofed() {
        setupProofed = true;
    }

    // <editor-fold defaultstate="collapsed" desc="Self-service setup / reset">
    public String navigateToMyAuthenticator() {
        selfSetupSecret = null;
        selfSetupCode = null;
        selfSetupFailedAttempts = 0;
        return "/user_authenticator?faces-redirect=true";
    }

    public void startSelfSetup() {
        if (isLoggedUserEnrolled()) {
            // Linking a new phone is done by an admin reset, not by the user
            JsfUtil.addErrorMessage("Your authenticator app is already linked. Ask an administrator to reset it.");
            return;
        }
        String blocked = getSetupBlockedMessage();
        if (blocked != null) {
            JsfUtil.addErrorMessage(blocked);
            return;
        }
        if (!isSetupProofSatisfied()) {
            // Password-only session: first a code to the staff-record mobile; a correct code calls this again
            twoFactorSmsController.startSetupProof();
            return;
        }
        selfSetupSecret = TotpUtil.generateSecret();
        selfSetupCode = null;
        selfSetupFailedAttempts = 0;
    }

    public void cancelSelfSetup() {
        selfSetupSecret = null;
        selfSetupCode = null;
        selfSetupFailedAttempts = 0;
    }

    public String confirmSelfSetup() {
        WebUser u = loggedUserFromDb();
        if (u == null || selfSetupSecret == null || !isSetupProofSatisfied()) {
            selfSetupSecret = null;
            JsfUtil.addErrorMessage("Start the setup again.");
            return null;
        }
        if (!isSetupWindowOpenFor(u)) {
            selfSetupSecret = null;
            JsfUtil.addErrorMessage(SETUP_WINDOW_EXPIRED_MESSAGE);
            return null;
        }
        long step = TotpUtil.verify(selfSetupSecret, selfSetupCode, null);
        selfSetupCode = null;
        if (step < 0) {
            selfSetupFailedAttempts++;
            Map<String, Object> attempt = new HashMap<>();
            attempt.put("failedAttempts", selfSetupFailedAttempts);
            logTwoFactorEvent(u, u, EVENT_SETUP_FAILED, null, attempt, "Failed");
            if (selfSetupFailedAttempts >= MAX_FAILED_ATTEMPTS) {
                // The password is already verified, so the account is not locked; the guessed QR is discarded
                // and the user is returned to the status screen to start the setup again
                selfSetupSecret = null;
                selfSetupFailedAttempts = 0;
                logTwoFactorEvent(u, u, EVENT_SETUP_CANCELLED_FAILED_ATTEMPTS);
                JsfUtil.addErrorMessage("Too many invalid codes. The setup has been cancelled. "
                        + "Delete the entry for this account from your app, then click Set Up Authenticator App to scan a new QR code.");
            } else {
                JsfUtil.addErrorMessage("Invalid authenticator code. Check the time on your phone and try again. "
                        + (MAX_FAILED_ATTEMPTS - selfSetupFailedAttempts) + " attempt(s) left before the setup is cancelled.");
            }
            return null;
        }
        selfSetupFailedAttempts = 0;
        String stored = TwoFactorSecretCipher.encrypt(selfSetupSecret, u.getId());
        if (stored == null) {
            JsfUtil.addErrorMessage(NOT_SET_UP_MESSAGE);
            return null;
        }
        boolean wasForcedSetup = isSetupRequiredNow();
        Map<String, Object> link = new HashMap<>();
        link.put("id", u.getId());
        link.put("sec", stored);
        link.put("now", new Date());
        link.put("step", step);
        // Only when no app is linked yet: two set-ups at the same moment cannot both win
        int linked = webUserFacade.updateByJpql("update WebUser u set u.twoFactorSecret=:sec, u.twoFactorEnrolled=true,"
                + " u.twoFactorEnrolledAt=:now, u.twoFactorLastUsedStep=:step"
                + " where u.id=:id and u.twoFactorEnrolled=false", link);
        if (linked != 1) {
            selfSetupSecret = null;
            JsfUtil.addErrorMessage("Your authenticator app is already linked. Ask an administrator to reset it.");
            return null;
        }
        logTwoFactorEvent(u, u, wasForcedSetup ? EVENT_FIRST_LOGIN_SETUP : EVENT_SELF_SETUP);
        // Keep the session copy in step with the database
        copyEnrollment(freshUser(u.getId()), sessionController.getLoggedUser());
        selfSetupSecret = null;
        if (wasForcedSetup) {
            // The templates keep showing only this screen, which now logs the user out so the
            // first login with the new app is a full password and code login
            reloginRequired = true;
            return null;
        }
        JsfUtil.addSuccessMessage("Authenticator app linked.");
        return null;
    }

    public boolean isSelfSetupInProgress() {
        return selfSetupSecret != null;
    }

    public String getSelfSetupQrSvg() {
        WebUser u = sessionController.getLoggedUser();
        return qrSvg(selfSetupSecret, u == null ? "" : u.getName());
    }

    public String getSelfSetupSecretForDisplay() {
        return TotpUtil.formatSecretForDisplay(selfSetupSecret);
    }

    public boolean isLoggedUserEnrolled() {
        WebUser u = sessionController.getLoggedUser();
        return u != null && u.isTwoFactorEnrolled();
    }

    public boolean isLoggedUserRequired() {
        return isTwoFactorRequiredFor(sessionController.getLoggedUser());
    }
    // </editor-fold>

    /**
     * Admin action: unlinks the user's authenticator (e.g. lost phone). The
     * user sets it up again at the next login.
     */
    public void resetAuthenticator(WebUser user) {
        if (!hasUserManagementPrivilege()) {
            return;
        }
        if (user == null || user.getId() == null) {
            JsfUtil.addErrorMessage("Select a user");
            return;
        }
        WebUser u = freshUser(user.getId());
        if (u.isTwoFactorLocked()) {
            JsfUtil.addErrorMessage("Unlock the account before resetting the authenticator.");
            return;
        }
        // One UPDATE (never on a locked account); the user has 24 hours from now to set up again
        Map<String, Object> rp = new HashMap<>();
        rp.put("id", u.getId());
        rp.put("now", new Date());
        int done = webUserFacade.updateByJpql("update WebUser u set u.twoFactorSecret=null, u.twoFactorEnrolled=false,"
                + " u.twoFactorEnrolledAt=null, u.twoFactorLastUsedStep=null, u.twoFactorSetupWindowStartedAt=:now"
                + " where u.id=:id and u.twoFactorLocked=false", rp);
        if (done != 1) {
            JsfUtil.addErrorMessage("Unlock the account before resetting the authenticator.");
            return;
        }
        logTwoFactorEvent(sessionController.getLoggedUser(), u, EVENT_AUTHENTICATOR_RESET);
        logTwoFactorEvent(sessionController.getLoggedUser(), u, EVENT_SETUP_WINDOW_OPENED);
        u = freshUser(u.getId());
        copyEnrollment(u, user);
        WebUser lu = sessionController.getLoggedUser();
        if (lu != null && user.getId().equals(lu.getId())) {
            copyEnrollment(u, lu);
        }
        JsfUtil.addSuccessMessage("Authenticator reset. The user will set it up again at next login.");
    }

    /**
     * Admin action: unlocks an account locked by too many invalid
     * authenticator codes.
     */
    public void unlockAccount(WebUser user) {
        if (!hasUserManagementPrivilege()) {
            return;
        }
        if (user == null || user.getId() == null) {
            JsfUtil.addErrorMessage("Select a user");
            return;
        }
        WebUser u = freshUser(user.getId());
        Map<String, Object> up = new HashMap<>();
        up.put("id", u.getId());
        webUserFacade.updateByJpql("update WebUser u set u.twoFactorLocked=false, u.twoFactorLockedAt=null,"
                + " u.twoFactorFailedAttempts=0 where u.id=:id", up);
        logTwoFactorEvent(sessionController.getLoggedUser(), u, EVENT_ACCOUNT_UNLOCKED);
        u = freshUser(u.getId());
        copyEnrollment(u, user);
        JsfUtil.addSuccessMessage("Account unlocked.");
    }

    /**
     * Admin action: removes the user's verified mobile number for SMS codes
     * (e.g. number changed). The user verifies a number again from My
     * Authenticator App.
     */
    public void resetSmsMobile(WebUser user) {
        if (!hasUserManagementPrivilege()) {
            return;
        }
        if (user == null || user.getId() == null) {
            JsfUtil.addErrorMessage("Select a user");
            return;
        }
        WebUser u = freshUser(user.getId());
        if (u.isTwoFactorLocked()) {
            JsfUtil.addErrorMessage("Unlock the account before removing the verified mobile.");
            return;
        }
        Map<String, Object> mp = new HashMap<>();
        mp.put("id", u.getId());
        webUserFacade.updateByJpql("update WebUser u set u.twoFactorMobile=null, u.twoFactorMobileVerifiedAt=null"
                + " where u.id=:id", mp);
        logTwoFactorEvent(sessionController.getLoggedUser(), u, EVENT_MOBILE_RESET);
        u = freshUser(u.getId());
        copyEnrollment(u, user);
        WebUser lu = sessionController.getLoggedUser();
        if (lu != null && user.getId().equals(lu.getId())) {
            copyEnrollment(u, lu);
        }
        JsfUtil.addSuccessMessage("Verified mobile removed. The user must verify a mobile number again to use SMS codes.");
    }

    /**
     * Server-side check for the admin 2FA actions. Manage User is reached
     * only with this privilege, but an action can be posted without the menu.
     */
    public boolean hasUserManagementPrivilege() {
        if (webUserController.hasPrivilege(Privileges.AdminManagingUsers.name())) {
            return true;
        }
        JsfUtil.addErrorMessage("You do not have the privilege to manage users.");
        return false;
    }

    // <editor-fold defaultstate="collapsed" desc="Audit log">
    /**
     * Records an admin turning "Require Authenticator App (2FA)" on or off
     * for a user.
     */
    public void logRequiredChange(WebUser subject, boolean wasRequired) {
        if (subject == null) {
            return;
        }
        Map<String, Object> before = new HashMap<>();
        before.put("twoFactorRequired", wasRequired);
        Map<String, Object> after = new HashMap<>();
        after.put("twoFactorRequired", subject.isTwoFactorRequired());
        logTwoFactorEvent(sessionController.getLoggedUser(), subject,
                subject.isTwoFactorRequired() ? EVENT_REQUIRED_ON : EVENT_REQUIRED_OFF, before, after);
    }

    /**
     * Records an admin allowing or stopping SMS codes for a user.
     */
    public void logSmsAllowedChange(WebUser subject, boolean wasAllowed) {
        if (subject == null) {
            return;
        }
        Map<String, Object> before = new HashMap<>();
        before.put("twoFactorSmsAllowed", wasAllowed);
        Map<String, Object> after = new HashMap<>();
        after.put("twoFactorSmsAllowed", subject.isTwoFactorSmsAllowed());
        logTwoFactorEvent(sessionController.getLoggedUser(), subject,
                subject.isTwoFactorSmsAllowed() ? EVENT_SMS_ALLOWED_ON : EVENT_SMS_ALLOWED_OFF, before, after);
    }

    private void logTwoFactorEvent(WebUser actor, WebUser subject, String trigger) {
        logTwoFactorEvent(actor, subject, trigger, null, null);
    }

    private void logTwoFactorEvent(WebUser actor, WebUser subject, String trigger, Object before, Object after) {
        logTwoFactorEvent(actor, subject, trigger, before, after, null);
    }

    private void logTwoFactorEvent(WebUser actor, WebUser subject, String trigger, Object before, Object after, String eventStatus) {
        if (actor == null || subject == null || subject.getId() == null) {
            return;
        }
        auditService.logAudit(before, after, actor, AUDIT_ENTITY_TYPE, trigger, subject.getId(), eventStatus);
    }

    /**
     * Loads the latest authentication events of a user for the popup on
     * Manage User.
     */
    public void loadAuthLogs(WebUser user) {
        authLogs = new ArrayList<>();
        if (user == null || user.getId() == null) {
            return;
        }
        String jpql = "select a from AuditEvent a"
                + " where a.entityType=:et"
                + " and a.objectId=:uid"
                + " order by a.eventDataTime desc, a.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("et", AUDIT_ENTITY_TYPE);
        m.put("uid", user.getId());
        authLogs = auditEventFacade.findByJpql(jpql, m, AUTH_LOG_LIMIT);
    }

    public String userNameOf(Long webUserId) {
        if (webUserId == null) {
            return "";
        }
        WebUser u = webUserFacade.find(webUserId);
        return u == null ? "" : u.getName();
    }

    public List<AuditEvent> getAuthLogs() {
        return authLogs;
    }
    // </editor-fold>

    /**
     * Copies the enrollment and lock fields, which only this controller
     * writes, from the database copy onto another in-memory copy of the same
     * user.
     */
    public static void copyEnrollment(WebUser from, WebUser to) {
        if (from == null || to == null) {
            return;
        }
        to.setTwoFactorSecret(from.getTwoFactorSecret());
        to.setTwoFactorEnrolled(from.isTwoFactorEnrolled());
        to.setTwoFactorEnrolledAt(from.getTwoFactorEnrolledAt());
        to.setTwoFactorLastUsedStep(from.getTwoFactorLastUsedStep());
        to.setTwoFactorFailedAttempts(from.getTwoFactorFailedAttempts());
        to.setTwoFactorLocked(from.isTwoFactorLocked());
        to.setTwoFactorLockedAt(from.getTwoFactorLockedAt());
        to.setTwoFactorMobile(from.getTwoFactorMobile());
        to.setTwoFactorMobileVerifiedAt(from.getTwoFactorMobileVerifiedAt());
        to.setTwoFactorSetupWindowStartedAt(from.getTwoFactorSetupWindowStartedAt());
    }

    /**
     * Plain secret of the user, or null when it cannot be read. Once this
     * server has a key, only gcm values are read: a value from the older
     * scheme that start-up did not re-encrypt is treated as unreadable (the
     * user needs an admin Reset). Without a key, the older scheme is read as
     * before.
     */
    private String decryptSecret(WebUser u) {
        String stored = u.getTwoFactorSecret();
        if (TwoFactorSecretCipher.isAnyGcmFormat(stored)) {
            return TwoFactorSecretCipher.decrypt(stored, u.getId());
        }
        if (TwoFactorSecretCipher.isKeyConfigured()) {
            return null;
        }
        return SecurityController.decryptLegacy(stored);
    }

    public boolean isSecretKeyConfigured() {
        return TwoFactorSecretCipher.isKeyConfigured();
    }

    private WebUser loggedUserFromDb() {
        WebUser lu = sessionController.getLoggedUser();
        if (lu == null || lu.getId() == null) {
            return null;
        }
        return freshUser(lu.getId());
    }

    private String qrSvg(String secret, String accountName) {
        if (secret == null) {
            return "";
        }
        String uri = TotpUtil.buildOtpAuthUri(configOptionApplicationController.getAuthenticatorAppIssuerName(), accountName, secret);
        return TotpUtil.toQrSvg(uri, QR_SIZE_PX);
    }

    public String getPendingUserName() {
        return pendingUserName;
    }

    public Long getPendingUserId() {
        return pendingUserId;
    }

    public int getMaxFailedAttempts() {
        return MAX_FAILED_ATTEMPTS;
    }

    public boolean isLoginTimedOutNotice() {
        return loginTimedOutNotice;
    }

    public boolean isAccountLockedNotice() {
        return accountLockedNotice;
    }

    public void showLockedNotice(String userName) {
        accountLockedNotice = true;
        lockedUserName = userName;
    }

    public String dismissLockedNotice() {
        accountLockedNotice = false;
        lockedUserName = null;
        return "";
    }

    public String getLockedUserName() {
        return lockedUserName;
    }

    public String getLockedMessage() {
        return LOCKED_MESSAGE;
    }

    public boolean isChannelingTabView() {
        return channelingTabView;
    }

    public void setChannelingTabView(boolean channelingTabView) {
        this.channelingTabView = channelingTabView;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSelfSetupCode() {
        return selfSetupCode;
    }

    public void setSelfSetupCode(String selfSetupCode) {
        this.selfSetupCode = selfSetupCode;
    }
}

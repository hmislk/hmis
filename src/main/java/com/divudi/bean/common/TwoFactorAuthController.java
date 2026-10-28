package com.divudi.bean.common;

import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.TotpUtil;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
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
            + " invalid authenticator codes. Please contact an administrator to unlock it.";

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
    private SecurityController securityController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    // Login challenge state (password already verified, code pending)
    private Long pendingUserId;
    private String pendingUserName;
    private boolean channelingTabView;
    // Shown on the login card when the account is locked (a growl disappears too quickly)
    private boolean accountLockedNotice;
    private String lockedUserName;
    // Plain secret shown during first-time setup; saved only after a code confirms it
    private String setupSecret;
    private String code;

    // Self-service setup / reset from the user's menu
    private String selfSetupSecret;
    private String selfSetupCode;

    // Authentication log popup on Manage User
    private List<AuditEvent> authLogs;

    public boolean isTwoFactorEnabled() {
        return configOptionApplicationController.isAuthenticatorAppTwoFactorLoginEnabled();
    }

    public boolean isTwoFactorRequiredFor(WebUser u) {
        if (u == null || !isTwoFactorEnabled()) {
            return false;
        }
        return configOptionApplicationController.isAuthenticatorAppTwoFactorLoginRequiredForAllUsers()
                || u.isTwoFactorRequired();
    }

    /**
     * Whether the login must stop for an authenticator code: every user with
     * a linked app, required or not. A required user without one logs in and
     * is held on the setup screen instead (see {@link #isSetupRequiredNow()}).
     */
    public boolean isCodeNeededAtLoginFor(WebUser u) {
        if (u == null || !isTwoFactorEnabled()) {
            return false;
        }
        return u.isTwoFactorEnrolled();
    }

    /**
     * True while the logged user must use 2FA but has not linked an app; the
     * templates then show only the setup screen, whatever page is opened.
     */
    public boolean isSetupRequiredNow() {
        WebUser u = sessionController.getLoggedUser();
        return u != null && !u.isTwoFactorEnrolled() && isTwoFactorRequiredFor(u);
    }

    public void startChallenge(WebUser u) {
        pendingUserId = u.getId();
        pendingUserName = u.getName();
        code = null;
        if (!u.isTwoFactorEnrolled() || u.getTwoFactorSecret() == null) {
            setupSecret = TotpUtil.generateSecret();
        } else {
            setupSecret = null;
        }
    }

    public String verifyLoginCode() {
        if (!isPending()) {
            return "";
        }
        WebUser u = webUserFacade.find(pendingUserId);
        if (u == null || u.isRetired()) {
            cancelLogin();
            JsfUtil.addErrorMessage("Login Failure. Please try again");
            return "";
        }
        if (u.isTwoFactorLocked()) {
            cancelLogin();
            showLockedNotice(u.getName());
            return "";
        }
        boolean enrolling = setupSecret != null;
        String secret = enrolling ? setupSecret : securityController.decrypt(u.getTwoFactorSecret());
        long step = TotpUtil.verify(secret, code, enrolling ? null : u.getTwoFactorLastUsedStep());
        code = null;
        if (step < 0) {
            // Counted on the user, not the session, so logging in again does not give fresh attempts
            int failedAttempts = u.getTwoFactorFailedAttempts() + 1;
            u.setTwoFactorFailedAttempts(failedAttempts);
            Map<String, Object> attempt = new HashMap<>();
            attempt.put("failedAttempts", failedAttempts);
            // Nobody is logged in yet, so the user trying to log in is recorded as the actor
            logTwoFactorEvent(u, u, EVENT_LOGIN_FAILED, null, attempt, "Failed");
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                u.setTwoFactorLocked(true);
                u.setTwoFactorLockedAt(new Date());
                webUserFacade.edit(u);
                logTwoFactorEvent(u, u, EVENT_ACCOUNT_LOCKED);
                cancelLogin();
                showLockedNotice(u.getName());
            } else {
                webUserFacade.edit(u);
                JsfUtil.addErrorMessage("Invalid authenticator code. " + (MAX_FAILED_ATTEMPTS - failedAttempts) + " attempt(s) left.");
            }
            return "";
        }
        if (enrolling) {
            u.setTwoFactorSecret(securityController.encrypt(setupSecret));
            u.setTwoFactorEnrolled(true);
            u.setTwoFactorEnrolledAt(new Date());
        }
        u.setTwoFactorFailedAttempts(0);
        u.setTwoFactorLastUsedStep(step);
        webUserFacade.edit(u);
        boolean channeling = channelingTabView;
        clearChallenge();
        String outcome = sessionController.completeTwoFactorLogin(u, channeling);
        if (outcome != null && !outcome.isEmpty()) {
            logTwoFactorEvent(u, u, enrolling ? EVENT_FIRST_LOGIN_SETUP : EVENT_LOGIN_SUCCESS);
        }
        return outcome;
    }

    public String cancelLogin() {
        clearChallenge();
        return "";
    }

    private void clearChallenge() {
        pendingUserId = null;
        pendingUserName = null;
        setupSecret = null;
        code = null;
        channelingTabView = false;
    }

    public boolean isPending() {
        return pendingUserId != null;
    }

    public boolean isEnrollmentNeeded() {
        return setupSecret != null;
    }

    public String getSetupQrSvg() {
        return qrSvg(setupSecret, pendingUserName);
    }

    public String getSetupSecretForDisplay() {
        return TotpUtil.formatSecretForDisplay(setupSecret);
    }

    // <editor-fold defaultstate="collapsed" desc="Self-service setup / reset">
    public String navigateToMyAuthenticator() {
        selfSetupSecret = null;
        selfSetupCode = null;
        return "/user_authenticator?faces-redirect=true";
    }

    public void startSelfSetup() {
        selfSetupSecret = TotpUtil.generateSecret();
        selfSetupCode = null;
    }

    public void cancelSelfSetup() {
        selfSetupSecret = null;
        selfSetupCode = null;
    }

    public String confirmSelfSetup() {
        WebUser u = loggedUserFromDb();
        if (u == null || selfSetupSecret == null) {
            JsfUtil.addErrorMessage("Start the setup again.");
            return null;
        }
        long step = TotpUtil.verify(selfSetupSecret, selfSetupCode, null);
        selfSetupCode = null;
        if (step < 0) {
            JsfUtil.addErrorMessage("Invalid authenticator code. Check the time on your phone and try again.");
            return null;
        }
        boolean wasForcedSetup = isSetupRequiredNow();
        u.setTwoFactorSecret(securityController.encrypt(selfSetupSecret));
        u.setTwoFactorEnrolled(true);
        u.setTwoFactorEnrolledAt(new Date());
        u.setTwoFactorLastUsedStep(step);
        webUserFacade.edit(u);
        logTwoFactorEvent(u, u, wasForcedSetup ? EVENT_FIRST_LOGIN_SETUP : EVENT_SELF_SETUP);
        // Keep the session copy in step so a later edit(loggedUser) does not restore the old secret
        copyEnrollment(u, sessionController.getLoggedUser());
        selfSetupSecret = null;
        JsfUtil.addSuccessMessage("Authenticator app linked.");
        if (wasForcedSetup) {
            // The templates were showing only this screen; open the user's home page now
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
            String home = sessionController.navigateToLoginPageByUsersDefaultLoginPage();
            if (home == null || home.isEmpty()) {
                home = "/index1";
            }
            // Redirect so the whole template renders again, not just this ajax form
            return home.contains("faces-redirect") ? home : home + (home.contains("?") ? "&" : "?") + "faces-redirect=true";
        }
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
        if (user == null || user.getId() == null) {
            JsfUtil.addErrorMessage("Select a user");
            return;
        }
        WebUser u = webUserFacade.find(user.getId());
        if (u.isTwoFactorLocked()) {
            JsfUtil.addErrorMessage("Unlock the account before resetting the authenticator.");
            return;
        }
        clearEnrollment(u);
        webUserFacade.edit(u);
        logTwoFactorEvent(sessionController.getLoggedUser(), u, EVENT_AUTHENTICATOR_RESET);
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
        if (user == null || user.getId() == null) {
            JsfUtil.addErrorMessage("Select a user");
            return;
        }
        WebUser u = webUserFacade.find(user.getId());
        u.setTwoFactorLocked(false);
        u.setTwoFactorLockedAt(null);
        u.setTwoFactorFailedAttempts(0);
        webUserFacade.edit(u);
        logTwoFactorEvent(sessionController.getLoggedUser(), u, EVENT_ACCOUNT_UNLOCKED);
        copyEnrollment(u, user);
        JsfUtil.addSuccessMessage("Account unlocked.");
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
                + " order by a.eventDataTime desc";
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

    private void clearEnrollment(WebUser u) {
        u.setTwoFactorSecret(null);
        u.setTwoFactorEnrolled(false);
        u.setTwoFactorEnrolledAt(null);
        u.setTwoFactorLastUsedStep(null);
    }

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
    }

    private WebUser loggedUserFromDb() {
        WebUser lu = sessionController.getLoggedUser();
        if (lu == null || lu.getId() == null) {
            return null;
        }
        return webUserFacade.find(lu.getId());
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

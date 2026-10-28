package com.divudi.bean.common;

import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.TotpUtil;
import java.io.Serializable;
import java.util.Date;
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

    @EJB
    private WebUserFacade webUserFacade;

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
    private int failedAttempts;
    // Plain secret shown during first-time setup; saved only after a code confirms it
    private String setupSecret;
    private String code;

    // Self-service setup / reset from the user's menu
    private String selfSetupSecret;
    private String selfSetupCode;

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

    public void startChallenge(WebUser u) {
        pendingUserId = u.getId();
        pendingUserName = u.getName();
        failedAttempts = 0;
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
        boolean enrolling = setupSecret != null;
        String secret = enrolling ? setupSecret : securityController.decrypt(u.getTwoFactorSecret());
        long step = TotpUtil.verify(secret, code, enrolling ? null : u.getTwoFactorLastUsedStep());
        code = null;
        if (step < 0) {
            failedAttempts++;
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                cancelLogin();
                JsfUtil.addErrorMessage("Too many invalid codes. Please log in again.");
            } else {
                JsfUtil.addErrorMessage("Invalid authenticator code. " + (MAX_FAILED_ATTEMPTS - failedAttempts) + " attempt(s) left.");
            }
            return "";
        }
        if (enrolling) {
            u.setTwoFactorSecret(securityController.encrypt(setupSecret));
            u.setTwoFactorEnrolled(true);
            u.setTwoFactorEnrolledAt(new Date());
        }
        u.setTwoFactorLastUsedStep(step);
        webUserFacade.edit(u);
        boolean channeling = channelingTabView;
        clearChallenge();
        return sessionController.completeTwoFactorLogin(u, channeling);
    }

    public String cancelLogin() {
        clearChallenge();
        return "";
    }

    private void clearChallenge() {
        pendingUserId = null;
        pendingUserName = null;
        failedAttempts = 0;
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

    public void confirmSelfSetup() {
        WebUser u = loggedUserFromDb();
        if (u == null || selfSetupSecret == null) {
            JsfUtil.addErrorMessage("Start the setup again.");
            return;
        }
        long step = TotpUtil.verify(selfSetupSecret, selfSetupCode, null);
        selfSetupCode = null;
        if (step < 0) {
            JsfUtil.addErrorMessage("Invalid authenticator code. Check the time on your phone and try again.");
            return;
        }
        u.setTwoFactorSecret(securityController.encrypt(selfSetupSecret));
        u.setTwoFactorEnrolled(true);
        u.setTwoFactorEnrolledAt(new Date());
        u.setTwoFactorLastUsedStep(step);
        webUserFacade.edit(u);
        // Keep the session copy in step so a later edit(loggedUser) does not restore the old secret
        copyEnrollment(u, sessionController.getLoggedUser());
        selfSetupSecret = null;
        JsfUtil.addSuccessMessage("Authenticator app linked.");
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
        clearEnrollment(u);
        webUserFacade.edit(u);
        copyEnrollment(u, user);
        WebUser lu = sessionController.getLoggedUser();
        if (lu != null && user.getId().equals(lu.getId())) {
            copyEnrollment(u, lu);
        }
        JsfUtil.addSuccessMessage("Authenticator reset. The user will set it up again at next login.");
    }

    private void clearEnrollment(WebUser u) {
        u.setTwoFactorSecret(null);
        u.setTwoFactorEnrolled(false);
        u.setTwoFactorEnrolledAt(null);
        u.setTwoFactorLastUsedStep(null);
    }

    /**
     * Copies the enrollment fields, which only this controller writes, from
     * the database copy onto another in-memory copy of the same user.
     */
    public static void copyEnrollment(WebUser from, WebUser to) {
        if (from == null || to == null) {
            return;
        }
        to.setTwoFactorSecret(from.getTwoFactorSecret());
        to.setTwoFactorEnrolled(from.isTwoFactorEnrolled());
        to.setTwoFactorEnrolledAt(from.getTwoFactorEnrolledAt());
        to.setTwoFactorLastUsedStep(from.getTwoFactorLastUsedStep());
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

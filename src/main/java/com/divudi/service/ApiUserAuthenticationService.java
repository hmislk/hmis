package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SecurityController;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.WebUserFacade;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * The one username + password check for callers that cannot ask for a
 * second-step code.
 *
 * {@link #authenticate} (channelling API, which hands out API keys): a
 * password alone must never let in an account that two-factor login
 * protects, so such accounts (and accounts locked by invalid codes) are
 * refused. {@link #authenticateMachine} (lab analysers and middleware, which
 * run unattended): name and password only, as before.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Stateless
public class ApiUserAuthenticationService {

    @EJB
    private WebUserFacade webUserFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /**
     * @return the user when the name and password match and the account may
     * log in with a password alone; otherwise null
     */
    public WebUser authenticate(String userName, String password) {
        WebUser u = authenticateMachine(userName, password);
        if (u == null || isPasswordOnlyLoginRefused(u)) {
            return null;
        }
        return u;
    }

    /**
     * Name + password only, without the two-factor refusal: for lab
     * analysers and lab middleware, which send results automatically with no
     * person present to enter a code. Use a dedicated machine account.
     *
     * @return the user when the name and password match; otherwise null
     */
    public WebUser authenticateMachine(String userName, String password) {
        if (userName == null || userName.trim().isEmpty() || password == null) {
            return null;
        }
        String jpql = "select u from WebUser u"
                + " where u.retired=:ret"
                + " and u.name=:n"
                + " order by u.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("n", userName.trim().toLowerCase());
        WebUser u = webUserFacade.findFirstByJpql(jpql, m);
        if (u == null) {
            return null;
        }
        if (!SecurityController.matchPassword(password, u.getWebUserPassword())) {
            return null;
        }
        return u;
    }

    /**
     * True when the account is locked, or two-factor login applies to it in
     * any way: required (for everyone or for this user), an authenticator app
     * linked, or SMS codes allowed.
     */
    public boolean isPasswordOnlyLoginRefused(WebUser u) {
        if (u == null) {
            return true;
        }
        if (u.isTwoFactorLocked()) {
            return true;
        }
        if (!configOptionApplicationController.isAuthenticatorAppTwoFactorLoginEnabled()) {
            return false;
        }
        if (configOptionApplicationController.isAuthenticatorAppTwoFactorLoginRequiredForAllUsers()) {
            return true;
        }
        if (u.isTwoFactorRequired() || u.isTwoFactorEnrolled()) {
            return true;
        }
        return configOptionApplicationController.isTwoFactorSmsLoginEnabled() && u.isTwoFactorSmsAllowed();
    }
}

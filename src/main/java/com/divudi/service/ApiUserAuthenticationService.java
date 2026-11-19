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
 * second-step code: the channelling API (which hands out API keys), lab
 * analysers and lab middleware (which run unattended).
 *
 * Service-login rule: an account is refused when two-factor login is on and
 * it is protected one by one (an authenticator app linked, "required" ticked
 * for the user, or SMS codes allowed), or when it is locked by invalid codes.
 * The "all users" switch is not applied, so unflagged machine accounts keep
 * working. Use a dedicated, never-flagged account for each machine.
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
     * log in with a password alone (service-login rule); otherwise null
     */
    public WebUser authenticate(String userName, String password) {
        return authenticateMachine(userName, password);
    }

    /**
     * Same check under the name the lab callers use.
     *
     * @return the user when the name and password match and the account may
     * log in with a password alone (service-login rule); otherwise null
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
        if (isPasswordOnlyLoginRefused(u)) {
            return null;
        }
        return u;
    }

    /**
     * True when the account is locked, or two-factor login protects it one
     * by one (service-login rule, see the class comment).
     */
    public boolean isPasswordOnlyLoginRefused(WebUser u) {
        if (u == null) {
            return true;
        }
        if (u.isTwoFactorLocked()) {
            return true;
        }
        return configOptionApplicationController.doesTwoFactorApplyToServiceLogin(u);
    }
}

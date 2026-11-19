package com.divudi.service;

import com.divudi.bean.common.SecurityController;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.TwoFactorSecretCipher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

/**
 * At startup, re-encrypts authenticator secrets still saved by the older
 * scheme (or as gcm1) in the newest format under this server's own key (see TwoFactorSecretCipher). Does
 * nothing when the key is not configured. Secrets of users who log in are
 * also re-encrypted at login, so a failure here only delays the change.
 *
 * Uses the CDI @Initialized observer, not an EJB @Startup singleton, for the
 * reason given in ApplicationStartupTimeService.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@ApplicationScoped
public class TwoFactorSecretMigration {

    private static final Logger LOGGER = Logger.getLogger(TwoFactorSecretMigration.class.getName());

    @EJB
    private WebUserFacade webUserFacade;

    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
        try {
            reencryptLegacySecrets();
        } catch (Throwable t) {
            // Never block deployment. A row left in the old format is read as unreadable once a key is set:
            // it is retried at the next restart, or the user needs an admin Reset
            LOGGER.log(Level.WARNING, "Two-factor secret re-encryption skipped", t);
        }
    }

    public int reencryptLegacySecrets() {
        if (!TwoFactorSecretCipher.isKeyConfigured()) {
            return 0;
        }
        String jpql = "select u from WebUser u"
                + " where u.twoFactorSecret is not null"
                + " and u.twoFactorSecret not like :prefix";
        Map<String, Object> m = new HashMap<>();
        m.put("prefix", "gcm2:%");
        List<WebUser> users = webUserFacade.findByJpql(jpql, m);
        int done = 0;
        for (WebUser u : users) {
            try {
            String plain = TwoFactorSecretCipher.isAnyGcmFormat(u.getTwoFactorSecret())
                    ? TwoFactorSecretCipher.decrypt(u.getTwoFactorSecret(), u.getId())
                    : SecurityController.decryptLegacy(u.getTwoFactorSecret());
            if (plain == null) {
                continue;
            }
            String stored = TwoFactorSecretCipher.encrypt(plain, u.getId());
            if (stored == null) {
                continue;
            }
            // Only the secret, and only if it still holds the value read above: a reset or change made
            // meanwhile (e.g. by another server) is kept, and no other column is written
            String update = "update WebUser u set u.twoFactorSecret=:newSecret"
                    + " where u.id=:id and u.twoFactorSecret=:oldSecret";
            Map<String, Object> p = new HashMap<>();
            p.put("newSecret", stored);
            p.put("id", u.getId());
            p.put("oldSecret", u.getTwoFactorSecret());
            done += webUserFacade.updateByJpql(update, p);
            } catch (RuntimeException e) {
                // One failing row must not stop the rows after it
                LOGGER.log(Level.WARNING, "Two-factor secret of user {0} was not re-encrypted", u.getId());
            }
        }
        if (done > 0) {
            LOGGER.log(Level.INFO, "Re-encrypted {0} two-factor secret(s) under the server key", done);
        }
        return done;
    }
}

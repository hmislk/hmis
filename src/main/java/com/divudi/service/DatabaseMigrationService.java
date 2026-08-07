package com.divudi.service;

import com.divudi.core.data.OptionScope;
import com.divudi.core.facade.ConfigOptionFacade;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Singleton EJB that controls access to the migration page (mf.xhtml).
 *
 * On every deployment/restart, migrationPending starts as true, making the
 * page accessible to anyone. If the stored DATABASE_DDL_VERSION config option
 * is set to "CONFIRMED", migration is automatically marked as not necessary
 * and the banner is suppressed. Otherwise, a background check (see
 * {@link DatabaseMigrationVersionCheckService}) compares the stored version
 * against the wiki's current DDL version shortly after startup and clears
 * the banner automatically if they already match. Failing that, the banner
 * remains until an admin visits mf.xhtml and marks the migration as
 * complete or not necessary.
 *
 * The wiki DDL version check runs asynchronously (never inline in
 * {@code @PostConstruct}) to avoid blocking the deploy thread with an
 * outbound HTTP request at startup — see commit c32868a9f5, which removed
 * an earlier synchronous version of this check for exactly that reason.
 *
 * @author Dr M H B Ariyaratne
 */
@Singleton
@Startup
@PermitAll
public class DatabaseMigrationService {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrationService.class.getName());
    private static final String CONFIG_KEY_DDL_VERSION = "DATABASE_DDL_VERSION";

    @EJB
    private ConfigOptionFacade configOptionFacade;

    @EJB
    private DatabaseMigrationVersionCheckService databaseMigrationVersionCheckService;

    @Resource
    private SessionContext sessionContext;

    private volatile boolean migrationPending = true;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void init() {
        try {
            String storedVersion = readStoredDdlVersion();
            if ("CONFIRMED".equals(storedVersion)) {
                migrationPending = false;
                LOGGER.info("DatabaseMigrationService: stored version is CONFIRMED — no migration pending.");
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "DatabaseMigrationService: Could not read stored DDL version at startup.", e);
        }
        LOGGER.info("DatabaseMigrationService: Migration page is open to all users until marked as complete or not necessary.");
        try {
            // Fire-and-forget: runs on a background thread via @Asynchronous.
            // Pass the container-managed no-interface business proxy (via
            // SessionContext.getBusinessObject), never raw "this" — the
            // callback's calls back into this bean must go through the
            // proxy so singleton concurrency locking applies. Also, the
            // outbound call itself must go through the injected
            // databaseMigrationVersionCheckService proxy, not a
            // self-invocation, or @Asynchronous would be silently ignored
            // and this would block startup exactly like the code removed
            // in c32868a9f5.
            DatabaseMigrationService self = sessionContext.getBusinessObject(DatabaseMigrationService.class);
            databaseMigrationVersionCheckService.checkAndUpdateMigrationStatus(self);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "DatabaseMigrationService: Could not schedule background DDL version check.", e);
        }
    }

    public String readStoredDdlVersion() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("key", CONFIG_KEY_DDL_VERSION);
            params.put("scope", OptionScope.APPLICATION);
            String jpql = "SELECT o FROM ConfigOption o WHERE o.retired=false AND o.optionKey=:key AND o.scope=:scope AND o.institution IS NULL AND o.department IS NULL AND o.webUser IS NULL";
            com.divudi.core.entity.ConfigOption option = configOptionFacade.findFirstByJpql(jpql, params);
            return option != null ? option.getOptionValue() : null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "DatabaseMigrationService: Could not read stored DDL version.", e);
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isMigrationPending() {
        return migrationPending;
    }

    public void markMigrationComplete() {
        migrationPending = false;
        LOGGER.info("DatabaseMigrationService: Migration marked as complete. Page now restricted to Admin only.");
    }

    public void markMigrationNotNecessary() {
        migrationPending = false;
        LOGGER.info("DatabaseMigrationService: Migration marked as not necessary. Page now restricted to Admin only.");
    }
}

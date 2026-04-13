/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service;

import com.divudi.core.facade.DatabaseMigrationFacade;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Ensures all entity table ID columns have AUTO_INCREMENT at application
 * startup.
 *
 * Background: switching from GenerationType.AUTO (shared SEQUENCE table) to
 * GenerationType.IDENTITY (per-table AUTO_INCREMENT) requires every entity
 * table's ID column to have AUTO_INCREMENT set. If the application is deployed
 * with the new GenerationType.IDENTITY code before the DB migration has run,
 * every INSERT fails with "Field 'ID' doesn't have a default value" (MySQL 1364).
 *
 * This @Singleton @Startup EJB runs once when Payara starts, before any user
 * can interact, and self-heals the database. The actual JDBC work is delegated
 * to DatabaseMigrationFacade.applyAutoIncrementToAllEntityTables() which uses
 * a proven raw-JDBC pattern (same as executeDdlNative) outside JTA.
 *
 * Pattern follows DatabaseMigrationService — inject facade via @EJB, not
 * @PersistenceContext directly, to avoid EclipseLink session initialisation
 * ordering issues at @Startup time.
 *
 * @author Dr M H B Ariyaratne
 */
@Singleton
@Startup
public class AutoIncrementBootstrapService {

    private static final Logger LOGGER = Logger.getLogger(AutoIncrementBootstrapService.class.getName());

    @EJB
    private DatabaseMigrationFacade migrationFacade;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void applyAutoIncrementIfNeeded() {
        try {
            List<String> altered = migrationFacade.applyAutoIncrementToAllEntityTables();
            if (altered.isEmpty()) {
                LOGGER.info("AutoIncrementBootstrap: all entity tables already have AUTO_INCREMENT — nothing to do.");
            } else {
                LOGGER.info("AutoIncrementBootstrap: applied AUTO_INCREMENT to " + altered.size()
                        + " table(s): " + altered);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "AutoIncrementBootstrap: failed to apply AUTO_INCREMENT — "
                    + "entity INSERTs may fail until migration v2.2.0 is run manually", e);
        }
    }
}

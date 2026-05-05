/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service;

import com.divudi.core.facade.AuditDatabaseFacade;
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

    @EJB
    private AuditDatabaseFacade auditDatabaseFacade;

    /**
     * Migration version key recorded in DatabaseMigration once all tables have
     * AUTO_INCREMENT. Subsequent startups check this record first and skip the
     * expensive information_schema scan when it already exists.
     */
    private static final String MIGRATION_VERSION = "v2.2.0";

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void applyAutoIncrementIfNeeded() {
        // Main database (hmisPU)
        try {
            if (migrationFacade.isMigrationExecuted(MIGRATION_VERSION)) {
                LOGGER.info("AutoIncrementBootstrap: migration " + MIGRATION_VERSION
                        + " already recorded as complete — skipping information_schema scan.");
            } else {
                long start = System.currentTimeMillis();
                List<String> altered = migrationFacade.applyAutoIncrementToAllEntityTables();
                long elapsed = System.currentTimeMillis() - start;
                if (altered.isEmpty()) {
                    LOGGER.info("AutoIncrementBootstrap: all entity tables already have AUTO_INCREMENT — nothing to do.");
                } else {
                    LOGGER.info("AutoIncrementBootstrap: applied AUTO_INCREMENT to " + altered.size()
                            + " table(s) in " + elapsed + "ms: " + altered);
                }
                // Record completion so future startups skip this scan entirely.
                recordMigrationComplete(elapsed);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "AutoIncrementBootstrap: failed to apply AUTO_INCREMENT — "
                    + "entity INSERTs may fail until migration v2.2.0 is run manually", e);
        }

        // Audit database (hmisAuditPU) — no persistent tracking table available;
        // the information_schema query returns immediately when all tables are done.
        try {
            List<String> altered = auditDatabaseFacade.applyAutoIncrementToAllEntityTables();
            if (altered.isEmpty()) {
                LOGGER.info("AutoIncrementBootstrap [AuditDB]: all tables already have AUTO_INCREMENT — nothing to do.");
            } else {
                LOGGER.info("AutoIncrementBootstrap [AuditDB]: applied AUTO_INCREMENT to " + altered.size()
                        + " table(s): " + altered);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "AutoIncrementBootstrap [AuditDB]: failed to apply AUTO_INCREMENT", e);
        }
    }

    private void recordMigrationComplete(long elapsedMs) {
        try {
            com.divudi.core.entity.DatabaseMigration record =
                    migrationFacade.findByVersion(MIGRATION_VERSION);
            if (record == null) {
                record = new com.divudi.core.entity.DatabaseMigration(
                        MIGRATION_VERSION,
                        "Apply AUTO_INCREMENT to all entity ID columns (AUTO→IDENTITY switch)",
                        "auto-increment-bootstrap");
            }
            record.setStatus(com.divudi.core.data.MigrationStatus.SUCCESS);
            record.setExecutionTimeMs(elapsedMs);
            if (record.getId() == null) {
                migrationFacade.create(record);
            } else {
                migrationFacade.edit(record);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "AutoIncrementBootstrap: could not record migration " + MIGRATION_VERSION
                    + " in DatabaseMigration table — scan will repeat on next startup", e);
        }
    }
}

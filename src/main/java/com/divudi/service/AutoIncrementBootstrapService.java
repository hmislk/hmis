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

/**
 * Ensures all entity table ID columns have AUTO_INCREMENT at application
 * startup.
 *
 * Runs on a daemon thread so the deploy thread is not blocked by the
 * INFORMATION_SCHEMA scan or ALTER TABLE statements, which can be slow
 * against remote (Azure) databases.
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

    private static final String MIGRATION_VERSION = "v2.2.0";

    @PostConstruct
    public void init() {
        Thread t = new Thread(this::applyAutoIncrementIfNeeded, "AutoIncrementBootstrap-startup");
        t.setDaemon(true);
        t.start();
    }

    private void applyAutoIncrementIfNeeded() {
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
                recordMigrationComplete(elapsed);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "AutoIncrementBootstrap: failed to apply AUTO_INCREMENT — "
                    + "entity INSERTs may fail until migration v2.2.0 is run manually", e);
        }

        // Audit database (hmisAuditPU)
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

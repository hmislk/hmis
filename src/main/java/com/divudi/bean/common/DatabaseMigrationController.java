/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.entity.DatabaseMigration;
import com.divudi.core.facade.DatabaseMigrationFacade;
import com.divudi.core.data.MigrationStatus;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.MigrationDiscoveryService;
import com.divudi.core.util.MigrationInfo;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.annotation.Resource;

/**
 * Controller for managing database migrations
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Named
@SessionScoped
public class DatabaseMigrationController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrationController.class.getName());

    @EJB
    private DatabaseMigrationFacade migrationFacade;

    @Inject
    private MigrationDiscoveryService discoveryService;

    @Inject
    private ConfigOptionApplicationController configController;

    @Inject
    private SessionController sessionController;

    @Resource(lookup = "java:jboss/datasources/hmisDatasource")
    private DataSource dataSource;

    // UI properties
    private List<MigrationInfo> availableMigrations;
    private List<DatabaseMigration> executedMigrations;
    private MigrationInfo selectedMigration;
    private String migrationLog;
    private boolean migrationInProgress;
    private AtomicBoolean migrationLock = new AtomicBoolean(false);

    // Migration progress
    private String currentMigrationVersion;
    private String currentStep;
    private int totalSteps;
    private int completedSteps;
    private long migrationStartTime;

    public DatabaseMigrationController() {
    }

    @PostConstruct
    public void init() {
        refreshMigrationLists();
    }

    /**
     * Refresh migration lists from database and filesystem
     */
    public void refreshMigrationLists() {
        try {
            availableMigrations = discoveryService.loadAllMigrationInfo();
            executedMigrations = migrationFacade.findAllMigrationsOrderedByVersion();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refreshing migration lists", e);
            JsfUtil.addErrorMessage("Error loading migration data: " + e.getMessage());
        }
    }

    /**
     * Get migrations that are pending execution
     */
    public List<MigrationInfo> getPendingMigrations() {
        List<MigrationInfo> pending = new ArrayList<>();
        String lastExecutedVersion = getLastExecutedVersion();

        for (MigrationInfo info : availableMigrations) {
            if (!isMigrationExecuted(info.getVersion())) {
                // Also check if this version is newer than last executed
                if (lastExecutedVersion == null || compareVersions(info.getVersion(), lastExecutedVersion) > 0) {
                    pending.add(info);
                }
            }
        }

        return pending;
    }

    /**
     * Execute all pending migrations
     */
    public void executeAllPendingMigrations() {
        if (!migrationLock.compareAndSet(false, true)) {
            JsfUtil.addErrorMessage("Migration is already in progress");
            return;
        }

        try {
            List<MigrationInfo> pending = getPendingMigrations();
            if (pending.isEmpty()) {
                JsfUtil.addSuccessMessage("No pending migrations to execute");
                return;
            }

            migrationInProgress = true;
            totalSteps = pending.size();
            completedSteps = 0;
            migrationStartTime = System.currentTimeMillis();
            migrationLog = "";

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("Starting migration execution at ").append(new Date()).append("\\n");
            logBuilder.append("Total migrations to execute: ").append(pending.size()).append("\\n\\n");

            boolean allSuccessful = true;

            for (MigrationInfo migrationInfo : pending) {
                currentMigrationVersion = migrationInfo.getVersion();
                currentStep = "Executing migration " + migrationInfo.getVersion();

                logBuilder.append("=== Executing Migration ").append(migrationInfo.getVersion()).append(" ===\\n");
                logBuilder.append("Description: ").append(migrationInfo.getDescription()).append("\\n");

                boolean success = executeSingleMigration(migrationInfo, logBuilder);
                completedSteps++;

                if (!success) {
                    allSuccessful = false;
                    logBuilder.append("MIGRATION FAILED - Stopping execution\\n");
                    break;
                }

                logBuilder.append("Migration ").append(migrationInfo.getVersion()).append(" completed successfully\\n\\n");
            }

            migrationLog = logBuilder.toString();
            migrationInProgress = false;
            currentMigrationVersion = null;
            currentStep = null;

            if (allSuccessful) {
                updateLastMigrationVersion();
                JsfUtil.addSuccessMessage("All migrations executed successfully");
            } else {
                JsfUtil.addErrorMessage("Migration execution failed. Check logs for details.");
            }

            refreshMigrationLists();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during migration execution", e);
            migrationInProgress = false;
            JsfUtil.addErrorMessage("Migration execution error: " + e.getMessage());
        } finally {
            migrationLock.set(false);
        }
    }

    /**
     * Execute a single migration
     */
    private boolean executeSingleMigration(MigrationInfo migrationInfo, StringBuilder logBuilder) {
        String version = migrationInfo.getVersion();
        long startTime = System.currentTimeMillis();

        try {
            // Create or update migration record
            DatabaseMigration migration = migrationFacade.createOrUpdateMigration(
                    version,
                    migrationInfo.getDescription(),
                    "migration.sql"
            );

            migration.setStatus(MigrationStatus.EXECUTING);
            migration.setExecutedBy(sessionController.getLoggedUser());
            migration.setRequiresDowntime(migrationInfo.isRequiresDowntime());
            migration.setEstimatedDurationMs(migrationInfo.getEstimatedDurationMs());
            migration.setMigrationMetadata(serializeMigrationInfo(migrationInfo));
            migrationFacade.edit(migration);

            // Load and execute SQL
            String sql = discoveryService.loadMigrationSql(version);
            if (sql == null || sql.trim().isEmpty()) {
                throw new RuntimeException("Migration SQL not found or empty for version: " + version);
            }

            logBuilder.append("Executing SQL for version ").append(version).append("\\n");
            executeSqlScript(sql, logBuilder);

            // Mark as successful
            long executionTime = System.currentTimeMillis() - startTime;
            migration.setStatus(MigrationStatus.SUCCESS);
            migration.setExecutionTimeMs(executionTime);
            migration.setExecutionLog(logBuilder.toString());
            migrationFacade.edit(migration);

            logBuilder.append("Execution time: ").append(executionTime).append("ms\\n");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing migration " + version, e);

            // Mark as failed
            try {
                DatabaseMigration migration = migrationFacade.findByVersion(version);
                if (migration != null) {
                    migration.setStatus(MigrationStatus.FAILED);
                    migration.setErrorMessage(e.getMessage());
                    migration.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    migration.setExecutionLog(logBuilder.toString());
                    migrationFacade.edit(migration);
                }
            } catch (Exception dbError) {
                LOGGER.log(Level.SEVERE, "Error updating migration status", dbError);
            }

            logBuilder.append("ERROR: ").append(e.getMessage()).append("\\n");
            return false;
        }
    }

    /**
     * Execute SQL script
     */
    private void executeSqlScript(String sql, StringBuilder logBuilder) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                // Split SQL into individual statements (simple approach)
                String[] statements = sql.split(";");

                for (String stmt : statements) {
                    String trimmedStmt = stmt.trim();
                    if (!trimmedStmt.isEmpty() && !trimmedStmt.startsWith("--")) {
                        logBuilder.append("Executing: ").append(trimmedStmt.substring(0, Math.min(100, trimmedStmt.length()))).append("...\\n");
                        statement.execute(trimmedStmt);
                    }
                }

                connection.commit();
                logBuilder.append("All statements executed successfully\\n");

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Rollback a specific migration
     */
    public void rollbackMigration(String version) {
        if (!migrationLock.compareAndSet(false, true)) {
            JsfUtil.addErrorMessage("Migration is already in progress");
            return;
        }

        try {
            DatabaseMigration migration = migrationFacade.findByVersion(version);
            if (migration == null || !migration.isSuccessful()) {
                JsfUtil.addErrorMessage("Cannot rollback migration that hasn't been successfully executed");
                return;
            }

            String rollbackSql = discoveryService.loadRollbackSql(version);
            if (rollbackSql == null || rollbackSql.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Rollback script not found for version: " + version);
                return;
            }

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("Rolling back migration ").append(version).append("\\n");

            try {
                executeSqlScript(rollbackSql, logBuilder);

                migration.setStatus(MigrationStatus.ROLLED_BACK);
                migration.setRollbackAt(new Date());
                migration.setRollbackBy(sessionController.getLoggedUser());
                migrationFacade.edit(migration);

                JsfUtil.addSuccessMessage("Migration " + version + " rolled back successfully");

            } catch (SQLException e) {
                migration.setStatus(MigrationStatus.ROLLBACK_FAILED);
                migration.setErrorMessage("Rollback failed: " + e.getMessage());
                migrationFacade.edit(migration);
                throw e;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error rolling back migration " + version, e);
            JsfUtil.addErrorMessage("Rollback error: " + e.getMessage());
        } finally {
            migrationLock.set(false);
            refreshMigrationLists();
        }
    }

    /**
     * Check if a migration has been executed successfully
     */
    public boolean isMigrationExecuted(String version) {
        return migrationFacade.isMigrationExecuted(version);
    }

    /**
     * Get last executed migration version
     */
    public String getLastExecutedVersion() {
        return migrationFacade.getLatestExecutedVersion();
    }

    /**
     * Update the last migration version in config
     */
    private void updateLastMigrationVersion() {
        String latestVersion = migrationFacade.getLatestExecutedVersion();
        if (latestVersion != null) {
            configController.setLongTextValueByKey("Database Schema Version", latestVersion);
        }
    }

    /**
     * Get application version (simplified - could be read from manifest)
     */
    public String getApplicationVersion() {
        return discoveryService.getLatestMigrationVersion();
    }

    /**
     * Compare two versions
     */
    private int compareVersions(String version1, String version2) {
        // Simple implementation - could be enhanced
        return version1.compareTo(version2);
    }

    /**
     * Serialize migration info to JSON string (simplified)
     */
    private String serializeMigrationInfo(MigrationInfo info) {
        try {
            return info.toString(); // Simplified - could use proper JSON serialization
        } catch (Exception e) {
            return "Serialization error: " + e.getMessage();
        }
    }

    /**
     * Get migration statistics
     */
    public Map<String, Long> getMigrationStatistics() {
        return migrationFacade.getMigrationStatistics();
    }

    // Getters and Setters
    public List<MigrationInfo> getAvailableMigrations() {
        return availableMigrations;
    }

    public List<DatabaseMigration> getExecutedMigrations() {
        return executedMigrations;
    }

    public MigrationInfo getSelectedMigration() {
        return selectedMigration;
    }

    public void setSelectedMigration(MigrationInfo selectedMigration) {
        this.selectedMigration = selectedMigration;
    }

    public String getMigrationLog() {
        return migrationLog;
    }

    public boolean isMigrationInProgress() {
        return migrationInProgress;
    }

    public String getCurrentMigrationVersion() {
        return currentMigrationVersion;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public int getProgressPercentage() {
        if (totalSteps == 0) return 0;
        return (completedSteps * 100) / totalSteps;
    }

    public boolean getHasPendingMigrations() {
        return !getPendingMigrations().isEmpty();
    }

    public int getPendingMigrationsCount() {
        return getPendingMigrations().size();
    }
}
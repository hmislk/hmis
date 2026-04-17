/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.DatabaseMigration;
import com.divudi.core.data.MigrationStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import org.eclipse.persistence.internal.jpa.EntityManagerImpl;
import org.eclipse.persistence.sessions.JNDIConnector;
import org.eclipse.persistence.sessions.server.ServerSession;

/**
 * Facade for DatabaseMigration entity operations
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Stateless
public class DatabaseMigrationFacade extends AbstractFacade<DatabaseMigration> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public DatabaseMigrationFacade() {
        super(DatabaseMigration.class);
    }

    /**
     * Execute a DDL statement (CREATE TABLE, ALTER TABLE, SET, etc.) outside JTA.
     *
     * DDL statements cause MySQL to issue an implicit COMMIT, which
     * desynchronises the JTA transaction manager and causes "Transaction
     * aborted". This method obtains a raw JDBC connection directly from
     * EclipseLink's datasource — bypassing JTA entirely — and sets
     * autoCommit=true so MySQL's implicit commits are harmless.
     *
     * Must NOT be called inside an active JTA transaction; the
     * NOT_SUPPORTED attribute suspends any surrounding transaction.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void executeDdlNative(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty");
        }
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } finally {
            // Restore autoCommit=false before returning connection to Payara's JTA pool.
            // Without this, the pool hands the connection (with autoCommit=true) to the
            // next JTA operation, which breaks JTA enlistment and causes
            // java.lang.reflect.UndeclaredThrowableException wrapped in SQLException.
            try { conn.setAutoCommit(false); } catch (Exception ignored) { }
            conn.close();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrationFacade.class.getName());

    /**
     * Obtain a raw JDBC connection from EclipseLink's JNDI datasource,
     * completely outside JTA. Caller is responsible for closing it.
     */
    private Connection getRawJdbcConnection() throws Exception {
        EntityManagerImpl emImpl = em.unwrap(EntityManagerImpl.class);
        ServerSession serverSession = emImpl.getServerSession();
        DataSource ds = ((JNDIConnector) serverSession.getLogin().getConnector()).getDataSource();
        return ds.getConnection();
    }

    /**
     * Scan every table in the current schema for a BIGINT column named ID that
     * lacks AUTO_INCREMENT and apply it.  Runs outside JTA so DDL implicit
     * COMMITs cannot desync the transaction manager.
     *
     * Safe to call at every startup: tables that already have AUTO_INCREMENT are
     * skipped by the information_schema query.  FK checks are disabled for the
     * duration so child-table ALTER statements do not fail.
     *
     * @return list of table names that were altered (empty when nothing needed)
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> applyAutoIncrementToAllEntityTables() throws Exception {
        List<String> altered = new ArrayList<>();
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
            // Find all BIGINT ID columns that do NOT yet have auto_increment
            String query = "SELECT TABLE_NAME FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND COLUMN_NAME = 'ID' "
                    + "AND DATA_TYPE = 'bigint' "
                    + "AND EXTRA NOT LIKE '%auto_increment%' "
                    + "ORDER BY TABLE_NAME";
            List<String> tables = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            if (tables.isEmpty()) {
                return altered;
            }
            // Disable FK checks so child-table ALTERs succeed
            try (Statement fkOff = conn.createStatement()) {
                fkOff.execute("SET FOREIGN_KEY_CHECKS=0");
            }
            try {
                for (String tableName : tables) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN ID BIGINT NOT NULL AUTO_INCREMENT");
                        altered.add(tableName);
                        LOGGER.info("AutoIncrement: applied to table `" + tableName + "`");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "AutoIncrement: could not alter `" + tableName + "` — skipping", e);
                    }
                }
            } finally {
                try (Statement fkOn = conn.createStatement()) {
                    fkOn.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            }
        } finally {
            // Restore autoCommit=false before returning connection to Payara's JTA pool.
            // Without this, the pool hands the connection (with autoCommit=true) to the
            // next JTA operation, which breaks JTA enlistment and causes
            // java.lang.reflect.UndeclaredThrowableException wrapped in SQLException.
            try { conn.setAutoCommit(false); } catch (Exception ignored) { }
            conn.close();
        }
        return altered;
    }

    /**
     * Find migration by version
     */
    public DatabaseMigration findByVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }

        String jpql = "SELECT m FROM DatabaseMigration m WHERE m.version = :version";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("version", version.trim());

        return findFirstByJpql(jpql, parameters);
    }

    /**
     * Check if migration exists for given version
     */
    public boolean isMigrationExecuted(String version) {
        DatabaseMigration migration = findByVersion(version);
        return migration != null && migration.isSuccessful();
    }

    /**
     * Get all executed migrations ordered by version
     */
    public List<DatabaseMigration> findExecutedMigrations() {
        String jpql = "SELECT m FROM DatabaseMigration m WHERE m.status = :status ORDER BY m.version";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", MigrationStatus.SUCCESS);

        return findByJpql(jpql, parameters);
    }

    /**
     * Get all migrations ordered by version
     */
    public List<DatabaseMigration> findAllMigrationsOrderedByVersion() {
        String jpql = "SELECT m FROM DatabaseMigration m ORDER BY m.version";
        return findByJpql(jpql);
    }

    /**
     * Get failed migrations
     */
    public List<DatabaseMigration> findFailedMigrations() {
        String jpql = "SELECT m FROM DatabaseMigration m WHERE m.status = :status ORDER BY m.executedAt DESC";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", MigrationStatus.FAILED);

        return findByJpql(jpql, parameters);
    }

    /**
     * Get currently executing migrations
     */
    public List<DatabaseMigration> findExecutingMigrations() {
        String jpql = "SELECT m FROM DatabaseMigration m WHERE m.status = :status ORDER BY m.executedAt";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", MigrationStatus.EXECUTING);

        return findByJpql(jpql, parameters);
    }

    /**
     * Get latest executed migration version
     * Uses semantic versioning comparison instead of string-based sorting
     */
    public String getLatestExecutedVersion() {
        String jpql = "SELECT m.version FROM DatabaseMigration m WHERE m.status = :status";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", MigrationStatus.SUCCESS);

        List<String> versions = findStringListByJpql(jpql, parameters);
        if (versions.isEmpty()) {
            return null;
        }

        // Sort versions using semantic versioning comparison
        versions.sort((v1, v2) -> compareVersions(v2, v1)); // Descending order (latest first)
        return versions.get(0);
    }

    /**
     * Compare two version strings using semantic versioning
     * Returns -1 if version1 < version2, 0 if equal, 1 if version1 > version2
     */
    private int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) return 0;
        if (version1 == null) return -1;
        if (version2 == null) return 1;

        // Remove 'v' prefix if present
        String v1 = version1.startsWith("v") ? version1.substring(1) : version1;
        String v2 = version2.startsWith("v") ? version2.substring(1) : version2;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            String part1 = i < parts1.length ? parts1[i] : "0";
            String part2 = i < parts2.length ? parts2[i] : "0";

            try {
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);

                if (num1 < num2) return -1;
                if (num1 > num2) return 1;
            } catch (NumberFormatException e) {
                // Fall back to string comparison for non-numeric segments
                int stringCompare = part1.compareTo(part2);
                if (stringCompare != 0) return stringCompare;
            }
        }

        return 0;
    }

    /**
     * Get migration statistics
     */
    public Map<String, Long> getMigrationStatistics() {
        Map<String, Long> stats = new HashMap<>();

        // Total migrations
        String totalJpql = "SELECT COUNT(m) FROM DatabaseMigration m";
        Long total = findLongByJpql(totalJpql);
        stats.put("total", total != null ? total : 0L);

        // Successful migrations
        String successJpql = "SELECT COUNT(m) FROM DatabaseMigration m WHERE m.status = :status";
        Map<String, Object> successParams = new HashMap<>();
        successParams.put("status", MigrationStatus.SUCCESS);
        Long successful = findLongByJpql(successJpql, successParams);
        stats.put("successful", successful != null ? successful : 0L);

        // Failed migrations
        Map<String, Object> failedParams = new HashMap<>();
        failedParams.put("status", MigrationStatus.FAILED);
        Long failed = findLongByJpql(successJpql, failedParams);
        stats.put("failed", failed != null ? failed : 0L);

        // Pending migrations
        Map<String, Object> pendingParams = new HashMap<>();
        pendingParams.put("status", MigrationStatus.PENDING);
        Long pending = findLongByJpql(successJpql, pendingParams);
        stats.put("pending", pending != null ? pending : 0L);

        return stats;
    }

    /**
     * Get recent migrations (last 10)
     */
    public List<DatabaseMigration> findRecentMigrations() {
        String jpql = "SELECT m FROM DatabaseMigration m ORDER BY m.executedAt DESC";
        return findByJpql(jpql, 10);
    }

    /**
     * Check if any migration is currently executing
     */
    public boolean isMigrationInProgress() {
        String jpql = "SELECT COUNT(m) FROM DatabaseMigration m WHERE m.status = :status";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", MigrationStatus.EXECUTING);

        Long count = findLongByJpql(jpql, parameters);
        return count != null && count > 0;
    }

    /**
     * Find migrations between versions (exclusive of fromVersion, inclusive of toVersion)
     */
    public List<DatabaseMigration> findMigrationsBetweenVersions(String fromVersion, String toVersion) {
        if (fromVersion == null || toVersion == null) {
            return findAllMigrationsOrderedByVersion();
        }

        String jpql = "SELECT m FROM DatabaseMigration m WHERE m.version > :fromVersion AND m.version <= :toVersion ORDER BY m.version";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fromVersion", fromVersion);
        parameters.put("toVersion", toVersion);

        return findByJpql(jpql, parameters);
    }

    /**
     * Update migration status
     */
    public void updateMigrationStatus(String version, MigrationStatus status, String errorMessage, Long executionTimeMs) {
        DatabaseMigration migration = findByVersion(version);
        if (migration != null) {
            migration.setStatus(status);
            if (errorMessage != null) {
                migration.setErrorMessage(errorMessage);
            }
            if (executionTimeMs != null) {
                migration.setExecutionTimeMs(executionTimeMs);
            }
            edit(migration);
        }
    }

    /**
     * Create or update migration record
     */
    public DatabaseMigration createOrUpdateMigration(String version, String description, String filename) {
        DatabaseMigration existing = findByVersion(version);
        if (existing != null) {
            existing.setDescription(description);
            existing.setFilename(filename);
            edit(existing);
            return existing;
        } else {
            DatabaseMigration newMigration = new DatabaseMigration(version, description, filename);
            create(newMigration);
            return newMigration;
        }
    }

    /**
     * Find migrations that need to be executed (not yet successful)
     */
    public List<DatabaseMigration> findPendingMigrations() {
        String jpql = "SELECT m FROM DatabaseMigration m WHERE m.status != :successStatus ORDER BY m.version";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("successStatus", MigrationStatus.SUCCESS);

        return findByJpql(jpql, parameters);
    }
}
/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
 * Facade for audit database operations including schema management.
 * This facade connects to the audit database for database administration tasks.
 *
 * @author buddhika
 */
@Stateless
public class AuditDatabaseFacade extends AbstractFacade<Item> {

    private static final Logger LOGGER = Logger.getLogger(AuditDatabaseFacade.class.getName());

    @PersistenceContext(unitName = "hmisAuditPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}
        return em;
    }

    public AuditDatabaseFacade() {
        super(Item.class);
    }

    /**
     * Execute a DDL statement on the audit database outside JTA.
     *
     * DDL statements cause MySQL to issue an implicit COMMIT, which
     * desynchronises the JTA transaction manager. This method obtains a raw
     * JDBC connection directly from EclipseLink's datasource — bypassing JTA
     * entirely — and sets autoCommit=true so MySQL's implicit commits are
     * harmless.
     *
     * Must NOT be called inside an active JTA transaction; the NOT_SUPPORTED
     * attribute suspends any surrounding transaction.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void executeDdlNative(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty");
        }
        if (em == null) {
            throw new IllegalStateException("Audit database EntityManager is not available");
        }
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } finally {
            try { conn.setAutoCommit(false); } catch (Exception ignored) { }
            conn.close();
        }
    }

    /**
     * Scan every table in the audit database schema for a BIGINT ID primary
     * key that lacks AUTO_INCREMENT and apply it, preserving each column's
     * exact type. See DatabaseMigrationFacade.applyAutoIncrementToAllEntityTables()
     * for the full rationale — this is the identical fix applied to the audit
     * persistence unit (hmisAuditPU) instead of the main one.
     *
     * @return list of table names that were altered (empty when nothing needed)
     * @throws Exception (specifically SQLException) if any detected table still
     *         lacks AUTO_INCREMENT after the fix loop.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> applyAutoIncrementToAllEntityTables() throws Exception {
        List<String> altered = new ArrayList<>();
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
            String detectQuery = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND UPPER(COLUMN_NAME) = 'ID' "
                    + "AND COLUMN_KEY = 'PRI' "
                    + "AND DATA_TYPE = 'bigint' "
                    + "AND EXTRA NOT LIKE '%auto_increment%' "
                    + "ORDER BY TABLE_NAME";

            List<String[]> rows = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(detectQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
                }
            }
            if (rows.isEmpty()) {
                return altered;
            }

            String originalSqlMode;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@SESSION.sql_mode")) {
                originalSqlMode = rs.next() ? rs.getString(1) : "";
            }
            try (Statement relaxMode = conn.createStatement()) {
                relaxMode.execute("SET SESSION sql_mode = ''");
            }
            try (Statement fkOff = conn.createStatement()) {
                fkOff.execute("SET FOREIGN_KEY_CHECKS=0");
            }
            try {
                for (String[] row : rows) {
                    String tableName = row[0];
                    String columnName = row[1];
                    String columnType = row[2];
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + columnName
                                + "` " + columnType + " NOT NULL AUTO_INCREMENT");
                        altered.add(tableName);
                        LOGGER.info("AuditDB AutoIncrement: applied to table `" + tableName + "`");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "AuditDB AutoIncrement: could not alter `" + tableName + "` — skipping", e);
                    }
                }
            } finally {
                try (Statement fkOn = conn.createStatement()) {
                    fkOn.execute("SET FOREIGN_KEY_CHECKS=1");
                }
                try (PreparedStatement restoreMode = conn.prepareStatement("SET SESSION sql_mode = ?")) {
                    restoreMode.setString(1, originalSqlMode);
                    restoreMode.execute();
                }
            }

            List<String> remaining = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(detectQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    remaining.add(rs.getString(1));
                }
            }
            if (!remaining.isEmpty()) {
                throw new SQLException("AUTO_INCREMENT could not be applied to: " + String.join(", ", remaining));
            }
        } finally {
            try { conn.setAutoCommit(false); } catch (Exception ignored) { }
            conn.close();
        }
        return altered;
    }

    private Connection getRawJdbcConnection() throws Exception {
        EntityManagerImpl emImpl = em.unwrap(EntityManagerImpl.class);
        ServerSession serverSession = emImpl.getServerSession();
        DataSource ds = ((JNDIConnector) serverSession.getLogin().getConnector()).getDataSource();
        return ds.getConnection();
    }
}

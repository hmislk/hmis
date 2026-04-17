/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> applyAutoIncrementToAllEntityTables() throws Exception {
        List<String> altered = new ArrayList<>();
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
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
            try (Statement fkOff = conn.createStatement()) {
                fkOff.execute("SET FOREIGN_KEY_CHECKS=0");
            }
            try {
                for (String tableName : tables) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN ID BIGINT NOT NULL AUTO_INCREMENT");
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

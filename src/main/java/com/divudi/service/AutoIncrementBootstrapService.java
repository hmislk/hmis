/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.eclipse.persistence.internal.jpa.EntityManagerImpl;

/**
 * Ensures all entity table ID columns have AUTO_INCREMENT at application
 * startup.
 *
 * Background: switching from GenerationType.AUTO (shared SEQUENCE table) to
 * GenerationType.IDENTITY (per-table AUTO_INCREMENT) requires every entity
 * table's ID column to have AUTO_INCREMENT set. If the application is deployed
 * with the new GenerationType.IDENTITY code before the DB migration has run,
 * every INSERT fails with "Field 'ID' doesn't have a default value" (MySQL
 * 1364).
 *
 * This @Singleton @Startup EJB runs once when Payara starts, before any user
 * can interact, and self-heals the database. It uses a raw JDBC connection
 * outside JTA so that DDL implicit-commit behaviour cannot abort a JTA
 * transaction. MySQL automatically sets AUTO_INCREMENT = MAX(ID) + 1 when
 * adding AUTO_INCREMENT to an existing column, so no existing data is affected.
 *
 * The migration page (v2.2.0) performs the same work; if this service has
 * already run, the v2.2.0 cursor finds no tables to alter and completes
 * immediately as a no-op.
 *
 * @author Dr M H B Ariyaratne
 */
@Singleton
@Startup
public class AutoIncrementBootstrapService {

    private static final Logger LOGGER = Logger.getLogger(AutoIncrementBootstrapService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void applyAutoIncrementIfNeeded() {
        try {
            List<String> altered = applyToAllEntityTables();
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

    /**
     * Finds every bigint ID column without AUTO_INCREMENT in the current
     * database and alters it. Works with both uppercase and lowercase table
     * names (uses information_schema, does not hardcode table names).
     *
     * @return list of table names that were altered
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> applyToAllEntityTables() throws Exception {
        List<String> altered = new ArrayList<>();
        EntityManagerImpl emImpl = em.unwrap(EntityManagerImpl.class);
        Connection conn = emImpl.getServerSession().getDatasource().getConnection();
        try {
            conn.setAutoCommit(true);

            // Find all bigint ID columns without AUTO_INCREMENT in current DB
            String query =
                    "SELECT TABLE_NAME FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "  AND COLUMN_NAME = 'ID' " +
                    "  AND DATA_TYPE = 'bigint' " +
                    "  AND EXTRA NOT LIKE '%auto_increment%' " +
                    "ORDER BY TABLE_NAME";

            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }

                for (String tableName : tables) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(
                                "ALTER TABLE `" + tableName +
                                "` MODIFY COLUMN ID BIGINT NOT NULL AUTO_INCREMENT");
                        altered.add(tableName);
                        LOGGER.fine("AutoIncrementBootstrap: altered `" + tableName + "`");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "AutoIncrementBootstrap: could not alter `" + tableName + "` — skipping", e);
                    }
                }
            }
        } finally {
            conn.close();
        }
        return altered;
    }
}

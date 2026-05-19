/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Transaction-bounded executor for a single archival batch.
 *
 * Each invocation of {@link #copyAndDelete} runs in its own transaction
 * (REQUIRES_NEW) so the row-locks acquired by the INSERT and DELETE are held
 * only for the duration of one batch, not the entire archival run. This is
 * what makes the archival job safe to run concurrently with normal pharmacy
 * write traffic against STOCKHISTORY.
 *
 * Issue #20726.
 */
@Stateless
public class ArchivalBatchTx {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    /**
     * Copy the rows identified by {@code ids} from {@code sourceTable} to
     * {@code archiveTable}, then delete them from {@code sourceTable}. All
     * within one new transaction.
     *
     * Table/column names come from concrete archival services (compile-time
     * constants), never from user input, so the direct string concatenation
     * is safe from injection.
     *
     * @param archiveTable   archive table name (e.g. STOCKHISTORYARCHIVE)
     * @param sourceTable    source table name (e.g. STOCKHISTORY)
     * @param columnList     comma-separated column list common to both tables,
     *                       excluding ARCHIVEDAT (which is set to NOW() here)
     * @param ids            primary-key IDs to move; must be non-null and non-empty
     * @return number of rows deleted from the source
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int copyAndDelete(String archiveTable, String sourceTable,
                             String columnList, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String insertSql = "INSERT INTO " + archiveTable + " (" + columnList + ", ARCHIVEDAT) "
                + "SELECT " + columnList + ", NOW() FROM " + sourceTable
                + " WHERE ID IN (:ids)";
        Query insertQ = em.createNativeQuery(insertSql);
        insertQ.setParameter("ids", ids);
        insertQ.executeUpdate();

        String deleteSql = "DELETE FROM " + sourceTable + " WHERE ID IN (:ids)";
        Query deleteQ = em.createNativeQuery(deleteSql);
        deleteQ.setParameter("ids", ids);
        return deleteQ.executeUpdate();
    }
}

/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private static final Logger LOGGER = Logger.getLogger(ArchivalBatchTx.class.getName());

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_SLEEP_MS = 2000;

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    /**
     * Copy the rows identified by {@code ids} from {@code sourceTable} to
     * {@code archiveTable}, then delete them from {@code sourceTable}. All
     * within one new transaction.
     *
     * Retries up to {@value #MAX_RETRIES} times on MySQL lock wait timeout
     * (error 1205) — the competing transaction will have committed by then.
     * Each retry opens a fresh REQUIRES_NEW transaction so the previous
     * rolled-back state is fully discarded before we attempt again.
     *
     * Table/column names come from concrete archival services (compile-time
     * constants), never from user input, so the direct string concatenation
     * is safe from injection.
     */
    public int copyAndDelete(String archiveTable, String sourceTable,
                             String columnList, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        PersistenceException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return doOneBatch(archiveTable, sourceTable, columnList, ids);
            } catch (PersistenceException ex) {
                if (!isLockTimeout(ex)) {
                    throw ex;
                }
                lastEx = ex;
                LOGGER.log(Level.WARNING,
                        "Archival batch lock timeout (attempt {0}/{1}), retrying in {2}ms",
                        new Object[]{attempt, MAX_RETRIES, RETRY_SLEEP_MS});
                try { Thread.sleep(RETRY_SLEEP_MS); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw lastEx;
    }

    /** Single attempt — runs in its own REQUIRES_NEW transaction. */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int doOneBatch(String archiveTable, String sourceTable,
                          String columnList, List<Long> ids) {
        // JPA native queries do not support binding a List to a named IN parameter;
        // expand to individual positional placeholders (?,?,?,...) instead.
        String placeholders = IntStream.range(0, ids.size())
                .mapToObj(i -> "?")
                .collect(Collectors.joining(","));

        String insertSql = "INSERT INTO " + archiveTable + " (" + columnList + ", ARCHIVEDAT) "
                + "SELECT " + columnList + ", NOW() FROM " + sourceTable
                + " WHERE ID IN (" + placeholders + ")";
        Query insertQ = em.createNativeQuery(insertSql);
        for (int i = 0; i < ids.size(); i++) {
            insertQ.setParameter(i + 1, ids.get(i));
        }
        insertQ.executeUpdate();

        String deleteSql = "DELETE FROM " + sourceTable + " WHERE ID IN (" + placeholders + ")";
        Query deleteQ = em.createNativeQuery(deleteSql);
        for (int i = 0; i < ids.size(); i++) {
            deleteQ.setParameter(i + 1, ids.get(i));
        }
        return deleteQ.executeUpdate();
    }

    private static boolean isLockTimeout(PersistenceException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            String msg = cause.getMessage();
            // MySQL error 1205: Lock wait timeout exceeded
            if (msg != null && msg.contains("Lock wait timeout exceeded")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

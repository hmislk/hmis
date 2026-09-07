/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import java.sql.SQLException;
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
import javax.ejb.EJB;

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

    // Self-injection so doOneBatch() is invoked through the EJB proxy —
    // mandatory for @TransactionAttribute(REQUIRES_NEW) to take effect.
    // Calling this.doOneBatch() bypasses the proxy and ignores the annotation.
    @EJB
    private ArchivalBatchTx self;

    /**
     * Copy rows from {@code sourceTable} to {@code archiveTable} then delete
     * them from {@code sourceTable}. Retries up to {@value #MAX_RETRIES} times
     * on MySQL lock wait timeout (error 1205) before giving up. Each retry is a
     * completely fresh REQUIRES_NEW transaction via the EJB proxy (self).
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int copyAndDelete(String archiveTable, String sourceTable,
                             String columnList, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        PersistenceException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return self.doOneBatch(archiveTable, sourceTable, columnList, ids);
            } catch (PersistenceException ex) {
                if (!isLockTimeout(ex)) {
                    throw ex;
                }
                lastEx = ex;
                if (attempt < MAX_RETRIES) {
                    LOGGER.log(Level.WARNING,
                            "Archival batch lock timeout (attempt {0}/{1}), retrying in {2}ms",
                            new Object[]{attempt, MAX_RETRIES, RETRY_SLEEP_MS});
                    try {
                        Thread.sleep(RETRY_SLEEP_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }
        throw lastEx;
    }

    /** Single transactional attempt — called via self proxy to honour REQUIRES_NEW. */
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
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SQLException && ((SQLException) cause).getErrorCode() == 1205) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

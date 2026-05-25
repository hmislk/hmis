/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import com.divudi.core.data.dto.ArchiveResult;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

/**
 * Reusable batched-archival skeleton.
 *
 * Concrete subclasses provide:
 * <ul>
 *   <li>the source and archive table names</li>
 *   <li>a JPA entity name to count and id-scan against</li>
 *   <li>a cutoff predicate (default: {@code e.createdAt < :cutoff})</li>
 *   <li>the EntityManager and the {@link ArchivalBatchTx} executor</li>
 * </ul>
 *
 * The base orchestrates: count → scan IDs → copyAndDelete per batch → repeat
 * until either no more candidate rows remain or {@code maxBatches} is hit.
 * Each batch's INSERT+DELETE runs in its own transaction so concurrent
 * writers to the source table are not blocked across the full run.
 *
 * Designed to be reused for #20724 (ItemBatch archive) by overriding the
 * cutoff predicate / id-scan method.
 *
 * Issue #20726.
 */
public abstract class ArchivalServiceBase {

    /**
     * Returns true when {@code ex} (or any cause in its chain) is a MySQL
     * lock wait timeout (error 1205). Used by subclass retry logic.
     */
    protected static boolean isLockTimeout(PersistenceException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SQLException && ((SQLException) cause).getErrorCode() == 1205) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    protected abstract EntityManager em();

    protected abstract ArchivalBatchTx batchTx();

    /** Physical name of the archive table, e.g. {@code STOCKHISTORYARCHIVE}. */
    protected abstract String archiveTable();

    /** Physical name of the source table, e.g. {@code STOCKHISTORY}. */
    protected abstract String sourceTable();

    /** JPA entity name used in JPQL, e.g. {@code StockHistory}. */
    protected abstract String sourceEntityName();

    /**
     * JPQL predicate applied to the source entity (alias {@code e}) to identify
     * archivable rows. The default targets a {@code createdAt} field. Override
     * for more complex eligibility checks (e.g. ItemBatch expired + zero-stock).
     * The implementation must use the named parameter {@code :cutoff} typed
     * as a {@link Date}.
     */
    protected String cutoffWhereJpql() {
        return "e.createdAt < :cutoff";
    }

    /**
     * Count of rows eligible for archival at the given cutoff. Default uses
     * {@link #cutoffWhereJpql()}; override if the cutoff needs custom params.
     */
    public long countOlderThan(Date cutoff) {
        String jpql = "SELECT COUNT(e) FROM " + sourceEntityName() + " e WHERE " + cutoffWhereJpql();
        Query q = em().createQuery(jpql);
        q.setParameter("cutoff", cutoff);
        Object result = q.getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    /**
     * Fetch the next batch of source IDs to archive. Default uses
     * {@link #cutoffWhereJpql()} ordered by id ascending. Override for
     * non-trivial selection criteria.
     */
    @SuppressWarnings("unchecked")
    protected List<Long> fetchNextBatchIds(Date cutoff, int batchSize) {
        String jpql = "SELECT e.id FROM " + sourceEntityName() + " e WHERE "
                + cutoffWhereJpql() + " ORDER BY e.id";
        Query q = em().createQuery(jpql);
        q.setParameter("cutoff", cutoff);
        q.setMaxResults(batchSize);
        return q.getResultList();
    }

    /**
     * Resolve the list of column names common to both source and archive
     * tables (excluding {@code ARCHIVEDAT}, which the batch executor sets
     * to {@code NOW()}). Uses an INFORMATION_SCHEMA intersection so any
     * future schema drift between the two tables — a column added to one
     * but not the other — yields a safe subset rather than a runtime
     * "column unknown" failure inside the INSERT…SELECT batch.
     */
    @SuppressWarnings("unchecked")
    protected List<String> resolveCommonColumns() {
        String sql =
                "SELECT a.COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS a "
              + "JOIN INFORMATION_SCHEMA.COLUMNS s "
              + "  ON s.TABLE_SCHEMA = a.TABLE_SCHEMA "
              + " AND UPPER(s.TABLE_NAME) = UPPER(?) "
              + " AND UPPER(s.COLUMN_NAME) = UPPER(a.COLUMN_NAME) "
              + "WHERE a.TABLE_SCHEMA = DATABASE() "
              + "  AND UPPER(a.TABLE_NAME) = UPPER(?) "
              + "  AND UPPER(a.COLUMN_NAME) <> 'ARCHIVEDAT' "
              + "ORDER BY a.ORDINAL_POSITION";
        Query q = em().createNativeQuery(sql);
        q.setParameter(1, sourceTable());
        q.setParameter(2, archiveTable());
        List<?> raw = q.getResultList();
        List<String> cols = new ArrayList<>(raw.size());
        for (Object o : raw) {
            cols.add(o.toString());
        }
        return cols;
    }

    /**
     * Run an archival pass.
     */
    public ArchiveResult archive(Date cutoff, int batchSize, int maxBatches, boolean dryRun) {
        return archive(cutoff, batchSize, maxBatches, dryRun, null);
    }

    /**
     * Run an archival pass, invoking {@code onBatchMoved} with the row count
     * after each committed batch so callers can track progress.
     *
     * @param onBatchMoved called with the number of rows moved after each batch;
     *                     may be null if progress tracking is not needed
     */
    public ArchiveResult archive(Date cutoff, int batchSize, int maxBatches, boolean dryRun,
                                  java.util.function.IntConsumer onBatchMoved) {
        Date startedAt = new Date();
        long candidates = countOlderThan(cutoff);

        if (dryRun || candidates == 0) {
            String msg = dryRun
                    ? "Dry run: " + candidates + " row(s) would be archived"
                    : "No rows older than cutoff";
            return new ArchiveResult(dryRun, candidates, 0L, 0, false,
                    startedAt, new Date(), msg);
        }

        String columnList = String.join(", ", resolveCommonColumns());
        if (columnList.isEmpty()) {
            return new ArchiveResult(false, candidates, 0L, 0, false,
                    startedAt, new Date(),
                    "Aborted: could not resolve column list from " + archiveTable());
        }

        long totalArchived = 0;
        int batchesRun = 0;
        boolean reachedLimit = false;

        for (int b = 0; b < maxBatches; b++) {
            List<Long> ids = fetchNextBatchIds(cutoff, batchSize);
            if (ids.isEmpty()) {
                break;
            }
            int moved = batchTx().copyAndDelete(archiveTable(), sourceTable(), columnList, ids);
            totalArchived += moved;
            batchesRun++;
            if (onBatchMoved != null) {
                onBatchMoved.accept(moved);
            }
            if (ids.size() < batchSize) {
                break;
            }
            if (b == maxBatches - 1) {
                reachedLimit = true;
            }
        }

        String msg = "Archived " + totalArchived + " row(s) across " + batchesRun + " batch(es)"
                + (reachedLimit ? " (batch limit reached; more rows remain)" : "");
        return new ArchiveResult(false, candidates, totalArchived, batchesRun,
                reachedLimit, startedAt, new Date(), msg);
    }
}

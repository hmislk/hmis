/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import com.divudi.core.data.dto.ArchiveResult;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

/**
 * Archives expired zero-stock {@code ItemBatch} rows (and their linked
 * {@code Stock} rows) to {@code ITEMBATCHARCHIVE} / {@code STOCKARCHIVE}.
 *
 * Eligibility (all must be true):
 * <ul>
 *   <li>ItemBatch.dateOfExpire is strictly before the configured cutoff
 *       (default: 1 year ago)</li>
 *   <li>No linked Stock row has stock &gt; 0 across any department</li>
 * </ul>
 *
 * Before each ItemBatch row is deleted, all PharmaceuticalBillItem rows that
 * reference it have their {@code ARCHIVEDITEMBATCH_ID} set and
 * {@code ITEMBATCH_ID} nullified — so bill viewing can fall back to the
 * archive via {@code pbi.getArchivedItemBatch()}.
 *
 * Top-level methods run with NOT_SUPPORTED so this bean does not open a
 * long-running transaction; each batch runs in its own REQUIRES_NEW
 * transaction inside {@link ItemBatchArchivalBatchTx}.
 *
 * Issue #20724.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ItemBatchArchivalService extends ArchivalServiceBase {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_SLEEP_MS = 2000;

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(ItemBatchArchivalService.class.getName());

    @EJB
    private ArchivalBatchTx batchTx;

    @EJB
    private ItemBatchArchivalBatchTx itemBatchBatchTx;

    @Inject
    private ItemBatchArchivalTracker tracker;

    @Override
    protected EntityManager em() {
        return em;
    }

    @Override
    protected ArchivalBatchTx batchTx() {
        return batchTx;
    }

    @Override
    protected String archiveTable() {
        return "ITEMBATCHARCHIVE";
    }

    @Override
    protected String sourceTable() {
        return "ITEMBATCH";
    }

    @Override
    protected String sourceEntityName() {
        return "ItemBatch";
    }

    /**
     * Eligibility predicate: expired AND no live stock in any department.
     * Uses :cutoff as the expiry cutoff date.
     */
    @Override
    protected String cutoffWhereJpql() {
        return "e.dateOfExpire < :cutoff "
                + "AND NOT EXISTS ("
                + "  SELECT s FROM Stock s WHERE s.itemBatch = e AND s.stock > 0"
                + ")";
    }

    /**
     * Fetch the next batch of eligible ItemBatch IDs.
     * Same predicate as {@link #cutoffWhereJpql()} with ORDER BY id for stable paging.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected List<Long> fetchNextBatchIds(Date cutoff, int batchSize) {
        String jpql = "SELECT e.id FROM ItemBatch e WHERE "
                + cutoffWhereJpql()
                + " ORDER BY e.id";
        Query q = em.createQuery(jpql);
        q.setParameter("cutoff", cutoff);
        q.setMaxResults(batchSize);
        return q.getResultList();
    }

    /**
     * Resolve the column list for Stock rows (common to STOCK and STOCKARCHIVE,
     * excluding ARCHIVEDAT). Delegates to INFORMATION_SCHEMA intersection logic
     * identical to the base class {@link ArchivalServiceBase#resolveCommonColumns()}.
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveStockCommonColumns() {
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
        Query q = em.createNativeQuery(sql);
        q.setParameter(1, "STOCK");
        q.setParameter(2, "STOCKARCHIVE");
        List<?> raw = q.getResultList();
        List<String> cols = new ArrayList<>(raw.size());
        for (Object o : raw) {
            cols.add(o.toString());
        }
        return cols;
    }

    /**
     * Detect the physical table names for ITEMBATCH, ITEMBATCHARCHIVE, STOCK,
     * STOCKARCHIVE, and PHARMACEUTICALBILLITEM via INFORMATION_SCHEMA so this
     * works on both case-sensitive (Linux) and case-insensitive (Windows/Azure)
     * MySQL deployments.
     */
    @SuppressWarnings("unchecked")
    private String resolveTableName(String upperName) {
        List<?> result = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ?")
                .setParameter(1, upperName)
                .getResultList();
        return result.isEmpty() ? upperName : result.get(0).toString();
    }

    @Override
    public ArchiveResult archive(Date cutoff, int batchSize, int maxBatches, boolean dryRun) {
        return archive(cutoff, batchSize, maxBatches, dryRun, null);
    }

    /**
     * ItemBatch-specific archival loop that uses {@link ItemBatchArchivalBatchTx}
     * (handles ItemBatch + Stock tables atomically per batch).
     * Invokes {@code onBatchMoved} after each committed batch for progress tracking.
     */
    @Override
    public ArchiveResult archive(Date cutoff, int batchSize, int maxBatches, boolean dryRun,
                                  java.util.function.IntConsumer onBatchMoved) {
        Date startedAt = new Date();
        long candidates = countOlderThan(cutoff);

        if (dryRun || candidates == 0) {
            String msg = dryRun
                    ? "Dry run: " + candidates + " ItemBatch row(s) would be archived"
                    : "No eligible ItemBatch rows found";
            return new ArchiveResult(dryRun, candidates, 0L, 0, false,
                    startedAt, new Date(), msg);
        }

        String ibTable      = resolveTableName("ITEMBATCH");
        String ibArchive    = resolveTableName("ITEMBATCHARCHIVE");
        String stTable      = resolveTableName("STOCK");
        String stArchive    = resolveTableName("STOCKARCHIVE");
        String pbiTable     = resolveTableName("PHARMACEUTICALBILLITEM");

        String ibCols = String.join(", ", resolveCommonColumns());
        String stCols = String.join(", ", resolveStockCommonColumns());

        if (ibCols.isEmpty()) {
            return new ArchiveResult(false, candidates, 0L, 0, false,
                    startedAt, new Date(),
                    "Aborted: could not resolve column list from " + ibArchive);
        }
        if (stCols.isEmpty()) {
            return new ArchiveResult(false, candidates, 0L, 0, false,
                    startedAt, new Date(),
                    "Aborted: could not resolve column list from " + stArchive);
        }

        long totalArchived = 0;
        int batchesRun = 0;
        boolean reachedLimit = false;

        for (int b = 0; b < maxBatches; b++) {
            List<Long> ids = fetchNextBatchIds(cutoff, batchSize);
            if (ids.isEmpty()) {
                break;
            }
            int moved = archiveBatchWithRetry(
                    ibTable, ibArchive, stTable, stArchive, pbiTable,
                    ibCols, stCols, ids);
            totalArchived += moved;
            batchesRun++;
            if (onBatchMoved != null) {
                onBatchMoved.accept(moved);
            }
            if (ids.size() < batchSize) {
                break;
            }
        }
        if (batchesRun == maxBatches) {
            reachedLimit = !fetchNextBatchIds(cutoff, 1).isEmpty();
        }

        String msg = "Archived " + totalArchived + " ItemBatch row(s) across " + batchesRun + " batch(es)"
                + (reachedLimit ? " (batch limit reached; more rows remain)" : "");
        return new ArchiveResult(false, candidates, totalArchived, batchesRun,
                reachedLimit, startedAt, new Date(), msg);
    }

    /**
     * Calls {@link ItemBatchArchivalBatchTx#archiveBatch} and retries up to
     * {@value #MAX_RETRIES} times on MySQL lock wait timeout (error 1205).
     * Since {@code ItemBatchArchivalService} runs NOT_SUPPORTED, each retry
     * goes through the EJB proxy so {@code REQUIRES_NEW} is honoured afresh.
     */
    private int archiveBatchWithRetry(String ibTable, String ibArchive,
            String stTable, String stArchive, String pbiTable,
            String ibCols, String stCols, List<Long> ids) {
        PersistenceException lastEx = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return itemBatchBatchTx.archiveBatch(
                        ibTable, ibArchive, stTable, stArchive, pbiTable,
                        ibCols, stCols, ids);
            } catch (PersistenceException ex) {
                if (!isLockTimeout(ex)) {
                    throw ex;
                }
                lastEx = ex;
                if (attempt < MAX_RETRIES) {
                    LOGGER.log(Level.WARNING,
                            "ItemBatch archival batch lock timeout (attempt {0}/{1}), retrying in {2}ms",
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

    /**
     * Fire-and-forget async wrapper for live archive runs.
     */
    @Asynchronous
    public void archiveAsync(Date cutoff, int batchSize, int maxBatches) {
        long candidates = countOlderThan(cutoff);
        tracker.start(candidates, maxBatches);
        try {
            ArchiveResult result = archive(cutoff, batchSize, maxBatches, false,
                    moved -> tracker.recordBatch(moved));
            tracker.finish(result);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Async ItemBatch archival failed", ex);
            ArchiveResult err = new ArchiveResult(false, tracker.getCandidates(),
                    tracker.getArchivedSoFar(), tracker.getBatchesDone(), false,
                    tracker.getStartedAt(), new Date(), "Error: " + ex.getMessage());
            tracker.finish(err);
        }
    }
}

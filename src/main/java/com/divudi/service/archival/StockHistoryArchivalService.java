/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import com.divudi.core.data.dto.ArchiveResult;
import java.util.Date;
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

/**
 * Archives old {@code StockHistory} rows to {@code STOCKHISTORYARCHIVE}.
 *
 * Eligible rows are those whose {@code createdAt} timestamp is strictly
 * before the configured cutoff. The default retention is 2 years for all
 * history types (transaction rows and {@code MonthlyRecord} summary rows
 * are treated the same way for v1; can be split later if needed).
 *
 * Each batch is INSERT…SELECT then DELETE inside its own transaction
 * (via {@link ArchivalBatchTx}), so we don't hold row-locks across the
 * full multi-million-row run.
 *
 * Top-level methods run with {@code NOT_SUPPORTED} so this bean itself
 * doesn't open a long-running transaction — only each batch does.
 *
 * Issue #20726.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StockHistoryArchivalService extends ArchivalServiceBase {

    private static final Logger LOGGER = Logger.getLogger(StockHistoryArchivalService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private ArchivalBatchTx batchTx;

    @Inject
    private StockHistoryArchivalTracker tracker;

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
        return "STOCKHISTORYARCHIVE";
    }

    @Override
    protected String sourceTable() {
        return "STOCKHISTORY";
    }

    @Override
    protected String sourceEntityName() {
        return "StockHistory";
    }

    /**
     * Fire-and-forget async wrapper for live archive runs.
     * Progress is reported to {@link StockHistoryArchivalTracker} after each
     * committed batch so the UI poll can display it without session locking.
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
            LOGGER.log(Level.SEVERE, "Async StockHistory archival failed", ex);
            ArchiveResult err = new ArchiveResult(false, tracker.getCandidates(),
                    tracker.getArchivedSoFar(), tracker.getBatchesDone(), false,
                    tracker.getStartedAt(), new Date(), "Error: " + ex.getMessage());
            tracker.finish(err);
        }
    }
}

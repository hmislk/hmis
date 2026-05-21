/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private ArchivalBatchTx batchTx;

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
}

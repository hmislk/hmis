package com.divudi.service;

import com.divudi.core.entity.FinalBillPdfSnapshot;
import com.divudi.core.facade.FinalBillPdfSnapshotFacade;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Persists exactly one {@link FinalBillPdfSnapshot} row in a transaction of
 * its own.
 *
 * <p>
 * Split out from {@link FinalBillPdfSnapshotService#getOrCreateSnapshot} purely
 * so the {@code REQUIRES_NEW} boundary is real: {@code getOrCreateSnapshot} is
 * itself a plain {@code @Stateless} method with the default {@code REQUIRED}
 * attribute, called from a CDI bean ({@code InwardSearch}) with no JTA
 * transaction already active, so it starts and owns the transaction for the
 * whole call. If the persist-and-flush below threw directly from inside that
 * method, the EclipseLink/EJB container would mark that SAME transaction
 * rollback-only the instant {@code em.flush()} raised the
 * {@code BILL_ID} unique-constraint violation - before the
 * {@code catch (RuntimeException e)} block even runs. The catch block's
 * recovery query would then find the real, already-committed row from the
 * concurrent caller that won the race and return its bytes, but the method
 * would still throw {@code EJBTransactionRolledbackException} on return
 * because the (poisoned) transaction is what the outer call is wrapped in -
 * discarding a perfectly valid recovered result.
 * </p>
 *
 * <p>
 * A self-invocation inside {@code FinalBillPdfSnapshotService} (e.g.
 * {@code this.create(snapshot)}) would not fix this: calling through
 * {@code this} bypasses the EJB proxy entirely, so
 * {@code @TransactionAttribute(REQUIRES_NEW)} on a method invoked that way is
 * silently ignored and the call still runs in the caller's transaction. Only
 * a call through this bean's own injected proxy actually opens a new,
 * independent transaction - see {@code InvestigationConversionTx} /
 * {@code ArchivalBatchTx} for the same pattern used elsewhere in this
 * codebase.
 * </p>
 */
@Stateless
public class FinalBillPdfSnapshotWriter {

    @EJB
    private FinalBillPdfSnapshotFacade finalBillPdfSnapshotFacade;

    /**
     * Persists and flushes {@code snapshot} in its own transaction. A
     * {@code BILL_ID} unique-constraint violation here rolls back only this
     * transaction, leaving the caller's (outer) transaction untouched so it
     * can still commit whatever else it has done and run its own recovery
     * query.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void create(FinalBillPdfSnapshot snapshot) {
        finalBillPdfSnapshotFacade.createAndFlush(snapshot);
    }
}

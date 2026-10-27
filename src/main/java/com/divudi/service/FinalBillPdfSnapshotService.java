package com.divudi.service;

import com.divudi.bean.common.PdfController;
import com.divudi.bean.inward.BhtSummeryController;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.FinalBillPdfSnapshot;
import com.divudi.core.facade.FinalBillPdfSnapshotFacade;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class FinalBillPdfSnapshotService {

    @Inject
    private FinalBillPdfSnapshotFacade finalBillPdfSnapshotFacade;

    @Inject
    private PdfController pdfController;

    @Inject
    private BhtSummeryController bhtSummeryController;

    // Injected via its own EJB proxy (not called via `this`) so that
    // @TransactionAttribute(REQUIRES_NEW) on create() actually takes effect -
    // see FinalBillPdfSnapshotWriter's class Javadoc for why the persist step
    // has to live in a separate bean rather than a private helper method here.
    @EJB
    private FinalBillPdfSnapshotWriter finalBillPdfSnapshotWriter;

    public byte[] getOrCreateSnapshot(Bill bill) throws IOException {
        if (bill == null) {
            return null;
        }
        FinalBillPdfSnapshot existing = finalBillPdfSnapshotFacade.findByBillId(bill.getId());
        if (existing != null) {
            return existing.getPdfBytes();
        }

        List<Map.Entry<String, Double>> categoryTotals = bhtSummeryController.foldInwardCategoryTotals(bill);
        List<Bill> paymentBills = bhtSummeryController.getPatientPaymentBillsForFinalBill(bill);
        byte[] pdfBytes = pdfController.createFinalBillSnapshotPdf(bill, categoryTotals, paymentBills);

        FinalBillPdfSnapshot snapshot = new FinalBillPdfSnapshot();
        snapshot.setBill(bill);
        snapshot.setPdfBytes(pdfBytes);
        snapshot.setCreatedAt(new Date());
        try {
            // Routed through the injected finalBillPdfSnapshotWriter proxy
            // (REQUIRES_NEW), not a local call, so a unique-constraint
            // violation here rolls back only that inner transaction. If this
            // were called directly (or via `this.` self-invocation) inside
            // getOrCreateSnapshot's own REQUIRED transaction, the container
            // would mark THIS method's transaction rollback-only the instant
            // the flush failed - before this catch block even runs - and the
            // raceWinner recovery below would compute a correct result only
            // to have it discarded by an EJBTransactionRolledbackException
            // on return.
            finalBillPdfSnapshotWriter.create(snapshot);
        } catch (RuntimeException e) {
            // Another concurrent call (eager generation at approval vs. lazy
            // generation on first view) may have already persisted a snapshot
            // for this bill, tripping the BILL_ID unique constraint. Re-check
            // for that snapshot before giving up on this one. Safe to do in
            // this (still healthy) transaction precisely because the failure
            // above happened in the writer's own, separate transaction.
            FinalBillPdfSnapshot raceWinner = finalBillPdfSnapshotFacade.findByBillId(bill.getId());
            if (raceWinner != null) {
                return raceWinner.getPdfBytes();
            }
            throw e;
        }

        return pdfBytes;
    }
}

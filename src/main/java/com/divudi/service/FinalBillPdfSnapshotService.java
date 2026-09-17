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
            finalBillPdfSnapshotFacade.create(snapshot);
        } catch (RuntimeException e) {
            // Another concurrent call (eager generation at approval vs. lazy
            // generation on first view) may have already persisted a snapshot
            // for this bill, tripping the BILL_ID unique constraint. Re-check
            // for that snapshot before giving up on this one.
            FinalBillPdfSnapshot raceWinner = finalBillPdfSnapshotFacade.findByBillId(bill.getId());
            if (raceWinner != null) {
                return raceWinner.getPdfBytes();
            }
            throw e;
        }

        return pdfBytes;
    }
}

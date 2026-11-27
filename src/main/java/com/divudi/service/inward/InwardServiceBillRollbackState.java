package com.divudi.service.inward;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Page-copy state of an inward service settle, captured before the one
 * transaction runs and put back if that transaction rolls back (#17562).
 * <p>
 * The database undoes every row of a failed settle. The objects the caller holds
 * do not undo themselves: a bill item, bill fee or bill component keeps the id
 * the insert gave it, a bill item keeps the fees added to it, and the bills the
 * settle created stay in the caller's list. Left like that, the retry would
 * update rows that do not exist and bill every fee twice. Free of CDI so it can
 * be unit tested.
 */
final class InwardServiceBillRollbackState {

    private final List<Runnable> undo = new ArrayList<>();
    private final List<Bill> collector;
    private final int collectorSize;
    private final InwardServiceBillRequest request;

    InwardServiceBillRollbackState(InwardServiceBillRequest request) {
        this.request = request;
        this.collector = request.getBillCollector();
        this.collectorSize = collector == null ? 0 : collector.size();
        if (collector != null) {
            // The batch bill is linked over the whole collector, so a bill the
            // caller already held is pointed at the new batch bill during the settle.
            for (Bill held : new ArrayList<>(collector)) {
                Bill backward = held.getBackwardReferenceBill();
                undo.add(() -> held.setBackwardReferenceBill(backward));
            }
        }
        if (request.getBillEntries() != null) {
            for (BillEntry entry : request.getBillEntries()) {
                captureEntry(entry);
            }
        }
    }

    private void captureEntry(BillEntry entry) {
        BillItem item = entry.getBillItem();
        if (item != null) {
            Long itemId = item.getId();
            Bill itemBill = item.getBill();
            // Read without creating: the getter would add a row the settle never writes.
            PharmaceuticalBillItem pbi = item.getPharmaceuticalBillItemIfPresent();
            Long pbiId = pbi == null ? null : pbi.getId();
            BillItemFinanceDetails bifd = item.hasBillItemFinanceDetails() ? item.getBillItemFinanceDetails() : null;
            Long bifdId = bifd == null ? null : bifd.getId();
            int feeCount = item.getBillFees() == null ? 0 : item.getBillFees().size();
            undo.add(() -> {
                item.setId(itemId);
                item.setBill(itemBill);
                if (pbi != null) {
                    pbi.setId(pbiId);
                }
                if (bifd != null) {
                    bifd.setId(bifdId);
                }
                List<BillFee> held = item.getBillFees();
                if (held != null && held.size() > feeCount) {
                    held.subList(feeCount, held.size()).clear();
                }
            });
        }
        // The fees the entry carries and the fees the item already holds: a fee in
        // either list gets its id from the insert, and the rollback removes the row.
        List<BillFee> fees = new ArrayList<>();
        if (entry.getLstBillFees() != null) {
            fees.addAll(entry.getLstBillFees());
        }
        if (item != null && item.getBillFees() != null) {
            fees.addAll(item.getBillFees());
        }
        java.util.Set<BillFee> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (BillFee fee : fees) {
            if (fee == null || !seen.add(fee)) {
                continue;
            }
            Long id = fee.getId();
            Bill bill = fee.getBill();
            BillItem billItem = fee.getBillItem();
            // The inward margin and discount rewrite these; the item's sums read
            // feeValue before the margin, so a retry needs the first-attempt figures.
            double feeValue = fee.getFeeValue();
            double feeMargin = fee.getFeeMargin();
            double feeDiscount = fee.getFeeDiscount();
            Double feeUnitValue = fee.getFeeUnitValue();
            Double feeUnitMargin = fee.getFeeUnitMargin();
            Double feeUnitDiscount = fee.getFeeUnitDiscount();
            undo.add(() -> {
                fee.setId(id);
                fee.setBill(bill);
                fee.setBillItem(billItem);
                fee.setFeeValue(feeValue);
                fee.setFeeMargin(feeMargin);
                fee.setFeeDiscount(feeDiscount);
                fee.setFeeUnitValue(feeUnitValue);
                fee.setFeeUnitMargin(feeUnitMargin);
                fee.setFeeUnitDiscount(feeUnitDiscount);
            });
        }
        if (entry.getLstBillComponents() != null) {
            for (BillComponent component : new ArrayList<>(entry.getLstBillComponents())) {
                Long id = component.getId();
                Bill bill = component.getBill();
                undo.add(() -> {
                    component.setId(id);
                    component.setBill(bill);
                });
            }
        }
    }

    /** Adds a step to run on restore, for state held outside the request (for example a session balance). */
    void addUndo(Runnable step) {
        undo.add(step);
    }

    /** Puts the caller's objects back as they were before the settle started. */
    void restore() {
        if (collector != null && collector.size() > collectorSize) {
            collector.subList(collectorSize, collector.size()).clear();
        }
        request.setCreatedBatchBill(null);
        for (Runnable step : undo) {
            step.run();
        }
    }
}

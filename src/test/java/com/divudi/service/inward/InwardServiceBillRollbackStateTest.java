package com.divudi.service.inward;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Issue #17562: a failed settle leaves the caller's objects as they were. */
class InwardServiceBillRollbackStateTest {

    @Test
    void restoreUndoesWhatASettleChangesOnTheCallersObjects() {
        BillItem item = new BillItem();
        BillFee fee = new BillFee();
        BillComponent component = new BillComponent();
        BillEntry entry = new BillEntry();
        entry.setBillItem(item);
        entry.setLstBillFees(new ArrayList<>(List.of(fee)));
        entry.setLstBillComponents(new ArrayList<>(List.of(component)));
        Bill held = new BilledBill();
        List<Bill> collector = new ArrayList<>(List.of(held));

        InwardServiceBillRequest request = new InwardServiceBillRequest();
        request.setBillEntries(new ArrayList<>(List.of(entry)));
        request.setBillCollector(collector);

        InwardServiceBillRollbackState state = new InwardServiceBillRollbackState(request);

        // What a settle does before it fails.
        Bill created = new BilledBill();
        Bill batch = new BilledBill();
        collector.add(created);
        request.setCreatedBatchBill(batch);
        held.setBackwardReferenceBill(batch);
        item.setId(11L);
        item.setBill(created);
        item.getBillFees().add(fee);
        fee.setId(12L);
        fee.setBill(created);
        fee.setBillItem(item);
        component.setId(13L);
        component.setBill(created);

        state.restore();

        assertEquals(1, collector.size());
        assertSame(held, collector.get(0));
        assertNull(request.getCreatedBatchBill());
        assertNull(held.getBackwardReferenceBill());
        assertNull(item.getId());
        assertNull(item.getBill());
        assertEquals(0, item.getBillFees().size());
        assertNull(fee.getId());
        assertNull(fee.getBill());
        assertNull(fee.getBillItem());
        assertNull(component.getId());
        assertNull(component.getBill());
    }

    @Test
    void restoreResetsIdsOfFeesHeldOnlyByTheItem() {
        BillItem item = new BillItem();
        BillFee itemOnly = new BillFee();
        BillFee saved = new BillFee();
        saved.setId(5L);
        item.getBillFees().add(itemOnly);
        item.getBillFees().add(saved);
        BillEntry entry = new BillEntry();
        entry.setBillItem(item);
        entry.setLstBillFees(new ArrayList<>());
        InwardServiceBillRequest request = new InwardServiceBillRequest();
        request.setBillEntries(new ArrayList<>(List.of(entry)));
        request.setBillCollector(new ArrayList<>());

        InwardServiceBillRollbackState state = new InwardServiceBillRollbackState(request);
        itemOnly.setId(21L);
        saved.setId(5L);
        state.restore();

        assertNull(itemOnly.getId());
        assertEquals(Long.valueOf(5L), saved.getId());
    }

    @Test
    void sumsCountAFeeOnceWhenTheItemListHoldsItTwice() {
        BillFee fee = new BillFee();
        fee.setFeeGrossValue(500.0);
        fee.setFeeValue(487.5);
        fee.setFeeDiscount(12.5);
        Object[] sums = InwardServiceBillSums.itemSums(new ArrayList<>(List.of(fee, fee)));
        assertEquals(500.0, ((Number) sums[0]).doubleValue());
        assertEquals(12.5, ((Number) sums[3]).doubleValue());
    }

    @Test
    void captureAndRestoreDoNotCreateAPharmaceuticalBillItem() {
        BillItem item = new BillItem();
        BillEntry entry = new BillEntry();
        entry.setBillItem(item);
        InwardServiceBillRequest request = new InwardServiceBillRequest();
        request.setBillEntries(new ArrayList<>(List.of(entry)));
        request.setBillCollector(new ArrayList<>());

        InwardServiceBillRollbackState state = new InwardServiceBillRollbackState(request);
        assertNull(item.getPharmaceuticalBillItemIfPresent());

        state.restore();
        assertNull(item.getPharmaceuticalBillItemIfPresent());
    }

    @Test
    void restoreBringsBackTheFeeFiguresTheMarginChanged() {
        BillFee fee = new BillFee();
        fee.setFeeGrossValue(100.0);
        fee.setFeeValue(100.0);
        fee.setFeeUnitValue(100.0);
        BillEntry entry = new BillEntry();
        entry.setBillItem(new BillItem());
        entry.setLstBillFees(new ArrayList<>(List.of(fee)));
        InwardServiceBillRequest request = new InwardServiceBillRequest();
        request.setBillEntries(new ArrayList<>(List.of(entry)));
        request.setBillCollector(new ArrayList<>());

        InwardServiceBillRollbackState state = new InwardServiceBillRollbackState(request);
        // What a 10 percent margin and a discount do before the settle fails.
        fee.setFeeMargin(10.0);
        fee.setFeeUnitMargin(10.0);
        fee.setFeeDiscount(5.0);
        fee.setFeeUnitDiscount(5.0);
        fee.setFeeValue(105.0);
        fee.setFeeUnitValue(105.0);
        state.restore();

        assertEquals(100.0, fee.getFeeValue());
        assertEquals(0.0, fee.getFeeMargin());
        assertEquals(0.0, fee.getFeeDiscount());
        assertEquals(100.0, fee.getFeeUnitValue());
        assertNull(fee.getFeeUnitMargin());
        assertNull(fee.getFeeUnitDiscount());
        // The retry sums as the first attempt did.
        assertEquals(100.0, ((Number) InwardServiceBillSums.itemSums(new ArrayList<>(List.of(fee)))[1]).doubleValue());
    }

    @Test
    void restoreRunsExtraUndoSteps() {
        InwardServiceBillRequest request = new InwardServiceBillRequest();
        request.setBillEntries(new ArrayList<>());
        request.setBillCollector(new ArrayList<>());
        double[] balance = {60.0};
        InwardServiceBillRollbackState state = new InwardServiceBillRollbackState(request);
        final double before = balance[0];
        state.addUndo(() -> balance[0] = before);
        balance[0] = 0.0;
        state.restore();
        assertEquals(60.0, balance[0]);
    }
}

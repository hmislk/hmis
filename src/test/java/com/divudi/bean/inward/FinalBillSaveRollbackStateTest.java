package com.divudi.bean.inward;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.PatientEncounter;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FinalBillSaveRollbackStateTest {

    @Test
    void restoreUndoesWhatASettleChangesOnThePageCopies() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        PatientEncounter pe = new PatientEncounter();
        pe.setGrantTotal(10.0);
        pe.setDiscount(1.0);
        pe.setNetTotal(9.0);
        pe.setCreditUsedAmount(2.0);

        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, pe);

        // What a settle does before it fails.
        current.setId(77L);
        current.getBillItems().add(new BillItem());
        current.getBillItems().add(new BillItem());
        pe.setFinalBill(current);
        pe.setGrantTotal(500.0);
        pe.setDiscount(50.0);
        pe.setNetTotal(450.0);
        pe.setPaymentFinalized(true);
        pe.setCreditUsedAmount(450.0);

        state.restore();

        assertNull(current.getId());
        assertEquals(0, current.getBillItems().size());
        assertNull(pe.getFinalBill());
        assertEquals(10.0, pe.getGrantTotal());
        assertEquals(1.0, pe.getDiscount());
        assertEquals(9.0, pe.getNetTotal());
        assertEquals(2.0, pe.getCreditUsedAmount());
        assertFalse(pe.isPaymentFinalized());
    }

    @Test
    void restoreKeepsBillItemsThatWereThereBeforeTheSave() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        BillItem before = new BillItem();
        current.getBillItems().add(before);
        PatientEncounter pe = new PatientEncounter();

        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, pe);
        current.getBillItems().add(new BillItem());
        state.restore();

        assertEquals(1, current.getBillItems().size());
        assertEquals(before, current.getBillItems().get(0));
    }

    @Test
    void restoreBringsBackTheReferenceBillAndConfirmedFlag() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        Bill earlierReference = new BilledBill();
        current.setReferenceBill(earlierReference);
        PatientEncounter pe = new PatientEncounter();

        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, pe);
        current.setReferenceBill(new BilledBill());
        current.setConfirmedFinalBill(true);
        state.restore();

        assertSame(earlierReference, current.getReferenceBill());
        assertFalse(current.isConfirmedFinalBill());
    }

    @Test
    void restoreKeepsAnOriginalBillThatExistedBeforeTheSave() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        Bill original = new BilledBill();
        original.setId(5L);
        original.setDiscount(3.0);
        original.setNetTotal(97.0);
        original.setBillItems(new ArrayList<>());
        PatientEncounter pe = new PatientEncounter();

        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, pe, original);
        original.setDiscount(20.0);
        original.setNetTotal(80.0);
        original.getBillItems().add(new BillItem());
        state.restore();

        assertSame(original, state.getOriginalBillBeforeSave());
        assertEquals(5L, original.getId());
        assertEquals(3.0, original.getDiscount());
        assertEquals(97.0, original.getNetTotal());
        assertEquals(0, original.getBillItems().size());
    }

    @Test
    void restoreGivesNoOriginalBillWhenThePageHadNone() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, new PatientEncounter(), null);
        state.restore();
        assertNull(state.getOriginalBillBeforeSave());
    }

    @Test
    void restoreResetsAnUnsavedOriginalBillThatTheSaveGaveAnId() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        Bill original = new BilledBill();
        original.setBillItems(new ArrayList<>());
        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, new PatientEncounter(), original);
        original.setId(9L);
        original.getBillItems().add(new BillItem());
        state.restore();
        assertNull(original.getId());
        assertEquals(0, original.getBillItems().size());
    }

    @Test
    void restoreClearsTheIdOfABillFinanceDetailsTheFailedSaveInserted() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        BillFinanceDetails bfd = current.getBillFinanceDetails();
        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, new PatientEncounter());

        bfd.setId(41L);
        state.restore();

        assertNull(bfd.getId());
        assertSame(bfd, current.getBillFinanceDetails());
    }

    @Test
    void restoreDropsABillFinanceDetailsTheSaveCreated() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, new PatientEncounter());

        current.getBillFinanceDetails().setId(42L);
        state.restore();

        assertFalse(current.hasBillFinanceDetails());
    }

    @Test
    void restoreKeepsTheIdOfABillFinanceDetailsThatWasSavedBefore() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        BillFinanceDetails bfd = current.getBillFinanceDetails();
        bfd.setId(7L);
        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, new PatientEncounter());

        state.restore();

        assertEquals(7L, bfd.getId());
    }

    @Test
    void restoreClearsNewChildIdsOfTheOriginalBillAndItsExistingItems() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        Bill original = new BilledBill();
        original.setId(5L);
        original.setBillItems(new ArrayList<>());
        BillFinanceDetails originalBfd = original.getBillFinanceDetails();
        originalBfd.setId(8L);
        BillItem item = new BillItem();
        item.setId(6L);
        BillItemFinanceDetails bifd = item.getBillItemFinanceDetails();
        BillFee fee = new BillFee();
        item.getBillFees().add(fee);
        BillFee savedFee = new BillFee();
        savedFee.setId(3L);
        item.getBillFees().add(savedFee);
        original.getBillItems().add(item);

        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, new PatientEncounter(), original);
        bifd.setId(61L);
        fee.setId(62L);
        state.restore();

        assertEquals(8L, originalBfd.getId());
        assertNull(bifd.getId());
        assertNull(fee.getId());
        assertEquals(3L, savedFee.getId());
    }
}

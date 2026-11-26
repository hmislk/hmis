package com.divudi.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO;
import com.divudi.core.entity.BillItem;
import org.junit.jupiter.api.Test;

class PharmacyItemRefundFlagsTest {

    private static BillItem flagged() {
        BillItem bi = new BillItem();
        bi.setRefunded(true);
        bi.setBillItemRefunded(true);
        return bi;
    }

    @Test
    void markRefundedSetsBothFlagsOnce() {
        BillItem bi = new BillItem();
        assertTrue(PharmacyItemRefundFlags.markRefunded(bi));
        assertTrue(bi.isRefunded());
        assertTrue(bi.isBillItemRefunded());
        assertFalse(PharmacyItemRefundFlags.markRefunded(bi));
    }

    @Test
    void markRefundedIgnoresNull() {
        assertFalse(PharmacyItemRefundFlags.markRefunded(null));
    }

    @Test
    void cancellingTheOnlyRefundClearsBothFlags() {
        BillItem bi = flagged();
        assertTrue(PharmacyItemRefundFlags.clearAfterRefundCancelled(bi, 0));
        assertFalse(bi.isRefunded());
        assertFalse(bi.isBillItemRefunded());
    }

    @Test
    void anotherActiveRefundKeepsTheFlags() {
        BillItem bi = flagged();
        assertFalse(PharmacyItemRefundFlags.clearAfterRefundCancelled(bi, 1));
        assertTrue(bi.isRefunded());
        assertTrue(bi.isBillItemRefunded());
        assertFalse(PharmacyItemRefundFlags.clearAfterRefundCancelled(bi, 3));
        assertTrue(bi.isRefunded());
    }

    @Test
    void clearingAnUnflaggedItemChangesNothing() {
        assertFalse(PharmacyItemRefundFlags.clearAfterRefundCancelled(new BillItem(), 0));
        assertFalse(PharmacyItemRefundFlags.clearAfterRefundCancelled(null, 0));
    }

    @Test
    void labelShowsWhenEitherItemFlagIsSet() {
        assertTrue(PharmacyItemRefundFlags.isShownRefunded(true, false));
        assertTrue(PharmacyItemRefundFlags.isShownRefunded(false, true));
        assertTrue(PharmacyItemRefundFlags.isShownRefunded(null, true));
        assertFalse(PharmacyItemRefundFlags.isShownRefunded(false, false));
        assertFalse(PharmacyItemRefundFlags.isShownRefunded(null, null));
    }

    @Test
    void searchDtoCarriesTheCombinedLabel() {
        PharmacySaleBillItemSearchDTO itemOnly = new PharmacySaleBillItemSearchDTO(
                1L, "D1", null, 2L, "n", "c", 1.0, 1.0, "p", false, false, true);
        assertEquals(Boolean.TRUE, itemOnly.getRefunded());
        PharmacySaleBillItemSearchDTO linkedOnly = new PharmacySaleBillItemSearchDTO(
                1L, "D1", null, 2L, "n", "c", 1.0, 1.0, "p", false, null, true);
        assertEquals(Boolean.TRUE, linkedOnly.getRefunded());
        PharmacySaleBillItemSearchDTO none = new PharmacySaleBillItemSearchDTO(
                1L, "D1", null, 2L, "n", "c", 1.0, 1.0, "p", false, false, false);
        assertEquals(Boolean.FALSE, none.getRefunded());
    }

    @Test
    void returnWithPaymentMarksTheReturnedItemAndShowsTheLabel() {
        BillItem original = new BillItem();
        BillItem returnItem = new BillItem();
        returnItem.setReferanceBillItem(original);
        assertTrue(PharmacyItemRefundFlags.markRefunded(returnItem.getReferanceBillItem()));
        assertTrue(PharmacyItemRefundFlags.isShownRefunded(original.isRefunded(), null));
    }

    @Test
    void cancellingAReturnWithPaymentKeepsTheLabelWhileAnotherRefundIsActive() {
        BillItem original = flagged();
        assertFalse(PharmacyItemRefundFlags.clearAfterRefundCancelled(original, 1));
        assertTrue(original.isRefunded());
        assertTrue(PharmacyItemRefundFlags.clearAfterRefundCancelled(original, 0));
        assertFalse(PharmacyItemRefundFlags.isShownRefunded(original.isRefunded(), null));
    }

    private static PharmacySaleBillItemSearchDTO row(Long billId, Long itemId, Boolean refunded) {
        return new PharmacySaleBillItemSearchDTO(
                billId, "D", null, itemId, "n", "c", 1.0, 1.0, "p", false, refunded);
    }

    @Test
    void billedItemFlagShowsTheLabelOnlyForTheSameBillAndItem() {
        PharmacySaleBillItemSearchDTO same = row(10L, 5L, false);
        PharmacySaleBillItemSearchDTO otherItem = row(10L, 6L, false);
        PharmacySaleBillItemSearchDTO otherBill = row(11L, 5L, false);
        java.util.List<PharmacySaleBillItemSearchDTO> rows = java.util.Arrays.asList(same, otherItem, otherBill);
        java.util.List<Object[]> pairs = new java.util.ArrayList<>();
        pairs.add(new Object[]{10L, 5L});
        PharmacyItemRefundFlags.applyBilledItemFlags(rows, PharmacyItemRefundFlags.lineKeys(pairs));
        assertEquals(Boolean.TRUE, same.getRefunded());
        assertEquals(Boolean.FALSE, otherItem.getRefunded());
        assertEquals(Boolean.FALSE, otherBill.getRefunded());
    }

    @Test
    void billedItemFlagKeepsAnAlreadyShownLabelAndToleratesNulls() {
        PharmacySaleBillItemSearchDTO shown = row(10L, 5L, true);
        PharmacyItemRefundFlags.applyBilledItemFlags(java.util.Arrays.asList(shown), new java.util.HashSet<>());
        assertEquals(Boolean.TRUE, shown.getRefunded());
        PharmacyItemRefundFlags.applyBilledItemFlags(null, null);
        assertTrue(PharmacyItemRefundFlags.lineKeys(null).isEmpty());
        java.util.List<Object[]> pairs = new java.util.ArrayList<>();
        pairs.add(new Object[]{10L, null});
        assertTrue(PharmacyItemRefundFlags.lineKeys(pairs).isEmpty());
    }
}

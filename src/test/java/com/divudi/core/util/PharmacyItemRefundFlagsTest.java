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
    void labelShowsWhenEitherFlagIsSet() {
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
        PharmacySaleBillItemSearchDTO none = new PharmacySaleBillItemSearchDTO(
                1L, "D1", null, 2L, "n", "c", 1.0, 1.0, "p", false, false, false);
        assertEquals(Boolean.FALSE, none.getRefunded());
    }
}

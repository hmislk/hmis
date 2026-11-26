package com.divudi.bean.inward;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A company share below what it has paid is rejected (#26163). */
class BhtSummeryControllerCompanyShareTest {

    @Test
    void shareBelowPaidIsRejected() {
        assertTrue(BhtSummeryController.isShareBelowPaid(900.0, 1000.0));
    }

    @Test
    void shareEqualOrAbovePaidIsAllowed() {
        assertFalse(BhtSummeryController.isShareBelowPaid(1000.0, 1000.0));
        assertFalse(BhtSummeryController.isShareBelowPaid(1500.0, 1000.0));
    }

    @Test
    void nothingPaidNeverBlocks() {
        assertFalse(BhtSummeryController.isShareBelowPaid(0.0, 0.0));
    }

    @Test
    void toleranceOfOneCentIsAllowed() {
        assertFalse(BhtSummeryController.isShareBelowPaid(999.99, 1000.0));
        assertTrue(BhtSummeryController.isShareBelowPaid(999.98, 1000.0));
    }

    @Test
    public void oneCentDifferenceIsNotBlockedButTwoCentsIs() {
        assertFalse(BhtSummeryController.isShareBelowPaid(99.99, 100.00));
        assertTrue(BhtSummeryController.isShareBelowPaid(99.98, 100.00));
    }

    @Test
    void companyLeftOutOfNewVersionIsBlockedWhenItHasPaid() {
        Map<String, Double> shares = new LinkedHashMap<>();
        shares.put("A", 500.0);
        Map<String, Double> paid = new LinkedHashMap<>();
        paid.put("A", 400.0);
        paid.put("B", 300.0); // B has no allocation row: share 0
        assertEquals("B", BhtSummeryController.firstCompanyBelowPaid(shares, paid));
    }

    @Test
    void noCompanyBelowPaidReturnsNull() {
        Map<String, Double> shares = new LinkedHashMap<>();
        shares.put("A", 500.0);
        Map<String, Double> paid = new LinkedHashMap<>();
        paid.put("A", 500.0);
        paid.put("B", 0.0); // no allocation, nothing paid
        assertNull(BhtSummeryController.firstCompanyBelowPaid(shares, paid));
    }
}

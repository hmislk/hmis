package com.divudi.bean.inward;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}

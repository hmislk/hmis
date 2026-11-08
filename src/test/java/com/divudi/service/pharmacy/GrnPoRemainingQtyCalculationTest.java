package com.divudi.service.pharmacy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests verifying that PO remaining quantity calculations floor at 0
 * and do not invert negative remainders to positive values (#25302).
 */
public class GrnPoRemainingQtyCalculationTest {

    /**
     * Replicates the calculation semantics of SQL:
     * GREATEST(COALESCE(col, 0) - ABS(receivedQty), 0)
     */
    static double calculateSqlRemaining(Double storedQty, double receivedQty) {
        double col = (storedQty == null) ? 0.0 : storedQty;
        return Math.max(col - Math.abs(receivedQty), 0.0);
    }

    /**
     * Replicates the calculation semantics in GrnCostingController:
     * Math.max(0.0, Math.max(0.0, storedQty) - Math.abs(receivedQty))
     */
    static double calculateLegacyRemaining(double storedQty, double receivedQty) {
        double current = Math.max(0.0, storedQty);
        return Math.max(0.0, current - Math.abs(receivedQty));
    }

    @Test
    void negativeStoredQty_doesNotBecomePositiveAfterReceipt_sql() {
        // Issue #25302: ABS(-5) - 1 previously became 4.
        // With GREATEST(COALESCE(col, 0) - ABS(?), 0): -5 - 1 = -6 -> 0.
        assertEquals(0.0, calculateSqlRemaining(-5.0, 1.0));
    }

    @Test
    void negativeStoredQty_doesNotBecomePositiveAfterReceipt_controller() {
        assertEquals(0.0, calculateLegacyRemaining(-5.0, 1.0));
    }

    @Test
    void normalPositiveQty_decrementsAccurately() {
        assertEquals(4.0, calculateSqlRemaining(5.0, 1.0));
        assertEquals(4.0, calculateLegacyRemaining(5.0, 1.0));
    }

    @Test
    void overReceipt_floorsAtZero() {
        assertEquals(0.0, calculateSqlRemaining(5.0, 10.0));
        assertEquals(0.0, calculateLegacyRemaining(5.0, 10.0));
    }

    @Test
    void nullOrZeroStoredQty_floorsAtZero() {
        assertEquals(0.0, calculateSqlRemaining(null, 2.0));
        assertEquals(0.0, calculateSqlRemaining(0.0, 2.0));
        assertEquals(0.0, calculateLegacyRemaining(0.0, 2.0));
    }
}

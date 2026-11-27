package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.cashTransaction.DrawerEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The drawer value that undoes a refund's drawer history row (#26196).
 */
public class DrawerServiceReversalTest {

    private static DrawerEntry entry(PaymentMethod m, Double value, boolean retired) {
        DrawerEntry e = new DrawerEntry();
        e.setPaymentMethod(m);
        e.setTransactionValue(value);
        e.setRetired(retired);
        return e;
    }

    @Test
    void cashRefundOutIsPutBackAsSameAmountIn() {
        DrawerEntry refundRow = entry(PaymentMethod.Cash, -1250.50, false);
        assertTrue(DrawerService.isReversible(refundRow));
        assertEquals(1250.50, DrawerService.reversalValue(refundRow), 0.0001);
    }

    @Test
    void cardRefundIsReversedOnTheCardMethodNotCash() {
        DrawerEntry refundRow = entry(PaymentMethod.Card, -300.0, false);
        assertTrue(DrawerService.isReversible(refundRow));
        assertEquals(300.0, DrawerService.reversalValue(refundRow), 0.0001);
        assertEquals(PaymentMethod.Card, refundRow.getPaymentMethod());
    }

    @Test
    void positiveRowIsReversedAsOut() {
        assertEquals(-40.0, DrawerService.reversalValue(entry(PaymentMethod.Cash, 40.0, false)), 0.0001);
    }

    @Test
    void rowsWithNothingToUndoAreSkipped() {
        assertFalse(DrawerService.isReversible(null));
        assertFalse(DrawerService.isReversible(entry(PaymentMethod.Cash, null, false)));
        assertFalse(DrawerService.isReversible(entry(PaymentMethod.Cash, 0.0, false)));
        assertFalse(DrawerService.isReversible(entry(null, -10.0, false)));
        assertFalse(DrawerService.isReversible(entry(PaymentMethod.Cash, -10.0, true)));
        assertEquals(0.0, DrawerService.reversalValue(null), 0.0);
    }
}

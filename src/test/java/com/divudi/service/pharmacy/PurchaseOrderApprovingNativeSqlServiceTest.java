package com.divudi.service.pharmacy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PurchaseOrderApprovingNativeSqlServiceTest {

    private final PurchaseOrderRequestNativeSqlService requestService = new PurchaseOrderRequestNativeSqlService();

    @Test
    void isZeroQtyLine_deferredToRequestService_trueWhenBothZero() {
        assertTrue(requestService.isZeroQtyLine(0.0, 0.0));
    }

    @Test
    void isZeroQtyLine_deferredToRequestService_falseWhenQtyPositive() {
        assertFalse(requestService.isZeroQtyLine(5.0, 0.0));
    }
}

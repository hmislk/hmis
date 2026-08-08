package com.divudi.service.pharmacy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * PurchaseOrderApprovingNativeSqlService itself has no pure-logic public
 * method -- every one of its methods requires a live EntityManager (native
 * SQL bill/billitem writes), same as every sibling *NativeSqlService class in
 * this codebase. What IS unit-testable is the cross-EJB delegation contract:
 * retireZeroQtyApprovedLines() decides which lines to retire by calling
 * PurchaseOrderRequestNativeSqlService.isZeroQtyLine(), so this locks in that
 * shared logic's behavior. It intentionally does not instantiate
 * PurchaseOrderApprovingNativeSqlService.
 */
class PurchaseOrderApprovingNativeSqlServiceTest {

    private final PurchaseOrderRequestNativeSqlService requestService = new PurchaseOrderRequestNativeSqlService();

    @Test
    void isZeroQtyLine_sharedWithRequestService_trueWhenBothZero() {
        assertTrue(requestService.isZeroQtyLine(0.0, 0.0));
    }

    @Test
    void isZeroQtyLine_sharedWithRequestService_falseWhenQtyPositive() {
        assertFalse(requestService.isZeroQtyLine(5.0, 0.0));
    }
}

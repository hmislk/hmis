package com.divudi.bean.inward;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** #26277: the surgery service item loader skips closed bills. */
public class BillBhtControllerSurgeryItemsJpqlTest {

    @Test
    public void loaderSkipsRetiredCancelledAndRefundedBills() {
        String q = BillBhtController.EXISTING_SURGERY_SERVICE_ITEMS_JPQL;
        assertTrue(q.contains("bi.retired = false"));
        assertTrue(q.contains("bi.bill.retired = false"));
        assertTrue(q.contains("bi.bill.cancelled = false"));
        assertTrue(q.contains("bi.bill.refunded = false"));
    }
}

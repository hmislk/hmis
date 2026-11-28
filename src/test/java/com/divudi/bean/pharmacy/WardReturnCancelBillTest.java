package com.divudi.bean.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.CancelledBill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cancel bill of a ward return to pharmacy must carry no items of its own.
 * Sharing the return bill's item list made create(cancelBill) insert a second
 * copy of every return item (Bill.billItems cascades ALL).
 */
public class WardReturnCancelBillTest {

    @Test
    public void cancelBillHasNoItemsAndDoesNotTouchReturnBillItems() {
        BilledBill returnBill = new BilledBill();
        returnBill.setNetTotal(-12.5);
        BillItem a = new BillItem();
        a.setBill(returnBill);
        BillItem b = new BillItem();
        b.setBill(returnBill);
        returnBill.getBillItems().add(a);
        returnBill.getBillItems().add(b);

        CancelledBill cb = WardPharmacyReturnToPharmacyController.newCancelBillFor(returnBill);

        assertTrue(cb.getBillItems().isEmpty(), "cancel bill must have no items");
        assertEquals(2, returnBill.getBillItems().size());
        assertSame(a, returnBill.getBillItems().get(0));
        assertSame(returnBill, a.getBill());
        assertEquals(12.5, cb.getNetTotal(), 0.0001);
        assertSame(returnBill, cb.getBilledBill());
        assertEquals(BillTypeAtomic.RETURN_MEDICINE_INWARD_CANCELLATION, cb.getBillTypeAtomic());
    }
}

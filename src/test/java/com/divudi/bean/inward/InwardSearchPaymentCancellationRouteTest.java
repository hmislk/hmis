package com.divudi.bean.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.RefundBill;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InwardSearchPaymentCancellationRouteTest {

    private static InwardSearch searchWithBill(Bill bill) throws Exception {
        InwardSearch search = new InwardSearch();
        Field f = InwardSearch.class.getDeclaredField("bill");
        f.setAccessible(true);
        f.set(search, bill);
        return search;
    }

    @Test
    void paymentRefund_routesToTheExistingRefundCancelPage() throws Exception {
        Bill refund = new RefundBill();
        refund.setBillTypeAtomic(BillTypeAtomic.INWARD_PAYMENT_REFUND);
        InwardSearch search = searchWithBill(refund);
        search.setPrintPreview(true);

        assertEquals("inward_cancel_bill_refund?faces-redirect=true", search.navigateToPaymentBillCancellation());
        assertFalse(search.isPrintPreview());
    }
}

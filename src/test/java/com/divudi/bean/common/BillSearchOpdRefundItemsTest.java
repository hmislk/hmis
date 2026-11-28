package com.divudi.bean.common;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.RefundBill;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BillSearchOpdRefundItemsTest {

    private BillItem itemWithFee(double feeValue) {
        BillItem bi = new BillItem();
        bi.setSearialNo(0); // unsaved OPD items share the same serial number
        BillFee bf = new BillFee();
        bf.setFeeValue(feeValue);
        bf.setBillItem(bi);
        bi.getBillFees().add(bf);
        return bi;
    }

    @Test
    public void partialRefundKeepsRefundedItemWhenZeroItemHasSameSerialNo() {
        BillItem zeroItem = itemWithFee(0.0);
        BillItem refundedItem = itemWithFee(100.0);
        assertEquals(zeroItem, refundedItem, "precondition: unsaved items with the same serial number are equal");

        Bill refundingBill = new RefundBill();
        refundingBill.setBillItems(new ArrayList<>());
        refundingBill.getBillItems().add(zeroItem);
        refundingBill.getBillItems().add(refundedItem);

        BillSearch billSearch = new BillSearch();
        billSearch.setRefundingBill(refundingBill);

        assertTrue(billSearch.calculateRefundTotalForOpdBill());

        assertEquals(1, refundingBill.getBillItems().size());
        assertSame(refundedItem, refundingBill.getBillItems().get(0));
        assertEquals(-100.0, refundingBill.getNetTotal(), 0.0001);
        assertEquals(-100.0, refundedItem.getNetValue(), 0.0001);
    }
}

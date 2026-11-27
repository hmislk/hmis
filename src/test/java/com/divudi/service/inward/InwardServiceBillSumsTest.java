package com.divudi.service.inward;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Issue #17562: the sums taken from the fees in memory equal the sums the
 * database gave for the same fees.
 */
class InwardServiceBillSumsTest {

    private static BillFee fee(Double gross, double value, double margin, double discount, double vat) {
        BillFee f = new BillFee();
        f.setFeeGrossValue(gross);
        f.setFeeValue(value);
        f.setFeeMargin(margin);
        f.setFeeDiscount(discount);
        f.setFeeVat(vat);
        return f;
    }

    @Test
    void itemSumsAddUpTheFiveColumns() {
        List<BillFee> fees = new ArrayList<>();
        fees.add(fee(100.0, 90.0, 10.0, 20.0, 5.0));
        fees.add(fee(50.0, 50.0, 0.0, 0.0, 1.0));

        Object[] sums = InwardServiceBillSums.itemSums(fees);

        assertEquals(150.0, (Double) sums[0], 0.0001);
        assertEquals(140.0, (Double) sums[1], 0.0001);
        assertEquals(10.0, (Double) sums[2], 0.0001);
        assertEquals(20.0, (Double) sums[3], 0.0001);
        assertEquals(6.0, (Double) sums[4], 0.0001);
    }

    @Test
    void retiredFeesAndNullGrossAreLeftOutAsTheDatabaseSumDoes() {
        List<BillFee> fees = new ArrayList<>();
        BillFee retired = fee(999.0, 999.0, 0.0, 0.0, 0.0);
        retired.setRetired(true);
        fees.add(retired);
        fees.add(fee(null, 10.0, 0.0, 0.0, 0.0));

        Object[] sums = InwardServiceBillSums.itemSums(fees);

        assertNull(sums[0]);
        assertEquals(10.0, (Double) sums[1], 0.0001);
    }

    @Test
    void noFeesGivesNoSumsAndAZeroBill() {
        Object[] sums = InwardServiceBillSums.itemSums(new ArrayList<>());
        for (Object o : sums) {
            assertNull(o);
        }
        Bill bill = new BilledBill();
        bill.setTotal(5.0);
        InwardServiceBillSums.applyToBill(bill, new ArrayList<>());
        assertEquals(0.0, bill.getTotal());
        assertEquals(0.0, bill.getNetTotal());
        assertEquals(0.0, bill.getVatPlusNetTotal());
    }

    @Test
    void billTotalsComeFromTheFees() {
        List<BillFee> fees = new ArrayList<>();
        fees.add(fee(100.0, 90.0, 0.0, 10.0, 4.0));
        fees.add(fee(40.0, 40.0, 0.0, 0.0, 1.0));
        Bill bill = new BilledBill();

        InwardServiceBillSums.applyToBill(bill, fees);

        assertEquals(140.0, bill.getTotal(), 0.0001);
        assertEquals(130.0, bill.getNetTotal(), 0.0001);
        assertEquals(5.0, bill.getVat(), 0.0001);
        assertEquals(135.0, bill.getVatPlusNetTotal(), 0.0001);
    }
}

package com.divudi.bean.common;

import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #17562: the five BillFee sums behind a BillItem now come from one query
 * row. The row must give the same BillItem values as the old five separate
 * sums, including zero rows, returns (negative fees) and a null sum.
 */
class BillBeanControllerBillItemSumsTest {

    private static BillFee fee(double gross, double net, double margin, double discount, double vat) {
        BillFee f = new BillFee();
        f.setFeeGrossValue(gross);
        f.setFeeValue(net);
        f.setFeeMargin(margin);
        f.setFeeDiscount(discount);
        f.setFeeVat(vat);
        return f;
    }

    /** The old logic: one sum per field, null counted as zero. */
    private static BillItem oldPerSum(List<BillFee> fees) {
        BillItem bi = new BillItem();
        double gross = 0, net = 0, margin = 0, discount = 0, vat = 0;
        for (BillFee f : fees) {
            gross += f.getFeeGrossValue();
            net += f.getFeeValue();
            margin += f.getFeeMargin();
            discount += f.getFeeDiscount();
            vat += f.getFeeVat();
        }
        bi.setGrossValue(gross);
        bi.setNetValue(net);
        bi.setMarginValue(margin);
        bi.setDiscount(discount);
        bi.setVat(vat);
        bi.setVatPlusNetValue(bi.getNetValue() + bi.getVat());
        return bi;
    }

    /** What the one-query row holds: the five SQL sums in order. */
    private static Object[] oneRow(List<BillFee> fees) {
        double[] s = new double[5];
        for (BillFee f : fees) {
            s[0] += f.getFeeGrossValue();
            s[1] += f.getFeeValue();
            s[2] += f.getFeeMargin();
            s[3] += f.getFeeDiscount();
            s[4] += f.getFeeVat();
        }
        return new Object[]{s[0], s[1], s[2], s[3], s[4]};
    }

    private static void assertSame(BillItem expected, BillItem actual) {
        assertEquals(expected.getGrossValue(), actual.getGrossValue(), 0.0);
        assertEquals(expected.getNetValue(), actual.getNetValue(), 0.0);
        assertEquals(expected.getMarginValue(), actual.getMarginValue(), 0.0);
        assertEquals(expected.getDiscount(), actual.getDiscount(), 0.0);
        assertEquals(expected.getVat(), actual.getVat(), 0.0);
        assertEquals(expected.getVatPlusNetValue(), actual.getVatPlusNetValue(), 0.0);
    }

    private static void check(List<BillFee> fees) {
        BillItem actual = new BillItem();
        BillBeanController.applyBillFeeSumsToBillItem(actual, oneRow(fees));
        assertSame(oldPerSum(fees), actual);
    }

    @Test
    void normalRows() {
        check(Arrays.asList(fee(1000, 900, 50, 150, 20), fee(500, 500, 0, 0, 5)));
    }

    @Test
    void zeroRows() {
        check(Arrays.asList(fee(0, 0, 0, 0, 0), fee(0, 0, 0, 0, 0)));
    }

    @Test
    void negativeRowsFromReturnAndCancel() {
        check(Arrays.asList(fee(1000, 900, 50, 100, 10), fee(-1000, -900, -50, -100, -10)));
        check(Arrays.asList(fee(-250, -225, 0, -25, -2.5)));
    }

    @Test
    void noFeesGivesZerosAsBefore() {
        check(new ArrayList<>());
        BillItem fromNullRow = new BillItem();
        BillBeanController.applyBillFeeSumsToBillItem(fromNullRow, null);
        assertSame(oldPerSum(new ArrayList<>()), fromNullRow);
    }

    @Test
    void nullSumCountsAsZero() {
        BillItem bi = new BillItem();
        BillBeanController.applyBillFeeSumsToBillItem(bi, new Object[]{100.0, null, 5.0, null, 1.0});
        assertEquals(100.0, bi.getGrossValue(), 0.0);
        assertEquals(0.0, bi.getNetValue(), 0.0);
        assertEquals(5.0, bi.getMarginValue(), 0.0);
        assertEquals(0.0, bi.getDiscount(), 0.0);
        assertEquals(1.0, bi.getVat(), 0.0);
        assertEquals(1.0, bi.getVatPlusNetValue(), 0.0);
    }
}

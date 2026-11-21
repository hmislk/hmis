package com.divudi.core.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InwardVatCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void noBillDiscountLeavesBaseUnchanged() {
        BigDecimal share = InwardVatCalculator.billDiscountShare(BigDecimal.ZERO, bd("1000"), bd("5000"));
        assertEquals(0, share.signum());
        BigDecimal base = InwardVatCalculator.taxableBase(bd("1000"), BigDecimal.ZERO, share);
        assertEquals(0, bd("1000").compareTo(base));
        assertEquals(bd("150.00"), InwardVatCalculator.vatOnBase(base, 15));
    }

    @Test
    void discountIsSpreadInProportionToNetAndSumsExactly() {
        BigDecimal discount = bd("300");
        BigDecimal a = bd("1000");
        BigDecimal b = bd("2000");
        BigDecimal sum = a.add(b);
        BigDecimal shareA = InwardVatCalculator.billDiscountShare(discount, a, sum);
        BigDecimal shareB = InwardVatCalculator.billDiscountShare(discount, b, sum);
        assertEquals(0, bd("100").compareTo(shareA));
        assertEquals(0, bd("200").compareTo(shareB));
        assertEquals(0, discount.compareTo(shareA.add(shareB)));
    }

    @Test
    void shareNeverNegativeAndZeroWithoutNet() {
        assertEquals(0, InwardVatCalculator.billDiscountShare(bd("-5"), bd("10"), bd("10")).signum());
        assertEquals(0, InwardVatCalculator.billDiscountShare(bd("5"), bd("-10"), bd("10")).signum());
        assertEquals(0, InwardVatCalculator.billDiscountShare(bd("5"), bd("10"), BigDecimal.ZERO).signum());
    }

    @Test
    void baseNeverBelowZeroAndOwnVatItemsAreLeftOut() {
        assertEquals(0, bd("10").compareTo(InwardVatCalculator.taxableBase(bd("100"), bd("80"), bd("50"))));
        assertEquals(0, InwardVatCalculator.taxableBase(bd("100"), bd("150"), BigDecimal.ZERO).signum());
        BigDecimal base = InwardVatCalculator.taxableBase(bd("1000"), bd("400"), bd("100"));
        assertEquals(0, bd("540").compareTo(base));
        assertEquals(bd("81.00"), InwardVatCalculator.vatOnBase(base, 15));
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        assertEquals(bd("0.13"), InwardVatCalculator.vatOnBase(bd("1.05"), 12.5));
        assertEquals(bd("0.12"), InwardVatCalculator.vatOnBase(bd("0.95"), 12.5));
        assertTrue(InwardVatCalculator.vatOnBase(BigDecimal.ZERO, 15).signum() == 0);
    }

    @Test
    void ownVatItemsTakeNoShareOfTheBillDiscount() {
        // Lab net 1000 (600 own VAT), Room 1000, discount 200, rate 10%: 36 + 90 = 126.
        BigDecimal sum = bd("2000");
        BigDecimal labShare = InwardVatCalculator.billDiscountShare(bd("200"), bd("1000"), sum);
        BigDecimal roomShare = InwardVatCalculator.billDiscountShare(bd("200"), bd("1000"), sum);
        BigDecimal lab = InwardVatCalculator.vatOnBase(InwardVatCalculator.taxableBase(bd("1000"), bd("600"), labShare), 10);
        BigDecimal room = InwardVatCalculator.vatOnBase(InwardVatCalculator.taxableBase(bd("1000"), BigDecimal.ZERO, roomShare), 10);
        assertEquals(bd("36.00"), lab);
        assertEquals(bd("90.00"), room);
        assertEquals(bd("126.00"), lab.add(room));
    }

    @Test
    void cancelledOwnVatItemNetsOutOfTheExcludedAmount() {
        // Lab: own-VAT item A 1000 cancelled (+1000 -1000 = 0 own VAT), plain item B 500.
        // Row net 500, own VAT amount 0 -> VAT 50 at 10%.
        BigDecimal ownVat = bd("1000").add(bd("-1000"));
        BigDecimal base = InwardVatCalculator.taxableBase(bd("500"), ownVat, BigDecimal.ZERO);
        assertEquals(bd("50.00"), InwardVatCalculator.vatOnBase(base, 10));
    }
}

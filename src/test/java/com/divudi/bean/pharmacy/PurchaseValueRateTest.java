package com.divudi.bean.pharmacy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PurchaseValueRateTest {

    @Test
    @DisplayName("2,500 / 3: derive, store at 4 dp, reload, and the gross is restored to the typed value")
    void deriveSaveReload() {
        BigDecimal value = new BigDecimal("2500.00");
        BigDecimal qty = new BigDecimal("3");
        BigDecimal rate = PurchaseValueRate.derive(value, qty);
        assertEquals(0, rate.multiply(qty).setScale(4, RoundingMode.HALF_UP).compareTo(new BigDecimal("2500.0000")));

        // what the database keeps: scale 4 on both columns
        BigDecimal storedRate = rate.setScale(4, RoundingMode.HALF_UP);
        BigDecimal storedGross = value.setScale(4, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("833.3333"), storedRate);
        assertEquals(0, storedRate.multiply(qty).compareTo(new BigDecimal("2499.9999")));

        // reload: the line is recognised as value-derived and the gross is the typed value
        assertTrue(PurchaseValueRate.isValueDerivedLine(storedGross, qty, storedRate));
    }

    @Test
    @DisplayName("An ordinary line (rate x qty reproduces the gross) is not treated as value-derived")
    void ordinaryLineNotRestored() {
        assertFalse(PurchaseValueRate.isValueDerivedLine(new BigDecimal("75.0000"), new BigDecimal("3"), new BigDecimal("25.0000")));
        assertFalse(PurchaseValueRate.isValueDerivedLine(new BigDecimal("75.0000"), BigDecimal.ZERO, new BigDecimal("25.0000")));
    }

    @Test
    @DisplayName("100 units / 2,500.00 gives 25 exactly")
    void exactRate() {
        assertEquals(0, PurchaseValueRate.derive(new BigDecimal("2500.00"), new BigDecimal("100")).compareTo(new BigDecimal("25")));
    }

    @Test
    @DisplayName("Rate box sent back with the 2-decimal text is untouched; a different rate is an edit")
    void untouchedRate() {
        BigDecimal value = new BigDecimal("2500.00");
        BigDecimal qty = new BigDecimal("3");
        assertTrue(PurchaseValueRate.isRateUntouched(value, qty, new BigDecimal("833.33")));
        assertTrue(PurchaseValueRate.isRateUntouched(value, qty, PurchaseValueRate.derive(value, qty)));
        assertFalse(PurchaseValueRate.isRateUntouched(value, qty, new BigDecimal("830")));
        assertFalse(PurchaseValueRate.isRateUntouched(value, BigDecimal.ZERO, new BigDecimal("833.33")));
    }

    @Test
    @DisplayName("Converter keeps a prefilled rate with more than 2 decimals when the box is left untouched")
    void converterKeepsExtraDecimals() {
        BigDecimal current = new BigDecimal("12.3456");
        assertEquals(0, PurchaseMoneyDisplayConverter.parse("12.35", current).compareTo(current));
        assertEquals(0, PurchaseMoneyDisplayConverter.parse("13.10", current).compareTo(new BigDecimal("13.10")));
        assertEquals(0, PurchaseMoneyDisplayConverter.parse("2,500.00", null).compareTo(new BigDecimal("2500")));
        assertEquals("12.35", new PurchaseMoneyDisplayConverter().getAsString(null, null, current));
    }
}

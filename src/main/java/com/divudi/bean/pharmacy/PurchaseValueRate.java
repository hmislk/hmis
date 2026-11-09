package com.divudi.bean.pharmacy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Shared rules for deriving a purchase rate from a typed total purchase value (issue #22864).
 */
public final class PurchaseValueRate {

    private PurchaseValueRate() {
    }

    /**
     * Rate = value / paid quantity at full double precision (per pack for a pack item).
     */
    public static BigDecimal derive(BigDecimal value, BigDecimal quantity) {
        return value.divide(quantity, MathContext.DECIMAL64);
    }

    /**
     * True when the rate sent back from the rate box is just the derived rate (exact, or its
     * 2-decimal display), i.e. the user did not type a rate of their own.
     */
    public static boolean isRateUntouched(BigDecimal enteredValue, BigDecimal quantity, BigDecimal submittedRate) {
        if (enteredValue == null || submittedRate == null || quantity == null || quantity.signum() <= 0) {
            return false;
        }
        BigDecimal derived = derive(enteredValue, quantity);
        return submittedRate.compareTo(derived) == 0
                || submittedRate.compareTo(derived.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    /**
     * A saved line comes back without the typed value (it is not stored). It is a value-derived
     * line when quantity x the stored 4-decimal rate no longer reproduces the stored gross but
     * gross / quantity does give that rate; then the stored gross is the typed value.
     */
    public static boolean isValueDerivedLine(BigDecimal storedGross, BigDecimal quantity, BigDecimal storedRate) {
        if (storedGross == null || storedRate == null || quantity == null || quantity.signum() <= 0) {
            return false;
        }
        boolean rateReproducesGross = storedRate.multiply(quantity).compareTo(storedGross) == 0;
        boolean grossGivesRate = storedGross.divide(quantity, 4, RoundingMode.HALF_UP)
                .compareTo(storedRate.setScale(4, RoundingMode.HALF_UP)) == 0;
        return !rateReproducesGross && grossGivesRate;
    }
}

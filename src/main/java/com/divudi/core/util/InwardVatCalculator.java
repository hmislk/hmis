package com.divudi.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure arithmetic for the inward final-bill VAT (issue #17491), kept free of
 * beans so the bill-level-discount split and the rounding can be unit tested.
 * All money maths is BigDecimal; each row's VAT is rounded to 2 decimals
 * HALF_UP.
 */
public final class InwardVatCalculator {

    private InwardVatCalculator() {
    }

    /**
     * The part of the bill-level discount that falls on one row: the discount
     * spread over all rows in proportion to each row's net. Never negative; zero
     * when there is no discount, no net on the row, or no net on the bill.
     *
     * @param billDiscount the single bill-level discount amount
     * @param rowNet the row's net (total - discount - charge type discount)
     * @param sumOfNets the sum of the nets of all rows with a net above zero
     */
    public static BigDecimal billDiscountShare(BigDecimal billDiscount, BigDecimal rowNet, BigDecimal sumOfNets) {
        if (billDiscount == null || rowNet == null || sumOfNets == null
                || billDiscount.signum() <= 0 || rowNet.signum() <= 0 || sumOfNets.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return billDiscount.multiply(rowNet).divide(sumOfNets, 10, RoundingMode.HALF_UP);
    }

    /**
     * The amount of a row that is taxed: its net, less the items that carry
     * their own VAT, less its share of the bill-level discount; never below 0.
     */
    public static BigDecimal taxableBase(BigDecimal rowNet, BigDecimal ownVatItemsAmount, BigDecimal billDiscountShare) {
        BigDecimal base = rowNet;
        if (ownVatItemsAmount != null) {
            base = base.subtract(ownVatItemsAmount);
        }
        if (billDiscountShare != null) {
            base = base.subtract(billDiscountShare);
        }
        return base.signum() < 0 ? BigDecimal.ZERO : base;
    }

    /**
     * VAT on a taxable base at a percentage rate, rounded to 2 decimals HALF_UP.
     */
    public static BigDecimal vatOnBase(BigDecimal base, double ratePercent) {
        return base.multiply(BigDecimal.valueOf(ratePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

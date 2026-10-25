package com.divudi.bean.channel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Container-free tendered-amount validation shared by the channel booking and
 * on-call settlement flows of the by-date and by-month controllers (#21953).
 * Kept separate so it can be unit tested.
 */
public final class ChannelTenderedAmountRules {

    // Plain decimals only: rejects exponents, NaN, Infinity, hex floats and signs,
    // and caps the length so parsing can never get expensive.
    private static final Pattern PLAIN_AMOUNT = Pattern.compile("\\d{1,12}(\\.\\d{0,6})?|\\.\\d{1,6}");

    private ChannelTenderedAmountRules() {
    }

    /**
     * @param tendered the value the cashier typed, may be null or blank
     * @param payable the amount the bill records as paid
     * @param required true when a blank tendered amount must be rejected
     * @return the error message to show, or null when the tendered amount is acceptable
     */
    public static String tenderedAmountError(String tendered, double payable, boolean required) {
        String value = tendered == null ? "" : tendered.trim();
        if (value.isEmpty()) {
            return required ? "Please Enter Tenderd Amount" : null;
        }
        if (!PLAIN_AMOUNT.matcher(value).matches()) {
            return "Please enter a valid tendered amount.";
        }
        // Tendered is compared unrounded, so 1999.995 does not pass for 2000.00.
        BigDecimal tenderedAmount = new BigDecimal(value);
        BigDecimal payableAmount = BigDecimal.valueOf(payable).setScale(2, RoundingMode.HALF_UP);
        if (tenderedAmount.compareTo(payableAmount) < 0) {
            return "Please enter the correct tendered amount. The payable amount is " + payableAmount.toPlainString() + ".";
        }
        return null;
    }
}

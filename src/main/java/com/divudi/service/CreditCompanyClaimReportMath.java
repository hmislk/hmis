package com.divudi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Pure arithmetic for the claim reports (#25847): age in days, age bucket and
 * money rounding. No database or session access, so it is unit tested.
 */
public final class CreditCompanyClaimReportMath {

    /** Age bands of the Claim Ageing report. */
    public enum AgeBucket {
        DAYS_0_30("0-30"),
        DAYS_31_60("31-60"),
        DAYS_61_90("61-90"),
        OVER_90("Over 90");

        private final String label;

        AgeBucket(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private CreditCompanyClaimReportMath() {
    }

    /** Whole days from {@code since} to {@code asOf}; never negative; null {@code since} gives 0. */
    public static long ageInDays(Date since, Date asOf) {
        if (since == null || asOf == null || !asOf.after(since)) {
            return 0;
        }
        return TimeUnit.MILLISECONDS.toDays(asOf.getTime() - since.getTime());
    }

    public static AgeBucket bucketOfDays(long days) {
        if (days <= 30) {
            return AgeBucket.DAYS_0_30;
        }
        if (days <= 60) {
            return AgeBucket.DAYS_31_60;
        }
        if (days <= 90) {
            return AgeBucket.DAYS_61_90;
        }
        return AgeBucket.OVER_90;
    }

    public static AgeBucket bucketOf(Date since, Date asOf) {
        return bucketOfDays(ageInDays(since, asOf));
    }

    /** Money to 2 places, half-up, the same as the bill due. */
    public static double money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Received or closed = claimed less outstanding, to 2 places. */
    public static double receivedOrClosed(double claimed, double outstanding) {
        return BigDecimal.valueOf(claimed).subtract(BigDecimal.valueOf(outstanding))
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

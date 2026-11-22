package com.divudi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.divudi.service.CreditCompanyClaimReportMath.AgeBucket;
import java.util.Date;
import org.junit.jupiter.api.Test;

class CreditCompanyClaimReportMathTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final Date NOW = new Date(1_700_000_000_000L);

    private static Date daysAgo(long days) {
        return new Date(NOW.getTime() - days * DAY);
    }

    @Test
    void bucketEdges() {
        assertEquals(AgeBucket.DAYS_0_30, CreditCompanyClaimReportMath.bucketOf(daysAgo(0), NOW));
        assertEquals(AgeBucket.DAYS_0_30, CreditCompanyClaimReportMath.bucketOf(daysAgo(30), NOW));
        assertEquals(AgeBucket.DAYS_31_60, CreditCompanyClaimReportMath.bucketOf(daysAgo(31), NOW));
        assertEquals(AgeBucket.DAYS_31_60, CreditCompanyClaimReportMath.bucketOf(daysAgo(60), NOW));
        assertEquals(AgeBucket.DAYS_61_90, CreditCompanyClaimReportMath.bucketOf(daysAgo(61), NOW));
        assertEquals(AgeBucket.DAYS_61_90, CreditCompanyClaimReportMath.bucketOf(daysAgo(90), NOW));
        assertEquals(AgeBucket.OVER_90, CreditCompanyClaimReportMath.bucketOf(daysAgo(91), NOW));
        assertEquals(AgeBucket.OVER_90, CreditCompanyClaimReportMath.bucketOf(daysAgo(400), NOW));
    }

    @Test
    void partDayDoesNotCountAsADay() {
        assertEquals(30, CreditCompanyClaimReportMath.ageInDays(new Date(NOW.getTime() - 31 * DAY + 1000), NOW));
        assertEquals(AgeBucket.DAYS_0_30,
                CreditCompanyClaimReportMath.bucketOf(new Date(NOW.getTime() - 31 * DAY + 1000), NOW));
    }

    @Test
    void futureOrMissingDateIsAgeZero() {
        assertEquals(0, CreditCompanyClaimReportMath.ageInDays(new Date(NOW.getTime() + DAY), NOW));
        assertEquals(0, CreditCompanyClaimReportMath.ageInDays(null, NOW));
    }

    @Test
    void receivedIsClaimedLessOutstanding() {
        assertEquals(0.10, CreditCompanyClaimReportMath.receivedOrClosed(1.10, 1.00), 0.0);
        assertEquals(60.0, CreditCompanyClaimReportMath.receivedOrClosed(100.0, 40.0), 0.0);
        assertEquals(1.01, CreditCompanyClaimReportMath.money(1.005 + 0.0000001), 0.0);
    }
}

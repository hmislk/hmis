package com.divudi.bean.inward;

import com.divudi.core.data.inward.TimedItemDurationUnit;
import com.divudi.core.entity.inward.TimedItemFee;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers issue #23206 — a timed service fee can be charged one time, per
 * minute, per hour or per day, and every existing (unit-less) fee keeps its
 * hour-based behaviour.
 */
class TimedItemDurationUnitCalculationTest {

    private static final long MINUTE_MS = 60L * 1000L;

    private final InwardBeanController inwardBean = new InwardBeanController();

    private TimedItemFee fee(double duration, double overShoot, TimedItemDurationUnit unit) {
        TimedItemFee f = new TimedItemFee();
        f.setDurationHours(duration);
        f.setOverShootHours(overShoot);
        f.setDurationUnit(unit);
        return f;
    }

    private Date[] span(long minutes) {
        Date from = new Date(0L);
        Date to = new Date(minutes * MINUTE_MS);
        return new Date[]{from, to};
    }

    private double count(TimedItemFee f, long elapsedMinutes) {
        Date[] s = span(elapsedMinutes);
        return inwardBean.calCount(f, s[0], s[1]);
    }

    // ---------- unit conversion on the entity ----------

    @Test
    void unsetUnitReadsAsHour() {
        TimedItemFee f = fee(2, 0, null);
        assertEquals(TimedItemDurationUnit.HOUR, f.getDurationUnit());
        assertEquals(120.0, f.getDurationMinutes(), 0.0001);
        assertFalse(f.isOneTime());
    }

    @Test
    void eachUnitConvertsToMinutes() {
        assertEquals(30.0, fee(30, 0, TimedItemDurationUnit.MINUTE).getDurationMinutes(), 0.0001);
        assertEquals(120.0, fee(2, 0, TimedItemDurationUnit.HOUR).getDurationMinutes(), 0.0001);
        assertEquals(1440.0, fee(1, 0, TimedItemDurationUnit.DAY).getDurationMinutes(), 0.0001);
        assertEquals(0.0, fee(5, 0, TimedItemDurationUnit.ONE_TIME).getDurationMinutes(), 0.0001);
    }

    @Test
    void overShootUsesTheSameUnitAsTheBlock() {
        assertEquals(15.0, fee(60, 15, TimedItemDurationUnit.MINUTE).getOverShootMinutes(), 0.0001);
        assertEquals(720.0, fee(1, 0.5, TimedItemDurationUnit.DAY).getOverShootMinutes(), 0.0001);
    }

    @Test
    void durationInHoursHonoursTheUnit() {
        assertEquals(24.0, fee(1, 0, TimedItemDurationUnit.DAY).getDurationInHours(), 0.0001);
        assertEquals(0.5, fee(30, 0, TimedItemDurationUnit.MINUTE).getDurationInHours(), 0.0001);
    }

    // ---------- existing hour-based behaviour is unchanged ----------

    @Test
    void legacyHourFeeCountsWholeHourBlocks() {
        // 1-hour block, no overshoot, 150 minutes used -> 2 whole blocks.
        assertEquals(2.0, count(fee(1, 0, null), 150), 0.0001);
    }

    @Test
    void legacyHourFeeChargesOneBlockForAShortStay() {
        // Under a full block still charges one block (count == 0 branch).
        assertEquals(1.0, count(fee(1, 0, null), 20), 0.0001);
    }

    @Test
    void hourUnitMatchesTheLegacyNullUnit() {
        assertEquals(count(fee(1, 0, null), 150),
                count(fee(1, 0, TimedItemDurationUnit.HOUR), 150), 0.0001);
    }

    // ---------- new units ----------

    @Test
    void perMinuteFeeChargesEveryMinuteUsed() {
        // The issue's worked example: Rs. 10 per minute for 30 minutes = Rs. 300.
        TimedItemFee f = fee(1, 0, TimedItemDurationUnit.MINUTE);
        f.setFee(10.0);
        double blocks = count(f, 30);
        assertEquals(30.0, blocks, 0.0001);
        assertEquals(300.0, blocks * f.getFee(), 0.0001);
    }

    @Test
    void perMinuteFeeWithAMultiMinuteBlock() {
        // 15-minute blocks, 50 minutes used -> 3 whole blocks (no overshoot configured).
        assertEquals(3.0, count(fee(15, 0, TimedItemDurationUnit.MINUTE), 50), 0.0001);
    }

    @Test
    void perDayFeeCountsWholeDayBlocks() {
        // 1-day block, 36 hours used -> 1 whole day, remainder not charged without overshoot.
        assertEquals(1.0, count(fee(1, 0, TimedItemDurationUnit.DAY), 36 * 60), 0.0001);
    }

    @Test
    void perDayOverShootIsCountedInDays() {
        // 1-day block with a half-day grace; 36 hours leaves a 12-hour remainder,
        // which reaches the grace window and so charges a second day.
        assertEquals(2.0, count(fee(1, 0.5, TimedItemDurationUnit.DAY), 36 * 60), 0.0001);
    }

    @Test
    void oneTimeFeeChargesExactlyOnceHoweverLongItRan() {
        TimedItemFee f = fee(0, 0, TimedItemDurationUnit.ONE_TIME);
        assertTrue(f.isOneTime());
        assertEquals(1.0, count(f, 5), 0.0001);
        assertEquals(1.0, count(f, 60 * 24 * 7), 0.0001);
    }

    @Test
    void oneTimeFeeIgnoresALeftoverDurationValue() {
        // Switching a configured fee back to One Time on the fee page leaves the
        // previous duration sitting on the row, so a one-time fee with a non-zero
        // duration is a state the UI can produce. It must still charge once.
        TimedItemFee f = fee(2, 1, TimedItemDurationUnit.ONE_TIME);
        assertEquals(0.0, f.getDurationMinutes(), 0.0001);
        assertEquals(0.0, f.getOverShootMinutes(), 0.0001);
        assertEquals(1.0, count(f, 500), 0.0001);
    }

    @Test
    void oneTimeFeeIsChargedEvenWithNoElapsedTime() {
        // calCount returns 0 for a zero-length span on time-based fees; a one-time
        // fee is not time-based, so it must still be charged.
        assertEquals(1.0, count(fee(0, 0, TimedItemDurationUnit.ONE_TIME), 0), 0.0001);
        assertEquals(0.0, count(fee(1, 0, TimedItemDurationUnit.HOUR), 0), 0.0001);
    }

    @Test
    void aMissingFeeCountsAsNothingToBillRatherThanThrowing() {
        // RoomFacilityCharge.timedItemFee is a nullable mapping, so the room paths
        // can hand these methods a null fee. That has to behave like an
        // unconfigured fee (count 0), not break the page rendering the bill.
        Date[] s = span(150);
        assertEquals(0.0, inwardBean.calCount(null, s[0], s[1]), 0.0001);
        assertEquals(0.0, inwardBean.calCountWithoutOverShoot(null, s[0], s[1]), 0.0001);
    }

    @Test
    void calCountWithoutOverShootAlsoHonoursUnits() {
        Date[] halfHour = span(30);
        assertEquals(30.0,
                inwardBean.calCountWithoutOverShoot(fee(1, 0, TimedItemDurationUnit.MINUTE), halfHour[0], halfHour[1]),
                0.0001);
        assertEquals(1.0,
                inwardBean.calCountWithoutOverShoot(fee(0, 0, TimedItemDurationUnit.ONE_TIME), halfHour[0], halfHour[1]),
                0.0001);
    }
}

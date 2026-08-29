package com.divudi.service;

import com.divudi.core.data.ScheduledFrequency;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ScheduledProcessServiceTest {

    private final ScheduledProcessService service = new ScheduledProcessService();

    // per-test timezone constant instead of mutating JVM-wide default
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Test
    public void hourlyAddsOneHour() {
        Date from = new GregorianCalendar(2024, Calendar.JUNE, 1, 10, 0, 0).getTime();
        Date expected = new GregorianCalendar(2024, Calendar.JUNE, 1, 11, 0, 0).getTime();
        assertEquals(expected, service.calculateNextSupposedAt(ScheduledFrequency.Hourly, from));
    }

    @Test
    public void midnightMovesToNextMidnight() {
        Date from = new GregorianCalendar(2024, Calendar.JUNE, 1, 15, 0, 0).getTime();
        Date expected = new GregorianCalendar(2024, Calendar.JUNE, 2, 0, 0, 0).getTime();
        assertEquals(expected, service.calculateNextSupposedAt(ScheduledFrequency.Midnight, from));
    }

    @Test
    public void weekEndMovesToNextSaturday() {
        Date from = new GregorianCalendar(2024, Calendar.SEPTEMBER, 30, 13, 0, 0).getTime(); // Monday
        Date expected = new GregorianCalendar(2024, Calendar.OCTOBER, 5, 0, 0, 0).getTime(); // Saturday
        assertEquals(expected, service.calculateNextSupposedAt(ScheduledFrequency.WeekEnd, from));
    }

    @Test
    public void monthEndMovesToEndOfNextMonth() {
        Date from = new GregorianCalendar(2024, Calendar.JUNE, 15, 12, 0, 0).getTime();
        Date expected = new GregorianCalendar(2024, Calendar.JULY, 31, 0, 0, 0).getTime();
        assertEquals(expected, service.calculateNextSupposedAt(ScheduledFrequency.MonthEnd, from));
    }

    @Test
    public void monthEndHandlesJanuaryEdge() {
        Date from = new GregorianCalendar(2023, Calendar.JANUARY, 31, 8, 0, 0).getTime();
        Date expected = new GregorianCalendar(2023, Calendar.FEBRUARY, 28, 0, 0, 0).getTime();
        assertEquals(expected, service.calculateNextSupposedAt(ScheduledFrequency.MonthEnd, from));
    }

    @Test
    public void monthEndHandlesLeapYear() {
        Date from = new GregorianCalendar(2024, Calendar.JANUARY, 31, 8, 0, 0).getTime();
        Date expected = new GregorianCalendar(2024, Calendar.FEBRUARY, 29, 0, 0, 0).getTime();
        assertEquals(expected, service.calculateNextSupposedAt(ScheduledFrequency.MonthEnd, from));
    }

    @Test
    public void yearEndMovesToEndOfNextYear() {
        Date from = new GregorianCalendar(2024, Calendar.DECEMBER, 31, 0, 0, 0).getTime();
        Date expected = new GregorianCalendar(2025, Calendar.DECEMBER, 31, 0, 0, 0).getTime();
        assertEquals(expected, service.calculateNextSupposedAt(ScheduledFrequency.YearEnd, from));
    }
}

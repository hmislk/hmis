package com.divudi.core.data.inward;

import com.divudi.bean.inward.InwardBeanController;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.AdmissionTypeHistory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Admission type in force at a date, and the split of a stay by type (#25814).
 * An admission with no history must give exactly the single-type answers.
 */
class AdmissionTypeTimelineTest {

    private static Date d(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s);
    }

    private static AdmissionType type(long id, String name, double linenPerDay, double fee) {
        AdmissionType t = new AdmissionType();
        t.setId(id);
        t.setName(name);
        t.setDblValue(linenPerDay);
        t.setAdmissionFee(fee);
        return t;
    }

    private static AdmissionTypeHistory row(AdmissionType t, Date from, Date to) {
        AdmissionTypeHistory h = new AdmissionTypeHistory();
        h.setAdmissionType(t);
        h.setFromDate(from);
        h.setToDate(to);
        return h;
    }

    @Test
    void noHistoryGivesCurrentTypeForEveryDate() throws Exception {
        AdmissionType inward = type(1, "Inward", 100, 500);
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, new ArrayList<>());
        assertFalse(tl.hasHistory());
        assertSame(inward, tl.typeAt(d("2026-01-01 10:00")));
        assertSame(inward, tl.typeAt(null));
        assertSame(inward, tl.firstType());
        assertEquals(1, tl.periods().size());
        assertNull(tl.periods().get(0).getFromDate());
        assertNull(tl.periods().get(0).getToDate());
    }

    @Test
    void noHistoryLinenEqualsSingleTypeFormula() throws Exception {
        AdmissionType inward = type(1, "Inward", 100, 500);
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, null);
        // 5 calendar days, 3rd to 7th, any time of day
        assertEquals(500.0, InwardBeanController.linenForDays(tl, d("2026-03-03 15:30"), d("2026-03-07 08:00")), 0.0001);
        // admitted and discharged the same day counts as one day
        assertEquals(100.0, InwardBeanController.linenForDays(tl, d("2026-03-03 08:00"), d("2026-03-03 20:00")), 0.0001);
    }

    @Test
    void typeAtFollowsTheExactChangeTime() throws Exception {
        AdmissionType etu = type(1, "ETU", 0, 0);
        AdmissionType inward = type(2, "Inward", 100, 500);
        Date admitted = d("2026-03-01 09:00");
        Date changed = d("2026-03-03 14:00");
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(inward, changed, null), row(etu, admitted, changed)));
        assertTrue(tl.hasHistory());
        assertSame(etu, tl.firstType());
        assertSame(etu, tl.typeAt(d("2026-03-01 09:00")));
        assertSame(etu, tl.typeAt(d("2026-03-02 23:59")));
        assertSame(etu, tl.typeAt(d("2026-03-03 00:00")));
        assertSame(etu, tl.typeAt(d("2026-03-03 09:00")));
        assertSame(inward, tl.typeAt(d("2026-03-03 14:00")));
        assertSame(inward, tl.typeAt(d("2026-03-10 00:00")));
        // before the first row (e.g. admission time edited later) the first type applies
        assertSame(etu, tl.typeAt(d("2026-02-28 00:00")));
        assertEquals(2, tl.periods().size());
        assertSame(etu, tl.periods().get(0).getAdmissionType());
        assertNull(tl.periods().get(0).getFromDate());
        assertEquals(changed, tl.periods().get(0).getToDate());
        assertEquals(changed, tl.periods().get(1).getFromDate());
        assertNull(tl.periods().get(1).getToDate());
        assertTrue(tl.periods().get(0).contains(d("2026-03-02 00:00")));
        assertFalse(tl.periods().get(0).contains(changed));
        assertTrue(tl.periods().get(1).contains(changed));
    }

    @Test
    void feeBilledOnTheChangeDayFollowsTheExactChangeTime() throws Exception {
        AdmissionType etu = type(1, "ETU", 0, 0);
        AdmissionType inward = type(2, "Inward", 100, 500);
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(etu, d("2026-03-01 09:00"), d("2026-03-03 14:00")), row(inward, d("2026-03-03 14:00"), null)));
        // typeAt is what the margin and bill-fee discount lookups call
        assertSame(etu, tl.typeAt(d("2026-03-03 09:00")));
        assertSame(etu, tl.typeAt(d("2026-03-03 13:59")));
        assertSame(inward, tl.typeAt(d("2026-03-03 14:00")));
        assertSame(inward, tl.typeAt(d("2026-03-03 15:00")));
        assertSame(etu, tl.typeAt(d("2026-03-02 23:59")));
    }

    @Test
    void periodWindowsPickEachFeeByItsBillTimeLikeTheDiscountSplit() throws Exception {
        AdmissionType etu = type(1, "ETU", 0, 0);
        AdmissionType inward = type(2, "Inward", 100, 500);
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(etu, d("2026-03-01 09:00"), d("2026-03-03 14:00")), row(inward, d("2026-03-03 14:00"), null)));
        Date billedBefore = d("2026-03-02 23:59");
        Date billedChangeDayMorning = d("2026-03-03 09:00");
        List<AdmissionTypeTimeline.Period> periods = tl.periods();
        AdmissionTypeTimeline.Period oldP = periods.get(0);
        AdmissionTypeTimeline.Period newP = periods.get(1);
        // the helper used by BhtSummeryController.discountSetByPeriods and the fee loops
        assertTrue(com.divudi.bean.inward.BhtSummeryController.inWindow(billedBefore, oldP.getFromDate(), oldP.getToDate()));
        assertFalse(com.divudi.bean.inward.BhtSummeryController.inWindow(billedBefore, newP.getFromDate(), newP.getToDate()));
        assertTrue(com.divudi.bean.inward.BhtSummeryController.inWindow(billedChangeDayMorning, oldP.getFromDate(), oldP.getToDate()));
        assertFalse(com.divudi.bean.inward.BhtSummeryController.inWindow(billedChangeDayMorning, newP.getFromDate(), newP.getToDate()));
        Date billedChangeDayAfternoon = d("2026-03-03 15:00");
        assertFalse(com.divudi.bean.inward.BhtSummeryController.inWindow(billedChangeDayAfternoon, oldP.getFromDate(), oldP.getToDate()));
        assertTrue(com.divudi.bean.inward.BhtSummeryController.inWindow(billedChangeDayAfternoon, newP.getFromDate(), newP.getToDate()));
    }

    @Test
    void roomDiscountTypeIsTheTypeAtRoomStartForTheWholeRoomStay() throws Exception {
        AdmissionType etu = type(1, "ETU", 0, 0);
        AdmissionType inward = type(2, "Inward", 100, 500);
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(etu, d("2026-03-01 09:00"), d("2026-03-03 14:00")), row(inward, d("2026-03-03 14:00"), null)));
        // product owner: room discounts and margin use typeAt(room start), as the room loops call it
        Date roomEnteredBeforeChangeDay = d("2026-03-02 22:00");
        Date roomEnteredOnChangeDay = d("2026-03-03 08:00");
        Date roomEnteredAfter = d("2026-03-04 10:00");
        assertSame(etu, tl.typeAt(roomEnteredBeforeChangeDay));
        assertSame(etu, tl.typeAt(roomEnteredOnChangeDay));
        assertSame(inward, tl.typeAt(d("2026-03-03 15:00")));
        assertSame(inward, tl.typeAt(roomEnteredAfter));
    }

    @Test
    void changeOnTheAdmissionDaySplitsThatDayAtTheChangeTime() throws Exception {
        AdmissionType etu = type(1, "ETU", 0, 0);
        AdmissionType inward = type(2, "Inward", 100, 500);
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(etu, d("2026-03-01 09:00"), d("2026-03-01 15:00")), row(inward, d("2026-03-01 15:00"), null)));
        assertEquals(2, tl.periods().size());
        assertSame(etu, tl.typeAt(d("2026-03-01 10:00")));
        assertSame(inward, tl.typeAt(d("2026-03-01 16:00")));
        assertSame(etu, tl.firstType());
    }

    @Test
    void linenSplitsTwoDaysOldTypeAndThreeDaysNewType() throws Exception {
        AdmissionType etu = type(1, "ETU", 40, 0);
        AdmissionType inward = type(2, "Inward", 100, 500);
        Date admitted = d("2026-03-01 09:00");
        // changed at the very start of the 3rd calendar day
        Date changed = d("2026-03-03 00:00");
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(etu, admitted, changed), row(inward, changed, null)));
        // stay 1 to 5 March: 5 days = 2 days ETU (1,2 March) + 3 days Inward (3,4,5 March)
        List<AdmissionTypeTimeline.TypeDays> split = tl.splitDays(admitted, d("2026-03-05 11:00"));
        assertEquals(2, split.size());
        assertSame(etu, split.get(0).getAdmissionType());
        assertEquals(2, split.get(0).getDays());
        assertSame(inward, split.get(1).getAdmissionType());
        assertEquals(3, split.get(1).getDays());
        assertEquals(2 * 40.0 + 3 * 100.0, InwardBeanController.linenForDays(tl, admitted, d("2026-03-05 11:00")), 0.0001);
    }

    @Test
    void changeDayLinenIsChargedAtTheOldType() throws Exception {
        AdmissionType etu = type(1, "ETU", 40, 0);
        AdmissionType inward = type(2, "Inward", 100, 500);
        Date admitted = d("2026-03-01 09:00");
        Date changed = d("2026-03-03 14:00");
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(etu, admitted, changed), row(inward, changed, null)));
        // 1,2,3 March (3 March is the change day) at ETU; 4,5 March at Inward
        List<AdmissionTypeTimeline.TypeDays> split = tl.splitDays(admitted, d("2026-03-05 11:00"));
        assertEquals(2, split.size());
        assertEquals(3, split.get(0).getDays());
        assertEquals(2, split.get(1).getDays());
        assertEquals(3 * 40.0 + 2 * 100.0, InwardBeanController.linenForDays(tl, admitted, d("2026-03-05 11:00")), 0.0001);
    }

    @Test
    void twoChangesOnTheSameDayLinenStaysOldForThatDay() throws Exception {
        AdmissionType a = type(1, "A", 10, 0);
        AdmissionType b = type(2, "B", 20, 0);
        AdmissionType c = type(3, "C", 30, 0);
        Date admitted = d("2026-03-01 09:00");
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(c, Arrays.asList(
                row(a, admitted, d("2026-03-03 08:00")),
                row(b, d("2026-03-03 08:00"), d("2026-03-03 16:00")),
                row(c, d("2026-03-03 16:00"), null)));
        // 1,2,3 March A (3 March is the change day); 4,5 March C
        List<AdmissionTypeTimeline.TypeDays> split = tl.splitDays(admitted, d("2026-03-05 11:00"));
        assertEquals(2, split.size());
        assertSame(a, split.get(0).getAdmissionType());
        assertEquals(3, split.get(0).getDays());
        assertSame(c, split.get(1).getAdmissionType());
        assertEquals(2, split.get(1).getDays());
        // dated charges follow the exact times
        assertEquals(3, tl.periods().size());
        assertSame(a, tl.typeAt(d("2026-03-03 07:00")));
        assertSame(b, tl.typeAt(d("2026-03-03 09:00")));
        assertSame(c, tl.typeAt(d("2026-03-03 17:00")));
    }

    @Test
    void daysAlwaysAddUpToTheSingleTypeDayCount() throws Exception {
        AdmissionType a = type(1, "A", 10, 0);
        AdmissionType b = type(2, "B", 20, 0);
        AdmissionType c = type(3, "C", 30, 0);
        Date admitted = d("2026-03-01 09:00");
        AdmissionTypeTimeline tl = new AdmissionTypeTimeline(c, Arrays.asList(
                row(a, admitted, d("2026-03-02 10:00")),
                row(b, d("2026-03-02 10:00"), d("2026-03-04 10:00")),
                row(c, d("2026-03-04 10:00"), null)));
        long total = 0;
        for (AdmissionTypeTimeline.TypeDays td : tl.splitDays(admitted, d("2026-03-06 09:00"))) {
            total += td.getDays();
        }
        assertEquals(com.divudi.core.util.CommonFunctions.getDayCount(admitted, d("2026-03-06 09:00")).longValue(), total);
        assertEquals(10.0 * 2 + 20.0 * 2 + 30.0 * 2, InwardBeanController.linenForDays(tl, admitted, d("2026-03-06 09:00")), 0.0001);
    }

    @Test
    void admissionFeeFollowsTheFirstType() throws Exception {
        AdmissionType etu = type(1, "ETU", 0, 250);
        AdmissionType inward = type(2, "Inward", 100, 500);
        Date admitted = d("2026-03-01 09:00");
        AdmissionTypeTimeline changed = new AdmissionTypeTimeline(inward, Arrays.asList(
                row(etu, admitted, d("2026-03-03 14:00")), row(inward, d("2026-03-03 14:00"), null)));
        assertEquals(250.0, changed.firstType().getAdmissionFee(), 0.0001);
        AdmissionTypeTimeline unchanged = new AdmissionTypeTimeline(inward, null);
        assertEquals(500.0, unchanged.firstType().getAdmissionFee(), 0.0001);
    }
}

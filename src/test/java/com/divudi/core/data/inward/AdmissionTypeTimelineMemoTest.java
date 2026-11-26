package com.divudi.core.data.inward;

import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.AdmissionTypeHistory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The remembered timeline gives the same type at every date as the timeline the
 * per-call lookup builds, and reads the history once per admission (#17562).
 */
class AdmissionTypeTimelineMemoTest {

    private static Date d(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s);
    }

    private static AdmissionType type(long id, String name) {
        AdmissionType t = new AdmissionType();
        t.setId(id);
        t.setName(name);
        return t;
    }

    private static AdmissionTypeHistory row(AdmissionType t, Date from) {
        AdmissionTypeHistory h = new AdmissionTypeHistory();
        h.setAdmissionType(t);
        h.setFromDate(from);
        return h;
    }

    private static PatientEncounter encounter(long id, AdmissionType current) {
        PatientEncounter e = new PatientEncounter();
        e.setId(id);
        e.setAdmissionType(current);
        return e;
    }

    @Test
    void sameAnswerAsPerCallLookupAndOneRead() throws Exception {
        AdmissionType a = type(1, "A");
        AdmissionType b = type(2, "B");
        AdmissionType c = type(3, "C");
        List<AdmissionTypeHistory> rows = new ArrayList<>(Arrays.asList(
                row(a, d("2026-01-01 08:00")),
                row(b, d("2026-01-05 00:00")),
                row(c, d("2026-01-10 12:00"))));
        PatientEncounter enc = encounter(7, c);
        AtomicInteger reads = new AtomicInteger();
        AdmissionTypeTimelineMemo memo = new AdmissionTypeTimelineMemo(e -> {
            reads.incrementAndGet();
            return new AdmissionTypeTimeline(e.getAdmissionType(), rows);
        });

        String[] dates = {
            "2025-12-31 23:59", // before the first entry
            "2026-01-01 08:00", // exactly at the first entry
            "2026-01-03 10:00", // between entries
            "2026-01-05 00:00", // exactly at an entry
            "2026-01-10 11:59", // just before the last entry
            "2026-01-10 12:00", // exactly at the last entry
            "2026-03-01 00:00"  // after the last entry
        };
        for (int round = 0; round < 3; round++) {
            for (String s : dates) {
                AdmissionType expected = new AdmissionTypeTimeline(c, rows).typeAt(d(s));
                assertSame(expected, memo.typeAt(enc, d(s)), s);
            }
            assertSame(a, memo.firstType(enc));
        }
        assertSame(a, memo.typeAt(enc, d("2025-12-31 23:59")));
        assertSame(c, memo.typeAt(enc, d("2026-03-01 00:00")));
        assertEquals(1, reads.get());
    }

    @Test
    void equalDatesKeepTheSameTieBreakAsTheDirectTimeline() throws Exception {
        AdmissionType a = type(1, "A");
        AdmissionType b = type(2, "B");
        AdmissionType c = type(3, "C");
        List<AdmissionTypeHistory> rows = new ArrayList<>(Arrays.asList(
                row(a, d("2026-01-01 08:00")),
                row(b, d("2026-01-05 00:00")),
                row(c, d("2026-01-05 00:00"))));
        PatientEncounter enc = encounter(8, c);
        AdmissionTypeTimelineMemo memo = new AdmissionTypeTimelineMemo(
                e -> new AdmissionTypeTimeline(e.getAdmissionType(), rows));
        for (String s : new String[]{"2026-01-04 23:59", "2026-01-05 00:00", "2026-01-06 00:00"}) {
            assertSame(new AdmissionTypeTimeline(c, rows).typeAt(d(s)), memo.typeAt(enc, d(s)), s);
        }
    }

    @Test
    void noHistoryAnswersCurrentTypeAndOneReadPerAdmission() throws Exception {
        AdmissionType a = type(1, "A");
        AtomicInteger reads = new AtomicInteger();
        AdmissionTypeTimelineMemo memo = new AdmissionTypeTimelineMemo(e -> {
            reads.incrementAndGet();
            return new AdmissionTypeTimeline(e.getAdmissionType(), new ArrayList<>());
        });
        PatientEncounter one = encounter(1, a);
        PatientEncounter two = encounter(2, a);
        for (int i = 0; i < 5; i++) {
            assertSame(a, memo.typeAt(one, d("2026-01-01 00:00")));
            assertSame(a, memo.typeAt(two, d("2026-01-01 00:00")));
        }
        assertEquals(2, reads.get());
    }

    @Test
    void changedCurrentTypeOrClearReadsAgain() throws Exception {
        AdmissionType a = type(1, "A");
        AdmissionType b = type(2, "B");
        AtomicInteger reads = new AtomicInteger();
        AdmissionTypeTimelineMemo memo = new AdmissionTypeTimelineMemo(e -> {
            reads.incrementAndGet();
            return new AdmissionTypeTimeline(e.getAdmissionType(), new ArrayList<>());
        });
        PatientEncounter enc = encounter(1, a);
        memo.typeAt(enc, d("2026-01-01 00:00"));
        enc.setAdmissionType(b);
        assertSame(b, memo.typeAt(enc, d("2026-01-01 00:00")));
        assertEquals(2, reads.get());
        memo.clear();
        memo.typeAt(enc, d("2026-01-01 00:00"));
        assertEquals(3, reads.get());
    }

    @Test
    void unsavedAdmissionIsNeverKept() throws Exception {
        AdmissionType a = type(1, "A");
        AtomicInteger reads = new AtomicInteger();
        AdmissionTypeTimelineMemo memo = new AdmissionTypeTimelineMemo(e -> {
            reads.incrementAndGet();
            return new AdmissionTypeTimeline(e == null ? null : e.getAdmissionType(), new ArrayList<>());
        });
        PatientEncounter enc = new PatientEncounter();
        enc.setAdmissionType(a);
        memo.typeAt(enc, d("2026-01-01 00:00"));
        memo.typeAt(enc, d("2026-01-01 00:00"));
        assertEquals(2, reads.get());
    }
}

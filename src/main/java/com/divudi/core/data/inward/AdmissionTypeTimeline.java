package com.divudi.core.data.inward;

import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.AdmissionTypeHistory;
import com.divudi.core.util.CommonFunctions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Which admission type was in force on a given date for one admission (#25814).
 *
 * <p>One boundary rule for every reader: a change takes effect at the start
 * (00:00) of the calendar day on which it was made, so the whole change day is
 * billed at the new type. Two changes on one day: the last one wins for that
 * day. The history rows keep the real change time for the audit; only this
 * class turns it into the boundary.
 *
 * <p>Built from the admission's current type and its history rows. With no
 * history rows (an admission whose type never changed, and every admission
 * created before this feature) the current type covers the whole stay, so
 * every answer is exactly what the code gave before the history existed.
 *
 * <p>Pure logic, no persistence: the caller loads the rows.
 */
public class AdmissionTypeTimeline {

    /** One stretch of the stay under one admission type; toDate null = open. */
    public static class Period {

        private final AdmissionType admissionType;
        private final Date fromDate;
        private final Date toDate;

        public Period(AdmissionType admissionType, Date fromDate, Date toDate) {
            this.admissionType = admissionType;
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public AdmissionType getAdmissionType() {
            return admissionType;
        }

        public Date getFromDate() {
            return fromDate;
        }

        public Date getToDate() {
            return toDate;
        }

        /** True when the date is inside this period; a null bound is open. */
        public boolean contains(Date date) {
            if (date == null) {
                return false;
            }
            return (fromDate == null || !date.before(fromDate))
                    && (toDate == null || date.before(toDate));
        }
    }

    /** A number of billed days under one admission type. */
    public static class TypeDays {

        private final AdmissionType admissionType;
        private final long days;

        public TypeDays(AdmissionType admissionType, long days) {
            this.admissionType = admissionType;
            this.days = days;
        }

        public AdmissionType getAdmissionType() {
            return admissionType;
        }

        public long getDays() {
            return days;
        }
    }

    private final AdmissionType currentType;
    private final List<AdmissionTypeHistory> rows = new ArrayList<>();
    /** Rows the answers are read from: type and effective start; the first start is null (open). */
    private final List<AdmissionType> effTypes = new ArrayList<>();
    private final List<Date> effFrom = new ArrayList<>();

    public AdmissionTypeTimeline(AdmissionType currentType, List<AdmissionTypeHistory> historyRows) {
        this.currentType = currentType;
        if (historyRows != null) {
            for (AdmissionTypeHistory r : historyRows) {
                if (r != null && r.getAdmissionType() != null && r.getFromDate() != null) {
                    rows.add(r);
                }
            }
            Collections.sort(rows, new Comparator<AdmissionTypeHistory>() {
                @Override
                public int compare(AdmissionTypeHistory a, AdmissionTypeHistory b) {
                    return a.getFromDate().compareTo(b.getFromDate());
                }
            });
        }
        Date lastDay = null;
        for (int i = 0; i < rows.size(); i++) {
            AdmissionTypeHistory r = rows.get(i);
            if (i == 0) {
                effTypes.add(r.getAdmissionType());
                effFrom.add(null);
                lastDay = boundaryOf(r.getFromDate());
                continue;
            }
            Date boundary = boundaryOf(r.getFromDate());
            if (!boundary.after(lastDay)) {
                // the earlier type never had a billable day: the later change wins
                effTypes.set(effTypes.size() - 1, r.getAdmissionType());
            } else {
                effTypes.add(r.getAdmissionType());
                effFrom.add(boundary);
            }
            lastDay = boundary;
        }
    }

    /** The moment a change made at this time takes effect: the start of its calendar day. */
    public static Date boundaryOf(Date changeTime) {
        return CommonFunctions.getStartOfDay(changeTime);
    }

    /** True when the type was changed at least once. */
    public boolean hasHistory() {
        return !rows.isEmpty();
    }

    /** The type in force at the date; a null date or no history gives the current type. */
    public AdmissionType typeAt(Date date) {
        if (date == null || effTypes.isEmpty()) {
            return currentType;
        }
        AdmissionType found = effTypes.get(0);
        for (int i = 1; i < effTypes.size(); i++) {
            if (!effFrom.get(i).after(date)) {
                found = effTypes.get(i);
            } else {
                break;
            }
        }
        return found;
    }

    /** The type the admission started with (the admission fee follows it). */
    public AdmissionType firstType() {
        return rows.isEmpty() ? currentType : rows.get(0).getAdmissionType();
    }

    /** The periods in date order; one open period with the current type when there is no history. */
    public List<Period> periods() {
        List<Period> list = new ArrayList<>();
        if (effTypes.isEmpty()) {
            list.add(new Period(currentType, null, null));
            return list;
        }
        for (int i = 0; i < effTypes.size(); i++) {
            Date to = i + 1 < effTypes.size() ? effFrom.get(i + 1) : null;
            list.add(new Period(effTypes.get(i), effFrom.get(i), to));
        }
        return list;
    }

    /**
     * Splits the billed days of a stay by admission type. The number of days is
     * the same as {@link CommonFunctions#getDayCount} (a stay that counts as 0
     * days is billed as 1). Each calendar day takes the type in force at the end
     * of that day, so the day a change is made is billed at the new type (the
     * last change of that day wins). The days always add up to the single-type
     * day count.
     */
    public List<TypeDays> splitDays(Date from, Date to) {
        long dayCount = CommonFunctions.getDayCount(from, to);
        long billed = dayCount != 0 ? dayCount : 1;
        List<TypeDays> result = new ArrayList<>();
        if (rows.isEmpty() || from == null) {
            result.add(new TypeDays(currentType, billed));
            return result;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(CommonFunctions.getStartOfDay(from));
        AdmissionType runType = null;
        long runDays = 0;
        for (long i = 0; i < billed; i++) {
            AdmissionType t = typeAt(cal.getTime());
            if (runDays > 0 && sameType(runType, t)) {
                runDays++;
            } else {
                if (runDays > 0) {
                    result.add(new TypeDays(runType, runDays));
                }
                runType = t;
                runDays = 1;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (runDays > 0) {
            result.add(new TypeDays(runType, runDays));
        }
        if (result.isEmpty()) {
            result.add(new TypeDays(currentType, billed));
        }
        return result;
    }

    private static boolean sameType(AdmissionType a, AdmissionType b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }
}

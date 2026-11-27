package com.divudi.core.util;

import com.divudi.core.entity.pharmacy.MeasurementUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pure ordering and grouping rules for the dose and duration unit lists of the
 * medicine prescribing form. No JSF or database access, so it can be unit tested.
 */
public final class MeasurementUnitLists {

    private static final Comparator<MeasurementUnit> BY_NAME = Comparator.comparing(
            (MeasurementUnit u) -> u.getName() == null ? "" : u.getName().toLowerCase(Locale.ROOT));

    private MeasurementUnitLists() {
    }

    /**
     * The item's own units sorted by name, followed by the strength units that
     * are not already listed, sorted by name. No duplicates, nulls ignored.
     */
    public static List<MeasurementUnit> doseUnits(Collection<MeasurementUnit> ownUnits,
            Collection<MeasurementUnit> strengthUnits) {
        List<MeasurementUnit> result = sortedDistinct(ownUnits, null);
        result.addAll(sortedDistinct(strengthUnits, result));
        return result;
    }

    /**
     * Time units (days, weeks, ...) sorted by name, followed by dose forms
     * (issue and pack units) sorted by name, not repeating a unit already in the
     * time group. Index 0 is the time group, index 1 is the dose-form group.
     */
    public static List<List<MeasurementUnit>> durationGroups(Collection<MeasurementUnit> timeUnits,
            Collection<MeasurementUnit> doseForms) {
        List<MeasurementUnit> time = sortedDistinct(timeUnits, null);
        List<MeasurementUnit> forms = sortedDistinct(doseForms, time);
        List<List<MeasurementUnit>> groups = new ArrayList<>();
        groups.add(time);
        groups.add(forms);
        return groups;
    }

    private static List<MeasurementUnit> sortedDistinct(Collection<MeasurementUnit> units,
            List<MeasurementUnit> exclude) {
        List<MeasurementUnit> out = new ArrayList<>();
        if (units == null) {
            return out;
        }
        for (MeasurementUnit u : units) {
            if (u == null || out.contains(u) || (exclude != null && exclude.contains(u))) {
                continue;
            }
            out.add(u);
        }
        out.sort(BY_NAME);
        return out;
    }
}

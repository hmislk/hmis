package com.divudi.core.util;

import com.divudi.core.entity.pharmacy.MeasurementUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class MeasurementUnitListsTest {

    private static MeasurementUnit unit(long id, String name) {
        MeasurementUnit u = new MeasurementUnit();
        u.setId(id);
        u.setName(name);
        return u;
    }

    private static List<String> names(List<MeasurementUnit> units) {
        List<String> out = new ArrayList<>();
        for (MeasurementUnit u : units) {
            out.add(u.getName());
        }
        return out;
    }

    @Test
    public void doseUnits_ownFirstThenStrength_sortedAndWithoutDuplicates() {
        MeasurementUnit tablet = unit(1, "Tablet");
        MeasurementUnit mg = unit(2, "mg");
        MeasurementUnit g = unit(3, "g");
        MeasurementUnit ml = unit(4, "ml");
        List<MeasurementUnit> result = MeasurementUnitLists.doseUnits(
                Arrays.asList(tablet, mg, null, tablet), Arrays.asList(ml, mg, g));
        assertEquals(Arrays.asList("mg", "Tablet", "g", "ml"), names(result));
    }

    @Test
    public void doseUnits_nullInputs_giveEmptyList() {
        assertTrue(MeasurementUnitLists.doseUnits(null, null).isEmpty());
    }

    @Test
    public void durationGroups_timeUnitsThenDoseForms_eachSorted() {
        MeasurementUnit days = unit(1, "Days");
        MeasurementUnit weeks = unit(2, "Weeks");
        MeasurementUnit tablet = unit(3, "Tablet");
        MeasurementUnit capsule = unit(4, "Capsule");
        List<List<MeasurementUnit>> groups = MeasurementUnitLists.durationGroups(
                Arrays.asList(weeks, days), Arrays.asList(tablet, capsule, days));
        assertEquals(2, groups.size());
        assertEquals(Arrays.asList("Days", "Weeks"), names(groups.get(0)));
        assertEquals(Arrays.asList("Capsule", "Tablet"), names(groups.get(1)));
    }

    @Test
    public void durationGroups_nothingRemoved_oldDoseFormDurationStillListed() {
        MeasurementUnit tablet = unit(3, "Tablet");
        List<List<MeasurementUnit>> groups = MeasurementUnitLists.durationGroups(
                new ArrayList<MeasurementUnit>(), Arrays.asList(tablet));
        assertTrue(groups.get(0).isEmpty());
        assertTrue(groups.get(1).contains(tablet));
    }
}

package com.divudi.service;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.MaintenanceWindow;
import java.util.Date;
import java.util.List;
import javax.ejb.ApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MaintenanceWindowRulesTest {

    private static final long HOUR = 60L * 60 * 1000;
    private final Date now = new Date(1_700_000_000_000L);

    private MaintenanceWindow window(long startOffsetHours, long endOffsetHours) {
        MaintenanceWindow w = new MaintenanceWindow();
        w.setMessage("System upgrade");
        w.setStartAt(new Date(now.getTime() + startOffsetHours * HOUR));
        w.setEndAt(new Date(now.getTime() + endOffsetHours * HOUR));
        return w;
    }

    private Institution institution(long id) {
        Institution i = new Institution();
        i.setId(id);
        return i;
    }

    private Department department(long id) {
        Department d = new Department();
        d.setId(id);
        return d;
    }

    @Test
    public void exceptionRollsBackAndIsApplicationException() {
        ApplicationException ann = MaintenanceWindowException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ann);
        assertTrue(ann.rollback());
    }

    @Test
    public void validateFillsDefaultShowFromTwentyFourHoursBeforeStart() {
        MaintenanceWindow w = window(48, 50);
        MaintenanceWindowRules.validate(w);
        assertEquals(w.getStartAt().getTime() - 24 * HOUR, w.getShowFromAt().getTime());
    }

    @Test
    public void validateRejectsOverLongTitleMessageAndReference() {
        MaintenanceWindow w = window(1, 2);
        w.setTitle("x".repeat(256));
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(w));
        MaintenanceWindow m = window(1, 2);
        m.setMessage("x".repeat(1001));
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(m));
        MaintenanceWindow r = window(1, 2);
        r.setExternalReference("x".repeat(256));
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(r));
        MaintenanceWindow ok = window(1, 2);
        ok.setTitle("x".repeat(255));
        ok.setMessage("x".repeat(1000));
        MaintenanceWindowRules.validate(ok);
    }

    @Test
    public void validateRejectsBlankMessage() {
        MaintenanceWindow w = window(1, 2);
        w.setMessage("   ");
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(w));
    }

    @Test
    public void validateRejectsEndNotAfterStart() {
        MaintenanceWindow same = window(5, 5);
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(same));
        MaintenanceWindow before = window(5, 4);
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(before));
    }

    @Test
    public void validateRejectsMissingTimes() {
        MaintenanceWindow noStart = window(1, 2);
        noStart.setStartAt(null);
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(noStart));
        MaintenanceWindow noEnd = window(1, 2);
        noEnd.setEndAt(null);
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(noEnd));
    }

    @Test
    public void validateRejectsShowFromAfterStart() {
        MaintenanceWindow w = window(1, 2);
        w.setShowFromAt(new Date(w.getStartAt().getTime() + HOUR));
        assertThrows(MaintenanceWindowException.class, () -> MaintenanceWindowRules.validate(w));
    }

    @Test
    public void showingBoundariesAreShowFromInclusiveEndExclusive() {
        MaintenanceWindow w = window(10, 12);
        w.setShowFromAt(now);
        assertTrue(MaintenanceWindowRules.isShowing(w, now));
        assertFalse(MaintenanceWindowRules.isShowing(w, new Date(now.getTime() - 1)));
        assertTrue(MaintenanceWindowRules.isShowing(w, new Date(w.getEndAt().getTime() - 1)));
        assertFalse(MaintenanceWindowRules.isShowing(w, w.getEndAt()));
    }

    @Test
    public void retiredWindowIsNeverShowing() {
        MaintenanceWindow w = window(1, 5);
        w.setShowFromAt(now);
        w.setRetired(true);
        assertFalse(MaintenanceWindowRules.isShowing(w, now));
    }

    @Test
    public void windowWithNoTargetsAffectsEveryone() {
        MaintenanceWindow w = window(1, 5);
        assertTrue(MaintenanceWindowRules.targets(w, institution(1), department(2)));
        assertTrue(MaintenanceWindowRules.targets(w, null, null));
    }

    @Test
    public void institutionTargetMatchesOnlyThatInstitution() {
        MaintenanceWindow w = window(1, 5);
        w.getInstitutions().add(institution(1));
        assertTrue(MaintenanceWindowRules.targets(w, institution(1), department(9)));
        assertFalse(MaintenanceWindowRules.targets(w, institution(2), department(9)));
        assertFalse(MaintenanceWindowRules.targets(w, null, null));
    }

    @Test
    public void departmentTargetMatchesOnlyThatDepartment() {
        MaintenanceWindow w = window(1, 5);
        w.getDepartments().add(department(7));
        assertTrue(MaintenanceWindowRules.targets(w, institution(1), department(7)));
        assertFalse(MaintenanceWindowRules.targets(w, institution(1), department(8)));
    }

    @Test
    public void filterVisibleKeepsOnlyShowingAndTargeted() {
        MaintenanceWindow showingAll = window(2, 4);
        showingAll.setShowFromAt(new Date(now.getTime() - HOUR));
        MaintenanceWindow notYetShowing = window(30, 32);
        notYetShowing.setShowFromAt(new Date(now.getTime() + 6 * HOUR));
        MaintenanceWindow otherInstitution = window(2, 4);
        otherInstitution.setShowFromAt(new Date(now.getTime() - HOUR));
        otherInstitution.getInstitutions().add(institution(99));
        MaintenanceWindow ended = window(-5, -1);
        ended.setShowFromAt(new Date(now.getTime() - 10 * HOUR));

        List<MaintenanceWindow> visible = MaintenanceWindowRules.filterVisible(
                List.of(showingAll, notYetShowing, otherInstitution, ended),
                institution(1), department(2), now);

        assertEquals(1, visible.size());
        assertSame(showingAll, visible.get(0));
    }
}

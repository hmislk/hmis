package com.divudi.core.data;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.WebUser;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The PDF and Excel exporters are shared by every bundle report, so they gate
 * the "FROM DATE / INSTITUTION / CASHIER" summary block on
 * {@link ReportTemplateRowBundle#hasFilterSummary()}. A bundle whose generator
 * never snapshotted its filters must report false, otherwise an unrelated
 * report would print "All Institutions / All Users" and misstate its scope.
 */
public class ReportTemplateRowBundleFilterSummaryTest {

    @Test
    public void testFreshBundleHasNoFilterSummary() {
        assertFalse(new ReportTemplateRowBundle().hasFilterSummary(),
                "A bundle that has not snapshotted its filters must not claim a filter summary");
    }

    @Test
    public void testAnySingleFilterIsEnoughToShowTheSummary() {
        ReportTemplateRowBundle withFromDate = new ReportTemplateRowBundle();
        withFromDate.setFromDate(new Date());
        assertTrue(withFromDate.hasFilterSummary());

        ReportTemplateRowBundle withToDate = new ReportTemplateRowBundle();
        withToDate.setToDate(new Date());
        assertTrue(withToDate.hasFilterSummary());

        ReportTemplateRowBundle withInstitution = new ReportTemplateRowBundle();
        withInstitution.setFilterInstitution(new Institution());
        assertTrue(withInstitution.hasFilterSummary());

        ReportTemplateRowBundle withSite = new ReportTemplateRowBundle();
        withSite.setFilterSite(new Institution());
        assertTrue(withSite.hasFilterSummary());

        ReportTemplateRowBundle withDepartment = new ReportTemplateRowBundle();
        withDepartment.setFilterDepartment(new Department());
        assertTrue(withDepartment.hasFilterSummary());

        ReportTemplateRowBundle withWebUser = new ReportTemplateRowBundle();
        withWebUser.setFilterWebUser(new WebUser());
        assertTrue(withWebUser.hasFilterSummary());
    }

    @Test
    public void testFilterWebUserDisplayNamePrefersThePersonOverTheLoginName() {
        Person person = new Person();
        person.setName("Nimal Perera");

        WebUser user = new WebUser();
        user.setName("nperera");
        user.setWebUserPerson(person);

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        bundle.setFilterWebUser(user);

        assertEquals("Nimal Perera", bundle.getFilterWebUserDisplayName(),
                "Exports must show the person's name, matching what the screen shows");
    }

    @Test
    public void testFilterWebUserDisplayNameFallsBackToTheLoginName() {
        WebUser withoutPerson = new WebUser();
        withoutPerson.setName("nperera");

        ReportTemplateRowBundle bundle = new ReportTemplateRowBundle();
        bundle.setFilterWebUser(withoutPerson);
        assertEquals("nperera", bundle.getFilterWebUserDisplayName());

        Person blank = new Person();
        blank.setName("   ");
        WebUser withBlankPerson = new WebUser();
        withBlankPerson.setName("nperera");
        withBlankPerson.setWebUserPerson(blank);

        ReportTemplateRowBundle blankBundle = new ReportTemplateRowBundle();
        blankBundle.setFilterWebUser(withBlankPerson);
        assertEquals("nperera", blankBundle.getFilterWebUserDisplayName());
    }

    @Test
    public void testFilterWebUserDisplayNameIsNullWhenNoUserFiltered() {
        assertNull(new ReportTemplateRowBundle().getFilterWebUserDisplayName());
    }
}

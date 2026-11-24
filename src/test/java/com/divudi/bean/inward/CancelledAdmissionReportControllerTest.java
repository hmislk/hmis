package com.divudi.bean.inward;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionCancellationReason;
import com.divudi.core.entity.inward.AdmissionType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Filters of the Cancelled Admissions report. (Issue #25745)
 */
public class CancelledAdmissionReportControllerTest {

    @Test
    public void dateRangeOnCancelledDateAndCancelMarkAlwaysApply() {
        Map<String, Object> params = new HashMap<>();
        Date from = new Date(0L);
        Date to = new Date();
        String jpql = CancelledAdmissionReportController.buildJpql(params, from, to, null, null, null, null, null);
        assertTrue(jpql.contains("ad.retired = true"));
        assertTrue(jpql.contains("ad.retiredAt between :fromDate and :toDate"));
        assertTrue(jpql.contains("order by ad.retiredAt desc"));
        assertEquals("BHT Cancel", params.get("cancelMark"));
        assertEquals(from, params.get("fromDate"));
        assertEquals(to, params.get("toDate"));
        assertEquals(3, params.size());
        assertFalse(jpql.contains(":admType"));
    }

    @Test
    public void optionalFiltersAreAddedOnlyWhenGiven() {
        Map<String, Object> params = new HashMap<>();
        AdmissionType type = new AdmissionType();
        AdmissionCancellationReason reason = new AdmissionCancellationReason();
        Institution ins = new Institution();
        Institution site = new Institution();
        Department dept = new Department();
        String jpql = CancelledAdmissionReportController.buildJpql(params, new Date(), new Date(), type, reason, ins, site, dept);
        assertTrue(jpql.contains("ad.admissionType = :admType"));
        assertTrue(jpql.contains("ad.cancellationReason = :reason"));
        assertTrue(jpql.contains("ad.institution = :ins"));
        assertTrue(jpql.contains("ad.department.site = :site"));
        assertTrue(jpql.contains("ad.department = :dept"));
        assertEquals(type, params.get("admType"));
        assertEquals(dept, params.get("dept"));
        assertEquals(8, params.size());
    }

    @Test
    public void oldCancellationsWithoutNewFactsAreKept() {
        String jpql = CancelledAdmissionReportController.buildJpql(new HashMap<>(), new Date(), new Date(), null, null, null, null, null);
        assertTrue(jpql.contains("left join ad.cancellationReason cr"));
        assertTrue(jpql.contains("left join ad.cancellationDecidedBy cdb"));
        assertTrue(jpql.contains("left join ad.referringConsultant rc"));
        assertTrue(jpql.contains("left join ad.retirer ru"));
    }
}

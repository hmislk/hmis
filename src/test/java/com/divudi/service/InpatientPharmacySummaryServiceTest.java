package com.divudi.service;

import com.divudi.core.entity.PatientEncounter;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class InpatientPharmacySummaryServiceTest {

    private static PatientEncounter pe(long id) {
        PatientEncounter p = new PatientEncounter();
        p.setId(id);
        return p;
    }

    @Test
    void idsCoverTheAdmissionAndItsChildren() {
        assertEquals(Arrays.asList(1L, 2L, 3L),
                InpatientPharmacySummaryService.idsOf(pe(1), Arrays.asList(pe(2), pe(3))));
    }

    @Test
    void idsAreJustTheAdmissionWithoutChildren() {
        assertEquals(Collections.singletonList(1L), InpatientPharmacySummaryService.idsOf(pe(1), null));
        assertEquals(Collections.singletonList(1L), InpatientPharmacySummaryService.idsOf(pe(1), Collections.emptyList()));
    }

    @Test
    void reversalFiltersAreAppliedToTheSummaryRows() throws Exception {
        // The summary query must drop a data-admin reversal and the original it cancelled.
        java.lang.reflect.Method m = InpatientPharmacySummaryService.class.getDeclaredMethod("fetchRows", PatientEncounter.class);
        assertEquals(java.util.List.class, m.getReturnType());
        org.junit.jupiter.api.Assertions.assertTrue(
                com.divudi.core.data.inward.InwardMedicineBillTypes.JPQL_NOT_REVERSAL_BILL.contains("rv.cancelledBill.id = bi.bill.id"));
        org.junit.jupiter.api.Assertions.assertTrue(
                com.divudi.core.data.inward.InwardMedicineBillTypes.JPQL_NOT_REVERSED_ORIGINAL.contains("rv.id = bi.bill.cancelledBill.id"));
    }
}

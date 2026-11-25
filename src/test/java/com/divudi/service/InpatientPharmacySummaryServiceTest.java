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
}

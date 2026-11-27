package com.divudi.service.inward;

import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.AdmissionCancellationReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Required fields when cancelling an admission. (Issue #25745)
 */
public class AdmissionCancellationRulesTest {

    private final AdmissionCancellationReason reason = new AdmissionCancellationReason();
    private final Staff staff = new Staff();

    @Test
    public void everythingGivenPasses() {
        assertNull(AdmissionCancellationRules.firstMissing("Entered by mistake", reason, staff));
    }

    @Test
    public void blankNotesAreRefused() {
        assertNotNull(AdmissionCancellationRules.firstMissing(null, reason, staff));
        assertNotNull(AdmissionCancellationRules.firstMissing("   ", reason, staff));
    }

    @Test
    public void missingCodedReasonIsRefused() {
        String message = AdmissionCancellationRules.firstMissing("Entered by mistake", null, staff);
        assertTrue(message.contains("reason from the list"));
    }

    @Test
    public void missingDecisionMakerIsRefused() {
        String message = AdmissionCancellationRules.firstMissing("Entered by mistake", reason, null);
        assertTrue(message.contains("who decided"));
    }

    @Test
    public void removedReasonIsRefused() {
        AdmissionCancellationReason removed = new AdmissionCancellationReason();
        removed.setRetired(true);
        String message = AdmissionCancellationRules.firstMissing("Entered by mistake", removed, staff);
        assertTrue(message.contains("has been removed"));
    }

    @Test
    public void blankNotesMessageNamesNotes() {
        assertTrue(AdmissionCancellationRules.firstMissing(" ", reason, staff).startsWith("Notes are required"));
    }
}

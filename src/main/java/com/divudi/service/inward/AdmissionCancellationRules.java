package com.divudi.service.inward;

import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.AdmissionCancellationReason;

/**
 * Required-field rules for cancelling an admission (#25745): the notes, the
 * coded reason and who decided the cancellation must all be given.
 */
public final class AdmissionCancellationRules {

    private AdmissionCancellationRules() {
    }

    /**
     * @return the message to show for the first missing field, or {@code null}
     * when the cancellation may go ahead
     */
    public static String firstMissing(String notes, AdmissionCancellationReason reason, Staff decidedBy) {
        if (notes == null || notes.trim().isEmpty()) {
            return "Notes are required. Please enter notes before proceeding.";
        }
        if (reason == null) {
            return "Select the cancellation reason from the list before proceeding.";
        }
        if (reason.isRetired()) {
            return "The selected cancellation reason has been removed. Select another reason before proceeding.";
        }
        if (decidedBy == null) {
            return "Select who decided the cancellation before proceeding.";
        }
        return null;
    }
}

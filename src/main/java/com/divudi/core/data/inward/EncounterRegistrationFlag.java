package com.divudi.core.data.inward;

/**
 * Operational / clinical flag applied to a {@code PatientEncounter} (and by
 * inheritance an {@code Admission}) at the time of registration.
 *
 * <p>The default for every encounter is {@link #STANDARD}, which represents a
 * normal admission and is shown without any badge in the UI. Non-standard
 * values mark encounters that need special handling for legal, statistical,
 * or billing purposes.</p>
 *
 * Issue: hmislk/hmis#21182
 *
 * @author Dr M H B Ariyaratne
 */
public enum EncounterRegistrationFlag {

    STANDARD("Standard"),                       // normal admission — default value, no badge shown
    ON_ADMISSION_DEATH("On Admission Death"),   // patient arrived dead or died before registration was completed
    RAPID_TEMP_AE("Provisional Emergency");      // quick/temporary A&E registration — demographics may be incomplete (see separate issue)

    private final String label;

    EncounterRegistrationFlag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

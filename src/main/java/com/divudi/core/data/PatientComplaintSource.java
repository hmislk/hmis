package com.divudi.core.data;

/**
 * Where a patient complaint came from (#25886): typed in by staff on site, or
 * submitted by a patient in the client portal.
 */
public enum PatientComplaintSource {

    STAFF("On site"),
    PORTAL("Online");

    private final String label;

    PatientComplaintSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

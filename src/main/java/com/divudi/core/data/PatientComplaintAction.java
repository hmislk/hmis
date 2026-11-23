package com.divudi.core.data;

/**
 * What a patient complaint log line records (#25886).
 */
public enum PatientComplaintAction {

    CREATED("Complaint received"),
    ASSIGNED("Assigned"),
    STATUS_CHANGED("Status changed"),
    RESPONSE_SAVED("Response saved");

    private final String label;

    PatientComplaintAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.divudi.core.data;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life cycle of a patient complaint (#25886). New, then In Progress, then
 * Closed. A Closed complaint can be reopened to In Progress.
 */
public enum PatientComplaintStatus {

    NEW("New"),
    IN_PROGRESS("In Progress"),
    CLOSED("Closed");

    private final String label;

    PatientComplaintStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Statuses a complaint may move to from this one. */
    public Set<PatientComplaintStatus> getAllowedNext() {
        switch (this) {
            case NEW:
                return EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS:
                return EnumSet.of(CLOSED);
            case CLOSED:
                return EnumSet.of(IN_PROGRESS);
            default:
                return EnumSet.noneOf(PatientComplaintStatus.class);
        }
    }

    public boolean canMoveTo(PatientComplaintStatus next) {
        return next != null && getAllowedNext().contains(next);
    }
}

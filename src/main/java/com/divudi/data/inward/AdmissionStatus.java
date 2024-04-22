package com.divudi.data.inward;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum AdmissionStatus {
    ADMITTED_BUT_NOT_DISCHARGED("Admitted but not discharged"),
    DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED("Discharged but final bill not completed"),
    DISCHARGED_AND_FINAL_BILL_COMPLETED("Discharged and final bill completed"),
    ANY_STATUS("Any status");

    private final String label;

    AdmissionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.divudi.core.data;

/**
 * Status of a single {@link com.divudi.core.entity.clinical.MedicationAdministrationRecord}
 * dose-administration event (#21469).
 */
public enum MedicationAdministrationStatus {

    GIVEN("Given"),
    REFUSED("Refused"),
    HELD("Held");

    private final String label;

    private MedicationAdministrationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}

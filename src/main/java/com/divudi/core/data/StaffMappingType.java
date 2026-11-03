package com.divudi.core.data;

/**
 * The purpose a staff member is mapped for. Add a value here for each new place
 * that needs to restrict a staff list to specifically mapped staff.
 */
public enum StaffMappingType {

    SAMPLE_TRANSPORTER("Sample Transporter");

    private final String label;

    private StaffMappingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}

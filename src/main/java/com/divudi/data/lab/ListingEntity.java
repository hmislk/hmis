package com.divudi.data.lab;

/**
 * Enum representing different types of listing entities.
 * 
 * Author: Dr M H B Ariyaratne
 */
public enum ListingEntity {
    BILLS("Bills"),
    BILL_ITEMS("Bill Items"),
    PATIENT_INVESTIGATIONS("Patient Investigations"),
    PATIENT_SAMPLES("Patient Samples"),
    PATIENT_REPORTS("Patient Reports"),
    BILL_BARCODES("Bill Barcodes"),
    PATIENT_SAMPLE_RUNS("Patient Sample Runs");

    private final String label;

    ListingEntity(String label) {
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

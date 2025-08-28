package com.divudi.core.data.lab;

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
    PATIENT_SAMPLES_INDIVIDUAL("IndividualPatient Samples"),
    PATIENT_REPORTS("Patient Reports"),
    VIEW_BARCODE("View Barcodes"),
    BILL_BARCODES("Bill Barcodes"),
    PATIENT_SAMPLE_RUNS("Patient Sample Runs"),
    REPORT_PRINT("Patient Reports Print");

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

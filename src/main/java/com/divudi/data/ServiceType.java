package com.divudi.data;

/**
 * Enumerates types of services for billing purposes, categorizing bills by the type of service provided.
 */
public enum ServiceType {
    OPD("Outpatient Department"),
    PHARMACY("Pharmacy"),
    CHANNELLING("Channelling"),
    OTHER("Other");

    private final String label;

    ServiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

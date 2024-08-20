package com.divudi.data;

/**
 * Enumerates types of services for billing purposes, categorizing bills by the type of service provided.
 */
public enum ServiceType {

    OPD("Outpatient Department"),
    PROFESSIONAL_PAYMENT("Professional Payment"),
    PHARMACY("Pharmacy"),
    STORE("Store"),
    CHANNELLING("Channelling"),
    COLLECTING_CENTRE("Colelcting Centre"),
    OTHER("Other"),
    INWARD("Inward");

    private final String label;

    ServiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

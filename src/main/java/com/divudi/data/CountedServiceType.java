package com.divudi.data;

/**
 * Enumerates types of services for billing purposes, categorizing bills by the type of service provided.
 */
public enum CountedServiceType {

    OPD("Outpatient Department"),
    OPD_OUT("Outpatient Department - Cancellation or Refunds"),
    OPD_PROFESSIONAL_PAYMENT("Outpatient Department - Professional Payment"),
    NONE("NONE"),
    PHARMACY("Pharmacy"),
    STORE("Store"),
    CHANNELLING("Channelling"),
    COLLECTING_CENTRE("Colelcting Centre"),
    OTHER("Other"),
    INWARD("Inward");

    private final String label;

    CountedServiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.divudi.data;

/**
 * Enumerates types of services for billing purposes, categorizing bills by the type of service provided.
 */
public enum CountedServiceType {

    OPD_IN("Outpatient Department - Collection"),
    OPD_OUT("Outpatient Department - Cancellation or Refunds"),
    OPD_PROFESSIONAL_PAYMENT("Outpatient Department - Professional Payment"),
    OPD_PROFESSIONAL_PAYMENT_RETURN("Outpatient Department - Professional Payment Cancellation or Returns"),
    NONE("NONE"),
    PHARMACY("Pharmacy"),
    STORE("Store"),
    CHANNELLING("Channelling"),
    CHANNELLING_PROFESSIONAL_PAYMENT("Channelling Professional Payment"),
    CHANNELLING_PROFESSIONAL_PAYMENT_RETURN("Channelling Professional Payment Cancellation or Returns"),
    INWARD("Inward"),
    INWARD_PROFESSIONAL_PAYMENT("Inward Professional Payment"),
    INWARD_PROFESSIONAL_PAYMENT_RETURN("Inward Professional Payment Cancellation or Returns"),
    COLLECTING_CENTRE("Colelcting Centre"),
    OTHER("Other"),
    OTHER_PROFESSIONAL_PAYMENT("Channelling Professional Payment"),
    OTHER_PROFESSIONAL_PAYMENT_RETURN("Channelling Professional Payment Cancellation or Returns"),
    
    ;

    private final String label;

    CountedServiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

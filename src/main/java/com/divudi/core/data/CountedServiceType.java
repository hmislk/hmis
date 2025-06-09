package com.divudi.core.data;

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
    SUPPLIER_PAYMENT("Supplier Payment"),
    CHANNELLING("Channelling"),
    CHANNELLING_PROFESSIONAL_PAYMENT("Channelling Professional Payment"),
    CHANNELLING_PROFESSIONAL_PAYMENT_RETURN("Channelling Professional Payment Cancellation or Returns"),
    INWARD("Inward"),
    INWARD_PROFESSIONAL_PAYMENT("Inward Professional Payment"),
    INWARD_PROFESSIONAL_PAYMENT_RETURN("Inward Professional Payment Cancellation or Returns"),
    COLLECTING_CENTRE("Collecting Centre"),
    OTHER("Other"),
    CREDIT_SETTLE_BY_PATIENT("Credit Settle by Patient"),
    CREDIT_SETTLE_BY_COMPANY("Credit Settle by Company"),
    OTHER_PROFESSIONAL_PAYMENT("Other Professional Payment"),
    OTHER_PROFESSIONAL_PAYMENT_RETURN("Other Professional Payment Cancellation or Returns"),
    AGENCY("Agency"),  // New entry for agency-related collections
    AGENCY_PROFESSIONAL_PAYMENT("Agency Professional Payment"),  // New entry for agency professional payments
    AGENCY_PROFESSIONAL_PAYMENT_RETURN("Agency Professional Payment Cancellation or Returns");  // New entry for returns or cancellations of agency payments

    private final String label;

    CountedServiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

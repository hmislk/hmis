package com.divudi.core.data.lab;

/**
 * Who billed a Collecting Centre's bill: the centre (agent) itself (Agent
 * Billing, billed from a Collecting Centre type department) or the hospital
 * on the centre's behalf (Hospital Billing). A null value means all bills.
 */
public enum CollectingCentreBillingType {
    AGENT_BILLING("Agent Billing"),
    HOSPITAL_BILLING("Hospital Billing");

    private final String label;

    CollectingCentreBillingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

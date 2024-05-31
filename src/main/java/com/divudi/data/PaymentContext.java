package com.divudi.data;

public enum PaymentContext {
    
    PURCHASES("Purchases"),
    ACCEPTING_PAYMENTS("Accepting Payments"),
    ACCEPTING_PAYMENTS_FOR_CHANNELLING("Accepting Payments for Channelling"),
    CREDIT_SETTLEMENTS("Credit Settlements");

    private final String label;

    PaymentContext(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

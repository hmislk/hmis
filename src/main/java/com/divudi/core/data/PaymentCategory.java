package com.divudi.core.data;

/**
 *
 * @author Dr M H B Ariyaratne
 */
public enum PaymentCategory {
    CREDIT_COLLECTION("Credit Collection"),
    NON_CREDIT_COLLECTION("Non-Credit Collection"),
    NON_CREDIT_SPEND("Cash Spend"),
    CREDIT_SPEND("Non-Cash Spend"),
    NO_PAYMENT("No Payment Involved"),
    OTHER("Other");

    private final String description;

    PaymentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

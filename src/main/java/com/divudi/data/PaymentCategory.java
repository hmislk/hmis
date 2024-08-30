package com.divudi.data;

/**
 *
 * @author Dr M H B Ariyaratne
 */
public enum PaymentCategory {
    CREDIT_COLLECTION("Credit Collection"),
    NON_CREDIT_COLLECTION("Non-Credit Collection"),
    CASH_SPEND("Cash Spend"),
    NON_CASH_SPEND("Non-Cash Spend"),
    OTHER("Other");

    private final String description;

    PaymentCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

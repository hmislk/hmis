package com.divudi.data;

/**
 * Enumerates the categories of bills, indicating whether it's a standard bill,
 * a cancellation, or a refund.
 */
public enum BillCategory {
    BILL("Bill"),
    CANCELLATION("Cancellation"),
    REFUND("Refund");

    private final String label;

    BillCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

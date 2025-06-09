package com.divudi.core.data;

/**
 *
 * @author Dr M H B Ariyaratne
 */
public enum PaymentSelectionMode {
    SELECT_ALL_FOR_HANDOVER_CREATION("Select all Payments for shift handover creation"),
    SELECT_NONE_FOR_HANDOVER_CREATION("Select no Payment for shift handover creation"),
    SELECT_FOR_HANDOVER_RECEIPT("Select payments for handover receipt depending on the selection for shift handover creation"),
    SELECT_FOR_HANDOVER_RECORD("Select payments for handover record depending on the selection for shift handover receipt"),
    SELECT_FOR_HANDOVER_RECORD_CONFIRMATION("Select payments for handover record confirmation depending on the selection for handover record"),
    NONE("No selection mode"); // Added None

    private final String description;

    PaymentSelectionMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

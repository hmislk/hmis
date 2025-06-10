package com.divudi.core.data;

/**
 * Enum representing predefined categories of historical records.
 */
public enum HistoricalRecordType {
    PHARMACY_STOCK_VALUE("Pharmacy Stock Value"),
    COLLECTION_CENTRE_BALANCE("Collection Centre Balance"),
    CREDIT_COMPANY_BALANCE("Credit Company Balance"),
    DRAWER_BALANCE("Drawer Balance");

    private final String label;

    HistoricalRecordType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

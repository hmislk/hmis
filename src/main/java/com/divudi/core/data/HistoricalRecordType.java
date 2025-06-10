package com.divudi.core.data;

/**
 * Enum representing predefined categories of historical records.
 */
public enum HistoricalRecordType {
    Pharmacy_Stock_Value("Pharmacy Stock Value"),
    Collection_Centre_Balance("Collection Centre Balance"),
    Credit_Company_Balance("Credit Company Balance"),
    Drawer_Balance("Drawer Balance");

    private final String label;

    HistoricalRecordType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.divudi.core.data;

/**
 * Enum representing predefined categories of historical records.
 */
public enum HistoricalRecordType {
    PHARMACY_STOCK_VALUE_PURCHASE_RATE("Pharmacy Stock Value Purchase Rate"),
    PHARMACY_STOCK_VALUE_RETAIL_RATE("Pharmacy Stock Value Retail Rate"),
    PHARMACY_STOCK_VALUE_COST_RATE("Pharmacy Stock Value Cost Rate"),
    COLLECTION_CENTRE_BALANCE("Collection Centre Balance"),
    CREDIT_COMPANY_BALANCE("Credit Company Balance"),
    ASYNC_REPORT("Async Report"),
    DRAWER_BALANCE("Drawer Balance");


    private final String label;

    HistoricalRecordType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}


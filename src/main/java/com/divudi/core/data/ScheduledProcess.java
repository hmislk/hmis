package com.divudi.core.data;

/**
 * Enum representing scheduled processes for historical data recording.
 * Additional values can be added as new requirements arise.
 */
public enum ScheduledProcess {
    Record_Pharmacy_Stock_Values("Record Pharmacy Stock Values"),
    All_Drawer_Balances("All Drawer Balances"),
    All_Collection_Centre_Balances("All Collection Centre Balances"),
    All_Credit_Company_Balances("All Credit Company Balances");

    private final String label;

    ScheduledProcess(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

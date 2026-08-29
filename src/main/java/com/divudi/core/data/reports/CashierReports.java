package com.divudi.core.data.reports;

public enum CashierReports implements IReportType {
    All_CASHIER_SUMMARY("All Cashier Summary"),
    CASHIER_SUMMARY("Cashier Summary"),
    CASHIER_DETAILED("Cashier Detailed"),
    TOTAL_CASHIER_SUMMARY("Total Cashier Summary"),
    CASHIER_SHIFT_END_SUMMARY("Cashier Shift End Summary"),;

    private final String displayName;

    CashierReports(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getReportType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getReportName() {
        return this.name();
    }
}

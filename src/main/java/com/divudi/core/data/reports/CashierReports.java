package com.divudi.core.data.reports;

public enum CashierReports implements IReportType {
    All_CASHIER_SUMMARY("All Cashier Summary");

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

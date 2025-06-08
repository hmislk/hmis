package com.divudi.core.data.reports;

public enum InventoryReports implements IReportType {
    CLOSING_STOCK_REPORT("Closing Stock Report");

    private final String displayName;

    InventoryReports(String displayName) {
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

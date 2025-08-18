package com.divudi.core.data.reports;

public enum InventoryReports implements IReportType {
    CLOSING_STOCK_REPORT("Closing Stock Report"),
    STOCK_TRANSFER_REPORT("Stock Transfer Report"),
    GOOD_IN_TRANSIT_REPORT("Good In Transit"),
    CONSUMPTION_REPORT("Consumption Report"),
    GRN_REPORT("GRN Report"),
    STOCK_LEDGER_REPORT("Stock Ledger Report");

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

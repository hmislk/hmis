package com.divudi.core.data.reports;

public enum PharmacyReports implements IReportType {
    STOCK_REPORT_BY_BATCH("Stock Report by Batch");

    private final String displayName;

    PharmacyReports(String displayName) {
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

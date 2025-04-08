package com.divudi.core.data.reports;

public enum SummaryReports implements IReportType {
    PHARMACY_INCOME_REPORT("Pharmacy Income Report");

    private final String displayName;

    SummaryReports(String displayName) {
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

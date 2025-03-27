package com.divudi.data.reports;

public enum CollectionCenterReport implements IReportType {
    ROUTE_ANALYSIS_REPORT("Route Analysis Report");

    private final String displayName;

    CollectionCenterReport(String displayName) {
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

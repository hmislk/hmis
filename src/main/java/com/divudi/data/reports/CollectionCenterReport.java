package com.divudi.data.reports;

public enum CollectionCenterReport implements IReportType {
    ROUTE_ANALYSIS_REPORT("Route Analysis Report", "route_analysis_report.xhtml");

    private final String displayName;
    private final String reportPath;

    CollectionCenterReport(String displayName, String reportPath) {
        this.displayName = displayName;
        this.reportPath = reportPath;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getReportPath() {
        return reportPath;
    }

    @Override
    public String getReportType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getValue() {
        return this.name();
    }
}

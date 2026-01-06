package com.divudi.core.data.reports;

public enum ManagementReport implements IReportType {
    OPD_WEEKLY_REPORT("OPD weekly report");

    private final String displayName;

    ManagementReport(String displayName) {
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

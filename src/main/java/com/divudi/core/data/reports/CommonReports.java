package com.divudi.core.data.reports;

/**
 * Generic report type used for miscellaneous report logging.
 */
public enum CommonReports implements IReportType {
    LAB_DASHBOARD("Lab Dashboard"),
    LAB_REPORTS("Lab Reports");

    private final String displayName;

    CommonReports(String displayName) {
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

package com.divudi.core.data.reports;

public enum ManagementReports implements IReportType {
    REFERRING_DOCTOR_WISE_REVENUE_REPORT("Referring Doctor Wise Revenue Report"),;


    private final String displayName;

    ManagementReports(String displayName) {
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

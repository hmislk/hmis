package com.divudi.core.data.reports;

public enum LaboratoryReport implements IReportType {
    SAMPLE_CARRIER_REPORT("Sample Carrier Report"),
    INVESTIGATION_MONTH_END_SUMMARY("Investigation Month End Summary");

    private final String displayName;

    LaboratoryReport(String displayName) {
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

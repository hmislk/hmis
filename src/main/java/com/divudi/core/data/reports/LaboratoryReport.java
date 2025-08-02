package com.divudi.core.data.reports;

public enum LaboratoryReport implements IReportType {
    SAMPLE_CARRIER_REPORT("Sample Carrier Report"),
    INVESTIGATION_MONTH_END_SUMMARY("Investigation Month End Summary"),
    EXTERNAL_LABORATORY_WORKLOAD_REPORT("External laboratory workload report"),
    LABORATORY_WORKLOAD_REPORT("Laboratory workload report"),
    COLLECTION_CENTER_STATEMENT_REPORT("Collection Center Statement Report"),
    PATIENT_SAMPLE_REPORT("Patient Sample Report");

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

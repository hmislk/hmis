package com.divudi.core.data.reports;

public enum CollectionCenterReport implements IReportType {
    ROUTE_ANALYSIS_REPORT("Route Analysis Report"),
    COLLECTION_CENTER_STATEMENT_REPORT("Collection Centre Statement Report"),
    COLLECTION_CENTER_REPORTS_PRINT("Collection Centre Reports Print"),
    COLLECTION_CENTER_BALANCE_REPORT("Collection Centre Balance Report"),
    COLLECTION_CENTER_RECEIPT_REPORT("Collection Centre Receipt Report"),
    COLLECTION_CENTER_TEST_WISE_COUNT_REPORT("Collection Center Test Wise Count Report");

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

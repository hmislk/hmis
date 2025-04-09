package com.divudi.core.data.reports;

public enum PharmacyReports implements IReportType {
    STOCK_REPORT_BY_BATCH("Stock Report by Batch"),
    PHARMACY_BIN_CARD("Pharmacy Bin Card"),
    BHT_ISSUE_BY_BILL("BHT Issue by Bill"),
    GRN_SUMMARY("GRN Summary");

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

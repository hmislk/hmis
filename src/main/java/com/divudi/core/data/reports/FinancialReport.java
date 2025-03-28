package com.divudi.core.data.reports;

public enum FinancialReport implements IReportType {
    STAFF_WELFARE_REPORT("Staff Welfare Report");

    private final String displayName;

    FinancialReport(String displayName) {
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

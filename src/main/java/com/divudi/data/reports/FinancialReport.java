package com.divudi.data.reports;

public enum FinancialReport implements IReportType {
    STAFF_WELFARE_REPORT("Staff Welfare Report", "reports/financialReports/staff_welfare.xhtml");

    private final String displayName;
    private final String reportPath;

    FinancialReport(String displayName, String reportPath) {
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

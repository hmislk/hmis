package com.divudi.core.data.reports;

public enum CreditReport implements IReportType {
    OPD_CREDIT_DUE("OPD credit due report"),
    INWARD_CREDIT_DUE("Inward credit due report");

    private final String displayName;

    CreditReport(String displayName) {
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

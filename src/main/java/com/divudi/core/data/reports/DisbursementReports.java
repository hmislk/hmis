package com.divudi.core.data.reports;

public enum DisbursementReports implements IReportType {
    TRANSFER_ISSUE_BY_BILL("Transfer Issue by Bill"),
    TRANSFER_RECEIVE_BY_BILL("Transfer Receive by Bill"),
    TRANSFER_ISSUE_BY_BILL_SUMMARY("Transfer Issue by Bill Summary"),
    TRANSFER_RECEIVE_BY_BILL_SUMMARY("Transfer Receive by Bill Summary");

    private final String displayName;

    DisbursementReports(String displayName) {
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

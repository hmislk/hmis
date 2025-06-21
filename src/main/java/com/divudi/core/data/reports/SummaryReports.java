package com.divudi.core.data.reports;



public enum SummaryReports implements IReportType {
    PHARMACY_INCOME_REPORT("Pharmacy Income Report"),
    PHARMACY_SINGLE_ITEM_TRANSACTION_SUMMARY("Pharmacy Single Item Transction Summary"),
    DAILY_STOCK_BALANCE_REPORT("Daily Stock Balance Report"),
    PHARMACY_INCOME_AND_COST_REPORT("Pharmacy Income and Cost Report"),
    PHARMACY_MOVEMENT_OUT_REPORT("Pharmacy Movement Out Report"),
    BILL_TYPE_LIST_REPORT("Bill Type List Report"),;


    private final String displayName;

    SummaryReports(String displayName) {
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

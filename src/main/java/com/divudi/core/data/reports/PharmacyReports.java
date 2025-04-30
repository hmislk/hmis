package com.divudi.core.data.reports;

public enum PharmacyReports implements IReportType {
    STOCK_REPORT_BY_BATCH("Stock Report by Batch"),
    PHARMACY_BIN_CARD("Pharmacy Bin Card"),
    BHT_ISSUE_BY_BILL("BHT Issue by Bill"),
    GRN_SUMMARY("GRN Summary"),
    CASH_IN_OUT_REPORT("Cash In/Out Report"),
    CASHIER_REPORT("Cashier Report"),
    CASHIER_SUMMERY_BY_USER_ONLY_CHANNEL_REPORT("Cashier Summary By User Only Channel Report"),
    CASHIER_SUMMERY_BY_USER_ONLY_CASHIER_REPORT("Cashier Summary By User Only Cashier Report"),
    ALL_CASHIER_REPORT("All Cashier Report"),
    ALL_CASHIER_SUMMERY_TOTAL_INCOME_REPORT("All Cashier summary Total Income Report"),
    ALL_CASHIER_SUMMERY_CHANNEL_INCOME_REPORT("All Cashier summary Channel Income Report"),
    ALL_CASHIER_SUMMERY_CASHIER_INCOME_REPORT("All Cashier summary Cashier Income Report"),;


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

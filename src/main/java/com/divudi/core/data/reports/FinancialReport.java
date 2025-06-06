package com.divudi.core.data.reports;

public enum FinancialReport implements IReportType {
    STAFF_WELFARE_REPORT("Staff Welfare Report"),
    DAILY_RETURN("Daily Return"),
    OPD_AND_INWARD_DUE_REPORT("OPD and Inward Due Report"),
    DEBTOR_BALANCE_REPORT("Debtor Balance Report"),
    INCOME_CATEGORY_WISE_REPORT("Income Category Wise Report"),
    PETTY_CASH_REPORT("Petty Cash Report"),
    BILL_WISE_ITEM_MOVEMENT_REPORT("Bill wise item Movement Report"),
    WITHHOLDING_TAX_REPORT("withholding tax Report"),;


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

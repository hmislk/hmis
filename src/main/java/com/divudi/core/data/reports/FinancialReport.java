package com.divudi.core.data.reports;

public enum FinancialReport implements IReportType {
    STAFF_WELFARE_REPORT("Staff Welfare Report"),
    DAILY_RETURN("Daily Return"),
    OPD_AND_INWARD_DUE_REPORT("OPD and Inward Due Report"),
    DEBTOR_BALANCE_REPORT("Debtor Balance Report"),
    DISCOUNT_REPORT("Discount Report"),
    INVOICE_AND_RECEIPT_REPORT("Invoice and receipt Report"),
    CREDIT_INVOICE_DISPATCH_REPORT("Credit Invoice dispatch Report"),
    PAYMENT_SETTLEMENT_REPORT("Payment Settlement Report"),
    PACKAGE_REPORT("Package Report"),
    INWARD_DUE_SEARCH("Inward Due Search"),
    DUE_AGE_DETAIL_REPORT("Due age detail Report"),
    INWARD_CREDIT_EXCESS("Inward Credit Excess"),
    INWARD_CREDIT_EXCESS_AGE_CREDIT_COMPANY("Inward credit excess Age Company"),
    INWARD_CASH_EXCESS("Inward cash Excess"),
    INCOME_CATEGORY_WISE_REPORT("Income Category Wise Report"),
    PETTY_CASH_REPORT("Petty Cash Report"),
    BILL_WISE_ITEM_MOVEMENT_REPORT("Bill wise item Movement Report"),
    WITHHOLDING_TAX_REPORT("Withholding tax Report"),
    DEBTOR_SETTLEMENT_REPORT("Debtor settlement Report");


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

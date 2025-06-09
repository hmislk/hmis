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
    INWARD_DUE_SEARCH("Inward Due Search"),;

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

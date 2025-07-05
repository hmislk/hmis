package com.divudi.core.data.reports;

/**
 * Generic report type used for miscellaneous report logging.
 */
public enum CommonReports implements IReportType {
    LAB_DASHBOARD("Lab Dashboard"),
    OPD_BILL("OPD Bill"),
    OPD_BILL_PACKAGE("OPD Bill Package"),
    OPD_PRE_BILL("OPD Pre Bill"),
    COLLECTING_CENTRE_BILL("Collecting Centre Bill"),
    PHARMACY_SEARCH_PRE_BILL("Pharmacy Search Pre Bill"),
    OPD_BILL_SEARCH("OPD Bill Search"),
    PHARMACY_BILL_RETAIL_SALE("Pharmacy Bill Retail Sale"),
    PHARMACY_BILL_RETAIL_SALE_FOR_CASHIER("Pharmacy Bill Retail Sale For Cashier"),
    PHARMACY_PURCHASE_ORDER_REQUEST("Pharmacy Purchase Order Request"),
    PHARMACY_PURCHASE_ORDER_LIST_FOR_RECEIVE("Pharmacy Purchase Order List For Receive");

    private final String displayName;

    CommonReports(String displayName) {
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

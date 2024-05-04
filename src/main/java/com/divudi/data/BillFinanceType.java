package com.divudi.data;

/**
 * Enumerates the types of financial transactions related to billing.
 * Each constant represents a specific kind of billing finance movement or status.
 * 
 * @author Dr M H B Ariyaratne
 */
public enum BillFinanceType {
    NO_FINANCE_TRANSACTIONS("No Finance Transactions"),
    CASH_IN("Cash In"),
    CASH_OUT("Cash Out"),
    CREDIT_IN("Credit In"),
    CREDIT_OUT("Credit Out"),
    CREDIT_SETTLEMENT("Credit Settlement"),
    CREDIT_SETTLEMENT_REVERSE("Credit Settlement Reverse"),
    FLOAT_STARTING_BALANCE("Float Starting Balance"),
    FLOAT_CLOSING_BALANCE("Float Closing Balance"),
    FLOAT_INCREASE("Float Increase"),
    FLOAT_DECREASE("Float Decrease");
    
    private final String label;

    BillFinanceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

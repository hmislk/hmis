package com.divudi.core.data;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representing different types of accounts where a cashier can store income.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
public enum BankAccountType {
    BANK_ACCOUNT("Bank Account"),
    SAFE("Safe"),
    DEPOSIT_BOX("Deposit Box"),
    CASH_REGISTER("Cash Register"),
    VAULT("Vault");

    private final String label;

    BankAccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static List<BankAccountType> getAllValues() {
        return Arrays.asList(BankAccountType.values());
    }
}

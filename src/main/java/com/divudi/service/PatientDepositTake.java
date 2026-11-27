package com.divudi.service;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PatientDeposit;

/**
 * Hardening, tracked under #26265.
 */
public class PatientDepositTake {

    private final PatientDeposit deposit;
    private double amount;
    private final double balanceBefore;
    private double balanceAfter;
    private final double creditLimit;
    private Bill historyBill;
    private boolean givenBack;
    private boolean patientBalanceLowered;

    public PatientDepositTake(PatientDeposit deposit, double amount, double balanceBefore, double balanceAfter, double creditLimit) {
        this.deposit = deposit;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.creditLimit = creditLimit;
    }

    public PatientDeposit getDeposit() {
        return deposit;
    }

    public double getAmount() {
        return amount;
    }

    void setAmount(double amount) {
        this.amount = amount;
    }

    public double getBalanceBefore() {
        return balanceBefore;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    public Bill getHistoryBill() {
        return historyBill;
    }

    void setHistoryBill(Bill historyBill) {
        this.historyBill = historyBill;
    }

    public boolean isGivenBack() {
        return givenBack;
    }

    void setGivenBack(boolean givenBack) {
        this.givenBack = givenBack;
    }

    public boolean isPatientBalanceLowered() {
        return patientBalanceLowered;
    }

    public void setPatientBalanceLowered(boolean patientBalanceLowered) {
        this.patientBalanceLowered = patientBalanceLowered;
    }
}

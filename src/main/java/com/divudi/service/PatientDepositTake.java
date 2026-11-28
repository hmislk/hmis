package com.divudi.service;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.PatientDepositHistory;
import com.divudi.core.entity.WebUser;

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
    private boolean finished;
    private boolean billSaved;
    private PatientDepositHistory historyRow;
    private WebUser creater;
    private Department department;
    private Institution institution;

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

    public boolean isFinished() {
        return finished;
    }

    public void markFinished() {
        this.finished = true;
    }

    /**
     * Hardening, tracked under #26265.
     */
    public boolean canGiveBack() {
        return !givenBack && !billSaved;
    }

    public boolean isBillSaved() {
        return billSaved;
    }

    public void markBillSaved() {
        this.billSaved = true;
    }

    public PatientDepositHistory getHistoryRow() {
        return historyRow;
    }

    void setHistoryRow(PatientDepositHistory historyRow) {
        this.historyRow = historyRow;
    }

    void setWriter(WebUser creater, Department department, Institution institution) {
        this.creater = creater;
        this.department = department;
        this.institution = institution;
    }

    public WebUser getCreater() {
        return creater;
    }

    public Department getDepartment() {
        return department;
    }

    public Institution getInstitution() {
        return institution;
    }
}

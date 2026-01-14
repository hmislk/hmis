package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;

public class PaymentTypeAdmissionDTO {

    private int month;

    private long cash;
    private long cashToBeClaim;
    private long credit;

    private boolean isGrandTotal;

    // Constructor for Raw data
    public PaymentTypeAdmissionDTO(Date dateOfAdmission,
            PaymentMethod paymentMethod,
            Boolean claimable) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(dateOfAdmission);
        this.month = cal.get(Calendar.MONTH);

        if (paymentMethod == PaymentMethod.Cash) {
            if (Boolean.TRUE.equals(claimable)) {
                cashToBeClaim = 1;
            } else {
                cash = 1;
            }
        }

        if (paymentMethod == PaymentMethod.Credit) {
            credit = 1;
        }
    }

    // used for aggregation
    public PaymentTypeAdmissionDTO() {
    }

    // Add one RAW row into month
    public void add(PaymentTypeAdmissionDTO dto) {
        this.cash += dto.cash;
        this.cashToBeClaim += dto.cashToBeClaim;
        this.credit += dto.credit;
    }

    // Add to total row
    public void addAll(PaymentTypeAdmissionDTO dto) {
        add(dto);
    }

    // Helpers
    public long getTotal() {
        return cash + cashToBeClaim + credit;
    }

    public String getMonthName() {
        return new DateFormatSymbols().getMonths()[month];
    }

    // Getters & Setters
    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public long getCash() {
        return cash;
    }

    public void setCash(long cash) {
        this.cash = cash;
    }

    public long getCashToBeClaim() {
        return cashToBeClaim;
    }

    public void setCashToBeClaim(long cashToBeClaim) {
        this.cashToBeClaim = cashToBeClaim;
    }

    public long getCredit() {
        return credit;
    }

    public void setCredit(long credit) {
        this.credit = credit;
    }

    public boolean isIsGrandTotal() {
        return isGrandTotal;
    }

    public void setIsGrandTotal(boolean isGrandTotal) {
        this.isGrandTotal = isGrandTotal;
    }

}

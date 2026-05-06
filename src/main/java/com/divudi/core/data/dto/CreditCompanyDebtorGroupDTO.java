package com.divudi.core.data.dto;

import com.divudi.core.entity.Bill;
import java.util.ArrayList;
import java.util.List;

/**
 * Groups debtor bill rows by credit company, with per-company subtotals.
 * Used by CreditCompanyDebtorGroupedReportController.
 */
public class CreditCompanyDebtorGroupDTO {

    private String creditCompanyName;
    private List<Bill> bills = new ArrayList<>();
    private double subBillTotal;
    private double subSettledByCompany;
    private double subSettledByPatient;
    private double subTotalPaid;
    private double subOutstandingTotal;

    public CreditCompanyDebtorGroupDTO(String creditCompanyName) {
        this.creditCompanyName = creditCompanyName;
    }

    public void addBill(Bill b) {
        bills.add(b);
        double outstanding = b.getNetTotal() - b.getPaidAmount();
        subBillTotal += b.getNetTotal();
        subSettledByCompany += b.getSettledAmountBySponsor();
        subSettledByPatient += b.getSettledAmountByPatient();
        subTotalPaid += b.getPaidAmount();
        subOutstandingTotal += outstanding;
    }

    public String getCreditCompanyName() { return creditCompanyName; }

    public List<Bill> getBills() { return bills; }

    public double getSubBillTotal() { return subBillTotal; }

    public double getSubSettledByCompany() { return subSettledByCompany; }

    public double getSubSettledByPatient() { return subSettledByPatient; }

    public double getSubTotalPaid() { return subTotalPaid; }

    public double getSubOutstandingTotal() { return subOutstandingTotal; }
}

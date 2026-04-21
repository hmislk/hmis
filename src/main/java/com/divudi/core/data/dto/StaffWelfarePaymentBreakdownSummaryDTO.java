package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import java.io.Serializable;

/**
 * DTO for Staff Welfare Payment Breakdown Summary — one row per staff,
 * summing paidValue across ALL payment methods on their staff welfare bills.
 */
public class StaffWelfarePaymentBreakdownSummaryDTO implements Serializable {

    private String staffEpfNo;
    private Title staffTitle;
    private String staffName;
    private Double totalPaidValue;

    public StaffWelfarePaymentBreakdownSummaryDTO(
            String staffEpfNo,
            Title staffTitle,
            String staffName,
            Double totalPaidValue) {
        this.staffEpfNo = staffEpfNo;
        this.staffTitle = staffTitle;
        this.staffName = staffName;
        this.totalPaidValue = totalPaidValue != null ? totalPaidValue : 0.0;
    }

    public String getStaffNameWithTitle() {
        if (staffTitle != null) {
            return staffTitle.name().replace("_", " ") + ". " + (staffName != null ? staffName : "");
        }
        return staffName != null ? staffName : "";
    }

    public String getStaffEpfNo() { return staffEpfNo; }
    public Title getStaffTitle() { return staffTitle; }
    public String getStaffName() { return staffName; }
    public Double getTotalPaidValue() { return totalPaidValue; }
}

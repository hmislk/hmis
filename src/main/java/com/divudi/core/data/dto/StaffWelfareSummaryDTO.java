package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import java.io.Serializable;

/**
 * DTO for Staff Welfare Summary report — one row per staff member,
 * aggregating paidValue across all Staff_Welfare payments.
 */
public class StaffWelfareSummaryDTO implements Serializable {

    private String staffEpfNo;
    private Title staffTitle;
    private String staffName;
    private Double totalGrossAmount;
    private Double totalDiscount;
    private Double totalPaidValue;

    public StaffWelfareSummaryDTO(
            String staffEpfNo,
            Title staffTitle,
            String staffName,
            Double totalGrossAmount,
            Double totalDiscount,
            Double totalPaidValue) {
        this.staffEpfNo = staffEpfNo;
        this.staffTitle = staffTitle;
        this.staffName = staffName;
        this.totalGrossAmount = totalGrossAmount != null ? totalGrossAmount : 0.0;
        this.totalDiscount = totalDiscount != null ? totalDiscount : 0.0;
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
    public Double getTotalGrossAmount() { return totalGrossAmount; }
    public Double getTotalDiscount() { return totalDiscount; }
    public Double getTotalPaidValue() { return totalPaidValue; }
}

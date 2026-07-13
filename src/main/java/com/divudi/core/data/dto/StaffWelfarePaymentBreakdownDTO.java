package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Title;
import java.io.Serializable;

/**
 * DTO for Staff Welfare Payment Breakdown report — one row per payment,
 * for all payments on bills that have at least one Staff_Welfare payment.
 * Shows every payment method used, not just Staff_Welfare.
 */
public class StaffWelfarePaymentBreakdownDTO implements Serializable {

    private String billDeptId;
    private String staffEpfNo;
    private Title staffTitle;
    private String staffName;
    private PaymentMethod paymentMethod;
    private Double paidValue;

    public StaffWelfarePaymentBreakdownDTO(
            String billDeptId,
            String staffEpfNo,
            Title staffTitle,
            String staffName,
            PaymentMethod paymentMethod,
            Double paidValue) {
        this.billDeptId = billDeptId;
        this.staffEpfNo = staffEpfNo;
        this.staffTitle = staffTitle;
        this.staffName = staffName;
        this.paymentMethod = paymentMethod;
        this.paidValue = paidValue != null ? paidValue : 0.0;
    }

    public String getStaffNameWithTitle() {
        if (staffTitle != null) {
            return staffTitle.name().replace("_", " ") + ". " + (staffName != null ? staffName : "");
        }
        return staffName != null ? staffName : "";
    }

    public String getPaymentMethodLabel() {
        return paymentMethod != null ? paymentMethod.getLabel() : "";
    }

    public String getBillDeptId() { return billDeptId; }
    public String getStaffEpfNo() { return staffEpfNo; }
    public Title getStaffTitle() { return staffTitle; }
    public String getStaffName() { return staffName; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public Double getPaidValue() { return paidValue; }
}

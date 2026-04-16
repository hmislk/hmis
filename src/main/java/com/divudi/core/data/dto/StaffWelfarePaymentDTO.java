package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Staff Welfare Payment report rows.
 * Replaces entity-based loading in ServiceSummery.opdPharmacyStaffWelfarePayments()
 * to eliminate N+1 lazy-load timeouts.
 */
public class StaffWelfarePaymentDTO implements Serializable {

    private String billDeptId;
    private String staffEpfNo;
    private Title staffTitle;
    private String staffName;
    private BillTypeAtomic billTypeAtomic;
    private Date createdAt;
    private Double billTotal;
    private Double billNetTotal;
    private Double billDiscount;
    private Double paidValue;

    public StaffWelfarePaymentDTO(
            String billDeptId,
            String staffEpfNo,
            Title staffTitle,
            String staffName,
            BillTypeAtomic billTypeAtomic,
            Date createdAt,
            Double billTotal,
            Double billNetTotal,
            Double billDiscount,
            Double paidValue) {
        this.billDeptId = billDeptId;
        this.staffEpfNo = staffEpfNo;
        this.staffTitle = staffTitle;
        this.staffName = staffName;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.billTotal = billTotal != null ? billTotal : 0.0;
        this.billNetTotal = billNetTotal != null ? billNetTotal : 0.0;
        this.billDiscount = billDiscount != null ? billDiscount : 0.0;
        this.paidValue = paidValue != null ? paidValue : 0.0;
    }

    /** Returns title + name, e.g. "Dr. Silva" — replaces non-persisted nameWithTitle */
    public String getStaffNameWithTitle() {
        if (staffTitle != null) {
            return staffTitle.name().replace("_", " ") + ". " + (staffName != null ? staffName : "");
        }
        return staffName != null ? staffName : "";
    }

    /**
     * Gross amount proportional to this payment's share of the bill.
     * Mirrors the XHTML expression: p.bill.total * (p.paidValue / p.bill.netTotal)
     */
    public double getGrossAmount() {
        if (billNetTotal == null || billNetTotal == 0.0 || paidValue == null) {
            return 0.0;
        }
        return billTotal * (paidValue / billNetTotal);
    }

    /**
     * Discount proportional to this payment's share of the bill.
     * Mirrors Payment.getDiscountValue().
     */
    public double getDiscountValue() {
        if (billNetTotal == null || billNetTotal == 0.0 || paidValue == null || billDiscount == null) {
            return 0.0;
        }
        double proportion = paidValue / billNetTotal;
        return billDiscount * proportion;
    }

    public boolean isPharmacyBill() {
        if (billTypeAtomic == null) return false;
        return billTypeAtomic.name().startsWith("PHARMACY");
    }

    // Getters
    public String getBillDeptId() { return billDeptId; }
    public String getStaffEpfNo() { return staffEpfNo; }
    public Title getStaffTitle() { return staffTitle; }
    public String getStaffName() { return staffName; }
    public BillTypeAtomic getBillTypeAtomic() { return billTypeAtomic; }
    public Date getCreatedAt() { return createdAt; }
    public Double getBillTotal() { return billTotal; }
    public Double getBillNetTotal() { return billNetTotal; }
    public Double getBillDiscount() { return billDiscount; }
    public Double getPaidValue() { return paidValue; }
}

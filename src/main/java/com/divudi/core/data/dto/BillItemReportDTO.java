package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.BillCategory;

import java.io.Serializable;
import java.util.Date;

public class BillItemReportDTO implements Serializable {
    private Long billId;
    private Long billItemId;
    private String deptId;                 // Bill Number
    private String patientName;
    private String itemName;               // NEW - Item name from BillItem
    private String doctorName;
    private Double hospitalFee;            // From BillItem.hospitalFee
    private Double staffFee;               // From BillItem.staffFee (doctor fee)
    private Double netValue;               // From BillItem.netValue
    private Date billDate;
    private PaymentMethod paymentMethod;
    private BillTypeAtomic billTypeAtomic;

    private Boolean cancelled;             // Whether the bill is cancelled

    // Computed fields
    private String transactionTypeLabel;   // User-friendly bill type description
    private BillCategory billCategory;     // Direct category access

    public BillItemReportDTO() {
    }

    // Constructor for JPQL
    public BillItemReportDTO(Long billId, Long billItemId, String deptId, String patientName, String itemName,
                            String doctorName, Double hospitalFee, Double staffFee, Double netValue,
                            Date billDate, PaymentMethod paymentMethod, BillTypeAtomic billTypeAtomic,
                            Boolean cancelled) {
        this.billId = billId;
        this.billItemId = billItemId;
        this.deptId = deptId;
        this.patientName = patientName;
        this.itemName = itemName;
        this.doctorName = doctorName;
        this.hospitalFee = hospitalFee;
        this.staffFee = staffFee;
        this.netValue = netValue;
        this.billDate = billDate;
        this.paymentMethod = paymentMethod;
        this.billTypeAtomic = billTypeAtomic;
        this.cancelled = cancelled;

        // Initialize computed fields
        this.billCategory = getBillCategoryFromBillTypeAtomic(billTypeAtomic);
        this.transactionTypeLabel = getBillCategoryDisplayName(billTypeAtomic);
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getStaffFee() {
        return staffFee;
    }

    public void setStaffFee(Double staffFee) {
        this.staffFee = staffFee;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
        // Update dependent fields when bill type changes
        this.billCategory = getBillCategoryFromBillTypeAtomic(billTypeAtomic);
        this.transactionTypeLabel = getBillCategoryDisplayName(billTypeAtomic);
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getTransactionTypeLabel() {
        if (transactionTypeLabel == null) {
            transactionTypeLabel = getBillCategoryDisplayName(billTypeAtomic);
        }
        return transactionTypeLabel;
    }

    public void setTransactionTypeLabel(String transactionTypeLabel) {
        this.transactionTypeLabel = transactionTypeLabel;
    }

    public BillCategory getBillCategory() {
        if (billCategory == null) {
            billCategory = getBillCategoryFromBillTypeAtomic(billTypeAtomic);
        }
        return billCategory;
    }

    public void setBillCategory(BillCategory billCategory) {
        this.billCategory = billCategory;
    }

    // ============ HELPER METHODS FOR BILL CATEGORIZATION ============

    /**
     * Maps BillTypeAtomic to BillCategory for grouping purposes
     */
    private BillCategory getBillCategoryFromBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        if (billTypeAtomic == null) {
            return BillCategory.BILL; // Default fallback
        }

        switch (billTypeAtomic) {
            case OPD_BILL_WITH_PAYMENT:
            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case INWARD_SERVICE_BILL:
            // OPD Batch Bill Types - Normal Bills
            case OPD_BATCH_BILL_WITH_PAYMENT:
            case OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
            // Package OPD Batch Bill Types - Normal Bills
            case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
            case PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case PACKAGE_OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
                return BillCategory.BILL;
            case OPD_BILL_CANCELLATION:
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
            // OPD Batch Bill Types - Cancellations
            case OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BATCH_BILL_CANCELLATION:
                return BillCategory.CANCELLATION;
            case OPD_BILL_REFUND:
            case INWARD_SERVICE_BILL_REFUND:
            // Inward Service Batch Refund
            case INWARD_SERVICE_BATCH_BILL_REFUND:
                return BillCategory.REFUND;
            default:
                return BillCategory.BILL; // Default to BILL for unknown types
        }
    }

    /**
     * Returns user-friendly display name for bill type
     */
    private String getBillCategoryDisplayName(BillTypeAtomic billTypeAtomic) {
        if (billTypeAtomic == null) {
            return "Normal";
        }

        switch (billTypeAtomic) {
            case OPD_BILL_WITH_PAYMENT:
            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case INWARD_SERVICE_BILL:
            // OPD Batch Bill Types - Normal Bills
            case OPD_BATCH_BILL_WITH_PAYMENT:
            case OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
            // Package OPD Batch Bill Types - Normal Bills
            case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
            case PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case PACKAGE_OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
                return "Normal";
            case OPD_BILL_CANCELLATION:
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
            // OPD Batch Bill Types - Cancellations
            case OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BATCH_BILL_CANCELLATION:
                return "Cancellation";
            case OPD_BILL_REFUND:
            case INWARD_SERVICE_BILL_REFUND:
            // Inward Service Batch Refund
            case INWARD_SERVICE_BATCH_BILL_REFUND:
                return "Return";
            default:
                return "Normal"; // Default to Normal for unknown types
        }
    }
}

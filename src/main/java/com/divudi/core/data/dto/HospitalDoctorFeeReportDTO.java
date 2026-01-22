package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.BillCategory;

import java.io.Serializable;
import java.util.Date;

public class HospitalDoctorFeeReportDTO implements Serializable {
    private Long billId;
    private String patientName;
    private String doctorName;
    private Double hospitalFee;
    private Double doctorFee;
    private Double netTotal;
    private Date billDate;
    private PaymentMethod paymentMethod;
    private BillTypeAtomic billTypeAtomic;

    // Enhanced transaction fields
    private String billReferenceNo;        // For tracking cancellations/returns
    private Date originalBillDate;         // For cancellation/return reference
    private String transactionTypeLabel;   // User-friendly bill type description
    private BillCategory billCategory;     // Direct category access

    public HospitalDoctorFeeReportDTO() {
    }

    // Constructor for JPQL
    public HospitalDoctorFeeReportDTO(Long billId, String patientName, String doctorName,
                                     Double hospitalFee, Double doctorFee, Double netTotal,
                                     Date billDate, PaymentMethod paymentMethod, BillTypeAtomic billTypeAtomic) {
        this.billId = billId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.hospitalFee = hospitalFee;
        this.doctorFee = doctorFee;
        this.netTotal = netTotal;
        this.billDate = billDate;
        this.paymentMethod = paymentMethod;
        this.billTypeAtomic = billTypeAtomic;

        // Initialize computed fields
        this.billCategory = getBillCategoryFromBillTypeAtomic(billTypeAtomic);
        this.transactionTypeLabel = getBillCategoryDisplayName(billTypeAtomic);
    }

    // Enhanced constructor for JPQL with additional fields
    public HospitalDoctorFeeReportDTO(Long billId, String patientName, String doctorName,
                                     Double hospitalFee, Double doctorFee, Double netTotal,
                                     Date billDate, PaymentMethod paymentMethod, BillTypeAtomic billTypeAtomic,
                                     String billReferenceNo, Date originalBillDate) {
        this(billId, patientName, doctorName, hospitalFee, doctorFee, netTotal, billDate, paymentMethod, billTypeAtomic);
        this.billReferenceNo = billReferenceNo;
        this.originalBillDate = originalBillDate;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
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

    public Double getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(Double doctorFee) {
        this.doctorFee = doctorFee;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
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

    // ============ ENHANCED TRANSACTION FIELD GETTERS AND SETTERS ============

    public String getBillReferenceNo() {
        return billReferenceNo;
    }

    public void setBillReferenceNo(String billReferenceNo) {
        this.billReferenceNo = billReferenceNo;
    }

    public Date getOriginalBillDate() {
        return originalBillDate;
    }

    public void setOriginalBillDate(Date originalBillDate) {
        this.originalBillDate = originalBillDate;
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
                return BillCategory.BILL;
            case OPD_BILL_CANCELLATION:
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return BillCategory.CANCELLATION;
            case OPD_BILL_REFUND:
            case INWARD_SERVICE_BILL_REFUND:
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
                return "Normal";
            case OPD_BILL_CANCELLATION:
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return "Cancellation";
            case OPD_BILL_REFUND:
            case INWARD_SERVICE_BILL_REFUND:
                return "Return";
            default:
                return "Normal"; // Default to Normal for unknown types
        }
    }
}
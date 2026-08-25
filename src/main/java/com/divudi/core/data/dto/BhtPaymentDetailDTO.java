package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for BHT Deposit and Credit Settlement Detail Report.
 * One instance per individual payment (deposit, post-final/"Make Payment"
 * settlement, or CC settlement) - see {@link #getPaymentCategory()}.
 */
public class BhtPaymentDetailDTO implements Serializable {

    private String bhtNo;
    private String patientName;
    private AdmissionType admissionType;
    private Date dateOfAdmission;
    private Date dateOfDischarge;
    private String billNo;
    private Date createdAt;
    private PaymentMethod paymentMethod;
    private double amount;
    private String referenceNo;
    private String creditCompanyName;

    /**
     * Distinguishes which kind of transaction this row is: "Deposit"
     * (Make Deposit), "Final Payment" (Make Payment / post-final-bill
     * settlement), or "CC Settlement" (credit company payment). Not set by
     * every caller of this DTO (e.g. {@code BhtDepositDetailReportController}
     * predates this field) - callers that don't set it leave rows blank
     * rather than defaulting to a possibly-wrong category.
     */
    private String paymentCategory;

    public BhtPaymentDetailDTO() {
    }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public Date getDateOfAdmission() { return dateOfAdmission; }
    public void setDateOfAdmission(Date dateOfAdmission) { this.dateOfAdmission = dateOfAdmission; }

    public Date getDateOfDischarge() { return dateOfDischarge; }
    public void setDateOfDischarge(Date dateOfDischarge) { this.dateOfDischarge = dateOfDischarge; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getReferenceNo() { return referenceNo != null ? referenceNo : ""; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getCreditCompanyName() { return creditCompanyName != null ? creditCompanyName : ""; }
    public void setCreditCompanyName(String creditCompanyName) { this.creditCompanyName = creditCompanyName; }

    public String getPaymentCategory() { return paymentCategory != null ? paymentCategory : ""; }
    public void setPaymentCategory(String paymentCategory) { this.paymentCategory = paymentCategory; }
}

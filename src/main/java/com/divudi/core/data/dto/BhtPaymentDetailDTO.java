package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for BHT Deposit and Credit Settlement Detail Report.
 * One instance per individual payment (deposit or CC settlement).
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
}

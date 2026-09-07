package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Title;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;

/**
 * DTO for Admission Category Wise Admission Report (Issue #17864).
 * One row per Admission, grouped/sorted by admission category (AdmissionType).
 */
public class AdmissionCategoryWiseAdmissionDTO implements Serializable {

    private Long admissionId;
    private String bhtNo;
    private String patientName;
    private String categoryName;
    private PaymentMethod paymentMethod;
    private boolean paymentFinalized;

    private double advance;
    private double professionalFees;
    private double hospitalAmount;
    private double sponsorAmount;
    private double patientAmount;
    private double discount;
    private double invoiceAmount;
    private double billBalance;
    private double patientBalance;

    public AdmissionCategoryWiseAdmissionDTO() {
    }

    /**
     * Constructor used by JPQL SELECT NEW. Financial fields are populated in a
     * post-query enrichment step in InwardReportController.
     */
    public AdmissionCategoryWiseAdmissionDTO(
            Long admissionId,
            String bhtNo,
            String patientName,
            Title patientTitle,
            AdmissionType admissionType,
            PaymentMethod paymentMethod,
            Boolean paymentFinalized) {

        this.admissionId = admissionId;
        this.bhtNo = bhtNo != null ? bhtNo : "";
        if (patientTitle != null && patientName != null) {
            this.patientName = patientTitle.getLabel() + " " + patientName;
        } else {
            this.patientName = patientName != null ? patientName : "";
        }
        this.categoryName = admissionType != null && admissionType.getName() != null
                ? admissionType.getName() : "";
        this.paymentMethod = paymentMethod;
        this.paymentFinalized = Boolean.TRUE.equals(paymentFinalized);
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isPaymentFinalized() {
        return paymentFinalized;
    }

    public void setPaymentFinalized(boolean paymentFinalized) {
        this.paymentFinalized = paymentFinalized;
    }

    public double getAdvance() {
        return advance;
    }

    public void setAdvance(double advance) {
        this.advance = advance;
    }

    public double getProfessionalFees() {
        return professionalFees;
    }

    public void setProfessionalFees(double professionalFees) {
        this.professionalFees = professionalFees;
    }

    public double getHospitalAmount() {
        return hospitalAmount;
    }

    public void setHospitalAmount(double hospitalAmount) {
        this.hospitalAmount = hospitalAmount;
    }

    public double getSponsorAmount() {
        return sponsorAmount;
    }

    public void setSponsorAmount(double sponsorAmount) {
        this.sponsorAmount = sponsorAmount;
    }

    public double getPatientAmount() {
        return patientAmount;
    }

    public void setPatientAmount(double patientAmount) {
        this.patientAmount = patientAmount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getInvoiceAmount() {
        return invoiceAmount;
    }

    public void setInvoiceAmount(double invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public double getBillBalance() {
        return billBalance;
    }

    public void setBillBalance(double billBalance) {
        this.billBalance = billBalance;
    }

    public double getPatientBalance() {
        return patientBalance;
    }

    public void setPatientBalance(double patientBalance) {
        this.patientBalance = patientBalance;
    }
}

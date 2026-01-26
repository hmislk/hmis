package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Payment entity details
 * Used in Bill API responses to provide complete payment information
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class PaymentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long billId;
    private String paymentMethod;
    private Double paidValue;
    private Date createdAt;
    private Date paymentDate;

    // Bank details
    private Long bankId;
    private String bankName;

    // Credit company details
    private Long creditCompanyId;
    private String creditCompanyName;

    // Staff welfare details
    private Long toStaffId;
    private String toStaffName;

    // Additional fields
    private String referenceNo;
    private String policyNo;
    private String chequeRefNo;
    private Date chequeDate;
    private String comments;
    private Boolean retired;

    // Constructors
    public PaymentDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getPaidValue() {
        return paidValue;
    }

    public void setPaidValue(Double paidValue) {
        this.paidValue = paidValue;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public Long getCreditCompanyId() {
        return creditCompanyId;
    }

    public void setCreditCompanyId(Long creditCompanyId) {
        this.creditCompanyId = creditCompanyId;
    }

    public String getCreditCompanyName() {
        return creditCompanyName;
    }

    public void setCreditCompanyName(String creditCompanyName) {
        this.creditCompanyName = creditCompanyName;
    }

    public Long getToStaffId() {
        return toStaffId;
    }

    public void setToStaffId(Long toStaffId) {
        this.toStaffId = toStaffId;
    }

    public String getToStaffName() {
        return toStaffName;
    }

    public void setToStaffName(String toStaffName) {
        this.toStaffName = toStaffName;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getPolicyNo() {
        return policyNo;
    }

    public void setPolicyNo(String policyNo) {
        this.policyNo = policyNo;
    }

    public String getChequeRefNo() {
        return chequeRefNo;
    }

    public void setChequeRefNo(String chequeRefNo) {
        this.chequeRefNo = chequeRefNo;
    }

    public Date getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(Date chequeDate) {
        this.chequeDate = chequeDate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    @Override
    public String toString() {
        return "PaymentDTO{" +
                "id=" + id +
                ", billId=" + billId +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paidValue=" + paidValue +
                ", createdAt=" + createdAt +
                ", paymentDate=" + paymentDate +
                '}';
    }
}

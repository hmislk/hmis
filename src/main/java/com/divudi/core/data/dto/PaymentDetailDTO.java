package com.divudi.core.data.dto;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Payment details needed for Daily Return reports.
 */
public class PaymentDetailDTO implements Serializable {
    private String billNumber;
    private BillType billType;
    private PaymentMethod paymentMethod;
    private Double paidValue;
    private Date createdAt;
    private String creditCardRefNo;
    private String bankName;
    private String institutionName;
    private String departmentName;

    public PaymentDetailDTO() {
    }

    public PaymentDetailDTO(String billNumber, BillType billType, PaymentMethod paymentMethod,
                           Double paidValue, Date createdAt, String creditCardRefNo, 
                           String bankName, String institutionName, String departmentName) {
        this.billNumber = billNumber;
        this.billType = billType;
        this.paymentMethod = paymentMethod;
        this.paidValue = paidValue;
        this.createdAt = createdAt;
        this.creditCardRefNo = creditCardRefNo;
        this.bankName = bankName;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
    }

    public String getBillNumber() {
        return billNumber != null ? billNumber : "";
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getPaidValue() {
        return paidValue != null ? paidValue : 0.0;
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

    public String getCreditCardRefNo() {
        return creditCardRefNo != null ? creditCardRefNo : "";
    }

    public void setCreditCardRefNo(String creditCardRefNo) {
        this.creditCardRefNo = creditCardRefNo;
    }

    public String getBankName() {
        return bankName != null ? bankName : "";
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getInstitutionName() {
        return institutionName != null ? institutionName : "";
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDepartmentName() {
        return departmentName != null ? departmentName : "";
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
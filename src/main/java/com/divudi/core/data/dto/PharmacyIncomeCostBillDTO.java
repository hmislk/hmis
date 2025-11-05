package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class PharmacyIncomeCostBillDTO implements Serializable {

    private Long id;
    private String billNo;
    private BillTypeAtomic billTypeAtomic;
    private String patientName;
    private String bhtNo;
    private Date createdAt;
    private BigDecimal retailValue;
    private BigDecimal purchaseValue;

    public PharmacyIncomeCostBillDTO() {
    }

    public PharmacyIncomeCostBillDTO(Long id, String billNo, BillTypeAtomic billTypeAtomic,
                                      String patientName, String bhtNo, Date createdAt,
                                      BigDecimal retailValue, BigDecimal purchaseValue) {
        this.id = id;
        this.billNo = billNo;
        this.billTypeAtomic = billTypeAtomic;
        this.patientName = patientName;
        this.bhtNo = bhtNo;
        this.createdAt = createdAt;
        this.retailValue = retailValue;
        this.purchaseValue = purchaseValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(BigDecimal retailValue) {
        this.retailValue = retailValue;
    }

    public BigDecimal getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(BigDecimal purchaseValue) {
        this.purchaseValue = purchaseValue;
    }
}

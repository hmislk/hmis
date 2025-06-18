package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class PharmacyIncomeCostBillItemDTO implements Serializable {

    private Long billId;
    private String billNo;
    private BillTypeAtomic billTypeAtomic;
    private String patientName;
    private String bhtNo;
    private Date createdAt;
    private String itemName;
    private Double qty;
    private BigDecimal retailRate;
    private BigDecimal purchaseRate;

    public PharmacyIncomeCostBillItemDTO() {
    }

    public PharmacyIncomeCostBillItemDTO(Long billId, String billNo, BillTypeAtomic billTypeAtomic,
                                         String patientName, String bhtNo, Date createdAt,
                                         String itemName, Double qty,
                                         BigDecimal retailRate, BigDecimal purchaseRate) {
        this.billId = billId;
        this.billNo = billNo;
        this.billTypeAtomic = billTypeAtomic;
        this.patientName = patientName;
        this.bhtNo = bhtNo;
        this.createdAt = createdAt;
        this.itemName = itemName;
        this.qty = qty;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public BigDecimal getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(BigDecimal retailRate) {
        this.retailRate = retailRate;
    }

    public BigDecimal getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(BigDecimal purchaseRate) {
        this.purchaseRate = purchaseRate;
    }
}

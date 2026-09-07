package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class BillItemDetailsDTO implements Serializable {

    private Long id;
    private Date createdAt;
    private Long billId;
    private Long itemId;
    private Long billItemFinanceDetailsId;
    private Double qty;
    private Double rate;
    private Double netRate;
    private Double grossValue;
    private Double netValue;
    private Boolean retired;

    // Bill Item Finance Details
    private BillItemFinanceDetailsDTO billItemFinanceDetails;

    // Pharmaceutical Bill Item
    private PharmaceuticalBillItemDTO pharmaceuticalBillItem;

    public BillItemDetailsDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getBillItemFinanceDetailsId() {
        return billItemFinanceDetailsId;
    }

    public void setBillItemFinanceDetailsId(Long billItemFinanceDetailsId) {
        this.billItemFinanceDetailsId = billItemFinanceDetailsId;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getNetRate() {
        return netRate;
    }

    public void setNetRate(Double netRate) {
        this.netRate = netRate;
    }

    public Double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public BillItemFinanceDetailsDTO getBillItemFinanceDetails() {
        return billItemFinanceDetails;
    }

    public void setBillItemFinanceDetails(BillItemFinanceDetailsDTO billItemFinanceDetails) {
        this.billItemFinanceDetails = billItemFinanceDetails;
    }

    public PharmaceuticalBillItemDTO getPharmaceuticalBillItem() {
        return pharmaceuticalBillItem;
    }

    public void setPharmaceuticalBillItem(PharmaceuticalBillItemDTO pharmaceuticalBillItem) {
        this.pharmaceuticalBillItem = pharmaceuticalBillItem;
    }
}

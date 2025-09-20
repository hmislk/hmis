package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class PharmacyGrnReturnItemDTO implements Serializable {
    private String grnReturnNo;
    private String departmentName;
    private Date createdAt;
    private String supplierName;
    private String itemName;
    private BigDecimal quantityReturned;
    private BigDecimal freeQuantityReturned;
    private Double purchaseRate;
    private BigDecimal saleRate;
    private BigDecimal returnedRate;
    private BigDecimal returnValue;

    public PharmacyGrnReturnItemDTO() {
    }

    // Original constructor for backward compatibility
    public PharmacyGrnReturnItemDTO(String grnReturnNo,
                                    String departmentName,
                                    Date createdAt,
                                    String supplierName,
                                    String itemName,
                                    BigDecimal quantityReturned,
                                    BigDecimal freeQuantityReturned,
                                    Double purchaseRate,
                                    BigDecimal saleRate,
                                    BigDecimal returnedRate,
                                    BigDecimal returnValue) {
        this.grnReturnNo = grnReturnNo;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.supplierName = supplierName;
        this.itemName = itemName;
        this.quantityReturned = quantityReturned;
        this.freeQuantityReturned = freeQuantityReturned;
        this.purchaseRate = purchaseRate;
        this.saleRate = saleRate;
        this.returnedRate = returnedRate;
        this.returnValue = returnValue;
    }

    // New constructor using BillItem netRate and netValue
    public PharmacyGrnReturnItemDTO(String grnReturnNo,
                                    String departmentName,
                                    Date createdAt,
                                    String supplierName,
                                    String itemName,
                                    BigDecimal quantityReturned,
                                    BigDecimal freeQuantityReturned,
                                    Double purchaseRate,
                                    BigDecimal saleRate,
                                    Double billItemNetRate,
                                    Double billItemNetValue) {
        this.grnReturnNo = grnReturnNo;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.supplierName = supplierName;
        this.itemName = itemName;
        this.quantityReturned = quantityReturned;
        this.freeQuantityReturned = freeQuantityReturned;
        this.purchaseRate = purchaseRate;
        this.saleRate = saleRate;
        this.returnedRate = billItemNetRate != null ? BigDecimal.valueOf(billItemNetRate) : null;
        this.returnValue = billItemNetValue != null ? BigDecimal.valueOf(billItemNetValue) : null;
    }

    public String getGrnReturnNo() {
        return grnReturnNo;
    }

    public void setGrnReturnNo(String grnReturnNo) {
        this.grnReturnNo = grnReturnNo;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getQuantityReturned() {
        return quantityReturned;
    }

    public void setQuantityReturned(BigDecimal quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public BigDecimal getFreeQuantityReturned() {
        return freeQuantityReturned;
    }

    public void setFreeQuantityReturned(BigDecimal freeQuantityReturned) {
        this.freeQuantityReturned = freeQuantityReturned;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public BigDecimal getSaleRate() {
        return saleRate;
    }

    public void setSaleRate(BigDecimal saleRate) {
        this.saleRate = saleRate;
    }

    public BigDecimal getReturnedRate() {
        return returnedRate;
    }

    public void setReturnedRate(BigDecimal returnedRate) {
        this.returnedRate = returnedRate;
    }

    public BigDecimal getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(BigDecimal returnValue) {
        this.returnValue = returnValue;
    }
}

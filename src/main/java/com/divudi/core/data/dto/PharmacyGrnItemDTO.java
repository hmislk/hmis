package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class PharmacyGrnItemDTO implements Serializable {
    private String grnNo;
    private String departmentName;
    private Date createdAt;
    private String poNo;
    private String supplierName;
    private String itemName;
    private Double quantity;
    private Double freeQuantity;
    private Double purchaseRate;
    private Double totalCostRate;
    private Double retailSaleRate;
    private Double netTotal;

    public PharmacyGrnItemDTO() {
    }

    public PharmacyGrnItemDTO(String grnNo, String departmentName, Date createdAt,
                              String poNo, String supplierName, String itemName,
                              Double quantity, Double freeQuantity,
                              Double purchaseRate, Double totalCostRate,
                              Double retailSaleRate, Double netTotal) {
        this.grnNo = grnNo;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.poNo = poNo;
        this.supplierName = supplierName;
        this.itemName = itemName;
        this.quantity = quantity;
        this.freeQuantity = freeQuantity;
        this.purchaseRate = purchaseRate;
        this.totalCostRate = totalCostRate;
        this.retailSaleRate = retailSaleRate;
        this.netTotal = netTotal;
    }

    public String getGrnNo() {
        return grnNo;
    }

    public void setGrnNo(String grnNo) {
        this.grnNo = grnNo;
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

    public String getPoNo() {
        return poNo;
    }

    public void setPoNo(String poNo) {
        this.poNo = poNo;
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

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(Double freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getTotalCostRate() {
        return totalCostRate;
    }

    public void setTotalCostRate(Double totalCostRate) {
        this.totalCostRate = totalCostRate;
    }

    public Double getRetailSaleRate() {
        return retailSaleRate;
    }

    public void setRetailSaleRate(Double retailSaleRate) {
        this.retailSaleRate = retailSaleRate;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }
}

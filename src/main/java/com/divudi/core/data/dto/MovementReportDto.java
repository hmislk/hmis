package com.divudi.core.data.dto;

import java.io.Serializable;

public class MovementReportDto implements Serializable {

    private Long itemId;
    private String itemCode;
    private String itemName;
    private String categoryName;
    private String dosageFormName;
    private String lastPurchaseSupplierName;
    private Double qty;
    private Double purchaseValue;
    private Double retailsaleValue;
    private Double stockQty;
    private Double stockOnHand;

    public MovementReportDto() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemCode() {
        return itemCode != null ? itemCode : "";
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName != null ? itemName : "";
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "";
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDosageFormName() {
        return dosageFormName != null ? dosageFormName : "";
    }

    public void setDosageFormName(String dosageFormName) {
        this.dosageFormName = dosageFormName;
    }

    public String getLastPurchaseSupplierName() {
        return lastPurchaseSupplierName != null ? lastPurchaseSupplierName : "";
    }

    public void setLastPurchaseSupplierName(String lastPurchaseSupplierName) {
        this.lastPurchaseSupplierName = lastPurchaseSupplierName;
    }

    public Double getQty() {
        return qty != null ? qty : 0.0;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getPurchaseValue() {
        return purchaseValue != null ? purchaseValue : 0.0;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getRetailsaleValue() {
        return retailsaleValue != null ? retailsaleValue : 0.0;
    }

    public void setRetailsaleValue(Double retailsaleValue) {
        this.retailsaleValue = retailsaleValue;
    }

    public Double getStockQty() {
        return stockQty != null ? stockQty : 0.0;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Double getStockOnHand() {
        return stockOnHand != null ? stockOnHand : 0.0;
    }

    public void setStockOnHand(Double stockOnHand) {
        this.stockOnHand = stockOnHand;
    }
}

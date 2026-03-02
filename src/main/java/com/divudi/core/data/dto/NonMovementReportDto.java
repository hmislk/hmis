package com.divudi.core.data.dto;

import java.io.Serializable;

public class NonMovementReportDto implements Serializable {

    private Long itemId;
    private String itemCode;
    private String itemName;
    private String categoryName;
    private String dosageFormName;
    private String lastSupplierName;

    public NonMovementReportDto() {
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

    public String getLastSupplierName() {
        return lastSupplierName != null ? lastSupplierName : "";
    }

    public void setLastSupplierName(String lastSupplierName) {
        this.lastSupplierName = lastSupplierName;
    }
}

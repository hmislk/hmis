package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Expiry Item List Report
 * Displays aggregated item information without cost/retail rates
 * Aggregated at the item level across all batches and stocks
 */
public class ExpiryItemListDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long itemId;                  // Hidden for references only
    private String departmentName;
    private String categoryCode;
    private String categoryName;
    private String itemCode;
    private String itemName;
    private String uom;                   // Unit of Measure
    private String itemType;
    private String batchNumber;
    private Date expiryDate;              // Earliest expiry date for this item
    private Double totalStockQuantity;    // Aggregated quantity across all batches

    public ExpiryItemListDto() {
    }

    // Constructor for JPQL query - direct DTO mapping for aggregated data
    public ExpiryItemListDto(Long itemId, String departmentName, String categoryCode,
                           String categoryName, String itemCode, String itemName,
                           String uom, String itemType, String batchNumber,
                           Date expiryDate, Double totalStockQuantity) {
        this.itemId = itemId;
        this.departmentName = departmentName;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.uom = uom;
        this.itemType = itemType;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.totalStockQuantity = totalStockQuantity;
    }

    // Getters and Setters
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public void setTotalStockQuantity(Double totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
    }
}
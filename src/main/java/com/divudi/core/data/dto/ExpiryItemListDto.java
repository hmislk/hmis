package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Expiry Item List Report
 * Displays item-level aggregated information across all batches
 * Shows totals for quantities and values aggregated per item
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
    private Long batchNumber;             // NULL for item-level aggregation (no single batch per item)
    private Date expiryDate;              // Earliest expiry date across all batches of this item
    private Double totalStockQuantity;    // Sum of stock quantities across all batches of this item
    private Double totalCostValue;        // Sum of (batch_quantity * cost_rate) across all batches of this item
    private Double totalRetailValue;      // Sum of (batch_quantity * retail_rate) across all batches of this item

    public ExpiryItemListDto() {
    }

    // Constructor for JPQL query - direct DTO mapping for aggregated data
    public ExpiryItemListDto(Long itemId, String departmentName, String categoryCode,
                           String categoryName, String itemCode, String itemName,
                           String uom, String itemType, Long batchNumber,
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

    // Constructor for JPQL query with value calculations - enhanced version
    public ExpiryItemListDto(Long itemId, String departmentName, String categoryCode,
                           String categoryName, String itemCode, String itemName,
                           String uom, String itemType, Long batchNumber,
                           Date expiryDate, Double totalStockQuantity,
                           Double totalCostValue, Double totalRetailValue) {
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
        this.totalCostValue = totalCostValue;
        this.totalRetailValue = totalRetailValue;
    }

    // Constructor for item-level aggregation (no batch number since aggregated across batches)
    public ExpiryItemListDto(Long itemId, String departmentName, String categoryCode,
                           String categoryName, String itemCode, String itemName,
                           String uom, String itemType, Date earliestExpiryDate,
                           Double totalStockQuantity, Double totalCostValue,
                           Double totalRetailValue) {
        this.itemId = itemId;
        this.departmentName = departmentName;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.uom = uom;
        this.itemType = itemType;
        this.batchNumber = null; // No single batch for item-level aggregation
        this.expiryDate = earliestExpiryDate;
        this.totalStockQuantity = totalStockQuantity;
        this.totalCostValue = totalCostValue;
        this.totalRetailValue = totalRetailValue;
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

    public Long getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(Long batchNumber) {
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

    public Double getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(Double totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public Double getTotalRetailValue() {
        return totalRetailValue;
    }

    public void setTotalRetailValue(Double totalRetailValue) {
        this.totalRetailValue = totalRetailValue;
    }
}
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Expiry Item Stock List Report
 * Displays detailed stock information with cost and retail rates
 */
public class ExpiryItemStockListDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long stockId;                 // Hidden for references only
    private String departmentName;
    private String categoryCode;
    private String categoryName;
    private String itemCode;
    private String itemName;
    private String uom;                   // Unit of Measure
    private String itemType;
    private Long batchNumber;
    private Date expiryDate;
    private Double costRate;
    private Double retailRate;
    private Double stockQuantity;

    // Calculated fields
    private Double valueAtCostRate;       // stockQuantity * costRate
    private Double valueAtRetailRate;     // stockQuantity * retailRate

    public ExpiryItemStockListDto() {
    }

    // Constructor for JPQL query - direct DTO mapping
    public ExpiryItemStockListDto(Long stockId, String departmentName, String categoryCode,
                                String categoryName, String itemCode, String itemName,
                                String uom, String itemType, Long batchNumber,
                                Date expiryDate, Double costRate, Double retailRate,
                                Double stockQuantity) {
        this.stockId = stockId;
        this.departmentName = departmentName;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.uom = uom;
        this.itemType = itemType;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.costRate = costRate;
        this.retailRate = retailRate;
        this.stockQuantity = stockQuantity;

        // Calculate values
        this.valueAtCostRate = (costRate != null && stockQuantity != null) ? costRate * stockQuantity : 0.0;
        this.valueAtRetailRate = (retailRate != null && stockQuantity != null) ? retailRate * stockQuantity : 0.0;
    }

    // Getters and Setters
    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
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

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
        // Recalculate value when rate changes
        this.valueAtCostRate = (costRate != null && stockQuantity != null) ? costRate * stockQuantity : 0.0;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
        // Recalculate value when rate changes
        this.valueAtRetailRate = (retailRate != null && stockQuantity != null) ? retailRate * stockQuantity : 0.0;
    }

    public Double getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Double stockQuantity) {
        this.stockQuantity = stockQuantity;
        // Recalculate values when quantity changes
        this.valueAtCostRate = (costRate != null && stockQuantity != null) ? costRate * stockQuantity : 0.0;
        this.valueAtRetailRate = (retailRate != null && stockQuantity != null) ? retailRate * stockQuantity : 0.0;
    }

    public Double getValueAtCostRate() {
        return valueAtCostRate;
    }

    public void setValueAtCostRate(Double valueAtCostRate) {
        this.valueAtCostRate = valueAtCostRate;
    }

    public Double getValueAtRetailRate() {
        return valueAtRetailRate;
    }

    public void setValueAtRetailRate(Double valueAtRetailRate) {
        this.valueAtRetailRate = valueAtRetailRate;
    }
}
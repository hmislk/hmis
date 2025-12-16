package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for Items with supplier information and stock data
 * Used in dropdowns to avoid loading full entities
 *
 * @author Dr M H B Ariyaratne
 */
public class ItemSupplierDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Item identification
    private Long itemId;
    private String itemName;
    private String itemCode;

    // Stock information
    private Double departmentStock;
    private BigDecimal unitsPerPack;

    // Usage data
    private Double usageCount;

    // Supplier information
    private Long supplierId;
    private String supplierName;

    // Last purchase information
    private Double lastPurchaseRate;
    private Double lastRetailRate;

    // Default constructor
    public ItemSupplierDto() {
        this.departmentStock = 0.0;
        this.usageCount = 0.0;
        this.unitsPerPack = BigDecimal.ONE;
        this.lastPurchaseRate = 0.0;
        this.lastRetailRate = 0.0;
    }

    // Constructor for JPQL queries (dealer items with stock)
    public ItemSupplierDto(
            Long itemId,
            String itemName,
            String itemCode,
            Double departmentStock,
            Object unitsPerPack) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.departmentStock = departmentStock != null ? departmentStock : 0.0;

        // Handle unitsPerPack which may come as Double from JPQL
        if (unitsPerPack != null) {
            if (unitsPerPack instanceof BigDecimal) {
                this.unitsPerPack = (BigDecimal) unitsPerPack;
            } else if (unitsPerPack instanceof Double) {
                this.unitsPerPack = BigDecimal.valueOf((Double) unitsPerPack);
            } else {
                this.unitsPerPack = BigDecimal.ONE;
            }
        } else {
            this.unitsPerPack = BigDecimal.ONE;
        }

        this.usageCount = 0.0;
        this.lastPurchaseRate = 0.0;
        this.lastRetailRate = 0.0;
    }

    // Full constructor with all fields
    public ItemSupplierDto(
            Long itemId,
            String itemName,
            String itemCode,
            Double departmentStock,
            BigDecimal unitsPerPack,
            Double usageCount,
            Long supplierId,
            String supplierName,
            Double lastPurchaseRate,
            Double lastRetailRate) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.departmentStock = departmentStock != null ? departmentStock : 0.0;
        this.unitsPerPack = unitsPerPack != null ? unitsPerPack : BigDecimal.ONE;
        this.usageCount = usageCount != null ? usageCount : 0.0;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.lastPurchaseRate = lastPurchaseRate != null ? lastPurchaseRate : 0.0;
        this.lastRetailRate = lastRetailRate != null ? lastRetailRate : 0.0;
    }

    // Getters and Setters
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Double getDepartmentStock() {
        return departmentStock;
    }

    public void setDepartmentStock(Double departmentStock) {
        this.departmentStock = departmentStock;
    }

    public BigDecimal getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(BigDecimal unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public Double getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Double usageCount) {
        this.usageCount = usageCount;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Double getLastPurchaseRate() {
        return lastPurchaseRate;
    }

    public void setLastPurchaseRate(Double lastPurchaseRate) {
        this.lastPurchaseRate = lastPurchaseRate;
    }

    public Double getLastRetailRate() {
        return lastRetailRate;
    }

    public void setLastRetailRate(Double lastRetailRate) {
        this.lastRetailRate = lastRetailRate;
    }

    // Helper method to get display label
    public String getDisplayLabel() {
        return itemName + " - " + itemCode;
    }

    @Override
    public String toString() {
        return "ItemSupplierDto{" +
                "itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", itemCode='" + itemCode + '\'' +
                ", departmentStock=" + departmentStock +
                '}';
    }
}

package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for Purchase Order Item with pre-calculated stock and usage data
 * to avoid per-row method calls in UI
 *
 * @author Dr M H B Ariyaratne
 */
public class PurchaseOrderItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // Item identification
    private Long itemId;
    private String itemName;
    private String itemCode;

    // Serial number for display
    private Integer serialNo;

    // Financial details
    private BigDecimal quantity;
    private BigDecimal freeQuantity;
    private BigDecimal purchaseRate;
    private BigDecimal retailRate;
    private BigDecimal lineTotal;
    private BigDecimal unitsPerPack;

    // Pre-calculated stock and usage (performance optimization)
    private Double stockInUnits;
    private Double usageCount;
    private Double expiringStock;

    // Link to actual BillItem entity (for editing operations)
    private Long billItemId;

    // Flags
    private boolean retired;

    // Default constructor
    public PurchaseOrderItemDto() {
        this.quantity = BigDecimal.ZERO;
        this.freeQuantity = BigDecimal.ZERO;
        this.purchaseRate = BigDecimal.ZERO;
        this.retailRate = BigDecimal.ZERO;
        this.lineTotal = BigDecimal.ZERO;
        this.unitsPerPack = BigDecimal.ONE;
        this.stockInUnits = 0.0;
        this.usageCount = 0.0;
        this.expiringStock = 0.0;
        this.retired = false;
    }

    // Full constructor for JPQL queries
    public PurchaseOrderItemDto(
            Long billItemId,
            Integer serialNo,
            Long itemId,
            String itemName,
            String itemCode,
            BigDecimal quantity,
            BigDecimal freeQuantity,
            BigDecimal purchaseRate,
            BigDecimal retailRate,
            BigDecimal lineTotal,
            BigDecimal unitsPerPack,
            Double stockInUnits,
            Double usageCount) {
        this.billItemId = billItemId;
        this.serialNo = serialNo;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.freeQuantity = freeQuantity != null ? freeQuantity : BigDecimal.ZERO;
        this.purchaseRate = purchaseRate != null ? purchaseRate : BigDecimal.ZERO;
        this.retailRate = retailRate != null ? retailRate : BigDecimal.ZERO;
        this.lineTotal = lineTotal != null ? lineTotal : BigDecimal.ZERO;
        this.unitsPerPack = unitsPerPack != null ? unitsPerPack : BigDecimal.ONE;
        this.stockInUnits = stockInUnits != null ? stockInUnits : 0.0;
        this.usageCount = usageCount != null ? usageCount : 0.0;
        this.expiringStock = 0.0;
        this.retired = false;
    }

    // Constructor for simple item data (used when adding items)
    public PurchaseOrderItemDto(
            Long itemId,
            String itemName,
            String itemCode,
            BigDecimal unitsPerPack,
            Double stockInUnits,
            Double usageCount) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.unitsPerPack = unitsPerPack != null ? unitsPerPack : BigDecimal.ONE;
        this.stockInUnits = stockInUnits != null ? stockInUnits : 0.0;
        this.usageCount = usageCount != null ? usageCount : 0.0;
        this.quantity = BigDecimal.ZERO;
        this.freeQuantity = BigDecimal.ZERO;
        this.purchaseRate = BigDecimal.ZERO;
        this.retailRate = BigDecimal.ZERO;
        this.lineTotal = BigDecimal.ZERO;
        this.retired = false;
    }

    // Calculate line total
    public void calculateLineTotal() {
        if (quantity != null && purchaseRate != null) {
            this.lineTotal = quantity.multiply(purchaseRate);
        } else {
            this.lineTotal = BigDecimal.ZERO;
        }
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

    public Integer getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(Integer serialNo) {
        this.serialNo = serialNo;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(BigDecimal freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public BigDecimal getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(BigDecimal purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public BigDecimal getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(BigDecimal retailRate) {
        this.retailRate = retailRate;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(BigDecimal unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public Double getStockInUnits() {
        return stockInUnits;
    }

    public void setStockInUnits(Double stockInUnits) {
        this.stockInUnits = stockInUnits;
    }

    public Double getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Double usageCount) {
        this.usageCount = usageCount;
    }

    public Double getExpiringStock() {
        return expiringStock;
    }

    public void setExpiringStock(Double expiringStock) {
        this.expiringStock = expiringStock;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    // Helper method to get display name
    public String getDisplayName() {
        return itemName + " - " + itemCode;
    }

    @Override
    public String toString() {
        return "PurchaseOrderItemDto{" +
                "itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", itemCode='" + itemCode + '\'' +
                ", quantity=" + quantity +
                ", purchaseRate=" + purchaseRate +
                ", lineTotal=" + lineTotal +
                '}';
    }
}

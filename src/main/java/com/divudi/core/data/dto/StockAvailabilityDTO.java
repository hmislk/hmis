package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for bulk retrieval of stock availability information.
 * Used to eliminate N+1 query patterns when looking up stocks for multiple items.
 * 
 * Contains all necessary information to create bill items without requiring
 * additional entity traversals or database queries.
 */
public class StockAvailabilityDTO implements Serializable {
    
    private Long itemId;
    private Long stockId;
    private Long itemBatchId;
    private String batchNo;
    private Date dateOfExpire;
    private Double availableStock;
    private Double purchaseRate;
    private Double retailRate;
    private Double costRate;
    private String itemName;
    private String itemCode;
    
    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------
    
    public StockAvailabilityDTO() {
    }
    
    /**
     * Constructor for bulk query results.
     * All parameters should come from the database query.
     */
    public StockAvailabilityDTO(Long itemId, Long stockId, Long itemBatchId, 
                               String batchNo, Date dateOfExpire, Double availableStock,
                               Double purchaseRate, Double retailRate, Double costRate, 
                               String itemName) {
        this.itemId = itemId;
        this.stockId = stockId;
        this.itemBatchId = itemBatchId;
        this.batchNo = batchNo;
        this.dateOfExpire = dateOfExpire;
        this.availableStock = availableStock != null ? availableStock : 0.0;
        this.purchaseRate = purchaseRate != null ? purchaseRate : 0.0;
        this.retailRate = retailRate != null ? retailRate : 0.0;
        this.costRate = costRate != null ? costRate : 0.0;
        this.itemName = itemName;
    }
    
    /**
     * Extended constructor including item code.
     */
    public StockAvailabilityDTO(Long itemId, Long stockId, Long itemBatchId, 
                               String batchNo, Date dateOfExpire, Double availableStock,
                               Double purchaseRate, Double retailRate, Double costRate, 
                               String itemName, String itemCode) {
        this(itemId, stockId, itemBatchId, batchNo, dateOfExpire, availableStock,
             purchaseRate, retailRate, costRate, itemName);
        this.itemCode = itemCode;
    }
    
    // ------------------------------------------------------------------
    // Getters & Setters
    // ------------------------------------------------------------------
    
    public Long getItemId() {
        return itemId;
    }
    
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    
    public Long getStockId() {
        return stockId;
    }
    
    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }
    
    public Long getItemBatchId() {
        return itemBatchId;
    }
    
    public void setItemBatchId(Long itemBatchId) {
        this.itemBatchId = itemBatchId;
    }
    
    public String getBatchNo() {
        return batchNo;
    }
    
    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }
    
    public Date getDateOfExpire() {
        return dateOfExpire;
    }
    
    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }
    
    public Double getAvailableStock() {
        return availableStock;
    }
    
    public void setAvailableStock(Double availableStock) {
        this.availableStock = availableStock;
    }
    
    public Double getPurchaseRate() {
        return purchaseRate;
    }
    
    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }
    
    public Double getRetailRate() {
        return retailRate;
    }
    
    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }
    
    public Double getCostRate() {
        return costRate;
    }
    
    public void setCostRate(Double costRate) {
        this.costRate = costRate;
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
    
    // ------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------
    
    /**
     * Check if stock is available for issuing.
     */
    public boolean isAvailable() {
        return availableStock != null && availableStock > 0.001; // Small tolerance for floating point precision
    }
    
    /**
     * Check if the item batch is expired.
     */
    public boolean isExpired() {
        return dateOfExpire != null && dateOfExpire.before(new Date());
    }
    
    /**
     * Check if the item batch is near expiry (within 3 months).
     */
    public boolean isNearExpiry() {
        if (dateOfExpire == null) {
            return false;
        }
        Date threeMonthsFromNow = new Date(System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)); // 90 days
        return dateOfExpire.before(threeMonthsFromNow);
    }
    
    @Override
    public String toString() {
        return "StockAvailabilityDTO{" +
                "itemId=" + itemId +
                ", stockId=" + stockId +
                ", itemBatchId=" + itemBatchId +
                ", batchNo='" + batchNo + '\'' +
                ", dateOfExpire=" + dateOfExpire +
                ", availableStock=" + availableStock +
                ", purchaseRate=" + purchaseRate +
                ", retailRate=" + retailRate +
                ", costRate=" + costRate +
                ", itemName='" + itemName + '\'' +
                ", itemCode='" + itemCode + '\'' +
                '}';
    }
}
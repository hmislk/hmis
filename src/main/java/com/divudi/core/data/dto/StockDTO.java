package com.divudi.core.data.dto;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;
import java.util.Date;

public class StockDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long stockId;
    private Long itemBatchId;
    private Long itemId;
    private String itemName;
    private String code;
    private String genericName;
    private String categoryName;
    private DepartmentType departmentType;
    private Double retailRate;
    private Double stockQty;
    private Date dateOfExpire;
    private String batchNo;
    private Double purchaseRate;
    private Double costRate;
    private Double wholesaleRate;
    // Temporary holder for purchase rate adjustments on the UI
    private Double newPurchaseRate;
    // Fields for retail rate adjustment
    private Double newRetailRate;
    private Double retailRateChange;
    private Double beforeRetailAdjustmentValue;
    // Fields for cost rate adjustment
    private Double newCostRate;
    private Double costRateChange;
    private Double beforeCostAdjustmentValue;
    // Field for total stock quantity across all departments (used in retail sale autocomplete)
    private Double totalStockQty;
    // Whether the underlying Item allows fractional quantities
    private Boolean allowFractions = false;
    // Whether the underlying Item allows discounts (prevents database queries during rate calculation)
    private Boolean discountAllowed = true;  // Default to true for safety
    // Category ID for price matrix discount calculations
    private Long categoryId;

    public StockDTO() {
    }

    public StockDTO(Long id, String itemName, String code, String genericName,
                     Double retailRate, Double stockQty, Date dateOfExpire) {
        this.id = id;
        this.itemName = itemName;
        this.code = code;
        this.genericName = genericName;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
    }

    // Same as above, with allowFractions
    public StockDTO(Long id, String itemName, String code, String genericName,
                    Double retailRate, Double stockQty, Date dateOfExpire,
                    Boolean allowFractions) {
        this(id, itemName, code, genericName, retailRate, stockQty, dateOfExpire);
        this.allowFractions = allowFractions;
    }

    // Constructor for pharmacy adjustment with all fields
    public StockDTO(Long id, String itemName, String code, Double retailRate, Double stockQty,
                    Date dateOfExpire, String batchNo, Double purchaseRate, Double wholesaleRate) {
        this.id = id;
        this.itemName = itemName;
        this.code = code;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
        this.batchNo = batchNo;
        this.purchaseRate = purchaseRate;
        this.wholesaleRate = wholesaleRate;
    }

    // Same as above, with allowFractions
    public StockDTO(Long id, String itemName, String code, Double retailRate, Double stockQty,
                    Date dateOfExpire, String batchNo, Double purchaseRate, Double wholesaleRate,
                    Boolean allowFractions) {
        this(id, itemName, code, retailRate, stockQty, dateOfExpire, batchNo, purchaseRate, wholesaleRate);
        this.allowFractions = allowFractions;
    }

    // Constructor for pharmacy adjustment with Stock ID and ItemBatch ID
    public StockDTO(Long id, Long stockId, Long itemBatchId, String itemName, String code,
                    Double retailRate, Double stockQty, Date dateOfExpire, String batchNo,
                    Double purchaseRate, Double wholesaleRate) {
        this.id = id;
        this.stockId = stockId;
        this.itemBatchId = itemBatchId;
        this.itemName = itemName;
        this.code = code;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
        this.batchNo = batchNo;
        this.purchaseRate = purchaseRate;
        this.wholesaleRate = wholesaleRate;
    }

    // Same as above, with allowFractions
    public StockDTO(Long id, Long stockId, Long itemBatchId, String itemName, String code,
                    Double retailRate, Double stockQty, Date dateOfExpire, String batchNo,
                    Double purchaseRate, Double wholesaleRate, Boolean allowFractions) {
        this(id, stockId, itemBatchId, itemName, code, retailRate, stockQty, dateOfExpire, batchNo, purchaseRate, wholesaleRate);
        this.allowFractions = allowFractions;
    }

    // Constructor for pharmacy adjustment with Stock ID, ItemBatch ID and Cost Rate
    public StockDTO(Long id, Long stockId, Long itemBatchId, String itemName, String code,
                    Double retailRate, Double stockQty, Date dateOfExpire, String batchNo,
                    Double purchaseRate, Double wholesaleRate, Double costRate) {
        this.id = id;
        this.stockId = stockId;
        this.itemBatchId = itemBatchId;
        this.itemName = itemName;
        this.code = code;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
        this.batchNo = batchNo;
        this.purchaseRate = purchaseRate;
        this.wholesaleRate = wholesaleRate;
        this.costRate = costRate;
    }

    // Same as above, with allowFractions
    public StockDTO(Long id, Long stockId, Long itemBatchId, String itemName, String code,
                    Double retailRate, Double stockQty, Date dateOfExpire, String batchNo,
                    Double purchaseRate, Double wholesaleRate, Double costRate,
                    Boolean allowFractions) {
        this(id, stockId, itemBatchId, itemName, code, retailRate, stockQty, dateOfExpire, batchNo, purchaseRate, wholesaleRate, costRate);
        this.allowFractions = allowFractions;
    }

    // Constructor for department stock report by batch using DTO
    public StockDTO(Long id,
                    String categoryName,
                    String itemName,
                    DepartmentType departmentType,
                    String code,
                    String genericName,
                    Date dateOfExpire,
                    String batchNo,
                    Double stockQty,
                    Double purchaseRate,
                    Double costRate,
                    Double retailRate) {
        this.id = id;
        this.stockId = id;
        this.categoryName = categoryName;
        this.itemName = itemName;
        this.departmentType = departmentType;
        this.code = code;
        this.genericName = genericName;
        this.dateOfExpire = dateOfExpire;
        this.batchNo = batchNo;
        this.stockQty = stockQty;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.retailRate = retailRate;
    }

    // Constructor for stock count bill generation (optimized for performance)
    public StockDTO(Long stockId, Long itemBatchId, Long itemId,
                    String categoryName, String itemName, String batchNo,
                    Date dateOfExpire, Double stockQty, Double costRate) {
        this.stockId = stockId;
        this.itemBatchId = itemBatchId;
        this.id = itemId;
        this.categoryName = categoryName;
        this.itemName = itemName;
        this.batchNo = batchNo;
        this.dateOfExpire = dateOfExpire;
        this.stockQty = stockQty;
        this.costRate = costRate;
    }


    // Constructor for retail sale autocomplete (includes both stock qty and total stock qty)
    public StockDTO(Long id, String itemName, String code, String genericName,
                    Double retailRate, Double stockQty, Date dateOfExpire, Double totalStockQty) {
        this.id = id;
        this.itemName = itemName;
        this.code = code;
        this.genericName = genericName;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
        this.totalStockQty = totalStockQty;
    }

    // Same as above, with allowFractions
    public StockDTO(Long id, String itemName, String code, String genericName,
                    Double retailRate, Double stockQty, Date dateOfExpire, Double totalStockQty,
                    Boolean allowFractions) {
        this(id, itemName, code, genericName, retailRate, stockQty, dateOfExpire, totalStockQty);
        this.allowFractions = allowFractions;
    }

    // Constructor for optimized retail sale autocomplete (includes itemBatchId and itemId for zero-query conversion)
    public StockDTO(Long id, Long itemBatchId, Long itemId, String itemName, String code,
                    String genericName, String batchNo, Double retailRate, Double stockQty,
                    Date dateOfExpire, Boolean discountAllowed) {
        this.id = id;
        this.itemBatchId = itemBatchId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.code = code;
        this.genericName = genericName;
        this.batchNo = batchNo;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
        this.discountAllowed = discountAllowed;
    }

    // Constructor for optimized retail sale autocomplete with category ID for price matrix discount calculation
    public StockDTO(Long id, Long itemBatchId, Long itemId, String itemName, String code,
                    String genericName, String batchNo, Double retailRate, Double stockQty,
                    Date dateOfExpire, Boolean discountAllowed, Long categoryId) {
        this.id = id;
        this.itemBatchId = itemBatchId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.code = code;
        this.genericName = genericName;
        this.batchNo = batchNo;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
        this.discountAllowed = discountAllowed;
        this.categoryId = categoryId;
    }

    // Constructor for optimized retail sale autocomplete with costRate (for cashier sale)
    public StockDTO(Long id, Long itemBatchId, Long itemId, String itemName, String code,
                    String genericName, String batchNo, Double retailRate, Double stockQty,
                    Date dateOfExpire, Boolean discountAllowed, Double costRate) {
        this.id = id;
        this.itemBatchId = itemBatchId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.code = code;
        this.genericName = genericName;
        this.batchNo = batchNo;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
        this.discountAllowed = discountAllowed;
        this.costRate = costRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }

    // Prefer boolean accessor for EL friendliness
    public boolean isAllowFractions() {
        return allowFractions != null && allowFractions;
    }

    public void setAllowFractions(Boolean allowFractions) {
        this.allowFractions = allowFractions;
    }

    public Double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(Double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public Double getNewPurchaseRate() {
        return newPurchaseRate;
    }

    public void setNewPurchaseRate(Double newPurchaseRate) {
        this.newPurchaseRate = newPurchaseRate;
    }

    public Double getNewRetailRate() {
        return newRetailRate;
    }

    public void setNewRetailRate(Double newRetailRate) {
        this.newRetailRate = newRetailRate;
    }

    public Double getRetailRateChange() {
        return retailRateChange;
    }

    public void setRetailRateChange(Double retailRateChange) {
        this.retailRateChange = retailRateChange;
    }

    public Double getBeforeRetailAdjustmentValue() {
        return beforeRetailAdjustmentValue;
    }

    public void setBeforeRetailAdjustmentValue(Double beforeRetailAdjustmentValue) {
        this.beforeRetailAdjustmentValue = beforeRetailAdjustmentValue;
    }

    public Double getNewCostRate() {
        return newCostRate;
    }

    public void setNewCostRate(Double newCostRate) {
        this.newCostRate = newCostRate;
    }

    public Double getCostRateChange() {
        return costRateChange;
    }

    public void setCostRateChange(Double costRateChange) {
        this.costRateChange = costRateChange;
    }

    public Double getBeforeCostAdjustmentValue() {
        return beforeCostAdjustmentValue;
    }

    public void setBeforeCostAdjustmentValue(Double beforeCostAdjustmentValue) {
        this.beforeCostAdjustmentValue = beforeCostAdjustmentValue;
    }

    public Double getTotalStockQty() {
        return totalStockQty;
    }

    public void setTotalStockQty(Double totalStockQty) {
        this.totalStockQty = totalStockQty;
    }

    public Boolean getDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(Boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
    }

    // Prefer boolean accessor for EL friendliness
    public boolean isDiscountAllowed() {
        return discountAllowed != null && discountAllowed;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}

package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class StockDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long stockId;
    private Long itemBatchId;
    private String itemName;
    private String code;
    private String genericName;
    private Double retailRate;
    private Double stockQty;
    private Date dateOfExpire;
    private String batchNo;
    private Double purchaseRate;
    private Double wholesaleRate;
    // Temporary holder for purchase rate adjustments on the UI
    private Double newPurchaseRate;

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
}

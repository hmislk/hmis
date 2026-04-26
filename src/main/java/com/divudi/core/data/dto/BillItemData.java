/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Carries all data needed to persist one inpatient direct-issue bill line and
 * its associated PharmaceuticalBillItem without loading any JPA entity graph.
 *
 * Fields mirror columns in billitem and pharmaceuticalbillitem tables.
 * All IDs are raw longs so no entity lookup is required at INSERT time.
 */
public class BillItemData implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- BillItem fields ----
    private Long itemId;
    private String itemName;
    private double qty;
    private double netValue;
    private double grossValue;
    private double rate;
    private double netRate;
    private String description;
    private Long catId;
    private Date createdAt;
    private Long createrId;

    // ---- PharmaceuticalBillItem fields ----
    private Long stockId;
    private Long itemBatchId;
    private double pbiQty;
    private double costRate;
    private double purchaseRate;
    private double retailRate;
    private double wholesaleRate;
    private double freeQty;
    private Date doe;
    private String stringValue;

    // ---- ItemBatch rate fields (for finance details) ----
    private double batchRetailRate;
    private double batchPurchaseRate;
    private double batchWholesaleRate;
    private Double batchCostRate;

    // ---- AMP item ID (for stock history — AMPP → AMP resolution done in controller) ----
    private Long ampItemId;
    private String ampItemName;

    // ---- Institution/Department IDs (for stock history scope queries) ----
    private Long departmentId;
    private Long institutionId;

    public BillItemData() {
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

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getNetValue() {
        return netValue;
    }

    public void setNetValue(double netValue) {
        this.netValue = netValue;
    }

    public double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(double grossValue) {
        this.grossValue = grossValue;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getNetRate() {
        return netRate;
    }

    public void setNetRate(double netRate) {
        this.netRate = netRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCatId() {
        return catId;
    }

    public void setCatId(Long catId) {
        this.catId = catId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreaterId() {
        return createrId;
    }

    public void setCreaterId(Long createrId) {
        this.createrId = createrId;
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

    public double getPbiQty() {
        return pbiQty;
    }

    public void setPbiQty(double pbiQty) {
        this.pbiQty = pbiQty;
    }

    public double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
        this.retailRate = retailRate;
    }

    public double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public double getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(double freeQty) {
        this.freeQty = freeQty;
    }

    public Date getDoe() {
        return doe;
    }

    public void setDoe(Date doe) {
        this.doe = doe;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public double getBatchRetailRate() {
        return batchRetailRate;
    }

    public void setBatchRetailRate(double batchRetailRate) {
        this.batchRetailRate = batchRetailRate;
    }

    public double getBatchPurchaseRate() {
        return batchPurchaseRate;
    }

    public void setBatchPurchaseRate(double batchPurchaseRate) {
        this.batchPurchaseRate = batchPurchaseRate;
    }

    public double getBatchWholesaleRate() {
        return batchWholesaleRate;
    }

    public void setBatchWholesaleRate(double batchWholesaleRate) {
        this.batchWholesaleRate = batchWholesaleRate;
    }

    public Double getBatchCostRate() {
        return batchCostRate;
    }

    public void setBatchCostRate(Double batchCostRate) {
        this.batchCostRate = batchCostRate;
    }

    public Long getAmpItemId() {
        return ampItemId;
    }

    public void setAmpItemId(Long ampItemId) {
        this.ampItemId = ampItemId;
    }

    public String getAmpItemName() {
        return ampItemName;
    }

    public void setAmpItemName(String ampItemName) {
        this.ampItemName = ampItemName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }
}

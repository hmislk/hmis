/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * A row of an uploaded stock-take sheet that has a counted quantity but does
 * not match any line of the snapshot. Shown on the review page so the user can
 * see it and, when the item code resolves, add it as a new batch (#24337).
 */
public class StockTakeUnmatchedRowDTO implements Serializable {

    private int rowNo; // 1-based sheet row number, as Excel shows it
    private String code;
    private String name;
    private String batchNo;
    private Date expiryDate;
    private Double quantity;
    private Double purchaseRate;
    private Double retailRate;
    private Double costRate;
    private Long itemId; // resolved Amp, null when the code is unknown
    private String itemName;
    private String reason;
    private boolean resolvable;
    private boolean addAsNewBatch;

    public StockTakeUnmatchedRowDTO() {
    }

    public int getRowNo() {
        return rowNo;
    }

    public void setRowNo(int rowNo) {
        this.rowNo = rowNo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isResolvable() {
        return resolvable;
    }

    public void setResolvable(boolean resolvable) {
        this.resolvable = resolvable;
    }

    public boolean isAddAsNewBatch() {
        return addAsNewBatch;
    }

    public void setAddAsNewBatch(boolean addAsNewBatch) {
        this.addAsNewBatch = addAsNewBatch;
    }
}

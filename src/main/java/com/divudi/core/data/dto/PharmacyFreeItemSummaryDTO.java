package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Aggregated row of the Free Items Received report (by supplier, item or date).
 */
public class PharmacyFreeItemSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private Long supplierId;
    private Long itemId;
    private Date date;
    private String itemCode;
    private long lineCount;
    private long billCount;
    private double paidQty;
    private double freeQty;
    private double freeValue;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public long getLineCount() { return lineCount; }
    public void setLineCount(long lineCount) { this.lineCount = lineCount; }
    public long getBillCount() { return billCount; }
    public void setBillCount(long billCount) { this.billCount = billCount; }
    public double getPaidQty() { return paidQty; }
    public void setPaidQty(double paidQty) { this.paidQty = paidQty; }
    public double getFreeQty() { return freeQty; }
    public void setFreeQty(double freeQty) { this.freeQty = freeQty; }
    public double getFreeValue() { return freeValue; }
    public void setFreeValue(double freeValue) { this.freeValue = freeValue; }
}

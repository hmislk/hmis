/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * UI-facing DTO for one issued item row in the native transfer receive workflow.
 * Populated by TransferReceiveNativeSqlService.loadIssuedItemsForReceive() and
 * displayed in the DataTable on pharmacy_transfer_receive_native.xhtml before settlement.
 *
 * receivingQty is user-editable (in packs); value is recalculated on qty change.
 */
public class TransferReceiveItemRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int serialNo;
    private String itemName;
    private String itemCode;
    private String batchNo;
    private Date dateOfExpire;

    /** Transfer rate for display (per unit, based on configuration). */
    private double rate;
    private double netRate;

    /** Total issued qty in units (display only). */
    private double issuedQty;

    /** Remaining qty in units after subtracting all prior receives. Used by over-receive guard. */
    private double remainingUnits;

    /** User-editable quantity in packs. */
    private BigDecimal receivingQty;

    /** Absolute net value for display; updated on qty change. */
    private double value;

    /** Reference to the issued BillItem ID (used as referanceBillItem_ID on receive BillItem). */
    private Long issuedBillItemId;

    /** stock_ID from the issued item's PharmaceuticalBillItem (staff stock to deduct from). */
    private Long staffStockId;

    private Long itemBatchId;
    private Long itemId;

    /** Resolved AMP item ID for StockHistory (same as itemId unless the item is Ampp). */
    private Long ampItemId;

    private double unitsPerPack;
    private double purchaseRate;
    private double retailRate;
    private double wholesaleRate;
    private double costRate;

    /** Rates sourced directly from ItemBatch columns (for finance details). */
    private double batchRetailRate;
    private double batchPurchaseRate;
    private double batchWholesaleRate;
    private Double batchCostRate;

    /** Configured transfer rate per pack (grossRate = unit rate * unitsPerPack). */
    private BigDecimal grossRate;

    public TransferReceiveItemRowDto() {
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
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

    public double getIssuedQty() {
        return issuedQty;
    }

    public void setIssuedQty(double issuedQty) {
        this.issuedQty = issuedQty;
    }

    public double getRemainingUnits() {
        return remainingUnits;
    }

    public void setRemainingUnits(double remainingUnits) {
        this.remainingUnits = remainingUnits;
    }

    public BigDecimal getReceivingQty() {
        return receivingQty;
    }

    public void setReceivingQty(BigDecimal receivingQty) {
        this.receivingQty = receivingQty;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Long getIssuedBillItemId() {
        return issuedBillItemId;
    }

    public void setIssuedBillItemId(Long issuedBillItemId) {
        this.issuedBillItemId = issuedBillItemId;
    }

    public Long getStaffStockId() {
        return staffStockId;
    }

    public void setStaffStockId(Long staffStockId) {
        this.staffStockId = staffStockId;
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

    public Long getAmpItemId() {
        return ampItemId;
    }

    public void setAmpItemId(Long ampItemId) {
        this.ampItemId = ampItemId;
    }

    public double getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(double unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
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

    public double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
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

    public BigDecimal getGrossRate() {
        return grossRate;
    }

    public void setGrossRate(BigDecimal grossRate) {
        this.grossRate = grossRate;
    }
}

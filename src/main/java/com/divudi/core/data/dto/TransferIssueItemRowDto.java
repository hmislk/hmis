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
 * UI-facing DTO for one requested item row in the native transfer issue workflow.
 * Populated by TransferIssueNativeSqlService.loadRequestedItemsForIssue() and
 * displayed in the DataTable on pharmacy_transfer_issue_native.xhtml before settlement.
 *
 * issuingQty is user-editable (in packs); lineTotal is recalculated on qty change.
 *
 * Related issue: #20583
 */
public class TransferIssueItemRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int serialNo;
    private String itemName;
    private String itemCode;
    private String batchNo;
    private Date dateOfExpire;

    /** ID of the original request BillItem (referanceBillItem_ID on the issued BillItem). */
    private Long requestedBillItemId;

    private Long itemId;

    /** AMP item ID (same as itemId unless item is Ampp). Used for StockHistory. */
    private Long ampItemId;

    private String itemDtype;

    /** Pack size: >1 for Ampp (units per pack), 1.0 for Amp/default. */
    private double unitsPerPack = 1.0;

    /** ID of the dept Stock row to deduct from at settle time. Null if no stock available. */
    private Long deptStockId;

    private Long itemBatchId;

    /** Available stock units in the issuing department for this batch. */
    private double availableStock;

    /** Originally requested quantity in packs. */
    private double requestedQty;

    /** Quantity already issued in packs (from prior issue bills for this request item). */
    private double alreadyIssuedQty;

    /** Remaining quantity to issue in packs (requestedQty - alreadyIssuedQty). */
    private double remainingQty;

    /** User-editable issuing quantity in packs. Defaults to remainingQty at load time. */
    private BigDecimal issuingQty;

    /** Transfer rate per pack (grossRate). Determined by config: purchase/cost/retail. */
    private BigDecimal grossRate;

    /** Line total = grossRate * issuingQty. Recalculated on qty change. */
    private double lineTotal;

    private double purchaseRate;
    private double retailRate;
    private double wholesaleRate;
    private double costRate;

    /** Rates sourced directly from ItemBatch columns (for StockHistory finance aggregates). */
    private double batchRetailRate;
    private double batchPurchaseRate;
    private double batchWholesaleRate;
    private Double batchCostRate;

    public TransferIssueItemRowDto() {
    }

    public int getSerialNo() { return serialNo; }
    public void setSerialNo(int serialNo) { this.serialNo = serialNo; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Date getDateOfExpire() { return dateOfExpire; }
    public void setDateOfExpire(Date dateOfExpire) { this.dateOfExpire = dateOfExpire; }

    public Long getRequestedBillItemId() { return requestedBillItemId; }
    public void setRequestedBillItemId(Long requestedBillItemId) { this.requestedBillItemId = requestedBillItemId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getAmpItemId() { return ampItemId; }
    public void setAmpItemId(Long ampItemId) { this.ampItemId = ampItemId; }

    public String getItemDtype() { return itemDtype; }
    public void setItemDtype(String itemDtype) { this.itemDtype = itemDtype; }

    public double getUnitsPerPack() { return unitsPerPack; }
    public void setUnitsPerPack(double unitsPerPack) { this.unitsPerPack = unitsPerPack; }

    public Long getDeptStockId() { return deptStockId; }
    public void setDeptStockId(Long deptStockId) { this.deptStockId = deptStockId; }

    public Long getItemBatchId() { return itemBatchId; }
    public void setItemBatchId(Long itemBatchId) { this.itemBatchId = itemBatchId; }

    public double getAvailableStock() { return availableStock; }
    public void setAvailableStock(double availableStock) { this.availableStock = availableStock; }

    public double getRequestedQty() { return requestedQty; }
    public void setRequestedQty(double requestedQty) { this.requestedQty = requestedQty; }

    public double getAlreadyIssuedQty() { return alreadyIssuedQty; }
    public void setAlreadyIssuedQty(double alreadyIssuedQty) { this.alreadyIssuedQty = alreadyIssuedQty; }

    public double getRemainingQty() { return remainingQty; }
    public void setRemainingQty(double remainingQty) { this.remainingQty = remainingQty; }

    public BigDecimal getIssuingQty() { return issuingQty; }
    public void setIssuingQty(BigDecimal issuingQty) { this.issuingQty = issuingQty; }

    public BigDecimal getGrossRate() { return grossRate; }
    public void setGrossRate(BigDecimal grossRate) { this.grossRate = grossRate; }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }

    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }

    public double getRetailRate() { return retailRate; }
    public void setRetailRate(double retailRate) { this.retailRate = retailRate; }

    public double getWholesaleRate() { return wholesaleRate; }
    public void setWholesaleRate(double wholesaleRate) { this.wholesaleRate = wholesaleRate; }

    public double getCostRate() { return costRate; }
    public void setCostRate(double costRate) { this.costRate = costRate; }

    public double getBatchRetailRate() { return batchRetailRate; }
    public void setBatchRetailRate(double batchRetailRate) { this.batchRetailRate = batchRetailRate; }

    public double getBatchPurchaseRate() { return batchPurchaseRate; }
    public void setBatchPurchaseRate(double batchPurchaseRate) { this.batchPurchaseRate = batchPurchaseRate; }

    public double getBatchWholesaleRate() { return batchWholesaleRate; }
    public void setBatchWholesaleRate(double batchWholesaleRate) { this.batchWholesaleRate = batchWholesaleRate; }

    public Double getBatchCostRate() { return batchCostRate; }
    public void setBatchCostRate(Double batchCostRate) { this.batchCostRate = batchCostRate; }
}

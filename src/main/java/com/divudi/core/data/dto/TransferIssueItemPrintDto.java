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
 * Print DTO for one item line in a native transfer issue note.
 * Populated by TransferIssueNativeSqlService.settle() and
 * rendered by the DTO-based composite print components.
 *
 * Related issue: #20583
 */
public class TransferIssueItemPrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int serialNo;
    private String itemName;
    private String itemCode;
    private String batchNo;
    private Date dateOfExpire;

    /** Issued quantity in packs (positive). */
    private double qty;

    /** Issued quantity in units (qty * unitsPerPack). */
    private double qtyInUnits;

    /** Originally requested quantity in packs (for reference column). */
    private double requestedQty;

    private double rate;
    private double netRate;

    /** Net value for this line (positive — issue = revenue). */
    private double netValue;

    private double purchaseRate;
    private double retailRate;
    private double costRate;

    private BigDecimal valueAtPurchaseRate;
    private BigDecimal valueAtRetailRate;
    private BigDecimal valueAtCostRate;

    private BigDecimal lineGrossTotal;

    public TransferIssueItemPrintDto() {
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

    public double getQty() { return qty; }
    public void setQty(double qty) { this.qty = qty; }

    public double getQtyInUnits() { return qtyInUnits; }
    public void setQtyInUnits(double qtyInUnits) { this.qtyInUnits = qtyInUnits; }

    public double getRequestedQty() { return requestedQty; }
    public void setRequestedQty(double requestedQty) { this.requestedQty = requestedQty; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public double getNetRate() { return netRate; }
    public void setNetRate(double netRate) { this.netRate = netRate; }

    public double getNetValue() { return netValue; }
    public void setNetValue(double netValue) { this.netValue = netValue; }

    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }

    public double getRetailRate() { return retailRate; }
    public void setRetailRate(double retailRate) { this.retailRate = retailRate; }

    public double getCostRate() { return costRate; }
    public void setCostRate(double costRate) { this.costRate = costRate; }

    public BigDecimal getValueAtPurchaseRate() { return valueAtPurchaseRate; }
    public void setValueAtPurchaseRate(BigDecimal valueAtPurchaseRate) { this.valueAtPurchaseRate = valueAtPurchaseRate; }

    public BigDecimal getValueAtRetailRate() { return valueAtRetailRate; }
    public void setValueAtRetailRate(BigDecimal valueAtRetailRate) { this.valueAtRetailRate = valueAtRetailRate; }

    public BigDecimal getValueAtCostRate() { return valueAtCostRate; }
    public void setValueAtCostRate(BigDecimal valueAtCostRate) { this.valueAtCostRate = valueAtCostRate; }

    public BigDecimal getLineGrossTotal() { return lineGrossTotal; }
    public void setLineGrossTotal(BigDecimal lineGrossTotal) { this.lineGrossTotal = lineGrossTotal; }
}

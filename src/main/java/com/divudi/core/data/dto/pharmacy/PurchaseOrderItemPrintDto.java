/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

/**
 * One line item on a Purchase Order print.
 * Populated by PurchaseOrderNativeSqlService.loadPrintDtoByBillId().
 * Related issue: #20923
 */
public class PurchaseOrderItemPrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemName;
    private String itemCode;
    private String issueUnitName;
    private double purchaseRate;
    private double quantity;
    private double freeQuantity;
    private double lineTotal;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getIssueUnitName() { return issueUnitName; }
    public void setIssueUnitName(String issueUnitName) { this.issueUnitName = issueUnitName; }

    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getFreeQuantity() { return freeQuantity; }
    public void setFreeQuantity(double freeQuantity) { this.freeQuantity = freeQuantity; }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}

/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;
import java.util.Date;

/**
 * Flat DTO for a single GRN bill item line — primitives, String, and Date only.
 * Populated by PharmacyGrnNativeSqlService from the item native SQL query.
 * Covers item identity, BillItemFinanceDetails, and PharmaceuticalBillItem fields.
 */
public class GrnPrintItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemName;
    private String itemCode;
    private String issueUnitName;

    // BillItemFinanceDetails
    private double quantity;
    private double freeQuantity;
    private double lineGrossRate;
    private double lineDiscountRate;
    private double lineNetTotal;
    private double retailSaleRate;
    private double retailSaleRatePerUnit;
    private double totalCost;
    private double totalQuantityByUnits;

    // PharmaceuticalBillItem
    private Date   doe;
    private String batchNumber;

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getIssueUnitName() { return issueUnitName; }
    public void setIssueUnitName(String issueUnitName) { this.issueUnitName = issueUnitName; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getFreeQuantity() { return freeQuantity; }
    public void setFreeQuantity(double freeQuantity) { this.freeQuantity = freeQuantity; }

    public double getLineGrossRate() { return lineGrossRate; }
    public void setLineGrossRate(double lineGrossRate) { this.lineGrossRate = lineGrossRate; }

    public double getLineDiscountRate() { return lineDiscountRate; }
    public void setLineDiscountRate(double lineDiscountRate) { this.lineDiscountRate = lineDiscountRate; }

    public double getLineNetTotal() { return lineNetTotal; }
    public void setLineNetTotal(double lineNetTotal) { this.lineNetTotal = lineNetTotal; }

    public double getRetailSaleRate() { return retailSaleRate; }
    public void setRetailSaleRate(double retailSaleRate) { this.retailSaleRate = retailSaleRate; }

    public double getRetailSaleRatePerUnit() { return retailSaleRatePerUnit; }
    public void setRetailSaleRatePerUnit(double retailSaleRatePerUnit) { this.retailSaleRatePerUnit = retailSaleRatePerUnit; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getTotalQuantityByUnits() { return totalQuantityByUnits; }
    public void setTotalQuantityByUnits(double totalQuantityByUnits) { this.totalQuantityByUnits = totalQuantityByUnits; }

    public Date getDoe() { return doe; }
    public void setDoe(Date doe) { this.doe = doe; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
}

package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class BillItemFinanceDetailsDTO implements Serializable {

    private Long id;
    private Date createdAt;
    private Double quantity;
    private Double quantityByUnits;
    private Double lineNetRate;
    private Double grossRate;
    private Double lineGrossRate;
    private Double costRate;
    private Double purchaseRate;
    private Double retailSaleRate;
    private Double lineCostRate;
    private Double billCostRate;
    private Double totalCostRate;
    private Double lineGrossTotal;
    private Double grossTotal;
    private Double lineCost;
    private Double billCost;
    private Double totalCost;
    private Double valueAtCostRate;
    private Double valueAtPurchaseRate;
    private Double valueAtRetailRate;

    public BillItemFinanceDetailsDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getQuantityByUnits() {
        return quantityByUnits;
    }

    public void setQuantityByUnits(Double quantityByUnits) {
        this.quantityByUnits = quantityByUnits;
    }

    public Double getLineNetRate() {
        return lineNetRate;
    }

    public void setLineNetRate(Double lineNetRate) {
        this.lineNetRate = lineNetRate;
    }

    public Double getGrossRate() {
        return grossRate;
    }

    public void setGrossRate(Double grossRate) {
        this.grossRate = grossRate;
    }

    public Double getLineGrossRate() {
        return lineGrossRate;
    }

    public void setLineGrossRate(Double lineGrossRate) {
        this.lineGrossRate = lineGrossRate;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getRetailSaleRate() {
        return retailSaleRate;
    }

    public void setRetailSaleRate(Double retailSaleRate) {
        this.retailSaleRate = retailSaleRate;
    }

    public Double getLineCostRate() {
        return lineCostRate;
    }

    public void setLineCostRate(Double lineCostRate) {
        this.lineCostRate = lineCostRate;
    }

    public Double getBillCostRate() {
        return billCostRate;
    }

    public void setBillCostRate(Double billCostRate) {
        this.billCostRate = billCostRate;
    }

    public Double getTotalCostRate() {
        return totalCostRate;
    }

    public void setTotalCostRate(Double totalCostRate) {
        this.totalCostRate = totalCostRate;
    }

    public Double getLineGrossTotal() {
        return lineGrossTotal;
    }

    public void setLineGrossTotal(Double lineGrossTotal) {
        this.lineGrossTotal = lineGrossTotal;
    }

    public Double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public Double getLineCost() {
        return lineCost;
    }

    public void setLineCost(Double lineCost) {
        this.lineCost = lineCost;
    }

    public Double getBillCost() {
        return billCost;
    }

    public void setBillCost(Double billCost) {
        this.billCost = billCost;
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }

    public Double getValueAtCostRate() {
        return valueAtCostRate;
    }

    public void setValueAtCostRate(Double valueAtCostRate) {
        this.valueAtCostRate = valueAtCostRate;
    }

    public Double getValueAtPurchaseRate() {
        return valueAtPurchaseRate;
    }

    public void setValueAtPurchaseRate(Double valueAtPurchaseRate) {
        this.valueAtPurchaseRate = valueAtPurchaseRate;
    }

    public Double getValueAtRetailRate() {
        return valueAtRetailRate;
    }

    public void setValueAtRetailRate(Double valueAtRetailRate) {
        this.valueAtRetailRate = valueAtRetailRate;
    }
}

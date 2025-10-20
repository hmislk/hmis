/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data;

import java.math.BigDecimal;

/**
 *
 * @author pubudupiyankara
 */
public class StockCorrectionRow {

    private String itemName;
    private double qty;
    private BigDecimal quantity;
    private double oldRate;
    private double oldValue;
    private double newRate;
    private double newValue;
    private double variance;

    private double purchaseRate;
    private double costRate;
    private double retailRate;
    private double beforeAdjustment;
    private double afterAdjustment;

    private BigDecimal preciseOldValue;
    private BigDecimal preciseNewValue;

    public BigDecimal getPreciseOldValue() {
        return preciseOldValue;
    }

    public void setPreciseOldValue(BigDecimal preciseOldValue) {
        this.preciseOldValue = preciseOldValue;
    }

    public BigDecimal getPreciseNewValue() {
        return preciseNewValue;
    }

    public void setPreciseNewValue(BigDecimal preciseNewValue) {
        this.preciseNewValue = preciseNewValue;
    }

    public String getFormattedOldValue() {
        if (preciseOldValue != null) {
            return String.format("%,.2f", preciseOldValue.doubleValue());
        }
        return "0.00";
    }

    public String getFormattedNewValue() {
        if (preciseNewValue != null) {
            return String.format("%,.2f", preciseNewValue.doubleValue());
        }
        return "0.00";
    }

    public StockCorrectionRow() {

    }

    public StockCorrectionRow(String itemName, BigDecimal quantity, double purchaseRate,
            double costRate, double retailRate,
            double beforeAdjustment, double afterAdjustment) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.retailRate = retailRate;
        this.beforeAdjustment = beforeAdjustment;
        this.afterAdjustment = afterAdjustment;
    }

    // Getters and setters
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

    public double getOldRate() {
        return oldRate;
    }

    public void setOldRate(double oldRate) {
        this.oldRate = oldRate;
    }

    public double getOldValue() {
        return oldValue;
    }

    public void setOldValue(double oldValue) {
        this.oldValue = oldValue;
    }

    public double getNewRate() {
        return newRate;
    }

    public void setNewRate(double newRate) {
        this.newRate = newRate;
    }

    public double getNewValue() {
        return newValue;
    }

    public void setNewValue(double newValue) {
        this.newValue = newValue;
    }

    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
    }

    public double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
        this.retailRate = retailRate;
    }

    public double getBeforeAdjustment() {
        return beforeAdjustment;
    }

    public void setBeforeAdjustment(double beforeAdjustment) {
        this.beforeAdjustment = beforeAdjustment;
    }

    public double getAfterAdjustment() {
        return afterAdjustment;
    }

    public void setAfterAdjustment(double afterAdjustment) {
        this.afterAdjustment = afterAdjustment;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data;

/**
 *
 * @author pubudupiyankara
 */
public class StockCorrectionRow {

    private String itemName;
    private double qty;
    private double oldRate;
    private double oldValue;
    private double newRate;
    private double newValue;
    private double variance;

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

}

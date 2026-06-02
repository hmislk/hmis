package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Lightweight DTO holding the three rates needed for transfer request item
 * entry: purchase, retail, and cost. Populated from a single BillItem query,
 * replacing three separate entity-loading rate calls.
 */
public class ItemRatesDTO implements Serializable {

    private double purchaseRate;
    private double retailRate;
    private double costRate;

    public ItemRatesDTO() {
    }

    public ItemRatesDTO(double purchaseRate, double retailRate, double costRate) {
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.costRate = costRate;
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

    public double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
    }
}

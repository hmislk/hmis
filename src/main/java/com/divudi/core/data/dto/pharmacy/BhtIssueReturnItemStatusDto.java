package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

/**
 * Per-item quantity reconciliation row shown on the BHT Issue Return "Running
 * Update Status" tab (issue #23338). Plain view-model POJO - not a JPQL
 * constructor DTO, it is never used in a query.
 */
public class BhtIssueReturnItemStatusDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemName;
    private double saleQty;
    private double previouslyReturnedQty;
    private double thisTimeReturnedQty;
    private double totalReturnedQty;
    private double balanceQty;

    public BhtIssueReturnItemStatusDto() {
    }

    public BhtIssueReturnItemStatusDto(String itemName, double saleQty, double previouslyReturnedQty, double thisTimeReturnedQty, double totalReturnedQty, double balanceQty) {
        this.itemName = itemName;
        this.saleQty = saleQty;
        this.previouslyReturnedQty = previouslyReturnedQty;
        this.thisTimeReturnedQty = thisTimeReturnedQty;
        this.totalReturnedQty = totalReturnedQty;
        this.balanceQty = balanceQty;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getSaleQty() {
        return saleQty;
    }

    public void setSaleQty(double saleQty) {
        this.saleQty = saleQty;
    }

    public double getPreviouslyReturnedQty() {
        return previouslyReturnedQty;
    }

    public void setPreviouslyReturnedQty(double previouslyReturnedQty) {
        this.previouslyReturnedQty = previouslyReturnedQty;
    }

    public double getThisTimeReturnedQty() {
        return thisTimeReturnedQty;
    }

    public void setThisTimeReturnedQty(double thisTimeReturnedQty) {
        this.thisTimeReturnedQty = thisTimeReturnedQty;
    }

    public double getTotalReturnedQty() {
        return totalReturnedQty;
    }

    public void setTotalReturnedQty(double totalReturnedQty) {
        this.totalReturnedQty = totalReturnedQty;
    }

    public double getBalanceQty() {
        return balanceQty;
    }

    public void setBalanceQty(double balanceQty) {
        this.balanceQty = balanceQty;
    }
}

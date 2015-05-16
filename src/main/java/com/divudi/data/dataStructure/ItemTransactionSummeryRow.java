/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Item;

/**
 *
 * @author buddhika
 */
public class ItemTransactionSummeryRow {
    Item item;
    Double retailSale;
    Double wholeSale;
    Double bhtSale;
    Double transferIssue;
    Double totalOut;
    Double transferIn;
    Double purchase;
    Double totalIn;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getRetailSale() {
        return retailSale;
    }

    public void setRetailSale(Double retailSale) {
        this.retailSale = retailSale;
    }

    public Double getWholeSale() {
        return wholeSale;
    }

    public void setWholeSale(Double wholeSale) {
        this.wholeSale = wholeSale;
    }

    public Double getBhtSale() {
        return bhtSale;
    }

    public void setBhtSale(Double bhtSale) {
        this.bhtSale = bhtSale;
    }

    public Double getTransferIssue() {
        return transferIssue;
    }

    public void setTransferIssue(Double transferIssue) {
        this.transferIssue = transferIssue;
    }

    public Double getTotalOut() {
        return totalOut;
    }

    public void setTotalOut(Double totalOut) {
        this.totalOut = totalOut;
    }

    public Double getTransferIn() {
        return transferIn;
    }

    public void setTransferIn(Double transferIn) {
        this.transferIn = transferIn;
    }

    public Double getPurchase() {
        return purchase;
    }

    public void setPurchase(Double purchase) {
        this.purchase = purchase;
    }

    public Double getTotalIn() {
        return totalIn;
    }

    public void setTotalIn(Double totalIn) {
        this.totalIn = totalIn;
    }
    
}

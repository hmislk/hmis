/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;

/**
 *
 * @author Buddhika
 */
public class StockReportRecord {

    Item item;
    Double qty;
    Double purchaseValue;
    Double retailsaleValue;
    Double wholeSaleValue;
    Double stockQty;
    Double stockOnHand;
    Institution institution;
    Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
    
    

    public StockReportRecord(Item item, Double qty, Double purchaseValue, Double retailsaleValue, Double wholeSaleValue) {
        this.item = item;
        this.qty = qty;
        this.purchaseValue = purchaseValue;
        this.retailsaleValue = retailsaleValue;
        this.wholeSaleValue = wholeSaleValue;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getRetailsaleValue() {
        return retailsaleValue;
    }

    public void setRetailsaleValue(Double retailsaleValue) {
        this.retailsaleValue = retailsaleValue;
    }

    public StockReportRecord() {
    }

    public Double getWholeSaleValue() {
        return wholeSaleValue;
    }

    public StockReportRecord(Item item, Double qty, Double retailsaleValue) {
        this.item = item;
        this.qty = qty;
        this.retailsaleValue = retailsaleValue;
    }

    public void setWholeSaleValue(Double wholeSaleValue) {
        this.wholeSaleValue = wholeSaleValue;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Double getStockOnHand() {
        return stockOnHand;
    }

    public void setStockOnHand(Double stockOnHand) {
        this.stockOnHand = stockOnHand;
    }
    
    

}

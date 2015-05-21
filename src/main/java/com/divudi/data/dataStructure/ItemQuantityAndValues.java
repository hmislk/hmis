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
public class ItemQuantityAndValues {
    Item item;
    Double quantity;
    Double value;
    Double retailValue;
    Double wholeSaleValue;
    Double purchaseValue;

    public ItemQuantityAndValues() {
    }

    public ItemQuantityAndValues(Item item, Double quantity, Double retailValue, Double wholeSaleValue, Double purchaseValue) {
        this.item = item;
        this.quantity = quantity;
        this.retailValue = retailValue;
        this.wholeSaleValue = wholeSaleValue;
        this.purchaseValue = purchaseValue;
    }

    public ItemQuantityAndValues(Item item, Double quantity, Double value) {
        this.item = item;
        this.quantity = quantity;
        this.value = value;
    }

    public ItemQuantityAndValues(Item item, double quantity, double value) {
        this.item = item;
        this.quantity = quantity;
        this.value = value;
    }

    
    
    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
    
    
    
    
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(Double retailValue) {
        this.retailValue = retailValue;
    }

    public Double getWholeSaleValue() {
        return wholeSaleValue;
    }

    public void setWholeSaleValue(Double wholeSaleValue) {
        this.wholeSaleValue = wholeSaleValue;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }
    
}

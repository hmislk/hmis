/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Item;
import java.util.Date;

/**
 *
 * @author Buddhika
 */
public class ItemTransactionSummeryRowReorder {
    Item item;
    Double quantity;
    Double value;
    Date date;

    public ItemTransactionSummeryRowReorder() {
    }

    public ItemTransactionSummeryRowReorder(Item item, double quantity, Date date) {
        this.item = item;
        this.quantity = quantity;
        this.date = date;
    }
    
    public ItemTransactionSummeryRowReorder(Item item, Double quantity, Date date) {
        this.item = item;
        this.quantity = quantity;
        this.date = date;
    }
    
    public ItemTransactionSummeryRowReorder(Item item, double quantity, Double value, Date date) {
        this.item = item;
        this.quantity = quantity;
        this.value = value;
        this.date = date;
    }

    public ItemTransactionSummeryRowReorder(Item item, Double quantity, double value, Date date) {
        this.item = item;
        this.quantity = quantity;
        this.value = value;
        this.date = date;
    }

    
    public ItemTransactionSummeryRowReorder(Item item, double quantity, double value, Date date) {
        this.item = item;
        this.quantity = quantity;
        this.value = value;
        this.date = date;
    }

    
    public ItemTransactionSummeryRowReorder(Item item, Double quantity, Double value, Date date) {
        this.item = item;
        this.quantity = quantity;
        this.value = value;
        this.date = date;
    }

    
    
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
    
}

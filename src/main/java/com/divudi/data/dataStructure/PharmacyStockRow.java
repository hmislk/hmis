/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Item;

/**
 *
 * @author pdhs
 */
public class PharmacyStockRow {

    String code;
    String name;
    Double qty;
    Double freeQty;
    Double purchaseValue;
    Double saleValue;
    Item item;

    public PharmacyStockRow() {
    }

    public PharmacyStockRow(String name, Double qty, Double freeQty) {
        this.name = name;
        this.qty = qty;
        this.freeQty = freeQty;
    }

    
    
    public PharmacyStockRow(Item item, Double qty, Double purchaseValue, Double saleValue) {
        this.qty = qty;
        this.purchaseValue = purchaseValue;
        this.saleValue = saleValue;
        this.item = item;
    }

    public PharmacyStockRow(String code, String name, Double qty, Double purchaseValue, Double saleValue) {
        this.code = code;
        this.name = name;
        this.qty = qty;
        this.purchaseValue = purchaseValue;
        this.saleValue = saleValue;
        String sql;
        sql = "select new com.divudi.data.dataStructure.PharmacyStockRow"
                + "(s.itemBatch.item.code, "
                + "s.itemBatch.item.name, "
                + "sum(s.stock), "
                + "sum(s.itemBatch.purcahseRate * s.stock), "
                + "sum(s.itemBatch.retailsaleRate * s.stock))  "
                + "from Stock s where s.stock>:z and s.department=:d "
                + "group by s.itemBatch.item.name, s.itemBatch.item.code "
                + "order by s.itemBatch.item.name";
    }

    public Double getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(Double freeQty) {
        this.freeQty = freeQty;
    }

    
    
    
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(Double saleValue) {
        this.saleValue = saleValue;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

}

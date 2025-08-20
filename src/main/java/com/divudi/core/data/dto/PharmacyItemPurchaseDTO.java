package com.divudi.core.data.dto;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Item;
import java.io.Serializable;

public class PharmacyItemPurchaseDTO implements Serializable {

    private Bill bill;
    private Item item;
    private Double qty;
    private Double freeQty;

    public PharmacyItemPurchaseDTO(Bill bill, Item item, Double qty, Double freeQty) {
        this.bill = bill;
        this.item = item;
        this.qty = qty;
        this.freeQty = freeQty;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
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

    public Double getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(Double freeQty) {
        this.freeQty = freeQty;
    }
}

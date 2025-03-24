package com.divudi.data;

import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.StockHistory;

/**
 *
 * @author Dr M H B Ariyaratne
 * 
 */
public class PharmacyRow {
    private StockHistory stockHistory;
    private Long id;
    private Item item;
    private ItemBatch itemBatch;
    private Double quantity;
    private Double purchaseValue;
    private Double saleValue;

    public PharmacyRow() {
    }

    public PharmacyRow(StockHistory stockHistory) {
        this.stockHistory = stockHistory;
    }

    public PharmacyRow(Long id) {
        this.id = id;
    }
    
    public StockHistory getStockHistory() {
        return stockHistory;
    }

    public void setStockHistory(StockHistory stockHistory) {
        this.stockHistory = stockHistory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemBatch getItemBatch() {
        return itemBatch;
    }

    public void setItemBatch(ItemBatch itemBatch) {
        this.itemBatch = itemBatch;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
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
    
    
    
}

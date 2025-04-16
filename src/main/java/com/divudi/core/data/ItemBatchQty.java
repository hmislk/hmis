/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data;

import com.divudi.core.entity.pharmacy.ItemBatch;

/**
 *
 * @author Buddhika
 */
public class ItemBatchQty {

    ItemBatch itemBatch;
    double qty;

    public ItemBatch getItemBatch() {
        return itemBatch;
    }

    public void setItemBatch(ItemBatch itemBatch) {
        this.itemBatch = itemBatch;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public ItemBatchQty(ItemBatch itemBatch, double qty) {
        this.itemBatch = itemBatch;
        this.qty = qty;
    }



}

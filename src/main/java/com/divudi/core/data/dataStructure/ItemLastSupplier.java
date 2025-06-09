package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;

/**
 *
 * @author Dr M H B Ariyaratne buddhika.ari@gmail.com
 *
 */
public class ItemLastSupplier {

    private Item item;
    private Institution lastSupplier;

    public ItemLastSupplier() {
    }

    public ItemLastSupplier(Item item, Institution lastSupplier) {
        this.item = item;
        this.lastSupplier = lastSupplier;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Institution getLastSupplier() {
        return lastSupplier;
    }

    public void setLastSupplier(Institution lastSupplier) {
        this.lastSupplier = lastSupplier;
    }

}

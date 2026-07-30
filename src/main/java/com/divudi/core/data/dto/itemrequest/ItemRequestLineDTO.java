/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.itemrequest;

import java.io.Serializable;

/**
 * A single requested item line (item + quantity) submitted as part of an
 * {@link ItemRequestCreateRequestDTO}.
 *
 * @author Claude AI Assistant
 */
public class ItemRequestLineDTO implements Serializable {

    private Long itemId;
    private double qty;

    public ItemRequestLineDTO() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }
}

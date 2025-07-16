package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;

public class ItemMovementSummaryDTO implements Serializable {
    private BillTypeAtomic billTypeAtomic;
    private String itemName;
    private Double quantity;
    private Double netValue;

    public ItemMovementSummaryDTO() {
    }

    public ItemMovementSummaryDTO(BillTypeAtomic billTypeAtomic, String itemName, Double quantity, Double netValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.itemName = itemName;
        this.quantity = quantity;
        this.netValue = netValue;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }
}

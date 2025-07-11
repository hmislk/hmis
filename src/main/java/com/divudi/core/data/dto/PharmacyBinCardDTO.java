package com.divudi.core.data.dto;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight projection used for the pharmacy bin card to avoid loading full
 * entity graphs.
 */
public class PharmacyBinCardDTO implements Serializable {
    private Long id;
    private Date createdAt;
    private BillType billType;
    private BillTypeAtomic billTypeAtomic;
    private String itemName;
    private String itemClass;
    private double qty;
    private double freeQty;
    private double qtyPacks;
    private double freeQtyPacks;
    private double itemDblValue;
    private double itemStock;

    public PharmacyBinCardDTO(Long id,
                              Date createdAt,
                              BillType billType,
                              BillTypeAtomic billTypeAtomic,
                              String itemName,
                              Class<?> itemClass,
                              double qty,
                              double freeQty,
                              double qtyPacks,
                              double freeQtyPacks,
                              double itemDblValue,
                              double itemStock) {
        this.id = id;
        this.createdAt = createdAt;
        this.billType = billType;
        this.billTypeAtomic = billTypeAtomic;
        this.itemName = itemName;
        this.itemClass = itemClass == null ? null : itemClass.getSimpleName();
        this.qty = qty;
        this.freeQty = freeQty;
        this.qtyPacks = qtyPacks;
        this.freeQtyPacks = freeQtyPacks;
        this.itemDblValue = itemDblValue;
        this.itemStock = itemStock;
    }

    public PharmacyBinCardDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
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

    public String getItemClass() {
        return itemClass;
    }

    public void setItemClass(String itemClass) {
        this.itemClass = itemClass;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(double freeQty) {
        this.freeQty = freeQty;
    }

    public double getQtyPacks() {
        return qtyPacks;
    }

    public void setQtyPacks(double qtyPacks) {
        this.qtyPacks = qtyPacks;
    }

    public double getFreeQtyPacks() {
        return freeQtyPacks;
    }

    public void setFreeQtyPacks(double freeQtyPacks) {
        this.freeQtyPacks = freeQtyPacks;
    }

    public double getItemDblValue() {
        return itemDblValue;
    }

    public void setItemDblValue(double itemDblValue) {
        this.itemDblValue = itemDblValue;
    }

    public double getItemStock() {
        return itemStock;
    }

    public void setItemStock(double itemStock) {
        this.itemStock = itemStock;
    }

    // Derived helper properties
    public double getTransQtyPlusFreeQty() {
        return qty + freeQty;
    }

    public double getTransAbsoluteQtyPlusFreeQty() {
        return Math.abs(getTransQtyPlusFreeQty());
    }

    public boolean isTransThisIsStockIn() {
        return getTransQtyPlusFreeQty() > 0;
    }

    public boolean isTransThisIsStockOut() {
        return getTransQtyPlusFreeQty() < 0;
    }

    public boolean isAmpp() {
        return "Ampp".equals(itemClass);
    }
}

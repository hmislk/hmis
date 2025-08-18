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
    private Class<?> itemClass;
    private Double qty;
    private Double freeQty;
    private Double qtyPacks;
    private Double freeQtyPacks;
    private Double itemDblValue;
    private Double itemStock;
    private Double stockQty;
    private String batchNo;

    public PharmacyBinCardDTO(Long id,
            Date createdAt,
            BillType billType,
            BillTypeAtomic billTypeAtomic,
            String itemName,
            Double qty,
            Double freeQty,
            Double qtyPacks,
            Double freeQtyPacks,
            Double itemDblValue,
            Double itemStock,
            Double stockQty,
            String batchNo) {
        this.id = id;
        this.createdAt = createdAt;
        this.billType = billType;
        this.billTypeAtomic = billTypeAtomic;
        this.itemName = itemName;
        this.qty = qty;
        this.freeQty = freeQty;
        this.qtyPacks = qtyPacks;
        this.freeQtyPacks = freeQtyPacks;
        this.itemDblValue = itemDblValue;
        this.itemStock = itemStock;
        this.stockQty = stockQty;
        this.batchNo = batchNo;
    }

    public PharmacyBinCardDTO(Long id,
            Date createdAt,
            BillType billType,
            BillTypeAtomic billTypeAtomic,
            String itemName,
            Double qty,
            Double freeQty,
            Double qtyPacks,
            Double freeQtyPacks,
            Double itemDblValue,
            Double itemStock) {
        this.id = id;
        this.createdAt = createdAt;
        this.billType = billType;
        this.billTypeAtomic = billTypeAtomic;
        this.itemName = itemName;
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

    public Class<?> getItemClass() {
        return itemClass;
    }

    public void setItemClass(Class<?> itemClass) {
        this.itemClass = itemClass;
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

    public Double getQtyPacks() {
        return qtyPacks;
    }

    public void setQtyPacks(Double qtyPacks) {
        this.qtyPacks = qtyPacks;
    }

    public Double getFreeQtyPacks() {
        return freeQtyPacks;
    }

    public void setFreeQtyPacks(Double freeQtyPacks) {
        this.freeQtyPacks = freeQtyPacks;
    }

    public Double getItemDblValue() {
        return itemDblValue;
    }

    public void setItemDblValue(Double itemDblValue) {
        this.itemDblValue = itemDblValue;
    }

    public Double getItemStock() {
        return itemStock;
    }

    public void setItemStock(Double itemStock) {
        this.itemStock = itemStock;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    // Derived helper properties
    public double getTransQtyPlusFreeQty() {
        double q = qty == null ? 0.0 : qty;
        double fq = freeQty == null ? 0.0 : freeQty;
        return q + fq;
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

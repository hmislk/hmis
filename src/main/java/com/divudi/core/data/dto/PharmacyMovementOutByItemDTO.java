package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for the "Pharmacy Movement Out with Stock" report, BY_ITEM view.
 *
 * Carries item identity plus database-aggregated movement-out values for a
 * single item, so the report can be built without loading full
 * {@code PharmaceuticalBillItem} entities and grouping them in memory.
 *
 * Current stock and supplier names are NOT part of this DTO; they are added in
 * a single batched query each, keyed by item id, after the rows are built.
 *
 * @author Dr M H B Ariyaratne
 */
public class PharmacyMovementOutByItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String itemName;
    private String itemCode;
    private String categoryName;

    private Double quantity;
    private Double grossValue;
    private Double discountValue;
    private Double marginValue;
    private Double netValue;

    public PharmacyMovementOutByItemDTO(
            Long itemId,
            String itemName,
            String itemCode,
            String categoryName,
            Double quantity,
            Double grossValue,
            Double discountValue,
            Double marginValue,
            Double netValue) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.categoryName = categoryName;
        this.quantity = quantity != null ? quantity : 0.0;
        this.grossValue = grossValue != null ? grossValue : 0.0;
        this.discountValue = discountValue != null ? discountValue : 0.0;
        this.marginValue = marginValue != null ? marginValue : 0.0;
        this.netValue = netValue != null ? netValue : 0.0;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public Double getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(Double marginValue) {
        this.marginValue = marginValue;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }
}

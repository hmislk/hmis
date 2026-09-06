package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Current stock held for one item within the selected scope.
 *
 * Populated by a constructor JPQL query over Stock grouped by item, for the
 * Ordering Requirement Report (issue #22466).
 *
 * @author Buddhika
 */
public class ItemCurrentStockDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String itemName;
    private String code;
    private Double qty;

    public ItemCurrentStockDto() {
    }

    public ItemCurrentStockDto(Long itemId, String itemName, String code, Double qty) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.code = code;
        this.qty = qty;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }
}

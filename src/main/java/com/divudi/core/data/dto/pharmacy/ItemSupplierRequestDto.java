package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

/**
 * Request payload for creating an item-supplier link.
 */
public class ItemSupplierRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long supplierId;
    private Long itemId;
    private Integer orderNo;

    public ItemSupplierRequestDto() {
    }

    public ItemSupplierRequestDto(Long supplierId, Long itemId) {
        this.supplierId = supplierId;
        this.itemId = itemId;
    }

    public ItemSupplierRequestDto(Long supplierId, Long itemId, Integer orderNo) {
        this.supplierId = supplierId;
        this.itemId = itemId;
        this.orderNo = orderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }
}

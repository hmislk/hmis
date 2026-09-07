/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.itemrequest;

import java.io.Serializable;

/**
 * A single line item within an {@link ItemRequestResponseDTO}.
 *
 * @author Claude AI Assistant
 */
public class ItemRequestLineResponseDTO implements Serializable {

    private Long billItemId;
    private Long itemId;
    private String itemName;
    private String itemType;
    private double qty;
    private double netValue;
    private String status;
    private Long fulfillingBillId;
    private String fulfillingBillType;
    private String rejectionReason;

    public ItemRequestLineResponseDTO() {
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
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

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getNetValue() {
        return netValue;
    }

    public void setNetValue(double netValue) {
        this.netValue = netValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getFulfillingBillId() {
        return fulfillingBillId;
    }

    public void setFulfillingBillId(Long fulfillingBillId) {
        this.fulfillingBillId = fulfillingBillId;
    }

    public String getFulfillingBillType() {
        return fulfillingBillType;
    }

    public void setFulfillingBillType(String fulfillingBillType) {
        this.fulfillingBillType = fulfillingBillType;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}

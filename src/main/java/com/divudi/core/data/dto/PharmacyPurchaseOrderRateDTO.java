package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Lightweight projection for purchase order request last-rate lookups.
 */
public class PharmacyPurchaseOrderRateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private Double rate;
    private Long billItemId;

    public PharmacyPurchaseOrderRateDTO() {
    }

    public PharmacyPurchaseOrderRateDTO(Long itemId, BigDecimal rate, Long billItemId) {
        this.itemId = itemId;
        this.rate = rate == null ? 0.0 : rate.doubleValue();
        this.billItemId = billItemId;
    }

    public PharmacyPurchaseOrderRateDTO(Long itemId, Double rate, Long billItemId) {
        this.itemId = itemId;
        this.rate = rate == null ? 0.0 : rate;
        this.billItemId = billItemId;
    }

    public PharmacyPurchaseOrderRateDTO(Long itemId, double rate, Long billItemId) {
        this.itemId = itemId;
        this.rate = rate;
        this.billItemId = billItemId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }
}

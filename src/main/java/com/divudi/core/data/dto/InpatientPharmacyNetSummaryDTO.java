package com.divudi.core.data.dto;

import java.io.Serializable;

public class InpatientPharmacyNetSummaryDTO implements Serializable {

    private Long itemId;
    private String itemName;
    private Double netQty;
    private Double netGrossValue;
    private Double netDiscount;
    private Double netServiceCharge;
    private Double netValue;

    public InpatientPharmacyNetSummaryDTO() {
    }

    public InpatientPharmacyNetSummaryDTO(Long itemId, String itemName, Double netQty,
            Double netGrossValue, Double netDiscount, Double netServiceCharge, Double netValue) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.netQty = netQty;
        this.netGrossValue = netGrossValue;
        this.netDiscount = netDiscount;
        this.netServiceCharge = netServiceCharge;
        this.netValue = netValue;
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

    public Double getNetQty() {
        return netQty;
    }

    public void setNetQty(Double netQty) {
        this.netQty = netQty;
    }

    public Double getNetGrossValue() {
        return netGrossValue;
    }

    public void setNetGrossValue(Double netGrossValue) {
        this.netGrossValue = netGrossValue;
    }

    public Double getNetDiscount() {
        return netDiscount;
    }

    public void setNetDiscount(Double netDiscount) {
        this.netDiscount = netDiscount;
    }

    public Double getNetServiceCharge() {
        return netServiceCharge;
    }

    public void setNetServiceCharge(Double netServiceCharge) {
        this.netServiceCharge = netServiceCharge;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }
}

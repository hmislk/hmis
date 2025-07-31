package com.divudi.core.data.dto;

import java.io.Serializable;

public class StockReportByItemDTO implements Serializable {

    private String code;
    private String name;
    private Double qty;
    private Double purchaseValue;
    private Double saleValue;

    public StockReportByItemDTO() {
    }

    public StockReportByItemDTO(String code, String name, Double qty, Double purchaseValue, Double saleValue) {
        this.code = code;
        this.name = name;
        this.qty = qty;
        this.purchaseValue = purchaseValue;
        this.saleValue = saleValue;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public void setSaleValue(Double saleValue) {
        this.saleValue = saleValue;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Double getQty() {
        return qty;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public Double getSaleValue() {
        return saleValue;
    }
}


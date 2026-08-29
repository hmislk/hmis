package com.divudi.core.data.dto;

import java.io.Serializable;

public class BillFinanceDetailsDTO implements Serializable {

    private Long id;
    private Double netTotal;
    private Double grossTotal;
    private Double totalCostValue;
    private Double totalPurchaseValue;
    private Double totalRetailSaleValue;

    public BillFinanceDetailsDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public Double getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(Double totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public Double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(Double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public Double getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(Double totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }
}

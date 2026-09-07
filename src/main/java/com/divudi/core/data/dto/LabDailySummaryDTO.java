package com.divudi.core.data.dto;

import java.io.Serializable;

public class LabDailySummaryDTO implements Serializable {
    private String itemName;
    private Double cashValue;
    private Double cardValue;
    private Double onlineSettlementValue;
    private Double creditValue;
    private Double inwardCreditValue;
    private Double otherValue;
    private Double total;
    private Double discount;
    private Double serviceCharge;

    public LabDailySummaryDTO() {
    }

    public LabDailySummaryDTO(String itemName, Double cashValue, Double cardValue,
                              Double onlineSettlementValue, Double creditValue,
                              Double inwardCreditValue, Double otherValue,
                              Double total, Double discount, Double serviceCharge) {
        this.itemName = itemName;
        this.cashValue = cashValue;
        this.cardValue = cardValue;
        this.onlineSettlementValue = onlineSettlementValue;
        this.creditValue = creditValue;
        this.inwardCreditValue = inwardCreditValue;
        this.otherValue = otherValue;
        this.total = total;
        this.discount = discount;
        this.serviceCharge = serviceCharge;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getCashValue() {
        return cashValue;
    }

    public void setCashValue(Double cashValue) {
        this.cashValue = cashValue;
    }

    public Double getCardValue() {
        return cardValue;
    }

    public void setCardValue(Double cardValue) {
        this.cardValue = cardValue;
    }

    public Double getOnlineSettlementValue() {
        return onlineSettlementValue;
    }

    public void setOnlineSettlementValue(Double onlineSettlementValue) {
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public Double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(Double creditValue) {
        this.creditValue = creditValue;
    }

    public Double getInwardCreditValue() {
        return inwardCreditValue;
    }

    public void setInwardCreditValue(Double inwardCreditValue) {
        this.inwardCreditValue = inwardCreditValue;
    }

    public Double getOtherValue() {
        return otherValue;
    }

    public void setOtherValue(Double otherValue) {
        this.otherValue = otherValue;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }
}

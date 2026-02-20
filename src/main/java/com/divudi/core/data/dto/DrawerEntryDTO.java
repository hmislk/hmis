package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for DrawerEntry entity
 * Used in Balance History API to provide drawer transaction history
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class DrawerEntryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long drawerId;
    private String drawerName;
    private String paymentMethod;
    private Double beforeBalance;
    private Double afterBalance;
    private Double transactionValue;
    private Double beforeInHandValue;
    private Double afterInHandValue;
    private Long billId;
    private String billNumber;
    private Long paymentId;
    private Date createdAt;
    private String createrName;

    // Constructors
    public DrawerEntryDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDrawerId() {
        return drawerId;
    }

    public void setDrawerId(Long drawerId) {
        this.drawerId = drawerId;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getBeforeBalance() {
        return beforeBalance;
    }

    public void setBeforeBalance(Double beforeBalance) {
        this.beforeBalance = beforeBalance;
    }

    public Double getAfterBalance() {
        return afterBalance;
    }

    public void setAfterBalance(Double afterBalance) {
        this.afterBalance = afterBalance;
    }

    public Double getTransactionValue() {
        return transactionValue;
    }

    public void setTransactionValue(Double transactionValue) {
        this.transactionValue = transactionValue;
    }

    public Double getBeforeInHandValue() {
        return beforeInHandValue;
    }

    public void setBeforeInHandValue(Double beforeInHandValue) {
        this.beforeInHandValue = beforeInHandValue;
    }

    public Double getAfterInHandValue() {
        return afterInHandValue;
    }

    public void setAfterInHandValue(Double afterInHandValue) {
        this.afterInHandValue = afterInHandValue;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreaterName() {
        return createrName;
    }

    public void setCreaterName(String createrName) {
        this.createrName = createrName;
    }

    @Override
    public String toString() {
        return "DrawerEntryDTO{" +
                "id=" + id +
                ", drawerId=" + drawerId +
                ", drawerName='" + drawerName + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", transactionValue=" + transactionValue +
                ", beforeBalance=" + beforeBalance +
                ", afterBalance=" + afterBalance +
                '}';
    }
}

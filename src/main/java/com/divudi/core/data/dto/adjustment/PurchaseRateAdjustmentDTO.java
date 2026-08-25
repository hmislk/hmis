/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.adjustment;

import java.io.Serializable;

/**
 * DTO for Purchase Rate Adjustment API requests
 *
 * @author Buddhika
 */
public class PurchaseRateAdjustmentDTO implements Serializable {

    private Long stockId;
    private Double newPurchaseRate;
    private String comment;
    private Long departmentId;

    public PurchaseRateAdjustmentDTO() {
    }

    public PurchaseRateAdjustmentDTO(Long stockId, Double newPurchaseRate, String comment, Long departmentId) {
        this.stockId = stockId;
        this.newPurchaseRate = newPurchaseRate;
        this.comment = comment;
        this.departmentId = departmentId;
    }

    // Getters and Setters

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public Double getNewPurchaseRate() {
        return newPurchaseRate;
    }

    public void setNewPurchaseRate(Double newPurchaseRate) {
        this.newPurchaseRate = newPurchaseRate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}

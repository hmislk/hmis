/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.adjustment;

import java.io.Serializable;

/**
 * DTO for Retail Rate Adjustment API requests
 *
 * @author Buddhika
 */
public class RetailRateAdjustmentDTO implements Serializable {

    private Long stockId;
    private Double newRetailRate;
    private String comment;
    private Long departmentId;

    public RetailRateAdjustmentDTO() {
    }

    public RetailRateAdjustmentDTO(Long stockId, Double newRetailRate, String comment, Long departmentId) {
        this.stockId = stockId;
        this.newRetailRate = newRetailRate;
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

    public Double getNewRetailRate() {
        return newRetailRate;
    }

    public void setNewRetailRate(Double newRetailRate) {
        this.newRetailRate = newRetailRate;
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
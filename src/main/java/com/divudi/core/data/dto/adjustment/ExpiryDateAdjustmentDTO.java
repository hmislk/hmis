/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.adjustment;

import java.io.Serializable;

/**
 * DTO for Expiry Date Adjustment API requests
 *
 * @author Buddhika
 */
public class ExpiryDateAdjustmentDTO implements Serializable {

    private Long stockId;
    private String newExpiryDate;
    private String comment;
    private Long departmentId;

    public ExpiryDateAdjustmentDTO() {
    }

    public ExpiryDateAdjustmentDTO(Long stockId, String newExpiryDate, String comment, Long departmentId) {
        this.stockId = stockId;
        this.newExpiryDate = newExpiryDate;
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

    public String getNewExpiryDate() {
        return newExpiryDate;
    }

    public void setNewExpiryDate(String newExpiryDate) {
        this.newExpiryDate = newExpiryDate;
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
/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.adjustment;

import java.io.Serializable;

/**
 * DTO for Stock Quantity Adjustment API requests
 *
 * @author Buddhika
 */
public class StockQuantityAdjustmentDTO implements Serializable {

    private Long stockId;
    private Double newQuantity;
    private String comment;
    private Long departmentId;

    public StockQuantityAdjustmentDTO() {
    }

    public StockQuantityAdjustmentDTO(Long stockId, Double newQuantity, String comment, Long departmentId) {
        this.stockId = stockId;
        this.newQuantity = newQuantity;
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

    public Double getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Double newQuantity) {
        this.newQuantity = newQuantity;
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
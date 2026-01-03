/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.adjustment;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Adjustment API response data
 *
 * @author Buddhika
 */
public class AdjustmentResponseDTO implements Serializable {

    private Long billId;
    private String billNumber;
    private Long stockId;
    private String stockType; // "QUANTITY", "RETAIL_RATE", "EXPIRY_DATE"
    private Double beforeValue;
    private Double afterValue;
    private String beforeText; // For non-numeric values like expiry dates
    private String afterText; // For non-numeric values like expiry dates
    private String comment;
    private Date adjustmentDate;

    public AdjustmentResponseDTO() {
    }

    public AdjustmentResponseDTO(Long billId, String billNumber, Long stockId, String stockType) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.stockId = stockId;
        this.stockType = stockType;
    }

    // Getters and Setters

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

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getStockType() {
        return stockType;
    }

    public void setStockType(String stockType) {
        this.stockType = stockType;
    }

    public Double getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(Double beforeValue) {
        this.beforeValue = beforeValue;
    }

    public Double getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(Double afterValue) {
        this.afterValue = afterValue;
    }

    public String getBeforeText() {
        return beforeText;
    }

    public void setBeforeText(String beforeText) {
        this.beforeText = beforeText;
    }

    public String getAfterText() {
        return afterText;
    }

    public void setAfterText(String afterText) {
        this.afterText = afterText;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getAdjustmentDate() {
        return adjustmentDate;
    }

    public void setAdjustmentDate(Date adjustmentDate) {
        this.adjustmentDate = adjustmentDate;
    }
}
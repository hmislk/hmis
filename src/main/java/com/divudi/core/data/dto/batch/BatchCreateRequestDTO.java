/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.batch;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Batch Creation API requests
 *
 * @author Buddhika
 */
public class BatchCreateRequestDTO implements Serializable {

    private Long itemId; // Required - AMP ID
    private String batchNo; // Optional - auto-generate if null
    private Date expiryDate; // Required
    private Double retailRate; // Required
    private Double purchaseRate; // Optional - 85% of retail if null
    private Double costRate; // Optional - equals purchase if null
    private Double wholesaleRate; // Optional
    private Long departmentId; // Required
    private String comment; // Optional

    public BatchCreateRequestDTO() {
    }

    public BatchCreateRequestDTO(Long itemId, String batchNo, Date expiryDate, Double retailRate,
                                 Double purchaseRate, Double costRate, Double wholesaleRate,
                                 Long departmentId, String comment) {
        this.itemId = itemId;
        this.batchNo = batchNo;
        this.expiryDate = expiryDate;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.wholesaleRate = wholesaleRate;
        this.departmentId = departmentId;
        this.comment = comment;
    }

    // Getters and Setters

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }

    public Double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(Double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
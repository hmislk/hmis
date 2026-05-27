/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.batch;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Batch Creation API responses
 * Contains details of created batch and associated stock
 *
 * @author Buddhika
 */
public class BatchCreateResponseDTO implements Serializable {

    private Long batchId;
    private Long stockId;
    private String batchNo;
    private AmpResponseDTO item;
    private String departmentName;
    private Double retailRate;
    private Double purchaseRate;
    private Double costRate;
    private Date expiryDate;
    private String message;

    public BatchCreateResponseDTO() {
    }

    public BatchCreateResponseDTO(Long batchId, Long stockId, String batchNo, AmpResponseDTO item,
                                  String departmentName, Double retailRate, Double purchaseRate,
                                  Double costRate, Date expiryDate, String message) {
        this.batchId = batchId;
        this.stockId = stockId;
        this.batchNo = batchNo;
        this.item = item;
        this.departmentName = departmentName;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.expiryDate = expiryDate;
        this.message = message;
    }

    // Getters and Setters

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public AmpResponseDTO getItem() {
        return item;
    }

    public void setItem(AmpResponseDTO item) {
        this.item = item;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "BatchCreateResponseDTO{" +
                "batchId=" + batchId +
                ", stockId=" + stockId +
                ", batchNo='" + batchNo + '\'' +
                ", item=" + item +
                ", departmentName='" + departmentName + '\'' +
                ", retailRate=" + retailRate +
                ", purchaseRate=" + purchaseRate +
                ", costRate=" + costRate +
                ", expiryDate=" + expiryDate +
                ", message='" + message + '\'' +
                '}';
    }
}
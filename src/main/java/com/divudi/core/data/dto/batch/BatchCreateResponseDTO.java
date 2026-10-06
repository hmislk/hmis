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
    private Boolean batchCreated;
    private Boolean stockCreated;
    private Boolean ratesDiffer;
    private Double requestedRetailRate;
    private Double requestedPurchaseRate;
    private Double requestedCostRate;
    private Double quantity;
    private Long adjustmentBillId;
    private String adjustmentBillNumber;

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

    public Boolean getBatchCreated() {
        return batchCreated;
    }

    public void setBatchCreated(Boolean batchCreated) {
        this.batchCreated = batchCreated;
    }

    public Boolean getStockCreated() {
        return stockCreated;
    }

    public void setStockCreated(Boolean stockCreated) {
        this.stockCreated = stockCreated;
    }

    public Boolean getRatesDiffer() {
        return ratesDiffer;
    }

    public void setRatesDiffer(Boolean ratesDiffer) {
        this.ratesDiffer = ratesDiffer;
    }

    public Double getRequestedRetailRate() {
        return requestedRetailRate;
    }

    public void setRequestedRetailRate(Double requestedRetailRate) {
        this.requestedRetailRate = requestedRetailRate;
    }

    public Double getRequestedPurchaseRate() {
        return requestedPurchaseRate;
    }

    public void setRequestedPurchaseRate(Double requestedPurchaseRate) {
        this.requestedPurchaseRate = requestedPurchaseRate;
    }

    public Double getRequestedCostRate() {
        return requestedCostRate;
    }

    public void setRequestedCostRate(Double requestedCostRate) {
        this.requestedCostRate = requestedCostRate;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Long getAdjustmentBillId() {
        return adjustmentBillId;
    }

    public void setAdjustmentBillId(Long adjustmentBillId) {
        this.adjustmentBillId = adjustmentBillId;
    }

    public String getAdjustmentBillNumber() {
        return adjustmentBillNumber;
    }

    public void setAdjustmentBillNumber(String adjustmentBillNumber) {
        this.adjustmentBillNumber = adjustmentBillNumber;
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
                ", batchCreated=" + batchCreated +
                ", stockCreated=" + stockCreated +
                ", ratesDiffer=" + ratesDiffer +
                ", quantity=" + quantity +
                ", adjustmentBillNumber='" + adjustmentBillNumber + '\'' +
                '}';
    }
}
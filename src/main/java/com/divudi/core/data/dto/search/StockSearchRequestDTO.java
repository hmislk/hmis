/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.search;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Stock Search API request parameters
 * Supports comprehensive filtering for stock searches
 *
 * @author Buddhika
 */
public class StockSearchRequestDTO implements Serializable {

    // Required parameters
    private String query;       // Item name/code/barcode search term
    private String department;  // Department name

    // Optional quantity filters
    private Double minQuantity;
    private Double maxQuantity;

    // Optional rate filters
    private Double minRetailRate;
    private Double maxRetailRate;
    private Double minCostRate;
    private Double maxCostRate;
    private Double minPurchaseRate;
    private Double maxPurchaseRate;

    // Optional date filters
    private Date expiryAfter;   // Find stocks expiring after this date
    private Date expiryBefore;  // Find stocks expiring before this date

    // Optional batch filters
    private String batchNo;

    // Result limits
    private Integer limit;      // Default: 30

    // Search behavior flags
    private Boolean includeZeroStock;  // Include stocks with zero quantity

    public StockSearchRequestDTO() {
        this.limit = 30; // Default limit
        this.includeZeroStock = false; // Default: only positive stock
    }

    // Getters and Setters

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Double getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Double minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Double getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Double maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public Double getMinRetailRate() {
        return minRetailRate;
    }

    public void setMinRetailRate(Double minRetailRate) {
        this.minRetailRate = minRetailRate;
    }

    public Double getMaxRetailRate() {
        return maxRetailRate;
    }

    public void setMaxRetailRate(Double maxRetailRate) {
        this.maxRetailRate = maxRetailRate;
    }

    public Double getMinCostRate() {
        return minCostRate;
    }

    public void setMinCostRate(Double minCostRate) {
        this.minCostRate = minCostRate;
    }

    public Double getMaxCostRate() {
        return maxCostRate;
    }

    public void setMaxCostRate(Double maxCostRate) {
        this.maxCostRate = maxCostRate;
    }

    public Double getMinPurchaseRate() {
        return minPurchaseRate;
    }

    public void setMinPurchaseRate(Double minPurchaseRate) {
        this.minPurchaseRate = minPurchaseRate;
    }

    public Double getMaxPurchaseRate() {
        return maxPurchaseRate;
    }

    public void setMaxPurchaseRate(Double maxPurchaseRate) {
        this.maxPurchaseRate = maxPurchaseRate;
    }

    public Date getExpiryAfter() {
        return expiryAfter;
    }

    public void setExpiryAfter(Date expiryAfter) {
        this.expiryAfter = expiryAfter;
    }

    public Date getExpiryBefore() {
        return expiryBefore;
    }

    public void setExpiryBefore(Date expiryBefore) {
        this.expiryBefore = expiryBefore;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit != null && limit > 0 && limit <= 100 ? limit : 30;
    }

    public Boolean getIncludeZeroStock() {
        return includeZeroStock;
    }

    public void setIncludeZeroStock(Boolean includeZeroStock) {
        this.includeZeroStock = includeZeroStock != null ? includeZeroStock : false;
    }

    /**
     * Validation method for required fields
     */
    public boolean isValid() {
        return query != null && !query.trim().isEmpty() &&
               department != null && !department.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "StockSearchRequestDTO{" +
                "query='" + query + '\'' +
                ", department='" + department + '\'' +
                ", minQuantity=" + minQuantity +
                ", maxQuantity=" + maxQuantity +
                ", minRetailRate=" + minRetailRate +
                ", maxRetailRate=" + maxRetailRate +
                ", limit=" + limit +
                ", includeZeroStock=" + includeZeroStock +
                '}';
    }
}
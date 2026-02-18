/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class BeforeStockTakingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String itemCode;
    private String itemName;
    private String stockLocator;
    private String batchCode;
    private Date expiryDate;
    private Double systemStock;
    private Double purchaseRate;
    private Double retailRate;
    private String categoryName;
    private Long categoryId;
    private Long departmentId;
    private String departmentName;

    public BeforeStockTakingDTO(
            Long id,
            String itemCode,
            String itemName,
            String stockLocator,
            String batchCode,
            Date expiryDate,
            Double systemStock,
            Double purchaseRate,
            Double retailRate,
            String categoryName,
            Long categoryId,
            Long departmentId,
            String departmentName) {
        this.id = id;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.stockLocator = stockLocator;
        this.batchCode = batchCode;
        this.expiryDate = expiryDate;
        this.systemStock = systemStock;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getStockLocator() {
        return stockLocator;
    }

    public void setStockLocator(String stockLocator) {
        this.stockLocator = stockLocator;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getSystemStock() {
        return systemStock;
    }

    public void setSystemStock(Double systemStock) {
        this.systemStock = systemStock;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}

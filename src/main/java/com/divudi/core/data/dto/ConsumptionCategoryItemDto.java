package com.divudi.core.data.dto;

import java.io.Serializable;

public class ConsumptionCategoryItemDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String departmentName;
    private String categoryName;
    private String itemName;
    private double qty;
    private Double totalPurchaseValue;
    private Double totalCostValue;
    private Double totalRetailValue;
    private Double netTotal;

    public ConsumptionCategoryItemDto() {}

    public ConsumptionCategoryItemDto(
            String departmentName, String categoryName, String itemName,
            double qty, Double totalPurchaseValue, Double totalCostValue,
            Double totalRetailValue, Double netTotal) {
        this.departmentName = departmentName;
        this.categoryName = categoryName;
        this.itemName = itemName;
        this.qty = qty;
        this.totalPurchaseValue = totalPurchaseValue != null ? totalPurchaseValue : 0.0;
        this.totalCostValue = totalCostValue != null ? totalCostValue : 0.0;
        this.totalRetailValue = totalRetailValue != null ? totalRetailValue : 0.0;
        this.netTotal = netTotal != null ? netTotal : 0.0;
    }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public double getQty() { return qty; }
    public void setQty(double qty) { this.qty = qty; }
    public Double getTotalPurchaseValue() { return totalPurchaseValue; }
    public void setTotalPurchaseValue(Double totalPurchaseValue) { this.totalPurchaseValue = totalPurchaseValue; }
    public Double getTotalCostValue() { return totalCostValue; }
    public void setTotalCostValue(Double totalCostValue) { this.totalCostValue = totalCostValue; }
    public Double getTotalRetailValue() { return totalRetailValue; }
    public void setTotalRetailValue(Double totalRetailValue) { this.totalRetailValue = totalRetailValue; }
    public Double getNetTotal() { return netTotal; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }
}

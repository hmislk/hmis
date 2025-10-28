/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;

/**
 *
 * @author Pubudu Piyankara
 */
public class DepartmentCategoryWiseItems {


     private Department mainDepartment;
     private Department consumptionDepartment;
     private Item item;
     private Category category;
     private Double netTotal;
     private Double purchaseRate;
     private Double costRate;
     private Double totalPurchaseValue;
     private Double totalCostValue;
     private Double totalRetailValue;
     private double qty;
     private double paidAmount;

     public DepartmentCategoryWiseItems(){ }

     public DepartmentCategoryWiseItems(
        Department mainDepartment,
        Department consumptionDepartment,
        Item item,
        Category category,
        Double netTotal,
        Double purchaseRate,
        double qty
    ) {
        this.mainDepartment = mainDepartment;
        this.consumptionDepartment = consumptionDepartment;
        this.item = item;
        this.category = category;
        this.netTotal = netTotal;
        this.purchaseRate = purchaseRate;
        this.qty = qty;
    }

    public DepartmentCategoryWiseItems(
            Department mainDepartment,
            Department consumptionDepartment,
            Item item,
            Category category,
            Double netTotal,
            Double costRate,
            Double purchaseRate,
            double qty
    ) {
        this.mainDepartment = mainDepartment;
        this.consumptionDepartment = consumptionDepartment;
        this.item = item;
        this.category = category;
        this.netTotal = netTotal;
        this.costRate = costRate;
        this.purchaseRate = purchaseRate;
        this.qty = qty;
    }

    // Constructor for summary reports with aggregated values only (no rates)
    public DepartmentCategoryWiseItems(
            Department mainDepartment,
            Department consumptionDepartment,
            Item item,
            Category category,
            Double purchaseValue,
            Double totalCostValue,
            Double totalRetailValue,
            Double netTotal,
            double qty,
            String summaryType  // Dummy parameter to make signature unique
    ) {
        this.mainDepartment = mainDepartment;
        this.consumptionDepartment = consumptionDepartment;
        this.item = item;
        this.category = category;
        this.totalPurchaseValue = purchaseValue;  // Stock valuation at purchase rate
        this.totalCostValue = totalCostValue;      // Stock valuation at cost rate
        this.totalRetailValue = totalRetailValue;  // Stock valuation at retail rate
        this.netTotal = netTotal;                  // Financial value (actual billing)
        this.qty = qty;
        // summaryType is just for signature differentiation, not stored
    }

    public Department getMainDepartment() {
        return mainDepartment;
    }

    public void setMainDepartment(Department mainDepartment) {
        this.mainDepartment = mainDepartment;
    }

    public Department getConsumptionDepartment() {
        return consumptionDepartment;
    }

    public void setConsumptionDepartment(Department consumptionDepartment) {
        this.consumptionDepartment = consumptionDepartment;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }

    public Double getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(Double totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public Double getTotalRetailValue() {
        return totalRetailValue;
    }

    public void setTotalRetailValue(Double totalRetailValue) {
        this.totalRetailValue = totalRetailValue;
    }

    public Double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(Double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }
}

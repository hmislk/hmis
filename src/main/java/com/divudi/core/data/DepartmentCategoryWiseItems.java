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


}

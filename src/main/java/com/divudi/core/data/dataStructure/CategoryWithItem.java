/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import java.util.List;

/**
 *
 * @author safrin
 */
public class CategoryWithItem {

    private Category category;
    private Department department;
    private List<Item> item;
    private List<ItemWithFee> itemWithFees;
    private double subTotal;
    private double subHosTotal;
    private double qty;
    private double total;
    private Item itm;
    private double purchaseRate;
    private double saleRate;

    public CategoryWithItem(Category category, Department department) {
        this.category = category;
        this.department = department;
    }

    public CategoryWithItem(Category category, List<Item> item) {
        this.category = category;
        this.item = item;
    }

    public CategoryWithItem() {
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<ItemWithFee> getItemWithFees() {
        return itemWithFees;
    }

    public void setItemWithFees(List<ItemWithFee> itemWithFees) {
        this.itemWithFees = itemWithFees;
    }

    public double getSubTotal() {
        subTotal = 0.0;
        if (itemWithFees != null) {
            for (ItemWithFee it : itemWithFees) {
                subTotal += it.getTotal();
            }
        }
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getSubHosTotal() {
        subHosTotal = 0.0;
        if (itemWithFees != null) {
            for (ItemWithFee it : itemWithFees) {
                subHosTotal += it.getHospitalFee();
            }
        }
        return subHosTotal;
    }

    public void setSubHosTotal(double subHosTotal) {
        this.subHosTotal = subHosTotal;
    }

    public Department getDepartment() {

        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Item> getItem() {
        return item;
    }

    public void setItem(List<Item> item) {
        this.item = item;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Item getItm() {
        return itm;
    }

    public void setItm(Item itm) {
        this.itm = itm;
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getSaleRate() {
        return saleRate;
    }

    public void setSaleRate(double saleRate) {
        this.saleRate = saleRate;
    }
}

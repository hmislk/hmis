/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Department;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class DailyCash {

    private Department department;
    private List<CategoryWithItem> categoryWitmItems;
    private double departmentTotal;
    private double profFee;

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<CategoryWithItem> getCategoryWitmItems() {
        if(categoryWitmItems==null){
            categoryWitmItems=new ArrayList<>();
        }
        return categoryWitmItems;
    }

    public void setCategoryWitmItems(List<CategoryWithItem> categoryWitmItems) {
        this.categoryWitmItems = categoryWitmItems;
    }

    public double getDepartmentTotal() {
        departmentTotal = 0.0;
        for (CategoryWithItem cwi : getCategoryWitmItems()) {
            departmentTotal += cwi.getSubTotal();
        }


        return departmentTotal;
    }
    
    public double getPharmacyDepartmentTotal(){
        return departmentTotal;
    }

    public void setDepartmentTotal(double departmentTotal) {
        this.departmentTotal = departmentTotal;
    }

    public double getProfFee() {
        profFee = 0.0;
        for (CategoryWithItem cwi : categoryWitmItems) {
            for (ItemWithFee i : cwi.getItemWithFees()) {
                profFee += i.getProFee();
            }
        }

        return profFee;
    }

    public void setProfFee(double profFee) {
        this.profFee = profFee;
    }
}

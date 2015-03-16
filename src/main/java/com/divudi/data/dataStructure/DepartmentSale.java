/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Department;

/**
 *
 * @author safrin
 */
public class DepartmentSale {

    private Department department;
    Department toDepartment;
    Department fromDepartment;
    private double saleQty;
    private double saleValue;
    

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }
    
    

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public double getSaleQty() {
        return saleQty;
    }

    public double getSaleQtyAbs() {
        return Math.abs(saleQty);

    }

    public void setSaleQty(double saleQty) {
        this.saleQty = saleQty;
    }

    public double getSaleValue() {
        return saleValue;
    }
    
    public double getSaleValueAbs(){
        return Math.abs(saleValue);
    }

    public void setSaleValue(double saleValue) {
        this.saleValue = saleValue;
    }
}

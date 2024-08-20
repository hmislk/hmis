/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import java.util.List;

/**
 *
 * @author safrin
 */
public class DailyCredit {
    private Department department;
    private List<Bill> bills;
    private double discountTotal;
    private double netTotal;
    private double vatTotal;

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public double getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(double discountTotal) {
        this.discountTotal = discountTotal;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getVatTotal() {
        return vatTotal;
    }

    public void setVatTotal(double vatTotal) {
        this.vatTotal = vatTotal;
    }
    
}

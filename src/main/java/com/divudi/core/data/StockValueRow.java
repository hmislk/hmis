package com.divudi.core.data;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import java.io.Serializable;

public class StockValueRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private Institution institution;
    private Institution site;
    private Department department;

    private double purchaseValue;
    private double retailValue;
    private double costValue;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(double retailValue) {
        this.retailValue = retailValue;
    }

    public double getCostValue() {
        return costValue;
    }

    public void setCostValue(double costValue) {
        this.costValue = costValue;
    }
}

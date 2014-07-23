/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Institution;
import java.util.List;

/**
 *
 * @author safrin
 */
public class InstitutionSale {

    private List<DepartmentSale> departmentSales;
    private double institutionValue;
    private double institutionQty;
    private Institution institution;

  

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<DepartmentSale> getDepartmentSales() {
        return departmentSales;
    }

    public void setDepartmentSales(List<DepartmentSale> departmentSales) {
        this.departmentSales = departmentSales;
    }

    public double getInstitutionQty() {
        return institutionQty;
    }

    public void setInstitutionQty(double institutionQty) {
        this.institutionQty = institutionQty;
    }

    public double getInstitutionValue() {
        return institutionValue;
    }

    public void setInstitutionValue(double institutionValue) {
        this.institutionValue = institutionValue;
    }
}

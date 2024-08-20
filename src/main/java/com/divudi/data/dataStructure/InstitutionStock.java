/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Institution;
import java.util.List;

/**
 *
 * @author safrin
 */
public class InstitutionStock {

    private List<DepartmentStock> depatmentStocks;
    private double institutionTotal;
    private double institutionAverage;
    private Institution institution;

    public List<DepartmentStock> getDepatmentStocks() {
        return depatmentStocks;
    }

    public void setDepatmentStocks(List<DepartmentStock> depatmentStocks) {
        this.depatmentStocks = depatmentStocks;
    }

    public double getInstitutionTotal() {
        return institutionTotal;
    }

    public void setInstitutionTotal(double institutionTotal) {
        this.institutionTotal = institutionTotal;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public double getInstitutionAverage() {
        return institutionAverage;
    }

    public void setInstitutionAverage(double institutionAverage) {
        this.institutionAverage = institutionAverage;
    }
}

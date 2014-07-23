/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;

/**
 *
 * @author ruhunu
 */
public class BillHandoverItem {
    Bill bill;
    double professionalFee;
    double institutionFee;
    double totalFee;

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public double getProfessionalFee() {
        return professionalFee;
    }

    public void setProfessionalFee(double professionalFee) {
        this.professionalFee = professionalFee;
    }

    public double getInstitutionFee() {
        return institutionFee;
    }

    public void setInstitutionFee(double institutionFee) {
        this.institutionFee = institutionFee;
    }

    public double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }
    
    
    
}

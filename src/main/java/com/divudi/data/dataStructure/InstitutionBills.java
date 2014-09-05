/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import java.util.List;

/**
 *
 * @author safrin
 */



public class InstitutionBills {

    
    private Institution institution;
    private List<Bill> bills;
    private double returned;
    private double total;
    private double paidTotal;
    double paidByPatient;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getPaidTotal() {
        return paidTotal;
    }

    public void setPaidTotal(double paidTotal) {
        this.paidTotal = paidTotal;
    }

    public double getReturned() {
        return returned;
    }

    public void setReturned(double returned) {
        this.returned = returned;
    }

    public double getPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(double paidByPatient) {
        this.paidByPatient = paidByPatient;
    }
    
    
}

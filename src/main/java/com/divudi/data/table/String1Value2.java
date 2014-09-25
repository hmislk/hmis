/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.table;

import com.divudi.entity.BillItem;
import com.divudi.entity.Speciality;

/**
 *
 * @author ruhunu
 */
public class String1Value2 {

    private String string;
    private double value1;
    private double value2;
    Speciality speciality;
    private boolean summery;
    BillItem billItem;

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }
    
    

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }
    
    

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public double getValue1() {
        return value1;
    }

    public void setValue1(double value1) {
        this.value1 = value1;
    }

    public double getValue2() {
        return value2;
    }

    public void setValue2(double value2) {
        this.value2 = value2;
    }

    public boolean isSummery() {
        return summery;
    }

    public void setSummery(boolean summery) {
        this.summery = summery;
    }
}

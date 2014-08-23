/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.table;

import com.divudi.entity.Bill;
import java.util.List;

/**
 *
 * @author safrin
 */
public class String3Value2 {

    private String string1;
    private String string2;
    String string3;
    List<Bill> bills;
    private double value1;
    private double value2;
    private boolean summery;

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }
    
    

    public String getString3() {
        return string3;
    }

    public void setString3(String string3) {
        this.string3 = string3;
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
    
    

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public String getString2() {
        return string2;
    }

    public void setString2(String string2) {
        this.string2 = string2;
    }

   

    public boolean isSummery() {
        return summery;
    }

    public void setSummery(boolean summery) {
        this.summery = summery;
    }
    
    
}

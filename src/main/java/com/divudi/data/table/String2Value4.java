/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.table;

import java.util.Date;

/**
 *
 * @author safrin
 */
public class String2Value4 {

    private String string;
    private double value1;
    private double value2;
    private double value3;
    private double value4;
    Date date;

    @Override
    public int hashCode() {
        return date.hashCode();
    }

    //overridden method, has to be exactly the same like the following
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof String2Value4)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.date.equals(((String2Value4) obj).date);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public double getValue3() {
        return value3;
    }

    public void setValue3(double value3) {
        this.value3 = value3;
    }

    public double getValue4() {
        return value4;
    }

    public void setValue4(double value4) {
        this.value4 = value4;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}

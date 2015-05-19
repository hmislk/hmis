/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.table;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author safrin
 */
public class String1Value3 {

    private String string;
    private Double value1 = 0.0;
    private Double value2 = 0.0;
    private Double value3 = 0.0;
    Long longValue1;
    Boolean summery = false;
    Date date;

    public Long getLongValue1() {
        return longValue1;
    }

    public void setLongValue1(Long longValue1) {
        this.longValue1 = longValue1;
    }
    
    

    public String1Value3() {
    }

    public String1Value3(Object object, Double value1, Double value2, Double value3) {
        Date date = (Date) object;
        DateFormat df = new SimpleDateFormat("dd MMMM yyyy");
        String formattedDate = df.format(date);

        this.string = formattedDate;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public Double getValue1() {
        return value1;
    }

    public void setValue1(Double value1) {
        this.value1 = value1;
    }

    public Double getValue2() {
        return value2;
    }

    public void setValue2(Double value2) {
        this.value2 = value2;
    }

    public Double getValue3() {
        return value3;
    }

    public void setValue3(Double value3) {
        this.value3 = value3;
    }

    public Boolean isSummery() {
        return summery;
    }

    public void setSummery(Boolean summery) {
        this.summery = summery;
    }

    //overridden method, has to be exactly the same like the following
    public boolean equals(Object obj) {
        if (!(obj instanceof String1Value3)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.date.equals(((String1Value3) obj).date);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;

        DateFormat df = new SimpleDateFormat("dd MMMM yyyy");
        string = df.format(date);

    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }

}

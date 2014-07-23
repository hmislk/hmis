/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

/**
 *
 * @author safrin
 */
public class OtNormalSpecial {

    private DateRange dateRange;
    private double normalMin;
    private double specialMin;
    private double normalValue;
    private double specialValue;

    public double getNormalValue() {
        return normalValue;
    }

    public void setNormalValue(double normalValue) {
        this.normalValue = normalValue;
    }

    public double getSpecialValue() {
        return specialValue;
    }

    public void setSpecialValue(double specialValue) {
        this.specialValue = specialValue;
    }

    public double getNormalMin() {
        return normalMin;
    }

    public void setNormalMin(double normalMin) {
        this.normalMin = normalMin;
    }

    public double getSpecialMin() {
        return specialMin;
    }

    public void setSpecialMin(double specialMin) {
        this.specialMin = specialMin;
    }

    public DateRange getDateRange() {
        if (dateRange == null) {
            dateRange = new DateRange();
        }
        return dateRange;
    }

    public void setDateRange(DateRange dateRange) {

        this.dateRange = dateRange;
    }

}

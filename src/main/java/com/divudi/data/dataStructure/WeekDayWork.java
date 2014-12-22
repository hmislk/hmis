/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.dataStructure;

import com.divudi.entity.Staff;

/**
 *
 * @author safrin
 */
public class WeekDayWork {
    private Staff staff;
    private double sunDay;
    private double monDay;
    private double tuesDay;
    private double wednesDay;
    private double thursDay;
    private double friDay;
    private double saturDay;
    double total;
    double overTime;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getOverTime() {
        return overTime;
    }

    public void setOverTime(double overTime) {
        this.overTime = overTime;
    }
    
    

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public double getSunDay() {
        return sunDay;
    }

    public void setSunDay(double sunDay) {
        this.sunDay = sunDay;
    }

    public double getMonDay() {
        return monDay;
    }

    public void setMonDay(double monDay) {
        this.monDay = monDay;
    }

    public double getTuesDay() {
        return tuesDay;
    }

    public void setTuesDay(double tuesDay) {
        this.tuesDay = tuesDay;
    }

    public double getWednesDay() {
        return wednesDay;
    }

    public void setWednesDay(double wednesDay) {
        this.wednesDay = wednesDay;
    }

    public double getThursDay() {
        return thursDay;
    }

    public void setThursDay(double thursDay) {
        this.thursDay = thursDay;
    }

    public double getFriDay() {
        return friDay;
    }

    public void setFriDay(double friDay) {
        this.friDay = friDay;
    }

    public double getSaturDay() {
        return saturDay;
    }

    public void setSaturDay(double saturDay) {
        this.saturDay = saturDay;
    }
    
    
}

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
    double noPay;

    public String getNoPay() {
        return getDate(noPay);
    }

    public void setNoPay(double noPay) {
        this.noPay = noPay;
    }

    public String getTotal() {
        return getDate(total);
    }
    
    public double getTotalDouble() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getOverTime() {
        return getDate(overTime);
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

    private String getDate(double seconds) {
        if (seconds == 0.0) {
            return "";
        }

        long s = (long)seconds % 60;
        long m = ((long)seconds / 60) % 60;
        long h = ((long)seconds / (60 * 60)) % 24;
        return String.format("%d:%02d:%02d", h, m, s);
    }

    public String getSunDay() {
        return getDate(sunDay);
    }

    public void setSunDay(double sunDay) {
        this.sunDay = sunDay;
    }

    public String getMonDay() {
        return getDate(monDay);

    }

    public void setMonDay(double monDay) {
        this.monDay = monDay;
    }

    public String getTuesDay() {
        return getDate(tuesDay);
    }

    public void setTuesDay(double tuesDay) {
        this.tuesDay = tuesDay;
    }

    public String getWednesDay() {
        return getDate(wednesDay);
    }

    public void setWednesDay(double wednesDay) {
        this.wednesDay = wednesDay;
    }

    public String getThursDay() {
        return getDate(thursDay);
    }

    public void setThursDay(double thursDay) {
        this.thursDay = thursDay;
    }

    public String getFriDay() {
        return getDate(friDay);
    }

    public void setFriDay(double friDay) {
        this.friDay = friDay;
    }

    public String getSaturDay() {
        return getDate(saturDay);
    }

    public void setSaturDay(double saturDay) {
        this.saturDay = saturDay;
    }

}

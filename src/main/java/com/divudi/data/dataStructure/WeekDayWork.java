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
    private double sunDayExtra;
    private double monDayExtra;
    private double tuesDayExtra;
    private double wednesDayExtra;
    private double thursDayExtra;
    private double friDayExtra;
    private double saturDayExtra;
    double total;
    double overTime;
    double overTimeValue;
    double extraDuty;
    double extraDutyValue;
    double noPay;
    double basicPerSecond;

    public double getBasicPerSecond() {
        return basicPerSecond;
    }

    public void setBasicPerSecond(double basicPerSecond) {
        this.basicPerSecond = basicPerSecond;
    }
    
    

    public double getExtraDutyValue() {
        return extraDutyValue;
    }

    public void setExtraDutyValue(double extraDutyValue) {
        this.extraDutyValue = extraDutyValue;
    }
    
    

    public double getOverTimeValue() {
        return overTimeValue;
    }

    public void setOverTimeValue(double overTimeValue) {
        this.overTimeValue = overTimeValue;
    }

    
    
    public double getSunDayExtra() {
        return sunDayExtra;
    }

    public void setSunDayExtra(double sunDayExtra) {
        this.sunDayExtra = sunDayExtra;
    }

    public double getMonDayExtra() {
        return monDayExtra;
    }

    public void setMonDayExtra(double monDayExtra) {
        this.monDayExtra = monDayExtra;
    }

    public double getTuesDayExtra() {
        return tuesDayExtra;
    }

    public void setTuesDayExtra(double tuesDayExtra) {
        this.tuesDayExtra = tuesDayExtra;
    }

    public double getWednesDayExtra() {
        return wednesDayExtra;
    }

    public void setWednesDayExtra(double wednesDayExtra) {
        this.wednesDayExtra = wednesDayExtra;
    }

    public double getThursDayExtra() {
        return thursDayExtra;
    }

    public void setThursDayExtra(double thursDayExtra) {
        this.thursDayExtra = thursDayExtra;
    }

    public double getFriDayExtra() {
        return friDayExtra;
    }

    public void setFriDayExtra(double friDayExtra) {
        this.friDayExtra = friDayExtra;
    }

    public double getSaturDayExtra() {
        return saturDayExtra;
    }

    public void setSaturDayExtra(double saturDayExtra) {
        this.saturDayExtra = saturDayExtra;
    }
    
    

    public double getExtraDuty() {
        return extraDuty;
    }

    public void setExtraDuty(double extraDuty) {
        this.extraDuty = extraDuty;
    }
    
    

    public double getNoPay() {
        return (noPay);
    }

    public void setNoPay(double noPay) {
        this.noPay = noPay;
    }

    public double getTotal() {
        return (total);
    }
    
  

    public void setTotal(double total) {
        this.total = total;
    }

    public double getOverTime() {
        return (overTime);
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

//    private double (double seconds) {
//        if (seconds == 0.0) {
//            return "";
//        }
//
//        long s = (long)seconds % 60;
//        long m = ((long)seconds / 60) % 60;
//        long h = ((long)seconds / (60 * 60)) % 24;
//        return double.format("%d:%02d:%02d", h, m, s);
//    }

    public double getSunDay() {
        return (sunDay);
    }

    public void setSunDay(double sunDay) {
        this.sunDay = sunDay;
    }

    public double getMonDay() {
        return (monDay);

    }

    public void setMonDay(double monDay) {
        this.monDay = monDay;
    }

    public double getTuesDay() {
        return (tuesDay);
    }

    public void setTuesDay(double tuesDay) {
        this.tuesDay = tuesDay;
    }

    public double getWednesDay() {
        return (wednesDay);
    }

    public void setWednesDay(double wednesDay) {
        this.wednesDay = wednesDay;
    }

    public double getThursDay() {
        return (thursDay);
    }

    public void setThursDay(double thursDay) {
        this.thursDay = thursDay;
    }

    public double getFriDay() {
        return (friDay);
    }

    public void setFriDay(double friDay) {
        this.friDay = friDay;
    }

    public double getSaturDay() {
        return (saturDay);
    }

    public void setSaturDay(double saturDay) {
        this.saturDay = saturDay;
    }

}

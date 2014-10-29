/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.hr;

import com.divudi.entity.Staff;

/**
 *
 * @author safrin
 */
public class StaffShiftAggrgation {
    Staff staff;
    Double workingTime;
    Double leavedTime;
    Double overTime;
    Double extraDutyTime;

    public StaffShiftAggrgation(Staff staff, Double workingTime, Double leavedTime, Double overTime, Double extraDutyTime) {
        this.staff = staff;
        this.workingTime = workingTime;
        this.leavedTime = leavedTime;
        this.overTime = overTime;
        this.extraDutyTime = extraDutyTime;
    }

    public StaffShiftAggrgation(Staff staff, Double workingTime, Double leavedTime) {
        this.staff = staff;
        this.workingTime = workingTime;
        this.leavedTime = leavedTime;
    }

  
    
    

    public Double getOverTime() {
        return overTime;
    }

    public void setOverTime(Double overTime) {
        this.overTime = overTime;
    }

    public Double getExtraDutyTime() {
        return extraDutyTime;
    }

    public void setExtraDutyTime(Double extraDutyTime) {
        this.extraDutyTime = extraDutyTime;
    }
    
    

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Double getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(Double workingTime) {
        this.workingTime = workingTime;
    }

    public Double getLeavedTime() {
        return leavedTime;
    }

    public void setLeavedTime(Double leavedTime) {
        this.leavedTime = leavedTime;
    }
    
    
}

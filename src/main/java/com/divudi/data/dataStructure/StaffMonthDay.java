/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Staff;
import com.divudi.entity.hr.StaffWorkDay;
import java.util.List;

/**
 *
 * @author safrin
 */
public class StaffMonthDay {

    private Staff staff;
    private List<StaffWorkDay> staffWorkDays;
    private int weekdays;
    private int saturday;
    private int sunday;
    private int holiday;
    private double rosteredTime;
    private double workedTime;

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<StaffWorkDay> getStaffWorkDays() {
        return staffWorkDays;
    }

    public void setStaffWorkDays(List<StaffWorkDay> staffWorkDays) {
        this.staffWorkDays = staffWorkDays;
    }

    public int getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(int weekdays) {
        this.weekdays = weekdays;
    }

    public int getSaturday() {
        return saturday;
    }

    public void setSaturday(int saturday) {
        this.saturday = saturday;
    }

    public int getSunday() {
        return sunday;
    }

    public void setSunday(int sunday) {
        this.sunday = sunday;
    }

    public int getHoliday() {
        return holiday;
    }

    public void setHoliday(int holiday) {
        this.holiday = holiday;
    }

    public double getRosteredTime() {
        return rosteredTime;
    }

    public void setRosteredTime(double rosteredTime) {
        this.rosteredTime = rosteredTime;
    }

    public double getWorkedTime() {
        return workedTime;
    }

    public void setWorkedTime(double workedTime) {
        this.workedTime = workedTime;
    }
}

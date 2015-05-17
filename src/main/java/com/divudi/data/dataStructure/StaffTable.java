/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Staff;
import com.divudi.entity.hr.Shift;

/**
 *
 * @author safrin
 */
public class StaffTable {

    private Staff staff;
    private Shift shift;
    private boolean dayOff;
    private int repeatedDay;

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        if (this.staff == null && getRepeatedDay() > 0) {
            this.staff = staff;
        }
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        if (this.shift == null && getRepeatedDay() > 0) {
            this.shift = shift;
        }
    }

    public int getRepeatedDay() {
        return repeatedDay;
    }

    public void setRepeatedDay(int repeatedDay) {
        if (this.repeatedDay == 0) {
            this.repeatedDay = repeatedDay;
        } else {
            this.repeatedDay = repeatedDay--;
        }

        //   ////System.out.println("Staff : " + this.staff.getPerson().getName());
        //  ////System.out.println("Settin : " + this.shift.getName());
        //  ////System.out.println("Settin : " + this.repeatedDay);
    }

    public boolean isDayOff() {
        return dayOff;
    }

    public void setDayOff(boolean dayOff) {
        this.dayOff = dayOff;
    }
}

/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import com.divudi.entity.Department;
import com.divudi.entity.Staff;

/**
 *
 * @author safrin
 */
public class MonthEndRecord {

    Department department;
    Staff staff;
    double workedDays;
    double workedDaysBefore;
    double workedDaysThis;
    double workedDaysAditional;
    double extraDutyDays;
    double poyaDays;
    double poyaDaysLeave;
    double merhchantileDays;
    double merhchantileDaysLeave;
    double leave_annual;
    double leave_casual;
    double leave_medical;
    double leave_nopay;
    double leave_lieuleave;
    double leave_dutyLeave;
    double leave_maternity;
    double overtime;
    double latedays;
    double lateNoPays;
    double absent;
    double dayoff;
    double sleepingDays;
    
    public double getLateNoPays() {
        return lateNoPays;
    }

    public void setLateNoPays(double lateNoPays) {
        this.lateNoPays = lateNoPays;
    }
    
    public double getSleepingDays() {
        return sleepingDays;
    }

    public void setSleepingDays(double sleepingDays) {
        this.sleepingDays = sleepingDays;
    }
    
    public double getPoyaDays() {
        return poyaDays;
    }

    public void setPoyaDays(double poyaDays) {
        this.poyaDays = poyaDays;
    }

    public double getMerhchantileDays() {
        return merhchantileDays;
    }

    public void setMerhchantileDays(double merhchantileDays) {
        this.merhchantileDays = merhchantileDays;
    }
    
    public double getLeave_dutyLeave() {
        return leave_dutyLeave;
    }

    public void setLeave_dutyLeave(double leave_dutyLeave) {
        this.leave_dutyLeave = leave_dutyLeave;
    }
    
    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public double getWorkedDays() {
        return workedDays;
    }

    public void setWorkedDays(double workedDays) {
        this.workedDays = workedDays;
    }

    public double getLeave_annual() {
        return leave_annual;
    }

    public void setLeave_annual(double leave_annual) {
        this.leave_annual = leave_annual;
    }

    public double getLeave_casual() {
        return leave_casual;
    }

    public void setLeave_casual(double leave_casual) {
        this.leave_casual = leave_casual;
    }

    public double getLeave_medical() {
        return leave_medical;
    }

    public void setLeave_medical(double leave_medical) {
        this.leave_medical = leave_medical;
    }

    public double getLeave_nopay() {
        return leave_nopay;
    }

    public void setLeave_nopay(double leave_nopay) {
        this.leave_nopay = leave_nopay;
    }

    public double getLeave_lieuleave() {
        return leave_lieuleave;
    }

    public void setLeave_lieuleave(double leave_lieuleave) {
        this.leave_lieuleave = leave_lieuleave;
    }

    public double getOvertime() {
        return overtime;
    }

    public void setOvertime(double overtime) {
        this.overtime = overtime;
    }

    public double getLatedays() {
        return latedays;
    }

    public void setLatedays(double latedays) {
        this.latedays = latedays;
    }

    public double getAbsent() {
        return absent;
    }

    public void setAbsent(double absent) {
        this.absent = absent;
    }

    public double getDayoff() {
        return dayoff;
    }

    public void setDayoff(double dayoff) {
        this.dayoff = dayoff;
    }

    public double getExtraDutyDays() {
        return extraDutyDays;
    }

    public void setExtraDutyDays(double extraDutyDays) {
        this.extraDutyDays = extraDutyDays;
    }

    public double getPoyaDaysLeave() {
        return poyaDaysLeave;
    }

    public void setPoyaDaysLeave(double poyaDaysLeave) {
        this.poyaDaysLeave = poyaDaysLeave;
    }

    public double getMerhchantileDaysLeave() {
        return merhchantileDaysLeave;
    }

    public void setMerhchantileDaysLeave(double merhchantileDaysLeave) {
        this.merhchantileDaysLeave = merhchantileDaysLeave;
    }

    public double getWorkedDaysBefore() {
        return workedDaysBefore;
    }

    public void setWorkedDaysBefore(double workedDaysBefore) {
        this.workedDaysBefore = workedDaysBefore;
    }

    public double getWorkedDaysThis() {
        return workedDaysThis;
    }

    public void setWorkedDaysThis(double workedDaysThis) {
        this.workedDaysThis = workedDaysThis;
    }

    public double getWorkedDaysAditional() {
        return workedDaysAditional;
    }

    public void setWorkedDaysAditional(double workedDaysAditional) {
        this.workedDaysAditional = workedDaysAditional;
    }

    public double getLeave_maternity() {
        return leave_maternity;
    }

    public void setLeave_maternity(double leave_maternity) {
        this.leave_maternity = leave_maternity;
    }

}

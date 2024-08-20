/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.hr;

import com.divudi.entity.Staff;

/**
 *
 * @author divudi
 */
public class StaffLeaveBallance {

    Staff staff;
    LeaveType leaveType;
    Double count;

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public Double getCount() {
        return count;
    }

    public void setCount(Double count) {
        this.count = count;
    }

    public StaffLeaveBallance(Staff staff, LeaveType leaveType, Double count) {
        this.staff = staff;
        this.leaveType = leaveType;
        this.count = count;
    }

}

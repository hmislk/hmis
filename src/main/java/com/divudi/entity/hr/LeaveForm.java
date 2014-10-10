/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.entity.hr;

import com.divudi.data.hr.LeaveType;
import com.divudi.entity.Department;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class LeaveForm extends HrForm {
    private Department subDepartment;
    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;
    @Temporal(TemporalType.TIMESTAMP)
    private Date RequestedDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date forDate;
    private Shift forShift;
    private double numberOfLeave;

    public Department getSubDepartment() {
        return subDepartment;
    }

    public void setSubDepartment(Department subDepartment) {
        this.subDepartment = subDepartment;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public Date getRequestedDate() {
        return RequestedDate;
    }

    public void setRequestedDate(Date RequestedDate) {
        this.RequestedDate = RequestedDate;
    }

    public Date getForDate() {
        return forDate;
    }

    public void setForDate(Date forDate) {
        this.forDate = forDate;
    }

    public Shift getForShift() {
        return forShift;
    }

    public void setForShift(Shift forShift) {
        this.forShift = forShift;
    }

    public double getNumberOfLeave() {
        return numberOfLeave;
    }

    public void setNumberOfLeave(double numberOfLeave) {
        this.numberOfLeave = numberOfLeave;
    }
    
    
}

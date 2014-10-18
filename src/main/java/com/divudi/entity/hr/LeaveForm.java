/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class LeaveForm extends HrForm {

    @ManyToOne
    StaffLeave staffLeave;
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestedDate;  
    @ManyToOne
    private Shift shift;
    private double numberOfLeave;

    public StaffLeave getStaffLeave() {
        return staffLeave;
    }

    public void setStaffLeave(StaffLeave staffLeave) {
        this.staffLeave = staffLeave;
    }

    public Date getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(Date requestedDate) {
        this.requestedDate = requestedDate;
    }

    
    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public double getNumberOfLeave() {
        return numberOfLeave;
    }

    public void setNumberOfLeave(double numberOfLeave) {
        this.numberOfLeave = numberOfLeave;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class AmendmentForm extends HrForm {

    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;
    @ManyToOne
    private Staff fromStaff;
    @ManyToOne
    private Staff toStaff;
    @ManyToOne
    private StaffShift fromStaffShift;
    @ManyToOne
    private StaffShift toStaffShift;    
    @ManyToOne
    Shift toShift;    

    public Shift getToShift() {
        return toShift;
    }

    public void setToShift(Shift toShift) {
        this.toShift = toShift;
    }
    
    

    public Staff getFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public StaffShift getFromStaffShift() {
        return fromStaffShift;
    }

    public void setFromStaffShift(StaffShift fromStaffShift) {
        this.fromStaffShift = fromStaffShift;
    }

    public StaffShift getToStaffShift() {
        return toStaffShift;
    }

    public void setToStaffShift(StaffShift toStaffShift) {
        this.toStaffShift = toStaffShift;
    }

}

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class AmendmentForm extends HrForm {
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromFDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromTDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toFDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toTDate;
    private Staff fromStaff;
    private Staff toStaff;
    private Shift fromShift;
    private Shift toShift;
    private Department fromDepartment;
    private Department toDepartment;


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

    public Shift getFromShift() {
        return fromShift;
    }

    public void setFromShift(Shift fromShift) {
        this.fromShift = fromShift;
    }

    public Shift getToShift() {
        return toShift;
    }

    public void setToShift(Shift toShift) {
        this.toShift = toShift;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Date getFromFDate() {
        return fromFDate;
    }

    public void setFromFDate(Date fromFDate) {
        this.fromFDate = fromFDate;
    }

    public Date getFromTDate() {
        return fromTDate;
    }

    public void setFromTDate(Date fromTDate) {
        this.fromTDate = fromTDate;
    }

    public Date getToFDate() {
        return toFDate;
    }

    public void setToFDate(Date toFDate) {
        this.toFDate = toFDate;
    }

    public Date getToTDate() {
        return toTDate;
    }

    public void setToTDate(Date toTDate) {
        this.toTDate = toTDate;
    }
    
}

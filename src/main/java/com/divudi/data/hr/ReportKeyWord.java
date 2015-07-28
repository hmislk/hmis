/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.hr;

import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.Designation;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffCategory;
import com.divudi.entity.hr.StaffShift;

/**
 *
 * @author safrin
 */
public class ReportKeyWord {

    DayType[] dayTypes;
    Staff staff;
    Times times;    
    Staff replacingStaff;
    Department department;
    StaffCategory staffCategory;
    Designation designation;
    Roster roster;
    PaysheetComponent paysheetComponent;
    SalaryCycle salaryCycle;    
    Shift shift;
    Speciality speciality;
    Patient patient;
    Institution institution;
    Institution bank;
    PaymentMethod paymentMethod;
    Item item;
    StaffShift staffShift;
    LeaveType leaveType;
    Double from;
    Double to;
    Sex sex;
    EmployeeStatus employeeStatus;

    public PaysheetComponent getPaysheetComponent() {
        return paysheetComponent;
    }

    public void setPaysheetComponent(PaysheetComponent paysheetComponent) {
        this.paysheetComponent = paysheetComponent;
    }
    
    

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    
    public SalaryCycle getSalaryCycle() {
        return salaryCycle;
    }

    public void setSalaryCycle(SalaryCycle salaryCycle) {
        this.salaryCycle = salaryCycle;
    }
    
    

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }
    
    
    

    public Double getFrom() {
        if (from == null) {
            from = 0.0;
        }

        return from;
    }

    public void setFrom(Double from) {
        this.from = from;
    }

    public Double getTo() {
        if (to == null) {
            to = 0.0;
        }
        return to;
    }

    public void setTo(Double to) {
        this.to = to;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public StaffCategory getStaffCategory() {
        return staffCategory;
    }

    public void setStaffCategory(StaffCategory staffCategory) {
        this.staffCategory = staffCategory;
    }

    public Designation getDesignation() {
        return designation;
    }

    public void setDesignation(Designation designation) {
        this.designation = designation;
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public Staff getReplacingStaff() {
        return replacingStaff;
    }

    public void setReplacingStaff(Staff replacingStaff) {
        this.replacingStaff = replacingStaff;
    }

    public EmployeeStatus getEmployeeStatus() {
        return employeeStatus;
    }

    public void setEmployeeStatus(EmployeeStatus employeeStatus) {
        this.employeeStatus = employeeStatus;
    }

    public Times getTimes() {
        return times;
    }

    public void setTimes(Times times) {
        this.times = times;
    }

    public DayType[] getDayTypes() {
        return dayTypes;
    }

    public void setDayTypes(DayType[] dayTypes) {
        this.dayTypes = dayTypes;
    }

    public void resetKeyWord(){
        dayTypes = null;
        staff = null;
        times = null;
        replacingStaff = null;
        department = null;
        staffCategory = null;
        designation = null;
        roster = null;
        paysheetComponent = null;
        salaryCycle = null;
        shift = null;
        speciality = null;
        patient = null;
        institution = null;
        bank = null;
        paymentMethod = null;
        item = null;
        staffShift = null;
        leaveType = null;
        from = null;
        to = null;
        sex = null;
        employeeStatus = null;
    }
    
    

    
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.Times;
import com.divudi.entity.Department;
import com.divudi.entity.Form;
import com.divudi.entity.Staff;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@XmlRootElement
public class HrForm extends Form implements Serializable {

    @ManyToOne
    Staff staff;
    @ManyToOne
    Staff approvedStaff;
    @ManyToOne
    Department department;
    @Temporal(TemporalType.TIMESTAMP)
    Date approvedAt;
    @OneToOne(fetch = FetchType.LAZY)
    StaffShift staffShift;
    @Enumerated(EnumType.STRING)
    Times times;
    @ManyToOne
    Roster fromRoster;
    @ManyToOne
    Roster toRoster;
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toTime;
    @Transient
    boolean transFlag;

    public boolean isTransFlag() {
        return transFlag;
    }

    public void setTransFlag(boolean transFlag) {
        this.transFlag = transFlag;
    }
    
    

    public Date getFromTime() {
        return fromTime;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }
    
    

    public Times getTimes() {
        return times;
    }

    public void setTimes(Times times) {
        this.times = times;
    }

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
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
        if (staff == null) {
            return;
        }

        this.staff = staff;
        setFromRoster(staff.getRoster());

    }

    public Roster getFromRoster() {
        return fromRoster;
    }

    public void setFromRoster(Roster fromRoster) {
        this.fromRoster = fromRoster;
    }

    public Roster getToRoster() {
        return toRoster;
    }

    public void setToRoster(Roster toRoster) {
        this.toRoster = toRoster;
    }

    public Staff getApprovedStaff() {
        return approvedStaff;
    }

    public void setApprovedStaff(Staff approvedStaff) {
        this.approvedStaff = approvedStaff;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

}

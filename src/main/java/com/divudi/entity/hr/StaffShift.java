/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.WorkingType;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 *
 * @author safrin
 */
@Entity
public class StaffShift implements Serializable {

    @ManyToOne
    Shift shift;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne
    private Staff staff;
    @Temporal(TemporalType.TIMESTAMP)
    private Date shiftDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date shiftStartTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date shiftEndTime;
    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    private int repeatedCount;
    @Enumerated(EnumType.STRING)
    private WorkingType workingType;
//    private boolean consideredForOt;
//    boolean consideredForSalary;
//    boolean consideredForExtraDuty;

    @ManyToOne
    StaffSalary staffSalary;
    @ManyToOne
    FingerPrintRecord startRecord;
    @ManyToOne
    FingerPrintRecord endRecord;

    @ManyToOne
    StaffShift previousStaffShift;
    @ManyToOne
    StaffShift nextStaffShift;
    @ManyToOne
    StaffShift referenceStaffShift;

    double earlyInLogged;
    double earlyOutLogged;
    double earlyInVarified;
    double earlyOutVarified;
    double workedWithinTimeFrameLogged;
    double workedOutSideTimeFrameLogged;
    double workedTimeLogged;
    double workedWithinTimeFrameVarified;
    double workedOutSideTimeFrameVarified;
    double workedTimeVarified;
    double lateInVarified;
    double lateOutVarified;
    double lateInLogged;
    double lateOutLogged;
    double leavedTime;
    @Column(name = "overTimeFromStartRecordLogged")
    double extraTimeFromStartRecordLogged;
    @Column(name = "overTimeFromEndRecordLogged")
    double extraTimeFromEndRecordLogged;
    @Column(name = "overTimeCompleteRecordLogged")
    double extraTimeCompleteRecordLogged;
    @Column(name = "overTimeFromStartRecordVarified")
    double extraTimeFromStartRecordVarified;
    @Column(name = "overTimeFromEndRecordVarified")
    double extraTimeFromEndRecordVarified;
    @Column(name = "overTimeCompleteRecordVarified")
    double extraTimeCompleteRecordVarified;

    private boolean dayOff;
    private boolean sleepingDay;
    @Transient
    boolean transFirstColumn;
    @Transient
    Date transTime;
    @Enumerated(EnumType.STRING)
    LeaveType leaveType;
    double qty;
    @ManyToOne
    HrForm hrForm;

    public double getLeavedTime() {
        return leavedTime;
    }

    public void setLeavedTime(double leavedTime) {
        this.leavedTime = leavedTime;
    }

    public void calLeaveTime() {
        if (leaveType == null) {
            return;
        }

        switch (getLeaveType()) {
            case Annual:
            case Casual:
            case Lieu:
                setLeavedTime(getStaff().getLeaveHour() * 60 * 60);
                break;
            case AnnualHalf:
            case CasualHalf:
            case LieuHalf:
                setLeavedTime((getStaff().getLeaveHour() * 60 * 60) / 0.5);
                break;
            case No_Pay:
                setLeavedTime(0 - (getStaff().getLeaveHour() * 60 * 60));
                break;
            case No_Pay_Half:
                setLeavedTime(0 - ((getStaff().getLeaveHour() * 60 * 60) / 0.5));
                break;
        }
    }

    public HrForm getHrForm() {
        return hrForm;
    }

    public void setHrForm(HrForm hrForm) {
        this.hrForm = hrForm;
    }

    private void calLoggedStartRecord() {
        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        Long inSecond = 0l;
        if (getStartRecord().getLoggedRecord() == null
                || getStartRecord().getLoggedRecord().getRecordTimeStamp() == null) {
            return;
        }

        //Calculate Early In Logged
        if (getStartRecord().getLoggedRecord().getRecordTimeStamp().before(getShiftStartTime())) {
            fromCalendar.setTime(getStartRecord().getLoggedRecord().getRecordTimeStamp());
            toCalendar.setTime(getShiftStartTime());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            earlyInLogged = inSecond;
        }

        //Calculate Late In Logged
        if (getShiftStartTime().before(getStartRecord().getLoggedRecord().getRecordTimeStamp())) {
            fromCalendar.setTime(getShiftStartTime());
            toCalendar.setTime(getStartRecord().getLoggedRecord().getRecordTimeStamp());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            lateInLogged = inSecond;
        }
    }

    private void calLoggedEndRecord() {
        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        Long inSecond = 0l;
        if (getEndRecord().getLoggedRecord() == null
                || getEndRecord().getLoggedRecord().getRecordTimeStamp() == null) {
            return;
        }

        //Calculate Early Out Logged
        if (getShiftEndTime().before(getEndRecord().getLoggedRecord().getRecordTimeStamp())) {
            fromCalendar.setTime(getShiftEndTime());
            toCalendar.setTime(getEndRecord().getLoggedRecord().getRecordTimeStamp());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            earlyOutLogged = inSecond;
        }

        //Calculate Late Out Logged
        if (getShiftEndTime().before(getEndRecord().getLoggedRecord().getRecordTimeStamp())) {
            fromCalendar.setTime(getShiftEndTime());
            toCalendar.setTime(getEndRecord().getLoggedRecord().getRecordTimeStamp());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            lateOutLogged = inSecond;
        }

    }

    private void calWorkedTimeLogged() {

        if (getStartRecord().getLoggedRecord() == null
                || getStartRecord().getLoggedRecord().getRecordTimeStamp() == null
                || getEndRecord().getLoggedRecord() == null
                || getEndRecord().getLoggedRecord().getRecordTimeStamp() == null) {
            return;

        }
        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        double inSecond = 0.0;
        fromCalendar.setTime(getStartRecord().getLoggedRecord().getRecordTimeStamp());
        toCalendar.setTime(getEndRecord().getLoggedRecord().getRecordTimeStamp());

        //Worked Time Within Time Frame Looged
        inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
        inSecond = inSecond - (earlyInLogged + lateOutLogged);
        workedWithinTimeFrameLogged = inSecond;

        //Worked Out Side Time Frame Looged        
        inSecond = earlyInLogged + lateOutLogged;
        workedOutSideTimeFrameLogged = inSecond;

        //Worked Time Logged
        inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
        workedTimeLogged = inSecond;

    }

    private void calWorkedTimeVarified() {
        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        double inSecond = 0.0;
        fromCalendar.setTime(getStartRecord().getRecordTimeStamp());
        toCalendar.setTime(getEndRecord().getRecordTimeStamp());

        //Worked Time Within Time Frame Varified
        inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
        inSecond = inSecond - (earlyInVarified + lateOutVarified);
        workedWithinTimeFrameVarified = inSecond;

        //Worked Out Side Time Frame Varified        
        inSecond = earlyInVarified + lateOutVarified;
        workedOutSideTimeFrameVarified = inSecond;

        //Worked Time Varified
        inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
        workedTimeVarified = inSecond;

    }

    private void calVarifiedRecord() {
        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        Long inSecond = 0l;
        //Calculate Early In Varified
        if (getStartRecord().getRecordTimeStamp().before(getShiftStartTime())) {
            fromCalendar.setTime(getStartRecord().getRecordTimeStamp());
            toCalendar.setTime(getShiftStartTime());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            earlyInVarified = inSecond;
        }

        //Calculate Early Out Varified
        if (getEndRecord().getRecordTimeStamp().before(getShiftEndTime())) {
            fromCalendar.setTime(getEndRecord().getRecordTimeStamp());
            toCalendar.setTime(getShiftEndTime());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            earlyOutVarified = inSecond;
        }
        //Calculate Late In Varified
        if (getShiftStartTime().before(getStartRecord().getRecordTimeStamp())) {
            fromCalendar.setTime(getShiftStartTime());
            toCalendar.setTime(getStartRecord().getRecordTimeStamp());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            lateInVarified = inSecond;
        }
        //Calculate Late Out Varified
        if (getShiftEndTime().before(getEndRecord().getRecordTimeStamp())) {
            fromCalendar.setTime(getShiftEndTime());
            toCalendar.setTime(getEndRecord().getRecordTimeStamp());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            lateOutVarified = inSecond;
        }

    }

    private void calOverTime() {
        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        Long inSecond = 0l;
        //Over Time From Start Record Logged 
        extraTimeFromStartRecordLogged = 0;
        if (getStartRecord().isAllowedExtraDuty()
                && getStartRecord().getLoggedRecord() != null
                && getStartRecord().getLoggedRecord().getRecordTimeStamp() != null) {

            if (getStartRecord().getLoggedRecord().getRecordTimeStamp().before(getShiftStartTime())) {
                fromCalendar.setTime(getStartRecord().getLoggedRecord().getRecordTimeStamp());
                toCalendar.setTime(getShiftStartTime());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromStartRecordLogged = inSecond;
            }
        }

        //Over Time From End Record Logged 
        extraTimeFromEndRecordLogged = 0;
        if (getEndRecord().isAllowedExtraDuty()
                && getEndRecord().getLoggedRecord() != null
                && getEndRecord().getLoggedRecord().getRecordTimeStamp() != null) {

            if (getShiftEndTime().before(getEndRecord().getLoggedRecord().getRecordTimeStamp())) {
                fromCalendar.setTime(getShiftEndTime());
                toCalendar.setTime(getEndRecord().getLoggedRecord().getRecordTimeStamp());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromEndRecordLogged = inSecond;
            }
        }

        //Over Time From Start Record Varified 
        extraTimeFromStartRecordVarified = 0;
        if (getStartRecord().isAllowedExtraDuty()) {

            if (getStartRecord().getRecordTimeStamp().before(getShiftStartTime())) {
                fromCalendar.setTime(getStartRecord().getRecordTimeStamp());
                toCalendar.setTime(getShiftStartTime());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromStartRecordVarified = inSecond;
            }
        }

        //Over Time From End Record Varified
        extraTimeFromEndRecordVarified = 0;
        if (getEndRecord().isAllowedExtraDuty()) {
            if (getShiftEndTime().before(getEndRecord().getRecordTimeStamp())) {
                fromCalendar.setTime(getShiftEndTime());
                toCalendar.setTime(getEndRecord().getRecordTimeStamp());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromEndRecordVarified = inSecond;
            }
        }

    }

    public void calExtraDuty() {

        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        Long inSecond = 0l;

        //Logged 
        if (getStartRecord().getLoggedRecord() != null
                && getStartRecord().getLoggedRecord().getRecordTimeStamp() != null
                && getStartRecord().getLoggedRecord().isAllowedExtraDuty()
                && getEndRecord().getLoggedRecord() != null
                && getEndRecord().getLoggedRecord().getRecordTimeStamp() != null
                && getEndRecord().getLoggedRecord().isAllowedExtraDuty()) {
            fromCalendar.setTime(getStartRecord().getLoggedRecord().getRecordTimeStamp());
            toCalendar.setTime(getEndRecord().getLoggedRecord().getRecordTimeStamp());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            extraTimeCompleteRecordLogged = inSecond;
        }

        //Varified 
        if (getStartRecord().getRecordTimeStamp() != null
                && getStartRecord().isAllowedExtraDuty()
                && getEndRecord().getRecordTimeStamp() != null
                && getEndRecord().isAllowedExtraDuty()) {
            fromCalendar.setTime(getStartRecord().getRecordTimeStamp());
            toCalendar.setTime(getEndRecord().getRecordTimeStamp());
            inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
            extraTimeCompleteRecordVarified = inSecond;
        }

    }

    public void calCulateTimes() {
        if (getStartRecord() == null || getEndRecord() == null) {
            return;
        }

        if (getShiftStartTime() == null
                || getShiftEndTime() == null
                || getStartRecord().getRecordTimeStamp() == null
                || getEndRecord().getRecordTimeStamp() == null) {
            return;
        }

        calVarifiedRecord();
        calLoggedStartRecord();
        calLoggedEndRecord();
        calWorkedTimeLogged();
        calWorkedTimeVarified();
        calOverTime();
    }

    public double getEarlyInLogged() {
        return earlyInLogged;
    }

    public void setEarlyInLogged(double earlyInLogged) {
        this.earlyInLogged = earlyInLogged;
    }

    public double getEarlyOutLogged() {
        return earlyOutLogged;
    }

    public void setEarlyOutLogged(double earlyOutLogged) {
        this.earlyOutLogged = earlyOutLogged;
    }

    public double getEarlyInVarified() {
        return earlyInVarified;
    }

    public void setEarlyInVarified(double earlyInVarified) {
        this.earlyInVarified = earlyInVarified;
    }

    public double getEarlyOutVarified() {
        return earlyOutVarified;
    }

    public void setEarlyOutVarified(double earlyOutVarified) {
        this.earlyOutVarified = earlyOutVarified;
    }

    public double getWorkedWithinTimeFrameLogged() {
        return workedWithinTimeFrameLogged;
    }

    public void setWorkedWithinTimeFrameLogged(double workedWithinTimeFrameLogged) {
        this.workedWithinTimeFrameLogged = workedWithinTimeFrameLogged;
    }

    public double getWorkedOutSideTimeFrameLogged() {
        return workedOutSideTimeFrameLogged;
    }

    public void setWorkedOutSideTimeFrameLogged(double workedOutSideTimeFrameLogged) {
        this.workedOutSideTimeFrameLogged = workedOutSideTimeFrameLogged;
    }

    public double getWorkedWithinTimeFrameVarified() {
        return workedWithinTimeFrameVarified;
    }

    public void setWorkedWithinTimeFrameVarified(double workedWithinTimeFrameVarified) {
        this.workedWithinTimeFrameVarified = workedWithinTimeFrameVarified;
    }

    public double getWorkedOutSideTimeFrameVarified() {
        return workedOutSideTimeFrameVarified;
    }

    public void setWorkedOutSideTimeFrameVarified(double workedOutSideTimeFrameVarified) {
        this.workedOutSideTimeFrameVarified = workedOutSideTimeFrameVarified;
    }

    public double getExtraTimeFromStartRecordLogged() {
        return extraTimeFromStartRecordLogged;
    }

    public void setExtraTimeFromStartRecordLogged(double extraTimeFromStartRecordLogged) {
        this.extraTimeFromStartRecordLogged = extraTimeFromStartRecordLogged;
    }

    public double getExtraTimeFromEndRecordLogged() {
        return extraTimeFromEndRecordLogged;
    }

    public void setExtraTimeFromEndRecordLogged(double extraTimeFromEndRecordLogged) {
        this.extraTimeFromEndRecordLogged = extraTimeFromEndRecordLogged;
    }

    public double getExtraTimeCompleteRecordLogged() {
        return extraTimeCompleteRecordLogged;
    }

    public void setExtraTimeCompleteRecordLogged(double extraTimeCompleteRecordLogged) {
        this.extraTimeCompleteRecordLogged = extraTimeCompleteRecordLogged;
    }

    public double getExtraTimeFromStartRecordVarified() {
        return extraTimeFromStartRecordVarified;
    }

    public void setExtraTimeFromStartRecordVarified(double extraTimeFromStartRecordVarified) {
        this.extraTimeFromStartRecordVarified = extraTimeFromStartRecordVarified;
    }

    public double getExtraTimeFromEndRecordVarified() {
        return extraTimeFromEndRecordVarified;
    }

    public void setExtraTimeFromEndRecordVarified(double extraTimeFromEndRecordVarified) {
        this.extraTimeFromEndRecordVarified = extraTimeFromEndRecordVarified;
    }

    public double getExtraTimeCompleteRecordVarified() {
        return extraTimeCompleteRecordVarified;
    }

    public void setExtraTimeCompleteRecordVarified(double extraTimeCompleteRecordVarified) {
        this.extraTimeCompleteRecordVarified = extraTimeCompleteRecordVarified;
    }

    public double getLateInVarified() {
        return lateInVarified;
    }

    public void setLateInVarified(double lateInVarified) {
        this.lateInVarified = lateInVarified;
    }

    public double getLateOutVarified() {
        return lateOutVarified;
    }

    public void setLateOutVarified(double lateOutVarified) {
        this.lateOutVarified = lateOutVarified;
    }

    public double getLateInLogged() {
        return lateInLogged;
    }

    public void setLateInLogged(double lateInLogged) {
        this.lateInLogged = lateInLogged;
    }

    public double getLateOutLogged() {
        return lateOutLogged;
    }

    public void setLateOutLogged(double lateOutLogged) {
        this.lateOutLogged = lateOutLogged;
    }

    public StaffShift() {
        qty = 1;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public Date getTransTime() {
        return transTime;
    }

    public void setTransTime(Date transTime) {
        this.transTime = transTime;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public boolean isTransFirstColumn() {
        return transFirstColumn;
    }

    public boolean getIsTransFirstColumn() {
        return transFirstColumn;
    }

    public void setTransFirstColumn(boolean transFirstColumn) {
        this.transFirstColumn = transFirstColumn;
    }

    public StaffSalary getStaffSalary() {
        return staffSalary;
    }

    public void setStaffSalary(StaffSalary staffSalary) {
        this.staffSalary = staffSalary;
    }

    public void copy(StaffShift staffShift) {
        this.setDayOff(staffShift.isDayOff());
        this.setShift(staffShift.getShift());
        this.setEndRecord(staffShift.getEndRecord());
        this.setPreviousStaffShift(staffShift.getPreviousStaffShift());
        this.setNextStaffShift(staffShift.getNextStaffShift());
        this.setShiftDate(staffShift.getShiftDate());
        this.setShiftStartTime(staffShift.getShiftStartTime());
        this.setShiftEndTime(staffShift.getShiftEndTime());
        this.setSleepingDay(staffShift.isSleepingDay());
        this.setStaff(staffShift.getStaff());
        this.setWorkingType(staffShift.getWorkingType());

    }

    public FingerPrintRecord getStartRecord() {
        return startRecord;
    }

    public void setStartRecord(FingerPrintRecord startRecord) {
        this.startRecord = startRecord;
    }

    public FingerPrintRecord getEndRecord() {
        return endRecord;
    }

    public void setEndRecord(FingerPrintRecord endRecord) {
        this.endRecord = endRecord;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StaffShift)) {
            return false;
        }
        StaffShift other = (StaffShift) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.StaffShift[ id=" + id + " ]";
    }

    public Date getShiftDate() {
        return shiftDate;
    }

    public void setShiftDate(Date shiftDate) {
        this.shiftDate = shiftDate;
        calShiftStartEndTime();
    }

    public void calShiftStartEndTime() {
        if (getShift() == null) {
            return;
        }

        Calendar sTime = Calendar.getInstance();
        Calendar sDate = Calendar.getInstance();

        if (getShift().getStartingTime() == null) {
            return;
        }

        sTime.setTime(getShift().getStartingTime());
        sDate.setTime(shiftDate);
        sDate.set(Calendar.HOUR_OF_DAY, sTime.get(Calendar.HOUR_OF_DAY));
        sDate.set(Calendar.MINUTE, sTime.get(Calendar.MINUTE));
        setShiftStartTime(sDate.getTime());

        Calendar eDate = Calendar.getInstance();
        eDate.setTime(getShiftStartTime());
        eDate.add(Calendar.HOUR_OF_DAY, getShift().getDurationHour());
        eDate.set(Calendar.MINUTE, 0);

        setShiftEndTime(eDate.getTime());
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public int getRepeatedCount() {
        return repeatedCount;
    }

    public void setRepeatedCount(int repeatedCount) {
        this.repeatedCount = repeatedCount;
    }

    public WorkingType getWorkingType() {
        return workingType;
    }

    public void setWorkingType(WorkingType workingType) {
        this.workingType = workingType;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public StaffShift getPreviousStaffShift() {
        return previousStaffShift;
    }

    public void setPreviousStaffShift(StaffShift previousStaffShift) {
        this.previousStaffShift = previousStaffShift;
    }

    public StaffShift getNextStaffShift() {
        return nextStaffShift;
    }

    public void setNextStaffShift(StaffShift nextStaffShift) {
        this.nextStaffShift = nextStaffShift;
    }

    public Date getShiftStartTime() {
        return shiftStartTime;
    }

    public void setShiftStartTime(Date shiftStartTime) {
        this.shiftStartTime = shiftStartTime;
    }

    public Date getShiftEndTime() {
        return shiftEndTime;
    }

    public void setShiftEndTime(Date shiftEndTime) {
        //  //System.err.println("setting End Time");       
        this.shiftEndTime = shiftEndTime;
    }

    public boolean isDayOff() {
        return dayOff;
    }

    public void setDayOff(boolean dayOff) {
        this.dayOff = dayOff;
    }

    public boolean isSleepingDay() {
        return sleepingDay;
    }

    public void setSleepingDay(boolean sleepingDay) {
        this.sleepingDay = sleepingDay;
    }

    @Transient
    private List<FingerPrintRecord> fingerPrintRecordList;

    public List<FingerPrintRecord> getFingerPrintRecordList() {
        return fingerPrintRecordList;
    }

    public void setFingerPrintRecordList(List<FingerPrintRecord> fingerPrintRecordList) {
        this.fingerPrintRecordList = fingerPrintRecordList;
    }

    public StaffShift getReferenceStaffShift() {
        return referenceStaffShift;
    }

    public void setReferenceStaffShift(StaffShift referenceStaffShift) {
        this.referenceStaffShift = referenceStaffShift;
    }

    public double getWorkedTimeLogged() {
        return workedTimeLogged;
    }

    public void setWorkedTimeLogged(double workedTimeLogged) {
        this.workedTimeLogged = workedTimeLogged;
    }

    public double getWorkedTimeVarified() {
        return workedTimeVarified;
    }

    public void setWorkedTimeVarified(double workedTimeVarified) {
        this.workedTimeVarified = workedTimeVarified;
    }

}

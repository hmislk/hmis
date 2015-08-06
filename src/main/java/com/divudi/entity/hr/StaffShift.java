/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.DayType;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.Times;
import com.divudi.data.hr.WorkingType;
import com.divudi.entity.BillFee;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author safrin
 */
@Entity
@XmlRootElement
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
    @Enumerated(EnumType.STRING)
    DayType dayType;
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
    boolean consideredForSalary;
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
    @ManyToOne
    private StaffShift referenceStaffShiftLateIn;
    @ManyToOne
    private StaffShift referenceStaffShiftEarlyOut;
    @OneToMany(mappedBy = "referenceStaffShiftLateIn", fetch = FetchType.LAZY)
    private List<StaffShift> referenceStaffShiftLateIns = new ArrayList<>();
    @OneToMany(mappedBy = "referenceStaffShiftEarlyOut", fetch = FetchType.LAZY)
    private List<StaffShift> referenceStaffShiftEarlyOuts = new ArrayList<>();

    //Multiplying Factor Always come by subtrating 1
    // if Multiplying Factor for Salary is 1 ,but actual value is 2
    @Column(name = "multiplyingFactor")
    private double multiplyingFactorSalary;
    //Multyiplying factor for Ot is Actual one.there is no substration
    double multiplyingFactorOverTime;
    //Varified Data
    double earlyInVarified;
    double earlyOutVarified;
    double workedWithinTimeFrameVarified;
    double workedOutSideTimeFrameVarified;
    double workedTimeVarified;
    double lateInVarified;
    double lateOutVarified;
    @Column(name = "overTimeFromStartRecordVarified")
    double extraTimeFromStartRecordVarified;
    @Column(name = "overTimeFromEndRecordVarified")
    double extraTimeFromEndRecordVarified;
    @Column(name = "overTimeCompleteRecordVarified")
    double extraTimeCompleteRecordVarified;
    //Logged Data
    @Column(name = "basicPerSecond")
    double overTimeValuePerSecond;
    double earlyInLogged;
    double earlyOutLogged;
    double workedWithinTimeFrameLogged;
    double workedOutSideTimeFrameLogged;
    double workedTimeLogged;
    double lateInLogged;
    double lateOutLogged;
    @Column(name = "overTimeFromStartRecordLogged")
    double extraTimeFromStartRecordLogged;
    @Column(name = "overTimeFromEndRecordLogged")
    double extraTimeFromEndRecordLogged;
    @Column(name = "overTimeCompleteRecordLogged")
    double extraTimeCompleteRecordLogged;
    //Leaved Time
    double leavedTime;
    private double leavedTimeNoPay;
    double leavedTimeOther;
    private boolean dayOff;
    private boolean sleepingDay;
    @Transient
    boolean transFirstColumn;
    @Transient
    Date transTime;
    @Enumerated(EnumType.STRING)
    LeaveType leaveType;
    double qty;
    private boolean autoLeave;
    @ManyToOne
//    @Column(name = "hrForm")
    HrForm additionalForm;
    @ManyToOne
    HrForm leaveForm;
    @ManyToOne
    HrForm amendmentForm;
    @ManyToOne
    Roster roster;
    double lieuQty;
    double lieuQtyUtilized;
    boolean lieuPaid;
    boolean lieuAllowed;
    boolean lieuPaymentAllowed;
//    boolean consideredForLateEarlyAttendance;
    boolean considerForLateIn;
    boolean considerForEarlyOut;
    @Transient
    boolean transChecked;
    int dayOfWeek;
//    int leaveDivident;

    @Transient
    double transWorkTime;
    @Transient
    double transShiftTime;

    public DayType getDayType() {
        return dayType;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
    }

    public HrForm getLeaveForm() {
        return leaveForm;
    }

    public void setLeaveForm(HrForm leaveForm) {
        this.leaveForm = leaveForm;
    }

    public HrForm getAmendmentForm() {
        return amendmentForm;
    }

    public void setAmendmentForm(HrForm amendmentForm) {
        this.amendmentForm = amendmentForm;
    }

//    public boolean isConsideredForLateEarlyAttendance() {
//        return consideredForLateEarlyAttendance;
//    }
//
//    public void setConsideredForLateEarlyAttendance(boolean consideredForLateEarlyAttendance) {
//        this.consideredForLateEarlyAttendance = consideredForLateEarlyAttendance;
//    }
    public boolean isConsiderForLateIn() {
        return considerForLateIn;
    }

    public void setConsiderForLateIn(boolean considerForLateIn) {
        this.considerForLateIn = considerForLateIn;
    }

    public boolean isConsiderForEarlyOut() {
        return considerForEarlyOut;
    }

    public void setConsiderForEarlyOut(boolean considerForEarlyOut) {
        this.considerForEarlyOut = considerForEarlyOut;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public boolean isTransChecked() {
        return transChecked;
    }

    public void setTransChecked(boolean transChecked) {
        this.transChecked = transChecked;
    }

    public void resetLeaveData() {
        lieuQty = 0;
        leavedTime = 0;
        leavedTimeNoPay = 0;
        leavedTimeOther = 0;
    }

    public void resetLeaveData(LeaveType leaveType) {
        if (leaveType == LeaveType.Lieu || leaveType == LeaveType.LieuHalf) {
            lieuQty = 0;
//            lieuPaymentAllowed = false;
        }

        switch (leaveType) {
            case Annual:
            case AnnualHalf:
            case Casual:
            case CasualHalf:
            case Lieu:
            case LieuHalf:
            case DutyLeave:
                leavedTime = 0;
                break;
            case No_Pay:
            case No_Pay_Half:
                leavedTimeNoPay = 0;
                break;
            case Medical:
            case Maternity1st:
            case Maternity1stHalf:
            case Maternity2nd:
            case Maternity2ndHalf:
                leavedTimeOther = 0;
                break;

        }

    }

    public void resetFingerPrintRecordTime() {
        earlyInLogged = 0;
        earlyOutLogged = 0;
        earlyInVarified = 0;
        earlyOutVarified = 0;
        workedWithinTimeFrameLogged = 0;
        workedOutSideTimeFrameLogged = 0;
        workedTimeLogged = 0;
        workedWithinTimeFrameVarified = 0;
        workedOutSideTimeFrameVarified = 0;
        workedTimeVarified = 0;
        lateInVarified = 0;
        lateOutVarified = 0;
        lateInLogged = 0;
        lateOutLogged = 0;

    }

    public void resetExtraTime() {
        extraTimeFromStartRecordLogged = 0;
        extraTimeFromEndRecordLogged = 0;
        extraTimeCompleteRecordLogged = 0;
        extraTimeFromStartRecordVarified = 0;
        extraTimeFromEndRecordVarified = 0;
        extraTimeCompleteRecordVarified = 0;
    }

    public void reset() {
        resetFingerPrintRecordTime();
        resetExtraTime();
//        resetLeaveData();
//        considerForLateIn = false;
//        considerForEarlyOut = false;
        multiplyingFactorOverTime = 0;
        multiplyingFactorSalary = 0;
        overTimeValuePerSecond = 0;
    }

    public void processLieuQtyUtilized(LeaveType leaveType) {
        if (leaveType == null) {
            return;
        }

        if (leaveType == LeaveType.Lieu && getLieuQtyUtilized() == 0) {
            setLieuQtyUtilized(1);
        }

        if (leaveType == LeaveType.LieuHalf && getLieuQtyUtilized() != 1) {
            setLieuQtyUtilized(getLieuQtyUtilized() + 0.5);
        }

    }

    public double getLieuQtyUtilized() {
        return lieuQtyUtilized;
    }

    public void setLieuQtyUtilized(double lieuQtyUtilized) {
        this.lieuQtyUtilized = lieuQtyUtilized;
    }

    public double getLieuQty() {
        return lieuQty;
    }

    public void setLieuQty(double lieuQty) {
        this.lieuQty = lieuQty;
    }

    public boolean isLieuPaid() {
        return lieuPaid;
    }

    public void setLieuPaid(boolean lieuPaid) {
        this.lieuPaid = lieuPaid;
    }

    public boolean isLieuAllowed() {
        return lieuAllowed;
    }

    public void setLieuAllowed(boolean lieuAllowed) {
        this.lieuAllowed = lieuAllowed;
    }

    public boolean isLieuPaymentAllowed() {
        return lieuPaymentAllowed;
    }

    public void setLieuPaymentAllowed(boolean lieuPaymentAllowed) {
        this.lieuPaymentAllowed = lieuPaymentAllowed;
    }

    public void calLieu() {
        if (getShift() == null) {
            return;
        }

        if (lieuAllowed) {
            lieuQty = getShift().isHalfShift() ? 0.5 : 1;
        }

//        if (getStartRecord() != null
//                && getEndRecord() != null
//                && getStartRecord().getRecordTimeStamp() != null
//                && getEndRecord().getRecordTimeStamp() != null) {
        DayType dtp = getShift().getDayType();
        if (dtp == DayType.DayOff
                || dtp == DayType.SleepingDay) {
            lieuAllowed = true;
            lieuPaymentAllowed = true;
            lieuQty = getShift().isHalfShift() ? 0.5 : 1;
        }
//        }
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
    }

    public double getLeavedTime() {
        return leavedTime;
    }

    public void setLeavedTime(double leavedTime) {
        this.leavedTime = leavedTime;
    }

    public void calLeaveTime() {
        System.out.println("leaveType = " + leaveType);
        if (leaveType == null) {
            return;
        }
        System.out.println("shift = " + shift);
        if (shift == null) {
            return;
        }

        switch (getLeaveType()) {
            case Annual:
            case Casual:
            case Lieu:
            case DutyLeave:
                setLeavedTime((shift.getLeaveHourFull() * 60 * 60));
                break;
            case Maternity1st:
            case Maternity2nd:
            case Medical:
                setLeavedTimeOther((shift.getLeaveHourFull() * 60 * 60));
                break;
            case No_Pay:
                setLeavedTimeNoPay((shift.getLeaveHourFull() * 60 * 60));
                break;
            case AnnualHalf:
            case CasualHalf:
            case LieuHalf:
                setLeavedTime((shift.getLeaveHourHalf() * 60 * 60));
                break;
            case Maternity1stHalf:
            case Maternity2ndHalf:
                setLeavedTimeOther((shift.getLeaveHourHalf() * 60 * 60));
                break;
            case No_Pay_Half:
                setLeavedTimeNoPay((shift.getLeaveHourHalf() * 60 * 60));
                break;
        }
    }

    public HrForm getAdditionalForm() {
        return additionalForm;
    }

    public void setAdditionalForm(HrForm additionalForm) {
        this.additionalForm = additionalForm;
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

    public void calExtraTimeWithStartOrEndRecord() {
        if (getStartRecord() == null || getEndRecord() == null) {
            return;
        }

        Calendar fromCalendar = Calendar.getInstance();
        Calendar toCalendar = Calendar.getInstance();
        Long inSecond = 0l;
        System.err.println("In");
        System.out.println("getShiftStartTime() = " + getShiftStartTime());
        System.out.println("getShiftEndTime() = " + getShiftEndTime());
        System.out.println("getStartRecord() = " + getStartRecord());
        System.out.println("getEndRecord() = " + getEndRecord());

        //Over Time From Start Record Logged 
        extraTimeFromStartRecordLogged = 0;
        if (getStartRecord().isAllowedExtraDuty()
                && getStartRecord().getLoggedRecord() != null
                && getStartRecord().getLoggedRecord().getRecordTimeStamp() != null) {

            if (getShiftStartTime() != null
                    && getStartRecord().getLoggedRecord().getRecordTimeStamp().before(getShiftStartTime())) {
                fromCalendar.setTime(getStartRecord().getLoggedRecord().getRecordTimeStamp());
                toCalendar.setTime(getShiftStartTime());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromStartRecordLogged = inSecond;
                System.out.println("1.inSecond = " + inSecond);
                System.out.println("getStartRecord().getLoggedRecord().getRecordTimeStamp() = " + getStartRecord().getLoggedRecord().getRecordTimeStamp());
                System.out.println("getShiftStartTime() = " + getShiftStartTime());
            }
        }

        //Over Time From End Record Logged 
        extraTimeFromEndRecordLogged = 0;
        if (getEndRecord().isAllowedExtraDuty()
                && getEndRecord().getLoggedRecord() != null
                && getEndRecord().getLoggedRecord().getRecordTimeStamp() != null) {

            if (getShiftEndTime() != null && getShiftEndTime().before(getEndRecord().getLoggedRecord().getRecordTimeStamp())) {
                fromCalendar.setTime(getShiftEndTime());
                toCalendar.setTime(getEndRecord().getLoggedRecord().getRecordTimeStamp());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromEndRecordLogged = inSecond;
                System.out.println("2.inSecond = " + inSecond);
                System.out.println("getShiftEndTime() = " + getShiftEndTime());
                System.out.println("getEndRecord().getLoggedRecord().getRecordTimeStamp() = " + getEndRecord().getLoggedRecord().getRecordTimeStamp());
            }
        }

        //Over Time From Start Record Varified 
        extraTimeFromStartRecordVarified = 0;
        if (getStartRecord().isAllowedExtraDuty() && getStartRecord().getRecordTimeStamp() != null) {
            System.out.println("3.getStartRecord().getRecordTimeStamp() = " + getStartRecord().getRecordTimeStamp());
            System.out.println("3.getShiftStartTime() = " + getShiftStartTime());
            if (getShiftStartTime() != null
                    && getStartRecord().getRecordTimeStamp().before(getShiftStartTime())) {
                fromCalendar.setTime(getStartRecord().getRecordTimeStamp());
                toCalendar.setTime(getShiftStartTime());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromStartRecordVarified = inSecond;
                System.out.println("3.inSecond = " + inSecond);
                System.out.println("getStartRecord().getRecordTimeStamp() = " + getStartRecord().getRecordTimeStamp());
                System.out.println("getShiftStartTime() = " + getShiftStartTime());
            }
        }

        //Over Time From End Record Varified
        extraTimeFromEndRecordVarified = 0;
        if (getEndRecord().isAllowedExtraDuty() && getEndRecord().getRecordTimeStamp() != null) {
            System.out.println("4.getShiftEndTime() = " + getShiftEndTime());
            System.out.println("4.getEndRecord().getRecordTimeStamp() = " + getEndRecord().getRecordTimeStamp());
            if (getShiftEndTime() != null && getShiftEndTime().before(getEndRecord().getRecordTimeStamp())) {
                fromCalendar.setTime(getShiftEndTime());
                toCalendar.setTime(getEndRecord().getRecordTimeStamp());
                inSecond = (toCalendar.getTimeInMillis() - fromCalendar.getTimeInMillis()) / (1000);
                extraTimeFromEndRecordVarified = inSecond;
                System.out.println("4.inSecond = " + inSecond);
                System.out.println("getShiftEndTime() = " + getShiftEndTime());
                System.out.println("getEndRecord().getRecordTimeStamp() = " + getEndRecord().getRecordTimeStamp());
            }
        }
        System.err.println("Out");
    }

    public void calExtraTimeComplete() {
        if (getStartRecord() == null || getEndRecord() == null) {
            return;
        }

        if (getShift() != null) {
            DayType dayType = getShift().getDayType();

            if (dayType == DayType.DayOff
                    || dayType == DayType.SleepingDay
                    || dayType == DayType.Extra) {

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
        this.setRoster(staffShift.getRoster());
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
        if (startRecord != null) {
            if (startRecord.getLoggedRecord() != null) {
                startRecord.getLoggedRecord().setTimes(Times.inTime);
            }
        }
    }

    public FingerPrintRecord getEndRecord() {
        return endRecord;
    }

    public void setEndRecord(FingerPrintRecord endRecord) {
        this.endRecord = endRecord;
        if (endRecord != null) {
            if (endRecord.getLoggedRecord() != null) {
                endRecord.getLoggedRecord().setTimes(Times.outTime);
            }
        }
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
        calDayOfWeek();
    }

    public void calDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(shiftDate);
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

    }

    public void calShiftStartEndTime() {
        if (getShift() == null) {
            return;
        }

        Calendar sTime = Calendar.getInstance();
        Calendar eTime = Calendar.getInstance();
        Calendar sDate = Calendar.getInstance();
        Calendar eDate = Calendar.getInstance();

        if (getShift().getStartingTime() == null) {
            return;
        }

        sTime.setTime(getShift().getStartingTime());
        sDate.setTime(shiftDate);
        sDate.set(Calendar.HOUR_OF_DAY, sTime.get(Calendar.HOUR_OF_DAY));
        sDate.set(Calendar.MINUTE, sTime.get(Calendar.MINUTE));
        setShiftStartTime(sDate.getTime());

        eTime.setTime(getShift().getEndingTime());
        eDate.setTime(shiftStartTime);
//        eDate.set(Calendar.HOUR_OF_DAY, sTime.get(Calendar.HOUR_OF_DAY));
        eDate.add(Calendar.MINUTE, (int) getShift().getDurationMin());
//        eDate.set(Calendar.MINUTE, eTime.get(Calendar.MINUTE));

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
        if (fingerPrintRecordList == null) {
            fingerPrintRecordList = new ArrayList<>();
        }
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

    public double getLeavedTimeNoPay() {
        return leavedTimeNoPay;
    }

    public void setLeavedTimeNoPay(double leavedTimeNoPay) {
        this.leavedTimeNoPay = leavedTimeNoPay;
    }

    public double getMultiplyingFactorSalary() {
        return multiplyingFactorSalary;
    }

    public void setMultiplyingFactorSalary(double multiplyingFactorSalary) {
        this.multiplyingFactorSalary = multiplyingFactorSalary;
    }

    public void calMultiplyingFactor(DayType dayType) {
        if (dayType == null) {
            return;
        }

        // Multiplying Factor Salary if 2 day Payment = 1
        // if one and half day  payment= 0.5
        switch (dayType) {
            case MurchantileHoliday:
                multiplyingFactorSalary = 2.0;//two day payments
                multiplyingFactorOverTime = 1.5;
                break;
            case Poya:
                multiplyingFactorSalary = 1.5;// One and Half Payment
                multiplyingFactorOverTime = 1.5;
                break;
            case DayOff:
                multiplyingFactorSalary = 2.0;// 2 Day Payments
                multiplyingFactorOverTime = 2.5;
                break;
            case SleepingDay:
                multiplyingFactorSalary = 2.0;// 2 Day Payments
                multiplyingFactorOverTime = 2.5;
                break;
            default:
                multiplyingFactorSalary = getShift().isHalfShift() == true ? 1 : 2;
                multiplyingFactorOverTime = 1.5;
                break;
        }

    }

    public double getMultiplyingFactorOverTime() {
        return multiplyingFactorOverTime;
    }

    public void setMultiplyingFactorOverTime(double multiplyingFactorOverTime) {
        this.multiplyingFactorOverTime = multiplyingFactorOverTime;
    }

    public double getLeavedTimeOther() {
        return leavedTimeOther;
    }

    public void setLeavedTimeOther(double leavedTimeOther) {
        this.leavedTimeOther = leavedTimeOther;
    }

    public double getOverTimeValuePerSecond() {
        return overTimeValuePerSecond;
    }

    public void setOverTimeValuePerSecond(double OverTimeValuePerSecond) {
        this.overTimeValuePerSecond = OverTimeValuePerSecond;
    }

    public boolean isConsideredForSalary() {
        return consideredForSalary;
    }

    public void setConsideredForSalary(boolean consideredForSalary) {
        this.consideredForSalary = consideredForSalary;
    }

    public StaffShift getReferenceStaffShiftLateIn() {
        return referenceStaffShiftLateIn;
    }

    public void setReferenceStaffShiftLateIn(StaffShift referenceStaffShiftLateIn) {
        this.referenceStaffShiftLateIn = referenceStaffShiftLateIn;
    }

    public StaffShift getReferenceStaffShiftEarlyOut() {
        return referenceStaffShiftEarlyOut;
    }

    public void setReferenceStaffShiftEarlyOut(StaffShift referenceStaffShiftEarlyOut) {
        this.referenceStaffShiftEarlyOut = referenceStaffShiftEarlyOut;
    }

    public boolean isAutoLeave() {
        return autoLeave;
    }

    public void setAutoLeave(boolean autoLeave) {
        this.autoLeave = autoLeave;
    }

    public List<StaffShift> getReferenceStaffShiftLateIns() {
        return referenceStaffShiftLateIns;
    }

    public void setReferenceStaffShiftLateIns(List<StaffShift> referenceStaffShiftLateIns) {
        this.referenceStaffShiftLateIns = referenceStaffShiftLateIns;
    }

    public List<StaffShift> getReferenceStaffShiftEarlyOuts() {
        return referenceStaffShiftEarlyOuts;
    }

    public void setReferenceStaffShiftEarlyOuts(List<StaffShift> referenceStaffShiftEarlyOuts) {
        this.referenceStaffShiftEarlyOuts = referenceStaffShiftEarlyOuts;
    }

    public double getTransWorkTime() {
        return transWorkTime;
    }

    public void setTransWorkTime(double transWorkTime) {
        this.transWorkTime = transWorkTime;
    }

    public double getTransShiftTime() {
        return transShiftTime;
    }

    public void setTransShiftTime(double transShiftTime) {
        this.transShiftTime = transShiftTime;
    }

}

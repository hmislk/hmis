/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.data.dataStructure.DateRange;
import com.divudi.data.dataStructure.ExtraDutyCount;
import com.divudi.data.hr.DateType;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.ExtraDutyType;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.data.hr.Times;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.HrForm;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.PhDate;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffLeaveEntitle;
import com.divudi.entity.hr.StaffLeaveSystem;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffSalaryComponant;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftExtra;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.PaysheetComponentFacade;
import com.divudi.facade.PhDateFacade;
import com.divudi.facade.ShiftFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffLeaveEntitleFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.facade.StaffSalaryComponantFacade;
import com.divudi.facade.StaffSalaryFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.java.CommonFunctions;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Stateless
public class HumanResourceBean {

    private LinkedList<Staff> staffLink;
    @EJB
    private StaffPaysheetComponentFacade staffPaysheetComponentFacade;
    @EJB
    private PaysheetComponentFacade paysheetComponentFacade;
    @EJB
    private StaffSalaryFacade staffSalaryFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private StaffShiftFacade staffShiftFacade;
    @EJB
    private PhDateFacade phDateFacade;
    @EJB
    private StaffLeaveFacade staffLeaveFacade;
    @EJB
    private FingerPrintRecordFacade fingerPrintRecordFacade;
    @EJB
    ShiftFacade shiftFacade;
    ///////////////////

    private CommonFunctions commonFunctions;
    @EJB
    private FinalVariables finalVariables;

    public boolean checkStaff(StaffPaysheetComponent staffPaysheetComponent,
            PaysheetComponent paysheetComponent, Staff staff, Date frmDate, Date toDate) {

        ////System.out.println("staffPaysheetComponent = " + staffPaysheetComponent);
        ////System.out.println("paysheetComponent = " + paysheetComponent);
        ////System.out.println("staff = " + staff);
        ////System.out.println("fromDate = " + frmDate);
        ////System.out.println("toDate = " + toDate);
        if (frmDate == null) {
            return true;
        }
        if (toDate == null) {
            return true;
        }

        if (frmDate.getTime() > toDate.getTime()) {
            return true;
        }

        String sql = "Select s From"
                + " StaffPaysheetComponent s "
                + " where s.retired=false"
                + " and s.paysheetComponent=:tp "
                + " and s.staff=:st "
                + " and s.fromDate<=:frm  "
                + " and s.toDate>=:frm ";

        if (staffPaysheetComponent.getId() != null) {
            sql += " and s.id!=" + staffPaysheetComponent.getId();
        }

        HashMap hm = new HashMap();
        hm.put("tp", paysheetComponent);
        hm.put("st", staff);
        hm.put("frm", frmDate);
        List<StaffPaysheetComponent> tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null && !tmp.isEmpty()) {
//            System.err.println("From Date ");
            return true;
        }

        sql = "Select s From"
                + " StaffPaysheetComponent s "
                + " where s.retired=false"
                + " and s.paysheetComponent=:tp "
                + " and s.staff=:st "
                + " and s.fromDate<=:to  "
                + " and s.toDate>=:to ";

        if (staffPaysheetComponent.getId() != null) {
            sql += " and s.id!=" + staffPaysheetComponent.getId();
        }

        hm = new HashMap();
        hm.put("tp", paysheetComponent);
        hm.put("st", staff);
        hm.put("to", toDate);
        tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null && !tmp.isEmpty()) {
//            System.err.println("To Date ");
            return true;
        }

        sql = "Select s From"
                + " StaffPaysheetComponent s "
                + " where s.retired=false"
                + " and s.paysheetComponent=:tp "
                + " and s.staff=:st "
                + " and s.fromDate<=:frm  "
                + " and s.toDate>=:to ";

        if (staffPaysheetComponent.getId() != null) {
            sql += " and s.id!=" + staffPaysheetComponent.getId();
        }

        hm = new HashMap();
        hm.put("tp", paysheetComponent);
        hm.put("st", staff);
        hm.put("frm", frmDate);
        hm.put("to", toDate);
        tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null && !tmp.isEmpty()) {
//            System.err.println("From && To Date ");
            return true;
        }

        return false;
    }

    public boolean checkStaff(PaysheetComponent paysheetComponentType, Staff staff, Date frmDate, Date toDate) {

        if (frmDate == null) {
            return true;
        }
        if (toDate == null) {
            return true;
        }

        if (frmDate.getTime() > toDate.getTime()) {
            return true;
        }

        String sql = "Select s From"
                + " StaffPaysheetComponent s "
                + " where s.retired=false"
                + " and s.paysheetComponent=:tp "
                + " and s.staff=:st "
                + " and s.fromDate<=:frm  "
                + " and s.toDate>=:frm ";

        HashMap hm = new HashMap();
        hm.put("tp", paysheetComponentType);
        hm.put("st", staff);
        hm.put("frm", frmDate);
        List<StaffPaysheetComponent> tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null && !tmp.isEmpty()) {
//            System.err.println("From Date ");
            return true;
        }

        sql = "Select s From"
                + " StaffPaysheetComponent s "
                + " where s.retired=false"
                + " and s.paysheetComponent=:tp "
                + " and s.staff=:st "
                + " and s.fromDate<=:to  "
                + " and s.toDate>=:to ";

        hm = new HashMap();
        hm.put("tp", paysheetComponentType);
        hm.put("st", staff);
        hm.put("to", toDate);
        tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null && !tmp.isEmpty()) {
            return true;
        }

        sql = "Select s From"
                + " StaffPaysheetComponent s "
                + " where s.retired=false"
                + " and s.paysheetComponent=:tp "
                + " and s.staff=:st "
                + " and s.fromDate<=:frm  "
                + " and s.toDate>=:to ";

        hm = new HashMap();
        hm.put("tp", paysheetComponentType);
        hm.put("st", staff);
        hm.put("frm", frmDate);
        hm.put("to", toDate);
        tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null && !tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    public boolean checkSalaryCycleDate(SalaryCycle salaryCycle, DateType dateType, Date frmDate, Date toDate) {

        ////System.out.println("paysheetComponent = " + salaryCycle);
        ////System.out.println("DateType = " + dateType);
        ////System.out.println("fromDate = " + frmDate);
        ////System.out.println("toDate = " + toDate);
        if (frmDate == null) {
            return true;
        }
        if (toDate == null) {
            return true;
        }

        if (frmDate.getTime() > toDate.getTime()) {
            return true;
        }

        String sql = "Select s From "
                + " SalaryCycle s "
                + " where s.retired=false ";

        switch (dateType) {
            case AdvanceDate:
                sql += " and s.salaryAdvanceFromDate<=:frm  "
                        + " and s.salaryAdvanceToDate>=:frm ";
                break;
            case OverTimeDate:
                sql += " and s.workedFromDate<=:frm  "
                        + " and s.workedToDate>=:frm ";
                break;
            case SalaryDate:
                sql += " and s.salaryFromDate<=:frm  "
                        + " and s.salaryToDate>=:frm ";
                break;
        }

        if (salaryCycle.getId() != null) {
            sql += " and s.id!=" + salaryCycle.getId();
        }

        HashMap hm = new HashMap();
        hm.put("frm", frmDate);
        List<StaffPaysheetComponent> tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

        if (tmp != null && !tmp.isEmpty()) {
            return true;
        }

        sql = "Select s From"
                + " SalaryCycle s "
                + " where s.retired=false";

        switch (dateType) {
            case AdvanceDate:
                sql += " and s.salaryAdvanceFromDate<=:to  "
                        + " and s.salaryAdvanceToDate>=:to ";
                break;
            case OverTimeDate:
                sql += " and s.workedFromDate<=:to  "
                        + " and s.workedToDate>=:to ";
                break;
            case SalaryDate:
                sql += " and s.salaryFromDate<=:to  "
                        + " and s.salaryToDate>=:to ";
                break;
        }

        if (salaryCycle.getId() != null) {
            sql += " and s.id!=" + salaryCycle.getId();
        }

        hm = new HashMap();

        hm.put("to", toDate);
        tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

        if (tmp != null && !tmp.isEmpty()) {
            return true;
        }

        sql = "Select s From"
                + " SalaryCycle s "
                + " where s.retired=false";

        switch (dateType) {
            case AdvanceDate:
                sql += " and s.salaryAdvanceFromDate<=:frm  "
                        + " and s.salaryAdvanceToDate>=:to ";
                break;
            case OverTimeDate:
                sql += " and s.workedFromDate<=:frm  "
                        + " and s.workedToDate>=:to ";
                break;
            case SalaryDate:
                sql += " and s.salaryFromDate<=:frm  "
                        + " and s.salaryToDate>=:to ";
                break;
        }

        if (salaryCycle.getId() != null) {
            sql += " and s.id!=" + salaryCycle.getId();
        }

        hm = new HashMap();
        hm.put("frm", frmDate);
        hm.put("to", toDate);
        tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

        if (tmp != null && !tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    public double getOverTimeFromRoster(double workingTimeForOverTimePerWeek, double numberOfWeeks, double totalWorkedTime) {
//        ////System.out.println("workingTimeForOverTimePerWeek = " + workingTimeForOverTimePerWeek);
//        ////System.out.println("numberOfWeeks = " + numberOfWeeks);
        if (workingTimeForOverTimePerWeek != 0 && numberOfWeeks != 0) {

            double normalWorkTime = numberOfWeeks * workingTimeForOverTimePerWeek * 60 * 60;
//            ////System.out.println("normalWorkTime = " + normalWorkTime);
            double overTime = totalWorkedTime - normalWorkTime;
//            ////System.out.println("overTime = " + overTime);

            if (overTime > 0) {
                return overTime;
            }
        }

        return 0.0;

    }
//    public StaffShift findInTime(FingerPrintRecord r) {
//        Date recordedDate = r.getRecordTimeStamp();
//        Calendar min = Calendar.getInstance();
//        min.setTime(recordedDate);
//        min.add(Calendar.HOUR, -1);
//
//        Calendar max = Calendar.getInstance();
//        max.setTime(recordedDate);
//        max.add(Calendar.HOUR, 2);
//
//        Map m = new HashMap();
//        m.put("f", min.getTime());
//        m.put("t", max.getTime());
//        m.put("s", r.getStaff());
//        m.put("ftp", FingerPrintRecordType.Varified);
//        String sql = "Select ss from StaffShift ss "
//                + " where ss.retired=false "
//                + " and ss.staff=:s "
//                + " and ss.previousStaffShift is null "
//                + " and ss.fingerPrintRecordType=:ftp "
//                + " and ss.shiftStartTime between :f and :t";
//        StaffShift ss = getStaffShiftFacade().findFirstByJpql(sql, m, TemporalType.TIMESTAMP);
//        //   System.err.println("Fetch Staff 1 : " + ss);
//        return ss;
//    }

    public FingerPrintRecord findInTimeRecord(StaffShift staffShift) {
        if (staffShift == null) {
            return null;
        }

        if (staffShift.getPreviousStaffShift() != null) {
            return null;
        }

        if (staffShift.getShiftStartTime() == null) {
            return null;
        }

        Date startTime = staffShift.getShiftStartTime();
        Calendar min = Calendar.getInstance();
        min.setTime(startTime);
        min.add(Calendar.HOUR, -2);

        Calendar max = Calendar.getInstance();
        max.setTime(startTime);
        max.add(Calendar.HOUR, 2);

        Map m = new HashMap();
        m.put("f", min.getTime());
        m.put("t", max.getTime());
        m.put("s", staffShift.getStaff());
        m.put("ftp", FingerPrintRecordType.Varified);
        String sql = "Select ss from FingerPrintRecord ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                //                + " and ss.loggedRecord is not null"
                + " and ss.fingerPrintRecordType=:ftp "
                + " and ss.recordTimeStamp between :f and :t";
        FingerPrintRecord ss = getFingerPrintRecordFacade().findFirstByJpql(sql, m, TemporalType.TIMESTAMP);
//        System.err.println("findInTimeRecordWithOutDayOffSleeping : " + ss);
        return ss;
    }

    public FingerPrintRecord findInTimeRecord(HrForm additionalForm) {
        if (additionalForm == null) {
            return null;
        }

        if (additionalForm.getFromTime() == null) {
            return null;
        }

        Date startTime = additionalForm.getFromTime();
        Calendar min = Calendar.getInstance();
        min.setTime(startTime);
        min.add(Calendar.MINUTE, -30);

        Calendar max = Calendar.getInstance();
        max.setTime(startTime);
        max.add(Calendar.MINUTE, 30);

        Map m = new HashMap();
        m.put("f", min.getTime());
        m.put("t", max.getTime());
        m.put("s", additionalForm.getStaff());
        m.put("ftp", FingerPrintRecordType.Varified);
        String sql = "Select ss from FingerPrintRecord ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.fingerPrintRecordType=:ftp "
                + " and ss.recordTimeStamp between :f and :t";
        FingerPrintRecord ss = getFingerPrintRecordFacade().findFirstByJpql(sql, m, TemporalType.TIMESTAMP);
//        System.err.println("findInTimeRecordWithOutDayOffSleeping : " + ss);
        return ss;
    }

    public FingerPrintRecord findOutTimeRecord(StaffShift staffShift) {
        if (staffShift == null) {
            return null;
        }

        if (staffShift.getNextStaffShift() != null) {
            return null;
        }

        if (staffShift.getShiftEndTime() == null) {
            return null;
        }

        Date endTime = staffShift.getShiftEndTime();
        Calendar min = Calendar.getInstance();
        min.setTime(endTime);
        min.add(Calendar.HOUR, -2);

        Calendar max = Calendar.getInstance();
        max.setTime(endTime);
        max.add(Calendar.HOUR, 2);

        Map m = new HashMap();
        m.put("f", min.getTime());
        m.put("t", max.getTime());
        m.put("s", staffShift.getStaff());
        m.put("ftp", FingerPrintRecordType.Varified);
        String sql = "Select ss from FingerPrintRecord ss "
                + " where ss.retired=false "
                + " and ss.staff=:s"
                //                + " and ss.loggedRecord is not null"
                + " and ss.fingerPrintRecordType=:ftp "
                + " and ss.recordTimeStamp between :f and :t";
        FingerPrintRecord ss = getFingerPrintRecordFacade().findFirstByJpql(sql, m, TemporalType.TIMESTAMP);
//        System.err.println("findOutTimeRecordWithoutDayOffSleeping : " + ss);
        return ss;
    }

    public FingerPrintRecord findOutTimeRecord(HrForm additionalForm) {
        if (additionalForm == null) {
            return null;
        }

        if (additionalForm.getToTime() == null) {
            return null;
        }

        Date endTime = additionalForm.getToTime();
        Calendar min = Calendar.getInstance();
        min.setTime(endTime);
        min.add(Calendar.MINUTE, -30);

        Calendar max = Calendar.getInstance();
        max.setTime(endTime);
        max.add(Calendar.MINUTE, 30);

        Map m = new HashMap();
        m.put("f", min.getTime());
        m.put("t", max.getTime());
        m.put("s", additionalForm.getStaff());
        m.put("ftp", FingerPrintRecordType.Varified);
        String sql = "Select ss from FingerPrintRecord ss "
                + " where ss.retired=false "
                + " and ss.staff=:s"
                //                + " and ss.loggedRecord is not null"
                + " and ss.fingerPrintRecordType=:ftp "
                + " and ss.recordTimeStamp between :f and :t";
        FingerPrintRecord ss = getFingerPrintRecordFacade().findFirstByJpql(sql, m, TemporalType.TIMESTAMP);
//        System.err.println("findOutTimeRecordWithoutDayOffSleeping : " + ss);
        return ss;
    }

    public List<StaffShift> fetchStaffShift(Date date, Staff staff) {
        Map m = new HashMap();
        m.put("date", date);
        m.put("s", staff);
        m.put("tp", StaffShiftExtra.class);
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s"
                + " and ss.shift is not null "
                + " and ss.shiftDate=:date "
                + " and type(ss)!=:tp";

        List<StaffShift> list1 = getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);

        ///////////////////
        m = new HashMap();
        m.put("date", date);
        m.put("s", staff);
        m.put("dtp", DayType.DayOff);
        sql = " Select ss from StaffShiftExtra ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.shiftDate=:date "
                + " and ss.shift.dayType=:dtp "
                + " and ss.additionalForm.retired=false ";

        List<StaffShift> list2 = getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);

        List<StaffShift> list3 = new ArrayList<>();

        if (list1 != null) {
            list3.addAll(list1);
        }

        if (list2 != null) {
            list3.addAll(list2);
        }

        return list3;
    }

    @EJB
    StaffLeaveEntitleFacade staffLeaveEntitleFacade;

    public StaffLeaveEntitle fetchStaffLeaveEntitle(Staff staff, LeaveType leaveType, Date date) {
        String sql = " select l "
                + " from StaffLeaveEntitle l "
                + " where l.retired=false "
                + " and l.staff=:stf "
                + " and l.leaveType in :ltp "
                + " and l.fromDate<=:cu  "
                + " and l.toDate>=:cu ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("cu", date);
//        hm.put("td", toDate);

        return staffLeaveEntitleFacade.findFirstByJpql(sql, hm, TemporalType.DATE);

    }

    public Double fetchStaffLeaveEntitle(Staff staff, LeaveType leaveType, Date fromDate, Date toDate) {
        String sql = " select sum(l.count) "
                + " from StaffLeaveEntitle l "
                + " where l.retired=false "
                + " and l.staff=:stf "
                + " and l.leaveType in :ltp "
                + " and l.fromDate=:frm  "
                + " and l.toDate=:to ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        return staffLeaveEntitleFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchStaffLeave(Staff staff, LeaveType leaveType, Date fromDate, Date toDate) {
        String sql = "select sum(l.qty) "
                + " from StaffLeave l"
                + " where l.retired=false"
                + " and l.form.retired=false "
                + " and l.staff=:stf "
                + " and l.leaveType in  :ltp "
                + " and l.leaveDate between  :fd and :td ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        double utilizedLeaveQty = staffLeaveEntitleFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
        return utilizedLeaveQty;

    }

    public List<StaffLeave> fetchStaffLeaveAddedLeaveList(Staff staff, LeaveType leaveType, Date fromDate, Date toDate) {
        String sql = "select l "
                + " from StaffLeave l"
                + " where l.retired=false "
                + " and type(l)!=:cl"
                + " and l.staff=:stf "
                + " and l.leaveType in  :ltp "
                + " and l.leaveDate between  :fd and :td ";
        HashMap hm = new HashMap();
        hm.put("cl", StaffLeaveSystem.class);
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("fd", fromDate);
        hm.put("td", toDate);

        return staffLeaveFacade.findByJpql(sql, hm, TemporalType.DATE);

    }

    public double fetchStaffLeaveAddedLeave(Staff staff, LeaveType leaveType, Date fromDate, Date toDate) {
        String sql = "select sum(l.qty) "
                + " from StaffLeave l"
                + " where l.retired=false "
                + " and type(l)!=:cl"
                + " and l.staff=:stf "
                + " and l.leaveType in  :ltp "
                + " and l.leaveDate between  :fd and :td ";
        HashMap hm = new HashMap();
        hm.put("cl", StaffLeaveSystem.class);
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("fd", fromDate);
        hm.put("td", toDate);

        return staffLeaveEntitleFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

//    public double fetchStaffLeaveSystemLeave(Staff staff, LeaveType leaveType, Date fromDate, Date toDate) {
//        String sql = "select sum(l.qty) "
//                + " from StaffLeaveSystem l"
//                + " where l.retired=false "
//                //                + " and type(l)!=:cl"
//                + " and l.staff=:stf "
//                + " and l.leaveType in  :ltp "
//                + " and l.leaveDate between  :fd and :td ";
//        HashMap hm = new HashMap();
////        hm.put("l", StaffLeaveSystem.class)
//        hm.put("stf", staff);
//        hm.put("ltp", leaveType.getLeaveTypes());
//        hm.put("fd", fromDate);
//        hm.put("td", toDate);
//
//        return staffLeaveEntitleFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
//
//    }
    public double fetchStaffLeaveSystem(Staff staff, LeaveType leaveType, Date fromDate, Date toDate) {
        String sql = "select sum(l.qty) "
                + " from StaffLeaveSystem l"
                + " where l.retired=false "
                + " and l.staff=:stf "
                + " and l.leaveType in  :ltp "
                + " and l.leaveDate between  :fd and :td ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("fd", fromDate);
        hm.put("td", toDate);

        return staffLeaveEntitleFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public List<StaffLeave> fetchStaffLeaveSystemList(Staff staff, LeaveType leaveType, Date fromDate, Date toDate) {
        String sql = "select l "
                + " from StaffLeaveSystem l"
                + " where l.retired=false "
                + " and l.staff=:stf "
                + " and l.leaveType in  :ltp "
                + " and l.leaveDate between  :fd and :td ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("fd", fromDate);
        hm.put("td", toDate);

        return staffLeaveFacade.findByJpql(sql, hm, TemporalType.DATE);

    }

    public List<StaffLeave> fetchStaffLeaveSystemList(Staff staff, Date fromDate, Date toDate) {
        String sql = "select l "
                + " from StaffLeaveSystem l"
                + " where l.retired=false "
                + " and l.staff=:stf "
                //                + " and l.leaveType in  :ltp "
                + " and l.leaveDate between  :fd and :td ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
//        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("fd", fromDate);
        hm.put("td", toDate);

        return staffLeaveFacade.findByJpql(sql, hm, TemporalType.DATE);

    }

    public List<StaffShift> fetchStaffShift(Date fromDate, Date toDate, Staff staff) {
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("s", staff);
//        m.put("tp", StaffShiftExtra.class);
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.shiftDate between :fd and :td "
                //                + " and type(ss)!=:tp"
                + " order by ss.shiftDate ";

        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public List<StaffShift> fetchStaffShiftForAutoLeave(Staff staff, Date fromDate, Date toDate) {
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("s", staff);
        m.put("dtp", DayType.Normal);
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.shiftDate between :fd and :td "
                + " and ss.dayType=:dtp "
                + " and ss.leaveType is null"
                + " and (ss.lateInVarified>0 "
                + " or ss.earlyOutVarified>0)"
                + " order by ss.shiftDate ";

        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public List<StaffShift> fetchStaffShiftForAutoLeaveReset(Staff staff, Date fromDate, Date toDate) {
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("s", staff);
        m.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.SleepingDay}));
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.shiftDate between :fd and :td "
                + " and ss.dayType not in :dtp "
                //                + " and ss.autoLeave=true "
                + " and (ss.considerForLateIn=true "
                + " or ss.considerForEarlyOut=true) "
                + " order by ss.shiftDate ";


        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public LeaveType getLeaveType(Staff staff, Date date) {
        StaffLeaveEntitle staffLeaveEntitle = fetchStaffLeaveEntitle(staff, LeaveType.Annual, date);

        if (staffLeaveEntitle != null
                && staffLeaveEntitle.getCount() > fetchStaffLeave(staff, LeaveType.Annual, staffLeaveEntitle.getFromDate(), staffLeaveEntitle.getToDate())) {

            return LeaveType.AnnualHalf;
        }

        staffLeaveEntitle = fetchStaffLeaveEntitle(staff, LeaveType.Casual, date);

        if (staffLeaveEntitle != null
                && staffLeaveEntitle.getCount() > fetchStaffLeave(staff, LeaveType.Casual, staffLeaveEntitle.getFromDate(), staffLeaveEntitle.getToDate())) {
            return LeaveType.CasualHalf;
        }

        return LeaveType.No_Pay_Half;

    }

    public List<StaffShift> fetchStaffShiftAllowance(Date fromDate, Date toDate, Staff staff, List<DayType> dayTypes) {
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("s", staff);
        m.put("tp", dayTypes);
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null"
                + " and ss.shiftDate between :fd and :td "
                + " and ss.dayType in :tp"
                + " order by ss.dayType,ss.shiftDate ";

        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public List<StaffShift> fetchStaffShiftNormal(Date fromDate, Date toDate, Staff staff) {
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("s", staff);
        m.put("tp", Arrays.asList(new DayType[]{DayType.MurchantileHoliday,
            DayType.Poya,
            DayType.DayOff,
            DayType.SleepingDay}));
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null"
                + " and ss.shiftDate between :fd and :td "
                + " and ss.dayType not in :tp"
                + " order by ss.dayType,ss.shiftDate ";

        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public List<StaffShift> fetchStaffShiftExtraDuty(Date fromDate, Date toDate, Staff staff) {
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("s", staff);
//        m.put("tp", StaffShiftExtra.class);
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s"
                + " and (ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)>0 "
                + " and ss.shiftDate between :fd and :td "
                //                + " and type(ss)!=:tp"
                + " order by ss.shiftDate ";

        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public List<StaffShift> fetchStaffShiftForAddingLeave(Date fromDate, Date toDate, Staff staff) {
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("s", staff);
        m.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.SleepingDay}));
        m.put("tp", StaffShiftExtra.class);
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.staff=:s "
                + " and ss.shift.dayType not in :dtp"
                + " and ss.leaveType is null  "
                + " and ss.shiftDate between :fd and :td "
                + " and type(ss)!=:tp"
                + " order by ss.staff.codeInterger ";

        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public List<Staff> fetchStaffShift(Date fromDate, Date toDate, Roster rs) {
        Map m = new HashMap();
        m.put("frm", fromDate);
        m.put("to", toDate);
        m.put("rs", rs);
        String sql = "Select distinct(ss.staff) from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.roster=:rs "
                + " and ss.shiftDate between :frm and :to "
                + " order by ss.staff.codeInterger ";

        return getStaffFacade().findByJpql(sql, m, TemporalType.DATE);
    }

    public List<StaffShift> fetchStaffShift(Date date, Roster roster) {
        Map m = new HashMap();
        m.put("date", date);
        m.put("s", roster);
        String sql = "Select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.roster=:s "
                + " and ss.shiftDate=:date"
                + " order by ss.staff.code ";

        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
    }

//     public List<StaffShift> fetchStaffShift(Date date, Staff staff) {
//        Map m = new HashMap();
//        m.put("date", date);
//        m.put("s", staff);
//        String sql = "Select ss from StaffShift ss "
//                + " where ss.retired=false "
//                + " and ss.staff=:s "
//                + " and ss.shiftDate=:date";
//
//        return getStaffShiftFacade().findByJpql(sql, m, TemporalType.DATE);
//    }
//    
//     public List<Staff> fetchStaff(Roster roster) {
//        Map m = new HashMap();     
//        m.put("s", roster);
//        String sql = "Select ss from Staff ss "
//                + " where ss.retired=false "
//                + " and ss.roster=:s "              
//                + " order by ss.staff.codeInterger ";
//
//        return getStaffFacade().findByJpql(sql, m, TemporalType.DATE);
//    }
//    private StaffShift findOutTime(FingerPrintRecord r) {
//        Date recordedDate = r.getRecordTimeStamp();
//        Calendar min = Calendar.getInstance();
//        min.setTime(recordedDate);
//        min.add(Calendar.HOUR, -1);
//
//        Calendar max = Calendar.getInstance();
//        max.setTime(recordedDate);
//        max.add(Calendar.HOUR, 2);
//
//        Map m = new HashMap();
//        m.put("f", min.getTime());
//        m.put("t", max.getTime());
//        m.put("s", r.getStaff());
//
//        String sql = "Select ss from StaffShift ss "
//                + " where ss.retired=false "
//                + " and ss.staff=:s "
//                + " and ss.nextStaffShift is null "
//                + " and  ss.shiftEndTime between :f and :t";
//        StaffShift ss = getStaffShiftFacade().findFirstByJpql(sql, m, TemporalType.TIMESTAMP);
//        return ss;
//    }
    public StaffShift fetchStaffShift(FingerPrintRecord r) {

        return null;

        //     StaffShift s = new StaffShift();
    }

    public ShiftFacade getShiftFacade() {
        return shiftFacade;
    }

    public void setShiftFacade(ShiftFacade shiftFacade) {
        this.shiftFacade = shiftFacade;
    }

    public StaffShift fetchPrevStaffShift(StaffShift tmp) {
        if (tmp.getShift() == null) {
            return null;
        }

        String sql = "Select s from StaffShift s "
                + " where s.retired=false"
                + " and s.staff=:st "
                + " and s.shiftDate=:date "
                + " and s.shift=:preSh";
        HashMap hm = new HashMap();
        hm.put("st", tmp.getStaff());
        hm.put("date", tmp.getShiftDate());
        hm.put("preSh", tmp.getShift().getPreviousShift());

        StaffShift stf = getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
        //System.out.println("tmp.getShiftDate() = " + tmp.getShiftDate());
        if (tmp.getShift().getPreviousShift() != null) {
        } else {
        }
        if (stf != null) {
            return stf;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(tmp.getShiftDate());
        cal.add(Calendar.DATE, -1);
        Date beforDate = cal.getTime();

        sql = "Select s from StaffShift s "
                + " where s.retired=false"
                + " and s.staff=:st "
                + " and s.shiftDate=:date "
                + " and s.shift=:preSh "
                + " and s.shift.lastShift=true";
        hm = new HashMap();
        hm.put("st", tmp.getStaff());
        hm.put("date", beforDate);
        hm.put("preSh", tmp.getShift().getPreviousShift());
        if (tmp.getShift().getPreviousShift() != null) {
        } else {
        }
        return getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
    }

    public StaffShift fetchStaffShift(Date date, Shift shift, Staff staff) {
        String sql = "Select s from StaffShift s "
                + " where s.retired=false"
                + " and s.staff=:st"
                + " and s.shift=:sh "
                + " and s.shiftDate=:date ";
        HashMap hm = new HashMap();
        hm.put("date", date);
        hm.put("sh", shift);
        hm.put("st", staff);

        return getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

    }

//    public StaffShift fetchPrevDayStaffShift(StaffShift ss) {
//        System.err.println("Fetch Prev Day Shift");
//        if (ss == null) {
//            return null;
//        }
//
//        if (ss.getShiftDate() == null) {
//            return null;
//        }
//
//        if (ss.getShift() == null) {
//            return null;
//        }
//
////        if (ss.getShift().getPreviousShift() != null) {
////            return null;
////        }
//
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(ss.getShiftDate());
//        cal.add(Calendar.DATE, -1);
//        Date beforDate = cal.getTime();
//        String sql = "select ss from  "
//                + " StaffShift ss "
//                + " where ss.shiftDate=:bDate"
//                + " and  ss.staff=:st "
//                + " and  ss.shift=:sh";
//        HashMap hm = new HashMap();
//        hm.put("bDate", beforDate);
//        hm.put("st", ss.getStaff());
//
//        StaffShift tmp = getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
//
//        if (tmp == null) {
//            return null;
//        }
//
//        if (tmp.getShift() == null) {
//            return null;
//        }
//
//        Calendar fetchShiftEndingTime = Calendar.getInstance();
//
//        if (tmp.getShift().getEndingTime() == null) {
//            return null;
//        }
//
//        fetchShiftEndingTime.setTime(tmp.getShift().getEndingTime());
//
//        Calendar shiftStartTime = Calendar.getInstance();
//
//        if (ss.getShift().getStartingTime() == null) {
//            return null;
//        }
//        shiftStartTime.setTime(ss.getShift().getStartingTime());
//
//        if (shiftStartTime.get(Calendar.HOUR_OF_DAY) == fetchShiftEndingTime.get(Calendar.HOUR_OF_DAY)) {
//            return tmp;
//        }
//
//        return null;
//    }
//
//    public double getMaxShiftOrder(StaffShift ss) {
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(ss.getShiftDate());
//        cal.add(Calendar.DATE, -1);
//        Date beforDate = cal.getTime();
//        String sql = "Select s from StaffShift s "
//                + " where s.shiftDate=:bDate ";
//        HashMap hm = new HashMap();
//        hm.put("bDate", beforDate);
////        hm.put("st", ss.getStaff());
//
//        List<StaffShift> tmp = getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
//        double max = 0;
//        for (StaffShift sst : tmp) {
//            if (sst.getShift().getShiftOrder() > max) {
//                max = sst.getShift().getShiftOrder();
//            }
//        }
//        System.err.println("max " + max);
//        return max;
//
//    }
    public StaffShift fetchFrvDayStaffShift(StaffShift ss) {
        //   System.err.println("Fetch Forward Day Shift");
        if (ss == null) {
            return null;
        }
        if (ss.getShiftDate() == null) {
            return null;
        }

        if (ss.getShift() == null) {
            return null;
        }

        if (ss.getShift().getNextShift() != null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(ss.getShiftDate());
        cal.add(Calendar.DATE, 1);
        Date afterDate = cal.getTime();
        String sql = "select ss  from  StaffShift ss "
                + " where ss.shiftDate=:bDate "
                + " and ss.staff=:st"
                + " and ss.shift.previousShift is null ";
        HashMap hm = new HashMap();
        hm.put("bDate", afterDate);
        hm.put("st", ss.getStaff());

        StaffShift tmp = getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (tmp == null) {
            return null;
        }

        if (tmp.getShift() == null) {
            return null;
        }

        Calendar fetchShiftStartTime = Calendar.getInstance();

        if (tmp.getShift().getStartingTime() == null) {
            return null;
        }

        fetchShiftStartTime.setTime(tmp.getShift().getStartingTime());

        Calendar shiftEndTime = Calendar.getInstance();

        if (ss.getShift().getEndingTime() == null) {
            return null;
        }

        shiftEndTime.setTime(ss.getShift().getEndingTime());

        if (fetchShiftStartTime.get(Calendar.HOUR_OF_DAY) == shiftEndTime.get(Calendar.HOUR_OF_DAY)) {
            return tmp;
        }

        return null;

    }

    public StaffShift fetchFrwStaffShift(StaffShift tmp) {
        if (tmp == null) {
            return null;
        }

        if (tmp.getShift() == null) {
            return null;
        }

        String sql = "Select s from StaffShift s "
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.shiftDate=:date  "
                + " and s.shift=:frwSh";
        HashMap hm = new HashMap();
        hm.put("st", tmp.getStaff());
        hm.put("date", tmp.getShiftDate());
        hm.put("frwSh", tmp.getShift().getNextShift());
        StaffShift stf = getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        //System.out.println("tmp.getShiftDate() = " + tmp.getShiftDate());
        if (tmp.getShift().getNextShift() != null) {
        } else {
        }

        if (stf != null) {
            return stf;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(tmp.getShiftDate());
        cal.add(Calendar.DATE, 1);
        Date afterDate = cal.getTime();

        sql = "Select s from StaffShift s "
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.shiftDate=:date  "
                + " and s.shift=:frwSh"
                + " and s.shift.firstShift=true";
        hm = new HashMap();
        hm.put("st", tmp.getStaff());
        hm.put("date", afterDate);
        hm.put("frwSh", tmp.getShift().getNextShift());
        stf = getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (tmp.getShift().getNextShift() != null) {
        } else {
        }

        return stf;
    }

//    public List<StaffShift> fetchStaffShift(Date d, Roster roster) {
//        // List<StaffShift> tmp=new ArrayList<>();
//        String sql;
//        HashMap hm = new HashMap();
//
//        sql = "select st From StaffShift st "
//                + " where st.retired=false"
//                + " and st.shiftDate=:dt "
//                + " and st.shift.roster=:rs";
//        hm.put("dt", d);
//        hm.put("rs", roster);
//        List<StaffShift> tmp = getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
//        for (StaffShift sst : tmp) {
//            updateStaffShift(sst);
//            sst.setFingerPrintRecordList(fetchMissedFingerFrintRecord(sst));
//        }
//        return tmp;
//    }
//    public List<StaffShift> fetchStaffShift(Date d, Staff staff) {
//        // List<StaffShift> tmp=new ArrayList<>();
//        String sql;
//        HashMap hm = new HashMap();
//
//        sql = "select st From StaffShift st "
//                + " where st.retired=false"
//                + " and st.shiftDate=:dt "
//                + " and st.staff=:st";
//        hm.put("dt", d);
//        hm.put("st", staff);
//        List<StaffShift> tmp = getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
//        System.err.println("Staff Shift " + tmp);
//        return tmp;
//    }
    public List<StaffShift> fetchStaffShiftWithShift(Date d, Staff staff) {
        // List<StaffShift> tmp=new ArrayList<>();
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false"
                + " and st.shiftDate=:dt "
                + " and st.staff=:st"
                + " and st.shift is not null"
                + " order by st.shiftStartTime";
        hm.put("dt", d);
        hm.put("st", staff);
        List<StaffShift> tmp = getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
//        System.err.println("fetchStaffShiftWithShift:: " + tmp);
        return tmp;
    }

    public List<StaffShift> fetchStaffShiftWithShift(Date d, Roster roster) {
        // List<StaffShift> tmp=new ArrayList<>();
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false"
                + " and st.shiftDate=:dt "
                + " and st.roster=:rs"
                + " and st.shift is not null "
                + " order by st.staff.codeInterger";
        hm.put("dt", d);
        hm.put("rs", roster);
        List<StaffShift> tmp = getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
//        System.err.println("fetchStaffShiftWithShift:: " + tmp);
        return tmp;
    }

    public List<StaffShift> fetchStaffShiftWithShiftAdditional(Date d, Roster roster) {
        // List<StaffShift> tmp=new ArrayList<>();
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                + " and st.additionalForm is not null"
                + " and st.shiftDate=:dt "
                + " and st.roster=:rs"
                + " and st.shift is not null"
                + " order by st.staff.codeInterger";
        hm.put("dt", d);
        hm.put("rs", roster);
        List<StaffShift> tmp = getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
//        System.err.println("fetchStaffShiftWithShift:: " + tmp);
        return tmp;
    }

    public List<StaffShift> fetchStaffShiftWithShiftAdditional(Date d, Staff staff) {
        // List<StaffShift> tmp=new ArrayList<>();
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                + " and st.additionalForm is not null"
                + " and st.shiftDate=:dt "
                + " and st.staff=:rs"
                + " and st.shift is not null"
                + " order by st.staff.codeInterger";
        hm.put("dt", d);
        hm.put("rs", staff);
        List<StaffShift> tmp = getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
//        System.err.println("fetchStaffShiftWithShift:: " + tmp);
        return tmp;
    }

    public StaffShift calPrevStaffShift(StaffShift tmp) {
        //  System.err.println("Cal Prev Shift");

        if (tmp == null) {
            return null;
        }

        if (tmp.getShift() == null) {
            return null;
        }

        return fetchPrevStaffShift(tmp);

    }

//       private StaffShift calPrevStaffShift(StaffShift tmp) {
//
//        StaffShift preShift = fetchPrevStaffShift(tmp);
//        
//        
//        
//        
//        StaffShift preDayStaffShift = fetchPrevDayStaffShift(tmp);
//
//        if (preDayStaffShift != null) {
//            preDayStaffShift.setContinuedTo(tmp);
//            getStaffShiftFacade().edit(preDayStaffShift);
//        }
//
//        if (preShift != null) {
//            System.err.println("This Staff continue From : " + preShift.getShift().getName());
//            return preShift;
//        } else if (preDayStaffShift != null && tmp.getShift().getShiftOrder() == 0) {
//            System.err.println("This Staff continue From : " + preDayStaffShift.getShift().getName());
//            return preDayStaffShift;
//        } else {
//            return null;
//        }
//    }
    public StaffShift calFrwStaffShift(StaffShift tmp) {
        //System.err.println("Cal Forward Staff Shift");
        if (tmp == null) {
            return null;
        }

        if (tmp.getShift() == null) {
            return null;
        }

        return fetchFrwStaffShift(tmp);

    }

//      public StaffShift calFrwStaffShift(StaffShift tmp) {
//        if (tmp == null) {
//            return null;
//        }
//
//        StaffShift frwStaffShift = fetchFrwStaffShift(tmp);
//        StaffShift frwDayStaffShift = fetchFrvDayStaffShift(tmp);
//
//        if (frwDayStaffShift != null) {
//            frwDayStaffShift.setContinuedFrom(tmp);
//            getStaffShiftFacade().edit(frwDayStaffShift);
//        }
//
//        if (frwStaffShift != null) {
//            JsfUtil.addSuccessMessage("This Staff continue to : " + frwStaffShift.getShift().getName());
//            return frwStaffShift;
//        } else if (frwDayStaffShift != null && tmp.getShift().getShiftOrder() == getMaxShiftOrder(tmp)) {
//            System.err.println("This Staff continue to : " + frwDayStaffShift.getShift().getName());
//            return frwDayStaffShift;
//        } else {
//            return null;
//        }
//    }
    public void updateStaffShift(StaffShift tmp) {
        StaffShift prevStaffShift = calPrevStaffShift(tmp);
        if (prevStaffShift != null) {
            tmp.setPreviousStaffShift(prevStaffShift);
        } else {
            tmp.setPreviousStaffShift(null);
        }

        StaffShift frwStaffShift = calFrwStaffShift(tmp);
        if (frwStaffShift != null) {
            tmp.setNextStaffShift(frwStaffShift);
        } else {
            tmp.setNextStaffShift(null);
        }

        if (tmp.getStartRecord().getId() == null) {
            getFingerPrintRecordFacade().create(tmp.getStartRecord());
        } else {
            getFingerPrintRecordFacade().edit(tmp.getStartRecord());
        }

        if (tmp.getEndRecord().getId() == null) {
            getFingerPrintRecordFacade().create(tmp.getEndRecord());
        } else {
            getFingerPrintRecordFacade().edit(tmp.getEndRecord());
        }

        getStaffShiftFacade().edit(tmp);
    }

    public List<Shift> fetchShift(Roster roster) {
        String sql = "Select s From Shift s "
                + " Where s.retired=false "
                + " and s.roster=:rs ";
        //    + "  order by s.shiftOrder  ";
        HashMap hm = new HashMap();
        hm.put("rs", roster);

        return getShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public List<Staff> fetchStaff(Roster roster) {
        String sql = "Select s From Staff s "
                + " Where s.retired=false "
                + " and s.roster=:rs "
                //                + " order by s.person.name  ";
                + " order by s.codeInterger ";
        HashMap hm = new HashMap();
        hm.put("rs", roster);
        List<Staff> list = getStaffFacade().findByJpql(sql, hm);

        return list;
    }

    public List<Staff> fetchStaffFromShift(Date date) {
        Map m = new HashMap();
        m.put("date", date);
        String sql = "Select ss.staff from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.shiftDate=:date"
                + " order by ss.staff.code ";

        return staffFacade.findByJpql(sql, m, TemporalType.DATE);

    }

    public List<Staff> fetchStaffFromShift(Date fromDate, Date toDate) {
        Map m = new HashMap();
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);
        String sql = "Select ss.staff from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.shiftDate between :fromDate and :toDate"
                + " order by ss.staff.code ";

        return staffFacade.findByJpql(sql, m, TemporalType.DATE);

    }

    public List<FingerPrintRecord> fetchMissedFingerFrintRecord(StaffShift sst) {
        //   System.err.println("Date : " + sst.getShiftDate());
        List<FingerPrintRecord> tmp;
        HashMap hm = new HashMap();

        Calendar frm = Calendar.getInstance();
        Calendar to = Calendar.getInstance();

        frm.setTime(commonFunctions.getStartOfDay(sst.getShiftDate()));
        frm.add(Calendar.DATE, -1);

        to.setTime(commonFunctions.getEndOfDay(sst.getShiftDate()));
        to.add(Calendar.DATE, 1);

        ////////////////
        String sql = "Select fpr from"
                + " FingerPrintRecord fpr "
                + " where fpr.fingerPrintRecordType=:tp "
                + " and fpr.retired=false"
                + " and  fpr.staff=:st"
                + " and fpr.recordTimeStamp between :fd and :td "
                + " and fpr.loggedRecord is not null ";
//                + " and fpr.staffShift is null ";
        hm.put("tp", FingerPrintRecordType.Varified);
        hm.put("st", sst.getStaff());
        hm.put("fd", frm.getTime());
        hm.put("td", to.getTime());
        tmp = getFingerPrintRecordFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
//        System.err.println("fetchMissedFingerFrintRecord:: " + tmp);

        return tmp;
    }

    public Staff fetchStaff(String code) {
        Map m = new HashMap();
        m.put("d", code);
        String sql = "select s from Staff s "
                + " where s.retired=false"
                + " and s.code=:d";
        return getStaffFacade().findFirstByJpql(sql, m);
    }

    public FingerPrintRecord fetchFingerPrintRecord(Staff staff, Date timeStamp, FingerPrintRecordType fingerPrintRecordType) {
        HashMap m = new HashMap();
        String jpql = "select r from FingerPrintRecord r "
                + " where r.retired=false "
                + " and r.fingerPrintRecordType=:p "
                + " and r.staff=:s "
                + " and r.recordTimeStamp=:t";
        m.put("s", staff);
        m.put("t", timeStamp);
        m.put("p", fingerPrintRecordType);
        FingerPrintRecord fpr = getFingerPrintRecordFacade().findFirstByJpql(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Logged Fetched " + fpr);
        return fpr;
    }

    public FingerPrintRecord fetchFingerPrintRecord(Staff staff, Date timeStamp, FingerPrintRecordType fingerPrintRecordType, FingerPrintRecord fingerPrintRecordLogged) {
        HashMap m = new HashMap();
        String jpql = "select r from FingerPrintRecord r "
                + " where r.retired=false "
                + " and r.fingerPrintRecordType=:p "
                + " and r.staff=:s "
                + " and r.recordTimeStamp=:t"
                + " and r.loggedRecord=:lfr";

        m.put("s", staff);
        m.put("t", timeStamp);
        m.put("p", fingerPrintRecordType);
        m.put("lfr", fingerPrintRecordLogged);
        FingerPrintRecord fpr = getFingerPrintRecordFacade().findFirstByJpql(jpql, m, TemporalType.TIMESTAMP);
//        System.err.println("Fetched Varified " + fpr);
        return fpr;
    }

    public Shift fetchShift(Staff staff, Date timeStamp) {
        HashMap m = new HashMap();
        String jpql = "select r.shift "
                + " from StaffShift r "
                + " where r.retired=false "
                + " and r.shiftDate=:dt "
                + " and r.staff=:s ";
        //  m = new HashMap();
        m.put("s", staff);
        m.put("dt", timeStamp);
        Shift fpr = getShiftFacade().findFirstByJpql(jpql, m, TemporalType.DATE);

        //  System.err.println("Fetched Shift " + fpr);
        return fpr;
    }

    public FingerPrintRecord createFingerPrintRecordLogged(Staff staff, Date timeStamp, WebUser webUser) {
        FingerPrintRecord fingerPrintRecord;
        fingerPrintRecord = fetchFingerPrintRecord(staff, timeStamp, FingerPrintRecordType.Logged);

        if (fingerPrintRecord == null) {
            fingerPrintRecord = new FingerPrintRecord();
            fingerPrintRecord.setCreatedAt(Calendar.getInstance().getTime());
            fingerPrintRecord.setCreater(webUser);
            fingerPrintRecord.setFingerPrintRecordType(FingerPrintRecordType.Logged);
            fingerPrintRecord.setStaff(staff);
            fingerPrintRecord.setRoster(staff.getRoster());
            fingerPrintRecord.setRecordTimeStamp(timeStamp);
            getFingerPrintRecordFacade().create(fingerPrintRecord);
        }

        return fingerPrintRecord;
    }

    public FingerPrintRecord createFingerPrintRecordVarified(WebUser webUser, FingerPrintRecord loggedFingerPrintRecord) {
        if (loggedFingerPrintRecord == null) {
            return null;
        }

        FingerPrintRecord fingerPrintRecord;
        fingerPrintRecord = fetchFingerPrintRecord(loggedFingerPrintRecord.getStaff(), loggedFingerPrintRecord.getRecordTimeStamp(), FingerPrintRecordType.Varified, loggedFingerPrintRecord);

        if (fingerPrintRecord == null) {
            fingerPrintRecord = new FingerPrintRecord();
            fingerPrintRecord.setCreatedAt(Calendar.getInstance().getTime());
            fingerPrintRecord.setCreater(webUser);
            fingerPrintRecord.setFingerPrintRecordType(FingerPrintRecordType.Varified);
            fingerPrintRecord.setStaff(loggedFingerPrintRecord.getStaff());
            fingerPrintRecord.setRoster(loggedFingerPrintRecord.getRoster());
            fingerPrintRecord.setRecordTimeStamp(loggedFingerPrintRecord.getRecordTimeStamp());
            fingerPrintRecord.setLoggedRecord(loggedFingerPrintRecord);
            getFingerPrintRecordFacade().create(fingerPrintRecord);
        }

        return fingerPrintRecord;
    }

    public List<StaffShift> fetchPreviousDayShift(StaffShift sst) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(sst.getShiftDate());
        cal.add(Calendar.DATE, -1);
        Date pre = cal.getTime();

        String sql = "Select ss From StaffShift ss "
                + " where ss.shiftDate=:date "
                + " and ss.shift=:ds ";
        HashMap hm = new HashMap();
        hm.put("date", pre);
        hm.put("ds", sst.getShift());

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);

    }

    public List<StaffLeave> getStaffLeave(Staff staff, Date frmDate, Date toDate) {

        String sql = "Select s From StaffLeave s"
                + " where s.retired=false "
                + " and s.staff=:st"
                + " and (s.leaveDate between :frm and :to )";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("frm", frmDate);
        hm.put("to", toDate);

        return getStaffLeaveFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public List<StaffLeave> getStaffLeave(Staff staff, LeaveType leaveType, Date frmDate, Date toDate) {
        List<LeaveType> list = leaveType.getLeaveTypes();
        String sql = "Select s From StaffLeave s"
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.leaveType in :ltp"
                + " and (s.leaveDate between :frm and :to)";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("ltp", list);
        hm.put("frm", frmDate);
        hm.put("to", toDate);

        return getStaffLeaveFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public double calStaffLeave(Staff staff, LeaveType leaveType, Date frmDate, Date toDate) {
        List<LeaveType> list = leaveType.getLeaveTypes();
        String sql = "Select sum(s.qty) From StaffLeave s"
                + " where s.retired=false "
                + " and s.staff=:st "
                //                + " and s.leaveType in :ltp"
                + " and s.leaveType in :ltp "
                + " and (s.leaveDate between :frm and :to)";
        HashMap hm = new HashMap();
        hm.put("st", staff);
//        hm.put("ltp", list);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("frm", frmDate);
        hm.put("to", toDate);

        return getStaffLeaveFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public double calStaffLeaveMaternity(Staff staff, Date frmDate, Date toDate) {
        double d = 0.0;
        String sql = "Select sum(s.qty) From StaffLeave s"
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.leaveType in :ltp "
                + " and (s.leaveDate between :frm and :to)";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("ltp", Arrays.asList(new LeaveType[]{LeaveType.Maternity1st, LeaveType.Maternity2nd, LeaveType.Maternity1stHalf, LeaveType.Maternity2ndHalf}));
        hm.put("frm", frmDate);
        hm.put("to", toDate);
//        //System.out.println("hm = " + hm);
//        List<StaffLeave> leaves=getStaffLeaveFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
//        
//        //System.out.println("leaves.size() = " + leaves.size());
//        
//        for (StaffLeave l : leaves) {
//            //System.out.println("l.getLeaveDate() = " + l.getLeaveDate());
//            //System.out.println("l.getForm().getFromTime() = " + l.getForm().getFromTime());
//            //System.out.println("l.getForm().getToTime() = " + l.getForm().getToTime());
//            
//        }
//        
//        
//        return d;

        return getStaffLeaveFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public double calStaffLeave(Staff staff, LeaveType leaveType, Date frmDate) {

        String sql = "Select sum(s.qty) From StaffLeave s"
                + " where s.retired=false "
                + " and s.staff=:st "
                //                + " and s.leaveType in :ltp"
                + " and s.leaveType =:ltp "
                + " and s.leaveDate <=:frm ";
        HashMap hm = new HashMap();
        hm.put("st", staff);
//        hm.put("ltp", list);
        hm.put("ltp", leaveType);
        hm.put("frm", frmDate);

        return getStaffLeaveFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public double calStaffLeave(Staff staff, LeaveType leaveType) {
        List<LeaveType> list = leaveType.getLeaveTypes();
        String sql = "Select sum(s.qty) From StaffLeave s"
                + " where s.retired=false "
                + " and s.staff=:st "
                //                + " and s.leaveType in :ltp"
                + " and s.leaveType in :ltp "
                + " and (s.leaveDate between :frm and :to)";
        HashMap hm = new HashMap();
        hm.put("st", staff);
//        hm.put("ltp", list);
        hm.put("ltp", leaveType);

        return getStaffLeaveFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public double calStaffLeaveSystem(Staff staff, LeaveType leaveType, Date frmDate, Date toDate) {
        List<LeaveType> list = leaveType.getLeaveTypes();
        String sql = "Select sum(s.qty) From StaffLeaveSystem s"
                + " where s.retired=false "
                + " and s.staff=:st "
                //                + " and s.leaveType in :ltp"
                + " and s.leaveType in :ltp "
                + " and (s.leaveDate between :frm and :to)";
        HashMap hm = new HashMap();
        hm.put("st", staff);
//        hm.put("ltp", list);
        hm.put("ltp", leaveType.getLeaveTypes());
        hm.put("frm", frmDate);
        hm.put("to", toDate);

        return getStaffLeaveFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public StaffLeave fetchFirstStaffLeave(Staff staff, Date date) {

        String sql = "Select s From StaffLeave s"
                + " where s.retired=false "
                + " and type(s)!=:tp "
                //                + " and s.retired=false "
                + " and s.staff=:st"
                + " and s.leaveDate=:date";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("date", date);
        hm.put("tp", StaffLeaveSystem.class);

        return getStaffLeaveFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
    }

    public List<StaffLeave> fetchStaffLeave(Staff staff, Date date) {
        String sql = "Select s From StaffLeave s"
                + " where s.retired=false "
                //                + " and s.form.retired=false "
                //                + " and s.retired=false "
                + " and s.staff=:st "
                + " and type(s) !=:lfs "
                + " and s.leaveDate=:date";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("date", date);
        hm.put("lfs", StaffLeaveSystem.class);

        List<StaffLeave> sls = getStaffLeaveFacade().findByJpql(sql, hm, TemporalType.DATE);
        for (StaffLeave sl : sls) {
        }
        return sls;
    }

    public List<StaffLeave> fetchStaffLeaveSystem(Staff staff, Date date) {
        String sql = "Select s From StaffLeave s"
                + " where s.retired=false "
                //                + " and s.form.retired=false "
                //                + " and s.retired=false "
                + " and s.staff=:st "
                + " and type(s) =:lfs "
                + " and s.leaveDate=:date";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("date", date);
        hm.put("lfs", StaffLeaveSystem.class);

        List<StaffLeave> sls = getStaffLeaveFacade().findByJpql(sql, hm, TemporalType.DATE);
        for (StaffLeave sl : sls) {
        }
        return sls;
    }

    public boolean isHoliday(Date d) {
        String sql = "Select d From PhDate d "
                + " Where d.retired=false"
                + " and d.phDate=:dtd";
        HashMap hm = new HashMap();
        hm.put("dtd", d);
        List<PhDate> tmp = getPhDateFacade().findByJpql(sql, hm, TemporalType.DATE);

        return !tmp.isEmpty();
    }

    public DayType isHolidayWithDayType(Date d) {
        String sql = "Select d.phType From PhDate d "
                + " Where d.retired=false"
                + " and d.phDate=:dtd";
        HashMap hm = new HashMap();
        hm.put("dtd", d);
        Object obj = getPhDateFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (obj == null) {
            return null;
        } else {
            return (DayType) obj;
        }

    }

    public double calWorkedDuraion(StaffShift ss) {
        long endTime = 0l;
        long startTime = 0l;
        long result;

        if (ss.getEndRecord() != null && ss.getEndRecord().getRecordTimeStamp() != null) {
            endTime = ss.getEndRecord().getRecordTimeStamp().getTime();
        }

        if (ss.getStartRecord() != null && ss.getStartRecord().getRecordTimeStamp() != null) {
            startTime = ss.getStartRecord().getRecordTimeStamp().getTime();
        }

        result = endTime - startTime;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(result);

        return cal.get(Calendar.MINUTE);
    }

//    public StaffMonthDay calculateDayType(Staff staff, Date frm, Date to) {
//
//        StaffMonthDay tmp = new StaffMonthDay();
//        tmp.setStaff(staff);
//        for (StaffShift ds : getStaffShift(staff, frm, to)) {
//            tmp.setRosteredTime(tmp.getRosteredTime() + (ds.getShift().getDurationHour() * 60));
//            //  tmp.setWorkedTime(tmp.getWorkedTime() + calWorkedDuraion(ds));
//            switch (getDayType(ds.getShiftDate())) {
//                case Weekday:
//                    tmp.setWeekdays(tmp.getWeekdays() + 1);
//                    break;
//                case Saturday:
//                    tmp.setSaturday(tmp.getSaturday() + 1);
//                    break;
//                case Sunday:
//                    tmp.setSunday(tmp.getSunday() + 1);
//                    break;
//                case Holiday:
//                    tmp.setHoliday(tmp.getHoliday() + 1);
//                    break;
//            }
//        }
//
//        return tmp;
//
//    }
//    public boolean checkDateWithDayType(Date date, Shift ds) {
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(date);
//        int dayValue = cal.get(Calendar.DAY_OF_WEEK);
//
//        if (isHoliday(date) && ds.getDayType() == DayType.Holiday) {
//            return true;
//        }
//
//        if (!isHoliday(date) && (dayValue == 2 || dayValue == 3 || dayValue == 4 || dayValue == 5 || dayValue == 6) && ds.getDayType() == DayType.Weekday) {
//            return true;
//        }
//
//        if (!isHoliday(date) && dayValue == 7 && ds.getDayType() == DayType.Saturday) {
//            return true;
//        }
//
//        if (!isHoliday(date) && dayValue == 1 && ds.getDayType() == DayType.Sunday) {
//            return true;
//        }
//
//        if (ds.getDayType() == DayType.All) {
//            return true;
//        }
//
//        return false;
//    }
//    public DayType getDayType(Date date) {
//        Calendar cal = Calendar.getInstance();
//        cal.setTime(date);
//        int dayValue = cal.get(Calendar.DAY_OF_WEEK);
//
//        if (isHoliday(date)) {
//            return DayType.Holiday;
//        }
//
//        if (!isHoliday(date) && (dayValue == 2 || dayValue == 3 || dayValue == 4 || dayValue == 5 || dayValue == 6)) {
//            return DayType.Weekday;
//        }
//
//        if (!isHoliday(date) && dayValue == 7) {
//            return DayType.Saturday;
//        }
//
//        if (!isHoliday(date) && dayValue == 1) {
//            return DayType.Sunday;
//        }
//
//        return DayType.All;
//    }
    public List<StaffShift> getStaffShift(Staff staff, Date frm, Date to) {
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st"
                + " where st.retired=false"
                + " and st.shiftDate between :frmD and :toD "
                + " and st.staff=:staf";
        hm.put("frmD", frm);
        hm.put("toD", to);
        hm.put("staf", staff);

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public List<StaffShift> getStaffShiftFromRecordForSlaryCalculation(Date frm, Date to, Staff staff) {
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                //                + " and st.consideredForSalary!=true "
                + " and st.startRecord.recordTimeStamp between :frmD and :toD "
                + " and st.staff=:staf";
        hm.put("frmD", frm);
        hm.put("toD", to);
        hm.put("staf", staff);

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<StaffShift> getStaffShiftFromRecordForExraDutyCalculation(Date frm, Date to, Staff staff) {
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                //                + " and st.consideredForExtraDuty!=true "
                + " and st.startRecord.recordTimeStamp between :frmD and :toD "
                + " and st.staff=:staf";
        hm.put("frmD", frm);
        hm.put("toD", to);
        hm.put("staf", staff);

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<StaffShift> getStaffShiftFromRecordSlaryCalculated(Date frm, Date to, Staff staff) {
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                //                + " and st.consideredForSalary=true "
                + " and st.startRecord.recordTimeStamp between :frmD and :toD "
                + " and st.staff=:staf";
        hm.put("frmD", frm);
        hm.put("toD", to);
        hm.put("staf", staff);

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<StaffShift> getStaffShiftFromRecordForOtCalculation(Date frm, Date to, Staff staff) {
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                //                + " and  st.consideredForOt!=true "
                + " and st.startRecord.recordTimeStamp between :frmD and :toD "
                + " and st.staff=:staf";
        hm.put("frmD", frm);
        hm.put("toD", to);
        hm.put("staf", staff);

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<StaffShift> getStaffShiftFromRecordOtCalculated(Date frm, Date to, Staff staff) {
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                //                + " and  st.consideredForOt=true "
                + " and st.startRecord.recordTimeStamp between :frmD and :toD "
                + " and st.staff=:staf";
        hm.put("frmD", frm);
        hm.put("toD", to);
        hm.put("staf", staff);

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<StaffShift> getStaffShiftFromRecordExtraDutyCalculated(Date frm, Date to, Staff staff) {
        String sql;
        HashMap hm = new HashMap();

        sql = "select st From StaffShift st "
                + " where st.retired=false "
                //                + " and  st.consideredForExtraDuty=true "
                + " and st.startRecord.recordTimeStamp between :frmD and :toD "
                + " and st.staff=:staf";
        hm.put("frmD", frm);
        hm.put("toD", to);
        hm.put("staf", staff);

        return getStaffShiftFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<Staff> getStaffForSalary() {
        String temSql;
        List<Staff> tmp;
        temSql = "SELECT i FROM Staff i "
                + " where i.retired=false "
                + " and i.person is not null "
                + " and i.person.name is not null "
                + " order by i.person.name";
        tmp = getStaffFacade().findByJpql(temSql);
        return tmp;
    }

    public void setEpf(StaffSalaryComponant ssc, double epf, double epfCompany) {
        if (ssc.getStaffPaysheetComponent().getPaysheetComponent().isIncludedForEpf()) {
            double tmp = ssc.getComponantValue() * (epf / 100.0);
            ssc.setEpfValue(tmp);

            tmp = ssc.getComponantValue() * (epfCompany / 100.0);
            ssc.setEpfCompanyValue(tmp);

        }
    }

    public void setEtf(StaffSalaryComponant ssc, double etf, double etfCompany) {
        if (ssc.getStaffPaysheetComponent().getPaysheetComponent().isIncludedForEpf()) {
            double tmp = ssc.getComponantValue() * (etf / 100.0);
            ssc.setEtfValue(tmp);

            tmp = ssc.getComponantValue() * (etfCompany / 100.0);
            ssc.setEtfCompanyValue(tmp);

        }
    }

    public boolean checkExistingSalary(Date fromDate, Date toDate, Staff staff) {
        String sql = "Select s From StaffSalary s "
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.salaryCycle.salaryFromDate=:fd "
                + " and s.salaryCycle.salaryToDate=:td";

        HashMap hm = new HashMap<>();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("st", staff);

        List<StaffSalary> tmp = getStaffSalaryFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (!tmp.isEmpty()) {
            updateItems(tmp, staff);
            return true;
        }

        return false;
    }

    private void updateItems(List<StaffSalary> items, Staff staff) {
        for (StaffSalary st : items) {
            if (st.getStaff().equals(staff.getId())) {
                st.setExist(true);
            }
        }

    }

    public List<StaffPaysheetComponent> fetchStaffPaysheetComponent(Staff staff, Date date) {

        String sql = " Select s From StaffPaysheetComponent s "
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.paysheetComponent.componentType in :bs1 "
                + " and s.fromDate<=:cu  "
                + " and s.toDate>=:cu ";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("cu", date);
        hm.put("bs1", PaysheetComponentType.addition.getUserDefinedComponents());
        return getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public List<StaffPaysheetComponent> fetchStaffPaysheetComponent(Staff staff, Date date, List<PaysheetComponentType> list) {

        String sql = " Select s From StaffPaysheetComponent s "
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.paysheetComponent.componentType in :bs1 "
                + " and s.fromDate<=:cu  "
                + " and s.toDate>=:cu "
                + " order by s.paysheetComponent.orderNo ";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("cu", date);
        hm.put("bs1", list);
        return getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public StaffPaysheetComponent fetchStaffPaysheetComponent(Staff staff, Date date, PaysheetComponentType paysheetComponentType) {

        String sql = " Select s From StaffPaysheetComponent s "
                + " where s.retired=false "
                + " and s.staff=:st "
                + " and s.paysheetComponent.componentType= :bs1 "
                + " and s.fromDate<=:cu  "
                + " and s.toDate>=:cu ";
        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("cu", date);
        hm.put("bs1", paysheetComponentType);
        return getStaffPaysheetComponentFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
    }

//    public double calValueForOverTime(Staff staff, Date date) {
//        double value = 0;
//        StaffPaysheetComponent staffPaysheetComponent = getBasic(staff, date);
//
//        if (staffPaysheetComponent != null) {
//            value = staffPaysheetComponent.getStaffPaySheetComponentValue();
//        }
//
//        PaysheetComponentType[] paysheetComponentTypes = {PaysheetComponentType.BasicSalary,
//            PaysheetComponentType.OT,
//            PaysheetComponentType.ExtraDuty,
//            PaysheetComponentType.No_Pay_Deduction};
//
//        String sql = " Select sum(s.staffPaySheetComponentValue)"
//                + "  From StaffPaysheetComponent s "
//                + " where s.retired=false "
//                + " and s.staff=:st"
//                + " and s.paysheetComponent.componentType not in :bs1 "
//                + " and s.fromDate<=:cu  "
//                + " and s.toDate>=:cu ";
//        HashMap hm = new HashMap();
//        hm.put("st", staff);
//        hm.put("cu", date);
//        hm.put("bs1", Arrays.asList(paysheetComponentTypes));
//        return value + getStaffPaysheetComponentFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);
//    }
    @EJB
    StaffSalaryComponantFacade staffSalaryComponantFacade;

    public StaffSalaryComponantFacade getStaffSalaryComponantFacade() {
        return staffSalaryComponantFacade;
    }

    public void setStaffSalaryComponantFacade(StaffSalaryComponantFacade staffSalaryComponantFacade) {
        this.staffSalaryComponantFacade = staffSalaryComponantFacade;
    }

    public List<StaffSalaryComponant> fetchStaffSalaryComponent(StaffSalary staffSalary) {
        String sql = "Select s From StaffSalaryComponant s "
                + " where s.retired=false "
                + " and s.staffSalary=:st ";
        HashMap hm = new HashMap();
        hm.put("st", staffSalary);
        return getStaffSalaryComponantFacade().findByJpql(sql, hm);
    }

    public List<StaffShift> fetchStaffShifts(StaffSalary staffSalary) {
        String sql = "Select s From StaffShift s "
                + " where s.retired=false "
                + " and s.staffSalary=:st ";
        HashMap hm = new HashMap();
        hm.put("st", staffSalary);
        return getStaffShiftFacade().findByJpql(sql, hm);
    }

    public StaffPaysheetComponent getBasic(Staff staff, Date date) {
        //System.err.println("Getting Basic " + staff.getStaffEmployment());

        String sql;
        HashMap hm;
        StaffPaysheetComponent tmp;

        sql = "Select s From StaffPaysheetComponent s"
                + " where s.retired=false "
                + " and  s.staff=:stf "
                + " and s.paysheetComponent.componentType=:type "
                + " and s.fromDate<=:cu  "
                + " and s.toDate>=:cu "
                + " order by s.createdAt desc ";

        hm = new HashMap();
        hm.put("stf", staff);
        hm.put("type", PaysheetComponentType.BasicSalary);
        hm.put("cu", date);
        tmp = getStaffPaysheetComponentFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null) {
        }

//        if (tmp == null) {
//            sql = "Select s From StaffPaysheetComponent s "
//                    + " where s.retired=false and "
//                    + " s.staff=:stf "
//                    + " and s.paysheetComponent.componentType=:type"
//                    + " and s.fromDate<=:cu "
//                    + " and s.toDate is null ";
//
//            hm = new HashMap();
//            hm.put("stf", staff);
//            hm.put("type", PaysheetComponentType.BasicSalary);
//            hm.put("cu", date);
//            tmp = getStaffPaysheetComponentFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
//
//        }
        return tmp;

    }

    public double getBasicValue(Staff staff, Date date) {
        //System.err.println("Getting Basic " + staff.getStaffEmployment());

        String sql;
        HashMap hm;
        StaffPaysheetComponent tmp;

        sql = "Select s From StaffPaysheetComponent s"
                + " where s.retired=false "
                + " and  s.staff=:stf "
                + " and s.paysheetComponent.componentType=:type "
                + " and s.fromDate<=:cu  "
                + " and s.toDate>=:cu ";

        hm = new HashMap();
        hm.put("stf", staff);
        hm.put("type", PaysheetComponentType.BasicSalary);
        hm.put("cu", date);
        tmp = getStaffPaysheetComponentFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (tmp != null) {
            return tmp.getStaffPaySheetComponentValue();
        } else {
            return 0;
        }
    }

    public double getOverTimeValue(Staff staff, Date date) {
        //System.err.println("Getting Basic " + staff.getStaffEmployment());
        if (staff == null || date == null) {
            return 0;
        }

        String sql;
        HashMap hm;

        sql = "Select s "
                + " from StaffPaysheetComponent s"
                + " where s.retired=false "
                + " and  s.staff=:stf "
                + " and s.paysheetComponent.includedForOt=true"
                + " and s.fromDate<=:cu  "
                + " and s.toDate>=:cu ";

        hm = new HashMap();
        hm.put("stf", staff);
        hm.put("cu", date);
        List<StaffPaysheetComponent> list = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

//        System.err.println("LIST " + list);
        if (list == null) {
            return 0;
        }
        double dbl = 0;
        for (StaffPaysheetComponent s : list) {
            dbl += s.getStaffPaySheetComponentValue();
        }
        return dbl;
    }

    public StaffPaysheetComponent getComponent(Staff staff, WebUser user, PaysheetComponentType paysheetComponentType) {
        String sql;
        HashMap hm;
        StaffPaysheetComponent tmp;

        sql = "Select s From StaffPaysheetComponent s "
                + " where s.retired=false "
                + " and s.staff=:stf "
                + " and s.paysheetComponent.componentType=:type ";

        hm = new HashMap();
        hm.put("stf", staff);
        hm.put("type", paysheetComponentType);
        tmp = getStaffPaysheetComponentFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (tmp == null) {

            tmp = new StaffPaysheetComponent();
            tmp.setCreatedAt(new Date());
            tmp.setCreater(user);
            tmp.setPaysheetComponent(getComponent(user, paysheetComponentType));
            tmp.setStaff(staff);
            getStaffPaysheetComponentFacade().create(tmp);
        }

        return tmp;

    }

//    public double calOt(Date date) {
//        DateRange dateRange = commonFunctions.getDateRangeForOT(date);
//        //System.err.println("From : " + dateRange.getFromDate());
//        //System.err.println("To : " + dateRange.getToDate());
//
//        return 0.0;
//    }
    private StaffShift fetchExistingStaffShift(Date date) {
        String sql = "select s from StaffShift s "
                + " where s.retired=false "
                //                + " and s.consideredForOt=false "
                + " and s.shiftStartTime=:d";

        HashMap hm = new HashMap();
        hm.put("d", date);

        StaffShift ss = getStaffShiftFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);

        return ss;
    }

    private StaffShift createNewStaffShift(StaffShift ss, Calendar date) {
        Date tmp = commonFunctions.getEndOfDay(date.getTime());
        Calendar time = Calendar.getInstance();
        time.setTime(tmp);
        time.add(Calendar.MILLISECOND, 1);
        //System.err.println("Start Time : " + time.getTime());

        StaffShift fetched = fetchExistingStaffShift(time.getTime());
        if (fetched != null) {
            return fetched;
        }

        StaffShift stf = new StaffShift();
        stf.copy(ss);
        stf.setShiftDate(date.getTime());
        stf.setCreatedAt(new Date());
        stf.setCreater(ss.getCreater());

        //   stf.setShiftStartTime(time.getTime());
        stf.setStartRecord(createFingerPrint(stf));

        getStaffShiftFacade().create(stf);

        return stf;
    }

    private FingerPrintRecord createFingerPrint(StaffShift ss) {
        FingerPrintRecord fpr = new FingerPrintRecord();
        fpr.setCreatedAt(new Date());
        fpr.setFingerPrintRecordType(FingerPrintRecordType.Varified);
        fpr.setStaff(ss.getStaff());
        //fpr.setRecordTimeStamp(ss.getShiftStartTime());
        fpr.setRecordTimeStamp(ss.getShift().getStartingTime());
        fpr.setStaffShift(ss);
        fpr.setTimes(Times.inTime);
        getFingerPrintRecordFacade().create(fpr);
        return fpr;
    }

    public List<StaffShift> getOrCreateRecordsForOt(Date fromDate, Date toDate, Staff staff) {
        List<StaffShift> staffShifts = getStaffShiftFromRecordForOtCalculation(fromDate, toDate, staff);
//        System.err.println("OT SHIFT  " + staffShifts);

        Calendar lastDate = Calendar.getInstance();
        lastDate.setTime(toDate);

        loopThroughStaffShift(staffShifts, lastDate, toDate);

        return staffShifts;
    }

    public List<StaffShift> getOrCreateRecordsForSalary(Date fromDate, Date toDate, Staff staff) {
        List<StaffShift> staffShifts = getStaffShiftFromRecordForSlaryCalculation(fromDate, toDate, staff);
//        System.err.println("SALARY SHIFT  " + staffShifts);
        Calendar lastDate = Calendar.getInstance();
        lastDate.setTime(toDate);

        loopThroughStaffShift(staffShifts, lastDate, toDate);

        return staffShifts;
    }

    public List<StaffShift> getOrCreateRecordsForExtraDuty(Date fromDate, Date toDate, Staff staff) {
        List<StaffShift> staffShifts = getStaffShiftFromRecordForExraDutyCalculation(fromDate, toDate, staff);
//        System.err.println("EXTRA DUTY SHIFT  " + staffShifts);
        Calendar lastDate = Calendar.getInstance();
        lastDate.setTime(toDate);

        loopThroughStaffShift(staffShifts, lastDate, toDate);

        return staffShifts;
    }

    public void loopThroughStaffShift(List<StaffShift> staffShifts, Calendar lastDate, Date toDate) {
        for (StaffShift ss : staffShifts) {

            Calendar startTime = Calendar.getInstance();
            Calendar endTime = Calendar.getInstance();

            FingerPrintRecord startRecord = ss.getStartRecord();
            FingerPrintRecord endRecord = ss.getEndRecord();

            if (startRecord != null) {
                startTime.setTime(startRecord.getRecordTimeStamp());
            }

            if (endRecord != null) {
                endTime.setTime(endRecord.getRecordTimeStamp());
            }

            if ((startRecord != null
                    && endRecord != null
                    && startTime.get(Calendar.DATE) == lastDate.get(Calendar.DATE)
                    && endTime.get(Calendar.DATE) != lastDate.get(Calendar.DATE))
                    ////////////////////////////
                    || (startRecord != null
                    && endRecord == null
                    && startTime.get(Calendar.DATE) == lastDate.get(Calendar.DATE))) {

                //create new Staffshft Record
                createNewStaffShift(ss, lastDate);
                //Update EndRecord Time
                endRecord.setRecordTimeStamp(commonFunctions.getEndOfDay(toDate));
                getFingerPrintRecordFacade().edit(endRecord);
                //Update Staff shift
//                ss.setShiftEndTime(commonFunctions.getEndOfDay(dateRange.getToDate()));               
                getStaffShiftFacade().edit(ss);
            }
        }

    }

//    public OtNormalSpecial calOt(Date date, Staff staff) {
//        OtNormalSpecial otNormalSpecial = new OtNormalSpecial();
//
//        DateRange dateRange = commonFunctions.getDateRangeForOT(date);
//
//        otNormalSpecial.setDateRange(dateRange);
//
//        //System.err.println("From : " + dateRange.getFromDate());
//        //System.err.println("To : " + dateRange.getToDate());
//        List<StaffShift> staffShifts = getOrCreateRecordsForOt(dateRange.getFromDate(), dateRange.getToDate(), staff);
//
//        double otMaxMinute = getFinalVariables().getOtTime() * 4 * 60;
//        double workedMinute = 0.0;
//
//        if (staffShifts == null) {
//            return otNormalSpecial;
//        }
//
//        for (StaffShift ser : staffShifts) {
//            Calendar start = Calendar.getInstance();
//            start.setTime(ser.getStartRecord().getRecordTimeStamp());
//            Calendar endTime = Calendar.getInstance();
//            endTime.setTime(ser.getEndRecord().getRecordTimeStamp());
//
//            int netTime = endTime.get(Calendar.MINUTE) - start.get(Calendar.MINUTE);
//            workedMinute += netTime;
//
////            ser.setConsideredForOt(Boolean.TRUE);
//            System.err.println("SETTING OT TRUE");
//            getStaffShiftFacade().edit(ser);
//
//        }
//
//        if (workedMinute > (staff.getWorkingHourPerShift() * 4 * 60)) {
//            otNormalSpecial.setSpecialMin(otMaxMinute - (staff.getWorkingHourPerShift() * 4 * 60));
//        }
//
//        if (workedMinute > otMaxMinute) {
//            otNormalSpecial.setNormalMin(workedMinute - otMaxMinute);
//        }
//
//        return otNormalSpecial;
//
//    }
//
//    public OtNormalSpecial calculateOt(Date fromDate, Date toDate, Staff staff) {
//        OtNormalSpecial otNormalSpecial = new OtNormalSpecial();
//
//        otNormalSpecial.getDateRange().setFromDate(fromDate);
//        otNormalSpecial.getDateRange().setToDate(toDate);
//
//        //System.err.println("From : " + dateRange.getFromDate());
//        //System.err.println("To : " + dateRange.getToDate());
//        List<StaffShift> staffShifts = getOrCreateRecordsForOt(fromDate, toDate, staff);
//
//        double otMaxMinute = getFinalVariables().getOtTime() * 4 * 60;
//        double workedMinute = 0.0;
//
//        for (StaffShift ser : staffShifts) {
//            Calendar start = Calendar.getInstance();
//            start.setTime(ser.getStartRecord().getRecordTimeStamp());
//            Calendar endTime = Calendar.getInstance();
//            endTime.setTime(ser.getEndRecord().getRecordTimeStamp());
//
//            int netTime = endTime.get(Calendar.MINUTE) - start.get(Calendar.MINUTE);
//            workedMinute += netTime;
//
////            ser.setConsideredForOt(Boolean.TRUE);
//            System.err.println("SETTING OT TRUE");
//            getStaffShiftFacade().edit(ser);
//        }
//
//        if (workedMinute > (staff.getWorkingHourPerShift() * 4 * 60)) {
//            otNormalSpecial.setSpecialMin(otMaxMinute - (staff.getWorkingHourPerShift() * 4 * 60));
//        }
//
//        if (workedMinute > otMaxMinute) {
//            otNormalSpecial.setNormalMin(workedMinute - otMaxMinute);
//        }
//
//        return otNormalSpecial;
//
//    }
    public double calculateWorkTimeForOverTime(Date fromDate, Date toDate, Staff staff) {
//        String sql = "Select sum(ss.workedWithinTimeFrameVarified+ss.leavedTime) "
//                + " from StaffShift ss "
//                + " where ss.retired=false "
//                //                + " and ss.leavedTime=0 "
//                + " and type(ss)!=:cls"
//                + " and ss.dayType not in :dtp "
//                + " and ss.shiftStartTime between :fd  and :td "
//                + " and ss.staff=:stf ";
//        HashMap hm = new HashMap();
//        hm.put("cls", StaffShiftExtra.class);
//        hm.put("fd", fromDate);
//        hm.put("td", toDate);
//        hm.put("stf", staff);
//        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));
//        ////System.out.println("hm = " + hm);
//        ////System.out.println("sql = " + sql);
//        double dbl = staffShiftFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        HashMap hm = new HashMap();
        hm.put("cls", StaffShiftExtra.class);
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));
        String sql = "Select ss "
                + " from StaffShift ss "
                + " where ss.retired=false "
                //                + " and ss.leavedTime=0 "
                + " and type(ss)!=:cls"
                + " and ss.dayType not in :dtp "
                + " and ss.shiftStartTime between :fd  and :td "
                + " and ss.staff=:stf ";
        List<StaffShift> sss = staffShiftFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        double dbl = 0.0;
        for (StaffShift ss : sss) {
            if (ss.getLeaveType() == null) {
                dbl += ss.getWorkedWithinTimeFrameVarified();
            } else {
                if (ss.getLeaveType().isFullDayLeave() && ss.getShift() != null) {
                    dbl += (ss.getShift().getDurationMin() * 60);
                }

                if (!ss.getLeaveType().isFullDayLeave()) {
                    List<StaffLeave> staffLeave = fetchStaffLeave(ss.getStaff(), ss.getShiftDate());
                    if (staffLeave.size() > 1 && ss.getShift() != null) {
                        dbl += (ss.getShift().getDurationMin() * 60);
                    }
                }

                if (ss.getShift() != null) {
                    if (ss.getShift().isHalfShift()) {
                        dbl += (ss.getShift().getDurationMin() * 60) * 0.5;
                    }
                }
            }
        }
        return dbl;
    }

    public double calculateWorkTimeForOverTimeByDate(Date fromDate, Date toDate, Staff staff) {
//        String sql = "Select sum(ss.workedWithinTimeFrameVarified+ss.leavedTime) "
//                + " from StaffShift ss "
//                + " where ss.retired=false "
//                //                + " and ss.leavedTime=0 "
//                + " and type(ss)!=:cls"
//                + " and ss.dayType not in :dtp "
//                + " and ss.shiftStartTime between :fd  and :td "
//                + " and ss.staff=:stf ";
//        HashMap hm = new HashMap();
//        hm.put("cls", StaffShiftExtra.class);
//        hm.put("fd", fromDate);
//        hm.put("td", toDate);
//        hm.put("stf", staff);
//        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));
//        ////System.out.println("hm = " + hm);
//        ////System.out.println("sql = " + sql);
//        double dbl = staffShiftFacade.findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        HashMap hm = new HashMap();
        hm.put("cls", StaffShiftExtra.class);
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));
        String sql = "Select ss "
                + " from StaffShift ss "
                + " where ss.retired=false "
                //                + " and ss.leavedTime=0 "
                + " and type(ss)!=:cls"
                + " and ss.dayType not in :dtp "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        List<StaffShift> sss = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
        double dbl = 0.0;
        for (StaffShift ss : sss) {

            //System.out.println("ss.getId() = " + ss.getId());
            if (ss.getShift() == null) {
                continue;
            }
            //System.out.println("ss.getWorkedWithinTimeFrameVarified() = " + ss.getWorkedWithinTimeFrameVarified() / (60 * 60));
            //System.out.println("ss.getShift()" + ss.getShift().getName());
            //System.out.println("shift date" + ss.getShiftDate());


            if (ss.getLeaveType() == null) {
                dbl += ss.getWorkedWithinTimeFrameVarified();
//                //System.out.println("ss.getWorkedWithinTimeFrameVarified() = " + ss.getWorkedWithinTimeFrameVarified());
            } else if ((ss.getLeaveType() == LeaveType.AnnualHalf || ss.getLeaveType() == LeaveType.CasualHalf)
                    && (ss.isConsiderForEarlyOut() || ss.isConsiderForLateIn())) {
//                
                //
                List<StaffLeave> staffLeave = fetchStaffLeave(ss.getStaff(), ss.getShiftDate());

                if (staffLeave.size() > 1 && ss.getShift() != null) {

                    for (StaffLeave sl : staffLeave) {
                        if (sl.getLeaveType() != LeaveType.No_Pay_Half) {
                            if (ss.getShift().isHalfShift()) {
                                dbl += (ss.getShift().getLeaveHourFull() * 60 * 60);
                            } else {
                                dbl += (ss.getShift().getLeaveHourHalf() * 60 * 60);
                            }
                        }
                    }
//                        dbl += (ss.getShift().getDurationMin() * 60);
//                        //System.out.println("ss.getShift().getDurationMin() * 60 = " + ss.getShift().getDurationMin() * 60);
                } else {
                    if (ss.getShift() != null && ss.getShift().getLeaveHourHalf() != 0) {
                        double d = 0.0;
                        if (ss.isAutoLeave()) {
                            d = ss.getWorkedWithinTimeFrameVarified();
                        } else {
                            d = ss.getWorkedWithinTimeFrameVarified() + (ss.getShift().getLeaveHourHalf() * 60 * 60);
                            //cheeck leave count is 2 give leave time 
                        }
                        if ((ss.getShift().getDurationMin() * 60) < d) {
                            dbl += ss.getShift().getDurationMin() * 60;
                        } else {
                            dbl += d;
                        }
                    } else {
                        dbl += ss.getWorkedWithinTimeFrameVarified();
                    }
                }

                //
            } else {
                if (ss.getLeaveType().isFullDayLeave() && ss.getShift() != null && !ss.getShift().isHalfShift()) {
                    //12 hour shifts adding hours for leave (6 hours(leaveHourFull not shift duration))
//                    dbl += (ss.getShift().getDurationMin() * 60);
//                    //System.out.println("ss.getShift().getDurationMin() * 60 = " + ss.getShift().getDurationMin() * 60);
//                    //System.out.println("3. dbl = " + dbl);
                    //12 hour shifts adding hours for leave (6 hours(leaveHourFull not shift duration))
                    if (ss.getLeaveType() != LeaveType.No_Pay && ss.getLeaveType() != LeaveType.No_Pay_Half) {
//no pay and no pay half can't add duraiion minits
                        //no pay and no pay half can't add duraiion minits
                                                dbl += (ss.getShift().getLeaveHourFull() * 60 * 60);
                    }
                }
                //anul half duraion over shift time ask dushan
//                if (!ss.getLeaveType().isFullDayLeave()) {
//                    List<StaffLeave> staffLeave = fetchStaffLeave(ss.getStaff(), ss.getShiftDate());
//                    //System.out.println("staffLeave = " + staffLeave);
//                    //System.out.println("staffLeave.size() = " + staffLeave.size());
//                    //Can I run now ????ok
//                    if (staffLeave.size() > 1 && ss.getShift() != null) {
//                        dbl += (ss.getShift().getDurationMin() * 60);
//                        //System.out.println("ss.getShift().getDurationMin() * 60 = " + ss.getShift().getDurationMin() * 60);
//                        //System.out.println("4. dbl = " + dbl);
//                    } else {
//
//                        //Created by Buddhika
//                        dbl += ss.getWorkedWithinTimeFrameVarified();
//                        //System.out.println("ss.getShift().getDurationMin() * 60 * staffLeave.size() = " + ss.getShift().getDurationMin() * 60 * staffLeave.size());
//                        //System.out.println("4.a dbl = " + dbl);
//
//                    }
//
//                    if (ss.getLeaveType() == LeaveType.AnnualHalf || ss.getLeaveType() == LeaveType.CasualHalf || ss.getLeaveType() == LeaveType.LieuHalf
//                            || ss.getLeaveType() == LeaveType.DutyLeaveHalf) {
//                        if (ss.getShift() != null && ss.getShift().getLeaveHourHalf() != 0) {
//                            dbl += ss.getShift().getLeaveHourHalf() * 60 * 60;
//                            //System.out.println("4.b dbl = " + dbl);
//                        }
//                    }
//                }
                //anul half duraion over shift time ask dushan
                if (!ss.getLeaveType().isFullDayLeave()) {
                    List<StaffLeave> staffLeave = fetchStaffLeave(ss.getStaff(), ss.getShiftDate());
                    //Can I run now ????ok
                    if (staffLeave.size() > 1 && ss.getShift() != null) {

                        for (StaffLeave sl : staffLeave) {
                            if (sl.getLeaveType() != LeaveType.No_Pay_Half) {
                                if (ss.getShift().isHalfShift()) {
                                    if (sl.getQty() == 0.5) {
                                        dbl += (ss.getShift().getLeaveHourFull() * 60 * 60);
                                    }
                                } else {
                                    dbl += (ss.getShift().getLeaveHourHalf() * 60 * 60);
                                }
                            }
                        }
//                        dbl += (ss.getShift().getDurationMin() * 60);
//                        //System.out.println("ss.getShift().getDurationMin() * 60 = " + ss.getShift().getDurationMin() * 60);
                    } else {
                        if (ss.getLeaveType() == LeaveType.AnnualHalf || ss.getLeaveType() == LeaveType.CasualHalf || ss.getLeaveType() == LeaveType.LieuHalf
                                || ss.getLeaveType() == LeaveType.DutyLeaveHalf) {
                            if (ss.getShift() != null && ss.getShift().getLeaveHourHalf() != 0) {
                                double d = 0.0;
                                if (ss.isAutoLeave()) {
                                    d = ss.getWorkedWithinTimeFrameVarified();
                                } else {
                                    d = ss.getWorkedWithinTimeFrameVarified() + (ss.getShift().getLeaveHourHalf() * 60 * 60);
                                }
                                if ((ss.getShift().getDurationMin() * 60) < d) {
                                    dbl += ss.getShift().getDurationMin() * 60;
                                } else {
                                    dbl += d;
                                }
                            } else {
                                if (ss.getShift() != null && ss.getShift().getLeaveHourHalf() == 0) {
                                    dbl += ss.getShift().getLeaveHourFull();
                                }
                            }
                        } else {
                            dbl += ss.getWorkedWithinTimeFrameVarified();
                        }
                    }

                    //newly add for No pay half
//                    List<StaffLeave> staffLeaveSystem = fetchStaffLeaveSystem(ss.getStaff(), ss.getShiftDate());
//                    //System.out.println("staffLeaveSystem = " + staffLeaveSystem);
//                    if (!staffLeaveSystem.isEmpty() && ss.getShift() != null && ss.getLeaveType() == LeaveType.No_Pay_Half) {
//                        dbl += (ss.getWorkedWithinTimeFrameVarified());
//                        //System.out.println("ss.getWorkedWithinTimeFrameVarified() = " + ss.getWorkedWithinTimeFrameVarified());
//                    }
                }

                if (ss.getShift() != null) {
                    if (ss.getShift().isHalfShift() && (!ss.isConsiderForEarlyOut() && !ss.isConsiderForLateIn())) {
                        //NOT or, but AND , now check ok
                        dbl += (ss.getShift().getLeaveHourHalf() * 60);
                    }
                }
            }

        }
        return dbl;
    }

    public double calculateLeaveTimeForOverTime(Date fromDate, Date toDate, Staff staff) {
        String sql = "Select sum(ss.leavedTime) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.workedWithinTimeFrameVarified!=0 "
                + " and ss.dayType not in :dtp "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));

        return staffShiftFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public double calculateExtraWorkTimeValue(Date fromDate, Date toDate, Staff staff, DayType dayType) {
//        String sql = "Select sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)*"
//                + "ss.multiplyingFactorOverTime*ss.overTimeValuePerSecond)"
//                + " from StaffShift ss "
//                + " where ss.retired=false"
//                + " and ss.shiftDate between :fd  and :td "
//                + " and ss.staff=:stf"
//                + " and ss.dayType=:dtp ";

        String sql = " Select ss "
                + " from StaffShift ss "
                + " where ss.retired=false"
                //+ " and ss.additionalForm.retired=false "  add for additonal form retiered 210 in 2015 april 5(calculate value)
                + " and ss.additionalForm.retired=false "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf"
                + " and ss.dayType=:dtp ";

        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);
        hm.put("dtp", dayType);

//        ////System.out.println("hm = " + hm);
//        ////System.out.println("sql = " + sql);
        //No. THer error is due to calculating by seconds. SQL can not find such find cals accut. I will have one other method.
        List<StaffShift> list = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);

        if (list == null) {
            return 0;
        }

        double dbl = 0;
        for (StaffShift ss : list) {
            dbl += roundOff((ss.getExtraTimeFromStartRecordVarified() + ss.getExtraTimeFromEndRecordVarified() + ss.getExtraTimeCompleteRecordVarified()) * ss.getMultiplyingFactorOverTime() * ss.getOverTimeValuePerSecond());
            ////System.out.println("if outside");
            ////System.out.println("ss.getExtraTimeFromStartRecordVarified() = " + ss.getExtraTimeCompleteRecordVarified());
            ////System.out.println("ss.getExtraTimeFromEndRecordVarified() = " + ss.getExtraTimeFromEndRecordVarified());
            ////System.out.println("ss.getMultiplyingFactorOverTime() = " + ss.getMultiplyingFactorOverTime());
            ////System.out.println("ss.getOverTimeValuePerSecond() = " + ss.getOverTimeValuePerSecond());

            if (dayType == DayType.Extra) {
                dbl += roundOff(ss.getExtraTimeCompleteRecordVarified() * ss.getMultiplyingFactorOverTime() * ss.getOverTimeValuePerSecond());

            }

//            if( dayType==DayType.DayOff){
//                dbl += roundOff(ss.getExtraTimeCompleteRecordVarified() * ((ss.getMultiplyingFactorOverTime())*2.5) * ss.getOverTimeValuePerSecond());
//            }
        }

//        System.err.println(">>>>>>>>>>> " + dbl);
        return dbl;
    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    public double calculateExtraWorkTimeValue(Date fromDate, Date toDate, Staff staff, DayType dayType, double rate) {
        String sql = "Select ss "
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf"
                + " and ss.dayType=:dtp ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);
        hm.put("dtp", dayType);

        ////System.out.println("hm = " + hm);
        ////System.out.println("sql = " + sql);
        List<StaffShift> sss = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
        double d = 0.0;
        for (StaffShift ss : sss) {
            ////System.out.println("id = " + ss.getId());
            double td;
            td = roundOff(((ss.getExtraTimeFromStartRecordVarified() + ss.getExtraTimeFromEndRecordVarified())
                    * ss.getMultiplyingFactorOverTime()
                    * rate)) / 60;

            ////System.out.println("rate = " + rate);
            ////System.out.println("ss.getExtraTimeFromStartRecordVarified() = " + ss.getExtraTimeFromStartRecordVarified());
            ////System.out.println("ss.getExtraTimeFromEndRecordVarified() = " + ss.getExtraTimeFromEndRecordVarified());
            ////System.out.println("(ss.getExtraTimeFromStartRecordVarified() + ss.getExtraTimeFromEndRecordVarified()) = " + (ss.getExtraTimeFromStartRecordVarified() + ss.getExtraTimeFromEndRecordVarified()));
            ////System.out.println("ss.getMultiplyingFactorOverTime() = " + ss.getMultiplyingFactorOverTime());
            ////System.out.println("ss.getOverTimeValuePerSecond() = " + ss.getOverTimeValuePerSecond());
            ////System.out.println("ss.getOverTimeValuePerSecond() / 60 = " + ss.getOverTimeValuePerSecond() / 60);
//            ////System.out.println("ss.getShift().getName(); = " + ss.getShift().getName());
            ////System.out.println("ss.getShiftDate() = " + ss.getShiftDate());;
            ////System.out.println("ss.getShiftStartTime() = " + ss.getShiftStartTime());;
            ////System.out.println("ss.getShiftEndTime() = " + ss.getShiftEndTime());
            ////System.out.println("td = " + td);
            d += td;
            ////System.out.println("d = " + d);
        }
        return d;
    }

    public Long calculateExtraWorkMinute(Date fromDate, Date toDate, Staff staff, DayType dayType) {
        String sql;
        HashMap hm = new HashMap();
        String s;
        if (dayType == DayType.Extra) {
            sql = "Select sum(ss.extraTimeCompleteRecordVarified)";
        } else {
            sql = "Select sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified+ss.extraTimeCompleteRecordVarified))";
        }

        sql += " from StaffShift ss "
                + " where ss.retired=false "
                //+ " and ss.additionalForm.retired=false "  add for additonal form retiered 210 in 2015 april 5
                + " and ss.additionalForm.retired=false "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf"
                + " and ss.dayType=:dtp ";
        if (dayType == DayType.DayOff) {
            sql += " and ss.additionalForm.times!=:ts ";
            hm.put("ts", Times.All);
        }
        s = " select ss from StaffShift ss "
                + " where ss.retired=false "
                //+ " and ss.additionalForm.retired=false "  add for additonal form retiered 210 in 2015 april 5
                + " and ss.additionalForm.retired=false "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf"
                + " and ss.dayType=:dtp ";
        if (dayType == DayType.DayOff) {
            s += " and ss.additionalForm.times!=:ts ";
            hm.put("ts", Times.All);
        }
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);
        hm.put("dtp", dayType);

        Double timeSecond = staffShiftFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
        List<StaffShift> shifts = staffShiftFacade.findByJpql(s, hm, TemporalType.DATE);
        for (StaffShift ss : shifts) {
            //System.out.println("ss = " + ss);
            //System.out.println("ss.getShiftDate() = " + ss.getShiftDate());
            //System.out.println("ss.getAdditionalForm() = " + ss.getAdditionalForm());

        }
        if (timeSecond != null) {
            return (timeSecond.longValue() / 60);
        } else {
            return 0l;
        }
    }

    public double calculateNoPay(Date fromDate, Date toDate, Staff staff) {
        String sql = "Select sum(ss.leavedTimeNoPay) "
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);

        return staffShiftFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public List<StaffShift> fetchStaffShiftNoPay(Date fromDate, Date toDate, Staff staff) {
        String sql = "Select ss"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf "
                + " and ss.leavedTimeNoPay>0 ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);

        return staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
    }

    public long calculateHolidayWork(Date fromDate, Date toDate, Staff staff) {
        String sql = "Select count(distinct(ss.shiftDate)) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.lieuAllowed=false "
                + " and ss.lieuQtyUtilized=0"
                + " and ( ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.dayType in :dtp "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.Poya, DayType.MurchantileHoliday}));
        hm.put("stf", staff);

        return staffShiftFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    public long calculateWorkedDaysForSalary(Date fromDate, Date toDate, Staff staff) {
        ////System.out.println("calculating worked days for salary");
        String sql = "Select count(distinct(ss.shiftDate)) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and (( ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ))"
                //                + " or ss.leaveType is not null) "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);
        ////System.out.println("hm = " + hm);
        ////System.out.println("sql = " + sql);
        Long l;
        l = staffShiftFacade.findLongByJpql(sql, hm, TemporalType.DATE);
        ////System.out.println("l = " + l);
        return l;
    }

    public Double calculateHolidayWork(Date fromDate, Date toDate, Staff staff, DayType dayType) {
        String sql = "Select ss"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.shift is  not null "
                + " and ss.lieuAllowed=false "
                + " and ss.lieuQtyUtilized=0"
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.dayType=:dtp "
                + " and ss.shiftDate between :fd  and :td "
                //                + " and ss.extraTimeFromStartRecordVarified=:d "
                //                + " and ss.extraTimeFromEndRecordVarified=:d "
                //                + " and ss.extraTimeCompleteRecordVarified=:d "
                + " and ss.staff=:stf "
                + " order by ss.shiftDate ";

        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("dtp", dayType);
        hm.put("stf", staff);
//        hm.put("d", 0.0);

        List<StaffShift> list = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);

        Double dbl = 0.0;

        if (list != null) {
            StaffShift lastStaffShift = new StaffShift();
            for (StaffShift s : list) {
                //System.out.println("lastStaffShift = " + lastStaffShift);
                if (lastStaffShift != null) {
                    if (lastStaffShift.getShiftDate() != null) {
                        if (lastStaffShift.getShiftDate().getTime() == s.getShiftDate().getTime()) {
                            if (lastStaffShift.getShift().isHalfShift() && !s.getShift().isHalfShift()) {
                                dbl = 1.0;
                            }
                            continue;
                        }
                    }
                }
                dbl += s.getShift().isHalfShift() ? 0.5 : 1;
                lastStaffShift = s;
            }

        }

        return dbl;
    }

    public double calculateHolidayWork(Date fromDate, Date toDate, Staff staff, DayType dayType, double salaryPerDay) {
//        String sql = "Select sum(ss.multiplyingFactorSalary*" + salaryPerDay + ")"
//                + " from StaffShift ss "
//                + " where ss.retired=false "
//                + " and ss.lieuAllowed=false "
//                + " and ss.lieuQtyUtilized=0"
//                + " and ( ss.startRecord.recordTimeStamp is not null "
//                + " and ss.endRecord.recordTimeStamp is not null ) "
//                + " and ss.dayType=:dtp "
//                + " and ss.shiftDate between :fd  and :td "
//                + " and ss.staff=:stf ";
//        HashMap hm = new HashMap();
//        hm.put("fd", fromDate);
//        hm.put("td", toDate);
//        hm.put("dtp", dayType);
//        hm.put("stf", staff);
//
//        return staffShiftFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
        HashMap hm = new HashMap();
        String sql = "Select ss"
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.shift is not null "
                + " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null  "
                + " and ss.dayType = :dtp "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";

        if (dayType == DayType.Poya || dayType == DayType.MurchantileHoliday) {
            sql += " and ss.lieuAllowed=false "
                    + " and ss.lieuQtyUtilized=0";
        }

        if (dayType == DayType.DayOff) {
            sql += " and type(ss)=:class "; //only get add forms for day off
            hm.put("class", StaffShiftExtra.class);
        }

        sql += " order by ss.shiftDate ";

        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("dtp", dayType);
        hm.put("stf", staff);

        List<StaffShift> list = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);

        List<StaffShift> listtemp = list;

        Double lg = 0.0;

        if (list != null) {
            StaffShift lastStaffShift = new StaffShift();
            for (StaffShift s : list) {
                if (lastStaffShift != null) {
                    if (lastStaffShift.getShiftDate() != null) {
                        if (lastStaffShift.getShiftDate().getTime() == s.getShiftDate().getTime()) {
                            if (lastStaffShift.getShift().isHalfShift() && !s.getShift().isHalfShift()) {
                                lg = 1.0 * s.getMultiplyingFactorSalary() * salaryPerDay;
                            }
                            continue;
                        }
                    }
                }
                lg += (s.getShift().isHalfShift() ? 0.5 : 1) * s.getMultiplyingFactorSalary() * salaryPerDay;
                lastStaffShift = s;
            }

        }

        return lg;
    }

//    public long calculateDayOffWork(Date fromDate, Date toDate, Staff staff) {
//        String sql = "Select count(distinct(ss.shiftDate)) "
//                + " from StaffShift ss "
//                + " where ss.retired=false "
//                //                + " and ss.lieuPaymentAllowed=true "
//                + " and ss.startRecord.recordTimeStamp is not null "
//                + " and ss.endRecord.recordTimeStamp is not null  "
//                + " and ss.dayType in :dtp "
//                + " and ss.shiftDate between :fd  and :td "
//                + " and ss.staff=:stf ";
//        HashMap hm = new HashMap();
//        hm.put("fd", fromDate);
//        hm.put("td", toDate);
//        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.SleepingDay}));
//        hm.put("stf", staff);
//
//        return staffShiftFacade.findLongByJpql(sql, hm, TemporalType.DATE);
//    }
    public Double calculateOffDays(Date fromDate, Date toDate, Staff staff, DayType dayType) {
        String sql = "Select ss"
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and type(ss)=:class " //only get add forms for day off
                + " and ss.shift is not null "
                + " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null  "
                + " and ss.dayType = :dtp "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("dtp", dayType);
        hm.put("stf", staff);
        hm.put("class", StaffShiftExtra.class);

        List<StaffShift> list = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);

        Double lg = 0.0;

        if (list != null) {
            for (StaffShift s : list) {
                lg += s.getShift().isHalfShift() ? 0.5 : 1;
            }

        }

        return lg;
    }

    public double calculateOffDays(Date fromDate, Date toDate, Staff staff, DayType dayType, double salaryPerDay) {
        String sql = "Select ss "
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and type(ss)=:class " //only get add forms for day off
                + " and ss.shift is not null "
                + " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null  "
                + " and ss.dayType = :dtp "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("dtp", dayType);
        hm.put("stf", staff);
        hm.put("class", StaffShiftExtra.class);

        List<StaffShift> list = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);

        Double dbl = 0.0;

        if (list != null) {
            for (StaffShift s : list) {
                //System.out.println("s = " + s);
                dbl += (s.getMultiplyingFactorSalary() * salaryPerDay * (s.getShift().isHalfShift() == true ? 0.5 : 1));
            }

        }

        return dbl;
    }

    public List<StaffShift> fetchStaffShiftLiePaymentAllowed(Date fromDate, Date toDate, Staff staff) {
        String sql = "Select ss "
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.lieuPaymentAllowed=true "
                + " and ss.dayType in :dtp "
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.SleepingDay, DayType.MurchantileHoliday, DayType.Poya}));
        hm.put("stf", staff);

        return staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
    }

    public double calculateExtraDutyTime(Date fromDate, Date toDate, Staff staff) {
        String sql = "Select sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified+ss.extraTimeCompleteRecordVarified)*ss.multiplyingFactorOverTime) "
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.shiftDate between :fd  and :td "
                + " and ss.staff=:stf ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("stf", staff);

        return staffShiftFacade.findDoubleByJpql(sql, hm, TemporalType.DATE);
    }

    public void calculateBasic(Date fromDate, Date toDate, Staff staff) {

        //System.err.println("From : " + dateRange.getFromDate());
        //System.err.println("To : " + dateRange.getToDate());
        List<StaffShift> staffShifts = getOrCreateRecordsForSalary(fromDate, toDate, staff);

//        double otMaxMinute = getFinalVariables().getOtTime() * 4 * 60;
//        double workedMinute = 0.0;
        for (StaffShift ser : staffShifts) {
//            Calendar start = Calendar.getInstance();
//            start.setTime(ser.getStartRecord().getRecordTimeStamp());
//            Calendar endTime = Calendar.getInstance();
//            endTime.setTime(ser.getEndRecord().getRecordTimeStamp());
//
//            int netTime = endTime.get(Calendar.MINUTE) - start.get(Calendar.MINUTE);
//            workedMinute += netTime;

//            ser.setConsideredForSalary(Boolean.TRUE);
            /**
             * Previous check eligible for salary if hours less than 28 hours if
             * can be covered by leave
             *
             * calculate salary as normal add to leaves if leave is NOT marked
             * else add to maximum leave hours (hours 8) add to leave if not
             * already marked calculate no pay for monthly total salary as usual
             * / divide by 45 * by working hours end for end if end if
             *
             *
             *
             *
             *
             *
             *
             *
             */
//            System.err.println("SETTING SALARY TRUE");
            getStaffShiftFacade().edit(ser);
        }
    }

    public void calculateExtraDuty(Date fromDate, Date toDate, Staff staff) {

        //System.err.println("From : " + dateRange.getFromDate());
        //System.err.println("To : " + dateRange.getToDate());
        List<StaffShift> staffShifts = getOrCreateRecordsForExtraDuty(fromDate, toDate, staff);

//        double otMaxMinute = getFinalVariables().getOtTime() * 4 * 60;
//        double workedMinute = 0.0;
        for (StaffShift ser : staffShifts) {
//            Calendar start = Calendar.getInstance();
//            start.setTime(ser.getStartRecord().getRecordTimeStamp());
//            Calendar endTime = Calendar.getInstance();
//            endTime.setTime(ser.getEndRecord().getRecordTimeStamp());
//
//            int netTime = endTime.get(Calendar.MINUTE) - start.get(Calendar.MINUTE);
//            workedMinute += netTime;

//            ser.setConsideredForExtraDuty(Boolean.TRUE);
//            System.err.println("SETTING SALARY TRUE");
            getStaffShiftFacade().edit(ser);
        }
    }

    public List<ExtraDutyCount> calExtraDuty(Date date, Staff staff) {

        DateRange dateRange = new DateRange();
        dateRange.setFromDate(com.divudi.java.CommonFunctions.getStartOfMonth(date));
        dateRange.setToDate(com.divudi.java.CommonFunctions.getEndOfMonth(date));

        //System.err.println("From : " + dateRange.getFromDate());
        //System.err.println("To : " + dateRange.getToDate());
        List<StaffShift> staffShifts = getStaffShift(staff, dateRange.getFromDate(), dateRange.getToDate());

        List<ExtraDutyCount> extraDutyCounts = new ArrayList<>();

        for (ExtraDutyType edt : ExtraDutyType.values()) {
            ExtraDutyCount newD = new ExtraDutyCount();
            newD.setExtraDutyType(edt);
            newD.setCount(calDutyCount(edt, staffShifts));
            extraDutyCounts.add(newD);
        }

        return extraDutyCounts;

    }

    public List<ExtraDutyCount> calExtraDuty(Date fromDate, Date toDate, Staff staff) {

        DateRange dateRange = new DateRange();
        dateRange.setFromDate(fromDate);
        dateRange.setToDate(toDate);

        //System.err.println("From : " + dateRange.getFromDate());
        //System.err.println("To : " + dateRange.getToDate());
        List<StaffShift> staffShifts = getStaffShift(staff, dateRange.getFromDate(), dateRange.getToDate());

        List<ExtraDutyCount> extraDutyCounts = new ArrayList<>();

        for (ExtraDutyType edt : ExtraDutyType.values()) {
            ExtraDutyCount newD = new ExtraDutyCount();
            newD.setExtraDutyType(edt);
            newD.setCount(calDutyCount(edt, staffShifts));
            extraDutyCounts.add(newD);
        }

        return extraDutyCounts;

    }

    private double calDutyCount(ExtraDutyType extraDutyType, List<StaffShift> staffShifts) {
        if (extraDutyType == ExtraDutyType.DayOff) {
            return getDayOffs(staffShifts);
        }

        if (extraDutyType == ExtraDutyType.SleepingDay) {
            return getSleepingDays(staffShifts);
        }

        if (extraDutyType == ExtraDutyType.MurchantileHoliday) {
            return getHoliDayCount(staffShifts, DayType.MurchantileHoliday);
        }

        if (extraDutyType == ExtraDutyType.Poya) {
            return getHoliDayCount(staffShifts, DayType.Poya);
        }

        if (extraDutyType == ExtraDutyType.PublicHoliday) {
            return getHoliDayCount(staffShifts, DayType.PublicHoliday);
        }

        return 0.0;
    }

    private double getDayOffs(List<StaffShift> list) {
        double tmp = 0.0;
        for (StaffShift sst : list) {
            if (sst.isDayOff()) {
                tmp++;
            }
        }

        return tmp;
    }

    private double getSleepingDays(List<StaffShift> list) {
        double tmp = 0.0;
        for (StaffShift sst : list) {
            if (sst.isSleepingDay()) {
                tmp++;
            }
        }

        return tmp;
    }

    private double getHoliDayCount(List<StaffShift> list, DayType phType) {
        double tmp = 0.0;
        for (StaffShift sst : list) {
            if (getHolidayType(sst.getShiftDate(), phType)) {
                tmp++;
            }
        }

        return tmp;
    }

    public boolean getHolidayType(Date d, DayType phType) {
        String sql = "Select d From PhDate d "
                + " Where d.retired=false "
                + " and d.phDate=:dtd "
                + " and d.phType=:tp";
        HashMap hm = new HashMap();
        hm.put("dtd", d);
        hm.put("tp", phType);
        List<PhDate> tmp = getPhDateFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (!tmp.isEmpty()) {
            return true;
        }

        return false;
    }

    public PaysheetComponent getComponent(WebUser user, PaysheetComponentType paysheetComponentType) {
        String sql;
        HashMap hm;
        PaysheetComponent tmp;

        sql = "Select s From PaysheetComponent s"
                + "  where s.retired=false "
                + " and  s.componentType=:type ";

        hm = new HashMap();
        hm.put("type", paysheetComponentType);
        tmp = getPaysheetComponentFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (tmp == null) {
            tmp = new PaysheetComponent();
            tmp.setCreatedAt(new Date());
            tmp.setComponentType(paysheetComponentType);
            tmp.setCreater(user);
            tmp.setName(paysheetComponentType.toString());

            getPaysheetComponentFacade().create(tmp);
        }

        return tmp;
    }

//    public StaffSalary getStaffSalary(Staff s, Date date) {
//
//        String sql = "Select s From StaffSalary s "
//                + " where s.retired=false "
//                + " and s.staff=:s "
//                + " and s.salaryCycle.salaryFromDate<=:fd "
//                + " and s.salaryCycle.salaryToDate>=:td";
//
//        HashMap hm = new HashMap<>();
//        hm.put("fd", commonFunctions.getStartOfMonth(date));
//        hm.put("td", commonFunctions.getEndOfMonth(date));
//        hm.put("s", s);
//
//        StaffSalary tmp = getStaffSalaryFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
//
//        if (tmp == null) {
//            tmp = new StaffSalary();
//            tmp.setStaff(s);
//        }
//
//        return tmp;
//    }
    public StaffSalary getStaffSalary(Staff s, SalaryCycle salaryCycle) {

        String sql = "Select s From StaffSalary s "
                + " where s.retired=false "
                + " and s.staff=:s "
                + " and s.salaryCycle=:sal ";

        HashMap hm = new HashMap<>();
        hm.put("sal", salaryCycle);
        hm.put("s", s);

        StaffSalary tmp = getStaffSalaryFacade().findFirstByJpql(sql, hm, TemporalType.DATE);

        if (tmp == null) {
            tmp = new StaffSalary();
            tmp.setStaff(s);
        }

        return tmp;
    }

    public StaffPaysheetComponentFacade getStaffPaysheetComponentFacade() {
        return staffPaysheetComponentFacade;
    }

    public void setStaffPaysheetComponentFacade(StaffPaysheetComponentFacade staffPaysheetComponentFacade) {
        this.staffPaysheetComponentFacade = staffPaysheetComponentFacade;
    }

    public LinkedList<Staff> getStaffLink() {
        return staffLink;
    }

    public void setStaffLink(LinkedList<Staff> staffLink) {
        this.staffLink = staffLink;
    }

    public StaffSalaryFacade getStaffSalaryFacade() {
        return staffSalaryFacade;
    }

    public void setStaffSalaryFacade(StaffSalaryFacade staffSalaryFacade) {
        this.staffSalaryFacade = staffSalaryFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

   

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public PhDateFacade getPhDateFacade() {
        return phDateFacade;
    }

    public void setPhDateFacade(PhDateFacade phDateFacade) {
        this.phDateFacade = phDateFacade;
    }

    public StaffLeaveFacade getStaffLeaveFacade() {
        return staffLeaveFacade;
    }

    public void setStaffLeaveFacade(StaffLeaveFacade staffLeaveFacade) {
        this.staffLeaveFacade = staffLeaveFacade;
    }

    public PaysheetComponentFacade getPaysheetComponentFacade() {
        return paysheetComponentFacade;
    }

    public void setPaysheetComponentFacade(PaysheetComponentFacade paysheetComponentFacade) {
        this.paysheetComponentFacade = paysheetComponentFacade;
    }

    public FingerPrintRecordFacade getFingerPrintRecordFacade() {
        return fingerPrintRecordFacade;
    }

    public void setFingerPrintRecordFacade(FingerPrintRecordFacade fingerPrintRecordFacade) {
        this.fingerPrintRecordFacade = fingerPrintRecordFacade;
    }

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.data.MonthEndRecord;
import com.divudi.data.dataStructure.WeekDayWork;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.DepartmentAttendance;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.hr.StaffLeaveBallance;
import com.divudi.data.hr.StaffShiftAggrgation;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffShiftFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class HrReportController implements Serializable {

    ReportKeyWord reportKeyWord;
    Date fromDate;
    Date toDate;
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    StaffFacade staffFacade;
    List<StaffShift> staffShifts;
    List<Staff> staffs;
    List<FingerPrintRecord> fingerPrintRecords;
    @EJB
    StaffShiftFacade staffShiftFacade;
    @EJB
    FingerPrintRecordFacade fingerPrintRecordFacade;
    List<WeekDayWork> weekDayWorks;

    public List<WeekDayWork> getWeekDayWorks() {
        return weekDayWorks;
    }

    public void setWeekDayWorks(List<WeekDayWork> weekDayWorks) {
        this.weekDayWorks = weekDayWorks;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    public void createFingerPrintRecordLogged() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp";
        hm.put("ftp", FingerPrintRecordType.Logged);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public List<Staff> getStaffs() {
        return staffs;
    }

    public void setStaffs(List<Staff> staffs) {
        this.staffs = staffs;
    }

    public void createFingerPrintRecordVarified() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintQuary(String sql, HashMap hm) {
        sql = "select ss from FingerPrintRecord ss"
                + " where ss.retired=false"
                + " and ss.recordTimeStamp between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.staffShift.shift=:sh";
            hm.put("sh", getReportKeyWord().getShift());
        }

    }

    public void createStaffList() {
        String sql;
        sql = "select s from Staff s "
                + " where s.retired=false "
                + " order by s.codeInterger";
        staffs = getStaffFacade().findBySQL(sql);
    }

    public String createStaffShiftQuary(HashMap hm) {
        String sql = "";
        sql = "select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.leaveType is null "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.shift=:sh ";
            hm.put("sh", getReportKeyWord().getShift());
        }

        return sql;
    }

    List<StaffLeave> staffLeaves;
    @EJB
    StaffLeaveFacade staffLeaveFacade;

    public FingerPrintRecordFacade getFingerPrintRecordFacade() {
        return fingerPrintRecordFacade;
    }

    public void setFingerPrintRecordFacade(FingerPrintRecordFacade fingerPrintRecordFacade) {
        this.fingerPrintRecordFacade = fingerPrintRecordFacade;
    }

    public List<StaffLeave> getStaffLeaves() {
        return staffLeaves;
    }

    public void setStaffLeaves(List<StaffLeave> staffLeaves) {
        this.staffLeaves = staffLeaves;
    }

    public StaffLeaveFacade getStaffLeaveFacade() {
        return staffLeaveFacade;
    }

    public void setStaffLeaveFacade(StaffLeaveFacade staffLeaveFacade) {
        this.staffLeaveFacade = staffLeaveFacade;
    }

    public void createStaffLeave() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select ss from StaffLeave ss "
                + " where ss.retired=false "
                + " and ss.leaveDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getLeaveType() != null) {
            List<LeaveType> list = getReportKeyWord().getLeaveType().getLeaveTypes();

            sql += " and ss.leaveType in :ltp ";
            hm.put("ltp", list);

        }

        staffLeaves = staffLeaveFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    List<StaffLeaveBallance> staffLeaveBallances;

    public List<StaffLeaveBallance> getStaffLeaveBallances() {
        return staffLeaveBallances;
    }

    public void setStaffLeaveBallances(List<StaffLeaveBallance> staffLeaveBallances) {
        this.staffLeaveBallances = staffLeaveBallances;
    }

    public void createStaffLeaveAggregate() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select new com.divudi.data.hr.StaffLeaveBallance(ss.staff,ss.leaveType,sum(ss.qty)) "
                + " from StaffLeave ss "
                + " where ss.retired=false "
                + " and ss.leaveDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getLeaveType() != null) {
            List<LeaveType> list = getReportKeyWord().getLeaveType().getLeaveTypes();
            sql += " and ss.leaveType in :ltp ";
            hm.put("ltp", list);
        }

        sql += " group by ss.staff,ss.leaveType"
                + " order by ss.staff.person.name,ss.leaveType ";

        staffLeaveBallances = (List<StaffLeaveBallance>) (Object) staffLeaveFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    public void createStaffAttendanceAggregate() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select new com.divudi.data.hr.DepartmentAttendance(ss.staff.department,"
                + "FUNC('Date',ss.shiftDate),count(distinct(ss.staff))) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by FUNC('Date',ss.shiftDate),ss.staff.department"
                + " order by ss.shiftDate,ss.staff.department.name";

        departmentAttendances = (List<DepartmentAttendance>) (Object) staffLeaveFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    private List<Staff> fetchStaff() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select distinct(ss.staff)"
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.staff.department.name";

        return staffFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    private long fetchWorkedDays(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private List<Object[]> fetchWorkedTime(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select ss.dayOfWeek,sum(ss.workedWithinTimeFrameVarified+ss.leavedTime)"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by ss.dayOfWeek"
                + " order by ss.dayOfWeek ";
        return staffShiftFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    private long fetchExtraDutyDays(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and (ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified+ss.extraTimeCompleteRecordVarified)>0"
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private long fetchLateDays(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and (ss.lateInVarified)>0"
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private long fetchDayOff(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and ss.shift.dayType=:dtp"
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);
        hm.put("dtp", DayType.DayOff);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private List<MonthEndRecord> monthEndRecords;

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }

    @EJB
    HumanResourceBean humanResourceBean;

    public void createMonthEndReport() {
        List<Staff> staffList = fetchStaff();
        monthEndRecords = new ArrayList<>();
        for (Staff stf : staffList) {
            MonthEndRecord monthEnd = new MonthEndRecord();
            monthEnd.setStaff(stf);
            monthEnd.setWorkedDays(fetchWorkedDays(stf));
            monthEnd.setLeave_annual(humanResourceBean.calStaffLeave(stf, LeaveType.Annual, getFromDate(), getToDate()));
            monthEnd.setLeave_casual(humanResourceBean.calStaffLeave(stf, LeaveType.Casual, getFromDate(), getToDate()));
            monthEnd.setLeave_medical(humanResourceBean.calStaffLeave(stf, LeaveType.Medical, getFromDate(), getToDate()));
            monthEnd.setLeave_nopay(humanResourceBean.calStaffLeave(stf, LeaveType.No_Pay, getFromDate(), getToDate()));
            monthEnd.setExtraDutyDays(fetchExtraDutyDays(stf));
            monthEnd.setLatedays(fetchLateDays(stf));
            monthEnd.setDayoff(fetchDayOff(stf));

            monthEndRecords.add(monthEnd);
        }
    }

    public void createMonthEndWorkTimeReport() {
        Long dateCount = commonFunctions.getDayCount(getFromDate(), getToDate());
        Long numOfWeeks = dateCount / 7;
        List<Staff> staffList = fetchStaff();
        weekDayWorks = new ArrayList<>();
        for (Staff stf : staffList) {
            WeekDayWork weekDayWork = new WeekDayWork();
            weekDayWork.setStaff(stf);
            List<Object[]> list = fetchWorkedTime(stf);

            for (Object[] obj : list) {
                int dayOfWeek = (int) obj[0];
                double value = (double) obj[1];

                switch (dayOfWeek) {
                    case Calendar.SUNDAY:
                        weekDayWork.setSunDay(value);
                        break;
                    case Calendar.MONDAY:
                        weekDayWork.setMonDay(value);
                        break;
                    case Calendar.TUESDAY:
                        weekDayWork.setTuesDay(value);
                        break;
                    case Calendar.WEDNESDAY:
                        weekDayWork.setWednesDay(value);
                        break;
                    case Calendar.THURSDAY:
                        weekDayWork.setThursDay(value);
                        break;
                    case Calendar.FRIDAY:
                        weekDayWork.setFriDay(value);
                        break;
                    case Calendar.SATURDAY:
                        weekDayWork.setSaturDay(value);
                        break;
                }

                weekDayWork.setTotal(weekDayWork.getTotal() + value);
            }

            double normalWorkTime = numOfWeeks * stf.getWorkingTimeForOverTimePerWeek() * 60 * 60;
            double overTime = weekDayWork.getTotal() - normalWorkTime;

            if (overTime > 0) {
                weekDayWork.setOverTime(overTime);
            }

            weekDayWorks.add(weekDayWork);
        }
    }

    List<StaffShiftAggrgation> staffShiftAggrgations;

    public List<StaffShiftAggrgation> getStaffShiftAggrgations() {
        return staffShiftAggrgations;
    }

    public void setStaffShiftAggrgations(List<StaffShiftAggrgation> staffShiftAggrgations) {
        this.staffShiftAggrgations = staffShiftAggrgations;
    }

    @EJB
    FinalVariables finalVariables;

    public void createStaffWorkingTime() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select new com.divudi.data.hr.StaffShiftAggrgation(ss.staff,"
                + "sum(ss.workedWithinTimeFrameVarified),sum(ss.leavedTime)) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by ss.staff "
                //                + "  having (sum(ss.workedWithinTimeFrameVarified)+sum(ss.leavedTime))> 0 "
                + " order by ss.staff.codeInterger";

        staffShiftAggrgations = (List<StaffShiftAggrgation>) (Object) staffLeaveFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    public void createStaffWorkingTimeBelow28() {
        createStaffWorkingTime();
        if (staffShiftAggrgations == null) {
            return;
        }

        Long datRange = commonFunctions.getDayCount(getFromDate(), getToDate());
        Long mul = datRange / 7;
        double maxLimitInSec = mul * finalVariables.getMinimumWorkingHourPerWeek() * 60 * 60;
        System.err.println("Max Limit " + maxLimitInSec);
        List<StaffShiftAggrgation> list = new ArrayList<>();

        for (StaffShiftAggrgation ssa : staffShiftAggrgations) {
            double sum = 0;

            if (ssa.getWorkingTime() != null) {
                sum += ssa.getWorkingTime();
            }

            if (ssa.getLeavedTime() != null) {
                sum += ssa.getLeavedTime();
            }

            if (sum < maxLimitInSec) {
                list.add(ssa);
            }
        }

        staffShiftAggrgations = list;
    }

    List<DepartmentAttendance> departmentAttendances;

    public List<DepartmentAttendance> getDepartmentAttendances() {
        return departmentAttendances;
    }

    public void setDepartmentAttendances(List<DepartmentAttendance> departmentAttendances) {
        this.departmentAttendances = departmentAttendances;
    }

    public void createStaffShift() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createStaffShiftOnlyOt() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and (ss.startRecord.allowedExtraDuty=true or "
                + " ss.endRecord.allowedExtraDuty=true )";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

        if (staffShifts == null) {
            return;
        }

//        for (StaffShift ss : staffShifts) {
//             
//        }
    }

    public void createStaffShiftEarlyIn() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftStartTime  > ss.startRecord.recordTimeStamp";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftLateIn() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftStartTime  < ss.startRecord.recordTimeStamp";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftEarlyOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftEndTime > ss.endRecord.recordTimeStamp";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftLateOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftEndTime < ss.endRecord.recordTimeStamp";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    /**
     * Creates a new instance of HrReport
     */
    public HrReportController() {
    }

    public void makeNull() {
        reportKeyWord = null;
        staffShifts = null;
        fingerPrintRecords = null;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }

        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<StaffShift> getStaffShifts() {
        return staffShifts;
    }

    public void setStaffShifts(List<StaffShift> staffShifts) {
        this.staffShifts = staffShifts;
    }

    public List<FingerPrintRecord> getFingerPrintRecords() {
        return fingerPrintRecords;
    }

    public void setFingerPrintRecords(List<FingerPrintRecord> fingerPrintRecords) {
        this.fingerPrintRecords = fingerPrintRecords;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public List<MonthEndRecord> getMonthEndRecords() {
        return monthEndRecords;
    }

    public void setMonthEndRecords(List<MonthEndRecord> monthEndRecords) {
        this.monthEndRecords = monthEndRecords;
    }

}

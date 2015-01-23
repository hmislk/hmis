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
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.FingerPrintRecordHistory;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffLeaveEntitle;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftHistory;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.FingerPrintRecordHistoryFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.StaffShiftHistoryFacade;
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
    Institution institution;
    List<Department> selectDepartments;
    @EJB
    DepartmentFacade departmentFacade;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Department> getSelectDepartments() {
        return selectDepartments;
    }

    public void setSelectDepartments(List<Department> selectDepartments) {
        this.selectDepartments = selectDepartments;
    }

    public void crateDepartmentList() {
        String sql = " select i from Department i "
                + " where i.retired=false "
                + " and i.institution=:ins "
                + " order by i.name ";
        HashMap hm = new HashMap();
        hm.put("ins", institution);
        selectDepartments = departmentFacade.findBySQL(sql, hm);
    }

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
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.verifiedRecord.staffShift is not null ";
        hm.put("ftp", FingerPrintRecordType.Logged);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
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

    public void createFingerPrintRecordVarifiedAll() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.staffShift is not null ";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintRecordVarifiedWithLogged() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.staffShift is not null "
                + " and ss.loggedRecord is not null ";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintRecordVarifiedWithOutLogged() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp  "
                + " and ss.staffShift is not null "
                + " and ss.loggedRecord is null ";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintRecordNoShiftSetted() {
        HashMap hm = new HashMap();
        String sql = "";
        sql = "select ss from FingerPrintRecord ss "
                + " where ss.retired=false "
                + " and ss.verifiedRecord.staffShift is null "
                + " and ss.recordTimeStamp between :frm  and :to "
                + " and ss.fingerPrintRecordType=:ftp";

        hm.put("ftp", FingerPrintRecordType.Logged);
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

        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
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

    public String createFingerPrintQuary(HashMap hm) {
        String sql = "";
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

        return sql;

    }

    public void createStaffList() {
        String sql;
        HashMap hm = new HashMap();

        sql = "select ss from Staff ss "
                + " where ss.retired=false ";

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.codeInterger";
        staffs = getStaffFacade().findBySQL(sql);
    }

    public String createStaffShiftQuary(HashMap hm) {
        String sql = "";
        sql = "select ss from StaffShift ss "
                + " where ss.retired=false "
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

        sql += " order by ss.staff.codeInterger";
        staffLeaves = staffLeaveFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    private List<StaffLeave> staffLeavesAnnual;
    private List<StaffLeave> staffLeavesCashual;
    private List<StaffLeave> staffLeavesNoPay;
    private List<StaffLeave> staffLeavesDutyLeave;
    private List<StaffLeave> staffLeavesMedical;
    private List<StaffLeave> staffLeavesMaternity1st;
    private List<StaffLeave> staffLeavesMaternity2nd;
    private List<StaffLeave> staffLeavesLieu;

    double annualEntitle;
    double annualUtilized;
    double casualEntitle;
    double casualUtilized;
    double nopayEntitle;
    double nopayUtilized;
    double dutyLeaveEntitle;
    double dutyLeaveUtilized;
    private double medicalEntitle;
    private double medicalUtilized;
    private double maternity1Entitle;
    private double maternity1Utilized;
    private double maternity2Entitle;
    private double maternity2Utilized;
    private double lieuEntitle;
    private double lieuUtilized;

    public double getAnnualEntitle() {
        return annualEntitle;
    }

    public void setAnnualEntitle(double annualEntitle) {
        this.annualEntitle = annualEntitle;
    }

    public double getAnnualUtilized() {
        return annualUtilized;
    }

    public void setAnnualUtilized(double annualUtilized) {
        this.annualUtilized = annualUtilized;
    }
    
    
    

    public void createStaffLeaveDetail() {
        if (getReportKeyWord().getStaff() == null) {
            return;
        }

        annualEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Annual, fromDate, toDate);
        annualUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Annual, fromDate, toDate);
        staffLeavesAnnual = createStaffLeave(LeaveType.Annual, getReportKeyWord().getStaff(), getFromDate(), getToDate());
        
        casualEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Casual, fromDate, toDate);
        casualUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Casual, fromDate, toDate);
        staffLeavesCashual = createStaffLeave(LeaveType.Casual, getReportKeyWord().getStaff(), getFromDate(), getToDate());
        
        nopayEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.No_Pay, fromDate, toDate);
        nopayUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.No_Pay, fromDate, toDate);
        staffLeavesNoPay = createStaffLeave(LeaveType.No_Pay, getReportKeyWord().getStaff(), getFromDate(), getToDate());
        
        dutyLeaveEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.DutyLeave, fromDate, toDate);
        dutyLeaveUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.DutyLeave, fromDate, toDate);
        staffLeavesDutyLeave = createStaffLeave(LeaveType.DutyLeave, getReportKeyWord().getStaff(), getFromDate(), getToDate());
        
        medicalEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Medical, fromDate, toDate);
        medicalUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Medical, fromDate, toDate);
        staffLeavesMedical = createStaffLeave(LeaveType.Medical, getReportKeyWord().getStaff(), getFromDate(), getToDate());
        
        maternity1Entitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Maternity1st, fromDate, toDate);
        maternity1Utilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Maternity1st, fromDate, toDate);
        staffLeavesMaternity1st = createStaffLeave(LeaveType.Maternity1st, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        maternity2Entitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Maternity2nd, fromDate, toDate);
        maternity2Utilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Maternity2nd, fromDate, toDate);
        staffLeavesMaternity2nd = createStaffLeave(LeaveType.Maternity2nd, getReportKeyWord().getStaff(), getFromDate(), getToDate());
        
        lieuEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Lieu, fromDate, toDate);
        lieuUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Lieu, fromDate, toDate);
        staffLeavesLieu = createStaffLeave(LeaveType.Lieu, getReportKeyWord().getStaff(), getFromDate(), getToDate());
    }

    public List<StaffLeave> createStaffLeave(LeaveType leaveType, Staff staff, Date fromDate, Date toDate) {
        if (leaveType == null || staff == null) {
            return null;
        }

        String sql = "";
        HashMap hm = new HashMap();
        sql = "select ss from StaffLeave ss "
                + " where ss.retired=false "
                + " and ss.leaveDate between :frm  and :to "
                + " and ss.staff=:stf"
                + " and ss.leaveType in :ltp ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());

        return staffLeaveFacade.findBySQL(sql, hm, TemporalType.DATE);
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
                + " order by ss.staff.codeInterger,ss.leaveType ";

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
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
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

        sql += " order by ss.codeInterger";

        return staffFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    private long fetchWorkedDays(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
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
        sql = "select ss.dayOfWeek,"
                + " sum(ss.workedWithinTimeFrameVarified+ss.leavedTime),"
                + " sum(ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified+ss.extraTimeCompleteRecordVarified),"
                + " sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified+ss.extraTimeCompleteRecordVarified)*ss.multiplyingFactorOverTime*ss.overTimeValuePerSecond)"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
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

    private List<Object[]> fetchStaffShiftData() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select ss.staff,"
                + " sum(ss.earlyInVarified),"
                + " sum(ss.earlyOutVarified),"
                + " sum(ss.workedWithinTimeFrameVarified),"
                + " sum(ss.workedOutSideTimeFrameVarified),"
                + " sum(ss.workedTimeVarified),"
                + " sum(ss.lateInVarified),"
                + " sum(ss.lateOutVarified),"
                + " sum(ss.extraTimeFromStartRecordVarified),"
                + " sum(ss.extraTimeFromEndRecordVarified),"
                + " sum(ss.extraTimeCompleteRecordVarified),"
                + "sum(ss.leavedTime),"
                + " sum(ss.leavedTimeNoPay),"
                + " sum(ss.leavedTimeOther)"
                + " from StaffShift ss "
                + " where ss.retired=false"
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null)) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

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
                + " order by ss.staff.codeInterger";
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
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
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
                + " and (ss.lateInVarified)>0 "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
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
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
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
            monthEnd.setLeave_dutyLeave(humanResourceBean.calStaffLeave(stf, LeaveType.DutyLeave, getFromDate(), getToDate()));
            monthEnd.setExtraDutyDays(fetchExtraDutyDays(stf));
            monthEnd.setLatedays(fetchLateDays(stf));
            monthEnd.setDayoff(fetchDayOff(stf));
            monthEndRecords.add(monthEnd);
        }
    }

    public void createMonthEndWorkTimeReport() {
        Long dateCount = commonFunctions.getDayCount(getFromDate(), getToDate());
        System.err.println(dateCount);
        double numOfWeeks = dateCount / 7.0;
        List<Staff> staffList = fetchStaff();
        weekDayWorks = new ArrayList<>();
        for (Staff stf : staffList) {
            WeekDayWork weekDayWork = new WeekDayWork();
            weekDayWork.setStaff(stf);
            List<Object[]> list = fetchWorkedTime(stf);

            for (Object[] obj : list) {
                Integer dayOfWeek = (Integer) obj[0] != null ? (Integer) obj[0] : -1;
                Double value = (Double) obj[1] != null ? (Double) obj[1] : 0;
                Double valueExtra = (Double) obj[2] != null ? (Double) obj[2] : 0;
                Double totalExtraDuty = (Double) obj[3] != null ? (Double) obj[3] : 0;

                switch (dayOfWeek) {
                    case Calendar.SUNDAY:
                        weekDayWork.setSunDay(value);
                        weekDayWork.setSunDayExtra(valueExtra);
                        break;
                    case Calendar.MONDAY:
                        weekDayWork.setMonDay(value);
                        weekDayWork.setMonDayExtra(valueExtra);
                        break;
                    case Calendar.TUESDAY:
                        weekDayWork.setTuesDay(value);
                        weekDayWork.setTuesDayExtra(valueExtra);
                        break;
                    case Calendar.WEDNESDAY:
                        weekDayWork.setWednesDay(value);
                        weekDayWork.setWednesDayExtra(valueExtra);
                        break;
                    case Calendar.THURSDAY:
                        weekDayWork.setThursDay(value);
                        weekDayWork.setThursDayExtra(valueExtra);
                        break;
                    case Calendar.FRIDAY:
                        weekDayWork.setFriDay(value);
                        weekDayWork.setFriDayExtra(valueExtra);
                        break;
                    case Calendar.SATURDAY:
                        weekDayWork.setSaturDay(value);
                        weekDayWork.setSaturDayExtra(valueExtra);
                        break;
                }

                weekDayWork.setTotal(weekDayWork.getTotal() + value);
                weekDayWork.setExtraDutyValue(weekDayWork.getExtraDutyValue() + totalExtraDuty);
                weekDayWork.setExtraDuty(weekDayWork.getExtraDuty() + valueExtra);

            }

//            if (stf.getWorkingTimeForOverTimePerWeek() != 0 && numOfWeeks != 0) {
//                double normalWorkTime = numOfWeeks * stf.getWorkingTimeForOverTimePerWeek() * 60 * 60;
//                double overTime = weekDayWork.getTotal() - normalWorkTime;
//                if (overTime > 0) {
//                    weekDayWork.setOverTime(overTime);
//                }
//            }
            weekDayWork.setOverTime(humanResourceBean.getOverTimeFromRoster(stf.getWorkingTimeForOverTimePerWeek(), numOfWeeks, weekDayWork.getTotal()));

            //Fetch Basic
            StaffPaysheetComponent basic = humanResourceBean.getBasic(stf, getToDate());

            if (basic != null) {
                weekDayWork.setBasicPerSecond(basic.getStaffPaySheetComponentValue() / (200 * 60 * 60));
            }

            weekDayWorks.add(weekDayWork);
        }
    }

    public void createMonthEndStaffShiftReport() {
        List<Object[]> list = fetchStaffShiftData();
        staffShifts = new ArrayList<>();
        for (Object[] obj : list) {
            StaffShift s = new StaffShift();
            Staff staff = (Staff) obj[0];
            Double earlyInVarified = (Double) obj[1];
            Double earlyOutVarified = (Double) obj[2];
            Double workedWithinTimeFrameVarified = (Double) obj[3];
            Double workedOutSideTimeFrameVarified = (Double) obj[4];
            Double workedTimeVarified = (Double) obj[5];
            Double lateInVarified = (Double) obj[6];
            Double lateOutVarified = (Double) obj[7];
            Double extraTimeFromStartRecordVarified = (Double) obj[8];
            Double extraTimeFromEndRecordVarified = (Double) obj[9];
            Double extraTimeCompleteRecordVarified = (Double) obj[10];
            Double leavedTime = (Double) obj[11];
            Double leavedTimeNoPay = (Double) obj[12];
            Double leavedTimeOther = (Double) obj[13];

            s.setStaff(staff);
            s.setEarlyInVarified(earlyInVarified == null ? 0 : earlyInVarified);
            s.setEarlyOutVarified(earlyOutVarified == null ? 0 : earlyOutVarified);
            s.setWorkedWithinTimeFrameVarified(workedWithinTimeFrameVarified == null ? 0 : workedWithinTimeFrameVarified);
            s.setWorkedOutSideTimeFrameVarified(workedOutSideTimeFrameVarified == null ? 0 : workedOutSideTimeFrameVarified);
            s.setWorkedTimeVarified(workedTimeVarified == null ? 0 : workedTimeVarified);
            s.setLateInVarified(lateInVarified == null ? 0 : lateInVarified);
            s.setLateOutVarified(lateOutVarified == null ? 0 : lateOutVarified);
            s.setExtraTimeFromStartRecordVarified(extraTimeFromStartRecordVarified == null ? 0 : extraTimeFromStartRecordVarified);
            s.setExtraTimeFromEndRecordVarified(extraTimeFromEndRecordVarified == null ? 0 : extraTimeFromEndRecordVarified);
            s.setExtraTimeCompleteRecordVarified(extraTimeCompleteRecordVarified == null ? 0 : extraTimeCompleteRecordVarified);
            s.setLeavedTime(leavedTime == null ? 0 : leavedTime);
            s.setLeavedTimeNoPay(leavedTimeNoPay == null ? 0 : leavedTimeNoPay);
            s.setLeavedTimeOther(leavedTimeOther == null ? 0 : leavedTimeOther);

            staffShifts.add(s);
        }
    }

    public void createMonthEndWorkTimeReportNoPay() {
        Long dateCount = commonFunctions.getDayCount(getFromDate(), getToDate());
        Long numOfWeeks = dateCount / 7;
        List<Staff> staffList = fetchStaff();
        weekDayWorks = new ArrayList<>();
        for (Staff stf : staffList) {
            WeekDayWork weekDayWork = new WeekDayWork();
            weekDayWork.setStaff(stf);
            List<Object[]> list = fetchWorkedTime(stf);

            for (Object[] obj : list) {
                Integer dayOfWeek = (Integer) obj[0];
                Double value = (Double) obj[1];

                if (value == null) {
                    value = 0.0;
                }

                if (dayOfWeek == null) {
                    dayOfWeek = -1;
                }
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

            double normalWorkTime = numOfWeeks * stf.getWorkingTimeForNoPayPerWeek() * 60 * 60;
            double noPays = weekDayWork.getTotal() - normalWorkTime;

            if (noPays < 0) {
                weekDayWork.setNoPay(noPays);
                weekDayWorks.add(weekDayWork);
            }

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
                + "sum(ss.workedWithinTimeFrameVarified),sum(ss.leavedTime+ss.leavedTimeOther)) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                //  + " and ((ss.startRecord.recordTimeStamp is not null "
                //   + " and ss.endRecord.recordTimeStamp is not null) "
                // + " or (ss.leaveType is not null) ) "
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
        sql += " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ";
        sql += " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createStaffShiftOnlyOt() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and (ss.startRecord.allowedExtraDuty=true or "
                + " ss.endRecord.allowedExtraDuty=true )";
        sql += " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ";
        sql += " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftEarlyIn() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftStartTime  > ss.startRecord.recordTimeStamp";
        sql += " order by ss.staff.codeInterger ";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftLateIn() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftStartTime  < ss.startRecord.recordTimeStamp";
        sql += " order by ss.staff.codeInterger ";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    List<StaffShiftHistory> staffShiftHistorys;

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<StaffShiftHistory> getStaffShiftHistorys() {
        return staffShiftHistorys;
    }

    public void setStaffShiftHistorys(List<StaffShiftHistory> staffShiftHistorys) {
        this.staffShiftHistorys = staffShiftHistorys;
    }

    @EJB
    StaffShiftHistoryFacade staffShiftHistoryFacade;

    public void createStaffShiftHistory() {
        String sql = "";
        HashMap hm = new HashMap();

        sql = "select ss from StaffShiftHistory ss "
                + " where ss.retired=false "
                + " and ss.staffShift.shiftDate between :frm  and :to ";
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

//        sql += " and ss.shiftStartTime  < ss.startRecord.recordTimeStamp";
        sql += " order by ss.staffShift.id,ss.createdAt";
        staffShiftHistorys = staffShiftHistoryFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    List<FingerPrintRecordHistory> fingerPrintRecordHistorys;
    @EJB
    FingerPrintRecordHistoryFacade fingerPrintRecordHistoryFacade;

    public StaffShiftHistoryFacade getStaffShiftHistoryFacade() {
        return staffShiftHistoryFacade;
    }

    public void setStaffShiftHistoryFacade(StaffShiftHistoryFacade staffShiftHistoryFacade) {
        this.staffShiftHistoryFacade = staffShiftHistoryFacade;
    }

    public List<FingerPrintRecordHistory> getFingerPrintRecordHistorys() {
        return fingerPrintRecordHistorys;
    }

    public void setFingerPrintRecordHistorys(List<FingerPrintRecordHistory> fingerPrintRecordHistorys) {
        this.fingerPrintRecordHistorys = fingerPrintRecordHistorys;
    }

    public FingerPrintRecordHistoryFacade getFingerPrintRecordHistoryFacade() {
        return fingerPrintRecordHistoryFacade;
    }

    public void setFingerPrintRecordHistoryFacade(FingerPrintRecordHistoryFacade fingerPrintRecordHistoryFacade) {
        this.fingerPrintRecordHistoryFacade = fingerPrintRecordHistoryFacade;
    }

    public void createFingerPrintHistory() {
        String sql = "";
        HashMap hm = new HashMap();

        sql = "select ss from FingerPrintRecordHistory ss "
                + " where ss.retired=false "
                + " and ss.beforeChange between :frm  and :to ";
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

//        sql += " and ss.shiftStartTime  < ss.startRecord.recordTimeStamp";
        sql += " order by ss.fingerPrintRecord.id,ss.id";
        fingerPrintRecordHistorys = fingerPrintRecordHistoryFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createStaffShiftLateInEarlyOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);

        sql += " and (ss.lateInVarified!=0"
                + " or ss.earlyOutVarified!=0)";

        if (getReportKeyWord().getFrom() != 0 && getReportKeyWord().getTo() != 0) {
            sql += " and ((ss.lateInVarified>= :frmTime  and ss.lateInVarified<= :toTime) "
                    + " or (ss.earlyOutVarified>= :frmTime and ss.earlyOutVarified<= :toTime )) ";
            hm.put("frmTime", getReportKeyWord().getFrom() * 60);
            hm.put("toTime", getReportKeyWord().getTo() * 60);
        } else if (getReportKeyWord().getFrom() != 0 && getReportKeyWord().getTo() == 0) {
            sql += " and ((ss.lateInVarified>= :frmTime) "
                    + " or (ss.earlyOutVarified>= :frmTime)) ";
            hm.put("frmTime", getReportKeyWord().getFrom() * 60);
        } else if (getReportKeyWord().getFrom() == 0 && getReportKeyWord().getTo() != 0) {
            sql += " and ((ss.lateInVarified<= :toTime) "
                    + " or (ss.earlyOutVarified<= :toTime )) ";
            hm.put("toTime", getReportKeyWord().getTo() * 60);
        }
        sql += " order by ss.codeInterger";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftEarlyOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftEndTime > ss.endRecord.recordTimeStamp";
        sql += " order by ss.codeInterger";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftLateOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftEndTime < ss.endRecord.recordTimeStamp";
        sql += " order by ss.codeInterger";
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

    public List<StaffLeave> getStaffLeavesAnnual() {
        return staffLeavesAnnual;
    }

    public void setStaffLeavesAnnual(List<StaffLeave> staffLeavesAnnual) {
        this.staffLeavesAnnual = staffLeavesAnnual;
    }

    public List<StaffLeave> getStaffLeavesCashual() {
        return staffLeavesCashual;
    }

    public void setStaffLeavesCashual(List<StaffLeave> staffLeavesCashual) {
        this.staffLeavesCashual = staffLeavesCashual;
    }

    public List<StaffLeave> getStaffLeavesNoPay() {
        return staffLeavesNoPay;
    }

    public void setStaffLeavesNoPay(List<StaffLeave> staffLeavesNoPay) {
        this.staffLeavesNoPay = staffLeavesNoPay;
    }

    public List<StaffLeave> getStaffLeavesDutyLeave() {
        return staffLeavesDutyLeave;
    }

    public void setStaffLeavesDutyLeave(List<StaffLeave> staffLeavesDutyLeave) {
        this.staffLeavesDutyLeave = staffLeavesDutyLeave;
    }

    public double getCasualEntitle() {
        return casualEntitle;
    }

    public void setCasualEntitle(double casualEntitle) {
        this.casualEntitle = casualEntitle;
    }

    public double getCasualUtilized() {
        return casualUtilized;
    }

    public void setCasualUtilized(double casualUtilized) {
        this.casualUtilized = casualUtilized;
    }

    public double getNopayEntitle() {
        return nopayEntitle;
    }

    public void setNopayEntitle(double nopayEntitle) {
        this.nopayEntitle = nopayEntitle;
    }

    public double getNopayUtilized() {
        return nopayUtilized;
    }

    public void setNopayUtilized(double nopayUtilized) {
        this.nopayUtilized = nopayUtilized;
    }

    public double getDutyLeaveEntitle() {
        return dutyLeaveEntitle;
    }

    public void setDutyLeaveEntitle(double dutyLeaveEntitle) {
        this.dutyLeaveEntitle = dutyLeaveEntitle;
    }

    public double getDutyLeaveUtilized() {
        return dutyLeaveUtilized;
    }

    public void setDutyLeaveUtilized(double dutyLeaveUtilized) {
        this.dutyLeaveUtilized = dutyLeaveUtilized;
    }

    public List<StaffLeave> getStaffLeavesMedical() {
        return staffLeavesMedical;
    }

    public void setStaffLeavesMedical(List<StaffLeave> staffLeavesMedical) {
        this.staffLeavesMedical = staffLeavesMedical;
    }

    public List<StaffLeave> getStaffLeavesMaternity1st() {
        return staffLeavesMaternity1st;
    }

    public void setStaffLeavesMaternity1st(List<StaffLeave> staffLeavesMaternity1st) {
        this.staffLeavesMaternity1st = staffLeavesMaternity1st;
    }

    public List<StaffLeave> getStaffLeavesMaternity2nd() {
        return staffLeavesMaternity2nd;
    }

    public void setStaffLeavesMaternity2nd(List<StaffLeave> staffLeavesMaternity2nd) {
        this.staffLeavesMaternity2nd = staffLeavesMaternity2nd;
    }

    public List<StaffLeave> getStaffLeavesLieu() {
        return staffLeavesLieu;
    }

    public void setStaffLeavesLieu(List<StaffLeave> staffLeavesLieu) {
        this.staffLeavesLieu = staffLeavesLieu;
    }

    public double getMedicalEntitle() {
        return medicalEntitle;
    }

    public void setMedicalEntitle(double medicalEntitle) {
        this.medicalEntitle = medicalEntitle;
    }

    public double getMedicalUtilized() {
        return medicalUtilized;
    }

    public void setMedicalUtilized(double medicalUtilized) {
        this.medicalUtilized = medicalUtilized;
    }

    public double getMaternity1Entitle() {
        return maternity1Entitle;
    }

    public void setMaternity1Entitle(double maternity1Entitle) {
        this.maternity1Entitle = maternity1Entitle;
    }

    public double getMaternity1Utilized() {
        return maternity1Utilized;
    }

    public void setMaternity1Utilized(double maternity1Utilized) {
        this.maternity1Utilized = maternity1Utilized;
    }

    public double getMaternity2Entitle() {
        return maternity2Entitle;
    }

    public void setMaternity2Entitle(double maternity2Entitle) {
        this.maternity2Entitle = maternity2Entitle;
    }

    public double getMaternity2Utilized() {
        return maternity2Utilized;
    }

    public void setMaternity2Utilized(double maternity2Utilized) {
        this.maternity2Utilized = maternity2Utilized;
    }

    public double getLieuEntitle() {
        return lieuEntitle;
    }

    public void setLieuEntitle(double lieuEntitle) {
        this.lieuEntitle = lieuEntitle;
    }

    public double getLieuUtilized() {
        return lieuUtilized;
    }

    public void setLieuUtilized(double lieuUtilized) {
        this.lieuUtilized = lieuUtilized;
    }

}

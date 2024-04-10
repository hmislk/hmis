/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.ReportKeyWord;

import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Form;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.LeaveForm;
import com.divudi.entity.hr.LeaveFormSystem;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffLeaveEntitle;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.LeaveFormFacade;
import com.divudi.facade.StaffLeaveEntitleFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Named(value = "staffLeaveApplicationFormController")
@SessionScoped
public class StaffLeaveApplicationFormController implements Serializable {

    LeaveForm currentLeaveForm;
    @EJB
    StaffLeaveFacade staffLeaveFacade;
    @EJB
    LeaveFormFacade leaveFormFacade;
    @Inject
    SessionController sessionController;
    @Inject
    StaffAdditionalFormController staffAdditionalFormController;
    @Inject
    CommonController commonController;

    @EJB
    CommonFunctions commonFunctions;
    List<LeaveForm> leaveForms;
    Staff approvedStaff;
    Staff staff;
    Date fromDate;
    Date toDate;
    @Enumerated(EnumType.STRING)
    LeaveType leaveType;
    double leaveEntitle;
    double leaved;
    @EJB
    FinalVariables finalVariables;
    ReportKeyWord reportKeyWord;

    StaffLeave staffLeave;
    List<StaffLeave> staffLeaves;

    boolean withOutretRierd;

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public double getLeaveEntitle() {
        return leaveEntitle;
    }

    public void setLeaveEntitle(double leaveEntitle) {
        this.leaveEntitle = leaveEntitle;
    }

    public double getLeaved() {
        return leaved;
    }

    public void setLeaved(double leaved) {
        this.leaved = leaved;
    }

    public PhDateController getPhDateController() {
        return phDateController;
    }

    public void setPhDateController(PhDateController phDateController) {
        this.phDateController = phDateController;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    @EJB
    StaffLeaveEntitleFacade staffLeaveEntitleFacade;
    @EJB
    StaffShiftFacade staffShiftFacade;
    List<StaffShift> staffShifts;
    List<StaffShift> staffShiftsLie;
    StaffShift[] staffShiftsArray;

    public StaffShift[] getStaffShiftsArray() {
        return staffShiftsArray;
    }

    public void setStaffShiftsArray(StaffShift[] staffShiftsArray) {
        this.staffShiftsArray = staffShiftsArray;
    }

    public void fetchStaffShift() {
        String sql = "Select s from StaffShift s"
                + " where s.retired=false"
                + " and s.lieuQtyUtilized=0 "
                + " and s.staff=:stf "
                + " and s.shiftDate between :f and :t";
        HashMap hm = new HashMap();
        hm.put("stf", getCurrentLeaveForm().getStaff());
        hm.put("f", getCurrentLeaveForm().getFromDate());
        hm.put("t", getCurrentLeaveForm().getToDate());
        staffShifts = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
    }

    public List<StaffShift> getStaffShiftsLie() {
        return staffShiftsLie;
    }

    public void setStaffShiftsLie(List<StaffShift> staffShiftsLie) {
        this.staffShiftsLie = staffShiftsLie;
    }

    public void fetchStaffShiftLie() {

        String sql = "Select s from StaffShift s"
                + " where s.retired=false"
                + " and s.lieuAllowed=true"
                + " and (s.lieuQty>s.lieuQtyUtilized)"
                + " and s.staff=:stf ";
//                + " and s.startRecord.recordTimeStamp is not null "
//                + " and s.endRecord.recordTimeStamp is not null";
        HashMap hm = new HashMap();
        hm.put("stf", getCurrentLeaveForm().getStaff());
        staffShiftsLie = staffShiftFacade.findByJpql(sql, hm);
    }

    public void fetchStaffShiftLateInErlyOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.consideredForLateEarlyAttendance=false "
                + "  and ss.staff=:stf ";
        hm.put("stf", getCurrentLeaveForm().getStaff());

        if (getReportKeyWord().getFrom() != 0) {
            sql += " and (ss.lateInVarified>= :frm "
                    + " or ss.earlyOutVarified>= :frm) ";
            hm.put("frm", getReportKeyWord().getFrom());
        }

        if (getReportKeyWord().getTo() != 0) {
            sql += " and (ss.lateInVarified<= :to "
                    + " or ss.earlyOutVarified<= :to) ";
            hm.put("to", getReportKeyWord().getTo());
        }

        staffShifts = staffShiftFacade.findByJpql(sql, hm, 10);
    }

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }

    public StaffLeaveEntitleFacade getStaffLeaveEntitleFacade() {
        return staffLeaveEntitleFacade;
    }

    public void setStaffLeaveEntitleFacade(StaffLeaveEntitleFacade staffLeaveEntitleFacade) {
        this.staffLeaveEntitleFacade = staffLeaveEntitleFacade;
    }

    public List<StaffShift> getStaffShifts() {
        return staffShifts;
    }

    public void setStaffShifts(List<StaffShift> staffShifts) {
        this.staffShifts = staffShifts;
    }

    public StaffLeaveEntitle fetchLeaveEntitle(Staff staff, LeaveType leaveType, Date frm, Date td) {
        List<LeaveType> list = leaveType.getLeaveTypes();

        String sql = "select  ss "
                + " from StaffLeaveEntitle ss "
                + " where ss.retired=false "
                + " and ss.staff=:stf "
                + " and ss.fromDate<=:frm  "
                + " and ss.toDate>=:frm "
                + " and ss.leaveType in :ltp ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", list);
        hm.put("frm", frm);
//        hm.put("td", td);
        StaffLeaveEntitle stf = staffLeaveEntitleFacade.findFirstByJpql(sql, hm, TemporalType.DATE);

        return stf;

        //Need to Add toLogicTo Date  
    }

    public StaffLeaveEntitle fetchLeaveEntitle(Staff staff, LeaveType leaveType, Date frm) {

        String sql = "select  ss "
                + " from StaffLeaveEntitle ss "
                + " where ss.retired=false "
                + " and ss.staff=:stf "
                + " and ss.fromDate<=:frm  "
                + " and ss.leaveType =:ltp ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", leaveType);
        hm.put("frm", frm);
        StaffLeaveEntitle stf = staffLeaveEntitleFacade.findFirstByJpql(sql, hm, TemporalType.DATE);

        return stf;

        //Need to Add toLogicTo Date  
    }

    public StaffLeaveEntitle fetchLeaveEntitle(Staff staff, LeaveType leaveType) {

        String sql = "select  ss "
                + " from StaffLeaveEntitle ss "
                + " where ss.retired=false "
                + " and ss.staff=:stf "
                + " and ss.leaveType =:ltp ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", leaveType);
        StaffLeaveEntitle stf = staffLeaveEntitleFacade.findFirstByJpql(sql, hm);

        return stf;

        //Need to Add toLogicTo Date  
    }

    public void calLeaveCount() {
        if (getCurrentLeaveForm().getStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return;
        }

        LeaveType leaveTypeLocal = getCurrentLeaveForm().getLeaveType();

        if (leaveTypeLocal == null) {
            JsfUtil.addErrorMessage("Please Select Leave Type");
            return;
        }

        StaffLeaveEntitle staffLeaveEntitle = fetchLeaveEntitle(
                getCurrentLeaveForm().getStaff(), getCurrentLeaveForm().getLeaveType(),
                getCurrentLeaveForm().getFromDate(),
                getCurrentLeaveForm().getToDate());
        ////System.out.println("getCurrentLeaveForm().getStaff() = " + getCurrentLeaveForm().getStaff());
        ////System.out.println("getCurrentLeaveForm().getLeaveType() = " + getCurrentLeaveForm().getLeaveType());
        ////System.out.println("commonFunctions.getLastDayOfYear(getCurrentLeaveForm().getFromDate()) = " + commonFunctions.getLastDayOfYear(getCurrentLeaveForm().getFromDate()));
        ////System.out.println("commonFunctions.getFirstDayOfYear(getCurrentLeaveForm().getFromDate()) = " + commonFunctions.getFirstDayOfYear(getCurrentLeaveForm().getFromDate()));
        ////System.out.println("staffLeaveEntitle = " + staffLeaveEntitle);
        ////System.out.println("leaveTypeLocal.isExceptionalLeave() = " + leaveTypeLocal.isExceptionalLeave());

        if (!leaveTypeLocal.isExceptionalLeave() && staffLeaveEntitle == null) {
            JsfUtil.addErrorMessage("Please Set Leave Enttile count for this Staff in Administration");
            return;
        }

        if (staffLeaveEntitle != null) {
            leaveEntitle = staffLeaveEntitle.getCount();
            leaved = humanResourceBean.calStaffLeave(getCurrentLeaveForm().getStaff(), leaveTypeLocal,
                    staffLeaveEntitle.getFromDate(),
                    staffLeaveEntitle.getToDate());
        }

    }

    public StaffLeaveFacade getStaffLeaveFacade() {
        return staffLeaveFacade;
    }

    public void setStaffLeaveFacade(StaffLeaveFacade staffLeaveFacade) {
        this.staffLeaveFacade = staffLeaveFacade;
    }

    public StaffLeaveApplicationFormController() {
    }

    public boolean errorCheck() {
        if (currentLeaveForm.getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Form Number");
            return true;
        }

        if (currentLeaveForm.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }
        if (getCurrentLeaveForm().getLeaveType() == null) {
            JsfUtil.addErrorMessage("Please Enter Leave Type");
            return true;
        }

        if (!getCurrentLeaveForm().getLeaveType().isExceptionalLeave()
                && (leaveEntitle - leaved) <= 0) {
            JsfUtil.addErrorMessage("Staff Leave Entitle is Overflow");
            return true;
        }

        if (currentLeaveForm.getRequestedDate() == null) {
            JsfUtil.addErrorMessage("Please Select Date");
            return true;
        }

        if (currentLeaveForm.getApprovedStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Person");
            return true;
        }
        if (currentLeaveForm.getApprovedAt() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Date");
            return true;
        }
        if (currentLeaveForm.getComments() == null || "".equals(currentLeaveForm.getComments())) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return true;
        }

        if (currentLeaveForm.getFromDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Date");
            return true;
        }

        if (currentLeaveForm.getToDate() == null) {
            JsfUtil.addErrorMessage("Please Select to Date");
            return true;
        }

        if (currentLeaveForm.getFromDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Date");
            return true;
        }

        if (currentLeaveForm.getToDate() == null) {
            JsfUtil.addErrorMessage("Please Select to Date");
            return true;
        }

        if (currentLeaveForm.getFromDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Date");
            return true;
        }

        if (currentLeaveForm.getToDate() == null) {
            JsfUtil.addErrorMessage("Please Select to Date");
            return true;
        }

        if (checkingForLieLeave()) {
            return true;
        }
        if (staffAdditionalFormController.fetchCurrentSalaryCycle(currentLeaveForm.getFromDate()) != null) {
            SalaryCycle s = staffAdditionalFormController.fetchCurrentSalaryCycle(currentLeaveForm.getFromDate());
            //System.out.println("s.getWorkedFromDate() = " + s.getWorkedFromDate());
            Date nowDate = com.divudi.java.CommonFunctions.getEndOfDay();
            if (nowDate.getTime() > s.getDayOffPhToDate().getTime()) {
                double d = (nowDate.getTime() - s.getDayOffPhToDate().getTime()) / (1000 * 60 * 60 * 24);
                if (d > 3) {
                    JsfUtil.addErrorMessage("You Can't Add This Addional."
                            + "because you can add only additionls within 3 days after Day off / PH To Date");
                    return true;
                }
            }
        }
        
        if (staffAdditionalFormController.fetchCurrentSalaryCycle(currentLeaveForm.getFromDate()) != null) {
            StaffSalary s=humanResourceBean.getStaffSalary(currentLeaveForm.getStaff(), staffAdditionalFormController.fetchCurrentSalaryCycle(currentLeaveForm.getFromDate()));
            if (s.getId()!=null) {
                JsfUtil.addErrorMessage("You Can't Add This Addional."
                            + "because this salary was createed");
                    return true;
            }
        }

        return false;
    }

    public boolean checkingForLieLeave() {

        LeaveType ltp = currentLeaveForm.getLeaveType();
        StaffShift stf = currentLeaveForm.getStaffShift();

        if ((ltp == LeaveType.Lieu
                || ltp == LeaveType.LieuHalf)) {
            if (stf == null) {
                JsfUtil.addErrorMessage("Please Select Shift That Lie Entitled");
                return true;
            }

            Long datRang = commonFunctions.getDayCount(getCurrentLeaveForm().getFromDate(), getCurrentLeaveForm().getToDate());

//            if (datRang != 1) {
//                JsfUtil.addErrorMessage("Date range should be 1");
//                return true;
//            }
            if (ltp == LeaveType.Lieu && stf.getLieuQtyUtilized() != 0) {
                JsfUtil.addErrorMessage("You cant get Liue leave from this Shift");
                return true;
            }

            if (ltp == LeaveType.LieuHalf && stf.getLieuQtyUtilized() == 1) {
                JsfUtil.addErrorMessage("You cant get Liue leave from this Shift");
                return true;
            }
        }

        return false;
    }

    @Inject
    PhDateController phDateController;

    @EJB
    HumanResourceBean humanResourceBean;

    private List<StaffShift> fetchStaffShift(Date date, Staff staff) {
        String sql = "select s from StaffShift s "
                + " where s.retired=false "
                + " and s.shiftDate=:dt "
                + " and s.staff=:st ";
        HashMap hm = new HashMap();
        hm.put("dt", date);
        hm.put("st", staff);

        return staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
    }

    public void addLeaveDataToStaffShift(Date date, Staff staff, LeaveType ltp) {
        List<StaffShift> list = fetchStaffShift(date, staff);
        if (list == null) {
            return;
        }

        for (StaffShift ss : list) {
            ss.resetLeaveData(getCurrentLeaveForm().getLeaveType());
            ss.calLeaveTime();
            ss.calLieu();
            ss.setLeaveForm(currentLeaveForm);
            ss.setLeaveType(ltp);
            staffShiftFacade.edit(ss);
        }
    }

    public void removeLeaveDataFromStaffShift(Date date, Staff staff) {
        List<StaffShift> list = fetchStaffShift(date, staff);
        if (list == null) {
            return;
        }

        for (StaffShift ss : list) {
            ss.resetLeaveData(getCurrentLeaveForm().getLeaveType());
            ss.setLeaveType(null);
            staffShiftFacade.edit(ss);
        }
    }

    private StaffLeave fetchStaffLeave(Date date, Staff staff, LeaveType leaveType) {
        String sql = "select s from StaffLeave s"
                + " where s.staff=:st"
                + " and s.retired=false "
                + " and s.leaveDate=:dt "
                + " and s.leaveType=:lt";

        HashMap hm = new HashMap();
        hm.put("st", staff);
        hm.put("dt", date);
        hm.put("lt", leaveType);

        return staffLeaveFacade.findFirstByJpql(sql, hm, TemporalType.DATE);

    }

    private void saveStaffLeaves() {
        Calendar nowDate = Calendar.getInstance();
        nowDate.setTime(getCurrentLeaveForm().getFromDate());
        Calendar toDateCal = Calendar.getInstance();
        toDateCal.setTime(getCurrentLeaveForm().getToDate());
        while (toDateCal.getTime().after(nowDate.getTime())
                || toDateCal.get(Calendar.DATE) == nowDate.get(Calendar.DATE)) {

            StaffLeave stfLeave = fetchStaffLeave(nowDate.getTime(), getCurrentLeaveForm().getStaff(), getCurrentLeaveForm().getLeaveType());
            if (stfLeave != null) {
                nowDate.add(Calendar.DATE, 1);
                continue;
            }

            stfLeave = new StaffLeave();
            stfLeave.setCreatedAt(new Date());
            stfLeave.setCreater(sessionController.getLoggedUser());
            stfLeave.setLeaveType(getCurrentLeaveForm().getLeaveType());
            stfLeave.setRoster(getCurrentLeaveForm().getStaff().getRoster());
            stfLeave.setStaff(getCurrentLeaveForm().getStaff());
            stfLeave.setLeaveDate(nowDate.getTime());
            stfLeave.setForm(getCurrentLeaveForm());
            stfLeave.calLeaveQty();
            staffLeaveFacade.create(stfLeave);
            addLeaveDataToStaffShift(stfLeave.getLeaveDate(), stfLeave.getStaff(), stfLeave.getLeaveType());
            nowDate.add(Calendar.DATE, 1);
        }

        StaffShift staffShift = getCurrentLeaveForm().getStaffShift();

        if (staffShift != null) {
            staffShift.processLieuQtyUtilized(getCurrentLeaveForm().getLeaveType());
        }

    }

    public void saveLeaveform() {
        if (currentLeaveForm.getId() != null) {
            return;
        }

        if (errorCheck()) {
            return;
        }
        currentLeaveForm.setCreater(getSessionController().getLoggedUser());
        currentLeaveForm.setCreatedAt(new Date());

        if (currentLeaveForm.getId() == null) {
            getLeaveFormFacade().create(currentLeaveForm);
        } else {
            getLeaveFormFacade().edit(currentLeaveForm);
        }

        saveStaffLeaves();

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

    public void createleaveTable() {
        String sql;
        Map m = new HashMap();

        sql = " select l from LeaveForm l "
                + " where "
                + " l.createdAt between :fd and :td ";

        if (withOutretRierd) {
            sql += " and l.retired=false ";
        } else {
//            sql+=" and l.retired=true ";
        }

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        if (leaveType != null) {
            sql += " and l.leaveType=:lt ";
            m.put("lt", leaveType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        leaveForms = getLeaveFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createStaffleaveTable() {
        Date startTime = new Date();
        
        String sql;
        Map m = new HashMap();

        sql = " select l from StaffLeave l where "
                + " l.createdAt between :fd and :td ";

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        if (leaveType != null) {
            sql += " and l.leaveType=:lt ";
            m.put("lt", leaveType);
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and l.roster=:ros ";
            m.put("ros", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and l.staff.designation=:des ";
            m.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and l.roster.department=:dep ";
            m.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and l.roster.department.institution=:ins ";
            m.put("ins", getReportKeyWord().getInstitution());
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        staffLeaves = getStaffLeaveFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        
        

    }

//    public void createStaffleaveTablebyLeaveDate() {
//        String sql;
//        Map m = new HashMap();
//
//        sql = " select l from StaffLeave l where "
//                + " l.leaveDate between :fd and :td ";
//
//        if (staff != null) {
//            sql += " and l.staff=:st ";
//            m.put("st", staff);
//        }
//
//        if (approvedStaff != null) {
//            sql += " and l.approvedStaff=:app ";
//            m.put("app", approvedStaff);
//        }
//
//        if (leaveType != null) {
//            sql += " and l.leaveType=:lt ";
//            m.put("lt", leaveType);
//        }
//
//        m.put("fd", fromDate);
//        m.put("td", toDate);
//
//        staffLeaves = getStaffLeaveFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//
//    }
    public void createStaffleaveTablebyLeaveDate() {
        Date startTime = new Date();
        
        String sql;
        Map m = new HashMap();

        sql = " select l from StaffLeave l where "
                + " l.leaveDate between :fd and :td ";

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        if (leaveType != null) {
            sql += " and l.leaveType=:lt ";
            m.put("lt", leaveType);
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and l.roster=:ros ";
            m.put("ros", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and l.staff.designation=:des ";
            m.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and l.roster.department=:dep ";
            m.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and l.roster.department.institution=:ins ";
            m.put("ins", getReportKeyWord().getInstitution());
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        staffLeaves = getStaffLeaveFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        
        

    }

    public void saveStaffLeave() {
        if (staffLeave != null) {
            getStaffLeaveFacade().edit(staffLeave);
            JsfUtil.addSuccessMessage("Updated");
        }
    }

    public void createleaveTableApprovedDate() {
        String sql;
        Map m = new HashMap();

        sql = " select l from LeaveForm l where "
                + " l.approvedAt between :fd and :td ";

        if (withOutretRierd) {
            sql += " and l.retired=false ";
        } else {
//            sql+=" and l.retired=true ";
        }

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        if (leaveType != null) {
            sql += " and l.leaveType=:lt ";
            m.put("lt", leaveType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        leaveForms = getLeaveFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createleaveTableByShiftDate() {
        String sql;
        Map m = new HashMap();

        sql = " select l from LeaveForm l where "
                + " l.staffShift.shiftDate between :fd and :td ";

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        if (leaveType != null) {
            sql += " and l.leaveType=:lt ";
            m.put("lt", leaveType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        leaveForms = getLeaveFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createleaveTableShiftDate() {
        String sql;
        Map m = new HashMap();

        sql = " select l from LeaveForm l where "
                + " ((l.fromDate between :fd and :td)"
                + " or(l.toDate between :fd and :td)) "
                + " and type(l)!=:class";

        if (withOutretRierd) {
            sql += " and l.retired=false ";
        } else {
//            sql+=" and l.retired=true ";
        }

        m.put("class", LeaveFormSystem.class);

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        if (leaveType != null) {
            sql += " and l.leaveType=:lt ";
            m.put("lt", leaveType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        leaveForms = getLeaveFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void deleteStaffLeave(Form form) {
        String sql = "Select l from StaffLeave l where l.form=:frm ";
        HashMap nm = new HashMap();
        nm.put("frm", form);
        List<StaffLeave> list = staffLeaveFacade.findByJpql(sql, nm);
        for (StaffLeave stf : list) {
            stf.setRetired(true);
            stf.setRetiredAt(new Date());
            stf.setRetirer(sessionController.getLoggedUser());
            staffLeaveFacade.edit(stf);

            removeLeaveDataFromStaffShift(stf.getLeaveDate(), stf.getStaff());
        }
    }

    public boolean errorcheckDeleteLeaveFom() {
        if (currentLeaveForm == null) {
            JsfUtil.addErrorMessage("Nothing to Delete");
            return true;
        }

        if (currentLeaveForm.getRetireComments() == null || "".equals(currentLeaveForm.getRetireComments())) {
            JsfUtil.addErrorMessage("Enter a Comment");
            return true;
        }

        return false;
    }

    public void deleteLeaveForm() {
        if (errorcheckDeleteLeaveFom()) {
            return;
        }
        deleteStaffLeave(currentLeaveForm);
        currentLeaveForm.setRetired(true);
        currentLeaveForm.setRetirer(getSessionController().getLoggedUser());
        currentLeaveForm.setRetiredAt(new Date());
        getLeaveFormFacade().edit(currentLeaveForm);
        JsfUtil.addSuccessMessage("Sucessfuly Deleted.");
        clear();

    }

    public void viewLeaveForm(LeaveForm leaveForm) {
        currentLeaveForm = leaveForm;
        calLeaveCount();
        fetchStaffShift();
        fetchStaffShiftLie();
    }

    public void viewStaffLeave(StaffLeave leave) {
        staffLeave = leave;
    }

    public void clear() {
        currentLeaveForm = null;
        leaveEntitle = 0;
        leaved = 0;
        reportKeyWord = null;

    }

    public LeaveForm getCurrentLeaveForm() {
        if (currentLeaveForm == null) {
            currentLeaveForm = new LeaveForm();

        }
        return currentLeaveForm;
    }

    public void setCurrentLeaveForm(LeaveForm currentLeaveForm) {
        this.currentLeaveForm = currentLeaveForm;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public LeaveFormFacade getLeaveFormFacade() {
        return leaveFormFacade;
    }

    public void setLeaveFormFacade(LeaveFormFacade leaveFormFacade) {
        this.leaveFormFacade = leaveFormFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public Staff getApprovedStaff() {
        return approvedStaff;
    }

    public void setApprovedStaff(Staff approvedStaff) {
        this.approvedStaff = approvedStaff;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.java.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public List<LeaveForm> getLeaveForms() {
        return leaveForms;
    }

    public void setLeaveForms(List<LeaveForm> leaveForms) {
        this.leaveForms = leaveForms;
    }

    public StaffLeave getStaffLeave() {
        return staffLeave;
    }

    public void setStaffLeave(StaffLeave staffLeave) {
        this.staffLeave = staffLeave;
    }

    public List<StaffLeave> getStaffLeaves() {
        return staffLeaves;
    }

    public void setStaffLeaves(List<StaffLeave> staffLeaves) {
        this.staffLeaves = staffLeaves;
    }

    public boolean isWithOutretRierd() {
        return withOutretRierd;
    }

    public void setWithOutretRierd(boolean withOutretRierd) {
        this.withOutretRierd = withOutretRierd;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
    
}

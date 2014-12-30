/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Form;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.LeaveForm;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffLeaveEntitle;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.LeaveFormFacade;
import com.divudi.facade.StaffLeaveEntitleFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.util.JsfUtil;
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
import javax.persistence.Temporal;
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
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
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
        HashMap hm = new HashMap();
        hm.put("stf", getCurrentLeaveForm().getStaff());
        staffShiftsLie = staffShiftFacade.findBySQL(sql, hm);
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

        staffShifts = staffShiftFacade.findBySQL(sql, hm, 10);
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

    public StaffLeaveEntitle fetchLeaveEntitle(Staff staff, LeaveType leaveType) {
        List<LeaveType> list = leaveType.getLeaveTypes();

        String sql = "select  ss "
                + " from StaffLeaveEntitle ss"
                + " where ss.retired=false"
                + " and ss.staff=:stf"
                + " and ss.leaveType in :ltp ";
        HashMap hm = new HashMap();
        hm.put("stf", staff);
        hm.put("ltp", list);

        return staffLeaveEntitleFacade.findFirstBySQL(sql, hm);
    }

    public void calLeaveCount() {
        if (getCurrentLeaveForm().getStaff() == null) {
            UtilityController.addErrorMessage("Please Select Staff");
            return;
        }

        LeaveType leaveTypeLocal = getCurrentLeaveForm().getLeaveType();

        if (leaveTypeLocal == null) {
            UtilityController.addErrorMessage("Please Select Leave Type");
            return;
        }

        StaffLeaveEntitle staffLeaveEntitle = fetchLeaveEntitle(getCurrentLeaveForm().getStaff(), getCurrentLeaveForm().getLeaveType());

        if (!leaveTypeLocal.isExceptionalLeave() && staffLeaveEntitle == null) {
            UtilityController.addErrorMessage("Please Set Leave Enttile count for this Staff in Administration");
            return;
        }

        if (staffLeaveEntitle != null) {
            leaveEntitle = staffLeaveEntitle.getCount();
        }

        leaved = humanResourceBean.calStaffLeave(getCurrentLeaveForm().getStaff(), leaveTypeLocal,
                getCommonFunctions().getFirstDayOfYear(new Date()),
                getCommonFunctions().getLastDayOfYear(new Date()));

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

        if (checkingForLieLeave()) {
            return true;
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

        return staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void addLeaveDataToStaffShift(Date date, Staff staff, LeaveType ltp) {
        List<StaffShift> list = fetchStaffShift(date, staff);
        if (list == null) {
            return;
        }

        for (StaffShift ss : list) {
//            ss.resetLeaveData();
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
            ss.resetLeaveData();
            ss.setLeaveType(null);
            staffShiftFacade.edit(ss);
        }
    }

    private void saveStaffLeaves() {
        Calendar nowDate = Calendar.getInstance();
        nowDate.setTime(getCurrentLeaveForm().getFromDate());
        Calendar toDateCal = Calendar.getInstance();
        toDateCal.setTime(getCurrentLeaveForm().getToDate());
        while (toDateCal.getTime().after(nowDate.getTime())
                || toDateCal.get(Calendar.DATE) == nowDate.get(Calendar.DATE)) {

            StaffLeave staffLeave = new StaffLeave();
            staffLeave.setCreatedAt(new Date());
            staffLeave.setCreater(sessionController.getLoggedUser());
            staffLeave.setLeaveType(getCurrentLeaveForm().getLeaveType());
            staffLeave.setRoster(getCurrentLeaveForm().getStaff().getRoster());
            staffLeave.setStaff(getCurrentLeaveForm().getStaff());
            staffLeave.setLeaveDate(nowDate.getTime());
            staffLeave.setForm(getCurrentLeaveForm());
            staffLeave.calLeaveQty();
            staffLeaveFacade.create(staffLeave);

            addLeaveDataToStaffShift(staffLeave.getLeaveDate(), staffLeave.getStaff(), staffLeave.getLeaveType());

            nowDate.add(Calendar.DATE, 1);
        }

        StaffShift staffShift = getCurrentLeaveForm().getStaffShift();

        if (staffShift != null) {
            staffShift.processLieuQtyUtilized(getCurrentLeaveForm().getLeaveType());
        }

    }

    public void saveLeaveform() {
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

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        leaveForms = getLeaveFormFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void createleaveTableApprovedDate() {
        String sql;
        Map m = new HashMap();

        sql = " select l from LeaveForm l where "
                + " l.approvedAt between :fd and :td ";

        if (staff != null) {
            sql += " and l.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and l.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        leaveForms = getLeaveFormFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void deleteStaffLeave(Form form) {
        String sql = "Select l from StaffLeave l where l.form=:frm ";
        HashMap nm = new HashMap();
        nm.put("frm", form);
        List<StaffLeave> list = staffLeaveFacade.findBySQL(sql, nm);
        for (StaffLeave stf : list) {
            stf.setRetired(true);
            stf.setRetiredAt(new Date());
            stf.setRetirer(sessionController.getLoggedUser());
            staffLeaveFacade.edit(stf);

            removeLeaveDataFromStaffShift(stf.getLeaveDate(), stf.getStaff());
        }
    }

    public void deleteLeaveForm() {
        if (currentLeaveForm != null) {
            deleteStaffLeave(currentLeaveForm);

            currentLeaveForm.setRetired(true);
            currentLeaveForm.setRetirer(getSessionController().getLoggedUser());
            currentLeaveForm.setRetiredAt(new Date());
            getLeaveFormFacade().edit(currentLeaveForm);
            JsfUtil.addSuccessMessage("Sucessfuly Deleted.");
            clear();
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete.");
        }
    }

    public void viewLeaveForm(LeaveForm leaveForm) {
        currentLeaveForm = leaveForm;
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

}

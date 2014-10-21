/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.LeaveType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.LeaveForm;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.facade.LeaveFormFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
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

    @EJB
    CommonFunctions commonFunctions;
    List<LeaveForm> leaveForms;
    Staff approvedStaff;
    Staff staff;
    Date fromDate;
    Date toDate;
    @Enumerated(EnumType.STRING)
    LeaveType leaveType;

    public StaffLeaveFacade getStaffLeaveFacade() {
        return staffLeaveFacade;
    }

    public void setStaffLeaveFacade(StaffLeaveFacade staffLeaveFacade) {
        this.staffLeaveFacade = staffLeaveFacade;
    }

    public StaffLeaveApplicationFormController() {
    }

    public boolean errorCheck() {
        if (currentLeaveForm.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }
        if (getCurrentLeaveForm().getLeaveType() == null) {
            JsfUtil.addErrorMessage("Please Enter Leave Type");
            return true;
        }

        if (currentLeaveForm.getRequestedDate() == null) {
            JsfUtil.addErrorMessage("Please Select Date");
            return true;
        }
//        if ((getCurrentLeaveForm().getStaffLeave().getLeaveType() == LeaveType.Lieu) && (currentLeaveForm.getForDate() == null)) {
//            JsfUtil.addErrorMessage("Please Select \"Leave For Date\" ");
//            return true;
//        }
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

        return false;
    }

    @Inject
    PhDateController phDateController;

    @EJB
    HumanResourceBean humanResourceBean;

    private void saveStaffLeaves() {
        Date nowDate = fromDate;

        while (toDate.after(nowDate)) {
            Calendar nc = Calendar.getInstance();
            nc.setTime(nowDate);

            boolean dontAddLeave = false;
            switch (nc.get(Calendar.DAY_OF_WEEK)) {
//                case Calendar.SATURDAY:                    
//                    continueFlag = true;
//                    break;
                case Calendar.SUNDAY:
                    dontAddLeave = true;
                    break;

            }

            DayType dayType = humanResourceBean.isHolidayWithDayType(nowDate);

            if (dayType == null) {
                return;
            } else {
                switch (dayType) {
                    case MurchantileHoliday:
                        dontAddLeave = true;
                        break;
                    case Poya:
                        dontAddLeave = true;
                        break;
                    case PublicHoliday:
                        dontAddLeave = true;
                        break;
                }
            }

            if (!dontAddLeave) {
                StaffLeave staffLeave = new StaffLeave();
                staffLeave.setCreatedAt(new Date());
                staffLeave.setCreater(sessionController.getLoggedUser());
                staffLeave.setLeaveType(getCurrentLeaveForm().getLeaveType());
                staffLeave.setLeaveDate(nowDate);
                staffLeave.setForm(getCurrentLeaveForm());
                staffLeave.calLeaveQty();
                staffLeaveFacade.create(staffLeave);
            }

            nc.add(Calendar.DATE, 1);
            nowDate = nc.getTime();
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

    public void deleteLeaveForm() {
        if (currentLeaveForm != null) {
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

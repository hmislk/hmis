/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.Times;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftExtra;
import com.divudi.facade.AdditionalFormFacade;
import com.divudi.facade.ShiftFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Named(value = "staffAdditionalFormController")
@SessionScoped
public class StaffAdditionalFormController implements Serializable {

    private AdditionalForm currentAdditionalForm;
    @EJB
    private AdditionalFormFacade additionalFormFacade;
    @Inject
    private SessionController sessionController;
    Date date;
    List<StaffShift> staffShifts;
    @EJB
    StaffShiftFacade staffShiftFacade;

    @EJB
    CommonFunctions commonFunctions;
    List<AdditionalForm> additionalForms;
    Department department;
    Staff staff;
    Staff approvedStaff;
    Date fromDate;
    Date toDate;

    public void deleteAdditionalForm() {
        if (getCurrentAdditionalForm() != null) {
            if (getCurrentAdditionalForm().getStaffShift() != null) {
                getCurrentAdditionalForm().getStaffShift().resetExtraTime();
                staffShiftFacade.edit(getCurrentAdditionalForm().getStaffShift());
            }

            currentAdditionalForm.setRetired(true);
            currentAdditionalForm.setRetirer(getSessionController().getLoggedUser());
            currentAdditionalForm.setRetiredAt(new Date());
            getAdditionalFormFacade().edit(currentAdditionalForm);
            JsfUtil.addSuccessMessage("Sucessfuly Deleted.");
            clear();
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete.");
        }
    }

    public void createAmmendmentTable() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.createdAt between :fd and :td ";

        if (department != null) {
            sql += " and a.department=:dept ";
            m.put("dept", department);
        }

        if (staff != null) {
            sql += " and a.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and a.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        additionalForms = getAdditionalFormFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void createAmmendmentTableApprovedDate() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.approvedAt between :fd and :td ";

        if (department != null) {
            sql += " and a.department=:dept ";
            m.put("dept", department);
        }

        if (staff != null) {
            sql += " and a.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and a.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        additionalForms = getAdditionalFormFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void viewAdditionalForm(AdditionalForm additionalForm) {
        date = additionalForm.getFromTime();
        currentAdditionalForm = additionalForm;
    }

    public void fetchStaffShift() {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shift is not null "
                + " and c.shiftDate between :fd and :td "
                + " and c.staff=:stf ";

        hm.put("fd", getDate());
        hm.put("td", getDate());
        hm.put("stf", getCurrentAdditionalForm().getStaff());

        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
    }

    public List<StaffShift> getStaffShifts() {
        return staffShifts;
    }

    public void setStaffShifts(List<StaffShift> staffShifts) {
        this.staffShifts = staffShifts;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public StaffAdditionalFormController() {
    }

    public void clear() {
        currentAdditionalForm = null;
    }

    public boolean errorCheck() {
        if (currentAdditionalForm.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }

        if (currentAdditionalForm.getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Form Number");
            return true;
        }

        if (currentAdditionalForm.getFromTime() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }
        if (currentAdditionalForm.getToTime() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }
        if (currentAdditionalForm.getApprovedStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Person");
            return true;
        }
        if (currentAdditionalForm.getApprovedAt() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Date");
            return true;
        }
        if (currentAdditionalForm.getComments() == null || "".equals(currentAdditionalForm.getComments())) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return true;
        }

        if (currentAdditionalForm.getTimes() == null) {
            JsfUtil.addErrorMessage("Please Select Time Type");
            return true;
        }

        if (date == null) {
            JsfUtil.addErrorMessage("Please Select Date");
            return true;
        }

        if (getCurrentAdditionalForm().getTimes() != Times.All && currentAdditionalForm.getStaffShift() == null) {
            JsfUtil.addErrorMessage("Please Select Staff Shiftt");
            return true;
        }

        if (getCurrentAdditionalForm().getTimes() == Times.All && currentAdditionalForm.getStaffShift() != null) {
            JsfUtil.addErrorMessage("Please Un Select Staff Shiftt");
            return true;
        }

        //NEED To Check StaffSHift  if not selected is there any shift time on that day
        return false;
    }

    @Inject
    PhDateController phDateController;

    public void saveAdditionalForm() {
        if (errorCheck()) {
            return;
        }

        DayType dayType = phDateController.getHolidayType(date);
        Shift shift = fetchShift(staff.getRoster(), dayType);
        
        currentAdditionalForm.setCreatedAt(new Date());
        currentAdditionalForm.setCreater(getSessionController().getLoggedUser());
        if (currentAdditionalForm.getId() == null) {
            getAdditionalFormFacade().create(currentAdditionalForm);
        } else {
            getAdditionalFormFacade().edit(currentAdditionalForm);
        }

        if (currentAdditionalForm.getStaffShift() != null) {
            currentAdditionalForm.getStaffShift().resetExtraTime();
            currentAdditionalForm.getStaffShift().setAdditionalForm(currentAdditionalForm);
            staffShiftFacade.edit(currentAdditionalForm.getStaffShift());
        } else {
            StaffShiftExtra staffShiftExtra = new StaffShiftExtra();
            staffShiftExtra.setCreatedAt(new Date());
            staffShiftExtra.setCreater(sessionController.getLoggedUser());
            staffShiftExtra.setAdditionalForm(currentAdditionalForm);
            staffShiftExtra.setStaff(currentAdditionalForm.getStaff());
            staffShiftExtra.setShiftDate(date);
            staffShiftExtra.setShift(shift);
            staffShiftExtra.setShiftStartTime(currentAdditionalForm.getFromTime());
            staffShiftExtra.setShiftEndTime(currentAdditionalForm.getToTime());
            staffShiftFacade.create(staffShiftExtra);

            currentAdditionalForm.setStaffShift(staffShiftExtra);
            additionalFormFacade.edit(currentAdditionalForm);
        }

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

    @EJB
    ShiftFacade shiftFacade;

    private Shift fetchShift(Roster roster, DayType dayType) {
        if (dayType == null || roster == null) {
            return null;
        }

        String sql = "select s from  Shift s "
                + " where s.retired=false "
                + " and s.roster=:rs"
                + " and s.dayType=:dtp ";
        HashMap hm = new HashMap();
        hm.put("rs", roster);
        hm.put("dtp", dayType);

        Shift sh = shiftFacade.findFirstBySQL(sql, hm, TemporalType.DATE);

        if (sh == null) {
            sh = new Shift();
            sh.setCreatedAt(new Date());
            sh.setCreater(sessionController.getLoggedUser());
            sh.setDayType(dayType);
            sh.setRoster(roster);
            sh.setName(dayType.toString());
            sh.setStartingTime(date);
            sh.setEndingTime(date);
            shiftFacade.create(sh);
        }

        return sh;
    }

    public AdditionalForm getCurrentAdditionalForm() {
        if (currentAdditionalForm == null) {
            currentAdditionalForm = new AdditionalForm();
        }
        return currentAdditionalForm;
    }

    public void setCurrentAdditionalForm(AdditionalForm currentAdditionalForm) {
        this.currentAdditionalForm = currentAdditionalForm;
    }

    public AdditionalFormFacade getAdditionalFormFacade() {
        return additionalFormFacade;
    }

    public void setAdditionalFormFacade(AdditionalFormFacade additionalFormFacade) {
        this.additionalFormFacade = additionalFormFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<AdditionalForm> getAdditionalForms() {
        return additionalForms;
    }

    public void setAdditionalForms(List<AdditionalForm> additionalForms) {
        this.additionalForms = additionalForms;
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
        this.staff = staff;
    }

    public Staff getApprovedStaff() {
        return approvedStaff;
    }

    public void setApprovedStaff(Staff approvedStaff) {
        this.approvedStaff = approvedStaff;
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

}

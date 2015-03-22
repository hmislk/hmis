/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.SystemTimeStamp;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.Times;
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
import com.divudi.java.CommonFunctions;
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
    SystemTimeStamp fromSystemTimeStamp;
    SystemTimeStamp toSystemTimeStamp;

    List<AdditionalForm> additionalForms;
    Department department;
    Staff staff;
    Staff approvedStaff;
    Date fromDate;
    Date toDate;

    public void timeEnterListenerFrom() {
//        System.err.println("Starting From ");        
        getCurrentAdditionalForm().setFromTime(getFromSystemTimeStamp().getTime());
//        System.err.println("Ending From ");
    }

    public void timeEnterListenerTo() {
        System.err.println("Starting To ");

        getCurrentAdditionalForm().setToTime(getToSystemTimeStamp().getTime());
        System.err.println("Ending To ");
    }

    public void timeSelectListenerFrom() {
//        System.err.println("Starting Select From ");                
        getFromSystemTimeStamp().processTime(getCurrentAdditionalForm().getFromTime());
//        System.err.println("Ending Select From ");
    }

    public void timeSelectListenerTo() {
//        System.err.println("Starting Select To ");                
        getToSystemTimeStamp().processTime(getCurrentAdditionalForm().getToTime());
//        System.err.println("Ending Select To ");
    }

    public boolean errorCheckAdditionalForm() {
        if (getCurrentAdditionalForm() == null) {
            JsfUtil.addErrorMessage("Nothing to Delete");
            return true;
        }

        if (getCurrentAdditionalForm().getRetireComments() == null) {
            JsfUtil.addErrorMessage("Nothing to Delete");
            return true;
        }

        if (getCurrentAdditionalForm().getStaffShift() == null) {
            JsfUtil.addErrorMessage("Nothing to Delete");
            return true;
        }

        return false;
    }

    public void onDateSelect() {
        getCurrentAdditionalForm().setFromTime(date);
        getCurrentAdditionalForm().setToTime(date);

        getFromSystemTimeStamp().processTime(date);
        getToSystemTimeStamp().processTime(date);

    }

    public void deleteAdditionalForm() {
        if (getCurrentAdditionalForm() != null) {
            if (getCurrentAdditionalForm().getStaffShift() != null) {
                getCurrentAdditionalForm().getStaffShift().resetExtraTime();

                if (getCurrentAdditionalForm().getStaffShift() instanceof StaffShiftExtra) {
                    getCurrentAdditionalForm().getStaffShift().setRetired(true);
                    getCurrentAdditionalForm().getStaffShift().setRetirer(sessionController.getLoggedUser());
                }
                
                if(getCurrentAdditionalForm().getStaffShift().getReferenceStaffShift()!=null){
                    getCurrentAdditionalForm().getStaffShift().getReferenceStaffShift().setRetired(false);
                    staffShiftFacade.edit(getCurrentAdditionalForm().getStaffShift().getReferenceStaffShift());
                }

                staffShiftFacade.edit(getCurrentAdditionalForm().getStaffShift());
            }

            currentAdditionalForm.setRetired(true);
            currentAdditionalForm.setRetirer(getSessionController().getLoggedUser());
            currentAdditionalForm.setRetiredAt(new Date());
            getAdditionalFormFacade().edit(currentAdditionalForm);
            JsfUtil.addSuccessMessage("Sucessfuly Deleted.");
            clear();
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }
    }

    public void createAmmendmentTable() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.createdAt between :fd and :td"
                + " and a.times=:tm ";

        m.put("tm", Times.All);
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

    public void createAmmendmentTableShiftDate() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.staffShift.shiftDate between :fd and :td ";

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

    public void createAmmendmentTableShiftDateExtraShift() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.staffShift.shiftDate between :fd and :td "
                + " and a.times!=:tm";

        m.put("tm", Times.All);

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

    public void createAmmendmentTableShiftDateAdditionalShift() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.staffShift.shiftDate between :fd and :td "
                + " and a.times=:tm";

        m.put("tm", Times.All);

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

        additionalForms = getAdditionalFormFacade().findBySQL(sql, m, TemporalType.DATE);

    }

    public void viewAdditionalForm(AdditionalForm additionalForm) {
        date = additionalForm.getFromTime();
        fetchStaffShift(date, additionalForm.getStaff());
        currentAdditionalForm = additionalForm;
//        shift = currentAdditionalForm.getStaffShift().getShift();
    }

    public void fetchStaffShift() {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and type(c)!=:cl "
                + " and c.shift is not null "
                //                + " and c.shift.dayType!=:dtp1 "
                //                + " and c.shift.dayType!=:dtp2 "
                + " and c.shiftDate =:dt "
                + " and c.staff=:stf ";

//        hm.put("dtp1", DayType.DayOff);
//        hm.put("dtp2", DayType.SleepingDay);
        hm.put("cl", StaffShiftExtra.class);
        hm.put("dt", getDate());
        hm.put("stf", getCurrentAdditionalForm().getStaff());

        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

        hm.clear();
        sql = "select c from "
                + " StaffShiftExtra c"
                + " where c.retired=false "
                + " and c.shiftDate =:dt "
                + " and c.staff=:stf ";
        hm.put("dt", getDate());
        hm.put("stf", getCurrentAdditionalForm().getStaff());

        staffShifts.addAll(staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE));

    }

    public void fetchStaffShiftNotNormal() {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and type(c)!=:cl "
                + " and c.shift is not null "
                + " and c.shift.dayType!=:dtp "
                + " and c.shiftDate =:dt "
                + " and c.staff=:stf ";

        hm.put("dtp", DayType.Normal);
        hm.put("cl", StaffShiftExtra.class);
        hm.put("dt", getDate());
        hm.put("stf", getCurrentAdditionalForm().getStaff());

        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void fetchStaffShift(Date date, Staff staff) {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                //                + " and c.shift is not null "
                + " and c.shiftDate=:dt "
                + " and c.staff=:stf ";

        hm.put("dt", date);
        hm.put("stf", staff);

        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
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
        staffShifts = null;
        date = null;
        newStaffShift = false;
    }

    public boolean errorCheckExtra() {
        if (currentAdditionalForm.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }

        if (currentAdditionalForm.getStaffShift() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff Shift");
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

        Long timePeriod = CommonFunctions.calTimePeriod(currentAdditionalForm.getFromTime(), currentAdditionalForm.getToTime());
        if (timePeriod < 0 || (timePeriod / 24) > 1) {
            JsfUtil.addErrorMessage("Please Check  From Time and To Time Range");
            return true;
        }

        if (currentAdditionalForm.getApprovedStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Person");
            return true;
        }
//        if (currentAdditionalForm.getApprovedAt() == null) {
//            JsfUtil.addErrorMessage("Please Select Approved Date");
//            return true;
//        }
//        if (currentAdditionalForm.getComments() == null || "".equals(currentAdditionalForm.getComments())) {
//            JsfUtil.addErrorMessage("Please Add Comment");
//            return true;
//        }

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

    public boolean errorCheckShift() {
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
        Long timePeriod = CommonFunctions.calTimePeriod(currentAdditionalForm.getFromTime(), currentAdditionalForm.getToTime());
        if (timePeriod <= 0 || (timePeriod / 24) > 1) {
            JsfUtil.addErrorMessage("Please Check  From Time and To Time Range");
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

//        if (currentAdditionalForm.getTimes() == null) {
//            JsfUtil.addErrorMessage("Please Select Time Type");
//            return true;
//        }
        if (date == null) {
            JsfUtil.addErrorMessage("Please Select Date");
            return true;
        }

//        if (getCurrentAdditionalForm().getTimes() != Times.All && currentAdditionalForm.getStaffShift() == null) {
//            JsfUtil.addErrorMessage("Please Select Staff Shiftt");
//            return true;
//        }
//
//        if (getCurrentAdditionalForm().getTimes() == Times.All && currentAdditionalForm.getStaffShift() != null) {
//            JsfUtil.addErrorMessage("Please Un Select Staff Shiftt");
//            return true;
//        }
        //NEED To Check StaffSHift  if not selected is there any shift time on that day
        return false;
    }

    @Inject
    PhDateController phDateController;

    public void saveAdditionalFormExtra() {
        if (errorCheckExtra()) {
            return;
        }

        currentAdditionalForm.setCreatedAt(new Date());
        currentAdditionalForm.setCreater(getSessionController().getLoggedUser());
        if (currentAdditionalForm.getId() == null) {
            getAdditionalFormFacade().create(currentAdditionalForm);
        } else {
            getAdditionalFormFacade().edit(currentAdditionalForm);
        }

        currentAdditionalForm.getStaffShift().resetExtraTime();
        currentAdditionalForm.getStaffShift().setAdditionalForm(currentAdditionalForm);
        staffShiftFacade.edit(currentAdditionalForm.getStaffShift());

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

    boolean newStaffShift;

    public boolean isNewStaffShift() {
        return newStaffShift;
    }

    public void setNewStaffShift(boolean newStaffShift) {
        this.newStaffShift = newStaffShift;
    }

    public void saveAdditionalFormShift() {
        if (errorCheckShift()) {
            return;
        }

        Shift shift = null;

        if (!isNewStaffShift()) {
            if (currentAdditionalForm.getStaffShift() == null) {
                UtilityController.addErrorMessage("Please Un Select Staff Shift");
                return;
            }

            DayType dayType = phDateController.getHolidayType(date);
            shift = fetchShift(currentAdditionalForm.getStaff().getRoster(), dayType);

            if (shift == null) {
                shift = fetchShift(currentAdditionalForm.getStaff().getRoster(), DayType.Extra);
            }

        } else {
            if (currentAdditionalForm.getStaffShift() != null) {
                UtilityController.addErrorMessage("Please Select Staff Shift");
                return;
            }
        }

        currentAdditionalForm.setTimes(Times.All);
        currentAdditionalForm.setCreatedAt(new Date());
        currentAdditionalForm.setCreater(getSessionController().getLoggedUser());
        if (currentAdditionalForm.getId() == null) {
            getAdditionalFormFacade().create(currentAdditionalForm);
        } else {
            getAdditionalFormFacade().edit(currentAdditionalForm);
        }

        StaffShiftExtra staffShiftExtra = new StaffShiftExtra();
        staffShiftFacade.create(staffShiftExtra);

        if (currentAdditionalForm.getStaffShift() != null) {
            staffShiftExtra.copy(currentAdditionalForm.getStaffShift());

            if (staffShiftExtra.getPreviousStaffShift() != null) {
                staffShiftExtra.getPreviousStaffShift().setNextStaffShift(staffShiftExtra);

                staffShiftFacade.edit(staffShiftExtra.getPreviousStaffShift());
            }
            
            staffShiftExtra.setReferenceStaffShift(currentAdditionalForm.getStaffShift());

            currentAdditionalForm.getStaffShift().setRetired(true);
            currentAdditionalForm.getStaffShift().setRetiredAt(new Date());
            currentAdditionalForm.getStaffShift().setRetirer(sessionController.getLoggedUser());
            staffShiftFacade.edit(currentAdditionalForm.getStaffShift());
        }
//        } else {

        staffShiftExtra.setStaff(currentAdditionalForm.getStaff());
        staffShiftExtra.setRoster(currentAdditionalForm.getStaff().getRoster());

        if (shift != null) {
            staffShiftExtra.setShift(shift);
        }
//        }

        staffShiftExtra.setCreatedAt(new Date());
        staffShiftExtra.setCreater(sessionController.getLoggedUser());
        staffShiftExtra.setAdditionalForm(currentAdditionalForm);
        staffShiftExtra.setShiftDate(date);
        staffShiftExtra.setShiftStartTime(currentAdditionalForm.getFromTime());
        staffShiftExtra.setShiftEndTime(currentAdditionalForm.getToTime());
        
        staffShiftFacade.edit(staffShiftExtra);

        currentAdditionalForm.setStaffShift(staffShiftExtra);
        additionalFormFacade.edit(currentAdditionalForm);

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
            sh.setStartingTime(null);
            sh.setEndingTime(null);
            shiftFacade.create(sh);
        }

        return sh;
    }

    private Shift fetchShift(Roster roster) {
        if (roster == null) {
            return null;
        }

        String sql = "select s from  Shift s "
                + " where s.retired=false "
                + " and s.roster=:rs"
                + " and (s.dayType=:dtp1 or s.dayType=:dtp2 )";
        HashMap hm = new HashMap();
        hm.put("rs", roster);
        hm.put("dtp1", DayType.DayOff);
        hm.put("dtp2", DayType.SleepingDay);

        Shift sh = shiftFacade.findFirstBySQL(sql, hm, TemporalType.DATE);

        return sh;
    }

    private Shift fetchShift(Roster roster, Date date) {
        if (roster == null) {
            return null;
        }

        String sql = "select s from  Shift s "
                + " where s.retired=false "
                + " and s.roster=:rs"
                + " and (s.dayType=:dtp1 or s.dayType=:dtp2 )";
        HashMap hm = new HashMap();
        hm.put("rs", roster);
        hm.put("dtp1", DayType.DayOff);
        hm.put("dtp2", DayType.SleepingDay);

        Shift sh = shiftFacade.findFirstBySQL(sql, hm, TemporalType.DATE);

        return sh;
    }

    List<Shift> shifts;

    public PhDateController getPhDateController() {
        return phDateController;
    }

    public void setPhDateController(PhDateController phDateController) {
        this.phDateController = phDateController;
    }

    public ShiftFacade getShiftFacade() {
        return shiftFacade;
    }

    public void setShiftFacade(ShiftFacade shiftFacade) {
        this.shiftFacade = shiftFacade;
    }

    public List<Shift> getShifts() {
        return shifts;
    }

    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
    }

//    public void fetchShift() {
//        if (getCurrentAdditionalForm().getStaff() == null) {
//            UtilityController.addErrorMessage("Please Select Staff");
//            return;
//        }
//
//        if (getDate() == null) {
//            UtilityController.addErrorMessage("Please Select Date");
//            return;
//        }
//
//        shifts = new ArrayList<>();
//
//        DayType dayType = phDateController.getHolidayType(date);
//        shift = fetchShiftForPh(currentAdditionalForm.getStaff().getRoster(), dayType);
//
//        if (shift != null) {
//            shifts.add(shift);
//            return;
//        }
//
//        HashMap hm = new HashMap();
//        String sql = " select c from "
//                + " Shift c "
//                + " where c.retired=false "
//                + " and c.dayType in :dtp"
//                + " and c.roster=:rs ";
//
//        hm.put("dtp", new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.Poya, DayType.SleepingDay});
//        hm.put("rs", getCurrentAdditionalForm().getStaff().getRoster());
//        shifts = shiftFacade.findBySQL(sql, hm);
//    }
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
            fromDate = CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public SystemTimeStamp getFromSystemTimeStamp() {
        if (fromSystemTimeStamp == null) {
            fromSystemTimeStamp = new SystemTimeStamp();
        }
        return fromSystemTimeStamp;
    }

    public void setFromSystemTimeStamp(SystemTimeStamp fromSystemTimeStamp) {
        this.fromSystemTimeStamp = fromSystemTimeStamp;
    }

    public SystemTimeStamp getToSystemTimeStamp() {
        if (toSystemTimeStamp == null) {
            toSystemTimeStamp = new SystemTimeStamp();
        }

        return toSystemTimeStamp;
    }

    public void setToSystemTimeStamp(SystemTimeStamp toSystemTimeStamp) {
        this.toSystemTimeStamp = toSystemTimeStamp;
    }

}

/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.SystemTimeStamp;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.hr.Times;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.entity.hr.HrForm;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftExtra;
import com.divudi.facade.AdditionalFormFacade;
import com.divudi.facade.HrFormFacade;
import com.divudi.facade.SalaryCycleFacade;
import com.divudi.facade.ShiftFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
    @Inject
    CommonController commonController;
    Date date;
    List<StaffShift> staffShifts;
    List<AditionalWithTime> aditionalWithTimes;
    @EJB
    StaffShiftFacade staffShiftFacade;
    @EJB
    SalaryCycleFacade salaryCycleFacade;
    @EJB
    HumanResourceBean humanResourceBean;

    SystemTimeStamp fromSystemTimeStamp;
    SystemTimeStamp toSystemTimeStamp;

    List<AdditionalForm> additionalForms;
    Department department;
    Staff staff;
    Staff approvedStaff;
    Date fromDate;
    Date toDate;

    double totalInTime;
    double totalOutTime;
    double totalAllTime;

    public void timeSelectListener() {
        if (getCurrentAdditionalForm().getStaffShift() == null) {
            return;
        }

        Times times = getCurrentAdditionalForm().getTimes();
        if (times == null) {
            return;
        }

        if (times == Times.inTime && getCurrentAdditionalForm().getStaffShift().getShiftStartTime() != null) {
            getCurrentAdditionalForm().setToTime(getCurrentAdditionalForm().getStaffShift().getShiftStartTime());
            getToSystemTimeStamp().processTime(getCurrentAdditionalForm().getToTime());
            return;
        }

        if (times == Times.outTime && getCurrentAdditionalForm().getStaffShift().getShiftEndTime() != null) {
            getCurrentAdditionalForm().setFromTime(getCurrentAdditionalForm().getStaffShift().getShiftEndTime());
            getFromSystemTimeStamp().processTime(getCurrentAdditionalForm().getFromTime());
//            return;
        }

    }

    public void timeEnterListenerFrom() {
//        System.err.println("Starting From ");        
        getCurrentAdditionalForm().setFromTime(getFromSystemTimeStamp().getTime());
//        System.err.println("Ending From ");
    }

    public void timeEnterListenerTo() {

        getCurrentAdditionalForm().setToTime(getToSystemTimeStamp().getTime());
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

                if (getCurrentAdditionalForm().getStaffShift().getReferenceStaffShift() != null) {
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
        List<StaffShift> tmp = getStaffShiftFacade().findByJpqlWithoutCache(sql, hm, TemporalType.DATE);
//        System.err.println("fetchStaffShiftWithShift:: " + tmp);
        return tmp;
    }

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

    public String createKeyWord(Map m) {
        String sql = "";
        if (getReportKeyWord().getInstitution() != null) {
            sql += " and a.staffShift.staff.workingDepartment.institution=:ins ";
            m.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and a.staffShift.staff.workingDepartment=:dept ";
            m.put("dept", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getTimes() != null) {
            sql += " and a.times=:t ";
            m.put("t", getReportKeyWord().getTimes());
        }

        if (getReportKeyWord().getDayTypes() != null && !Arrays.asList(getReportKeyWord().getDayTypes()).isEmpty()) {
            sql += " and a.staffShift.dayType in :dtp ";
            m.put("dtp", Arrays.asList(getReportKeyWord().getDayTypes()));
        }

        if (getReportKeyWord().getStaff() != null) {
            sql += " and a.staffShift.staff=:st ";
            m.put("st", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and a.staffShift.staff.staffCategory=:stCat ";
            m.put("stCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and a.staffShift.staff.designation=:des ";
            m.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and a.staffShift.roster=:rs ";
            m.put("rs", getReportKeyWord().getRoster());
        }

        return sql;
    }

    public void searchByCreatedDate() {
        Date startTime = new Date();
        
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where"
                + " a.retired=false "
                + " and a.createdAt between :fd and :td ";
//                + " and a.times=:tm ";
        m.put("fd", fromDate);
        m.put("td", toDate);
//        m.put("tm", Times.All);
        sql += createKeyWord(m);

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

//        calMinitsAditional(additionalForms);
        
        
    }

    public void calTotals(List<AdditionalForm> list) {
        for (AdditionalForm a : list) {
            totalInTime = 0.0;
            totalOutTime = 0.0;
            totalAllTime = 0.0;
            if (!(a.getTimes() == Times.All && (a.getStaffShift().getDayType() == DayType.Poya || a.getStaffShift().getDayType() == DayType.DayOff || a.getStaffShift().getDayType() == DayType.MurchantileHoliday))) {
//                totalInTime+=;
//                totalOutTime+=;
//                totalAllTime+=;
            }
        }
    }
    List<HrForm> hrForms;
    @EJB
    HrFormFacade hrFormFacade;

    public void searchFormByCreatedDate() {
        Date startTime = new Date();
        
        String sql = "";
        Map m = new HashMap();

        sql = " select a from HrForm a where "
                + " a.createdAt between :fd and :td"
                + " and ( a.staffShift.startRecord is null "
                + " or  a.staffShift.endRecord is null "
                + " or a.staffShift.startRecord.recordTimeStamp is null "
                + " or a.staffShift.endRecord.recordTimeStamp is null ) ";
//                + " and a.times=:tm ";
        m.put("fd", fromDate);
        m.put("td", toDate);
//        m.put("tm", Times.All);
        sql += createKeyWord(m);

        hrForms = hrFormFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

//        calMinitsAditional(additionalForms);
        
        
    }

//    public void searchFormByApprovedDate() {
//        String sql = "";
//        Map m = new HashMap();
//
//        sql = " select a from HrForm a where "
//                + " a.approvedDate between :fd and :td"
//                + " and (a.staffShift.startRecord.recordTimeStamp is null or"
//                + " a.staffShift.endRecord.recordTimeStamp is null ) ";
////                + " and a.times=:tm ";
//        m.put("fd", fromDate);
//        m.put("td", toDate);
////        m.put("tm", Times.All);
//        sql += createKeyWord(m);
//
//        System.err.println("SQL " + sql);
//        hrForms = hrFormFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
//
////        calMinitsAditional(additionalForms);
//    }
    public void searchFormByShiftDate() {
        Date startTime = new Date();
        
        String sql = "";
        Map m = new HashMap();

        sql = " select a from HrForm a where "
                + " a.staffShift.shiftDate between :fd and :td"
                + " and (a.staffShift.startRecord.recordTimeStamp is null or"
                + " a.staffShift.endRecord.recordTimeStamp is null ) ";
//                + " and a.times=:tm ";
        m.put("fd", fromDate);
        m.put("td", toDate);
//        m.put("tm", Times.All);
        sql += createKeyWord(m);

        hrForms = hrFormFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

//        calMinitsAditional(additionalForms);
        
        
    }

    public List<HrForm> getHrForms() {
        return hrForms;
    }

    public void setHrForms(List<HrForm> hrForms) {
        this.hrForms = hrForms;
    }

    public void searchByShiftDate() {
        Date startTime = new Date();
        
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.retired=false "
                + " and a.staffShift.shiftDate between :fd and :td ";
//                + " and a.times=:tm ";
        m.put("fd", fromDate);
        m.put("td", toDate);
//        m.put("tm", Times.All);
        sql += createKeyWord(m);

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

//        calMinitsAditional(additionalForms);
        
        
    }

    public void searchByApproveDate() {
        Date startTime = new Date();
        
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.retired=false "
                + " and a.approvedDate between :fd and :td ";
//                + " and a.times=:tm ";
        m.put("fd", fromDate);
        m.put("td", toDate);
//        m.put("tm", Times.All);
        sql += createKeyWord(m);

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

//        calMinitsAditional(additionalForms);
        
        
    }

    public void createAmmendmentTable() {
        Date startTime = new Date();

        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.createdAt between :fd and :td";
//                + " and a.times=:tm ";

//        m.put("tm", Times.All);
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

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calMinitsAditional(additionalForms);

        

    }

    public void calMinitsAditional(List<AdditionalForm> additionalForms) {
        aditionalWithTimes = new ArrayList<>();
        for (AdditionalForm a : additionalForms) {
            AditionalWithTime awt = new AditionalWithTime();
            if (a.getFromTime() != null && a.getToTime() != null) {
                Calendar frmCal = Calendar.getInstance();
                frmCal.setTime(a.getFromTime());
                ////// // System.out.println("frmCal = " + frmCal);
                Calendar toCal = Calendar.getInstance();
                frmCal.setTime(a.getToTime());
                ////// // System.out.println("toCal = " + toCal);
                ////// // System.out.println("toCal.getTimeInMillis() = " + toCal.getTimeInMillis());
                ////// // System.out.println("frmCal.getTimeInMillis() = " + frmCal.getTimeInMillis());
                ////// // System.out.println("toCal.getTimeInMillis()-frmCal.getTimeInMillis() = " + (toCal.getTimeInMillis() - frmCal.getTimeInMillis()));
                long time = ((toCal.getTime().getTime() - frmCal.getTime().getTime()) / (1000 * 60));
                awt.setAf(a);
                awt.setTime(time);
                ////// // System.out.println("awt.getAf() = " + awt.getAf());
                ////// // System.out.println("awt.getTime() = " + awt.getTime());
                aditionalWithTimes.add(awt);
            }
        }

    }

    public void update(HrForm hrForm) {
        hrFormFacade.edit(hrForm);
    }

    public void createAmmendmentTableApprovedDate() {
        Date startTime = new Date();
        
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

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calMinitsAditional(additionalForms);
        
        

    }

    public void createAmmendmentTableShiftDate() {
        Date startTime = new Date();
        
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

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calMinitsAditional(additionalForms);
        
        

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

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createAmmendmentTableShiftDateAdditionalShift() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.staffShift.shiftDate between :fd and :td "
                + " and a.times=:tm"
                + " and a.staffShift.shift.dayType=:dtp";
        m.put("dtp", DayType.Extra);
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

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.DATE);

    }

    public void createAmmendmentTableShiftDateAdditionalShiftDayOff() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.staffShift.shiftDate between :fd and :td "
                + " and a.times=:tm "
                + " and a.staffShift.shift.dayType!=:dtp";
        m.put("dtp", DayType.Extra);
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

        additionalForms = getAdditionalFormFacade().findByJpql(sql, m, TemporalType.DATE);

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

        staffShifts = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);

        hm.clear();
        sql = "select c from "
                + " StaffShiftExtra c"
                + " where c.retired=false "
                + " and c.shiftDate =:dt "
                + " and c.staff=:stf ";
        hm.put("dt", getDate());
        hm.put("stf", getCurrentAdditionalForm().getStaff());

        staffShifts.addAll(staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE));

    }

    public void fetchStaffShiftNotNormal() {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and type(c)!=:cl "
                + " and c.shift is not null "
                + " and c.shift.dayType in :dtp "
                + " and c.shiftDate =:dt "
                + " and c.staff=:stf ";

        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.Poya, DayType.PublicHoliday, DayType.MurchantileHoliday}));
        hm.put("cl", StaffShiftExtra.class);
        hm.put("dt", getDate());
        hm.put("stf", getCurrentAdditionalForm().getStaff());

        staffShifts = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);

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

        staffShifts = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
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
        additionalForms = null;
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

//        if (fetchCurrentSalaryCycle(date) != null) {
//            SalaryCycle s = fetchCurrentSalaryCycle(date);
//            //// // System.out.println("s.getWorkedFromDate() = " + s.getWorkedFromDate());
//            //// // System.out.println("s.getWorkedToDate() = " + s.getWorkedToDate());
//            //// // System.out.println("s.getDayOffPhFromDate() = " + s.getDayOffPhFromDate());
//            //// // System.out.println("s.getDayOffPhToDate() = " + s.getDayOffPhToDate());
//            Date nowDate = CommonFunctions.getEndOfDay();
//            //// // System.out.println("nowDate = " + nowDate);
//            if (nowDate.getTime() > s.getDayOffPhToDate().getTime()) {
//                double d = (nowDate.getTime() - s.getDayOffPhToDate().getTime()) / (1000 * 60 * 60 * 24);
//                //// // System.out.println("d = " + d);
//                if (d > 3) {
//                    JsfUtil.addErrorMessage("You Can't Add This Addional."
//                            + "because you can add only additionls within 3 days after Day off / PH To Date");
//                    return true;
//                }
//            }
//        }
//
//        if (fetchCurrentSalaryCycle(date) != null) {
//            StaffSalary s = humanResourceBean.getStaffSalary(currentAdditionalForm.getStaff(), fetchCurrentSalaryCycle(date));
//            if (s.getId() != null) {
//                JsfUtil.addErrorMessage("You Can't Add This Addional."
//                        + "because this salary was createed");
//                return true;
//            }
//        }
        //NEED To Check StaffSHift  if not selected is there any shift time on that day
        return false;
    }

    public boolean errorCheckShiftDayOff() {
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
        if (fetchCurrentSalaryCycle(date) != null) {
            SalaryCycle s = fetchCurrentSalaryCycle(date);
            //// // System.out.println("s.getWorkedFromDate() = " + s.getWorkedFromDate());
            Date nowDate = CommonFunctions.getEndOfDay();
            if (nowDate.getTime() > s.getDayOffPhToDate().getTime()) {
                double d = (nowDate.getTime() - s.getDayOffPhToDate().getTime()) / (1000 * 60 * 60 * 24);
                if (d > 3) {
                    JsfUtil.addErrorMessage("You Can't Add This Addional."
                            + "because you can add only additionls within 3 days after Day off / PH To Date");
                    return true;
                }
            }
        }

        if (fetchCurrentSalaryCycle(date) != null) {
            StaffSalary s = humanResourceBean.getStaffSalary(currentAdditionalForm.getStaff(), fetchCurrentSalaryCycle(date));
            if (s.getId() != null) {
                JsfUtil.addErrorMessage("You Can't Add This Addional."
                        + "because this salary was createed");
                return true;
            }
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

//        if (fetchCurrentSalaryCycle(date) != null) {
//            SalaryCycle s = fetchCurrentSalaryCycle(date);
//            //// // System.out.println("s.getWorkedFromDate() = " + s.getWorkedFromDate());
//            //// // System.out.println("s.getWorkedToDate() = " + s.getWorkedToDate());
//            //// // System.out.println("s.getDayOffPhFromDate() = " + s.getDayOffPhFromDate());
//            //// // System.out.println("s.getDayOffPhToDate() = " + s.getDayOffPhToDate());
//            Date nowDate = CommonFunctions.getEndOfDay();
//            //// // System.out.println("nowDate = " + nowDate);
//            if (nowDate.getTime() > s.getDayOffPhToDate().getTime()) {
//                double d = (nowDate.getTime() - s.getDayOffPhToDate().getTime()) / (1000 * 60 * 60 * 24);
//                //// // System.out.println("d = " + d);
//                if (d > 3) {
//                    JsfUtil.addErrorMessage("You Can't Add This Addional."
//                            + "because you can add only additionls within 3 days after Day off / PH To Date");
//                    return true;
//                }
//            }
//        }
//
//        if (fetchCurrentSalaryCycle(date) != null) {
//            StaffSalary s = humanResourceBean.getStaffSalary(currentAdditionalForm.getStaff(), fetchCurrentSalaryCycle(date));
//            if (s.getId() != null) {
//                JsfUtil.addErrorMessage("You Can't Add This Addional."
//                        + "because this salary was createed");
//                return true;
//            }
//        }
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

    public SalaryCycle fetchCurrentSalaryCycle(Date d) {
        String sql;
        Map m = new HashMap();
        sql = "select c from SalaryCycle c "
                + " where c.retired=false "
                + " and c.dayOffPhFromDate<:d "
                + " and c.dayOffPhToDate>:d ";
        m.put("d", d);

        return salaryCycleFacade.findFirstByJpql(sql, m, TemporalType.TIMESTAMP);

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

    public void saveAdditionalFormShiftDayOff() {
        if (errorCheckShiftDayOff()) {
            return;
        }

        StaffShift staffShift = staffShiftFacade.find(currentAdditionalForm.getStaffShift().getId());




        Shift shift = null;

        if (currentAdditionalForm.getStaffShift() == null) {
            JsfUtil.addErrorMessage("Please Un Select Staff Shift");
            return;
        }
        DayType dayType;
        if (currentAdditionalForm.getStaffShift().getDayType() != null || currentAdditionalForm.getStaffShift().getDayType() == DayType.DayOff
                || currentAdditionalForm.getStaffShift().getShift().isHalfShift()) {
            dayType = currentAdditionalForm.getStaffShift().getDayType();
        } else {
            dayType = phDateController.getHolidayType(date);
        }
        shift = currentAdditionalForm.getStaffShift().getShift();

        if (shift == null) {
            shift = fetchShift(currentAdditionalForm.getStaff().getRoster(), dayType);
        }

//        if(dayType==DayType.Poya && currentAdditionalForm.getStaffShift().getShift()!=null){
//            shift=currentAdditionalForm.getStaffShift().getShift();
//        }else{
//            shift = fetchShift(currentAdditionalForm.getStaff().getRoster(), dayType);
//        }
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
        staffShiftExtra.setDayType(dayType);
        staffShiftFacade.edit(staffShiftExtra);

        currentAdditionalForm.setStaffShift(staffShiftExtra);
        additionalFormFacade.edit(currentAdditionalForm);

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

    public void saveAdditionalFormShift() {
        if (errorCheckShift()) {
            return;
        }

        Shift shift = null;

        shift = fetchShift(currentAdditionalForm.getStaff().getRoster(), DayType.Extra);

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

        staffShiftExtra.setDayType(DayType.Extra);
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

        Shift sh = shiftFacade.findFirstByJpql(sql, hm, TemporalType.DATE);
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
//        //// // System.out.println("sh.getName() = " + sh.getShift().getName());
        return sh;
    }

    private Shift fetchShift(Roster roster, DayType dayType, Staff staff) {
        if (dayType == null || roster == null) {
            return null;
        }

        String sql = "select s from  Shift s "
                + " where s.retired=false "
                + " and s.roster=:rs"
                + " and s.dayType=:dtp "
                + " and s.staff=:stf";
        HashMap hm = new HashMap();
        hm.put("rs", roster);
        hm.put("dtp", dayType);

        Shift sh = shiftFacade.findFirstByJpql(sql, hm, TemporalType.DATE);
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

        Shift sh = shiftFacade.findFirstByJpql(sql, hm, TemporalType.DATE);

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

        Shift sh = shiftFacade.findFirstByJpql(sql, hm, TemporalType.DATE);

        return sh;
    }

    List<Shift> shifts;

    public class AditionalWithTime {

        AdditionalForm af;
        long time;

        public AdditionalForm getAf() {
            return af;
        }

        public void setAf(AdditionalForm af) {
            this.af = af;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

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
//            JsfUtil.addErrorMessage("Please Select Staff");
//            return;
//        }
//
//        if (getDate() == null) {
//            JsfUtil.addErrorMessage("Please Select Date");
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
//        shifts = shiftFacade.findByJpql(sql, hm);
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

    public List<AditionalWithTime> getAditionalWithTimes() {
        return aditionalWithTimes;
    }

    public void setAditionalWithTimes(List<AditionalWithTime> aditionalWithTimes) {
        this.aditionalWithTimes = aditionalWithTimes;
    }

    public double getTotalInTime() {
        return totalInTime;
    }

    public void setTotalInTime(double totalInTime) {
        this.totalInTime = totalInTime;
    }

    public double getTotalOutTime() {
        return totalOutTime;
    }

    public void setTotalOutTime(double totalOutTime) {
        this.totalOutTime = totalOutTime;
    }

    public double getTotalAllTime() {
        return totalAllTime;
    }

    public void setTotalAllTime(double totalAllTime) {
        this.totalAllTime = totalAllTime;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    
}

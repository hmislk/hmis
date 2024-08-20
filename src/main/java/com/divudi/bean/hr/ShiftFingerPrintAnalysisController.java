/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.dataStructure.ShiftTable;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.FingerPrintComparator;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.Times;
import static com.divudi.data.hr.Times.inTime;
import static com.divudi.data.hr.Times.outTime;

import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Form;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.FingerPrintRecordHistory;
import com.divudi.entity.hr.HrForm;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftExtra;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.FingerPrintRecordHistoryFacade;
import com.divudi.facade.FormFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ShiftFingerPrintAnalysisController implements Serializable {

    /**
     *
     * EJBs
     *
     */
    @EJB
    HumanResourceBean humanResourceBean;

    CommonFunctions commonFunctions;
    @EJB
    StaffShiftFacade staffShiftFacade;
    @EJB
    FingerPrintRecordFacade fingerPrintRecordFacade;
    @EJB
    StaffLeaveFacade staffLeaveFacade;
    @EJB
    FormFacade formFacade;

    /**
     *
     * Managed Beans
     *
     */
    @Inject
    SessionController sessionController;
    @Inject
    StaffLeaveApplicationFormController staffLeaveApplicationFormController;
    @Inject
    StaffAdditionalFormController staffAdditionalFormController;
    @Inject
    ShiftTableController shiftTableController;
    @Inject
    CommonController commonController;
    /**
     *
     * Properties
     *
     */
    DayType dayType;
    Date fromDate;
    Date toDate;
    Roster roster;
    boolean flag;
    boolean backButtonIsActive;
    String backButtonPage;

    List<ShiftTable> shiftTables;
    private List<String> errorMessage = null;

    /**
     *
     *
     * Methods
     *
     *
     */
    /**
     *
     * @return
     *
     */
    public String back() {
        backButtonIsActive = false;
        String t = backButtonPage;
        backButtonPage = "";
        return t;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public void toStaffLeaveApplicationFormController() {
        staffLeaveApplicationFormController.setStaff(staff);
        staffLeaveApplicationFormController.setFromDate(fromDate);
        staffLeaveApplicationFormController.setToDate(toDate);
    }

    public void toStaffAdditionalFormController() {
        staffAdditionalFormController.setStaff(staff);
        staffAdditionalFormController.setFromDate(fromDate);
        staffAdditionalFormController.setToDate(toDate);
    }

    public void toShiftTableController() {
        shiftTableController.setFromDate(fromDate);
        shiftTableController.setToDate(toDate);
        shiftTableController.setStaff(staff);
        shiftTableController.setRoster(roster);
    }

    public StaffLeaveFacade getStaffLeaveFacade() {
        return staffLeaveFacade;
    }

    public void setStaffLeaveFacade(StaffLeaveFacade staffLeaveFacade) {
        this.staffLeaveFacade = staffLeaveFacade;
    }

    public DayType getDayType() {
        return dayType;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
    }

    public FormFacade getFormFacade() {
        return formFacade;
    }

    public void setFormFacade(FormFacade formFacade) {
        this.formFacade = formFacade;
    }

    public void restTimeStamp(FingerPrintRecord fingerPrintRecord) {
        if (fingerPrintRecord == null) {
            return;
        }

        if (fingerPrintRecord.getLoggedRecord() != null) {
            fingerPrintRecord.setRecordTimeStamp(fingerPrintRecord.getLoggedRecord().getRecordTimeStamp());
        } else {
            fingerPrintRecord.setRecordTimeStamp(null);
        }

    }

    public void makeTableNull() {
        shiftTables = null;
        errorMessage = null;
    }

    public void checkFromdateBeforeToDate(StaffShift staffShift) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(staffShift.getShiftStartTime());
        cal2.setTime(staffShift.getShiftEndTime());

        if (cal1.getTimeInMillis() > cal2.getTimeInMillis()) {
            JsfUtil.addErrorMessage("To Date Must Be lager Than From Date");
            return;
        }
    }

    public void calDayCount(StaffShift staffShift) {

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(staffShift.getShiftStartTime());
        cal2.setTime(staffShift.getShiftEndTime());

        Long daycount = (cal1.getTimeInMillis() - cal2.getTimeInMillis()) / (1000 * 60 * 60 * 24);

        ////// // System.out.println("daycount = " + daycount);
        if (daycount > 2) {
            JsfUtil.addErrorMessage("Date Must Be less Than 2 Days");
            return;
        }

    }

    public void listenStart(StaffShift staffShift) {
        if (staffShift == null) {
            return;
        }

        if (staffShift.getStartRecord().getLoggedRecord() != null) {
            return;
        }

        if (staffShift.getShift() != null
                && staffShift.getLeaveType() != null
                && !staffShift.getLeaveType().isFullDayLeave()
                && staffShift.getEndRecord().getRecordTimeStamp() != null) {

            Calendar cal = Calendar.getInstance();
            cal.setTime(staffShift.getEndRecord().getRecordTimeStamp());
            cal.add(Calendar.HOUR, 0 - (int) staffShift.getShift().getLeaveHourHalf());
            staffShift.getStartRecord().setRecordTimeStamp(cal.getTime());
            return;
        }

//        checkFromdateBeforeToDate(staffShift);
//        calDayCount(staffShift);
        staffShift.getStartRecord().setRecordTimeStamp(staffShift.getShiftStartTime());
//        fingerPrintRecordFacade.edit(staffShift.getStartRecord());
//        staffShiftFacade.edit(staffShift);
    }

    public void listenEnd(StaffShift staffShift) {
        if (staffShift == null) {
            return;
        }

        if (staffShift.getEndRecord().getLoggedRecord() != null) {
            return;
        }

        if (staffShift.getShift() != null
                && staffShift.getLeaveType() != null
                && !staffShift.getLeaveType().isFullDayLeave()
                && staffShift.getStartRecord().getRecordTimeStamp() != null) {

            Calendar cal = Calendar.getInstance();
            cal.setTime(staffShift.getStartRecord().getRecordTimeStamp());
            cal.add(Calendar.HOUR, (int) staffShift.getShift().getLeaveHourHalf());
            staffShift.getEndRecord().setRecordTimeStamp(cal.getTime());
            return;
        }

//        checkFromdateBeforeToDate(staffShift);
//        calDayCount(staffShift);
        staffShift.getEndRecord().setRecordTimeStamp(staffShift.getShiftEndTime());

//        fingerPrintRecordFacade.edit(staffShift.getEndRecord());
//        staffShiftFacade.edit(staffShift);
    }

    public void listenClear(StaffShift staffShift) {

        staffShift.getEndRecord().setRetired(true);
        getFingerPrintRecordFacade().edit(staffShift.getEndRecord());
        staffShift.setEndRecord(null);

        staffShift.getStartRecord().setRetired(true);
        getFingerPrintRecordFacade().edit(staffShift.getStartRecord());
        staffShift.setStartRecord(null);

        getStaffShiftFacade().edit(staffShift);
    }

    public void selectRosterLstener() {
        makeTableNull();
    }

    private boolean errorCheck() {
        return getFromDate() == null || getToDate() == null;
    }

//    private AdditionalForm fetchAdditionalForm(StaffShift staffShift) {
//        String sql = "Select a from AdditionalForm a "
//                + " where a.retired=false"
//                + " and a.staffShift=:stf ";
//        HashMap hm = new HashMap();
//        hm.put("stf", staffShift);
//
//        return (AdditionalForm) formFacade.findByJpql(sql, hm);
//    }
    private void fetchTimeFromAddiationalFrom(StaffShift ss, FingerPrintRecord fingerPrintRecordIn, FingerPrintRecord fingerPrintRecordOut, Set<FingerPrintRecord> fingerPrintRecords, HrForm additionalForm) {

        if (additionalForm == null) {
            return;
        }

        if (additionalForm.getStaffShift() == null) {
            return;
        }

        if (additionalForm.getTimes() != Times.All
                && !additionalForm.getStaffShift().equals(ss)) {
            return;
        }

        if (fingerPrintRecordIn == null && additionalForm.getTimes() == Times.inTime) {
            fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(additionalForm);

            if (fingerPrintRecordIn == null) {
                fingerPrintRecordIn = new FingerPrintRecord();
                fingerPrintRecordIn.setCreatedAt(new Date());
                fingerPrintRecordIn.setCreater(sessionController.getLoggedUser());
                fingerPrintRecordIn.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                fingerPrintRecordIn.setComments("(new Additional)");
                fingerPrintRecordIn.setRecordTimeStamp(additionalForm.getFromTime());
                fingerPrintRecordFacade.create(fingerPrintRecordIn);
            } else {
                if (fingerPrintRecordIn.getRecordTimeStamp().getTime() < additionalForm.getFromTime().getTime()) {
                    fingerPrintRecordIn.setRecordTimeStamp(additionalForm.getFromTime());
                }

            }

            fingerPrintRecordIn.setTimes(Times.inTime);
            fingerPrintRecords.add(fingerPrintRecordIn);
            ss.setStartRecord(fingerPrintRecordIn);
        }

        if (fingerPrintRecordOut == null && additionalForm.getTimes() == Times.outTime) {
            fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(additionalForm);

            if (fingerPrintRecordOut == null) {
                fingerPrintRecordOut = new FingerPrintRecord();

                fingerPrintRecordOut.setCreatedAt(new Date());
                fingerPrintRecordOut.setCreater(sessionController.getLoggedUser());
                fingerPrintRecordOut.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                fingerPrintRecordOut.setComments("(new Additional)");
                fingerPrintRecordOut.setRecordTimeStamp(additionalForm.getToTime());
                fingerPrintRecordFacade.create(fingerPrintRecordOut);

            } else {
                if (fingerPrintRecordOut.getRecordTimeStamp().getTime() > additionalForm.getToTime().getTime()) {
                    fingerPrintRecordOut.setRecordTimeStamp(additionalForm.getToTime());
                }
            }

            fingerPrintRecordOut.setTimes(Times.outTime);
            fingerPrintRecords.add(fingerPrintRecordOut);
            ss.setEndRecord(fingerPrintRecordOut);

        }

        if (fingerPrintRecordIn == null && fingerPrintRecordOut == null && additionalForm.getTimes() == Times.All) {
            fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(additionalForm);
            fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(additionalForm);

            if (fingerPrintRecordIn == null) {
                fingerPrintRecordIn = new FingerPrintRecord();
                fingerPrintRecordIn.setCreatedAt(new Date());
                fingerPrintRecordIn.setCreater(sessionController.getLoggedUser());
                fingerPrintRecordIn.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                fingerPrintRecordIn.setComments("(new Additional)");
                fingerPrintRecordIn.setRecordTimeStamp(additionalForm.getFromTime());
                fingerPrintRecordFacade.create(fingerPrintRecordIn);
            } else {
                if (fingerPrintRecordIn.getRecordTimeStamp().getTime() < additionalForm.getFromTime().getTime()) {
                    fingerPrintRecordIn.setRecordTimeStamp(additionalForm.getFromTime());
                }

            }

            fingerPrintRecordIn.setTimes(Times.inTime);
            fingerPrintRecords.add(fingerPrintRecordIn);
            ss.setStartRecord(fingerPrintRecordIn);

            if (fingerPrintRecordOut == null) {
                fingerPrintRecordOut = new FingerPrintRecord();
                fingerPrintRecordOut.setCreatedAt(new Date());
                fingerPrintRecordOut.setCreater(sessionController.getLoggedUser());
                fingerPrintRecordOut.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                fingerPrintRecordOut.setComments("(new Additional)");
                fingerPrintRecordOut.setRecordTimeStamp(additionalForm.getToTime());
                fingerPrintRecordFacade.create(fingerPrintRecordOut);
            } else {
                if (fingerPrintRecordOut.getRecordTimeStamp().getTime() > additionalForm.getToTime().getTime()) {
                    fingerPrintRecordOut.setRecordTimeStamp(additionalForm.getToTime());
                }
            }

            fingerPrintRecordOut.setTimes(Times.outTime);
            fingerPrintRecords.add(fingerPrintRecordOut);
            ss.setEndRecord(fingerPrintRecordOut);

        }

        switch (additionalForm.getTimes()) {

            case inTime:
                if (fingerPrintRecordIn != null) {
                    fingerPrintRecordIn.setAllowedExtraDuty(true);
                }
                break;
            case outTime:
                if (fingerPrintRecordOut != null) {
                    fingerPrintRecordOut.setAllowedExtraDuty(true);
                }
                break;
            case All:
                if (fingerPrintRecordIn != null) {
                    fingerPrintRecordIn.setAllowedExtraDuty(true);
                }
                if (fingerPrintRecordOut != null) {
                    fingerPrintRecordOut.setAllowedExtraDuty(true);
                }
                break;
        }

        if (fingerPrintRecordIn != null && fingerPrintRecordIn.getId() != null) {
            fingerPrintRecordFacade.edit(fingerPrintRecordIn);
        }

        if (fingerPrintRecordOut != null && fingerPrintRecordOut.getId() != null) {
            fingerPrintRecordFacade.edit(fingerPrintRecordOut);
        }

    }

    public void fetchAndSetDayType(StaffShift ss) {
        if (ss.getDayType() == DayType.Extra || ss.getDayType() == DayType.DayOff) {
            return;
        }

        ss.setDayType(null);

        DayType dtp = phDateController.getHolidayType(ss.getShiftDate());
        ss.setDayType(dtp);
        if (dtp == null) {
            if (ss.getShift() != null) {
                ss.setDayType(ss.getShift().getDayType());
            }
        }
    }

    private void fetchAndSetStaffLeave(StaffShift ss) {
        ss.setLeaveType(null);
        StaffLeave staffLeave = getHumanResourceBean().fetchFirstStaffLeave(ss.getStaff(), ss.getShiftDate());
        //Setting Leave Type To StaffShift From Staff Leave
        if (!(ss instanceof StaffShiftExtra) && staffLeave != null) {
            ss.setLeaveType(staffLeave.getLeaveType());
        }

    }

    @Inject
    PhDateController phDateController;

    public void createShiftTable() {
        Date startTime = new Date();

        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        ////// // System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        ////// // System.out.println("Line2 = " + new Date());

        //CREATE FIRTS TABLE For Indexing Purpuse
        ShiftTable netT;

        while (tmpToDate.after(nowDate)) {
            netT = new ShiftTable();
            netT.setDate(nowDate);

            Calendar calNowDate = Calendar.getInstance();
            calNowDate.setTime(nowDate);

            Calendar calFromDate = Calendar.getInstance();
            calFromDate.setTime(getFromDate());

            if (calNowDate.get(Calendar.DATE) == calFromDate.get(Calendar.DATE)) {
                netT.setFlag(Boolean.TRUE);
            } else {
                netT.setFlag(Boolean.FALSE);
            }

            List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShiftWithShift(nowDate, roster);

            if (staffShifts.isEmpty()) {
//                    System.err.println("CONTINUE");
                Calendar c = Calendar.getInstance();
                c.setTime(nowDate);
                c.add(Calendar.DATE, 1);
                nowDate = c.getTime();
                continue;
            }

            for (StaffShift ss : staffShifts) {
//                ss.setStartRecord(null);
//                ss.setEndRecord(null);
                setShiftTableData(ss);
                netT.getStaffShift().add(ss);

            }

            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        // Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
        
    }

    public void createShiftTableByStaff() {
        Date startTime = new Date();
        
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        ////// // System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        ////// // System.out.println("Line2 = " + new Date());

        //CREATE FIRTS TABLE For Indexing Purpuse
        ShiftTable netT;

        while (tmpToDate.after(nowDate)) {
            netT = new ShiftTable();
            netT.setDate(nowDate);

            Calendar calNowDate = Calendar.getInstance();
            calNowDate.setTime(nowDate);

            Calendar calFromDate = Calendar.getInstance();
            calFromDate.setTime(getFromDate());

            if (calNowDate.get(Calendar.DATE) == calFromDate.get(Calendar.DATE)) {
                netT.setFlag(Boolean.TRUE);
            } else {
                netT.setFlag(Boolean.FALSE);
            }

            List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShiftWithShift(nowDate, staff);

            if (staffShifts.isEmpty()) {
//                    System.err.println("CONTINUE");
                Calendar c = Calendar.getInstance();
                c.setTime(nowDate);
                c.add(Calendar.DATE, 1);
                nowDate = c.getTime();
                continue;
            }

            for (StaffShift ss : staffShifts) {
//                ss.setStartRecord(null);
//                ss.setEndRecord(null);
                setShiftTableData(ss);
                netT.getStaffShift().add(ss);
            }

            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        // Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
        
        
    }

    private List<Form> fetchAdditionalForm(StaffShift staffShift) {
        String sql = "select s from AdditionalForm s "
                + " where s.retired=false "
                + " and s.staffShift=:stf";

        HashMap hm = new HashMap();
        hm.put("stf", staffShift);
        List<Form> list = formFacade.findByJpql(sql, hm);

        if (list == null) {
            return null;
        } else {

            return list;
        }

    }

    private void setShiftTableData(StaffShift ss) {
        fetchAndSetStaffLeave(ss);
        fetchAndSetDayType(ss);

        Set<FingerPrintRecord> list = new HashSet<>();
        FingerPrintRecord fingerPrintRecordIn = ss.getStartRecord();
        FingerPrintRecord fingerPrintRecordOut = ss.getEndRecord();

        List<Form> additionalFormList = fetchAdditionalForm(ss);

        if (additionalFormList == null || additionalFormList.isEmpty()) {
            if (fingerPrintRecordIn == null) {
                fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(ss);

            }

            if (fingerPrintRecordOut == null) {
                fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(ss);
            }
        } else {

            for (Form addittional : additionalFormList) {
                AdditionalForm addittionalForm = (AdditionalForm) addittional;
                //Fetch Time From Additional From

                fetchTimeFromAddiationalFrom(ss, fingerPrintRecordIn, fingerPrintRecordOut, list, addittionalForm);

//                if (addittionalForm.getTimes() == Times.inTime) {
//                    fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(ss);
//                }
//
//                if (addittionalForm.getTimes() == Times.outTime) {
//                    fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(ss);
//                }
            }

        }

//                System.err.println(" 1 "+fingerPrintRecordIn+" : "+fingerPrintRecordOut);
        if (fingerPrintRecordIn != null) {
            fingerPrintRecordIn.setTimes(Times.inTime);
            ss.setStartRecord(fingerPrintRecordIn);
            list.add(fingerPrintRecordIn);
        }

        if (fingerPrintRecordOut != null) {
            fingerPrintRecordOut.setTimes(Times.outTime);
            ss.setEndRecord(fingerPrintRecordOut);
            list.add(fingerPrintRecordOut);
        }


        FingerPrintRecord fpr = null;
        if (ss.getStartRecord() == null) {
            fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.inTime);
            list.add(fpr);
            ss.setStartRecord(fpr);

            if (ss.getPreviousStaffShift() != null) {
                ss.getStartRecord().setComments("(NEW PREV)");
                ss.getStartRecord().setRecordTimeStamp(ss.getShiftStartTime());

            }

        }
        if (ss.getEndRecord() == null) {
            fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.outTime);
            list.add(fpr);
            ss.setEndRecord(fpr);
            if (ss.getNextStaffShift() != null) {
                ss.getEndRecord().setComments("(NEW NEXT)");
                ss.getEndRecord().setRecordTimeStamp(ss.getShiftEndTime());

            }
        }

        checkLeave(ss);
        List<FingerPrintRecord> missedRecords = getHumanResourceBean().fetchMissedFingerFrintRecord(ss);

        if (missedRecords != null) {
            for (FingerPrintRecord fp : missedRecords) {
                list.add(fp);
            }

        }

        List<FingerPrintRecord> listFpr = new ArrayList<>();
        for (FingerPrintRecord fp : list) {
            listFpr.add(fp);
        }

        Collections.sort(listFpr, new FingerPrintComparator());
        ss.getFingerPrintRecordList().addAll(listFpr);

    }

    private void checkLeave(StaffShift ss) {
        LeaveType leaveType = ss.getLeaveType();
        boolean flagLeave = false;
        if (leaveType != null && leaveType.isFullDayLeave()) {
            flagLeave = true;
        }

        if (flagLeave) {
            if (ss.getStartRecord() != null && ss.getStartRecord().getRecordTimeStamp() != null) {
                ss.getStartRecord().setRecordTimeStamp(null);
            }
            if (ss.getEndRecord() != null && ss.getEndRecord().getRecordTimeStamp() != null) {
                ss.getEndRecord().setRecordTimeStamp(null);
            }
        }

    }

    @Inject
    StaffLeaveFromLateAndEarlyController staffLeaveFromLateAndEarlyController;

//    public List<StaffShift> fetchStaffShift(StaffShift referenceShift){
//        String sql="";
//        
//        
//    }
    public void createShiftTableAdditional() {
        Date startTime = new Date();
        
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        ////// // System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        ////// // System.out.println("Line2 = " + new Date());

        //CREATE FIRTS TABLE For Indexing Purpuse
        ShiftTable netT;

        while (tmpToDate.after(nowDate)) {
            netT = new ShiftTable();
            netT.setDate(nowDate);

            Calendar calNowDate = Calendar.getInstance();
            calNowDate.setTime(nowDate);

            Calendar calFromDate = Calendar.getInstance();
            calFromDate.setTime(getFromDate());

            if (calNowDate.get(Calendar.DATE) == calFromDate.get(Calendar.DATE)) {
                netT.setFlag(Boolean.TRUE);
            } else {
                netT.setFlag(Boolean.FALSE);
            }

            List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShiftWithShiftAdditional(nowDate, roster);

            if (staffShifts.isEmpty()) {
//                    System.err.println("CONTINUE");
                Calendar c = Calendar.getInstance();
                c.setTime(nowDate);
                c.add(Calendar.DATE, 1);
                nowDate = c.getTime();
                continue;
            }

            for (StaffShift ss : staffShifts) {
//                ss.setStartRecord(null);
//                ss.setEndRecord(null);
                setShiftTableData(ss);
                netT.getStaffShift().add(ss);

            }

            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        // Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
        
        
    }

    public void createShiftTableAdditionalByStaff() {
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        ////// // System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        ////// // System.out.println("Line2 = " + new Date());

        //CREATE FIRTS TABLE For Indexing Purpuse
        ShiftTable netT;

        while (tmpToDate.after(nowDate)) {
            netT = new ShiftTable();
            netT.setDate(nowDate);

            Calendar calNowDate = Calendar.getInstance();
            calNowDate.setTime(nowDate);

            Calendar calFromDate = Calendar.getInstance();
            calFromDate.setTime(getFromDate());

            if (calNowDate.get(Calendar.DATE) == calFromDate.get(Calendar.DATE)) {
                netT.setFlag(Boolean.TRUE);
            } else {
                netT.setFlag(Boolean.FALSE);
            }

            List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShiftWithShiftAdditional(nowDate, staff);

            if (staffShifts.isEmpty()) {
//                    System.err.println("CONTINUE");
                Calendar c = Calendar.getInstance();
                c.setTime(nowDate);
                c.add(Calendar.DATE, 1);
                nowDate = c.getTime();
                continue;
            }

            for (StaffShift ss : staffShifts) {
//                ss.setStartRecord(null);
//                ss.setEndRecord(null);
                setShiftTableData(ss);
                netT.getStaffShift().add(ss);

            }

            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        // Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
    }

    public void createShiftTableReset() {
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        ////// // System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        ////// // System.out.println("Line2 = " + new Date());

        //CREATE FIRTS TABLE For Indexing Purpuse
        ShiftTable netT;

        while (tmpToDate.after(nowDate)) {
            netT = new ShiftTable();
            netT.setDate(nowDate);

            Calendar calNowDate = Calendar.getInstance();
            calNowDate.setTime(nowDate);

            Calendar calFromDate = Calendar.getInstance();
            calFromDate.setTime(getFromDate());

            if (calNowDate.get(Calendar.DATE) == calFromDate.get(Calendar.DATE)) {
                netT.setFlag(Boolean.TRUE);
            } else {
                netT.setFlag(Boolean.FALSE);
            }

            List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShiftWithShift(nowDate, roster);

            if (staffShifts.isEmpty()) {
//                    System.err.println("CONTINUE");
                Calendar c = Calendar.getInstance();
                c.setTime(nowDate);
                c.add(Calendar.DATE, 1);
                nowDate = c.getTime();
                continue;
            }

            for (StaffShift ss : staffShifts) {
                ss.setStartRecord(null);
                ss.setEndRecord(null);
                setShiftTableData(ss);
                netT.getStaffShift().add(ss);

            }

            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        // Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
    }

    public void createShiftTableResetByStaff() {
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        ////// // System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        ////// // System.out.println("Line2 = " + new Date());

        //CREATE FIRTS TABLE For Indexing Purpuse
        ShiftTable netT;

        while (tmpToDate.after(nowDate)) {
            netT = new ShiftTable();
            netT.setDate(nowDate);

            Calendar calNowDate = Calendar.getInstance();
            calNowDate.setTime(nowDate);

            Calendar calFromDate = Calendar.getInstance();
            calFromDate.setTime(getFromDate());

            if (calNowDate.get(Calendar.DATE) == calFromDate.get(Calendar.DATE)) {
                netT.setFlag(Boolean.TRUE);
            } else {
                netT.setFlag(Boolean.FALSE);
            }

            List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShiftWithShift(nowDate, staff);

            if (staffShifts.isEmpty()) {
//                    System.err.println("CONTINUE");
                Calendar c = Calendar.getInstance();
                c.setTime(nowDate);
                c.add(Calendar.DATE, 1);
                nowDate = c.getTime();
                continue;
            }

            for (StaffShift ss : staffShifts) {
                ss.setStartRecord(null);
                ss.setEndRecord(null);
//                //// // System.out.println("ss.getShift().getPreviousShift() = " + ss.getShift().getPreviousShift().getName());
//                //// // System.out.println("ss.getShift().getNextShift() = " + ss.getShift().getNextShift().getName());
                setShiftTableData(ss);
                netT.getStaffShift().add(ss);

            }

            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        // Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
    }

    public void fingerPrintSelectListenerStartRecord(StaffShift staffShift) {
        if (staffShift == null) {

            return;
        }

//        if (staffShift.getStartRecord() == null) {
//            return;
//        }
        if (staffShift.getStartRecord() != null && staffShift.getStartRecord().getStaffShift() != null) {
            JsfUtil.addErrorMessage("This record associated with anther staff shift");
            return;
        }

        FingerPrintRecord fingerPrintRecord = staffShift.getStartRecord();

        if (fingerPrintRecord != null) {
            fingerPrintRecord.setTimes(Times.inTime);
        }
    }

    public void fingerPrintSelectListenerEndRecord(StaffShift staffShift) {
        if (staffShift == null) {
            return;
        }

//        if (staffShift.getEndRecord() == null) {
//            return;
//        }
        if (staffShift.getEndRecord() != null && staffShift.getEndRecord().getStaffShift() != null) {
            JsfUtil.addErrorMessage("This record associated with anther staff shift");
            return;
        }

        FingerPrintRecord fingerPrintRecord = staffShift.getEndRecord();

        if (fingerPrintRecord != null) {
            fingerPrintRecord.setTimes(Times.outTime);
        }
    }

    private FingerPrintRecord createFingerPrint(StaffShift staffShift, FingerPrintRecordType fingerPrintRecordType, Times times) {
        FingerPrintRecord fpr = new FingerPrintRecord();
        fpr.setCreatedAt(new Date());
        fpr.setCreater(getSessionController().getLoggedUser());
        fpr.setFingerPrintRecordType(fingerPrintRecordType);
        fpr.setStaff(staffShift.getStaff());
        fpr.setStaffShift(staffShift);
        fpr.setTimes(times);
        fpr.setTransNew(true);
        fpr.setComments("(NEW " + times.toString() + " )");
        fingerPrintRecordFacade.create(fpr);
        return fpr;
    }

//    private boolean errorCheckForSave() {
//        for (ShiftTable st : shiftTables) {
//            errorMessage = "<br/>" + st.getDate();
//            for (StaffShift ss : st.getStaffShift()) {
//
//                if (ss.getShift().getDayType() == DayType.DayOff
//                        || ss.getShift().getDayType() == DayType.SleepingDay
//                        || ss.getLeaveType() != null) {
//                    continue;
//                }
//
//                if (ss.getPreviousStaffShift() == null) {
//                    if (ss.getStartRecord() == null) {
//                        errorMessage += "  Some Starting Records Has"
//                                + " No Starting Record";
////                        System.err.println("SS " + ss.getId());
//                        JsfUtil.addErrorMessage(errorMessage);
//                        return true;
//                    }
//                    if (ss.getStartRecord().getRecordTimeStamp() == null) {
//                        errorMessage += " Some Starting Records Has No Time ";
//                        JsfUtil.addErrorMessage(errorMessage);
//                        return true;
//                    }
//                }
//
//                if (ss.getNextStaffShift() == null) {
//                    if (ss.getEndRecord() == null) {
//                        errorMessage += " Some End Records Has No Starting Record";
//                        JsfUtil.addErrorMessage(errorMessage);
//                        return true;
//                    }
//                    if (ss.getEndRecord().getRecordTimeStamp() == null) {
//                        errorMessage += " Some End Records Has No Time ";
//                        JsfUtil.addErrorMessage(errorMessage);
//                        return true;
//                    }
//                }
//
//            }
//        }
//
//        return false;
//    }
    private boolean errorCheckForSave(StaffShift ss, ShiftTable shiftTable) {

        SimpleDateFormat ft = new SimpleDateFormat("yyyy MMM dd");

        String date = ft.format(ss.getShiftDate());
        String message = "";

        String code = ss.getStaff().getCode();

        if (ss.getShift().getDayType() != DayType.Normal) {
            return false;
        }

        if (ss.getLeaveType() != null) {
            if (ss.getLeaveType().isFullDayLeave()) {
                return false;
            }

            if (!ss.getLeaveType().isFullDayLeave()) {
                List<StaffLeave> staffLeave = getHumanResourceBean().fetchStaffLeave(ss.getStaff(), ss.getShiftDate());
                if (staffLeave.size() > 1) {
                    return false;
                }
            }

            if (ss.getShift().isHalfShift()) {
                return false;
            }
        }
//        if (ss.getLeaveType() != null && ss.getLeaveType().isFullDayLeave() && !ss.getShift().isHalfShift()) {
//            return false;
//        }

//        else {
//            if (ss.getShift() != null
//                    && ss.getShift().getLeaveHourHalf() == ss.getShift().getDurationHour()) {
//                return false;
//            }
//        }
        if (ss.getPreviousStaffShift() == null) {
            if (ss.getStartRecord() == null) {

                ////// // System.out.println("ss.getLeaveType() = " + ss.getLeaveType());
                ////// // System.out.println("ss.getShift().isHalfShift() = " + ss.getShift().isHalfShift());
//                if ((ss.getLeaveType() == LeaveType.LieuHalf || ss.getLeaveType() == LeaveType.AnnualHalf || ss.getLeaveType() == LeaveType.CasualHalf) && !ss.getShift().isHalfShift()) {
                message = date
                        + " -> " + code
                        + "  Has No Starting Record";
                errorMessage.add(message);
//                        System.err.println("SS " + ss.getId());
//                    JsfUtil.addErrorMessage(errorMessage);
                shiftTable.getStaffShift().add(ss);
                return true;

//                }
            }
            if (ss.getStartRecord().getRecordTimeStamp() == null) {
//                if ((ss.getLeaveType() == LeaveType.LieuHalf || ss.getLeaveType() == LeaveType.AnnualHalf || ss.getLeaveType() == LeaveType.CasualHalf) && !ss.getShift().isHalfShift()) {
                message = date
                        + " -> " + code
                        + " Some Starting Records Has No Time \r ";
                errorMessage.add(message);
//                    JsfUtil.addErrorMessage(errorMessage);
                shiftTable.getStaffShift().add(ss);
                return true;
//                }

            }
        }

        if (ss.getNextStaffShift() == null) {
            if (ss.getEndRecord() == null) {
//                if ((ss.getLeaveType() == LeaveType.LieuHalf || ss.getLeaveType() == LeaveType.AnnualHalf || ss.getLeaveType() == LeaveType.CasualHalf) && !ss.getShift().isHalfShift()) {
                message = date
                        + " -> " + code
                        + " Some End Records Has No Starting Record \r";
                errorMessage.add(message);
                shiftTable.getStaffShift().add(ss);
//                    JsfUtil.addErrorMessage(errorMessage);
                return true;
//                }
            }
            if (ss.getEndRecord().getRecordTimeStamp() == null) {
//                if ((ss.getLeaveType() == LeaveType.LieuHalf || ss.getLeaveType() == LeaveType.AnnualHalf || ss.getLeaveType() == LeaveType.CasualHalf) && !ss.getShift().isHalfShift()) {
                message = date
                        + " -> " + code
                        + " Some End Records Has No Time \r ";
                errorMessage.add(message);
                shiftTable.getStaffShift().add(ss);
//                    JsfUtil.addErrorMessage(errorMessage);
                return true;
//                }
            }
        }

        return false;
    }

    @EJB
    FingerPrintRecordHistoryFacade fingerPrintRecordHistoryFacade;
    Staff staff;

    public PhDateController getPhDateController() {
        return phDateController;
    }

    public void setPhDateController(PhDateController phDateController) {
        this.phDateController = phDateController;
    }

    public StaffLeaveFromLateAndEarlyController getStaffLeaveFromLateAndEarlyController() {
        return staffLeaveFromLateAndEarlyController;
    }

    public void setStaffLeaveFromLateAndEarlyController(StaffLeaveFromLateAndEarlyController staffLeaveFromLateAndEarlyController) {
        this.staffLeaveFromLateAndEarlyController = staffLeaveFromLateAndEarlyController;
    }

    public FingerPrintRecordHistoryFacade getFingerPrintRecordHistoryFacade() {
        return fingerPrintRecordHistoryFacade;
    }

    public void setFingerPrintRecordHistoryFacade(FingerPrintRecordHistoryFacade fingerPrintRecordHistoryFacade) {
        this.fingerPrintRecordHistoryFacade = fingerPrintRecordHistoryFacade;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    private void saveHistory(FingerPrintRecord fingerPrintRecord) {

        if (fingerPrintRecord == null) {
            return;
        }

        boolean flag = false;
        FingerPrintRecord fetchFingerPrintRecord = fingerPrintRecord.getId() != null ? fingerPrintRecordFacade.find(fingerPrintRecord.getId()) : null;

        if (fetchFingerPrintRecord == null) {
            flag = true;
        }

        if (fetchFingerPrintRecord != null) {
            Date date1 = fetchFingerPrintRecord.getRecordTimeStamp();
            Date date2 = fingerPrintRecord.getRecordTimeStamp();


            if (date1 != null & date2 != null) {
                if (!date1.equals(date2)) {
                    flag = true;
                }
            }
        }

        if (flag) {
            FingerPrintRecordHistory fingerPrintRecordHistory = new FingerPrintRecordHistory();
            fingerPrintRecordHistory.setFingerPrintRecord(fingerPrintRecord);
            fingerPrintRecordHistory.setCreatedAt(new Date());
            fingerPrintRecordHistory.setCreater(sessionController.getLoggedUser());
            //Changes
            Date before = fetchFingerPrintRecord != null ? fetchFingerPrintRecord.getRecordTimeStamp() : null;

            fingerPrintRecordHistory.setBeforeChange(before);
            fingerPrintRecordHistory.setAfterChange(fingerPrintRecord.getRecordTimeStamp());
            fingerPrintRecordHistory.setStaff(fingerPrintRecord.getStaff());
            fingerPrintRecordHistory.setStaffShift(fingerPrintRecord.getStaffShift());
            if (fingerPrintRecord.getStaff() != null) {
                fingerPrintRecordHistory.setRoster(fingerPrintRecord.getStaff().getRoster());
            }
            if (fingerPrintRecord.getStaffShift() != null) {
                fingerPrintRecordHistory.setShift(fingerPrintRecord.getStaffShift().getShift());
            }
            if (fingerPrintRecord.getId() == null) {
                fingerPrintRecordFacade.create(fingerPrintRecord);
            }
            fingerPrintRecordHistoryFacade.create(fingerPrintRecordHistory);
        }

    }

    public void save() {
        List<ShiftTable> tmpShiftTable = new ArrayList<>();
        errorMessage = new ArrayList<>();

//        Set<StaffShift> staffShiftsTmp = new HashSet<>();
        if (shiftTables == null) {
            final String empty_List = "Empty List";
            JsfUtil.addErrorMessage(empty_List);
            errorMessage.add(empty_List);
            return;
        }

//        System.err.println("2");
//        if (errorCheckForSave()) {
////            JsfUtil.addErrorMessage("Staff Shift Not Updated");
//            return;
//        }
        for (ShiftTable st : shiftTables) {
            ShiftTable newSh = new ShiftTable();
            newSh.setDate(st.getDate());
            newSh.setFlag(st.isFlag());
            for (StaffShift ss : st.getStaffShift()) {
//                Collections.sort(ss.getFingerPrintRecordList(), new FingerPrintComparator());
                if (errorCheckForSave(ss, newSh)) {
                    continue;
                }

                //UPDATE START RECORD
                FingerPrintRecord startRecord = ss.getStartRecord();
                if (startRecord != null) {
                    startRecord.setStaffShift(ss);
                    saveHistory(startRecord);

                    if (startRecord.getId() != null) {
                        getFingerPrintRecordFacade().edit(startRecord);
                    } else {
                        getFingerPrintRecordFacade().create(startRecord);
                    }
                }

                //UPDATE END RECORD
                FingerPrintRecord endRecord = ss.getEndRecord();
                if (endRecord != null) {
                    endRecord.setStaffShift(ss);
                    saveHistory(endRecord);

                    if (endRecord.getId() != null) {
                        getFingerPrintRecordFacade().edit(endRecord);
                    } else {
                        getFingerPrintRecordFacade().create(endRecord);
                    }
                }

                //Update Extra Duty
                HrForm additionalForm = ss.getAdditionalForm();
                if (additionalForm != null) {
                    switch (additionalForm.getTimes()) {
                        case inTime:
                            startRecord.setAllowedExtraDuty(true);
                            break;
                        case outTime:
                            endRecord.setAllowedExtraDuty(true);
                            break;
                        case All:
                            startRecord.setAllowedExtraDuty(true);
                            endRecord.setAllowedExtraDuty(true);
                            break;
                    }
                } else {
                    if (startRecord != null) {
                        startRecord.setAllowedExtraDuty(false);
                    }
                    if (endRecord != null) {
                        endRecord.setAllowedExtraDuty(false);
                    }
                }

                //Ress Old Calculated Data
                ss.reset();

                //Fetch Value for Oer Time per Month
                double valueForOverTime = humanResourceBean.getOverTimeValue(ss.getStaff(), ss.getShiftDate());

                //Chang to Second
                ss.setOverTimeValuePerSecond(valueForOverTime / (200 * 60 * 60));

                //UPDATE Staff Shift Time Only if working days
                ss.calCulateTimes();
                //Update Extra Time
                ss.calExtraTimeWithStartOrEndRecord();
                //Update Staff Shift OT time if DayOff or Sleeping Day                
                ss.calExtraTimeComplete();

                ss.calMultiplyingFactor(ss.getShift().getDayType());
                if (ss.getShift().getDayType() != DayType.DayOff) {
                    DayType dt = humanResourceBean.isHolidayWithDayType(ss.getShiftDate());
                    ss.calMultiplyingFactor(dt);
                }

                //UPDATE Leave
                ss.calLeaveTime();
                //Update Lieu Leave
                ss.calLieu();

                getStaffShiftFacade().edit(ss);

            }

            if (newSh.getStaffShift() != null && !newSh.getStaffShift().isEmpty()) {
                tmpShiftTable.add(newSh);
            }
        }

        shiftTables = new ArrayList<>();
        shiftTables.addAll(tmpShiftTable);

        if (shiftTables.isEmpty()) {
            JsfUtil.addSuccessMessage("All Record Successfully Updated");
        }

    }

    public boolean isBackButtonIsActive() {
        return backButtonIsActive;
    }

    public void setBackButtonIsActive(boolean backButtonIsActive) {
        this.backButtonIsActive = backButtonIsActive;
    }

    public String getBackButtonPage() {
        return backButtonPage;
    }

    /**
     *
     * Getters and Setters
     *
     */
    public void setBackButtonPage(String backButtonPage) {
        this.backButtonPage = backButtonPage;
    }

    public FingerPrintRecordFacade getFingerPrintRecordFacade() {
        return fingerPrintRecordFacade;
    }

    public void setFingerPrintRecordFacade(FingerPrintRecordFacade fingerPrintRecordFacade) {
        this.fingerPrintRecordFacade = fingerPrintRecordFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<ShiftTable> getShiftTables() {
        return shiftTables;
    }

    public void setShiftTables(List<ShiftTable> shiftTables) {
        this.shiftTables = shiftTables;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public ShiftFingerPrintAnalysisController() {
    }

    public List<String> getErrorMessage() {
        if (errorMessage == null) {
            errorMessage = new ArrayList<>();
        }
        return errorMessage;
    }

    public void setErrorMessage(List<String> errorMessage) {
        this.errorMessage = errorMessage;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}

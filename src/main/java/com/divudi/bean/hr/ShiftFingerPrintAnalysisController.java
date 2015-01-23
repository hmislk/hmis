/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.dataStructure.ShiftTable;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.Times;
import static com.divudi.data.hr.Times.inTime;
import static com.divudi.data.hr.Times.outTime;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Form;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.FingerPrintRecordHistory;
import com.divudi.entity.hr.HrForm;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffLeaveEntitle;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.FingerPrintRecordHistoryFacade;
import com.divudi.facade.FormFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffShiftFacade;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

    Date fromDate;
    Date toDate;
    Roster roster;
    List<ShiftTable> shiftTables;
    @EJB
    HumanResourceBean humanResourceBean;
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    StaffShiftFacade staffShiftFacade;
    @Inject
    SessionController sessionController;
    @EJB
    FingerPrintRecordFacade fingerPrintRecordFacade;
    @EJB
    StaffLeaveFacade staffLeaveFacade;
    private List<String> errorMessage = null;
    boolean flag;

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
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

    public void listenStart(StaffShift staffShift) {
        if (staffShift == null) {
            return;
        }

        if (staffShift.getStartRecord().getLoggedRecord() != null) {
            return;
        }

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
        staffShift.getEndRecord().setRecordTimeStamp(staffShift.getShiftEndTime());

//        fingerPrintRecordFacade.edit(staffShift.getEndRecord());
//        staffShiftFacade.edit(staffShift);
    }

    public void selectRosterLstener() {
        makeTableNull();
    }

    private boolean errorCheck() {
        if (getFromDate() == null || getToDate() == null) {
            return true;
        }
        return false;
    }
    DayType dayType;
    @EJB
    FormFacade formFacade;

//    private AdditionalForm fetchAdditionalForm(StaffShift staffShift) {
//        String sql = "Select a from AdditionalForm a "
//                + " where a.retired=false"
//                + " and a.staffShift=:stf ";
//        HashMap hm = new HashMap();
//        hm.put("stf", staffShift);
//
//        return (AdditionalForm) formFacade.findBySQL(sql, hm);
//    }
    private void fetchTimeFromAddiationalFrom(StaffShift ss, FingerPrintRecord fingerPrintRecordIn, FingerPrintRecord fingerPrintRecordOut, List<FingerPrintRecord> fingerPrintRecords) {

        System.err.println("Fetch From Additional");
        HrForm additionalForm = ss.getAdditionalForm();

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
            }
            fingerPrintRecordIn.setTimes(Times.inTime);
            fingerPrintRecords.add(fingerPrintRecordIn);
            ss.setStartRecord(fingerPrintRecordIn);
        }

        if (fingerPrintRecordOut == null && additionalForm.getTimes() == Times.outTime) {
            fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(additionalForm);

            if (fingerPrintRecordOut == null) {
                fingerPrintRecordOut = new FingerPrintRecord();
                fingerPrintRecordOut.setTimes(Times.outTime);
                fingerPrintRecordOut.setCreatedAt(new Date());
                fingerPrintRecordOut.setCreater(sessionController.getLoggedUser());
                fingerPrintRecordOut.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                fingerPrintRecordOut.setComments("(new Additional)");
                fingerPrintRecordOut.setRecordTimeStamp(additionalForm.getToTime());
                fingerPrintRecordFacade.create(fingerPrintRecordOut);
                fingerPrintRecords.add(fingerPrintRecordOut);
                ss.setEndRecord(fingerPrintRecordOut);
            }

        }

        if (fingerPrintRecordIn == null && fingerPrintRecordOut == null && additionalForm.getTimes() == Times.All) {
            fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(additionalForm);
            fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(additionalForm);

            if (fingerPrintRecordIn == null) {
                fingerPrintRecordIn = new FingerPrintRecord();
                fingerPrintRecordIn.setTimes(Times.inTime);
                fingerPrintRecordIn.setCreatedAt(new Date());
                fingerPrintRecordIn.setCreater(sessionController.getLoggedUser());
                fingerPrintRecordIn.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                fingerPrintRecordIn.setComments("(new Additional)");
                fingerPrintRecordIn.setRecordTimeStamp(additionalForm.getFromTime());
                fingerPrintRecordFacade.create(fingerPrintRecordIn);
                fingerPrintRecords.add(fingerPrintRecordIn);
                ss.setStartRecord(fingerPrintRecordIn);
            }

            if (fingerPrintRecordOut == null) {
                fingerPrintRecordOut = new FingerPrintRecord();
                fingerPrintRecordOut.setTimes(Times.outTime);
                fingerPrintRecordOut.setCreatedAt(new Date());
                fingerPrintRecordOut.setCreater(sessionController.getLoggedUser());
                fingerPrintRecordOut.setFingerPrintRecordType(FingerPrintRecordType.Varified);
                fingerPrintRecordOut.setComments("(new Additional)");
                fingerPrintRecordOut.setRecordTimeStamp(additionalForm.getToTime());
                fingerPrintRecordFacade.create(fingerPrintRecordOut);
                fingerPrintRecords.add(fingerPrintRecordOut);
                ss.setEndRecord(fingerPrintRecordOut);
            }

        }

        switch (additionalForm.getTimes()) {
            case inTime:
                fingerPrintRecordIn.setAllowedExtraDuty(true);
                break;
            case outTime:
                fingerPrintRecordOut.setAllowedExtraDuty(true);
                break;
            case All:
                fingerPrintRecordIn.setAllowedExtraDuty(true);
                fingerPrintRecordOut.setAllowedExtraDuty(true);
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
        ss.setDayType(null);

        DayType dtp = phDateController.getHolidayType(ss.getShiftDate());
        ss.setDayType(dtp);
        if (ss.getDayType() == null) {
            if (ss.getShift() != null) {
                ss.setDayType(ss.getShift().getDayType());
            }
        }
    }

    private void fetchAndSetStaffLeave(StaffShift ss) {
        ss.setLeaveType(null);
        StaffLeave staffLeave = getHumanResourceBean().fetchFirstStaffLeave(ss.getStaff(), ss.getShiftDate());
        //Setting Leave Type To StaffShift From Staff Leave
        if (staffLeave != null) {
            ss.setLeaveType(staffLeave.getLeaveType());
        }

    }

    @Inject
    PhDateController phDateController;

    public void createShiftTable() {
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        System.out.println("Line2 = " + new Date());

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
                System.err.println("******** " + ss.getShift().getName() + ":::" + ss.getStaff().getPerson().getName());
                fetchAndSetStaffLeave(ss);
                fetchAndSetDayType(ss);

                List<FingerPrintRecord> list = new ArrayList<>();
                FingerPrintRecord fingerPrintRecordIn = ss.getStartRecord();
                FingerPrintRecord fingerPrintRecordOut = ss.getEndRecord();

                HrForm additionalForm = ss.getAdditionalForm();

                if (additionalForm == null) {
                    if (fingerPrintRecordIn == null) {
                        fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(ss);

                    }

                    if (fingerPrintRecordOut == null) {
                        fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(ss);
                    }
                } else {
                    //Fetch Time From Additional From
                    fetchTimeFromAddiationalFrom(ss, fingerPrintRecordIn, fingerPrintRecordOut, list);
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

                System.err.println("2 " + fingerPrintRecordIn + " : " + fingerPrintRecordOut);

                FingerPrintRecord fpr = null;
                if (ss.getStartRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.inTime);
                    list.add(fpr);
                    ss.setStartRecord(fpr);

//                        staffShiftFacade.edit(ss);
                    if (ss.getPreviousStaffShift() != null) {
                        System.err.println("PREV************************");
                        ss.getStartRecord().setComments("(NEW PREV)");
                        ss.getStartRecord().setRecordTimeStamp(ss.getShiftStartTime());

                    }

                }

                if (ss.getEndRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.outTime);
                    list.add(fpr);
                    ss.setEndRecord(fpr);
//                        staffShiftFacade.edit(ss);

                    if (ss.getNextStaffShift() != null) {
                        System.err.println("NEXT*****************");
                        ss.getEndRecord().setComments("(NEW NEXT)");
                        ss.getEndRecord().setRecordTimeStamp(ss.getShiftEndTime());
                    }
                }

                System.err.println("3 " + fingerPrintRecordIn + " : " + fingerPrintRecordOut);
                ss.setFingerPrintRecordList(getHumanResourceBean().fetchMissedFingerFrintRecord(ss));
                ss.getFingerPrintRecordList().addAll(list);
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

    @Inject
    StaffLeaveFromLateAndEarlyController staffLeaveFromLateAndEarlyController;

    public void calStaffLeaveFromLate(Staff staff) {
        List<StaffShift> staffShifts = humanResourceBean.fetchStaffShiftForAddingLeave(fromDate, toDate, staff);

        if (staff == null) {
            return;
        }

        if (staffShifts == null) {
            return;
        }

        if (staffShifts.isEmpty()) {
            return;
        }

        List<StaffShift> stfLateIn10 = staffLeaveFromLateAndEarlyController.fetchStaffShiftLateIn(staff, 10 * 60, 90 * 60);
        List<StaffShift> stfEarlyOut30 = staffLeaveFromLateAndEarlyController.fetchStaffShiftEarlyOut(staff, 30 * 60, 90 * 60);
        LinkedList<StaffShift> staffShiftLateInTenMinuteLinked = new LinkedList<>();
        LinkedList<StaffShift> staffShiftEarlyOutThirtyMinuteLinked = new LinkedList<>();

        if (stfLateIn10 != null) {
            for (StaffShift stf : stfLateIn10) {
                staffShiftLateInTenMinuteLinked.add(stf);
            }
        }

        if (stfEarlyOut30 != null) {
            for (StaffShift stf : stfEarlyOut30) {
                staffShiftEarlyOutThirtyMinuteLinked.add(stf);
            }
        }

        for (StaffShift stf : staffShifts) {
            if (staffShiftLateInTenMinuteLinked.size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    StaffShift lateShift = staffShiftLateInTenMinuteLinked.pollFirst();
                    lateShift.setReferenceStaffShiftLateIn(stf);
                    lateShift.setConsideredForLateEarlyAttendance(true);
                    staffShiftFacade.edit(lateShift);
                }

                LeaveType leaveType = getLeaveType(staff, commonFunctions.getFirstDayOfYear(stf.getShiftDate()), commonFunctions.getLastDayOfYear(stf.getShiftDate()));
                HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(staff, leaveType, stf.getShiftDate(), stf.getShiftDate());
                staffLeaveFromLateAndEarlyController.saveStaffLeaves(staff, leaveType, stf.getShiftDate(), hr);
                staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stf, leaveType, hr);
            }
        }

        staffShifts = humanResourceBean.fetchStaffShiftForAddingLeave(fromDate, toDate, staff);

        if (staffShifts == null) {
            return;
        }

        if (staffShifts.isEmpty()) {
            return;
        }

        for (StaffShift stf : staffShifts) {
            if (staffShiftEarlyOutThirtyMinuteLinked.size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    StaffShift earlyOut = staffShiftEarlyOutThirtyMinuteLinked.pollFirst();
                    earlyOut.setReferenceStaffShiftEarlyOut(stf);
                    earlyOut.setConsideredForLateEarlyAttendance(true);
                    staffShiftFacade.edit(earlyOut);
                }

                LeaveType leaveType = getLeaveType(staff, commonFunctions.getFirstDayOfYear(stf.getShiftDate()), commonFunctions.getLastDayOfYear(stf.getShiftDate()));
                HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(staff, leaveType, stf.getShiftDate(), stf.getShiftDate());
                staffLeaveFromLateAndEarlyController.saveStaffLeaves(staff, leaveType, stf.getShiftDate(), hr);
                staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stf, leaveType, hr);
            }
        }

    }

    public LeaveType getLeaveType(Staff staff, Date fromDate, Date toDate) {
        StaffLeaveEntitle staffLeaveEntitle = humanResourceBean.fetchStaffLeaveEntitle(staff, LeaveType.Annual, fromDate, toDate);

        if (staffLeaveEntitle.getCount() > humanResourceBean.fetchStaffLeave(staff, LeaveType.Annual, fromDate, toDate)) {
            return LeaveType.AnnualHalf;
        }

        staffLeaveEntitle = humanResourceBean.fetchStaffLeaveEntitle(staff, LeaveType.Casual, fromDate, toDate);

        if (staffLeaveEntitle.getCount() > humanResourceBean.fetchStaffLeave(staff, LeaveType.Casual, fromDate, toDate)) {
            return LeaveType.CasualHalf;
        }

        return LeaveType.No_Pay_Half;

    }

    public void createShiftTableAdditional() {
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();
//        System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        System.out.println("Line2 = " + new Date());

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
                System.err.println("******** " + ss.getShift().getName() + ":::" + ss.getStaff().getPerson().getName());
                fetchAndSetStaffLeave(ss);
                fetchAndSetDayType(ss);

                List<FingerPrintRecord> list = new ArrayList<>();
                FingerPrintRecord fingerPrintRecordIn = ss.getStartRecord();
                FingerPrintRecord fingerPrintRecordOut = ss.getEndRecord();

                HrForm additionalForm = ss.getAdditionalForm();

                if (additionalForm == null) {
                    if (fingerPrintRecordIn == null) {
                        fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(ss);

                    }

                    if (fingerPrintRecordOut == null) {
                        fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(ss);
                    }
                } else {
                    //Fetch Time From Additional From
                    fetchTimeFromAddiationalFrom(ss, fingerPrintRecordIn, fingerPrintRecordOut, list);
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

                System.err.println("2 " + fingerPrintRecordIn + " : " + fingerPrintRecordOut);

                FingerPrintRecord fpr = null;
                if (ss.getStartRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.inTime);
                    list.add(fpr);
                    ss.setStartRecord(fpr);

//                        staffShiftFacade.edit(ss);
                    if (ss.getPreviousStaffShift() != null) {
                        System.err.println("PREV************************");
                        ss.getStartRecord().setComments("(NEW PREV)");
                        ss.getStartRecord().setRecordTimeStamp(ss.getShiftStartTime());

                    }

                }

                if (ss.getEndRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.outTime);
                    list.add(fpr);
                    ss.setEndRecord(fpr);
//                        staffShiftFacade.edit(ss);

                    if (ss.getNextStaffShift() != null) {
                        System.err.println("NEXT*****************");
                        ss.getEndRecord().setComments("(NEW NEXT)");
                        ss.getEndRecord().setRecordTimeStamp(ss.getShiftEndTime());
                    }
                }

                System.err.println("3 " + fingerPrintRecordIn + " : " + fingerPrintRecordOut);
                ss.setFingerPrintRecordList(getHumanResourceBean().fetchMissedFingerFrintRecord(ss));
                ss.getFingerPrintRecordList().addAll(list);
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
//        System.out.println("Line1 = " + new Date());

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();
//        System.out.println("Line2 = " + new Date());

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
                System.err.println("******** " + ss.getShift().getName() + ":::" + ss.getStaff().getPerson().getName());
                fetchAndSetStaffLeave(ss);
                fetchAndSetDayType(ss);

                List<FingerPrintRecord> list = new ArrayList<>();
                FingerPrintRecord fingerPrintRecordIn = null;
                FingerPrintRecord fingerPrintRecordOut = null;

                HrForm additionalForm = ss.getAdditionalForm();

                if (additionalForm == null) {
                    fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(ss);
                    fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(ss);
                } else {
                    //Fetch Time From Additional From
                    fetchTimeFromAddiationalFrom(ss, fingerPrintRecordIn, fingerPrintRecordOut, list);
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

                System.err.println("2 " + fingerPrintRecordIn + " : " + fingerPrintRecordOut);

                FingerPrintRecord fpr = null;
                if (ss.getStartRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.inTime);
                    list.add(fpr);
                    ss.setStartRecord(fpr);

//                        staffShiftFacade.edit(ss);
                    if (ss.getPreviousStaffShift() != null) {
                        System.err.println("PREV************************");
                        ss.getStartRecord().setComments("(NEW PREV)");
                        ss.getStartRecord().setRecordTimeStamp(ss.getShiftStartTime());

                    }

                }

                if (ss.getEndRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.outTime);
                    list.add(fpr);
                    ss.setEndRecord(fpr);
//                        staffShiftFacade.edit(ss);

                    if (ss.getNextStaffShift() != null) {
                        System.err.println("NEXT*****************");
                        ss.getEndRecord().setComments("(NEW NEXT)");
                        ss.getEndRecord().setRecordTimeStamp(ss.getShiftEndTime());
                    }
                }

                System.err.println("3 " + fingerPrintRecordIn + " : " + fingerPrintRecordOut);
                ss.setFingerPrintRecordList(getHumanResourceBean().fetchMissedFingerFrintRecord(ss));
                ss.getFingerPrintRecordList().addAll(list);
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
            UtilityController.addErrorMessage("This record associated with anther staff shift");
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
            UtilityController.addErrorMessage("This record associated with anther staff shift");
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
//                        UtilityController.addErrorMessage(errorMessage);
//                        return true;
//                    }
//                    if (ss.getStartRecord().getRecordTimeStamp() == null) {
//                        errorMessage += " Some Starting Records Has No Time ";
//                        UtilityController.addErrorMessage(errorMessage);
//                        return true;
//                    }
//                }
//
//                if (ss.getNextStaffShift() == null) {
//                    if (ss.getEndRecord() == null) {
//                        errorMessage += " Some End Records Has No Starting Record";
//                        UtilityController.addErrorMessage(errorMessage);
//                        return true;
//                    }
//                    if (ss.getEndRecord().getRecordTimeStamp() == null) {
//                        errorMessage += " Some End Records Has No Time ";
//                        UtilityController.addErrorMessage(errorMessage);
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

        if (ss.getShift().getDayType() == DayType.DayOff
                || ss.getShift().getDayType() == DayType.PublicHoliday
                || ss.getShift().getDayType() == DayType.SleepingDay
                || ss.getLeaveType() != null) {
            return false;
        }

        if (ss.getPreviousStaffShift() == null) {
            if (ss.getStartRecord() == null) {
                message = date
                        + " -> " + code
                        + "  Has No Starting Record";
                errorMessage.add(message);
//                        System.err.println("SS " + ss.getId());
//                    UtilityController.addErrorMessage(errorMessage);
                shiftTable.getStaffShift().add(ss);
                return true;
            }
            if (ss.getStartRecord().getRecordTimeStamp() == null) {
                message = date
                        + " -> " + code
                        + " Some Starting Records Has No Time \r ";
                errorMessage.add(message);
//                    UtilityController.addErrorMessage(errorMessage);
                shiftTable.getStaffShift().add(ss);
                return true;
            }
        }

        if (ss.getNextStaffShift() == null) {
            if (ss.getEndRecord() == null) {
                message = date
                        + " -> " + code
                        + " Some End Records Has No Starting Record \r";
                errorMessage.add(message);
                shiftTable.getStaffShift().add(ss);
//                    UtilityController.addErrorMessage(errorMessage);
                return true;
            }
            if (ss.getEndRecord().getRecordTimeStamp() == null) {
                message = date
                        + " -> " + code
                        + " Some End Records Has No Time \r ";
                errorMessage.add(message);
                shiftTable.getStaffShift().add(ss);
//                    UtilityController.addErrorMessage(errorMessage);
                return true;
            }
        }

        return false;
    }

    @EJB
    FingerPrintRecordHistoryFacade fingerPrintRecordHistoryFacade;

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

            System.err.println("Date 1 " + date1);
            System.err.println("Date 2 " + date2);

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

//        System.err.println("1");
        if (shiftTables == null) {
            final String empty_List = "Empty List";
            UtilityController.addErrorMessage(empty_List);
            errorMessage.add(empty_List);
            return;
        }

//        System.err.println("2");
//        if (errorCheckForSave()) {
////            UtilityController.addErrorMessage("Staff Shift Not Updated");
//            return;
//        }
        for (ShiftTable st : shiftTables) {
            ShiftTable newSh = new ShiftTable();
            newSh.setDate(st.getDate());
            newSh.setFlag(st.isFlag());
            for (StaffShift ss : st.getStaffShift()) {
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
                double valueForOverTime = humanResourceBean.calValueForOverTime(ss.getStaff(), ss.getShiftDate());

                //Chang to Second
                ss.setOverTimeValuePerSecond(valueForOverTime / (200 * 60 * 60));

                //UPDATE Staff Shift Time Only if working days
                ss.calCulateTimes();
                //Update Extra Time
                ss.calExtraTimeWithStartOrEndRecord();
                //Update Staff Shift OT time if DayOff or Sleeping Day                
                ss.calExtraTimeComplete();

                ss.calMultiplyingFactor(ss.getShift().getDayType());
                DayType dt = humanResourceBean.isHolidayWithDayType(ss.getShiftDate());
                ss.calMultiplyingFactor(dt);

                //UPDATE Leave
                ss.calLeaveTime();
                //Update Lieu Leave
                ss.calLieu();

                getStaffShiftFacade().edit(ss);
            }

            tmpShiftTable.add(newSh);
        }

        shiftTables = new ArrayList<>();
        shiftTables.addAll(tmpShiftTable);

        if (shiftTables.isEmpty()) {
            UtilityController.addSuccessMessage("All Record Successfully Updated");
        }

        List<Staff> staffs = humanResourceBean.fetchStaffFromShift(fromDate, toDate);

        if (staffs == null) {
            return;
        }

        for (Staff s : staffs) {
            calStaffLeaveFromLate(s);
        }

    }

    //GETTERS AND SETTERS
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

}

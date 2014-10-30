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
import com.divudi.data.hr.Times;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.FormFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffShiftFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;

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
    public void createShiftTable() {
        if (errorCheck()) {
            return;
        }

        shiftTables = new ArrayList<>();

        Calendar nc = Calendar.getInstance();
        nc.setTime(getFromDate());
        Date nowDate = nc.getTime();

        nc.setTime(getToDate());
        nc.add(Calendar.DATE, 1);
        Date tmpToDate = nc.getTime();

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
                continue;
            }

            for (StaffShift ss : staffShifts) {
                StaffLeave staffLeave = getHumanResourceBean().fetchFirstStaffLeave(ss.getStaff(), ss.getShiftDate());

                List<FingerPrintRecord> list = new ArrayList<>();
                FingerPrintRecord fingerPrintRecordIn = getHumanResourceBean().findInTimeRecord(ss);
                FingerPrintRecord fingerPrintRecordOut = getHumanResourceBean().findOutTimeRecord(ss);

                if (fingerPrintRecordIn != null) {
                    fingerPrintRecordIn.setTimes(Times.inTime);
                    ss.setStartRecord(fingerPrintRecordIn);
                }

                if (fingerPrintRecordOut != null) {
                    fingerPrintRecordOut.setTimes(Times.outTime);
                    ss.setEndRecord(fingerPrintRecordOut);
                }

                //Setting Leave Type To StaffShift From Staff Leave
                if (staffLeave != null) {
                    ss.setLeaveType(staffLeave.getLeaveType());
                }

                FingerPrintRecord fpr = null;
                if (ss.getStartRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.inTime);
//                        fingerPrintRecordFacade.create(fpr);
                    list.add(fpr);
                    ss.setStartRecord(fpr);
//                        staffShiftFacade.edit(ss);

                    if (ss.getPreviousStaffShift() != null) {
//                            System.err.println("PREV");
                        ss.getStartRecord().setComments("(NEW PREV)");
                        ss.getStartRecord().setRecordTimeStamp(ss.getShiftStartTime());
                    }

                }

                if (ss.getEndRecord() == null) {
                    fpr = createFingerPrint(ss, FingerPrintRecordType.Varified, Times.outTime);
//                        fingerPrintRecordFacade.create(fpr);
                    list.add(fpr);
                    ss.setEndRecord(fpr);
//                        staffShiftFacade.edit(ss);

                    if (ss.getNextStaffShift() != null) {
//                            System.err.println("NEXT");
                        ss.getEndRecord().setComments("(NEW NEXT)");
                        ss.getEndRecord().setRecordTimeStamp(ss.getShiftEndTime());
                    }
                }

                if (ss.getHrForm() != null) {
                    if (ss.getHrForm() instanceof AdditionalForm) {
                        AdditionalForm additionalForm = (AdditionalForm) ss.getHrForm();

                        switch (ss.getHrForm().getTimes()) {
                            case inTime:
                                ss.getStartRecord().setAllowedExtraDuty(true);
                                ss.getStartRecord().setRecordTimeStamp(additionalForm.getFromTime());
                                break;
                            case outTime:
                                ss.getEndRecord().setAllowedExtraDuty(true);
                                ss.getEndRecord().setRecordTimeStamp(additionalForm.getToTime());
                                break;
                            case All:
                                ss.getStartRecord().setAllowedExtraDuty(true);
                                ss.getStartRecord().setRecordTimeStamp(additionalForm.getFromTime());
                                ss.getEndRecord().setAllowedExtraDuty(true);
                                ss.getEndRecord().setRecordTimeStamp(additionalForm.getToTime());
                                break;
                        }
                    }
                }

                ss.setFingerPrintRecordList(getHumanResourceBean().fetchMissedFingerFrintRecord(ss));
                ss.getFingerPrintRecordList().addAll(list);
                netT.getStaffShift().add(ss);
            }

            System.err.println("BOOL " + netT.getFlag());
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

        FingerPrintRecord fingerPrintRecord = staffShift.getStartRecord();

        if (fingerPrintRecord != null) {
            fingerPrintRecord.setTimes(Times.inTime);
        }
    }

    public void fingerPrintSelectListenerEndRecord(StaffShift staffShift) {
        if (staffShift == null) {
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
        fpr.setComments("(NEW " + times.toString() + " )");
        return fpr;
    }

    private boolean errorCheckForSave() {
        for (ShiftTable st : shiftTables) {
            for (StaffShift ss : st.getStaffShift()) {

                if (ss.getShift().getDayType() == DayType.DayOff
                        || ss.getShift().getDayType() == DayType.SleepingDay
                        || ss.getLeaveType() != null) {
                    continue;
                }

                if (ss.getPreviousStaffShift() == null) {
                    if (ss.getStartRecord() == null) {
                        System.err.println("SS " + ss.getId());
                        UtilityController.addErrorMessage(" Some Starting Records Has"
                                + " No Starting Record");
                        return true;
                    }
                    if (ss.getStartRecord().getRecordTimeStamp() == null) {
                        UtilityController.addErrorMessage(" Some Starting Records Has No Time ");
                        return true;
                    }
                }

                if (ss.getNextStaffShift() == null) {
                    if (ss.getEndRecord() == null) {
                        UtilityController.addErrorMessage(" Some End Records Has No Starting Record");
                        return true;
                    }
                    if (ss.getEndRecord().getRecordTimeStamp() == null) {
                        UtilityController.addErrorMessage(" Some End Records Has No Time ");
                        return true;
                    }
                }

            }
        }

        return false;
    }

    public void save() {
//        System.err.println("1");
        if (shiftTables == null) {
            UtilityController.addErrorMessage("Empty List");
            return;
        }

//        System.err.println("2");
        if (errorCheckForSave()) {
//            UtilityController.addErrorMessage("Staff Shift Not Updated");
            return;
        }

        for (ShiftTable st : shiftTables) {
            for (StaffShift ss : st.getStaffShift()) {
                //UPDATE START RECORD
                FingerPrintRecord startRecord = ss.getStartRecord();
                startRecord.setStaffShift(ss);
                if (startRecord.getId() != null) {
                    getFingerPrintRecordFacade().edit(startRecord);
                } else {
                    getFingerPrintRecordFacade().create(startRecord);
                }

                //UPDATE END RECORD
                FingerPrintRecord endRecord = ss.getEndRecord();
                endRecord.setStaffShift(ss);
                if (endRecord.getId() != null) {
                    getFingerPrintRecordFacade().edit(endRecord);
                } else {
                    getFingerPrintRecordFacade().create(endRecord);
                }

                //Fetch Basic
                StaffPaysheetComponent basic = humanResourceBean.getBasic(ss.getStaff());
                ss.setBasicPerSecond(basic.getStaffPaySheetComponentValue() / (200 * 60 * 60));

                //UPDATE Staff Shift Time Only if working days
                ss.calCulateTimes();

                //Update Extra Time
                ss.calExtraTimeWithStartOrEndRecord();

                //UPDATE Leave
                ss.calLeaveTime();

                //Update Staff Shift OT time if DayOff or Sleeping Day
                if (ss.getShift().getDayType() == DayType.DayOff
                        || ss.getShift().getDayType() == DayType.SleepingDay) {
//                        || ss instanceof StaffShiftReplace) {
                    ss.calExtraTimeComplete();

                }

                ss.calMultiplyingFactor(ss.getShift().getDayType());
                DayType dt = humanResourceBean.isHolidayWithDayType(ss.getShiftDate());
                ss.calMultiplyingFactor(dt);

                //Update Lieu Leave
                ss.calLieu(dt);

                getStaffShiftFacade().edit(ss);
            }
        }

        UtilityController.addSuccessMessage("All Record Successfully Updated");

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

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.data.dataStructure.ShiftTable;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftHistory;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.StaffShiftHistoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
public class ShiftTableController implements Serializable {

    Date fromDate;
    Date toDate;
    Long dateRange;
    Roster roster;
    Shift shift;
    StaffShift staffShift;
    List<ShiftTable> shiftTables;
    @EJB
    HumanResourceBean humanResourceBean;
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    StaffShiftFacade staffShiftFacade;
    @Inject
    SessionController sessionController;
    @Inject
    ShiftController shiftController;
    boolean all;

    //FUNTIONS
    public void makeNull() {
        fromDate = null;
        toDate = null;
        dateRange = 0l;
        roster = null;
        shiftTables = null;

    }

    private boolean errorCheck() {
        if (getFromDate() == null || getToDate() == null) {
            return true;
        }

        return false;
    }

    private void saveStaffShift() {
        for (ShiftTable st : shiftTables) {
            for (StaffShift ss : st.getStaffShift()) {
//                if (ss.getShift() == null) {
//                    continue;
//                }

                ss.calShiftStartEndTime();
                ss.calLieu();
                if (ss.getId() == null) {
                    getStaffShiftFacade().create(ss);
                }

                ss.setPreviousStaffShift(getHumanResourceBean().calPrevStaffShift(ss));
                ss.setNextStaffShift(getHumanResourceBean().calFrwStaffShift(ss));
                getStaffShiftFacade().edit(ss);
            }
        }
    }

    @EJB
    StaffShiftHistoryFacade staffShiftHistoryFacade;

    private void saveHistory() {
        for (ShiftTable st : shiftTables) {
            for (StaffShift ss : st.getStaffShift()) {

                if (ss.getId() != null) {
                    boolean flag = false;
                    StaffShift fetchStaffShift = staffShiftFacade.find(ss.getId());

                    if (fetchStaffShift.getRoster() != ss.getRoster()) {
                        flag = true;
                    }

                    if (fetchStaffShift.getStaff() != ss.getStaff()) {
                        flag = true;
                    }

                    if (fetchStaffShift.getShift() != ss.getShift()) {
                        flag = true;
                    }

                    if (flag) {
                        StaffShiftHistory staffShiftHistory = new StaffShiftHistory();
                        staffShiftHistory.setStaffShift(ss);
                        staffShiftHistory.setCreatedAt(new Date());
                        staffShiftHistory.setCreater(sessionController.getLoggedUser());
                        //CHanges
                        staffShiftHistory.setStaff(ss.getStaff());
                        staffShiftHistory.setShift(ss.getShift());
                        staffShiftHistory.setRoster(ss.getRoster());

                        staffShiftHistoryFacade.create(staffShiftHistory);
                    }
                }

            }
        }
    }

    public void save() {
        if (shiftTables == null) {
            return;
        }

        saveHistory();

        saveStaffShift();
        saveStaffShift();

    }

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

            for (Staff stf : getHumanResourceBean().fetchStaff(getRoster())) {
                List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShift(nowDate, stf);
                if (staffShifts.isEmpty()) {
                    for (int i = getRoster().getShiftPerDay(); i > 0; i--) {
                        StaffShift ss = new StaffShift();
                        ss.setStaff(stf);
                        ss.setShiftDate(nowDate);
                        ss.setRoster(roster);
                        netT.getStaffShift().add(ss);
                    }
                } else {
                    for (StaffShift ss : staffShifts) {
                        netT.getStaffShift().add(ss);
                    }
                }

            }
            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
        setDateRange(range + 1);
    }

    public void fetchShiftTable() {
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

//            List<StaffShift> staffShifts = getHumanResourceBean().fetchStaffShift(nowDate, roster);
//
//            for (StaffShift ss : staffShifts) {
//                netT.getStaffShift().add(ss);
//            }
            List<Staff> staffs = getHumanResourceBean().fetchStaffShift(fromDate, toDate, roster);

            for (Staff staff : staffs) {
                List<StaffShift> ss = getHumanResourceBean().fetchStaffShift(nowDate, staff);
                if (ss == null) {
                    for (int i = 0; i < roster.getShiftPerDay(); i++) {
                        StaffShift newStaffShift = new StaffShift();
                        newStaffShift.setStaff(staff);
                        newStaffShift.setShiftDate(nowDate);
                        newStaffShift.setCreatedAt(new Date());
                        newStaffShift.setCreater(sessionController.getLoggedUser());
                        netT.getStaffShift().add(newStaffShift);
                    }
                } else {
                    netT.getStaffShift().addAll(ss);
                    int ballance = roster.getShiftPerDay() - ss.size();
                    if (ballance < 0) {
                        continue;
                    }
                    for (int i = 0; i < ballance; i++) {
                        StaffShift newStaffShift = new StaffShift();
                        newStaffShift.setStaff(staff);
                        newStaffShift.setShiftDate(nowDate);
                        newStaffShift.setCreatedAt(new Date());
                        newStaffShift.setCreater(sessionController.getLoggedUser());
                        netT.getStaffShift().add(newStaffShift);
                    }

                }
            }

            shiftTables.add(netT);

            Calendar c = Calendar.getInstance();
            c.setTime(nowDate);
            c.add(Calendar.DATE, 1);
            nowDate = c.getTime();

        }

        Long range = getCommonFunctions().getDayCount(getFromDate(), getToDate());
        setDateRange(range + 1);
    }

    public void selectRosterLstener() {
        makeTableNull();
        getShiftController().setCurrentRoster(roster);

    }

    public void makeTableNull() {
        shiftTables = null;
    }

    //GETTER AND SETTERS
    public ShiftController getShiftController() {
        return shiftController;
    }

    public void setShiftController(ShiftController shiftController) {
        this.shiftController = shiftController;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public Long getDateRange() {
        return dateRange;
    }

    public void setDateRange(Long dateRange) {
        this.dateRange = dateRange;
    }

    public List<ShiftTable> getShiftTables() {
        return shiftTables;
    }

    public void setShiftTables(List<ShiftTable> shiftTables) {
        this.shiftTables = shiftTables;
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

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public void visible() {
        all = true;
    }

    public void hide() {
        all = false;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public StaffShiftHistoryFacade getStaffShiftHistoryFacade() {
        return staffShiftHistoryFacade;
    }

    public void setStaffShiftHistoryFacade(StaffShiftHistoryFacade staffShiftHistoryFacade) {
        this.staffShiftHistoryFacade = staffShiftHistoryFacade;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
    }

}

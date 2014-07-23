/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.ShiftAmendment;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.ShiftAmendmentFacade;
import com.divudi.facade.StaffShiftFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ShiftAmendmentController implements Serializable {

    private ShiftAmendment shiftAmendment1;
    private ShiftAmendment shiftAmendment2;
    @EJB
    StaffShiftFacade staffShiftFacade;
    @EJB
    ShiftAmendmentFacade shiftAmendmentFacade;
    @Inject
    SessionController sessionController;

    private boolean checkShiftExist(Date date, Shift shift) {
        String sql = "Select s from StaffShift s "
                + " where s.retired=false "
                + " and s.shift=:sh "
                + " and s.shiftDate=:dt ";
        HashMap hm = new HashMap();
        hm.put("dt", date);
        hm.put("sh", shift);

        StaffShift staffShift = staffShiftFacade.findFirstBySQL(sql, hm, TemporalType.DATE);

        return staffShift != null;
    }

    private boolean checkShiftExist(Date date, Shift shift, Staff staff) {
        String sql = "Select s from StaffShift s "
                + " where s.retired=false "
                + " and s.shift=:sh "
                + " and s.staff=:stf "
                + " and s.shiftDate=:dt ";
        HashMap hm = new HashMap();
        hm.put("dt", date);
        hm.put("sh", shift);
        hm.put("stf", staff);

        StaffShift staffShift = staffShiftFacade.findFirstBySQL(sql, hm, TemporalType.DATE);

        return staffShift != null;
    }

    public void change() {
        if (shiftAmendment2 == null) {
            if (getShiftAmendment1().getFromDate() == null || getShiftAmendment1().getFromShift() == null) {
                UtilityController.addErrorMessage("Check FromShift");
                return;
            }

            if (getShiftAmendment1().getToDate() == null || getShiftAmendment1().getToShift() == null) {
                UtilityController.addErrorMessage("Check To Shift");
                return;
            }

            if (!checkShiftExist(getShiftAmendment1().getFromDate(), getShiftAmendment1().getFromShift(), getShiftAmendment1().getStaff())) {
                UtilityController.addErrorMessage("There is No Shift Defined");
                return;
            }

            if (checkShiftExist(getShiftAmendment1().getToDate(), getShiftAmendment1().getToShift())) {
                UtilityController.addErrorMessage("There is Already Staff Shift There");
                return;
            }

            getShiftAmendment1().setCreater(sessionController.getLoggedUser());
            getShiftAmendment1().setCreatedAt(new Date());
            shiftAmendmentFacade.create(getShiftAmendment1());

            StaffShift staffShift = new StaffShift();
            staffShift.setCreatedAt(new Date());
            staffShift.setCreater(sessionController.getLoggedUser());
            staffShift.setStaff(shiftAmendment1.getStaff());
            staffShift.setShiftDate(shiftAmendment1.getToDate());
            staffShift.setShift(shiftAmendment1.getToShift());

            staffShiftFacade.create(staffShift);
        }

    }

    /**
     * Creates a new instance of ShiftAmendmentController
     */
    public ShiftAmendmentController() {
    }

    public ShiftAmendment getShiftAmendment1() {
        if (shiftAmendment1 == null) {
            shiftAmendment1 = new ShiftAmendment();
        }
        return shiftAmendment1;
    }

    public void setShiftAmendment1(ShiftAmendment shiftAmendment1) {
        this.shiftAmendment1 = shiftAmendment1;
    }

    public ShiftAmendment getShiftAmendment2() {
        if (shiftAmendment2 == null) {
            shiftAmendment2 = new ShiftAmendment();
        }
        return shiftAmendment2;
    }

    public void setShiftAmendment2(ShiftAmendment shiftAmendment2) {
        this.shiftAmendment2 = shiftAmendment2;
    }

}

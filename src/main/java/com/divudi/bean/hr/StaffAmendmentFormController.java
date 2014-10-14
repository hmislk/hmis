/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import static com.divudi.bean.common.UtilityController.addSuccessMessage;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.AmendmentForm;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftHistory;
import com.divudi.facade.AmendmentFormFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.StaffShiftHistoryFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Named(value = "staffAmendmentFormController")
@SessionScoped
public class StaffAmendmentFormController implements Serializable {

    AmendmentForm currAmendmentForm;
    @EJB
    AmendmentFormFacade amendmentFormFacade;
    @Inject
    SessionController sessionController;

    public StaffAmendmentFormController() {
    }

    public void clear() {
        currAmendmentForm = null;
    }

    public boolean errorCheck() {
        if (currAmendmentForm.getFromStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }
        if (currAmendmentForm.getToStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }

        if (currAmendmentForm.getFromDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }

        if (currAmendmentForm.getToDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }

        if (currAmendmentForm.getApprovedStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Person");
            return true;
        }
        if (currAmendmentForm.getApprovedAt() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Date");
            return true;
        }
        if (currAmendmentForm.getComments() == null || "".equals(currAmendmentForm.getComments())) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return true;
        }

        return false;
    }

    public void saveAmendmentForm() {
        if (errorCheck()) {
            return;
        }
        currAmendmentForm.setCreatedAt(new Date());
        currAmendmentForm.setCreater(getSessionController().getLoggedUser());
        getAmendmentFormFacade().create(currAmendmentForm);

        //CHange SHifts
        StaffShift fromStaffShift = getCurrAmendmentForm().getFromStaffShift();
        StaffShift toStaffShift = getCurrAmendmentForm().getToStaffShift();
        Staff fromStaff = getCurrAmendmentForm().getFromStaff();
        Staff toStaff = getCurrAmendmentForm().getToStaff();

        fromStaffShift.setStaff(toStaff);
        toStaffShift.setStaff(fromStaff);

        StaffShiftHistory staffShiftHistory = new StaffShiftHistory();
        staffShiftHistory.setCreatedAt(new Date());
        staffShiftHistory.setCreater(sessionController.getLoggedUser());
        staffShiftHistory.setStaff(fromStaff);
        staffShiftHistory.setStaffShift(fromStaffShift);
        staffShiftHistoryFacade.create(staffShiftHistory);

        staffShiftHistory = new StaffShiftHistory();
        staffShiftHistory.setCreatedAt(new Date());
        staffShiftHistory.setCreater(sessionController.getLoggedUser());
        staffShiftHistory.setStaff(toStaff);
        staffShiftHistory.setStaffShift(toStaffShift);
        staffShiftHistoryFacade.create(staffShiftHistory);

        staffShiftFacade.edit(fromStaffShift);
        staffShiftFacade.edit(toStaffShift);

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

    @EJB
    StaffShiftHistoryFacade staffShiftHistoryFacade;

    List<StaffShift> fromStaffShifts;
    List<StaffShift> toStaffShifts;
    @EJB
    StaffShiftFacade staffShiftFacade;

    public void fetchFromStaffShift() {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shiftDate=:dt "
                + " and c.staff=:stf ";

        hm.put("dt", getCurrAmendmentForm().getFromDate());
        hm.put("stf", getCurrAmendmentForm().getStaff());

        fromStaffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void fetchToStaffShift() {
        HashMap hm = new HashMap();
        String sql = " select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shiftDate=:dt "
                + " and c.staff=:stf ";

        hm.put("dt", getCurrAmendmentForm().getToDate());
        hm.put("stf", getCurrAmendmentForm().getStaff());

        toStaffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public List<StaffShift> getFromStaffShifts() {
        return fromStaffShifts;
    }

    public void setFromStaffShifts(List<StaffShift> fromStaffShifts) {
        this.fromStaffShifts = fromStaffShifts;
    }

    public List<StaffShift> getToStaffShifts() {
        return toStaffShifts;
    }

    public void setToStaffShifts(List<StaffShift> toStaffShifts) {
        this.toStaffShifts = toStaffShifts;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public AmendmentForm getCurrAmendmentForm() {
        if (currAmendmentForm == null) {
            currAmendmentForm = new AmendmentForm();
        }
        return currAmendmentForm;
    }

    public void setCurrAmendmentForm(AmendmentForm currAmendmentForm) {
        this.currAmendmentForm = currAmendmentForm;
    }

    public AmendmentFormFacade getAmendmentFormFacade() {
        return amendmentFormFacade;
    }

    public void setAmendmentFormFacade(AmendmentFormFacade amendmentFormFacade) {
        this.amendmentFormFacade = amendmentFormFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}

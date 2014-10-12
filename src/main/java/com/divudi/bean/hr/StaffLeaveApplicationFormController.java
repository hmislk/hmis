/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.data.hr.LeaveType;
import com.divudi.entity.hr.LeaveForm;
import com.divudi.facade.LeaveFormFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Sniper 619
 */
@Named(value = "staffLeaveApplicationFormController")
@SessionScoped
public class StaffLeaveApplicationFormController implements Serializable {

    LeaveForm currentLeaveForm;
    @EJB
    LeaveFormFacade leaveFormFacade;
    @Inject
    SessionController sessionController;

    public StaffLeaveApplicationFormController() {
    }

    public LeaveType[] getLeaveType() {
        return LeaveType.values();
    }

    public boolean errorCheck() {
        if (currentLeaveForm.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }
        if (currentLeaveForm.getLeaveType() == null) {
            JsfUtil.addErrorMessage("Please Enter Leave Type");
            return true;
        }
        if (currentLeaveForm.getSubDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Department");
            return true;
        }
        if (currentLeaveForm.getRequestedDate() == null) {
            JsfUtil.addErrorMessage("Please Select Date");
            return true;
        }
        if ((currentLeaveForm.getLeaveType() == LeaveType.Lieu) && (currentLeaveForm.getForDate() == null)) {
            JsfUtil.addErrorMessage("Please Select \"Leave For Date\" ");
            return true;
        }
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

    public void saveLeaveform() {
        if (errorCheck()) {
            return;
        }
        currentLeaveForm.setCreater(getSessionController().getLoggedUser());
        currentLeaveForm.setCreatedAt(new Date());
        getLeaveFormFacade().create(currentLeaveForm);
        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
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

}

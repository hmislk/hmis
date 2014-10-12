/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.entity.hr.AmendmentForm;
import com.divudi.facade.AmendmentFormFacade;
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
    
    public void clear(){
        currAmendmentForm=null;
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
        if (currAmendmentForm.getFromDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Section");
            return true;
        }
        if (currAmendmentForm.getToDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Section");
            return true;
        }
        if (currAmendmentForm.getFromFDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }
        if (currAmendmentForm.getFromTDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }
        if (currAmendmentForm.getToFDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }
        if (currAmendmentForm.getToTDate() == null) {
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
        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
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

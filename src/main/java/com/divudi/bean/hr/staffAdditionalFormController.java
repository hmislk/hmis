/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.data.hr.LeaveType;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.facade.AdditionalFormFacade;
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
@Named(value = "staffAdditionalFormController")
@SessionScoped
public class staffAdditionalFormController implements Serializable {
    
    private AdditionalForm currentAdditionalForm;
    @EJB
    private AdditionalFormFacade additionalFormFacade;
    @Inject
    private SessionController sessionController;
    
    public staffAdditionalFormController() {
    }
    
    public void clear(){
        currentAdditionalForm=null;
    }
    
    public boolean errorCheck() {
        if (currentAdditionalForm.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }
        if (currentAdditionalForm.getRequestDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Section");
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
        if (currentAdditionalForm.getApproved() == null) {
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
        
        return false;
    }
    
    public void saveAdditionalForm() {
        if (errorCheck()) {
            return;
        }
        currentAdditionalForm.setCreatedAt(new Date());
        currentAdditionalForm.setCreater(getSessionController().getLoggedUser());
        getAdditionalFormFacade().create(currentAdditionalForm);
        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }
    
    public AdditionalForm getCurrentAdditionalForm() {
        if (currentAdditionalForm==null) {
            currentAdditionalForm=new AdditionalForm();
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
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.data.hr.LeaveType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.facade.AdditionalFormFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

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

    @EJB
    CommonFunctions commonFunctions;
    List<AdditionalForm> additionalForms;
    Department department;
    Staff staff;
    Staff approvedStaff;
    Date fromDate;
    Date toDate;

    public staffAdditionalFormController() {
    }

    public void clear() {
        currentAdditionalForm = null;
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
        if (getCurrentAdditionalForm().getId() != null && getCurrentAdditionalForm().getId() > 0) {
            currentAdditionalForm.setEditedAt(new Date());
            currentAdditionalForm.setEditor(getSessionController().getLoggedUser());
            getAdditionalFormFacade().edit(currentAdditionalForm);
            JsfUtil.addSuccessMessage("Updated.");
        } else {
            currentAdditionalForm.setCreatedAt(new Date());
            currentAdditionalForm.setCreater(getSessionController().getLoggedUser());
            getAdditionalFormFacade().create(currentAdditionalForm);
            JsfUtil.addSuccessMessage("Sucessfully Saved");
            clear();
        }
    }
    
    public void deleteAdditionalForm(){
        if (getCurrentAdditionalForm()!=null) {
            currentAdditionalForm.setRetired(true);
            currentAdditionalForm.setRetirer(getSessionController().getLoggedUser());
            currentAdditionalForm.setRetiredAt(new Date());
            getAdditionalFormFacade().edit(currentAdditionalForm);
            JsfUtil.addSuccessMessage("Sucessfuly Deleted.");
            clear();
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete.");
        }
    }

    public void createAmmendmentTable() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AdditionalForm a where "
                + " a.createdAt between :fd and :td ";

        if (department != null) {
            sql += " and a.requestDepartment=:dept ";
            m.put("dept", department);
        }

        if (staff != null) {
            sql += " and a.staff=:st ";
            m.put("st", staff);
        }

        if (approvedStaff != null) {
            sql += " and a.approved=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        additionalForms = getAdditionalFormFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void viewAdditionalForm(AdditionalForm additionalForm) {
        currentAdditionalForm = additionalForm;
    }

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

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Staff getApprovedStaff() {
        return approvedStaff;
    }

    public void setApprovedStaff(Staff approvedStaff) {
        this.approvedStaff = approvedStaff;
    }

    public List<AdditionalForm> getAdditionalForms() {
        return additionalForms;
    }

    public void setAdditionalForms(List<AdditionalForm> additionalForms) {
        this.additionalForms = additionalForms;
    }

}

/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;

import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.TransferForm;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.TransferFormFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Named
@SessionScoped
public class StaffTransferFormController implements Serializable {

    private TransferForm currentTransferForm;
    @EJB
    private TransferFormFacade transferFormFacade;
    @Inject
    private SessionController sessionController;
    Date date;
    List<StaffShift> staffShifts;
    @EJB
    StaffShiftFacade staffShiftFacade;

    CommonFunctions commonFunctions;
    List<TransferForm> transferForms;
    Department department;
    Staff staff;
    Staff approvedStaff;
    Date fromDate;
    Date toDate;

//    public void deleteTransferForm() {
//        if (getCurrentTransferForm() != null) {
//            currentTransferForm.setRetired(true);
//            currentTransferForm.setRetirer(getSessionController().getLoggedUser());
//            currentTransferForm.setRetiredAt(new Date());
//            getTransferFormFacade().edit(currentTransferForm);
//            JsfUtil.addSuccessMessage("Sucessfuly Deleted.");
//            clear();
//        } else {
//            JsfUtil.addErrorMessage("Nothing to Delete.");
//        }
//    }
    public void createTransferTable() {
        String sql;
        Map m = new HashMap();

        sql = " select a from TransferForm a where "
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
            sql += " and a.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        transferForms = getTransferFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void viewTransferForm(TransferForm transferForm) {
        currentTransferForm = transferForm;
    }

    public List<StaffShift> getStaffShifts() {
        return staffShifts;
    }

    public void setStaffShifts(List<StaffShift> staffShifts) {
        this.staffShifts = staffShifts;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public StaffTransferFormController() {
    }

    public void clear() {
        currentTransferForm = null;
    }

    public boolean errorCheck() {
        if (currentTransferForm.getStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }

        if (currentTransferForm.getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Please Enter Form Number");
            return true;
        }

        if (currentTransferForm.getComments() == null || "".equals(currentTransferForm.getComments())) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return true;
        }

        if (currentTransferForm.getToRoster() == null) {
            JsfUtil.addErrorMessage("Please Select Changing Roster");
            return true;
        }

        return false;
    }

    public void saveTransferForm() {
        if (errorCheck()) {
            return;
        }
        currentTransferForm.setCreatedAt(new Date());
        currentTransferForm.setCreater(getSessionController().getLoggedUser());

        if (currentTransferForm.getId() == null) {
            getTransferFormFacade().create(currentTransferForm);
        } else {
            getTransferFormFacade().edit(currentTransferForm);
        }

        currentTransferForm.getStaff().setRoster(currentTransferForm.getToRoster());
        staffFacade.edit(currentTransferForm.getStaff());

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

    @EJB
    StaffFacade staffFacade;

    public TransferForm getCurrentTransferForm() {
        if (currentTransferForm == null) {
            currentTransferForm = new TransferForm();
        }
        return currentTransferForm;
    }

    public void setCurrentTransferForm(TransferForm currentTransferForm) {
        this.currentTransferForm = currentTransferForm;
    }

    public TransferFormFacade getTransferFormFacade() {
        return transferFormFacade;
    }

    public void setTransferFormFacade(TransferFormFacade transferFormFacade) {
        this.transferFormFacade = transferFormFacade;
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

    public List<TransferForm> getTransferForms() {
        return transferForms;
    }

    public void setTransferForms(List<TransferForm> transferForms) {
        this.transferForms = transferForms;
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

    public Staff getApprovedStaff() {
        return approvedStaff;
    }

    public void setApprovedStaff(Staff approvedStaff) {
        this.approvedStaff = approvedStaff;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.java.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

}

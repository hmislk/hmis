/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;


import com.divudi.entity.Staff;
import com.divudi.entity.hr.AmendmentForm;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.AmendmentFormFacade;
import com.divudi.facade.ShiftFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.StaffShiftHistoryFacade;
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
@Named(value = "staffAmendmentFormController")
@SessionScoped
public class StaffAmendmentFormController implements Serializable {

    AmendmentForm currAmendmentForm;
    @EJB
    AmendmentFormFacade amendmentFormFacade;
    @Inject
    SessionController sessionController;


    CommonFunctions commonFunctions;
    List<AmendmentForm> amendmentForms;
    Staff fromStaff;
    Staff toStaff;
    Staff approvedStaff;
    Date fromDate;
    Date toDate;

    public StaffAmendmentFormController() {
    }

    public void clear() {
        currAmendmentForm = null;
        fromStaffShifts = null;
        toStaffShifts = null;
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

        if (currAmendmentForm.getFromStaffShift() == null) {
            JsfUtil.addErrorMessage("Please Select FromStaffShift");
            return true;
        }

        if (currAmendmentForm.getToShift() == null) {
            JsfUtil.addErrorMessage("Please Select  ToShift");
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

    public StaffShift fetchStaffShift(Date date, Shift shift, Staff staff) {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shift=:sh"
                + " and c.shiftDate=:dt "
                + " and c.staff=:stf ";

        hm.put("dt", date);
        hm.put("stf", staff);
        hm.put("sh", shift);

        return staffShiftFacade.findFirstByJpql(sql, hm, TemporalType.DATE);
    }

    private StaffShift createToStaffShift(Shift shift) {
        StaffShift toStaffShift = new StaffShift();
        toStaffShift.setStaff(toStaff);
        toStaffShift.setShift(shift);
        toStaffShift.setShiftDate(getCurrAmendmentForm().getToDate());
        toStaffShift.setCreatedAt(new Date());
        toStaffShift.setCreater(sessionController.getLoggedUser());
        staffShiftFacade.create(toStaffShift);

        return toStaffShift;

    }

    public void saveAmendmentForm() {
        if (errorCheck()) {
            return;
        }
        if (currAmendmentForm.getId() == null) {
            currAmendmentForm.setCreatedAt(new Date());
            currAmendmentForm.setCreater(getSessionController().getLoggedUser());
            getAmendmentFormFacade().create(currAmendmentForm);
        } else {
            getAmendmentFormFacade().edit(currAmendmentForm);
        }

        //Change Shifts
        StaffShift fromStaffShift1st = getCurrAmendmentForm().getFromStaffShift();
        StaffShift fromStaffShift2nd = getCurrAmendmentForm().getFromStaffShiftSecond();
        StaffShift toStaffShift1st = fetchStaffShift(getCurrAmendmentForm().getToDate(), getCurrAmendmentForm().getToShift(), getCurrAmendmentForm().getToStaff());
        StaffShift toStaffShift2nd = fetchStaffShift(getCurrAmendmentForm().getToDate(), getCurrAmendmentForm().getToShiftSecond(), getCurrAmendmentForm().getToStaff());

        //Resetting Data
        if(fromStaffShift1st!=null)fromStaffShift1st.reset();
        if(fromStaffShift2nd!=null)fromStaffShift2nd.reset();
        if(toStaffShift1st!=null)toStaffShift1st.reset();
        if(toStaffShift2nd!=null)toStaffShift2nd.reset();
        
        
        //Create New Staff Shift if ToSatffShift is null
        if (getCurrAmendmentForm().getToShift() != null && toStaffShift1st == null) {
            toStaffShift1st = createToStaffShift(getCurrAmendmentForm().getToShift());
            if (fromStaffShift1st != null) {
                fromStaffShift1st.setTransChecked(true);
            }
        }

        //Create New Staff Shift if toShiftSecond not null and ToSatffShiftSecond Shift is null
        if (getCurrAmendmentForm().getToShiftSecond() != null && toStaffShift2nd == null) {
            toStaffShift2nd = createToStaffShift(getCurrAmendmentForm().getToShiftSecond());
            if (fromStaffShift2nd != null) {
                fromStaffShift2nd.setTransChecked(true);
            }
        }

        Shift fromShift1 = fromStaffShift1st != null ? fromStaffShift1st.getShift() : null;
        Shift fromShift2 = fromStaffShift2nd != null ? fromStaffShift2nd.getShift() : null;
        Shift toShift1 = toStaffShift1st != null ? toStaffShift1st.getShift() : null;;
        Shift toShift2 = toStaffShift2nd != null ? toStaffShift2nd.getShift() : null;;

        if (fromStaffShift1st != null && toStaffShift1st != null) {
            fromStaffShift1st.setShift(toShift1);
            toStaffShift1st.setShift(fromShift1);
            toStaffShift1st.setAmendmentForm(currAmendmentForm);
            fromStaffShift1st.setAmendmentForm(currAmendmentForm);
            staffShiftFacade.edit(toStaffShift1st);
            staffShiftFacade.edit(fromStaffShift1st);
        }

        if (fromStaffShift2nd != null && toStaffShift2nd == null) {
            fromStaffShift2nd.setShift(null);
            fromStaffShift2nd.setAmendmentForm(currAmendmentForm);
            staffShiftFacade.edit(fromStaffShift2nd);
        } else if (fromStaffShift2nd == null && toStaffShift2nd != null) {
            toStaffShift2nd.setShift(null);
            toStaffShift2nd.setAmendmentForm(currAmendmentForm);
            staffShiftFacade.edit(toStaffShift2nd);
        } else if (fromStaffShift2nd != null && toStaffShift2nd != null) {
            fromStaffShift2nd.setShift(toShift2);
            toStaffShift2nd.setShift(fromShift2);
            toStaffShift2nd.setAmendmentForm(currAmendmentForm);
            fromStaffShift2nd.setAmendmentForm(currAmendmentForm);
            staffShiftFacade.edit(toStaffShift2nd);
            staffShiftFacade.edit(fromStaffShift2nd);
        }

        if (fromStaffShift1st != null && fromStaffShift1st.isTransChecked()) {
            fromStaffShift1st.setShift(null);
            fromStaffShift1st.setAmendmentForm(currAmendmentForm);
            staffShiftFacade.edit(fromStaffShift1st);

        }

        if (fromStaffShift2nd != null && fromStaffShift2nd.isTransChecked()) {
            fromStaffShift2nd.setShift(null);
            fromStaffShift2nd.setAmendmentForm(currAmendmentForm);
            staffShiftFacade.edit(fromStaffShift2nd);

        }

        ///////////////////////Finish Amendment
        getCurrAmendmentForm().setToStaffShift(toStaffShift1st);
        getCurrAmendmentForm().setToStaffShiftSecond(toStaffShift2nd);
        amendmentFormFacade.edit(currAmendmentForm);

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

//    
//           StaffShiftHistory staffShiftHistory = new StaffShiftHistory();
//        staffShiftHistory.setCreatedAt(new Date());
//        staffShiftHistory.setCreater(sessionController.getLoggedUser());
//        staffShiftHistory.setStaff(fromStaff);
//        staffShiftHistory.setStaffShift(fromStaffShift1st);
//        staffShiftHistoryFacade.create(staffShiftHistory);
//
//        staffShiftHistory = new StaffShiftHistory();
//        staffShiftHistory.setCreatedAt(new Date());
//        staffShiftHistory.setCreater(sessionController.getLoggedUser());
//        staffShiftHistory.setStaff(toStaff);
//        staffShiftHistory.setStaffShift(toStaffShift1st);
//        staffShiftHistoryFacade.create(staffShiftHistory);
    @EJB
    StaffShiftHistoryFacade staffShiftHistoryFacade;

    List<StaffShift> fromStaffShifts;
    List<StaffShift> toStaffShifts;
    @EJB
    StaffShiftFacade staffShiftFacade;

    public void createAmmendmentTable() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AmendmentForm a where "
                + " a.createdAt between :fd and :td ";

        if (fromStaff != null) {
            sql += " and a.fromStaff=:fst ";
            m.put("fst", fromStaff);
        }

        if (toStaff != null) {
            sql += " and a.toStaff=:tst ";
            m.put("tst", toStaff);
        }

        if (approvedStaff != null) {
            sql += " and a.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        amendmentForms = getAmendmentFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createAmmendmentTableApprovedDate() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AmendmentForm a where "
                + " a.approvedAt between :fd and :td ";

        if (fromStaff != null) {
            sql += " and a.fromStaff=:fst ";
            m.put("st", fromStaff);
        }

        if (toStaff != null) {
            sql += " and a.toStaff=:tst ";
            m.put("st", toStaff);
        }

        if (approvedStaff != null) {
            sql += " and a.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        amendmentForms = getAmendmentFormFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void viewAmendmentForm(AmendmentForm amendmentForm) {
        currAmendmentForm = amendmentForm;
        fetchToStaffShift();
        fetchFromStaffShift();
    }

    public void deleteAmmendmentForm() {
        if (currAmendmentForm == null) {
            return;
        }
        currAmendmentForm.setRetired(true);
        currAmendmentForm.setRetiredAt(new Date());
        currAmendmentForm.setRetirer(getSessionController().getLoggedUser());
        getAmendmentFormFacade().edit(currAmendmentForm);
        JsfUtil.addSuccessMessage("Deleted");
        clear();
    }

    public void fetchFromStaffShift() {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shift is not null "
                + " and c.shiftDate=:dt "
                + " and c.staff=:stf ";

        hm.put("dt", getCurrAmendmentForm().getFromDate());
        hm.put("stf", getCurrAmendmentForm().getFromStaff());

        fromStaffShifts = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
    }

    public void fetchToStaffShift() {
        HashMap hm = new HashMap();
        String sql = " select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shiftDate=:dt "
                + " and c.shift is not null "
                + " and c.staff=:stf ";

        hm.put("dt", getCurrAmendmentForm().getToDate());
        hm.put("stf", getCurrAmendmentForm().getToStaff());

        toStaffShifts = staffShiftFacade.findByJpql(sql, hm, TemporalType.DATE);
    }

    public void fetchToShift() {
        if (getCurrAmendmentForm().getToStaff() == null) {
            return;
        }

        HashMap hm = new HashMap();
        String sql = " select c from "
                + " Shift c"
                + " where c.retired=false "
                + " and c.roster=:rs ";

        hm.put("rs", getCurrAmendmentForm().getToStaff().getRoster());

        toShifts = shiftFacade.findByJpql(sql, hm, TemporalType.DATE);
    }

    @EJB
    ShiftFacade shiftFacade;
    List<Shift> toShifts;

    public List<Shift> getToShifts() {
        return toShifts;
    }

    public void setToShifts(List<Shift> toShifts) {
        this.toShifts = toShifts;
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

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<AmendmentForm> getAmendmentForms() {
        return amendmentForms;
    }

    public void setAmendmentForms(List<AmendmentForm> amendmentForms) {
        this.amendmentForms = amendmentForms;
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

    public StaffShiftHistoryFacade getStaffShiftHistoryFacade() {
        return staffShiftHistoryFacade;
    }

    public void setStaffShiftHistoryFacade(StaffShiftHistoryFacade staffShiftHistoryFacade) {
        this.staffShiftHistoryFacade = staffShiftHistoryFacade;
    }

    public Staff getFromStaff() {

        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {

        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

}

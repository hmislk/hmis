/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.Roster;
import com.divudi.facade.RosterFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class RosterStaffController implements Serializable {

    private Roster currentRoster;
    private Staff currentStaff;
    List<Staff> staffList;
    @EJB
    private RosterFacade rosterFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    HumanResourceBean humanResourceBean;

    public void createStaff() {
        staffList = humanResourceBean.fetchStaff(getCurrentRoster());
    }

    /**
     * Creates a new instance of RosterStaffController
     */
    /**
     * Creates a new instance of RosterStaffController
     */
    public void add() {

        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Select Staff");
            return;
        }

        if (currentStaff.getRoster() != null) {
            JsfUtil.addErrorMessage("This staff already in other roster");
            return;
        }

        getCurrentStaff().setRoster(getCurrentRoster());
        getStaffFacade().edit(getCurrentStaff());
        currentStaff = null;
        createStaff();
    }

    public void remove() {
        //    getCurrentRoster().getStaffList().remove(getCurrentStaff());
        getCurrentStaff().setRoster(null);
        getStaffFacade().edit(getCurrentStaff());

        currentStaff = null;
        createStaff();
    }

    public RosterStaffController() {
    }

    public Roster getCurrentRoster() {
        return currentRoster;
    }

    public void setCurrentRoster(Roster currentRoster) {
        this.currentRoster = currentRoster;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public RosterFacade getRosterFacade() {
        return rosterFacade;
    }

    public void setRosterFacade(RosterFacade rosterFacade) {
        this.rosterFacade = rosterFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;

    }

    public List<Staff> getStaffList() {
        return staffList;
    }

    public void setStaffList(List<Staff> staffList) {
        this.staffList = staffList;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.UtilityController;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.Roster;
import com.divudi.facade.RosterFacade;
import com.divudi.facade.StaffFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;

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
            UtilityController.addErrorMessage("Select Staff");
            return;
        }

        if (currentStaff.getRoster() != null) {
            UtilityController.addErrorMessage("This staff already in other roster");
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

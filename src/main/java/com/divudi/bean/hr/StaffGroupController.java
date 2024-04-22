/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Staff;
import com.divudi.facade.RosterFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StaffGroupController implements Serializable {
   
    private Staff currentStaff;   
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private RosterFacade rosterFacade;
    @Inject
    private SessionController sessionController;

    private boolean checkGroup() {
        String sql = "Select s From Staff s where s.retired=false and s.staffGroup is not null "
                + "and s.staffGroup.roster.retired=false and s.id=" + getCurrentStaff().getId();
        Staff s = getStaffFacade().findFirstByJpql(sql);

        return s != null;
    }

   
    public void add() {
        if (checkGroup()) {
            JsfUtil.addErrorMessage("This Staff allready in a Group");
            currentStaff = null;
            return;
        }

      //  getCurrent().getStaffList().add(getCurrentStaff());

//        if (getCurrent().getId() == null) {
//            getFacade().create(getCurrent());
//        } else {
//            getFacade().edit(getCurrent());
//        }

       // getCurrentStaff().setStaffGroup(getCurrent());
      
       // getStaffFacade().edit(getCurrentStaff());

//        getCurrent().getRoster().getStaffGroupList().add(getCurrent());
//        getRosterFacade().edit(getCurrent().getRoster());

        currentStaff = null;
    }

 
    public StaffGroupController() {
    }

    public void makeNull() {
    
        currentStaff = null;
      

    }

//    public void saveSelected() {
//
//        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
//            getFacade().edit(current);
//            JsfUtil.addSuccessMessage("Updated Successfully.");
//        } else {
//            current.setCreatedAt(new Date());
//            current.setCreater(getSessionController().getLoggedUser());
//            getFacade().create(current);
//            JsfUtil.addSuccessMessage("Saved Successfully");
//        }
//
//        recreateModel();
//        getItems();
//    }
//    private void removeAll() {
//        for (Staff s : getCurrent().getStaffList()) {
//            s.setStaffGroup(null);
//            getStaffFacade().edit(s);
//        }
//    }

//    public void delete() {
//
//        if (current != null) {
//            removeAll();
//            current.setRetired(true);
//            current.setRetiredAt(new Date());
//            current.setRetirer(getSessionController().getLoggedUser());
//            getFacade().edit(current);
//            JsfUtil.addSuccessMessage("Deleted Successfully");
//        } else {
//            JsfUtil.addSuccessMessage("Nothing to Delete");
//        }
//        recreateModel();
//        getItems();
//        current = null;
//
//    }

    
  
  
   

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public RosterFacade getRosterFacade() {
        return rosterFacade;
    }

    public void setRosterFacade(RosterFacade rosterFacade) {
        this.rosterFacade = rosterFacade;
    }

  
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.Shift;
import com.divudi.facade.RosterFacade;
import com.divudi.facade.ShiftFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ShiftController implements Serializable {

    private Shift current;
    private Roster currentRoster;
    private List<Shift> shiftList;
    @EJB
    private ShiftFacade facade;
    @EJB
    private RosterFacade rosterFacade;
    @Inject
    private SessionController sessionController;
    boolean checked = false;

    private boolean errorCheck() {
        if (getCurrent().getRoster() == null) {
            UtilityController.addErrorMessage("Select Roster");
            return true;
        }

        if (getCurrent().getName().trim().isEmpty() && getCurrent().getName().equals("")) {
            UtilityController.addErrorMessage("Enter Name");
            return true;
        }

        if (getCurrent().getDayType() == null) {
            UtilityController.addErrorMessage("Select Day Type");
            return true;
        }

//        if (getCurrent().getStartingTime() == null) {
//            UtilityController.addErrorMessage("Set Start Time");
//            return true;
//        }
//
//        if (getCurrent().getEndingTime() == null) {
//            UtilityController.addErrorMessage("Set End Time");
//            return true;
//        }
//        if (getCurrent().getCount() == 0) {
//            UtilityController.addErrorMessage("Set Staff count correctly");
//            return true;
//        }
//        if(getCurrentDayShift().getRepeatedDay()!=0 && getCurrentDayShift().isDayOff()){
//            UtilityController.addErrorMessage("Repeated day & dayoff can't active at Same  time");
//            return true;
//        }
//        if (checkTimeLimit()) {
//            UtilityController.addErrorMessage("You Cant add more than 24h per Roster");
//            return true;
//        }
        return false;
    }

    List<Shift> shifts;

    public List<Shift> completeShift(String qry) {

        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from Shift c "
                + " where c.retired=false "
                + " and upper(c.name) like :q "
                + " and (c.hideShift=false or c.hideShift is null) ";

        if (getCurrentRoster() != null) {
            sql += " and c.roster=:rs";
            hm.put("rs", getCurrentRoster());
        }

        sql += " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        shifts = getFacade().findBySQL(sql, hm);

        return shifts;
    }

    public List<Shift> completeShiftAll(String qry) {

        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from Shift c "
                + " where c.retired=false "
                + " and upper(c.name) like :q ";

        if (getCurrentRoster() != null) {
            sql += " and c.roster=:rs";
            hm.put("rs", getCurrentRoster());
        }

        sql += " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        shifts = getFacade().findBySQL(sql, hm);

        return shifts;
    }

    public void saveSelected() {

        if (errorCheck()) {
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }

        //     recreateModel();
        createShiftList();
        current = null;
    }

    public ShiftController() {
    }

    public void prepareAdd() {
        current = null;
    }

    private void recreateModel() {
        currentRoster = null;
    }

    public void delete() {

        if (current != null) {
            // removeAll();
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());

            getFacade().edit(current);

//            getFacade().remove(current);
//            getCurrentRoster().getShiftList().remove(getCurrent());
            getRosterFacade().edit(getCurrentRoster());
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        //   recreateModel();

        current = null;

    }

    public Shift getCurrent() {
        if (current == null) {
            current = new Shift();
            current.setRoster(getCurrentRoster());
        }
        return current;
    }

    public void setCurrent(Shift current) {
        this.current = current;

    }

    public ShiftFacade getFacade() {
        return facade;
    }

    public void setFacade(ShiftFacade facade) {
        this.facade = facade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Roster getCurrentRoster() {
        return currentRoster;
    }

    public void setCurrentRoster(Roster currentRoster) {
        current = null;
        this.currentRoster = currentRoster;
    }

    public RosterFacade getRosterFacade() {
        return rosterFacade;
    }

    public void setRosterFacade(RosterFacade rosterFacade) {
        this.rosterFacade = rosterFacade;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void createShiftList() {
        String sql = "Select s From Shift s "
                + " where s.retired=false "
                + " and s.roster=:rs ";
        //   + " order by s.shiftOrder ";
        //System.out.println("sql = " + sql);
        HashMap hm = new HashMap();
        hm.put("rs", getCurrentRoster());

        shiftList = getFacade().findBySQL(sql, hm);
    }

    public void createShiftListReport() {
        String sql = "Select s From Shift s "
                + " where s.retired=false ";

        if (checked) {
            sql += " and s.hideShift=false ";
        }

        sql += " order by s.roster.name ";

        shiftList = getFacade().findBySQL(sql);
    }

    public List<Shift> getShiftList() {
        return shiftList;
    }

    public void setShiftList(List<Shift> shiftList) {
        this.shiftList = shiftList;
    }

    @FacesConverter(forClass = Shift.class)
    public static class ShiftConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ShiftController controller = (ShiftController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "shiftController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            try {
                key = Long.valueOf(value);
            } catch (NumberFormatException exception) {
                key = 0l;
            }
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Shift) {
                Shift o = (Shift) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ShiftController.class.getName());
            }
        }
    }

    @FacesConverter("shift")
    public static class ShiftControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ShiftController controller = (ShiftController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "shiftController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Shift) {
                Shift o = (Shift) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ShiftController.class.getName());
            }
        }
    }

}

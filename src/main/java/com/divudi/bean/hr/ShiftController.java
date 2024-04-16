/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.Shift;
import com.divudi.facade.RosterFacade;
import com.divudi.facade.ShiftFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

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
    @Inject
    CommonController commonController;
    boolean checked = false;

    private boolean errorCheck() {
        if (getCurrent().getRoster() == null) {
            JsfUtil.addErrorMessage("Select Roster");
            return true;
        }

        if (getCurrent().getName().trim().isEmpty() && getCurrent().getName().equals("")) {
            JsfUtil.addErrorMessage("Enter Name");
            return true;
        }

        if (getCurrent().getDayType() == null) {
            JsfUtil.addErrorMessage("Select Day Type");
            return true;
        }

//        if (getCurrent().getStartingTime() == null) {
//            JsfUtil.addErrorMessage("Set Start Time");
//            return true;
//        }
//
//        if (getCurrent().getEndingTime() == null) {
//            JsfUtil.addErrorMessage("Set End Time");
//            return true;
//        }
//        if (getCurrent().getCount() == 0) {
//            JsfUtil.addErrorMessage("Set Staff count correctly");
//            return true;
//        }
//        if(getCurrentDayShift().getRepeatedDay()!=0 && getCurrentDayShift().isDayOff()){
//            JsfUtil.addErrorMessage("Repeated day & dayoff can't active at Same  time");
//            return true;
//        }
//        if (checkTimeLimit()) {
//            JsfUtil.addErrorMessage("You Cant add more than 24h per Roster");
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
                + " and (c.name) like :q "
                + " and (c.hideShift=false or c.hideShift is null) ";

        if (getCurrentRoster() != null) {
            sql += " and c.roster=:rs";
            hm.put("rs", getCurrentRoster());
        }

        sql += " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        shifts = getFacade().findByJpql(sql, hm);

        return shifts;
    }

    public List<Shift> completeShiftAll(String qry) {

        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from Shift c "
                + " where c.retired=false "
                + " and (c.name) like :q ";

        if (getCurrentRoster() != null) {
            sql += " and c.roster=:rs";
            hm.put("rs", getCurrentRoster());
        }

        sql += " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        shifts = getFacade().findByJpql(sql, hm);

        return shifts;
    }

    public void saveSelected() {
        if (errorCheck()) {
            return;
        }
        if (getCurrent().getId() != null) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
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
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Seleced");
            return;
        } else {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            getCurrentRoster().getShiftList().remove(getCurrent());
            getRosterFacade().edit(getCurrentRoster());
            JsfUtil.addSuccessMessage("Deleted Successfully");
        }
        createShiftList();
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
        String jpql = "Select s "
                + " From Shift s "
                + " where s.retired=false "
                + " and s.roster=:rs ";
        HashMap m = new HashMap();
        m.put("rs", getCurrentRoster());
        shiftList = getFacade().findByJpql(jpql, m);
        JsfUtil.addSuccessMessage("Listed");
    }

    public void createShiftListReport() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        String sql = "Select s From Shift s "
                + " where s.retired=false ";

        if (checked) {
            sql += " and s.hideShift=false ";
        }

        sql += " order by s.roster.name ";

        shiftList = getFacade().findByJpql(sql);

        
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}

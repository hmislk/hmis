/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.Roster;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffSalaryComponant;
import com.divudi.facade.PaysheetComponentFacade;
import com.divudi.facade.RosterFacade;
import com.divudi.facade.SalaryCycleFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.facade.StaffSalaryComponantFacade;
import com.divudi.facade.StaffSalaryFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class SalaryCycleController implements Serializable {

    private SalaryCycle current;
    private Roster currentRoster;
    private List<SalaryCycle> salaryCycleList;
    @EJB
    private SalaryCycleFacade facade;
    @EJB
    private RosterFacade rosterFacade;
    @Inject
    private SessionController sessionController;

 
    List<SalaryCycle> salaryCycles;

    public List<SalaryCycle> completeSalaryCycle(String qry) {

        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from SalaryCycle c "
                + " where c.retired=false "
                + " and upper(c.name) like :q "
                + " and (c.hideSalaryCycle=false or c.hideSalaryCycle is null) ";

        if (getCurrentRoster() != null) {
            sql += " and c.roster=:rs";
            hm.put("rs", getCurrentRoster());
        }

        sql += " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        salaryCycles = getFacade().findBySQL(sql, hm);

        return salaryCycles;
    }

    public List<SalaryCycle> completeSalaryCycleAll(String qry) {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select c from SalaryCycle c "
                + " where c.retired=false "
                + " and upper(c.name) like :q ";
        if (getCurrentRoster() != null) {
            sql += " and c.roster=:rs";
            hm.put("rs", getCurrentRoster());
        }
        sql += " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        salaryCycles = getFacade().findBySQL(sql, hm);
        return salaryCycles;
    }

    public void listAllSalaryCycles(){
        String sql ;
        HashMap hm = new HashMap();
        sql = "select c from SalaryCycle c "
                + " where c.retired=false "
                + " order by c.id desc";
        salaryCycles = getFacade().findBySQL(sql);
    }

   
    @EJB
    StaffSalaryFacade staffSalaryFacade;
    
    @EJB
    PaysheetComponentFacade paysheetComponentFacade;
    
    @EJB
    StaffPaysheetComponentFacade staffPaysheetComponentFacade;
    
    
    
    public String hr_report_all_staff_salary(){
        listAllSalaryCycles();
        return "/hr/hr_report_all_staff_salary";
    }
    
    
    public List<SalaryCycle> getSalaryCycles() {
        return salaryCycles;
    }

    public void setSalaryCycles(List<SalaryCycle> salaryCycles) {
        this.salaryCycles = salaryCycles;
    }
    
    
    
    
    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }

        //     recreateModel();
        createSalaryCycleList();
        current = null;
    }

    public SalaryCycleController() {
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
//            getCurrentRoster().getSalaryCycleList().remove(getCurrent());
            getRosterFacade().edit(getCurrentRoster());
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        //   recreateModel();

        current = null;

    }

    public SalaryCycle getCurrent() {
        if (current == null) {
            current = new SalaryCycle();
        }
        return current;
    }

    public void setCurrent(SalaryCycle current) {
        this.current = current;

    }

    public SalaryCycleFacade getFacade() {
        return facade;
    }

    public void setFacade(SalaryCycleFacade facade) {
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

    public void createSalaryCycleList() {
        String sql = "Select s From SalaryCycle s "
                + " where s.retired=false "
                + " and s.roster=:rs ";
        //   + " order by s.salaryCycleOrder ";
        System.out.println("sql = " + sql);
        HashMap hm = new HashMap();
        hm.put("rs", getCurrentRoster());

        salaryCycleList = getFacade().findBySQL(sql, hm);
    }
    
    public void createSalaryCycleListReport() {
        String sql = "Select s From SalaryCycle s "
                + " where s.retired=false "
                + " order by s.roster.name " ;
                

        salaryCycleList = getFacade().findBySQL(sql);
    }

    public List<SalaryCycle> getSalaryCycleList() {
        return salaryCycleList;
    }

    public void setSalaryCycleList(List<SalaryCycle> salaryCycleList) {
        this.salaryCycleList = salaryCycleList;
    }
    
    List<StaffAndSalarySalaryComponent> staffAnsAndSalarySalaryComponents;

    public List<StaffAndSalarySalaryComponent> getStaffAnsAndSalarySalaryComponents() {
        return staffAnsAndSalarySalaryComponents;
    }

    public void setStaffAnsAndSalarySalaryComponents(List<StaffAndSalarySalaryComponent> staffAnsAndSalarySalaryComponents) {
        this.staffAnsAndSalarySalaryComponents = staffAnsAndSalarySalaryComponents;
    }
    
    public void fillStaffAndSalaryComponents(){
        List<PaysheetComponent> paysheetComponents;
        List<Staff> staffes;
        String jpql;
        StaffPaysheetComponent pc;
        Map m;
        
        m= new HashMap();
        jpql="select distinct spc.paysheetComponent from StaffSalaryComponant spc where ";
        m.put("sc", current);
        paysheetComponents = staffPaysheetComponentFacade.findBySQL(jpql, m);
        
        m = new HashMap();
        jpql="select distinct spc.paysheetComponent from StaffSalaryComponant spc where ";
        m.put("sc", current);
        staffes = staffPaysheetComponentFacade.findBySQL(jpql, m);
        
        staffAnsAndSalarySalaryComponents = new ArrayList<>();
        
        for (Staff s:staffes){
            StaffAndSalarySalaryComponent sc = new StaffAndSalarySalaryComponent();
            sc.setStaffSalaryComponants(new ArrayList<StaffSalaryComponant>());
            for(PaysheetComponent psc:paysheetComponents){
                jpql = "select spc from StaffSalaryComponant spc where spc.staff=:st and spc.paysheetComponent=:pc";
                m = new HashMap();
                m.put("st", s);
                m.put("pc", psc);
                StaffSalaryComponant c = staffSalaryComponantFacade.findFirstBySQL(jpql, m);
                sc.getStaffSalaryComponants().add(c);
            }
            staffAnsAndSalarySalaryComponents.add(sc);
        }
    }
    
    @EJB
    StaffSalaryComponantFacade staffSalaryComponantFacade;

    public class StaffAndSalarySalaryComponent {
        Staff staff;
        List<StaffSalaryComponant> staffSalaryComponants;

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public List<StaffSalaryComponant> getStaffSalaryComponants() {
            return staffSalaryComponants;
        }

        public void setStaffSalaryComponants(List<StaffSalaryComponant> staffSalaryComponants) {
            this.staffSalaryComponants = staffSalaryComponants;
        }


        
        
    }
    
    @FacesConverter(forClass = SalaryCycle.class)
    public static class SalaryCycleConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SalaryCycleController controller = (SalaryCycleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "salaryCycleController");
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
            if (object instanceof SalaryCycle) {
                SalaryCycle o = (SalaryCycle) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SalaryCycleController.class.getName());
            }
        }
    }

    @FacesConverter("salaryCycleConverter")
    public static class SalaryCycleControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SalaryCycleController controller = (SalaryCycleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "salaryCycleController");
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
            if (object instanceof SalaryCycle) {
                SalaryCycle o = (SalaryCycle) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SalaryCycleController.class.getName());
            }
        }
    }

}

/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Department;
import com.divudi.entity.lab.DepartmentMachine;
import com.divudi.entity.lab.Machine;
import com.divudi.facade.DepartmentMachineFacade;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *  @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 *  Consultant in Health Informatics
 * 
 */
@Named
@SessionScoped
public class DepartmentMachineController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private DepartmentMachineFacade ejbFacade;
    private DepartmentMachine current;
    private List<DepartmentMachine> items = null;

    public void save(DepartmentMachine depMachine) {
        if (depMachine == null) {
            return;
        }
        if (depMachine.getId() != null) {
            getFacade().edit(depMachine);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            depMachine.setCreatedAt(new Date());
            depMachine.setCreater(sessionController.getLoggedUser());
            getFacade().create(depMachine);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public DepartmentMachineFacade getFacade() {
        return ejbFacade;
    }

    public DepartmentMachineController() {
    }

    public DepartmentMachine getCurrent() {
        if (current == null) {
            current = new DepartmentMachine();
        }
        return current;
    }

    public void setCurrent(DepartmentMachine current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        getItems();
        current = null;
        getCurrent();
    }

    public List<DepartmentMachine> getItems() {
        return items;
    }

    public void fillItems() {
        String j;
        j = "select a "
                + " from DepartmentMachine a "
                + " where a.retired=false "
                + " order by a.name";
        items = getFacade().findByJpql(j);
    }

    public List<DepartmentMachine> fillDepartmentMachines(Department dep) {
        return fillDepartmentMachines(dep, null);
    }

    public List<DepartmentMachine> fillDepartmentMachines(Machine machine) {
        return fillDepartmentMachines(null, machine);
    }

    public List<DepartmentMachine> fillDepartmentMachines(Department dep, Machine machine) {
        String j;
        Map m = new HashMap();
        j = "select a "
                + " from DepartmentMachine a "
                + " where a.retired=false";
        if (dep != null) {
            j += " and a.department=:dep ";
            m.put("dep", dep);
        }
        if (machine != null) {
            j += " and a.machine=:machine ";
            m.put("machine", machine);
        }
        j += " order by a.name";
        items = getFacade().findByJpql(j, m);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = DepartmentMachine.class)
    public static class DepartmentMachineConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DepartmentMachineController controller = (DepartmentMachineController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "categoryItemController");
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
            if (object instanceof DepartmentMachine) {
                DepartmentMachine o = (DepartmentMachine) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DepartmentMachine.class.getName());
            }
        }
    }

}

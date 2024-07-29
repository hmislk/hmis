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
import com.divudi.entity.Institution;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * in Health Informatics
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
    private Institution institution;
    private Department department;
    private Machine machine;

    public void addDepartmentMachine() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        if (current.getName() == null) {
            JsfUtil.addErrorMessage("No Name");
            return;
        }

        if (current.getDepartment() == null) {
            JsfUtil.addErrorMessage("Select Department");
            return;
        }
        if (current.getMachine() == null) {
            JsfUtil.addErrorMessage("Select Machine");
            return;
        }
        save(current);
        current = new DepartmentMachine();
        listDepMachines();
    }

    public void toEdit(DepartmentMachine dm) {
        if (dm == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        current = dm;
    }

    public void prepareToAddNew() {
        current = new DepartmentMachine();
        current.setDepartment(department);
        current.setMachine(machine);

    }
    
    public void clearDeptAndDept() {
       department=null;
       institution=null;
    }

    public void save(DepartmentMachine depMachine) {
        if (depMachine == null) {
            return;
        }
        if (depMachine.getDepartment() == null) {
            JsfUtil.addErrorMessage("No Department");
            return;
        }
        if (depMachine.getMachine() == null) {
            JsfUtil.addErrorMessage("No Machine");
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

    public void deleteDeptMachine(DepartmentMachine dm) {
        if (dm == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        current = dm;
        delete();
        listDepMachines();
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

    public void listDepMachines() {
        items = fillDepartmentMachines(department, machine);
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    /**
     *
     */
    @FacesConverter(forClass = DepartmentMachine.class)
    public static class DepartmentMachineConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            DepartmentMachineController controller = (DepartmentMachineController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "departmentMachineController");

            Long key = getKey(value);
            if (key == null) {
                return null;  // Avoid calling find with null
            }
            return controller.getFacade().find(key);
        }

        java.lang.Long getKey(String value) {
            try {
                return Long.valueOf(value.trim());
            } catch (NumberFormatException e) {
                return null;  // Log this if needed
            }
        }

        String getStringKey(java.lang.Long value) {
            if (value == null) {
                return null;
            }
            return value.toString();
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

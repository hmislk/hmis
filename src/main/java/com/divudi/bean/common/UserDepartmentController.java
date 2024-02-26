/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 Genealogical, Clinical, Storeoratory and Genetic Data
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserDepartment;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.WebUserDepartmentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class UserDepartmentController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private WebUserDepartmentFacade ejbFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    List<WebUserDepartment> selectedItems;
    private WebUserDepartment current;
    private WebUser selectedUser;
    private Institution currentInstituion;
    Department currentDepartment;
    private List<Department> lstDep;
    private List<Department> currentInsDepartments;
    private List<Department> selectedUserDeparment;
    private List<Institution> selectedInstitutions;
    private List<WebUserDepartment> items = null;
    String selectText = "";

    public Department getCurrentDepartment() {
        return currentDepartment;
    }

    public void setCurrentDepartment(Department currentDepartment) {
        this.currentDepartment = currentDepartment;
    }

    public void prepareAdd() {
        current = new WebUserDepartment();
    }

    // Need new Enum WebUserDepartment type
    public void setSelectedItems(List<WebUserDepartment> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getEjbFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getEjbFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public UserDepartmentController() {
    }

    public WebUserDepartment getCurrent() {
        if (current == null) {
            current = new WebUserDepartment();
        }
        return current;
    }

    public void setCurrent(WebUserDepartment current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getEjbFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public void addDepartmentForUser() {
        if (selectedUser == null) {
            JsfUtil.addSuccessMessage("Select A User");
            return;
        }
        if (currentDepartment == null) {
            JsfUtil.addSuccessMessage("Select a Department");
            return;
        }
        WebUserDepartment d = new WebUserDepartment();
        d.setCreatedAt(Calendar.getInstance().getTime());
        ///other properties
        d.setDepartment(currentDepartment);
        d.setWebUser(selectedUser);
        getEjbFacade().create(d);
        items=null;
        currentDepartment = null;
        
    }

    public List<WebUserDepartment> fillWebUserDepartments(WebUser wu) {
        List<WebUserDepartment> tis = new ArrayList<>();
        String sql = "SELECT i "
                + " FROM WebUserDepartment i "
                + " where i.retired=:ret "
                + " and i.webUser=:wu "
                + "  order by i.department.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("wu", wu);
        tis = getEjbFacade().findByJpql(sql, m);
        return tis;
    }

    public List<WebUserDepartment> getItems() {
        if (selectedUser == null) {
            items = new ArrayList<>();
            return items;
        }
        if (items == null) {
            items = fillWebUserDepartments(selectedUser);
        }
        return items;
    }
    
    

    public WebUserDepartmentFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebUserDepartmentFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public WebUser getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(WebUser selectedUser) {
        //////// // System.out.println("Setting user");
        this.selectedUser = selectedUser;
    }

    public List<Department> getSelectedUserDeparment() {
        if (getSelectedUser() == null) {
            return new ArrayList<Department>();
        }

        String sql = "SELECT i.department FROM WebUserDepartment i where i.retired=false and i.webUser=" + getSelectedUser() + "order by i.name";
        selectedUserDeparment = getDepartmentFacade().findByJpql(sql);

        if (selectedUserDeparment == null) {
            selectedUserDeparment = new ArrayList<Department>();
        }

        return selectedUserDeparment;
    }

    public void setSelectedUserDeparment(List<Department> selectedUserDeparment) {
        this.selectedUserDeparment = selectedUserDeparment;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Institution> getSelectedInstitutions() {
        if (getSelectedUser() == null) {
            return new ArrayList<Institution>();
        }

        String sql = "SELECT i.institution FROM WebUserDepartment i where i.retired=false and i.webUser=" + getSelectedUser() + "order by i.name";
        selectedInstitutions = getInstitutionFacade().findByJpql(sql);

        if (selectedInstitutions == null) {
            selectedInstitutions = new ArrayList<Institution>();
        }

        return selectedInstitutions;
    }

    public void setSelectedInstitutions(List<Institution> selectedInstitutions) {
        this.selectedInstitutions = selectedInstitutions;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public Institution getCurrentInstituion() {
        return currentInstituion;
    }

    public void setCurrentInstituion(Institution currentInstituion) {
        this.currentInstituion = currentInstituion;
//        getCurrentInsDepartments();
    }

//    public List<Department> getCurrentInsDepartments() {
//        if (currentInstituion == null) {
//            //////// // System.out.println("1");
//            return new ArrayList<>();
//        }
//        //////// // System.out.println("2");
//        Map m = new HashMap();
//        m.put("ins", currentInstituion);
//        String sql = "SELECT i FROM Department i where i.retired=false and i.institution=:ins order by i.name";
//        currentInsDepartments = getDepartmentFacade().findByJpql(sql,m);
//        //////// // System.out.println("3");
//        if (currentInsDepartments == null) {
//            //////// // System.out.println("4");
//            currentInsDepartments = new ArrayList<>();
//        }
//        return currentInsDepartments;
//    }
    public void setCurrentInsDepartments(List<Department> currentInsDepartments) {
        this.currentInsDepartments = currentInsDepartments;
    }

    public List<Department> getLstDep() {
        return lstDep;
    }

    public void setLstDep(List<Department> lstDep) {
        this.lstDep = lstDep;
    }

    public void setItems(List<WebUserDepartment> items) {
        this.items = items;
    }

    /**
     *
     */
    @FacesConverter(forClass = WebUserDepartment.class)
    public static class DepartmentControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserDepartmentController controller = (UserDepartmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userDepartmentController");
            return controller.getEjbFacade().find(getKey(value));
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
            if (object instanceof WebUserDepartment) {
                WebUserDepartment o = (WebUserDepartment) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserDepartmentController.class.getName());
            }
        }
    }
}

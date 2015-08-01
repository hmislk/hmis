/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.data.DepartmentType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.facade.DepartmentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class DepartmentController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private DepartmentFacade ejbFacade;
    List<Department> selectedItems;
    private Department current;
    private List<Department> items = null;
    private List<Department> searchItems = null;
    String selectText = "";
    private Boolean codeDisabled = false;
    private Institution institution;

    List<Department> itemsToRemove;
    
    public List<Department> getSearchItems() {
        return searchItems;
    }
    
    
    public void removeSelectedItems() {
        for (Department s : itemsToRemove) {
            s.setRetired(true);
            s.setRetireComments("Bulk Remove");
            s.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(s);
        }
        itemsToRemove = null;
        items = null;
    }

    public void fillSearchItems() {
        if (selectText == null || selectText.trim().equals("")) {
            searchItems = getFacade().findAll(true);
        } else {
            String sql = "Select d from Department d where d.retired=false and upper(d.name) like :dn order by d.name";
            Map m = new HashMap();
            m.put("dn", "%" + selectText.toUpperCase() + "%");
            searchItems = getFacade().findBySQL(sql, m);
            if (searchItems != null && !searchItems.isEmpty()) {
                current = searchItems.get(0);
            } else {
                current = null;
            }
        }
    }

    public List<Department> getInstitutionDepatrments() {
        if (getInstitution() == null) {
            String sql = "Select d From Department d "
                    + " where d.retired=false "
                    + " and d.institution.id=" + getSessionController().getInstitution().getId();
            items = getFacade().findBySQL(sql);
        } else {
            String sql = "Select d From Department d "
                    + " where d.retired=false"
                    + " and d.institution=:ins";
            HashMap hm = new HashMap();
            hm.put("ins", getInstitution());
            items = getFacade().findBySQL(sql, hm);
        }

        //items = getFacade().findAll("name", true);
        return items;
    }

    public List<Department> listAllDepatrments() {
        List<Department> departments;
        String sql = "Select d From Department d "
                + " where d.retired=false ";
        departments = getFacade().findBySQL(sql);
        return departments;
    }

    public List<Department> getInstitutionDepatrments(Institution ins) {
        List<Department> deps;
        if (ins == null) {
            deps = new ArrayList<>();
        } else {
            Map m = new HashMap();
            m.put("ins", ins);
            String sql = "Select d From Department d "
                    + " where d.retired=false "
                    + " and d.institution=:ins "
                    + " order by d.name";
            deps = getFacade().findBySQL(sql, m);
        }
        return deps;
    }

    public List<Department> getLogedDepartments() {

        String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getSessionController().getInstitution().getId();
        items = getFacade().findBySQL(sql);

        return items;
    }

    public DepartmentType[] getDepartmentType() {
        return DepartmentType.values();
    }

    public List<Department> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from Department c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    List<Department> departmentList;

    public List<Department> completeDept(String qry) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and upper(c.name) like :q"
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        departmentList = getFacade().findBySQL(sql, hm);

        return departmentList;
    }

    public List<Department> completeDeptWithDeptOrIns(String qry) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and (upper(c.name) like :q or upper(c.institution.name) like :q )"
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        return getFacade().findBySQL(sql, hm);
    }

    public void prepareAdd() {
        codeDisabled = false;
        current = new Department();
    }

    public void setSelectedItems(List<Department> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private Boolean checkCodeExist() {
        String sql = "SELECT i FROM Department i where i.retired=false ";
        List<Department> ins = getEjbFacade().findBySQL(sql);
        if (ins != null) {
            for (Department i : ins) {
                if (i.getDepartmentCode().trim().equals("")) {
                    continue;
                }
                if (i.getDepartmentCode() != null && i.getDepartmentCode().equals(getCurrent().getDepartmentCode())) {
                    UtilityController.addErrorMessage("Insituion Code Already Exist Try another Code");
                    return true;
                }
            }
        }
        return false;
    }

    public void saveSelected() {
        if (getCurrent() == null || getCurrent().getName().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a name");
            return;
        }
        if (getCurrent().getInstitution() == null) {
            UtilityController.addErrorMessage("Please select an institution");
            return;
        }
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            if (getCurrent().getDepartmentCode() != null) {
                getCurrent().setDepartmentCode(getCurrent().getDepartmentCode());
            }
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Updated");
        } else {
            if (getCurrent().getDepartmentCode() != null) {
                if (!checkCodeExist()) {
                    getCurrent().setDepartmentCode(getCurrent().getDepartmentCode());

                } else {
                    return;
                }
            }
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("Saved");
        }
        fillSearchItems();
        recreateModel();
    }
    
    public List<Department> getInstitutionDepatrments(Institution ins, DepartmentType departmentType) {
        return getInstitutionDepatrments(ins, false, departmentType);
    }
    
    public List<Department> getInstitutionDepatrments(DepartmentType departmentType) {
        return getInstitutionDepatrments(null, true, departmentType);
    }
    
    public List<Department> getDepartments(String jpql, Map m) {
        return getDepartments(jpql, m, null);
    }

    public List<Department> getDepartments(String jpql) {
        return getDepartments(jpql, null, null);
    }

    public List<Department> getDepartments(String jpql,  TemporalType t) {
        return getDepartments(jpql, null, t);
    }
    
    public List<Department> getDepartments(String jpql, Map m, TemporalType t) {
        if (jpql == null || jpql.isEmpty() || jpql.trim().equals("")) {
            return new ArrayList<>();
        }
        if (t == null) {
            t = TemporalType.DATE;
        }
        if (m == null) {
            m = new HashMap();
        }
        return getFacade().findBySQL(jpql, m, t);
    }
    
    public List<Department> getInstitutionDepatrments(Institution ins, boolean includeAllInstitutionDepartmentsIfInstitutionIsNull, DepartmentType departmentType) {
        List<Department> deps;
        if (!includeAllInstitutionDepartmentsIfInstitutionIsNull) {
            if (ins == null) {
                deps = new ArrayList<>();
                return deps;
            }
        }
        Map m = new HashMap();
        String sql = "Select d From Department d "
                + " where d.retired=false ";
        if (ins != null) {
            sql += " and d.institution=:ins ";
            m.put("ins", ins);
        }
//        Department d = new Department();
//        d.getDepartmentType();
        if (departmentType != null) {
            sql += " and d.departmentType=:dt ";
            m.put("dt", departmentType);
        }
        sql += " order by d.name";
        deps = getFacade().findBySQL(sql, m);
        return deps;
    }


    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public DepartmentFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(DepartmentFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DepartmentController() {
    }

    public Department getCurrent() {
        return current;
    }

    public void setCurrent(Department current) {
        codeDisabled = true;
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public List<Department> getItemsToRemove() {
        return itemsToRemove;
    }

    public void setItemsToRemove(List<Department> itemsToRemove) {
        this.itemsToRemove = itemsToRemove;
    }

    public List<Department> getDepartmentList() {
        return departmentList;
    }

    public void setDepartmentList(List<Department> departmentList) {
        this.departmentList = departmentList;
    }
    
    /**
     * Getters & Setters
     */
    
    
    /**
     * 
     * @return 
     */
    
    

    private DepartmentFacade getFacade() {
        return ejbFacade;
    }

    public List<Department> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public Boolean getCodeDisabled() {
        return codeDisabled;
    }

    public void setCodeDisabled(Boolean codeDisabled) {
        this.codeDisabled = codeDisabled;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    /**
     * Converters
     */
    
    
    /**
     *
     */
    @FacesConverter("departmentConverter")
    public static class DepartmentControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DepartmentController controller = (DepartmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "departmentController");
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
            if (object instanceof Department) {
                Department o = (Department) object;
                return getStringKey(o.getId());
            } else {
                return "";
            }
        }
    }

    @FacesConverter(forClass = Department.class)
    public static class DepartmentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DepartmentController controller = (DepartmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "departmentController");
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
            if (object instanceof Department) {
                Department o = (Department) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DepartmentController.class.getName());
            }
        }
    }
}

/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.data.DepartmentType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.facade.DepartmentFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
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

    private Department superDepartment;

    List<Department> itemsToRemove;

    public Department findAndSaveDepartmentByName(String name) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        String sql;
        Map m = new HashMap();
        m.put("name", name);
        m.put("ret", false);
        sql = "select i "
                + " from Department i "
                + " where i.name=:name"
                + " and i.retired=:ret";
        Department i = getFacade().findFirstByJpql(sql, m);
        if (i == null) {
            i = new Department();
            i.setName(name);
            getFacade().create(i);
        } else {
            i.setRetired(false);
            getFacade().edit(i);
        }
        return i;
    }

    public void fillItems() {
        String j;
        j = "select i from Department i where i.retired=false order by i.name";
        items = getFacade().findByJpql(j);
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
            deps = getFacade().findByJpql(sql, m);
        }
        return deps;
    }

    @Deprecated
    public List<Department> getInsDepartments(Institution currentInstituion) {
        // Please use public List<Department> getInstitutionDepatrments(Institution ins) {
        List<Department> currentInsDepartments = new ArrayList<>();
        if (currentInstituion == null) {
            return currentInsDepartments;
        }
        Map m = new HashMap();
        m.put("ins", currentInstituion);
        m.put("ret", false);
        String jpql = "SELECT d "
                + " FROM Department d "
                + " where d.retired=:ret "
                + " and d.institution=:ins"
                + " order by d.name";
        currentInsDepartments = getFacade().findByJpql(jpql, m);
        if (currentInsDepartments == null) {
            currentInsDepartments = new ArrayList<>();
        }
        return currentInsDepartments;
    }

    public String toListDepartments() {
        fillItems();
        return "/admin/institutions/departments?faces-redirect=true";
    }

    public String toAddNewDepartment() {
        current = new Department();
        return "/admin/institutions/department?faces-redirect=true";
    }

    public String toEditDepartment() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/admin/institutions/department";
    }

    public String deleteDepartment() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        current.setRetired(true);
        getFacade().edit(current);
        return toListDepartments();
    }

    public String saveSelectedDepartment() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        if (current.getId() == null) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }
        return toListDepartments();
    }

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
            String jpql = "select d "
                    + "from Department d "
                    + "where d.retired=:ret "
                    + "order by d.name";

            Map m = new HashMap();
            m.put("ret", false);
            searchItems = getFacade().findByJpql(jpql, m);

            if (searchItems != null && !searchItems.isEmpty()) {
                current = searchItems.get(0);
            } else {
                current = null;
            }
        } else {
            String sql = "Select d from Department d where d.retired=false and (d.name) like :dn order by d.name";
            Map m = new HashMap();
            m.put("dn", "%" + selectText.toUpperCase() + "%");
            searchItems = getFacade().findByJpql(sql, m);
            if (searchItems != null && !searchItems.isEmpty()) {
                current = searchItems.get(0);
            } else {
                current = null;
            }
        }
    }

    public List<Department> fillAllItems() {
        List<Department> newItems;
        String sql = "Select d "
                + " from Department d "
                + " where d.retired=:ret "
                + " order by d.name";
        Map m = new HashMap();
        m.put("ret", false);
        newItems = getFacade().findByJpql(sql, m);
        return newItems;
    }

    public List<Department> getInstitutionDepatrments() {
        if (getInstitution() == null) {
            String sql = "Select d From Department d "
                    + " where d.retired=false "
                    + " and d.institution.id=" + getSessionController().getInstitution().getId();
            items = getFacade().findByJpql(sql);
        } else {
            String sql = "Select d From Department d "
                    + " where d.retired=false"
                    + " and d.institution=:ins";
            HashMap hm = new HashMap();
            hm.put("ins", getInstitution());
            items = getFacade().findByJpql(sql, hm);
        }
        return items;
    }

    public List<Department> listAllDepatrments() {
        List<Department> departments;
        String sql = "Select d From Department d "
                + " where d.retired=false ";
        departments = getFacade().findByJpql(sql);
        return departments;
    }

    

    public Department getDefaultDepatrment(Institution ins) {
        Department dep;
        if (ins == null) {
            dep = null;
        } else {
            Map m = new HashMap();
            m.put("ins", ins);
            String sql = "Select d From Department d "
                    + " where d.retired=false "
                    + " and d.institution=:ins ";
            dep = getFacade().findFirstByJpql(sql, m);
            if (dep == null) {
                dep = new Department();
                dep.setCreatedAt(new Date());
                dep.setCreater(getSessionController().getLoggedUser());
                dep.setDepartmentType(DepartmentType.Opd);
                dep.setInstitution(ins);
                dep.setName(ins.getName());
                getFacade().create(dep);
            }
        }
        return dep;
    }

    public List<Department> getLogedDepartments() {

        String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getSessionController().getInstitution().getId();
        items = getFacade().findByJpql(sql);

        return items;
    }

    public DepartmentType[] getDepartmentType() {
        return DepartmentType.values();
    }

    public List<Department> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Department c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public List<Department> getPharmacies() {
        return getDepartments(DepartmentType.Pharmacy, getSessionController().getLoggedUser().getInstitution());
    }

    public List<Department> getLabs() {
        return getDepartments(DepartmentType.Lab, null);
    }

    public List<Department> getDepartments(DepartmentType dt, Institution i) {
        String sql;
        Map m = new HashMap();

        sql = "select d from Department d "
                + " where d.retired=false ";
        if (dt != null) {
            sql += " and d.departmentType=:dt ";
            m.put("dt", dt);
        }
        if (i != null) {
            sql += " and d.institution=:ins ";
            m.put("ins", i);
        }
        sql += " order by d.name";
        selectedItems = getFacade().findByJpql(sql, m);
        return selectedItems;
    }

    List<Department> departmentList;

    public List<Department> completeDept(String qry) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and (c.name) like :q"
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        departmentList = getFacade().findByJpql(sql, hm);

        return departmentList;
    }

    public List<Department> completeDeptWithIns(String qry) {
        FacesContext context = FacesContext.getCurrentInstance();
        Institution selectedInstitution = (Institution) UIComponent.getCurrentComponent(context).getAttributes().get("selectedInstitution");
        String sql;
        HashMap<String, Object> hm = new HashMap<>();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " and c.institution=:ins "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        hm.put("ins", selectedInstitution);
        departmentList = getFacade().findByJpql(sql, hm);
        return departmentList;
    }

    public List<Department> completeDept(String qry, Institution ins) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " and c.institution=:ins "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        hm.put("ins", ins);
        departmentList = getFacade().findByJpql(sql, hm);

        return departmentList;
    }

    public List<Department> completeDeptPharmacy(String qry) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " and c.institution=:ins "
                + " and c.departmentType=:dt"
                + " order by c.name";

        hm.put("dt", DepartmentType.Pharmacy);
        hm.put("ins", getSessionController().getInstitution());
        hm.put("q", "%" + qry.toUpperCase() + "%");

        return getFacade().findByJpql(sql, hm);
    }

    public List<Department> completeDeptWithDeptOrIns(String qry) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and ((c.name) like :q or (c.institution.name) like :q )"
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        return getFacade().findByJpql(sql, hm);
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
        searchItems = null;
        items = null;
    }

    public void saveSelected() {
        if (getCurrent() == null || getCurrent().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a name");
            return;
        }
        if (getCurrent().getInstitution() == null) {
            JsfUtil.addErrorMessage("Please select an institution");
            return;
        }
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved");
        }
        recreateModel();
        fillSearchItems();

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

    public List<Department> getDepartments(String jpql, TemporalType t) {
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
        return getFacade().findByJpql(jpql, m, t);
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
        deps = getFacade().findByJpql(sql, m);
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
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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
        if (items == null) {
            fillSearchItems();
        }
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

    public void save(Department dep) {
        if (dep == null) {
            return;
        }
        if (dep.getId() == null) {
            getFacade().create(dep);
        } else {
            getFacade().edit(dep);
        }
    }

    public Department findDepartment(Long id) {
        return getFacade().find(id);
    }

    public Department getSuperDepartment() {
        return superDepartment;
    }

    public void setSuperDepartment(Department superDepartment) {
        this.superDepartment = superDepartment;
    }

    /**
     * Converters
     */
    /**
     *
     */
    @FacesConverter(forClass = Department.class)
    public static class DepartmentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DepartmentController controller = (DepartmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "departmentController");
            if (controller == null) {
                return null;
            }
            if (controller.getEjbFacade() == null) {
                return null;
            }
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            try {
                java.lang.Long key;
                key = Long.valueOf(value);
                return key;
            } catch (NumberFormatException e) {
                return 0l;
            }
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

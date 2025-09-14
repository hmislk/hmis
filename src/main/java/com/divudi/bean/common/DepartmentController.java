/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.data.DepartmentType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.entity.Route;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    private List<DepartmentDuplicateGroup> duplicateGroups;

    public Department findAndSaveDepartmentByName(String name) {
        if (name == null) {
            return null;
        }

        String cleanedName = name.trim();
        if (cleanedName.isEmpty()) {
            return null;
        }

        String sql = "select i from Department i where upper(i.name)=:name and i.retired=:ret";
        Map<String, Object> m = new HashMap<>();
        m.put("name", cleanedName.toUpperCase());
        m.put("ret", false);
        Department i = getFacade().findFirstByJpql(sql, m);

        if (i == null) {
            i = new Department();
            i.setName(cleanedName);
            getFacade().create(i);
        } else if (i.isRetired()) {
            i.setRetired(false);
            getFacade().edit(i);
        }

        return i;
    }

    public Department findAndSaveDepartmentByName(String name, Institution ins) {
        if (name == null) {
            return null;
        }

        String cleanedName = name.trim();
        if (cleanedName.isEmpty()) {
            return null;
        }

        String sql = "select i from Department i where upper(i.name)=:name and i.retired=:ret";
        Map<String, Object> m = new HashMap<>();
        m.put("name", cleanedName.toUpperCase());
        m.put("ret", false);
        Department i = getFacade().findFirstByJpql(sql, m);

        if (i == null) {
            i = new Department();
            i.setName(cleanedName);
            i.setInstitution(ins);
            getFacade().create(i);
        } else if (i.isRetired()) {
            i.setRetired(false);
            getFacade().edit(i);
        }

        return i;
    }

    public Department findExistingDepartmentByName(String name, Institution ins) {
        if (name == null) {
            return null;
        }

        String cleanedName = name.trim();
        if (cleanedName.isEmpty()) {
            return null;
        }

        String sql = "select i from Department i where upper(i.name)=:name and i.retired=:ret";
        Map<String, Object> m = new HashMap<>();
        m.put("name", cleanedName.toUpperCase());
        m.put("ret", false);
        Department i = getFacade().findFirstByJpql(sql, m);
        return i;
    }

    public DepartmentType findDepartmentType(String deptType) {
        if (deptType == null || deptType.trim().isEmpty()) {
            return DepartmentType.Other; // Default to 'Other' if the input is null or empty
        }

        String cleanedDeptType = deptType.trim().toLowerCase();

        // First, try to match with enum name
        for (DepartmentType type : DepartmentType.values()) {
            if (type.name().equalsIgnoreCase(cleanedDeptType)) {
                return type;
            }
        }

        // Next, try to match with labels
        for (DepartmentType type : DepartmentType.values()) {
            if (type.getLabel().equalsIgnoreCase(cleanedDeptType)) {
                return type;
            }
        }

        // Finally, attempt partial match with labels
        for (DepartmentType type : DepartmentType.values()) {
            if (type.getLabel().toLowerCase().contains(cleanedDeptType)) {
                return type;
            }
        }

        // If no match found, default to 'Other'
        return DepartmentType.Other;
    }

    public void fillItems() {
        String j;
        j = "select i from Department i where i.retired=false order by i.name";
        items = getFacade().findByJpql(j);
    }

    public List<Department> getInstitutionDepartments(Institution ins) {
        List<Department> deps;
        if (ins == null) {
            deps = new ArrayList<>();
        } else {
            Map<String, Object> m = new HashMap<>();
            m.put("ins", ins);
            String jpql = "Select d From Department d "
                    + " where d.retired=false "
                    + " and d.institution=:ins "
                    + " and TYPE(d) <> Route "
                    + " order by d.name";
            deps = getFacade().findByJpql(jpql, m);
        }
        return deps;
    }

    public List<Department> getInstitutionLabDepartments(Institution ins) {
        List<Department> deps;
        if (ins == null) {
            deps = new ArrayList<>();
        } else {
            Map<String, Object> m = new HashMap<>();
            m.put("ins", ins);
            m.put("type", DepartmentType.Lab);
            String jpql = "Select d From Department d "
                    + " where d.retired=false "
                    + " and d.institution=:ins "
                    + " and d.departmentType=:type "
                    + " and TYPE(d) <> Route "
                    + " order by d.name";
            deps = getFacade().findByJpql(jpql, m);
        }
        return deps;
    }
    
    public List<Department> getInstitutionAllLabTypesDepartments(Institution ins) {
    List<Department> deps;
        System.out.println("ins = " + ins);
    if (ins == null) {
        deps = new ArrayList<>();
    } else {
        List<DepartmentType> dtypes = Arrays.asList(DepartmentType.Lab, DepartmentType.External_Lab);
        System.out.println("dtypes = " + dtypes);
        Map<String, Object> m = new HashMap<>();
        m.put("ins", ins);
        m.put("types", dtypes);
        
        String jpql = "Select d From Department d "
                + " where d.retired=false "
                + " and d.institution=:ins "
                + " and d.departmentType in :types "
                + " and TYPE(d) NOT IN (Route)" // Adjust based on your entity structure
                + " order by d.name";
        
        deps = getFacade().findByJpql(jpql, m);
        System.out.println("deps = " + deps);
    }
    return deps;
}

    public List<Department> getAllDepartmentsWithInstitutionFilter(Institution ins) {
        List<Department> deps;
        Map<String, Object> m = new HashMap<>();

        String jpql = "Select d From Department d "
                + " where d.retired=false "
                + " and TYPE(d) <> Route "
                + " and d.name IS NOT NULL "
                + " and TRIM(d.name) <> '' ";

        if (ins != null) {
            m.put("ins", ins);
            jpql += " and d.institution=:ins ";
        }

        jpql += " order by d.name";

        deps = getFacade().findByJpql(jpql, m);

        return deps != null ? deps : new ArrayList<>();
    }

    public List<Department> getInstitutionDepartmentsWithSite(Institution ins, Institution site) {
        List<Department> deps;
        Map<String, Object> m = new HashMap<>();
        String jpql = "Select d From Department d "
                + " where d.retired=false ";
        if (site != null) {
            jpql += " and d.site=:site ";
            m.put("site", site);
        }
        if (ins != null) {
            jpql += " and d.institution=:ins ";
            m.put("ins", ins);
        }

        jpql += " and TYPE(d) <> Route "
                + " order by d.name ";

        deps = getFacade().findByJpql(jpql, m);

        return deps;
    }

    public List<Department> getInstitutionRoutes(Institution ins) {
        List<Department> deps;
        if (ins == null) {
            deps = new ArrayList<>();
        } else {
            Map m = new HashMap();
            m.put("ins", ins);
            String sql = "Select d From Route d "
                    + " where d.retired=false "
                    + " and d.institution=:ins "
                    + " order by d.name";
            deps = getFacade().findByJpql(sql, m);
        }
        return deps;
    }

    @Deprecated
    public List<Department> getInsDepartments(Institution currentInstitution) {
        // Please use public List<Department> getInstitutionDepatrments(Institution ins) {
        List<Department> currentInsDepartments = new ArrayList<>();
        if (currentInstitution == null) {
            return currentInsDepartments;
        }
        Map m = new HashMap();
        m.put("ins", currentInstitution);
        m.put("ret", false);
        String jpql = "SELECT d "
                + " FROM Department d "
                + " where d.retired=:ret "
                + " and d.institution=:ins "
                + " order by d.name";
        currentInsDepartments = getFacade().findByJpql(jpql, m);
        if (currentInsDepartments == null) {
            currentInsDepartments = new ArrayList<>();
        }
        return currentInsDepartments;
    }

    public List<Department> getDepartmentsOfInstitutionAndSite() {
        return getDepartmentsOfInstitutionAndSiteHelper(null, null);
    }

    public List<Department> getDepartmentsOfInstitutionAndSite(Institution site) {
        return getDepartmentsOfInstitutionAndSiteHelper(null, site);
    }

    public List<Department> getDepartmentsOfInstitutionAndSiteForInstitution(Institution ins) {
        return getDepartmentsOfInstitutionAndSiteHelper(ins, null);
    }

    public List<Department> getDepartmentsOfInstitutionAndSite(Institution ins, Institution site) {
        return getDepartmentsOfInstitutionAndSiteHelper(ins, site);
    }

    private List<Department> getDepartmentsOfInstitutionAndSiteHelper(Institution ins, Institution site) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder jpql = new StringBuilder("SELECT d FROM Department d WHERE d.retired = :ret AND TYPE(d) NOT IN :excludedTypes");
        parameters.put("ret", false);

        // Create a list of classes to exclude (e.g., Route)
        List<Class<?>> excludedTypes = new ArrayList<>();
        excludedTypes.add(Route.class); // Add any other subclasses to exclude if needed
        parameters.put("excludedTypes", excludedTypes);

        // Exclude departments without names or with blank names
        jpql.append(" AND d.name IS NOT NULL AND TRIM(d.name) <> ''");

        // Exclude departments with institution type of CollectingCentre
        jpql.append(" AND d.institution.institutionType <> :cc ");
        parameters.put("cc", InstitutionType.CollectingCentre);

        if (ins != null) {
            jpql.append(" AND d.institution = :ins");
            parameters.put("ins", ins);
        }

        if (site != null) {
            jpql.append(" AND d.site = :site");
            parameters.put("site", site);
        }

        jpql.append(" ORDER BY d.name");

        List<Department> departments = getFacade().findByJpql(jpql.toString(), parameters);
        return departments != null ? departments : new ArrayList<>();
    }

//    public List<Department> getDepartmentsOfInstitutionAndSite(Institution ins, Institution site) {
//        if (ins == null && site == null) {
//            return new ArrayList<>();
//        }
//        Map<String, Object> parameters = new HashMap<>();
//        StringBuilder jpql = new StringBuilder("SELECT d FROM Department d WHERE d.retired = :ret ");
//        parameters.put("ret", false);
//        if (ins != null) {
//            jpql.append(" AND d.institution = :ins");
//            parameters.put("ins", ins);
//        }
//        if (site != null) {
//            jpql.append(" AND d.site = :site");
//            parameters.put("site", site);
//        }
//        if (ins == null && site == null) {
//            return new ArrayList<>();
//        }
//        jpql.append(" ORDER BY d.name");
//        List<Department> currentInsDepartments = getFacade().findByJpql(jpql.toString(), parameters);
//        return currentInsDepartments != null ? currentInsDepartments : new ArrayList<>();
//    }
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
        if (current.getName() != null) {
            current.setName(current.getName().trim());
        }
        String sql = "select d from Department d where upper(d.name)=:nm and d.retired=false";
        Map<String, Object> m = new HashMap<>();
        m.put("nm", current.getName() == null ? "" : current.getName().toUpperCase());
        Department existing = getFacade().findFirstByJpql(sql, m);
        if (existing != null && (current.getId() == null || !existing.getId().equals(current.getId()))) {
            JsfUtil.addErrorMessage("Department with same name already exists");
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

    public List<Department> listAllLabDepartments() {
        List<Department> departments;
        Map<String, Object> m = new HashMap<>();
        m.put("type", DepartmentType.Lab);
        String sql = "Select d From Department d "
                + " where d.retired=false "
                + " and d.departmentType=:type";
        departments = getFacade().findByJpql(sql, m);
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
        List<Department> results = new ArrayList<>();
        if (qry == null) {
            return results;
        }
        if (qry.trim().equals("")) {
            return results;
        }
        qry = qry.trim();
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Department c "
                + " where c.retired=false "
                + " and c.name like :q "
                + " and c.institution=:ins "
                + " and c.departmentType=:dt"
                + " order by c.name";

        hm.put("dt", DepartmentType.Pharmacy);
        hm.put("ins", getSessionController().getInstitution());
        hm.put("q", "%" + qry.toUpperCase() + "%");
        results = getFacade().findByJpql(sql, hm);

        if (results != null && !results.isEmpty()) {
            return results;
        }
        results = new ArrayList<>();
        if (qry.length() > 2) {
            hm = new HashMap();
            sql = "select c from Department c "
                    + " where c.retired=false "
                    + " and c.name like :q "
                    + " order by c.name";
            hm.put("q", "%" + qry.toUpperCase() + "%");
            results = getFacade().findByJpql(sql, hm);
        }
        return results;
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

    public List<Department> getInstitutionDepatrments(Institution ins) {
        return getInstitutionDepatrments(ins, true, null);
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

    public Department findDepartment(String strId) {
        if (strId == null) {
            return null;
        }
        Long id;
        try {
            id = Long.valueOf(strId);
        } catch (Exception e) {
            return null;
        }
        return getFacade().find(id);
    }

    public Department getSuperDepartment() {
        return superDepartment;
    }

    public void setSuperDepartment(Department superDepartment) {
        this.superDepartment = superDepartment;
    }

    public List<DepartmentDuplicateGroup> getDuplicateGroups() {
        return duplicateGroups;
    }

    public String navigateToDuplicateDepartments() {
        detectDuplicateDepartments();
        return "/admin/institutions/department_duplicates?faces-redirect=true";
    }

    public void detectDuplicateDepartments() {
        String jpql = "SELECT d FROM Department d WHERE d.retired=false ORDER BY UPPER(TRIM(d.name)), d.id";
        List<Department> all = getFacade().findByJpql(jpql);
        Map<String, List<Department>> grouped = all.stream()
                .collect(Collectors.groupingBy(d -> d.getName() == null ? "" : d.getName().trim().toUpperCase()));
        duplicateGroups = grouped.values().stream()
                .filter(l -> l.size() > 1)
                .map(l -> new DepartmentDuplicateGroup(l))
                .collect(Collectors.toList());
    }

    public void retireDuplicateGroup(DepartmentDuplicateGroup g) {
        if (g == null || g.getDepartments() == null || g.getDepartments().size() < 2) {
            return;
        }
        g.getDepartments().sort((a, b) -> a.getId().compareTo(b.getId()));
        for (int i = 1; i < g.getDepartments().size(); i++) {
            Department d = g.getDepartments().get(i);
            d.setRetired(true);
            d.setRetiredAt(new Date());
            d.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(d);
        }
        detectDuplicateDepartments();
        JsfUtil.addSuccessMessage("Duplicates retired for " + g.getName());
    }

    public static class DepartmentDuplicateGroup {

        private List<Department> departments;

        public DepartmentDuplicateGroup(List<Department> departments) {
            this.departments = departments;
        }

        public List<Department> getDepartments() {
            return departments;
        }

        public String getName() {
            return departments.get(0).getName();
        }
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
                        + object.getClass().getName() + "; expected type: " + Department.class.getName());
            }
        }
    }
}

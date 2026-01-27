package com.divudi.bean.common;

import com.divudi.bean.common.UserPrivilageController.PrivilegeHolder;
import com.divudi.core.entity.TriggerSubscription;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.Privileges;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.WebUserPrivilege;
import com.divudi.core.entity.WebUserRole;
import com.divudi.core.entity.WebUserRolePrivilege;
import com.divudi.core.entity.WebUserRoleUser;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import com.divudi.core.facade.WebUserRoleUserFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Named
@SessionScoped
public class WebUserRoleUserController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @EJB
    private WebUserRoleUserFacade facade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;
    
    @Inject
    private SessionController sessionController;
    @Inject
    private UserPrivilageController userPrivilageController;
    
    private WebUserRoleUser current;
    private WebUserRole webUserRole;
    private List<WebUser> users = null;
    private WebUser webUser = null;
    private List<WebUserRoleUser> roleUsers = null;
    private Institution institution;
    private Department department;
    private List<Department> departments;

    public void addUsersToDepartmentRoleWithPrivileges() {
        if (webUser == null) {
            JsfUtil.addErrorMessage("Select User");
            return;
        }
        if (webUserRole == null) {
            JsfUtil.addErrorMessage("Select Role");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select Department");
            return;
        }
        String jpql = "select ru "
                + " from WebUserRoleUser ru "
                + " where ru.department=:dept "
                + " and ru.webUserRole=:role"
                + " and ru.webUser=:user "
                + " order by ru.id desc ";
        Map params = new HashMap();
        params.put("dept", department);
        params.put("role", webUserRole);
        params.put("user", webUser);
        WebUserRoleUser roleUser = getFacade().findFirstByJpql(jpql, params);

        if (roleUser == null) {
            roleUser = new WebUserRoleUser();
            roleUser.setDepartment(department);
            roleUser.setWebUser(webUser);
            roleUser.setWebUserRole(webUserRole);
            getFacade().create(roleUser);
        } else {
            roleUser.setRetired(false);
        }
        
        updatePrivilegesToUserRole(webUserRole,webUser,department);
        
        clear();
        loadWebUserRoles();
    }
    
    public void clear(){
        webUserRole = null;
        department = null;
    }
    
    public void updatePrivilegesToUserRole(WebUserRole userRole, WebUser user, Department department){
        List<WebUserRolePrivilege> rolePrivileges = userPrivilageController.fetchUserPrivileges(userRole);
        
        for(WebUserRolePrivilege wurp:rolePrivileges){
            Privileges p = wurp.getPrivilege();
            userPrivilageController.addUserPrivilege(p, user, department);
        }
    }
    
    public void resetRolePrivileges(WebUserRoleUser roleUser){
        
        WebUser user = roleUser.getWebUser();
        Department dept = roleUser.getDepartment();
        
        // Clesr All Privillages
        userPrivilageController.clesrUserAllDepartmentPrivileges(user,dept);
        // Add Role Privillage
        updatePrivilegesToUserRole(roleUser.getWebUserRole(),user, dept);
        
        JsfUtil.addSuccessMessage("Reset "+ roleUser.getWebUserRole().getName() +" UserRole Privileges for " + dept.getName());
    }
    
    public void clesrUserRolePrivileges(WebUserRoleUser roleUser){
        List<WebUserRolePrivilege> rolePrivileges = userPrivilageController.fetchUserPrivileges(roleUser.getWebUserRole());
        if (rolePrivileges == null || rolePrivileges.isEmpty()) {
            return;
        }

        Set<Privileges> rolePrivilegeSet = new HashSet<>();
        for (WebUserRolePrivilege wurp : rolePrivileges) {
            if (wurp.getPrivilege() != null) {
                rolePrivilegeSet.add(wurp.getPrivilege());
            }
        }

        List<WebUserPrivilege> userPrivileges = userPrivilageController.loadUserPrivileges(roleUser.getWebUser(), roleUser.getDepartment());

        for (WebUserPrivilege wup : userPrivileges) {
            if (wup.getPrivilege() != null && rolePrivilegeSet.contains(wup.getPrivilege())) {
                wup.setRetired(true);
                wup.setRetiredAt(new Date());
                wup.setRetirer(sessionController.getLoggedUser());
                webUserPrivilegeFacade.edit(wup);
            }
        }
    }
    
    public void loadWebUserRoles(){
        roleUsers = new ArrayList<>();
        
        String jpql = "select ru "
                + " from WebUserRoleUser ru "
                + " where ru.retired=:ret "
                + " and ru.webUser=:user "
                + " order by ru.id desc ";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("user", webUser);

        roleUsers = getFacade().findByJpqlWithoutCache(jpql, params);

    }

    public void addUsers() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select UserRole");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select Department");
            return;
        }
        if (current.getWebUserRole() == null) {
            JsfUtil.addErrorMessage("Program Error. Cannot have this page without a user. Create an issue in GitHub");
            return;
        }
        if (isUsersAlreadyAdded()) {
            JsfUtil.addErrorMessage("User already Added to User Role");
        } else {
            fillUseRolePrivilage();
            List<PrivilegeHolder> userRolePrivilage = userPrivilageController.getCurrentUserPrivilegeHolders();
            if (userRolePrivilage == null) {
                JsfUtil.addErrorMessage("There are No Permission to add!");
            } else {
                userPrivilageController.saveWebUserPrivileges(current.getWebUser(), userRolePrivilage, department);
                WebUserRoleUser ru = new WebUserRoleUser();
                ru.setWebUserRole(current.getWebUserRole());
                ru.setWebUser(current.getWebUser());
                ru.setCreatedAt(new Date());
                ru.setCreater(sessionController.getLoggedUser());
                save(ru);
                JsfUtil.addSuccessMessage("Save Success ");

                fillRoleUsers();
            }
        }

    }

    public void fillDepartment() {
        departments = fillWebUserDepartments(current.getWebUser());
    }

    public List<Department> fillWebUserDepartments(WebUser wu) {
        Set<Department> departmentSet = new HashSet<>();
        String sql = "SELECT i.department "
                + " FROM WebUserDepartment i "
                + " WHERE i.retired = :ret "
                + " AND i.webUser = :wu "
                + " ORDER BY i.department.name";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("wu", wu);
        List<Department> depts = departmentFacade.findByJpql(sql, m);
        departmentSet.addAll(depts);
        return new ArrayList<>(departmentSet);
    }

    public void fillUseRolePrivilage() {
        userPrivilageController.fillUserRolePrivileges(current.getWebUserRole());
    }

    public void fillUsers() {
        if (current.getWebUserRole() == null) {
            JsfUtil.addErrorMessage("User Role?");
        }
        Map m = new HashMap();
        String jpql = "SELECT i FROM WebUser i WHERE i.retired = :ret";
        m.put("ret", false);
        users = getFacade().findByJpql(jpql, m);
        fillRoleUsers();
        fillUseRolePrivilage();
        fillDepartment();
    }

    // Method to validate if the Icon is already added for the webUser
    public boolean isUsersAlreadyAdded() {
        for (WebUser ru : users) {
            if (ru == current.getWebUser()) {
                JsfUtil.addErrorMessage("User already added");
                return true;
            }
        }
        return false;
    }

    private void fillRoleUsers() {
        roleUsers = fillRoleUsers(current.getWebUserRole());
    }

    public List<WebUserRoleUser> fillRoleUsers(WebUserRole u) {
        List<WebUserRoleUser> wus = null;
        if (u == null) {
            roleUsers = null;
            return wus;
        }
        String Jpql = "select i "
                + " from WebUserRoleUser i "
                + " where i.retired=:ret "
                + " and i.webUserRole=:u ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("u", u);
        wus = getFacade().findByJpql(Jpql, m);
//        Collections.sort(wus, new UserSubscriptionOrderComparator());
        return wus;
    }

    public void save(WebUserRoleUser ru) {
        if (ru == null) {
            return;
        }
        if (ru.getId() != null) {
            getFacade().edit(ru);
        } else {
            getFacade().create(ru);
        }
    }

    public void removeUser(WebUserRoleUser roleUser) {
        if(roleUser == null){
            JsfUtil.addErrorMessage("Error in UserRole");
                return;
        }
        
        WebUserRoleUser user = facade.findWithoutCache(roleUser.getId());
        
        if (user != null) {
            clesrUserRolePrivileges(user);
            user.setRetired(true);
            user.setRetiredAt(new Date());
            roleUser.setRetirer(sessionController.getLoggedUser());
            save(user);
            JsfUtil.addSuccessMessage("Removed Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Remove");
        }
        loadWebUserRoles();
        clear();
    }

    public WebUserRoleUserController() {
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebUserRoleUserFacade getFacade() {
        return facade;
    }

    public void setFacade(WebUserRoleUserFacade facade) {
        this.facade = facade;
    }

    public WebUserRoleUser getCurrent() {
        if (current == null) {
            current = new WebUserRoleUser();
        }
        return current;
    }

    public void setCurrent(WebUserRoleUser current) {
        this.current = current;
    }

    public List<WebUser> getUsers() {
        if (users == null) {
            Map m = new HashMap();
            String jpql = "SELECT i FROM WebUser i WHERE i.retired = :ret";
            m.put("ret", false);
            users = getFacade().findByJpql(jpql, m);
        }
        return users;
    }

    public void setUsers(List<WebUser> users) {
        this.users = users;
    }

    public List<WebUserRoleUser> getRoleUsers() {
        return roleUsers;
    }

    public void setRoleUsers(List<WebUserRoleUser> roleUsers) {
        this.roleUsers = roleUsers;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public WebUserRole getWebUserRole() {
        return webUserRole;
    }

    public void setWebUserRole(WebUserRole webUserRole) {
        this.webUserRole = webUserRole;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @FacesConverter(forClass = WebUserRoleUser.class)
    public static class WebUserRoleUserControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebUserRoleUserController controller = (WebUserRoleUserController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webUserRoleUserController");
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
            if (object instanceof WebUserRoleUser) {
                WebUserRoleUser o = (WebUserRoleUser) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebUserRoleUser.class.getName());
            }
        }
    }

    public static class UserSubscriptionOrderComparator implements Comparator<TriggerSubscription> {

        @Override
        public int compare(TriggerSubscription o1, TriggerSubscription o2) {
            return Double.compare(o1.getOrderNumber(), o2.getOrderNumber());
        }
    }

}

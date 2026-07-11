/*
 * Open Hospital Management Information System
 *
 * Role-template based user management: reset/expand/narrow a user's
 * privileges/icons/subscriptions/login-page against a WebUserRole template,
 * for a single user or in bulk. Thin UI layer over
 * com.divudi.service.UserRoleApplicationService (the single engine also used
 * by the REST API and the AI assistant).
 */
package com.divudi.bean.common;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.WebUserRole;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.facade.WebUserRoleFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.UserRoleApplicationService;
import com.divudi.service.UserRoleApplicationService.RoleAspect;
import com.divudi.service.UserRoleApplicationService.RoleApplicationResult;
import com.divudi.service.UserRoleApplicationService.RoleOperation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
 * @author www.divudi.com
 */
@Named
@SessionScoped
public class UserRoleManagementController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Same cap as UserManagementApi.bulkRoleOperations — bulk role
     * operations run synchronously, so bound one run's size.
     */
    private static final int MAX_BULK_USERS = 500;

    // <editor-fold desc="EJBs / injected controllers">
    @EJB
    private UserRoleApplicationService userRoleApplicationService;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private WebUserRoleFacade webUserRoleFacade;
    @EJB
    private InstitutionFacade institutionFacade;

    @Inject
    private SessionController sessionController;
    // </editor-fold>

    // <editor-fold desc="Per-user page state">
    private WebUser currentUser;
    private WebUserRole selectedRole;
    private List<WebUserRole> roles;
    private List<Department> userDepartments;
    private List<Department> selectedDepartments;

    private boolean aspectPrivileges = true;
    private boolean aspectIcons;
    private boolean aspectSubscriptions;
    private boolean aspectLoginPage;

    private boolean updateUserRole = true;

    private String previewText;
    private List<UserRoleApplicationService.RoleApplicationResult> results;
    // </editor-fold>

    // <editor-fold desc="Bulk page state">
    private WebUserRole filterRole;
    private Department filterDepartment;
    private Institution filterInstitution;
    private List<Department> allDepartments;
    private List<Institution> institutions;
    private List<WebUser> filteredUsers;
    private List<WebUser> selectedUsers;
    // </editor-fold>

    /**
     * Re-entrancy guard shared across the action methods on this
     * session-scoped bean, so a double click (or two tabs racing) cannot
     * apply the same role operation twice.
     */
    private volatile boolean processing;

    // <editor-fold desc="Navigation">
    public String navigateToManageUserRoleDefaults(WebUser user) {
        if (user == null) {
            JsfUtil.addErrorMessage("Please select a user");
            return "";
        }
        currentUser = user;
        selectedRole = user.getRole();
        userDepartments = fillWebUserDepartments(user);
        selectedDepartments = new ArrayList<>();
        results = null;
        previewText = null;
        return "/admin/users/user_role_users?faces-redirect=true";
    }

    public String navigateToBulkRoleOperations() {
        filterRole = null;
        filterDepartment = null;
        filterInstitution = null;
        allDepartments = null;
        institutions = null;
        filteredUsers = null;
        selectedUsers = new ArrayList<>();
        selectedDepartments = new ArrayList<>();
        selectedRole = null;
        results = null;
        previewText = null;
        return "/admin/users/user_role_bulk_operations?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold desc="Per-user actions">
    public void saveUserRole() {
        if (processing) {
            JsfUtil.addErrorMessage("Already processing a request. Please wait.");
            return;
        }
        processing = true;
        try {
            if (currentUser == null) {
                JsfUtil.addErrorMessage("No user selected.");
                return;
            }
            currentUser.setRole(selectedRole);
            webUserFacade.edit(currentUser);
            JsfUtil.addSuccessMessage("User role saved.");
        } finally {
            processing = false;
        }
    }

    public void previewOperation(String op) {
        if (processing) {
            JsfUtil.addErrorMessage("Already processing a request. Please wait.");
            return;
        }
        processing = true;
        try {
            previewText = null;
            RoleOperation operation = parseOperation(op);
            if (operation == null) {
                JsfUtil.addErrorMessage("Unknown operation.");
                return;
            }
            if (currentUser == null) {
                JsfUtil.addErrorMessage("No user selected.");
                return;
            }
            WebUserRole effectiveRole = selectedRole != null ? selectedRole : currentUser.getRole();
            if (effectiveRole == null) {
                JsfUtil.addErrorMessage("No role available: select a role or ensure the user has a default role.");
                return;
            }
            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one department.");
                return;
            }
            Set<RoleAspect> aspects = selectedAspects();
            if (aspects == null) {
                return;
            }
            Map<RoleAspect, Long> counts = userRoleApplicationService.previewCounts(
                    operation, currentUser, effectiveRole, selectedDepartments, aspects);
            previewText = formatPreview(counts);
        } finally {
            processing = false;
        }
    }

    public void executeResetToDefaultRole() {
        if (processing) {
            JsfUtil.addErrorMessage("Already processing a request. Please wait.");
            return;
        }
        processing = true;
        try {
            if (currentUser == null) {
                JsfUtil.addErrorMessage("No user selected.");
                return;
            }
            WebUserRole role = currentUser.getRole();
            if (role == null) {
                JsfUtil.addErrorMessage("User has no default role");
                return;
            }
            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one department.");
                return;
            }
            Set<RoleAspect> aspects = selectedAspects();
            if (aspects == null) {
                return;
            }
            RoleApplicationResult result = userRoleApplicationService.apply(
                    RoleOperation.RESET, currentUser, role, selectedDepartments, aspects, false, actor());
            applyResult(result);
        } finally {
            processing = false;
        }
    }

    public void executeResetToSelectedRole() {
        if (processing) {
            JsfUtil.addErrorMessage("Already processing a request. Please wait.");
            return;
        }
        processing = true;
        try {
            if (currentUser == null) {
                JsfUtil.addErrorMessage("No user selected.");
                return;
            }
            if (selectedRole == null) {
                JsfUtil.addErrorMessage("Select a role.");
                return;
            }
            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one department.");
                return;
            }
            Set<RoleAspect> aspects = selectedAspects();
            if (aspects == null) {
                return;
            }
            RoleApplicationResult result = userRoleApplicationService.apply(
                    RoleOperation.RESET, currentUser, selectedRole, selectedDepartments, aspects, updateUserRole, actor());
            applyResult(result);
        } finally {
            processing = false;
        }
    }

    public void executeExpand() {
        executeRoleOperation(RoleOperation.EXPAND);
    }

    public void executeNarrow() {
        executeRoleOperation(RoleOperation.NARROW);
    }

    private void executeRoleOperation(RoleOperation operation) {
        if (processing) {
            JsfUtil.addErrorMessage("Already processing a request. Please wait.");
            return;
        }
        processing = true;
        try {
            if (currentUser == null) {
                JsfUtil.addErrorMessage("No user selected.");
                return;
            }
            if (selectedRole == null) {
                JsfUtil.addErrorMessage("Select a role.");
                return;
            }
            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one department.");
                return;
            }
            Set<RoleAspect> aspects = selectedAspects();
            if (aspects == null) {
                return;
            }
            RoleApplicationResult result = userRoleApplicationService.apply(
                    operation, currentUser, selectedRole, selectedDepartments, aspects, false, actor());
            applyResult(result);
        } finally {
            processing = false;
        }
    }

    private void applyResult(RoleApplicationResult result) {
        results = java.util.Collections.singletonList(result);
        if (result.isSuccess()) {
            JsfUtil.addSuccessMessage("Applied: " + result.summary());
        } else {
            JsfUtil.addErrorMessage(result.getErrorMessage());
        }
    }
    // </editor-fold>

    // <editor-fold desc="Bulk actions">
    public void fillUsersByFilter() {
        StringBuilder jpql = new StringBuilder("select u from WebUser u where u.retired=false");
        Map<String, Object> params = new HashMap<>();
        if (filterRole != null) {
            jpql.append(" and u.role=:r");
            params.put("r", filterRole);
        }
        if (filterDepartment != null) {
            jpql.append(" and u.id in (select wd.webUser.id from WebUserDepartment wd where wd.department=:d and wd.retired=false)");
            params.put("d", filterDepartment);
        }
        if (filterInstitution != null) {
            jpql.append(" and u.institution=:ins");
            params.put("ins", filterInstitution);
        }
        jpql.append(" order by u.name");
        filteredUsers = webUserFacade.findByJpql(jpql.toString(), params);
        selectedUsers = new ArrayList<>();
    }

    public void selectAllFilteredUsers() {
        selectedUsers = filteredUsers == null ? new ArrayList<>() : new ArrayList<>(filteredUsers);
    }

    public void executeBulk(String op) {
        if (processing) {
            JsfUtil.addErrorMessage("Already processing a request. Please wait.");
            return;
        }
        processing = true;
        try {
            RoleOperation operation = parseOperation(op);
            if (operation == null) {
                JsfUtil.addErrorMessage("Unknown operation.");
                return;
            }
            if (selectedUsers == null || selectedUsers.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one user.");
                return;
            }
            if (selectedUsers.size() > MAX_BULK_USERS) {
                JsfUtil.addErrorMessage("Too many users selected (" + selectedUsers.size()
                        + "). Narrow the filter to " + MAX_BULK_USERS + " users or fewer per run.");
                return;
            }
            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one department.");
                return;
            }
            Set<RoleAspect> aspects = selectedAspects();
            if (aspects == null) {
                return;
            }
            WebUserRole role = selectedRole;
            if (role == null && operation != RoleOperation.RESET) {
                JsfUtil.addErrorMessage("Select a role.");
                return;
            }
            results = userRoleApplicationService.applyBulk(
                    operation, selectedUsers, role, selectedDepartments, aspects, updateUserRole, actor());
            JsfUtil.addSuccessMessage(RoleApplicationResult.summarize(results));
        } finally {
            processing = false;
        }
    }

    public void previewBulk(String op) {
        if (processing) {
            JsfUtil.addErrorMessage("Already processing a request. Please wait.");
            return;
        }
        processing = true;
        try {
            previewText = null;
            RoleOperation operation = parseOperation(op);
            if (operation == null) {
                JsfUtil.addErrorMessage("Unknown operation.");
                return;
            }
            if (selectedUsers == null || selectedUsers.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one user.");
                return;
            }
            if (selectedUsers.size() > MAX_BULK_USERS) {
                JsfUtil.addErrorMessage("Too many users selected (" + selectedUsers.size()
                        + "). Narrow the filter to " + MAX_BULK_USERS + " users or fewer per run.");
                return;
            }
            if (selectedDepartments == null || selectedDepartments.isEmpty()) {
                JsfUtil.addErrorMessage("Select at least one department.");
                return;
            }
            Set<RoleAspect> aspects = selectedAspects();
            if (aspects == null) {
                return;
            }
            if (selectedRole == null && operation != RoleOperation.RESET) {
                JsfUtil.addErrorMessage("Select a role.");
                return;
            }
            Map<RoleAspect, Long> totals = new EnumMap<>(RoleAspect.class);
            int usersCounted = 0;
            for (WebUser u : selectedUsers) {
                if (u == null) {
                    continue;
                }
                WebUserRole effectiveRole = selectedRole != null ? selectedRole : u.getRole();
                if (effectiveRole == null) {
                    continue;
                }
                Map<RoleAspect, Long> counts = userRoleApplicationService.previewCounts(
                        operation, u, effectiveRole, selectedDepartments, aspects);
                for (Map.Entry<RoleAspect, Long> e : counts.entrySet()) {
                    totals.merge(e.getKey(), e.getValue(), Long::sum);
                }
                usersCounted++;
            }
            previewText = usersCounted + " users; " + formatPreview(totals);
        } finally {
            processing = false;
        }
    }
    // </editor-fold>

    // <editor-fold desc="Helpers">
    private WebUser actor() {
        return sessionController.getLoggedUser();
    }

    private RoleOperation parseOperation(String op) {
        if (op == null) {
            return null;
        }
        try {
            return RoleOperation.valueOf(op);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Set<RoleAspect> selectedAspects() {
        Set<RoleAspect> aspects = new LinkedHashSet<>();
        if (aspectPrivileges) {
            aspects.add(RoleAspect.PRIVILEGES);
        }
        if (aspectIcons) {
            aspects.add(RoleAspect.ICONS);
        }
        if (aspectSubscriptions) {
            aspects.add(RoleAspect.SUBSCRIPTIONS);
        }
        if (aspectLoginPage) {
            aspects.add(RoleAspect.LOGIN_PAGE);
        }
        if (aspects.isEmpty()) {
            JsfUtil.addErrorMessage("Select at least one aspect (Privileges, Icons, Subscriptions, Login Page).");
            return null;
        }
        return aspects;
    }

    private String formatPreview(Map<RoleAspect, Long> counts) {
        if (counts == null || counts.isEmpty()) {
            return "No changes";
        }
        StringBuilder sb = new StringBuilder();
        appendAspect(sb, "Privileges", counts.get(RoleAspect.PRIVILEGES));
        appendAspect(sb, "Icons", counts.get(RoleAspect.ICONS));
        appendAspect(sb, "Subscriptions", counts.get(RoleAspect.SUBSCRIPTIONS));
        appendAspect(sb, "Login Page", counts.get(RoleAspect.LOGIN_PAGE));
        return sb.length() == 0 ? "No changes" : sb.toString();
    }

    private void appendAspect(StringBuilder sb, String label, Long count) {
        if (count == null) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("; ");
        }
        sb.append(label).append(": ").append(count).append(" changes");
    }

    /**
     * Pattern reused from UserPrivilageController.fillWebUserDepartments /
     * WebUserRoleUserController.fillWebUserDepartments.
     */
    public List<Department> fillWebUserDepartments(WebUser wu) {
        Set<Department> departmentSet = new HashSet<>();
        String jpql = "SELECT i.department "
                + " FROM WebUserDepartment i "
                + " WHERE i.retired = :ret "
                + " AND i.webUser = :wu "
                + " AND i.department.name is not null "
                + " ORDER BY i.department.name";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("wu", wu);
        List<Department> depts = departmentFacade.findByJpql(jpql, m);
        departmentSet.addAll(depts);
        return new ArrayList<>(departmentSet);
    }
    // </editor-fold>

    // <editor-fold desc="Getters / Setters">
    public WebUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(WebUser currentUser) {
        this.currentUser = currentUser;
    }

    public WebUserRole getSelectedRole() {
        return selectedRole;
    }

    public void setSelectedRole(WebUserRole selectedRole) {
        this.selectedRole = selectedRole;
    }

    public List<WebUserRole> getRoles() {
        if (roles == null) {
            roles = webUserRoleFacade.findByJpql("select r from WebUserRole r where r.retired=false order by r.name");
        }
        return roles;
    }

    public void setRoles(List<WebUserRole> roles) {
        this.roles = roles;
    }

    public List<Department> getUserDepartments() {
        return userDepartments;
    }

    public void setUserDepartments(List<Department> userDepartments) {
        this.userDepartments = userDepartments;
    }

    public List<Department> getSelectedDepartments() {
        return selectedDepartments;
    }

    public void setSelectedDepartments(List<Department> selectedDepartments) {
        this.selectedDepartments = selectedDepartments;
    }

    public boolean isAspectPrivileges() {
        return aspectPrivileges;
    }

    public void setAspectPrivileges(boolean aspectPrivileges) {
        this.aspectPrivileges = aspectPrivileges;
    }

    public boolean isAspectIcons() {
        return aspectIcons;
    }

    public void setAspectIcons(boolean aspectIcons) {
        this.aspectIcons = aspectIcons;
    }

    public boolean isAspectSubscriptions() {
        return aspectSubscriptions;
    }

    public void setAspectSubscriptions(boolean aspectSubscriptions) {
        this.aspectSubscriptions = aspectSubscriptions;
    }

    public boolean isAspectLoginPage() {
        return aspectLoginPage;
    }

    public void setAspectLoginPage(boolean aspectLoginPage) {
        this.aspectLoginPage = aspectLoginPage;
    }

    public boolean isUpdateUserRole() {
        return updateUserRole;
    }

    public void setUpdateUserRole(boolean updateUserRole) {
        this.updateUserRole = updateUserRole;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }

    public List<UserRoleApplicationService.RoleApplicationResult> getResults() {
        return results;
    }

    public void setResults(List<UserRoleApplicationService.RoleApplicationResult> results) {
        this.results = results;
    }

    public WebUserRole getFilterRole() {
        return filterRole;
    }

    public void setFilterRole(WebUserRole filterRole) {
        this.filterRole = filterRole;
    }

    public Department getFilterDepartment() {
        return filterDepartment;
    }

    public void setFilterDepartment(Department filterDepartment) {
        this.filterDepartment = filterDepartment;
    }

    public Institution getFilterInstitution() {
        return filterInstitution;
    }

    public void setFilterInstitution(Institution filterInstitution) {
        this.filterInstitution = filterInstitution;
    }

    public List<Department> getAllDepartments() {
        if (allDepartments == null) {
            // d.name is not null: legacy rows with null names would NPE the checkbox label renderer
            allDepartments = departmentFacade.findByJpql("select d from Department d where d.retired=false and d.name is not null order by d.name");
        }
        return allDepartments;
    }

    public void setAllDepartments(List<Department> allDepartments) {
        this.allDepartments = allDepartments;
    }

    public List<Institution> getInstitutions() {
        if (institutions == null) {
            institutions = institutionFacade.findByJpql("select i from Institution i where i.retired=false and i.name is not null order by i.name");
        }
        return institutions;
    }

    public void setInstitutions(List<Institution> institutions) {
        this.institutions = institutions;
    }

    public List<WebUser> getFilteredUsers() {
        return filteredUsers;
    }

    public void setFilteredUsers(List<WebUser> filteredUsers) {
        this.filteredUsers = filteredUsers;
    }

    public List<WebUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<WebUser> selectedUsers) {
        this.selectedUsers = selectedUsers;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }
    // </editor-fold>

    /**
     * Explicit converter for Department multi-selects. UISelectMany over a
     * List cannot detect the element type (generics erasure), so the
     * forClass Department converter is never applied and submitted values
     * stay Strings — this named converter must be set on the component.
     */
    @FacesConverter("userRoleDepartmentConverter")
    public static class UserRoleDepartmentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            UserRoleManagementController controller = (UserRoleManagementController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userRoleManagementController");
            try {
                return controller.getDepartmentFacade().find(Long.valueOf(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Department) {
                Department d = (Department) object;
                return d.getId() == null ? null : String.valueOf(d.getId());
            }
            return null;
        }
    }

}

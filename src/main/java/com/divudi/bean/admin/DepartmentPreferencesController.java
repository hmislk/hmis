package com.divudi.bean.admin;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.Department;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Session-scoped controller for managing department-level preferences backed
 * by department-scoped ConfigOption entries (OptionScope.DEPARTMENT).
 *
 * Starts with a single preference: "Pharmacy - Allow Issue to Same Department".
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class DepartmentPreferencesController implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KEY_ALLOW_PHARMACY_ISSUE_TO_SAME_DEPARTMENT = "Pharmacy - Allow Issue to Same Department";
    private static final boolean DEFAULT_ALLOW_PHARMACY_ISSUE_TO_SAME_DEPARTMENT = false;

    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    private boolean allowPharmacyIssueToSameDepartment;

    public DepartmentPreferencesController() {
    }

    @PostConstruct
    public void init() {
        loadPreferences();
    }

    /**
     * Loads department-level preferences for the logged-in user's current department.
     */
    public void loadPreferences() {
        Department department = getLoggedUserDepartment();
        allowPharmacyIssueToSameDepartment = configOptionApplicationController.getBooleanValueByKeyForDepartment(
                KEY_ALLOW_PHARMACY_ISSUE_TO_SAME_DEPARTMENT, department, DEFAULT_ALLOW_PHARMACY_ISSUE_TO_SAME_DEPARTMENT);
    }

    /**
     * Persists department-level preferences for the logged-in user's current department.
     * Requires the Admin privilege.
     *
     * @return null (stay on the same page)
     */
    public String savePreferences() {
        if (!webUserController.hasPrivilege("Admin")) {
            JsfUtil.addErrorMessage("You do not have permission to change department preferences.");
            return null;
        }
        Department department = getLoggedUserDepartment();
        if (department == null) {
            JsfUtil.addErrorMessage("No department selected. Please select a department first.");
            return null;
        }
        configOptionApplicationController.setBooleanValueByKeyForDepartment(
                KEY_ALLOW_PHARMACY_ISSUE_TO_SAME_DEPARTMENT, department, allowPharmacyIssueToSameDepartment);
        JsfUtil.addSuccessMessage("Department preferences saved.");
        return null;
    }

    /**
     * Resets department-level preferences to system defaults for the logged-in
     * user's current department. Requires the Admin privilege.
     *
     * @return null (stay on the same page)
     */
    public String resetToDefaults() {
        if (!webUserController.hasPrivilege("Admin")) {
            JsfUtil.addErrorMessage("You do not have permission to change department preferences.");
            return null;
        }
        Department department = getLoggedUserDepartment();
        if (department == null) {
            JsfUtil.addErrorMessage("No department selected. Please select a department first.");
            return null;
        }
        allowPharmacyIssueToSameDepartment = DEFAULT_ALLOW_PHARMACY_ISSUE_TO_SAME_DEPARTMENT;
        configOptionApplicationController.setBooleanValueByKeyForDepartment(
                KEY_ALLOW_PHARMACY_ISSUE_TO_SAME_DEPARTMENT, department, allowPharmacyIssueToSameDepartment);
        JsfUtil.addSuccessMessage("Department preferences reset to defaults.");
        return null;
    }

    public Department getLoggedUserDepartment() {
        return sessionController.getDepartment();
    }

    public String getLoggedUserDepartmentName() {
        Department department = getLoggedUserDepartment();
        return department == null ? "" : department.getName();
    }

    public boolean isAllowPharmacyIssueToSameDepartment() {
        return allowPharmacyIssueToSameDepartment;
    }

    public void setAllowPharmacyIssueToSameDepartment(boolean allowPharmacyIssueToSameDepartment) {
        this.allowPharmacyIssueToSameDepartment = allowPharmacyIssueToSameDepartment;
    }

}

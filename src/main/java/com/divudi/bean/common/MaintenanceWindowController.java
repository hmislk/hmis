package com.divudi.bean.common;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.MaintenanceWindow;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.MaintenanceWindowException;
import com.divudi.service.MaintenanceWindowRules;
import com.divudi.service.MaintenanceWindowService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Admin page to schedule and cancel maintenance windows (#25838).
 * Every action checks the ManageMaintenanceWindows privilege on the server.
 */
@Named
@ViewScoped
public class MaintenanceWindowController implements Serializable {

    private static final long serialVersionUID = 1L;
    static final String PRIVILEGE = "ManageMaintenanceWindows";

    @EJB
    private MaintenanceWindowService maintenanceWindowService;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @Inject
    private WebUserController webUserController;
    @Inject
    private SessionController sessionController;
    @Inject
    private MaintenanceBannerCache maintenanceBannerCache;

    private String title;
    private String message;
    private Date startAt;
    private Date endAt;
    private Date showFromAt;
    private String reference;
    private List<Long> institutionIds = new ArrayList<>();
    private List<Long> departmentIds = new ArrayList<>();
    private List<MaintenanceWindow> windows;
    private List<Institution> institutions;
    private List<Department> departments;

    public String navigateToMaintenanceWindows() {
        windows = null;
        return "/admin/maintenance_windows?faces-redirect=true";
    }

    public boolean isAllowed() {
        return webUserController.hasPrivilege(PRIVILEGE);
    }

    /** Called when the start time changes: the banner defaults to 24 hours before. */
    public void startChanged() {
        showFromAt = MaintenanceWindowRules.defaultShowFrom(startAt);
    }

    public void save() {
        if (!isAllowed()) {
            JsfUtil.addErrorMessage("You are not authorized to manage maintenance windows.");
            return;
        }
        MaintenanceWindow w = new MaintenanceWindow();
        w.setTitle(title);
        w.setMessage(message);
        w.setStartAt(startAt);
        w.setEndAt(endAt);
        w.setShowFromAt(showFromAt);
        w.setExternalReference(reference);
        try {
            maintenanceWindowService.create(w, institutionIds, departmentIds,
                    sessionController.getLoggedUser(), "admin page");
        } catch (MaintenanceWindowException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return;
        }
        maintenanceBannerCache.invalidate();
        JsfUtil.addSuccessMessage("Maintenance window saved.");
        clearForm();
        windows = null;
    }

    public void cancel(MaintenanceWindow w) {
        if (!isAllowed()) {
            JsfUtil.addErrorMessage("You are not authorized to manage maintenance windows.");
            return;
        }
        if (w == null) {
            return;
        }
        try {
            maintenanceWindowService.cancel(w.getId(), sessionController.getLoggedUser());
        } catch (MaintenanceWindowException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return;
        }
        maintenanceBannerCache.invalidate();
        JsfUtil.addSuccessMessage("Maintenance window cancelled.");
        windows = null;
    }

    private void clearForm() {
        title = null;
        message = null;
        startAt = null;
        endAt = null;
        showFromAt = null;
        reference = null;
        institutionIds = new ArrayList<>();
        departmentIds = new ArrayList<>();
    }

    public List<MaintenanceWindow> getWindows() {
        if (windows == null) {
            windows = isAllowed() ? maintenanceWindowService.listUpcomingAndActive(new Date()) : new ArrayList<>();
        }
        return windows;
    }

    public List<Institution> getInstitutions() {
        if (institutions == null) {
            institutions = institutionFacade.findByJpql(
                    "select i from Institution i where i.retired=false order by i.name");
        }
        return institutions;
    }

    public List<Department> getDepartments() {
        if (departments == null) {
            departments = departmentFacade.findByJpql(
                    "select d from Department d where d.retired=false order by d.name");
        }
        return departments;
    }

    /** Short text of who a window affects, for the list. */
    public String audience(MaintenanceWindow w) {
        if (w.getInstitutions().isEmpty() && w.getDepartments().isEmpty()) {
            return "Everyone";
        }
        List<String> names = new ArrayList<>();
        for (Institution i : w.getInstitutions()) {
            names.add(i.getName());
        }
        for (Department d : w.getDepartments()) {
            names.add(d.getName());
        }
        return String.join(", ", names);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStartAt() {
        return startAt;
    }

    public void setStartAt(Date startAt) {
        this.startAt = startAt;
    }

    public Date getEndAt() {
        return endAt;
    }

    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }

    public Date getShowFromAt() {
        return showFromAt;
    }

    public void setShowFromAt(Date showFromAt) {
        this.showFromAt = showFromAt;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public List<Long> getInstitutionIds() {
        return institutionIds;
    }

    public void setInstitutionIds(List<Long> institutionIds) {
        this.institutionIds = institutionIds;
    }

    public List<Long> getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(List<Long> departmentIds) {
        this.departmentIds = departmentIds;
    }
}

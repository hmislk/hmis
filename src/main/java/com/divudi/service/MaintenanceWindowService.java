package com.divudi.service;

import com.divudi.core.data.Privileges;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.MaintenanceWindow;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.MaintenanceWindowFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Creates, cancels and lists maintenance windows (#25838). Callers check the
 * ManageMaintenanceWindows privilege before create and cancel.
 */
@Stateless
public class MaintenanceWindowService {

    @EJB
    private MaintenanceWindowFacade facade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;

    /**
     * Validates and saves a new window. Institution and department ids that do
     * not exist are rejected rather than ignored.
     */
    public MaintenanceWindow create(MaintenanceWindow window, List<Long> institutionIds,
            List<Long> departmentIds, WebUser createdBy, String source) {
        MaintenanceWindowRules.validate(window);
        window.getInstitutions().clear();
        if (institutionIds != null) {
            for (Long id : institutionIds) {
                Institution i = id == null ? null : institutionFacade.find(id);
                if (i == null || i.isRetired()) {
                    throw new MaintenanceWindowException("Institution not found: " + id);
                }
                window.getInstitutions().add(i);
            }
        }
        window.getDepartments().clear();
        if (departmentIds != null) {
            for (Long id : departmentIds) {
                Department d = id == null ? null : departmentFacade.find(id);
                if (d == null || d.isRetired()) {
                    throw new MaintenanceWindowException("Department not found: " + id);
                }
                window.getDepartments().add(d);
            }
        }
        window.setCreatedBy(createdBy);
        window.setCreatedAt(new Date());
        window.setSource(source);
        window.setRetired(false);
        facade.create(window);
        return window;
    }

    /** Soft-retires a window. */
    public MaintenanceWindow cancel(Long id, WebUser retirer) {
        MaintenanceWindow w = id == null ? null : facade.find(id);
        if (w == null || w.isRetired()) {
            throw new MaintenanceWindowException("Maintenance window not found", 404);
        }
        w.setRetired(true);
        w.setRetiredAt(new Date());
        w.setRetirer(retirer);
        facade.edit(w);
        return w;
    }

    /** Active and upcoming windows (not retired, not yet ended), soonest start first. */
    public List<MaintenanceWindow> listUpcomingAndActive(Date now) {
        Map<String, Object> m = new HashMap<>();
        m.put("now", now);
        return facade.findByJpql("select w from MaintenanceWindow w"
                + " where w.retired=false and w.endAt>:now order by w.startAt", m, TemporalType.TIMESTAMP);
    }

    /** Windows whose banner is showing now for this institution and department. */
    public List<MaintenanceWindow> findVisibleFor(Institution institution, Department department, Date now) {
        return MaintenanceWindowRules.filterVisible(listUpcomingAndActive(now), institution, department, now);
    }

    /** True when the user holds the privilege in any department (used by the API, which has no session). */
    public boolean userMayManage(WebUser user) {
        if (user == null) {
            return false;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("u", user);
        m.put("p", Privileges.ManageMaintenanceWindows);
        long n = webUserPrivilegeFacade.findLongByJpql("select count(w) from WebUserPrivilege w"
                + " where w.webUser=:u and w.privilege=:p and w.retired=false", m);
        return n > 0;
    }
}

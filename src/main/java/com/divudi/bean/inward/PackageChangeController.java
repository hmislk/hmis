package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Changes the {@link InpatientPackage} recorded on an existing admission and
 * logs an {@code AuditEvent} for the change. Deliberately does not touch
 * billing (no {@code InpatientPackageApplicationBean} calls) — see
 * developer_docs/specs/2026-09-29-package-admission-menu-and-change-screen-design.md.
 */
@Named
@SessionScoped
public class PackageChangeController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @EJB
    private AuditService auditService;
    @EJB
    private AdmissionFacade ejbFacade;

    private Admission current;
    private InpatientPackage newPackage;

    public Map<String, Object> packageStateMap(InpatientPackage p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("package", p != null ? p.getName() : null);
        m.put("admissionType", p != null && p.getAdmissionType() != null ? p.getAdmissionType().getName() : null);
        m.put("roomCategory", p != null && p.getRoomCategory() != null ? p.getRoomCategory().getName() : null);
        m.put("totalPrice", p != null ? p.getTotalPrice() : null);
        return m;
    }

    public void change() {
        if (current == null) {
            JsfUtil.addErrorMessage("No admission selected.");
            return;
        }
        Map<String, Object> beforeState = packageStateMap(current.getInpatientPackage());
        current.setInpatientPackage(newPackage);
        ejbFacade.edit(current);
        Map<String, Object> afterState = packageStateMap(current.getInpatientPackage());
        auditService.logEncounterAudit(current, "Package Changed", beforeState, afterState,
                sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage("Package updated successfully");
        newPackage = null;
    }

    public void removePackage() {
        newPackage = null;
        change();
    }

    public Admission getCurrent() {
        return current;
    }

    public void setCurrent(Admission current) {
        this.current = current;
    }

    public InpatientPackage getNewPackage() {
        return newPackage;
    }

    public void setNewPackage(InpatientPackage newPackage) {
        this.newPackage = newPackage;
    }
}

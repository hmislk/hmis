/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.AuditEventController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.VmppDto;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.facade.VmppFacade;
import com.divudi.service.AuditService;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class VmppController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VmppFacade ejbFacade;
    @Inject
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;

    private Vmpp current;
    private boolean editable;

    // DTO Management Fields
    private VmppDto selectedVmppDto;
    private List<VmppDto> vmppDtos;
    private List<AuditEvent> vmppAuditEvents;

    // Filter state for active/inactive VMPPs
    private String filterStatus = "active"; // "active", "inactive", "all"

    @Deprecated
    public String navigateToListAllVmpps() {
        return "/emr/reports/vmpps?faces-redirect=true";
    }

    public List<Vmpp> completeVmpp(String query) {
        List<Vmpp> vmppList;
        if (query == null || query.trim().isEmpty()) {
            vmppList = new ArrayList<>();
        } else {
            String jpql = "SELECT c FROM Vmpp c WHERE c.retired = false "
                    + "AND LOWER(c.name) LIKE :query "
                    + "AND c.departmentType=:dep ORDER BY c.name";
            Map<String, Object> m = new HashMap<>();
            m.put("query", "%" + query.trim().toLowerCase() + "%");
            m.put("dep", DepartmentType.Pharmacy);
            vmppList = getFacade().findByJpqlWithoutCache(jpql, m);
        }
        return vmppList;
    }

    private void saveVmpp() {
        if (current.getName() == null || current.getName().isEmpty()) {
            JsfUtil.addErrorMessage("No Name");
            return;
        }

        if (current.getId() == null || current.getId() == 0) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }

    }

    public void prepareAdd() {
        current = new Vmpp();
        current.setDepartmentType(DepartmentType.Pharmacy);
        selectedVmppDto = null;
        vmppAuditEvents = null;
        editable = true;
    }

    public void edit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to edit");
            return;
        }
        if (current.getId() != null) {
            selectedVmppDto = createVmppDto(current);
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedVmppDto = null;
        vmppAuditEvents = null;
        editable = false;
    }

    private void recreateModel() {
        vmppDtos = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            // UPDATE - capture before state for audit
            Vmpp beforeUpdate = getFacade().find(getCurrent().getId());
            Map<String, Object> beforeData = createAuditMap(beforeUpdate);

            getCurrent().setDepartmentType(DepartmentType.Pharmacy);
            getFacade().edit(getCurrent());

            // Log audit for update
            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(beforeData, afterData,
                    getSessionController().getLoggedUser(),
                    "Vmpp", "Update VMPP", getCurrent().getId());

            selectedVmppDto = createVmppDto(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            // CREATE - no before state
            getCurrent().setDepartmentType(DepartmentType.Pharmacy);
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());

            // Log audit for create
            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(null, afterData,
                    getSessionController().getLoggedUser(),
                    "Vmpp", "Create VMPP", getCurrent().getId());

            selectedVmppDto = createVmppDto(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully.");
        }
        recreateModel();
        editable = false;
    }

    public void save() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            // UPDATE - capture before state
            Vmpp beforeUpdate = getFacade().find(getCurrent().getId());
            Map<String, Object> beforeData = createAuditMap(beforeUpdate);

            getCurrent().setDepartmentType(DepartmentType.Pharmacy);
            getFacade().edit(getCurrent());

            // Log audit for update
            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(beforeData, afterData,
                    getSessionController().getLoggedUser(),
                    "Vmpp", "Update VMPP", getCurrent().getId());

            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            // CREATE - no before state
            getCurrent().setDepartmentType(DepartmentType.Pharmacy);
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());

            // Log audit for create
            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(null, afterData,
                    getSessionController().getLoggedUser(),
                    "Vmpp", "Create VMPP", getCurrent().getId());

            JsfUtil.addSuccessMessage("Saved Successfully.");
        }
        recreateModel();
    }

    private Map<String, Object> createAuditMap(Vmpp vmpp) {
        Map<String, Object> auditData = new HashMap<>();
        if (vmpp != null) {
            auditData.put("id", vmpp.getId());
            auditData.put("name", vmpp.getName());
            auditData.put("code", vmpp.getCode());
            auditData.put("retired", vmpp.isRetired());
            auditData.put("inactive", vmpp.isInactive());
            auditData.put("departmentType", vmpp.getDepartmentType() != null
                    ? vmpp.getDepartmentType().toString() : null);
            auditData.put("descreption", vmpp.getDescreption());
            auditData.put("vmpId", vmpp.getVmp() != null ? vmpp.getVmp().getId() : null);
            auditData.put("vmpName", vmpp.getVmp() != null ? vmpp.getVmp().getName() : null);
        }
        return auditData;
    }

    public VmppFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(VmppFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Vmpp getCurrent() {
        if (current == null) {
            current = new Vmpp();
        }
        return current;
    }

    public void setCurrent(Vmpp current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            // Capture before state for audit
            Map<String, Object> beforeData = createAuditMap(current);

            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);

            // Capture after state for audit
            Map<String, Object> afterData = createAuditMap(current);
            auditService.logAudit(beforeData, afterData,
                    getSessionController().getLoggedUser(),
                    "Vmpp", "Delete VMPP", current.getId());

            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }
        recreateModel();
        current = null;
        selectedVmppDto = null;
        vmppAuditEvents = null;
        getCurrent();
        editable = false;
    }

    private VmppFacade getFacade() {
        return ejbFacade;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // ========================== DTO Management Methods ==========================

    public List<VmppDto> getVmppDtos() {
        String jpql = "SELECT new com.divudi.core.data.dto.VmppDto("
                + "v.id, v.name, v.code, v.retired, v.inactive) "
                + "FROM Vmpp v WHERE v.departmentType=:dep AND v.retired=:retired ";

        Map<String, Object> params = new HashMap<>();
        params.put("dep", DepartmentType.Pharmacy);
        params.put("retired", false);

        if ("active".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", true);
        }

        jpql += "ORDER BY v.name";

        return (List<VmppDto>) getFacade().findLightsByJpql(jpql, params);
    }

    public List<VmppDto> completeVmppDto(String query) {
        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }

        String jpql = "SELECT new com.divudi.core.data.dto.VmppDto("
                + "v.id, v.name, v.code, v.retired, v.inactive) "
                + "FROM Vmpp v WHERE LOWER(v.name) LIKE :query "
                + "AND v.departmentType=:dep AND v.retired=:retired ";

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toLowerCase() + "%");
        params.put("dep", DepartmentType.Pharmacy);
        params.put("retired", false);

        if ("active".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", true);
        }

        jpql += "ORDER BY v.name";

        return (List<VmppDto>) getFacade().findLightsByJpql(jpql, params);
    }

    public VmppDto getSelectedVmppDto() {
        return selectedVmppDto;
    }

    public void setSelectedVmppDto(VmppDto selectedVmppDto) {
        this.selectedVmppDto = selectedVmppDto;
        if (selectedVmppDto != null && selectedVmppDto.getId() != null) {
            this.current = getFacade().find(selectedVmppDto.getId());
        } else {
            this.current = null;
        }
    }

    public VmppDto createVmppDto(Vmpp vmpp) {
        if (vmpp == null) {
            return null;
        }
        return new VmppDto(vmpp.getId(), vmpp.getName(), vmpp.getCode(),
                vmpp.isRetired(), vmpp.isInactive());
    }

    // ========================== Audit History Methods ==========================

    public void fillVmppAuditEvents() {
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId "
                        + "AND a.entityType = :entityType "
                        + "ORDER BY a.eventDataTime DESC";

                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "Vmpp");

                vmppAuditEvents = auditEventFacade.findByJpql(jpql, params);
            } catch (Exception e) {
                vmppAuditEvents = new ArrayList<>();
            }
        } else {
            vmppAuditEvents = new ArrayList<>();
        }
    }

    public String navigateToVmppAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMPP first");
            return null;
        }
        fillVmppAuditEvents();
        return "/pharmacy/admin/vmpp_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getVmppAuditEvents() {
        if (vmppAuditEvents == null) {
            fillVmppAuditEvents();
        }
        return vmppAuditEvents;
    }

    public void setVmppAuditEvents(List<AuditEvent> vmppAuditEvents) {
        this.vmppAuditEvents = vmppAuditEvents;
    }

    // ========================== Filter Status Management ==========================

    public String getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(String filterStatus) {
        this.filterStatus = filterStatus;
    }

    public void setFilterToActive() {
        filterStatus = "active";
        refreshData();
    }

    public void setFilterToInactive() {
        filterStatus = "inactive";
        refreshData();
    }

    public void setFilterToAll() {
        filterStatus = "all";
        refreshData();
    }

    public void refreshData() {
        recreateModel();

        if (current != null) {
            boolean shouldKeepSelection = false;
            switch (filterStatus) {
                case "active":
                    shouldKeepSelection = !current.isInactive();
                    break;
                case "inactive":
                    shouldKeepSelection = current.isInactive();
                    break;
                case "all":
                    shouldKeepSelection = true;
                    break;
            }

            if (!shouldKeepSelection) {
                current = null;
                selectedVmppDto = null;
                vmppAuditEvents = null;
            }
        }
    }

    public boolean isShowingActive() {
        return "active".equals(filterStatus);
    }

    public boolean isShowingInactive() {
        return "inactive".equals(filterStatus);
    }

    public boolean isShowingAll() {
        return "all".equals(filterStatus);
    }

    public String getFilterStatusDisplay() {
        switch (filterStatus) {
            case "active":
                return "Active VMPPs";
            case "inactive":
                return "Inactive VMPPs";
            case "all":
                return "All VMPPs";
            default:
                return "Active VMPPs";
        }
    }

    // ========================== Toggle Status ==========================

    public void toggleVmppStatus() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMPP to change status");
            return;
        }

        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("VMPP Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("VMPP Deactivated Successfully");
        }

        getFacade().edit(current);

        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate VMPP" : "Deactivate VMPP";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "Vmpp", action, current.getId());

        if (selectedVmppDto != null) {
            selectedVmppDto.setInactive(current.isInactive());
        }

        recreateModel();
    }

    public String getToggleStatusButtonText() {
        if (current == null || current.getId() == null) {
            return "Toggle Status";
        }
        return current.isInactive() ? "Activate" : "Deactivate";
    }

    public String getToggleStatusButtonIcon() {
        if (current == null || current.getId() == null) {
            return "fas fa-toggle-off";
        }
        return current.isInactive() ? "fas fa-check-circle" : "fas fa-times-circle";
    }

    public String getToggleStatusButtonClass() {
        if (current == null || current.getId() == null) {
            return "ui-button-secondary";
        }
        return current.isInactive() ? "ui-button-success" : "ui-button-warning";
    }

    // ========================== Converters ==========================

    /**
     *
     */
    @FacesConverter(forClass = Vmpp.class)
    public static class VmppControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            VmppController controller = (VmppController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vmppController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            long key;
            key = Long.parseLong(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            return String.valueOf(value);
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Vmpp) {
                Vmpp o = (Vmpp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Vmpp.class.getName());
            }
        }
    }

    /**
     * JSF converter for VMPP DTO
     */
    @FacesConverter("vmppDtoConverter")
    public static class VmppDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                VmppController controller = (VmppController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "vmppController");

                Vmpp entity = controller.getFacade().find(id);
                return entity != null ? controller.createVmppDto(entity) : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof VmppDto) {
                VmppDto dto = (VmppDto) object;
                return dto.getId() != null ? dto.getId().toString() : null;
            }
            throw new IllegalArgumentException("Expected VmppDto object");
        }
    }
}

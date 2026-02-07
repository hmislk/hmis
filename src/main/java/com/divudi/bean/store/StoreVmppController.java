/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.store;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.AuditEventController;
import com.divudi.service.AuditService;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.VmppDto;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.facade.VmppFacade;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.util.JsfUtil;

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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class StoreVmppController implements Serializable {

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
    private List<Vmpp> items;

    // DTO properties
    private VmppDto selectedVmppDto;
    private List<VmppDto> vmppDtos;

    // Audit properties
    private List<AuditEvent> vmppAuditEvents;

    private boolean editable;
    private Map<String, Object> beforeEditData;

    // Filter state for active/inactive VMPPs
    private String filterStatus = "active";

    public List<Vmpp> completeVmpp(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "SELECT c FROM Vmpp c WHERE c.retired = false "
                + "AND LOWER(c.name) LIKE :query "
                + "AND c.departmentType=:dep "
                + "ORDER BY c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.trim().toLowerCase() + "%");
        params.put("dep", DepartmentType.Store);
        return getFacade().findByJpqlWithoutCache(jpql, params);
    }

    public void prepareAdd() {
        current = new Vmpp();
        current.setDepartmentType(DepartmentType.Store);
        selectedVmppDto = null;
        vmppAuditEvents = null;
        beforeEditData = null;
        editable = true;
    }

    public void edit() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMPP to edit");
            return;
        }
        if (current.isInactive()) {
            JsfUtil.addWarningMessage("Editing inactive VMPP '" + current.getName() + "'");
        }
        if (current.getId() != null) {
            selectedVmppDto = createVmppDto(current);
        }
        beforeEditData = createAuditMap(current);
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedVmppDto = null;
        vmppAuditEvents = null;
        beforeEditData = null;
        editable = false;
    }

    public void save() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }

        try {
            if (getCurrent().getId() != null && getCurrent().getId() > 0) {
                // UPDATE
                getCurrent().setDepartmentType(DepartmentType.Store);
                getFacade().edit(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeEditData, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreVmpp", "Update Store VMPP", getCurrent().getId());

                JsfUtil.addSuccessMessage("Updated Successfully.");
            } else {
                // CREATE
                getCurrent().setDepartmentType(DepartmentType.Store);
                getCurrent().setCreatedAt(new Date());
                getCurrent().setCreater(getSessionController().getLoggedUser());
                getFacade().create(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(null, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreVmpp", "Create Store VMPP", getCurrent().getId());

                JsfUtil.addSuccessMessage("Saved Successfully.");
            }
            editable = false;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Store VMPP: " + e.getMessage());
            e.printStackTrace();
        }

        items = null;
        vmppDtos = null;
        getItems();
    }

    public void delete() {
        if (current != null) {
            try {
                Map<String, Object> beforeData = createAuditMap(current);

                current.setRetired(true);
                current.setRetiredAt(new Date());
                current.setRetirer(getSessionController().getLoggedUser());
                getFacade().edit(current);

                Map<String, Object> afterData = createAuditMap(current);
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreVmpp", "Delete Store VMPP", current.getId());

                JsfUtil.addSuccessMessage("Deleted Successfully");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error deleting Store VMPP: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }

        current = null;
        selectedVmppDto = null;
        vmppAuditEvents = null;
        items = null;
        vmppDtos = null;
        getItems();
        getCurrent();
        editable = false;
    }

    // ========== Getters/Setters ==========

    public StoreVmppController() {
    }

    private VmppFacade getFacade() {
        return ejbFacade;
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
            current.setDepartmentType(DepartmentType.Store);
        }
        return current;
    }

    public void setCurrent(Vmpp current) {
        this.current = current;
    }

    public List<Vmpp> getItems() {
        if (items == null) {
            String jpql = "select v from Vmpp v "
                    + "where v.departmentType=:dep "
                    + "and v.retired=:retired ";
            Map<String, Object> params = new HashMap<>();
            params.put("dep", DepartmentType.Store);
            params.put("retired", false);

            if ("active".equals(filterStatus)) {
                jpql += "and v.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "and v.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "order by v.name";
            items = getFacade().findByJpql(jpql, params);
        }
        return items;
    }

    public void setItems(List<Vmpp> items) {
        this.items = items;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // ===================== DTO Methods =====================

    public List<VmppDto> getVmppDtos() {
        if (vmppDtos == null) {
            String jpql = "SELECT new com.divudi.core.data.dto.VmppDto("
                    + "v.id, v.name, v.code, v.retired, v.inactive) "
                    + "FROM Vmpp v WHERE v.departmentType=:dep AND v.retired=:retired ";

            Map<String, Object> params = new HashMap<>();
            params.put("dep", DepartmentType.Store);
            params.put("retired", false);

            if ("active".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "ORDER BY v.name";
            vmppDtos = (List<VmppDto>) getFacade().findLightsByJpql(jpql, params);
        }
        return vmppDtos;
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
        params.put("dep", DepartmentType.Store);
        params.put("retired", false);

        if ("active".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", true);
        }

        jpql += "ORDER BY v.name";

        try {
            return (List<VmppDto>) getFacade().findLightsByJpql(jpql, params);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
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

    // ===================== Filter Methods =====================

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
        items = null;
        vmppDtos = null;

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
                return "Active Store VMPPs";
            case "inactive":
                return "Inactive Store VMPPs";
            case "all":
                return "All Store VMPPs";
            default:
                return "Active Store VMPPs";
        }
    }

    // ===================== Toggle Status Methods =====================

    public void toggleVmppStatus() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMPP to change status");
            return;
        }

        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("Store VMPP Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("Store VMPP Deactivated Successfully");
        }

        getFacade().edit(current);

        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate Store VMPP" : "Deactivate Store VMPP";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "StoreVmpp", action, current.getId());

        if (selectedVmppDto != null) {
            selectedVmppDto.setInactive(current.isInactive());
        }

        items = null;
        vmppDtos = null;
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

    // ===================== Audit Methods =====================

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

    public void fillVmppAuditEvents() {
        vmppAuditEvents = new ArrayList<>();
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId "
                        + "AND a.entityType = :entityType ORDER BY a.eventDataTime DESC";
                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "StoreVmpp");

                vmppAuditEvents = auditEventFacade.findByJpql(jpql, params);
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error loading audit history: " + e.getMessage());
            }
        }
    }

    public String navigateToVmppAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMPP first");
            return null;
        }
        fillVmppAuditEvents();
        return "/pharmacy/admin/store_vmpp_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getVmppAuditEvents() {
        return vmppAuditEvents;
    }

    public void setVmppAuditEvents(List<AuditEvent> vmppAuditEvents) {
        this.vmppAuditEvents = vmppAuditEvents;
    }

    // ===================== JSF Converters =====================

    @FacesConverter("storeVmppDtoConverter")
    public static class StoreVmppDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                StoreVmppController controller = (StoreVmppController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "storeVmppController");

                if (controller == null) {
                    return null;
                }

                Vmpp entity = controller.getFacade().find(id);
                if (entity != null) {
                    return controller.createVmppDto(entity);
                }
                return null;
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

    @FacesConverter("stoVmppCon")
    public static class VmppControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                StoreVmppController controller = (StoreVmppController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "storeVmppController");

                if (controller == null) {
                    return null;
                }

                return controller.getEjbFacade().find(id);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Vmpp) {
                Vmpp o = (Vmpp) object;
                return String.valueOf(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Vmpp.class.getName());
            }
        }
    }
}

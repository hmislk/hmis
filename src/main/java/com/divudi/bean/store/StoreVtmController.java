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
import com.divudi.core.data.dto.VtmDto;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.facade.VtmFacade;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class StoreVtmController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VtmFacade ejbFacade;
    @EJB
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;

    private Vtm current;
    private List<Vtm> items;

    // DTO properties
    private List<VtmDto> vtmDtoList;
    private VtmDto selectedVtmDto;

    // Audit properties
    private List<AuditEvent> vtmAuditEvents;

    private boolean editable;

    // Filter state for active/inactive VTMs
    private String filterStatus = "active";

    public List<Vtm> completeVtm(String query) {
        if (query == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                + "AND UPPER(c.name) LIKE :query "
                + "AND c.departmentType=:dep "
                + "ORDER BY c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("query", "%" + query.toUpperCase() + "%");
        params.put("dep", DepartmentType.Store);
        return getFacade().findByJpql(sql, params);
    }

    public void prepareAdd() {
        current = new Vtm();
        current.setDepartmentType(DepartmentType.Store);
        selectedVtmDto = null;
        editable = true;
    }

    public void edit() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VTM to edit");
            return;
        }
        if (current.isRetired()) {
            JsfUtil.addWarningMessage("Editing inactive VTM '" + current.getName() + "'");
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedVtmDto = null;
        editable = false;
    }

    public void save() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }

        if (!validateVtm()) {
            return;
        }

        try {
            boolean isNewVtm = getCurrent().getId() == null;

            if (getCurrent().getId() != null) {
                // UPDATE
                Vtm beforeUpdate = getFacade().find(getCurrent().getId());
                Map<String, Object> beforeData = createAuditMap(beforeUpdate);

                getFacade().edit(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreVtm", "Update Store VTM", getCurrent().getId());

                JsfUtil.addSuccessMessage("Store VTM '" + getCurrent().getName() + "' updated successfully");
            } else {
                // CREATE
                getCurrent().setCreatedAt(new Date());
                getCurrent().setCreater(getSessionController().getLoggedUser());
                getFacade().create(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(null, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreVtm", "Create Store VTM", getCurrent().getId());

                JsfUtil.addSuccessMessage("Store VTM '" + getCurrent().getName() + "' created successfully");

                if (isNewVtm && getCurrent().getId() != null) {
                    selectedVtmDto = createVtmDto(getCurrent());
                }
            }
            editable = false;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Store VTM '" + (getCurrent().getName() != null ? getCurrent().getName() : "Unknown") + "': " + e.getMessage());
            e.printStackTrace();
        }

        items = null;
        clearDtoCache();
        getItems();
    }

    public void delete() {
        if (current != null) {
            String vtmName = current.getName();
            try {
                Map<String, Object> beforeData = createAuditMap(current);

                current.setRetired(true);
                current.setRetiredAt(new Date());
                current.setRetirer(getSessionController().getLoggedUser());
                getFacade().edit(current);

                Map<String, Object> afterData = createAuditMap(current);
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreVtm", "Delete Store VTM", current.getId());

                JsfUtil.addSuccessMessage("Store VTM '" + (vtmName != null ? vtmName : "Unknown") + "' deleted successfully");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error deleting Store VTM '" + (vtmName != null ? vtmName : "Unknown") + "': " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            JsfUtil.addErrorMessage("No VTM selected for deletion");
        }
        current = null;
        selectedVtmDto = null;
        items = null;
        clearDtoCache();
        getItems();
        getCurrent();
        editable = false;
    }

    // ========== Getters/Setters ==========

    public StoreVtmController() {
    }

    private VtmFacade getFacade() {
        return ejbFacade;
    }

    public VtmFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(VtmFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Vtm getCurrent() {
        if (current == null) {
            current = new Vtm();
            current.setDepartmentType(DepartmentType.Store);
        }
        return current;
    }

    public void setCurrent(Vtm current) {
        this.current = current;
    }

    public List<Vtm> getItems() {
        if (items == null) {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                    + "AND c.departmentType=:dep ";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Store);

            if ("active".equals(filterStatus)) {
                sql += "AND c.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                sql += "AND c.inactive=:inactive ";
                params.put("inactive", true);
            }

            sql += "ORDER BY c.name";
            items = getFacade().findByJpql(sql, params);
        }
        return items;
    }

    public void setItems(List<Vtm> items) {
        this.items = items;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // ===================== DTO Methods =====================

    public List<VtmDto> getVtmDtos() {
        if (vtmDtoList == null) {
            String jpql = "SELECT new com.divudi.core.data.dto.VtmDto("
                    + "v.id, "
                    + "v.name, "
                    + "v.code, "
                    + "v.descreption, "
                    + "v.instructions, "
                    + "v.retired, "
                    + "v.inactive, "
                    + "COALESCE(CAST(v.departmentType AS string), 'Store')) "
                    + "FROM Vtm v WHERE v.retired=:retired "
                    + "AND v.departmentType=:dep ";

            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Store);

            if ("active".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "ORDER BY v.name";

            vtmDtoList = (List<VtmDto>) getFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        }
        return vtmDtoList;
    }

    public List<VtmDto> completeVtmDto(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT new com.divudi.core.data.dto.VtmDto("
                + "v.id, "
                + "v.name, "
                + "v.code, "
                + "v.descreption, "
                + "v.instructions, "
                + "v.retired, "
                + "v.inactive) "
                + "FROM Vtm v WHERE v.retired=:retired "
                + "AND v.name LIKE :query "
                + "AND v.departmentType=:dep ";

        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("query", "%" + query + "%");
        params.put("dep", DepartmentType.Store);

        if ("active".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", true);
        }

        jpql += "ORDER BY v.name";

        try {
            return (List<VtmDto>) getFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void clearDtoCache() {
        vtmDtoList = null;
    }

    public VtmDto createVtmDto(Vtm vtm) {
        if (vtm == null) {
            return null;
        }
        return new VtmDto(
                vtm.getId(),
                vtm.getName(),
                vtm.getCode(),
                vtm.getDescreption(),
                vtm.getInstructions(),
                vtm.isRetired(),
                vtm.isInactive()
        );
    }

    public List<VtmDto> getVtmDtoList() {
        return getVtmDtos();
    }

    public void setVtmDtoList(List<VtmDto> vtmDtoList) {
        this.vtmDtoList = vtmDtoList;
    }

    public VtmDto getSelectedVtmDto() {
        return selectedVtmDto;
    }

    public void setSelectedVtmDto(VtmDto selectedVtmDto) {
        this.selectedVtmDto = selectedVtmDto;
        if (selectedVtmDto != null && selectedVtmDto.getId() != null) {
            this.current = getFacade().find(selectedVtmDto.getId());
        } else {
            this.current = null;
        }
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
        clearDtoCache();

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
                selectedVtmDto = null;
                vtmAuditEvents = null;
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
                return "Active Store VTMs";
            case "inactive":
                return "Inactive Store VTMs";
            case "all":
                return "All Store VTMs";
            default:
                return "Active Store VTMs";
        }
    }

    // ===================== Toggle Status Methods =====================

    public void toggleVtmStatus() {
        if (current == null) {
            JsfUtil.addErrorMessage("No VTM selected");
            return;
        }

        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("Store VTM Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("Store VTM Deactivated Successfully");
        }

        getFacade().edit(current);

        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate Store VTM" : "Deactivate Store VTM";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "StoreVtm", action, current.getId());

        items = null;
        clearDtoCache();
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

    private Map<String, Object> createAuditMap(Vtm vtm) {
        Map<String, Object> auditData = new HashMap<>();
        if (vtm != null) {
            auditData.put("id", vtm.getId());
            auditData.put("name", vtm.getName());
            auditData.put("code", vtm.getCode());
            auditData.put("retired", vtm.isRetired());
            auditData.put("inactive", vtm.isInactive());
            auditData.put("departmentType", vtm.getDepartmentType() != null ? vtm.getDepartmentType().toString() : null);
            auditData.put("descreption", vtm.getDescreption());
            auditData.put("instructions", vtm.getInstructions());
        }
        return auditData;
    }

    public void fillVtmAuditEvents() {
        vtmAuditEvents = new ArrayList<>();
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId "
                        + "AND a.entityType = :entityType ORDER BY a.eventDataTime DESC";
                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "StoreVtm");

                vtmAuditEvents = auditEventFacade.findByJpql(jpql, params);
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error loading audit history: " + e.getMessage());
            }
        }
    }

    public String navigateToAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VTM first");
            return null;
        }
        fillVtmAuditEvents();
        return "/pharmacy/admin/store_vtm_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getVtmAuditEvents() {
        return vtmAuditEvents;
    }

    public void setVtmAuditEvents(List<AuditEvent> vtmAuditEvents) {
        this.vtmAuditEvents = vtmAuditEvents;
    }

    // ===================== Validation =====================

    private boolean validateVtm() {
        if (current == null) {
            JsfUtil.addErrorMessage("No VTM selected for validation");
            return false;
        }

        if (current.getName() == null || current.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("VTM name is required");
            return false;
        }

        if (checkVtmName(current.getName(), current)) {
            JsfUtil.addErrorMessage("A VTM with this name already exists");
            return false;
        }

        if (current.getCode() != null && !current.getCode().trim().isEmpty()) {
            if (checkVtmCode(current.getCode(), current)) {
                JsfUtil.addErrorMessage("A VTM with this code already exists");
                return false;
            }
        }

        return true;
    }

    public boolean checkVtmName(String name, Vtm savingVtm) {
        if (savingVtm == null || name == null || name.trim().isEmpty()) {
            return false;
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT c FROM Vtm c WHERE c.retired=:retired AND UPPER(c.name)=:name";
        if (savingVtm.getId() != null) {
            jpql += " AND c.id <> :id";
            params.put("id", savingVtm.getId());
        }
        params.put("retired", false);
        params.put("name", name.toUpperCase().trim());
        Vtm vtm = getFacade().findFirstByJpql(jpql, params);
        return vtm != null;
    }

    public boolean checkVtmCode(String code, Vtm savingVtm) {
        if (savingVtm == null || code == null || code.trim().isEmpty()) {
            return false;
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT c FROM Vtm c WHERE c.retired=:retired AND UPPER(c.code)=:code";
        if (savingVtm.getId() != null) {
            jpql += " AND c.id <> :id";
            params.put("id", savingVtm.getId());
        }
        params.put("retired", false);
        params.put("code", code.toUpperCase().trim());
        Vtm vtm = getFacade().findFirstByJpql(jpql, params);
        return vtm != null;
    }

    // ===================== JSF Converter =====================

    @FacesConverter("storeVtmDtoConverter")
    public static class StoreVtmDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                StoreVtmController controller = (StoreVtmController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "storeVtmController");

                if (controller == null) {
                    return null;
                }

                Vtm entity = controller.getFacade().find(id);
                if (entity != null) {
                    return controller.createVtmDto(entity);
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
            if (object instanceof VtmDto) {
                VtmDto dto = (VtmDto) object;
                return dto.getId() != null ? dto.getId().toString() : null;
            }
            throw new IllegalArgumentException("Expected VtmDto object");
        }
    }

    @FacesConverter("stoVtmCon")
    public static class VtmControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StoreVtmController controller = (StoreVtmController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "storeVtmController");
            return controller.getEjbFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Vtm) {
                Vtm o = (Vtm) object;
                return String.valueOf(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Vtm.class.getName());
            }
        }
    }

}

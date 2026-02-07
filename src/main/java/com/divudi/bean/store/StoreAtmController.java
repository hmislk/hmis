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
import com.divudi.core.data.dto.AtmDto;
import com.divudi.core.entity.pharmacy.Atm;
import com.divudi.core.facade.AtmFacade;
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
public class StoreAtmController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AtmFacade ejbFacade;
    @EJB
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;

    private Atm current;
    private List<Atm> items;

    // DTO properties
    private List<AtmDto> atmDtoList;
    private AtmDto selectedAtmDto;

    // Audit properties
    private List<AuditEvent> atmAuditEvents;

    private boolean editable;

    // Filter state for active/inactive ATMs
    private String filterStatus = "active";

    public List<Atm> completeAtm(String query) {
        if (query == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT c FROM Atm c WHERE c.retired=:retired "
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
        current = new Atm();
        current.setDepartmentType(DepartmentType.Store);
        selectedAtmDto = null;
        editable = true;
    }

    public void edit() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select an ATM to edit");
            return;
        }
        if (current.isInactive()) {
            JsfUtil.addWarningMessage("Editing inactive ATM '" + current.getName() + "'");
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedAtmDto = null;
        editable = false;
    }

    public void save() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }

        if (!validateAtm()) {
            return;
        }

        try {
            boolean isNewAtm = getCurrent().getId() == null;

            if (getCurrent().getId() != null) {
                // UPDATE
                Atm beforeUpdate = getFacade().find(getCurrent().getId());
                Map<String, Object> beforeData = createAuditMap(beforeUpdate);

                getFacade().edit(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreAtm", "Update Store ATM", getCurrent().getId());

                JsfUtil.addSuccessMessage("Store ATM '" + getCurrent().getName() + "' updated successfully");
            } else {
                // CREATE
                getCurrent().setCreatedAt(new Date());
                getCurrent().setCreater(getSessionController().getLoggedUser());
                getFacade().create(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(null, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreAtm", "Create Store ATM", getCurrent().getId());

                JsfUtil.addSuccessMessage("Store ATM '" + getCurrent().getName() + "' created successfully");

                if (isNewAtm && getCurrent().getId() != null) {
                    selectedAtmDto = createAtmDto(getCurrent());
                }
            }
            editable = false;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Store ATM '" + (getCurrent().getName() != null ? getCurrent().getName() : "Unknown") + "': " + e.getMessage());
            e.printStackTrace();
        }

        items = null;
        clearDtoCache();
        getItems();
    }

    public void delete() {
        if (current != null) {
            String atmName = current.getName();
            try {
                Map<String, Object> beforeData = createAuditMap(current);

                current.setRetired(true);
                current.setRetiredAt(new Date());
                current.setRetirer(getSessionController().getLoggedUser());
                getFacade().edit(current);

                Map<String, Object> afterData = createAuditMap(current);
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreAtm", "Delete Store ATM", current.getId());

                JsfUtil.addSuccessMessage("Store ATM '" + (atmName != null ? atmName : "Unknown") + "' deleted successfully");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error deleting Store ATM '" + (atmName != null ? atmName : "Unknown") + "': " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            JsfUtil.addErrorMessage("No ATM selected for deletion");
        }
        current = null;
        selectedAtmDto = null;
        items = null;
        clearDtoCache();
        getItems();
        getCurrent();
        editable = false;
    }

    // ========== Getters/Setters ==========

    public StoreAtmController() {
    }

    private AtmFacade getFacade() {
        return ejbFacade;
    }

    public AtmFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AtmFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Atm getCurrent() {
        if (current == null) {
            current = new Atm();
            current.setDepartmentType(DepartmentType.Store);
        }
        return current;
    }

    public void setCurrent(Atm current) {
        this.current = current;
    }

    public List<Atm> getItems() {
        if (items == null) {
            String sql = "SELECT c FROM Atm c WHERE c.retired=:retired "
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

    public void setItems(List<Atm> items) {
        this.items = items;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // ===================== DTO Methods =====================

    public List<AtmDto> getAtmDtos() {
        if (atmDtoList == null) {
            String jpql = "SELECT new com.divudi.core.data.dto.AtmDto("
                    + "a.id, "
                    + "a.name, "
                    + "a.code, "
                    + "a.descreption, "
                    + "a.retired, "
                    + "a.inactive) "
                    + "FROM Atm a WHERE a.retired=:retired "
                    + "AND a.departmentType=:dep ";

            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Store);

            if ("active".equals(filterStatus)) {
                jpql += "AND a.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "AND a.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "ORDER BY a.name";

            atmDtoList = (List<AtmDto>) getFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        }
        return atmDtoList;
    }

    public List<AtmDto> completeAtmDto(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT new com.divudi.core.data.dto.AtmDto("
                + "a.id, "
                + "a.name, "
                + "a.code, "
                + "a.descreption, "
                + "a.retired, "
                + "a.inactive) "
                + "FROM Atm a WHERE a.retired=:retired "
                + "AND a.name LIKE :query "
                + "AND a.departmentType=:dep ";

        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("query", "%" + query + "%");
        params.put("dep", DepartmentType.Store);

        if ("active".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", true);
        }

        jpql += "ORDER BY a.name";

        try {
            return (List<AtmDto>) getFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void clearDtoCache() {
        atmDtoList = null;
    }

    public AtmDto createAtmDto(Atm atm) {
        if (atm == null) {
            return null;
        }
        AtmDto dto = new AtmDto(
                atm.getId(),
                atm.getName(),
                atm.getCode(),
                atm.getDescreption(),
                atm.isRetired(),
                atm.isInactive()
        );
        return dto;
    }

    public List<AtmDto> getAtmDtoList() {
        return getAtmDtos();
    }

    public void setAtmDtoList(List<AtmDto> atmDtoList) {
        this.atmDtoList = atmDtoList;
    }

    public AtmDto getSelectedAtmDto() {
        return selectedAtmDto;
    }

    public void setSelectedAtmDto(AtmDto selectedAtmDto) {
        this.selectedAtmDto = selectedAtmDto;
        if (selectedAtmDto != null && selectedAtmDto.getId() != null) {
            this.current = getFacade().find(selectedAtmDto.getId());
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
                selectedAtmDto = null;
                atmAuditEvents = null;
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
                return "Active Store ATMs";
            case "inactive":
                return "Inactive Store ATMs";
            case "all":
                return "All Store ATMs";
            default:
                return "Active Store ATMs";
        }
    }

    // ===================== Toggle Status Methods =====================

    public void toggleAtmStatus() {
        if (current == null) {
            JsfUtil.addErrorMessage("No ATM selected");
            return;
        }

        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("Store ATM Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("Store ATM Deactivated Successfully");
        }

        getFacade().edit(current);

        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate Store ATM" : "Deactivate Store ATM";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "StoreAtm", action, current.getId());

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

    private Map<String, Object> createAuditMap(Atm atm) {
        Map<String, Object> auditData = new HashMap<>();
        if (atm != null) {
            auditData.put("id", atm.getId());
            auditData.put("name", atm.getName());
            auditData.put("code", atm.getCode());
            auditData.put("retired", atm.isRetired());
            auditData.put("inactive", atm.isInactive());
            auditData.put("departmentType", atm.getDepartmentType() != null ? atm.getDepartmentType().toString() : null);
            auditData.put("descreption", atm.getDescreption());
            auditData.put("vtmId", atm.getVtm() != null ? atm.getVtm().getId() : null);
            auditData.put("vtmName", atm.getVtm() != null ? atm.getVtm().getName() : null);
        }
        return auditData;
    }

    public void fillAtmAuditEvents() {
        atmAuditEvents = new ArrayList<>();
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId "
                        + "AND a.entityType = :entityType ORDER BY a.eventDataTime DESC";
                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "StoreAtm");

                atmAuditEvents = auditEventFacade.findByJpql(jpql, params);
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error loading audit history: " + e.getMessage());
            }
        }
    }

    public String navigateToAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select an ATM first");
            return null;
        }
        fillAtmAuditEvents();
        return "/pharmacy/admin/store_atm_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getAtmAuditEvents() {
        return atmAuditEvents;
    }

    public void setAtmAuditEvents(List<AuditEvent> atmAuditEvents) {
        this.atmAuditEvents = atmAuditEvents;
    }

    // ===================== Validation =====================

    private boolean validateAtm() {
        if (current == null) {
            JsfUtil.addErrorMessage("No ATM selected for validation");
            return false;
        }

        if (current.getName() == null || current.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("ATM name is required");
            return false;
        }

        if (checkAtmName(current.getName(), current)) {
            JsfUtil.addErrorMessage("An ATM with this name already exists");
            return false;
        }

        if (current.getCode() != null && !current.getCode().trim().isEmpty()) {
            if (checkAtmCode(current.getCode(), current)) {
                JsfUtil.addErrorMessage("An ATM with this code already exists");
                return false;
            }
        }

        return true;
    }

    public boolean checkAtmName(String name, Atm savingAtm) {
        if (savingAtm == null || name == null || name.trim().isEmpty()) {
            return false;
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT c FROM Atm c WHERE c.retired=:retired AND UPPER(c.name)=:name";
        if (savingAtm.getId() != null) {
            jpql += " AND c.id <> :id";
            params.put("id", savingAtm.getId());
        }
        params.put("retired", false);
        params.put("name", name.toUpperCase().trim());
        Atm atm = getFacade().findFirstByJpql(jpql, params);
        return atm != null;
    }

    public boolean checkAtmCode(String code, Atm savingAtm) {
        if (savingAtm == null || code == null || code.trim().isEmpty()) {
            return false;
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT c FROM Atm c WHERE c.retired=:retired AND UPPER(c.code)=:code";
        if (savingAtm.getId() != null) {
            jpql += " AND c.id <> :id";
            params.put("id", savingAtm.getId());
        }
        params.put("retired", false);
        params.put("code", code.toUpperCase().trim());
        Atm atm = getFacade().findFirstByJpql(jpql, params);
        return atm != null;
    }

    // ===================== JSF Converter =====================

    @FacesConverter("storeAtmDtoConverter")
    public static class StoreAtmDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                StoreAtmController controller = (StoreAtmController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "storeAtmController");

                if (controller == null) {
                    return null;
                }

                Atm entity = controller.getFacade().find(id);
                if (entity != null) {
                    return controller.createAtmDto(entity);
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
            if (object instanceof AtmDto) {
                AtmDto dto = (AtmDto) object;
                return dto.getId() != null ? dto.getId().toString() : null;
            }
            throw new IllegalArgumentException("Expected AtmDto object");
        }
    }

    @FacesConverter("stoAtmCon")
    public static class AtmControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StoreAtmController controller = (StoreAtmController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "storeAtmController");
            return controller.getEjbFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Atm) {
                Atm o = (Atm) object;
                return String.valueOf(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Atm.class.getName());
            }
        }
    }

}

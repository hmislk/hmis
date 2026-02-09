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
import com.divudi.core.data.dto.AmppDto;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.facade.AmppFacade;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.PharmacyBean;

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
public class StoreAmppController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AmppFacade ejbFacade;
    @Inject
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;
    @EJB
    PharmacyBean pharmacyBean;

    private Ampp current;
    private List<Ampp> items;
    MeasurementUnit packUnit;

    // DTO properties
    private AmppDto selectedAmppDto;
    private List<AmppDto> amppDtos;

    // Audit properties
    private List<AuditEvent> amppAuditEvents;

    private boolean editable;
    private Map<String, Object> beforeEditData;

    // Filter state for active/inactive AMPPs
    private String filterStatus = "active";

    public List<Ampp> completeAmpp(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "SELECT c FROM Ampp c WHERE c.retired = false "
                + "AND LOWER(c.name) LIKE :query "
                + "AND c.departmentType=:dep "
                + "ORDER BY c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.trim().toLowerCase() + "%");
        params.put("dep", DepartmentType.Store);
        return getFacade().findByJpqlWithoutCache(jpql, params);
    }

    public void prepareAdd() {
        current = new Ampp();
        current.setVmpp(new Vmpp());
        current.setDepartmentType(DepartmentType.Store);
        selectedAmppDto = null;
        amppAuditEvents = null;
        beforeEditData = null;
        editable = true;
    }

    public void edit() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select an AMPP to edit");
            return;
        }
        if (current.isInactive()) {
            JsfUtil.addWarningMessage("Editing inactive AMPP '" + current.getName() + "'");
        }
        if (current.getId() != null) {
            selectedAmppDto = createAmppDto(current);
        }
        beforeEditData = createAuditMap(current);
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedAmppDto = null;
        amppAuditEvents = null;
        beforeEditData = null;
        editable = false;
    }

    public void saveSelected() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }

        Vmpp tmp = pharmacyBean.getVmpp(getCurrent(), getPackUnit());
        getCurrent().setVmpp(tmp);

        try {
            if (getCurrent().getId() != null && getCurrent().getId() > 0) {
                // UPDATE
                getCurrent().setDepartmentType(DepartmentType.Store);
                getFacade().edit(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeEditData, afterData,
                        getSessionController().getLoggedUser(),
                        "StoreAmpp", "Update Store AMPP", getCurrent().getId());

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
                        "StoreAmpp", "Create Store AMPP", getCurrent().getId());

                JsfUtil.addSuccessMessage("Saved Successfully.");
            }
            selectedAmppDto = createAmppDto(getCurrent());
            editable = false;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Store AMPP: " + e.getMessage());
            e.printStackTrace();
        }

        items = null;
        amppDtos = null;
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
                        "StoreAmpp", "Delete Store AMPP", current.getId());

                JsfUtil.addSuccessMessage("Deleted Successfully");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error deleting Store AMPP: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }

        current = null;
        selectedAmppDto = null;
        amppAuditEvents = null;
        items = null;
        amppDtos = null;
        getItems();
        editable = false;
    }

    // ========== Getters/Setters ==========

    public StoreAmppController() {
    }

    private AmppFacade getFacade() {
        return ejbFacade;
    }

    public AmppFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AmppFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Ampp getCurrent() {
        if (current == null) {
            current = new Ampp();
            current.setDepartmentType(DepartmentType.Store);
            current.setDblValue(1.0f);
        }
        return current;
    }

    public void setCurrent(Ampp current) {
        this.current = current;
    }

    public MeasurementUnit getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(MeasurementUnit packUnit) {
        this.packUnit = packUnit;
    }

    public List<Ampp> getItems() {
        if (items == null) {
            String jpql = "select a from Ampp a "
                    + "where a.departmentType=:dep "
                    + "and a.retired=:retired ";
            Map<String, Object> params = new HashMap<>();
            params.put("dep", DepartmentType.Store);
            params.put("retired", false);

            if ("active".equals(filterStatus)) {
                jpql += "and a.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "and a.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "order by a.name";
            items = getFacade().findByJpql(jpql, params);
        }
        return items;
    }

    public void setItems(List<Ampp> items) {
        this.items = items;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // ===================== DTO Methods =====================

    public List<AmppDto> getAmppDtos() {
        if (amppDtos == null) {
            String jpql = "SELECT new com.divudi.core.data.dto.AmppDto("
                    + "a.id, a.name, a.code, a.retired, a.inactive) "
                    + "FROM Ampp a WHERE a.departmentType=:dep AND a.retired=:retired ";

            Map<String, Object> params = new HashMap<>();
            params.put("dep", DepartmentType.Store);
            params.put("retired", false);

            if ("active".equals(filterStatus)) {
                jpql += "AND a.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "AND a.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "ORDER BY a.name";
            amppDtos = (List<AmppDto>) getFacade().findLightsByJpql(jpql, params);
        }
        return amppDtos;
    }

    public List<AmppDto> completeAmppDto(String query) {
        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }

        String jpql = "SELECT new com.divudi.core.data.dto.AmppDto("
                + "a.id, a.name, a.code, a.retired, a.inactive, "
                + "a.dblValue, a.amp.id, a.amp.name) "
                + "FROM Ampp a "
                + "WHERE (LOWER(a.name) LIKE :query "
                + "OR LOWER(a.amp.name) LIKE :query) "
                + "AND a.departmentType=:dep AND a.retired=:retired ";

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toLowerCase() + "%");
        params.put("dep", DepartmentType.Store);
        params.put("retired", false);

        if ("active".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", true);
        }

        jpql += "ORDER BY a.name";

        try {
            return (List<AmppDto>) getFacade().findLightsByJpql(jpql, params);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public AmppDto getSelectedAmppDto() {
        return selectedAmppDto;
    }

    public void setSelectedAmppDto(AmppDto selectedAmppDto) {
        this.selectedAmppDto = selectedAmppDto;
        if (selectedAmppDto != null && selectedAmppDto.getId() != null) {
            this.current = getFacade().find(selectedAmppDto.getId());
        } else {
            this.current = null;
        }
    }

    public AmppDto createAmppDto(Ampp ampp) {
        if (ampp == null) {
            return null;
        }
        return new AmppDto(ampp.getId(), ampp.getName(), ampp.getCode(),
                ampp.isRetired(), ampp.isInactive());
    }

    // ===================== Filter Methods =====================

    public String getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(String filterStatus) {
        this.filterStatus = filterStatus;
        amppDtos = null;
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
        amppDtos = null;

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
                selectedAmppDto = null;
                amppAuditEvents = null;
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
                return "Active Store AMPPs";
            case "inactive":
                return "Inactive Store AMPPs";
            case "all":
                return "All Store AMPPs";
            default:
                return "Active Store AMPPs";
        }
    }

    // ===================== Toggle Status Methods =====================

    public void toggleAmppStatus() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select an AMPP to change status");
            return;
        }

        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("Store AMPP Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("Store AMPP Deactivated Successfully");
        }

        getFacade().edit(current);

        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate Store AMPP" : "Deactivate Store AMPP";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "StoreAmpp", action, current.getId());

        if (selectedAmppDto != null) {
            selectedAmppDto.setInactive(current.isInactive());
        }

        items = null;
        amppDtos = null;
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

    private Map<String, Object> createAuditMap(Ampp ampp) {
        Map<String, Object> auditData = new HashMap<>();
        if (ampp != null) {
            auditData.put("id", ampp.getId());
            auditData.put("name", ampp.getName());
            auditData.put("code", ampp.getCode());
            auditData.put("retired", ampp.isRetired());
            auditData.put("inactive", ampp.isInactive());
            auditData.put("departmentType", ampp.getDepartmentType() != null
                    ? ampp.getDepartmentType().toString() : null);
            auditData.put("dblValue", ampp.getDblValue());
            auditData.put("ampId", ampp.getAmp() != null ? ampp.getAmp().getId() : null);
            auditData.put("ampName", ampp.getAmp() != null ? ampp.getAmp().getName() : null);
            auditData.put("vmppId", ampp.getVmpp() != null ? ampp.getVmpp().getId() : null);
        }
        return auditData;
    }

    public void fillAmppAuditEvents() {
        amppAuditEvents = new ArrayList<>();
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId "
                        + "AND a.entityType = :entityType ORDER BY a.eventDataTime DESC";
                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "StoreAmpp");

                amppAuditEvents = auditEventFacade.findByJpql(jpql, params);
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error loading audit history: " + e.getMessage());
            }
        }
    }

    public String navigateToAmppAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select an AMPP first");
            return null;
        }
        fillAmppAuditEvents();
        return "/pharmacy/admin/store_ampp_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getAmppAuditEvents() {
        return amppAuditEvents;
    }

    public void setAmppAuditEvents(List<AuditEvent> amppAuditEvents) {
        this.amppAuditEvents = amppAuditEvents;
    }

    // ===================== JSF Converters =====================

    @FacesConverter("storeAmppDtoConverter")
    public static class StoreAmppDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                StoreAmppController controller = (StoreAmppController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "storeAmppController");

                if (controller == null) {
                    return null;
                }

                Ampp entity = controller.getFacade().find(id);
                if (entity != null) {
                    return controller.createAmppDto(entity);
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
            if (object instanceof AmppDto) {
                AmppDto dto = (AmppDto) object;
                return dto.getId() != null ? dto.getId().toString() : null;
            }
            throw new IllegalArgumentException("Expected AmppDto object");
        }
    }

    @FacesConverter("stoAmppCon")
    public static class AmppControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                StoreAmppController controller = (StoreAmppController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "storeAmppController");

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
            if (object instanceof Ampp) {
                Ampp o = (Ampp) object;
                return String.valueOf(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Ampp.class.getName());
            }
        }
    }
}

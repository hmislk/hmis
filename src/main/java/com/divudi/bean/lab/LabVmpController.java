/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.AuditEventController;
import com.divudi.service.AuditService;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.VmpDto;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.facade.VmpFacade;
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
public class LabVmpController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VmpFacade ejbFacade;
    @EJB
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;

    private Vmp current;
    private List<Vmp> items;

    // DTO properties
    private VmpDto selectedVmpDto;
    private List<VmpDto> vmpDtos;

    // Audit properties
    private List<AuditEvent> vmpAuditEvents;

    private boolean editable;

    // Filter state for active/inactive VMPs
    private String filterStatus = "active";

    public List<Vmp> completeVmp(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "SELECT c FROM Vmp c WHERE c.retired = false "
                + "AND LOWER(c.name) LIKE :query "
                + "AND c.departmentType=:dep "
                + "ORDER BY c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.trim().toLowerCase() + "%");
        params.put("dep", DepartmentType.Lab);
        return getFacade().findByJpqlWithoutCache(jpql, params);
    }

    public void prepareAdd() {
        current = new Vmp();
        current.setDepartmentType(DepartmentType.Lab);
        selectedVmpDto = null;
        vmpAuditEvents = null;
        editable = true;
    }

    public void edit() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMP to edit");
            return;
        }
        if (current.isInactive()) {
            JsfUtil.addWarningMessage("Editing inactive VMP '" + current.getName() + "'");
        }
        if (current.getId() != null) {
            selectedVmpDto = createVmpDto(current);
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedVmpDto = null;
        vmpAuditEvents = null;
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
                Vmp beforeUpdate = getFacade().find(getCurrent().getId());
                Map<String, Object> beforeData = createAuditMap(beforeUpdate);

                getCurrent().setDepartmentType(DepartmentType.Lab);
                getFacade().edit(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "LabVmp", "Update Lab VMP", getCurrent().getId());

                JsfUtil.addSuccessMessage("Updated Successfully.");
            } else {
                // CREATE
                getCurrent().setDepartmentType(DepartmentType.Lab);
                getCurrent().setCreatedAt(new Date());
                getCurrent().setCreater(getSessionController().getLoggedUser());
                getFacade().create(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(null, afterData,
                        getSessionController().getLoggedUser(),
                        "LabVmp", "Create Lab VMP", getCurrent().getId());

                JsfUtil.addSuccessMessage("Saved Successfully.");
            }
            editable = false;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Lab VMP: " + e.getMessage());
            e.printStackTrace();
        }

        items = null;
        vmpDtos = null;
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
                        "LabVmp", "Delete Lab VMP", current.getId());

                JsfUtil.addSuccessMessage("Deleted Successfully");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error deleting Lab VMP: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }

        current = null;
        selectedVmpDto = null;
        vmpAuditEvents = null;
        items = null;
        vmpDtos = null;
        getItems();
        getCurrent();
        editable = false;
    }

    // ========== Getters/Setters ==========

    public LabVmpController() {
    }

    private VmpFacade getFacade() {
        return ejbFacade;
    }

    public VmpFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(VmpFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Vmp getCurrent() {
        if (current == null) {
            current = new Vmp();
            current.setDepartmentType(DepartmentType.Lab);
        }
        return current;
    }

    public void setCurrent(Vmp current) {
        this.current = current;
    }

    public List<Vmp> getItems() {
        if (items == null) {
            String jpql = "select v from Vmp v "
                    + "where v.departmentType=:dep ";
            Map<String, Object> params = new HashMap<>();
            params.put("dep", DepartmentType.Lab);

            if ("active".equals(filterStatus)) {
                jpql += "and v.retired=:retired ";
                params.put("retired", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "and v.retired=:retired ";
                params.put("retired", true);
            }

            jpql += "order by v.name";
            items = getFacade().findByJpql(jpql, params);
        }
        return items;
    }

    public void setItems(List<Vmp> items) {
        this.items = items;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // ===================== DTO Methods =====================

    public List<VmpDto> getVmpDtos() {
        if (vmpDtos == null) {
            String jpql = "SELECT new com.divudi.core.data.dto.VmpDto("
                    + "v.id, v.name, v.code, v.descreption, v.retired, v.inactive) "
                    + "FROM Vmp v WHERE v.departmentType=:dep AND v.retired=:retired ";

            Map<String, Object> params = new HashMap<>();
            params.put("dep", DepartmentType.Lab);
            params.put("retired", false);

            if ("active".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "ORDER BY v.name";
            vmpDtos = (List<VmpDto>) getFacade().findLightsByJpql(jpql, params);
        }
        return vmpDtos;
    }

    public List<VmpDto> completeVmpDto(String query) {
        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }

        String jpql = "SELECT new com.divudi.core.data.dto.VmpDto("
                + "v.id, v.name, v.code, v.descreption, v.retired, v.inactive) "
                + "FROM Vmp v WHERE LOWER(v.name) LIKE :query "
                + "AND v.departmentType=:dep AND v.retired=:retired ";

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toLowerCase() + "%");
        params.put("dep", DepartmentType.Lab);
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
            return (List<VmpDto>) getFacade().findLightsByJpql(jpql, params);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public VmpDto getSelectedVmpDto() {
        return selectedVmpDto;
    }

    public void setSelectedVmpDto(VmpDto selectedVmpDto) {
        this.selectedVmpDto = selectedVmpDto;
        if (selectedVmpDto != null && selectedVmpDto.getId() != null) {
            this.current = getFacade().find(selectedVmpDto.getId());
        } else {
            this.current = null;
        }
    }

    public VmpDto createVmpDto(Vmp vmp) {
        if (vmp == null) {
            return null;
        }
        return new VmpDto(vmp.getId(), vmp.getName(), vmp.getCode(),
                vmp.getDescreption(), vmp.isRetired(), vmp.isInactive());
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
        vmpDtos = null;

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
                selectedVmpDto = null;
                vmpAuditEvents = null;
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
                return "Active Lab VMPs";
            case "inactive":
                return "Inactive Lab VMPs";
            case "all":
                return "All Lab VMPs";
            default:
                return "Active Lab VMPs";
        }
    }

    // ===================== Toggle Status Methods =====================

    public void toggleVmpStatus() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMP to change status");
            return;
        }

        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("Lab VMP Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("Lab VMP Deactivated Successfully");
        }

        getFacade().edit(current);

        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate Lab VMP" : "Deactivate Lab VMP";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "LabVmp", action, current.getId());

        if (selectedVmpDto != null) {
            selectedVmpDto.setInactive(current.isInactive());
        }

        items = null;
        vmpDtos = null;
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

    private Map<String, Object> createAuditMap(Vmp vmp) {
        Map<String, Object> auditData = new HashMap<>();
        if (vmp != null) {
            auditData.put("id", vmp.getId());
            auditData.put("name", vmp.getName());
            auditData.put("code", vmp.getCode());
            auditData.put("retired", vmp.isRetired());
            auditData.put("inactive", vmp.isInactive());
            auditData.put("departmentType", vmp.getDepartmentType() != null
                    ? vmp.getDepartmentType().toString() : null);
            auditData.put("descreption", vmp.getDescreption());
            auditData.put("vtmId", vmp.getVtm() != null ? vmp.getVtm().getId() : null);
            auditData.put("vtmName", vmp.getVtm() != null ? vmp.getVtm().getName() : null);
            auditData.put("dosageFormId", vmp.getDosageForm() != null ? vmp.getDosageForm().getId() : null);
            auditData.put("dosageFormName", vmp.getDosageForm() != null ? vmp.getDosageForm().getName() : null);
        }
        return auditData;
    }

    public void fillVmpAuditEvents() {
        vmpAuditEvents = new ArrayList<>();
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId "
                        + "AND a.entityType = :entityType ORDER BY a.eventDataTime DESC";
                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "LabVmp");

                vmpAuditEvents = auditEventFacade.findByJpql(jpql, params);
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error loading audit history: " + e.getMessage());
            }
        }
    }

    public String navigateToVmpAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMP first");
            return null;
        }
        fillVmpAuditEvents();
        return "/pharmacy/admin/lab_vmp_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getVmpAuditEvents() {
        return vmpAuditEvents;
    }

    public void setVmpAuditEvents(List<AuditEvent> vmpAuditEvents) {
        this.vmpAuditEvents = vmpAuditEvents;
    }

    // ===================== JSF Converters =====================

    @FacesConverter("labVmpDtoConverter")
    public static class LabVmpDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                LabVmpController controller = (LabVmpController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "labVmpController");

                if (controller == null) {
                    return null;
                }

                Vmp entity = controller.getFacade().find(id);
                if (entity != null) {
                    return controller.createVmpDto(entity);
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
            if (object instanceof VmpDto) {
                VmpDto dto = (VmpDto) object;
                return dto.getId() != null ? dto.getId().toString() : null;
            }
            throw new IllegalArgumentException("Expected VmpDto object");
        }
    }

    @FacesConverter("labVmpCon")
    public static class VmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                LabVmpController controller = (LabVmpController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "labVmpController");

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
            if (object instanceof Vmp) {
                Vmp o = (Vmp) object;
                return String.valueOf(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Vmp.class.getName());
            }
        }
    }
}

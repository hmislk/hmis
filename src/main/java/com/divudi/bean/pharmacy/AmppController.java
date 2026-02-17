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

import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.Vmpp;
import com.divudi.core.facade.AmppFacade;
import com.divudi.core.facade.VmppFacade;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.AmppDto;
import com.divudi.core.entity.AuditEvent;
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
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class AmppController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AmppFacade ejbFacade;
    private List<Ampp> selectedItems;
    private Ampp current;
    private List<Ampp> items = null;
    @EJB
    private VmppFacade vmppFacade;
    double dblValue;
    @EJB
    PharmacyBean pharmacyBean;
    MeasurementUnit packUnit;
    private String selectText = "";
    private boolean editable;

    // DTO Management Fields
    private AmppDto selectedAmppDto;
    private List<AmppDto> amppDtos;
    private List<AuditEvent> amppAuditEvents;

    // Audit-related injections
    @Inject
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;

    // Filter state for active/inactive AMPPs
    private String filterStatus = "active"; // "active", "inactive", "all"

    public MeasurementUnit getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(MeasurementUnit packUnit) {
        this.packUnit = packUnit;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public double getDblValue() {
        return dblValue;
    }

    public void setDblValue(double dblValue) {
        this.dblValue = dblValue;
    }

    public List<Ampp> completeAmpp(String qry) {
        List<Ampp> a = null;
        if (qry != null) {
            String jpql = "select c from Ampp c where c.retired=false "
                    + "and c.departmentType=:dep "
                    + "and (c.name) like :qry order by c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("qry", "%" + qry.toUpperCase() + "%");
            params.put("dep", DepartmentType.Pharmacy);
            a = getFacade().findByJpql(jpql, params);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    // DTO Management Methods

    public List<AmppDto> getAmppDtos() {
        String jpql = "SELECT new com.divudi.core.data.dto.AmppDto("
                + "a.id, a.name, a.code, a.retired, a.inactive) "
                + "FROM Ampp a WHERE a.departmentType=:dep AND a.retired=:retired ";

        Map<String, Object> params = new HashMap<>();
        params.put("dep", DepartmentType.Pharmacy);
        params.put("retired", false); // Only show non-deleted items

        // Apply status filter based on inactive attribute
        if ("active".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", true);
        }
        // For "all", no additional filter needed

        jpql += "ORDER BY a.name";

        return (List<AmppDto>) getFacade().findLightsByJpql(jpql, params);
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
        params.put("dep", DepartmentType.Pharmacy);
        params.put("retired", false); // Only show non-deleted items

        // Apply status filter based on inactive attribute
        if ("active".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND a.inactive=:inactive ";
            params.put("inactive", true);
        }
        // For "all", no additional filter needed

        jpql += "ORDER BY a.name";

        return (List<AmppDto>) getFacade().findLightsByJpql(jpql, params);
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

    // Audit History Management
    public void fillAmppAuditEvents() {
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId " +
                             "AND a.entityType = :entityType " +
                             "ORDER BY a.eventDataTime DESC";

                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "Ampp");

                amppAuditEvents = auditEventFacade.findByJpql(jpql, params);

            } catch (Exception e) {
                amppAuditEvents = new ArrayList<>();
            }
        } else {
            amppAuditEvents = new ArrayList<>();
        }
    }

    public String navigateToAmppAuditEvents() {
        fillAmppAuditEvents();
        return "/pharmacy/admin/ampp_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getAmppAuditEvents() {
        if (amppAuditEvents == null) {
            fillAmppAuditEvents();
        }
        return amppAuditEvents;
    }

    public void refreshAuditEvents() {
        amppAuditEvents = null;
        fillAmppAuditEvents();
    }

    // Filter Status Management
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
        amppDtos = null; // Clear DTO cache

        // Clear selection if current item doesn't match new filter
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
                return "Active AMPPs";
            case "inactive":
                return "Inactive AMPPs";
            case "all":
                return "All AMPPs";
            default:
                return "Active AMPPs";
        }
    }

    // Toggle Status Methods
    public void toggleAmppStatus() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select an AMPP to change status");
            return;
        }

        // Capture before state for audit
        Map<String, Object> beforeData = createAuditMap(current);

        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            // Reactivate AMPP
            current.setInactive(false);
            JsfUtil.addSuccessMessage("AMPP Activated Successfully");
        } else {
            // Deactivate AMPP
            current.setInactive(true);
            JsfUtil.addSuccessMessage("AMPP Deactivated Successfully");
        }

        getFacade().edit(current);

        // Capture after state for audit
        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate AMPP" : "Deactivate AMPP";

        // Log audit event
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "Ampp", action, current.getId());

        // Update DTO to reflect new status
        if (selectedAmppDto != null) {
            selectedAmppDto.setInactive(current.isInactive());
        }

        // Refresh data to reflect changes
        recreateModel();
        getItems();
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

    public void prepareAdd() {
        current = new Ampp();
        current.setVmpp(new Vmpp());
        current.setDepartmentType(DepartmentType.Pharmacy); // Automatically set to Pharmacy
        dblValue = 0.0;
        selectedAmppDto = null; // Clear DTO selection
        amppAuditEvents = null; // Clear audit events
        editable = true;
    }

    public void edit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to edit");
            return;
        }
        // Sync the DTO with current entity
        if (current.getId() != null) {
            selectedAmppDto = createAmppDto(current);
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedAmppDto = null;
        amppAuditEvents = null;
        editable = false;
    }

    private void recreateModel() {
        items = null;
    }

    private Map<String, Object> createAuditMap(Ampp ampp) {
        Map<String, Object> auditData = new HashMap<>();
        if (ampp != null) {
            auditData.put("id", ampp.getId());
            auditData.put("name", ampp.getName());
            auditData.put("code", ampp.getCode());
            auditData.put("retired", ampp.isRetired());
            auditData.put("inactive", ampp.isInactive());
            auditData.put("departmentType", ampp.getDepartmentType() != null ?
                ampp.getDepartmentType().toString() : null);
            auditData.put("dblValue", ampp.getDblValue());
            auditData.put("ampId", ampp.getAmp() != null ? ampp.getAmp().getId() : null);
            auditData.put("ampName", ampp.getAmp() != null ? ampp.getAmp().getName() : null);
            auditData.put("vmppId", ampp.getVmpp() != null ? ampp.getVmpp().getId() : null);
        }
        return auditData;
    }

    public void saveSelected() {
        Vmpp tmp = getPharmacyBean().getVmpp(getCurrent(), getPackUnit());
        getCurrent().setVmpp(tmp);

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            // UPDATE - capture before state
            Ampp beforeUpdate = getFacade().find(getCurrent().getId());
            Map<String, Object> beforeData = createAuditMap(beforeUpdate);

            getCurrent().setDepartmentType(DepartmentType.Pharmacy); // Ensure Pharmacy type
            getFacade().edit(getCurrent());

            // Log audit for update
            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(beforeData, afterData,
                    getSessionController().getLoggedUser(),
                    "Ampp", "Update AMPP", getCurrent().getId());

            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            // CREATE - no before state
            getCurrent().setDepartmentType(DepartmentType.Pharmacy); // Ensure Pharmacy type
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());

            // Log audit for create
            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(null, afterData,
                    getSessionController().getLoggedUser(),
                    "Ampp", "Create AMPP", getCurrent().getId());

            JsfUtil.addSuccessMessage("Saved Successfully");
        }

        recreateModel();
        getItems();
        editable = false;
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

    public AmppController() {
    }

    public Ampp getCurrent() {
        if (current == null) {
            current = new Ampp();
            current.setDblValue(1.0f);
        }
        return current;
    }

    public void setCurrent(Ampp current) {
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
                    "Ampp", "Delete AMPP", current.getId());

            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }

        // Clear all selections and state
        recreateModel();
        getItems();
        current = null;
        selectedAmppDto = null; // Clear DTO selection
        amppAuditEvents = null; // Clear audit events
        getCurrent();
        editable = false;
    }

    private AmppFacade getFacade() {
        return ejbFacade;
    }

    public List<Ampp> getItems() {
        if (items == null) {
            String j;
            Map<String, Object> params = new HashMap<>();
            j = "select a "
                    + " from Ampp a "
                    + " where a.departmentType=:dep ";

            params.put("dep", DepartmentType.Pharmacy);

            // Apply status filter
            if ("active".equals(filterStatus)) {
                j += "and a.retired=:retired and a.inactive=:inactive ";
                params.put("retired", false);
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                j += "and a.retired=:retired and a.inactive=:inactive ";
                params.put("retired", false);
                params.put("inactive", true);
            } else {
                // For "all", show only non-retired
                j += "and a.retired=:retired ";
                params.put("retired", false);
            }

            j += "order by a.name";
            items = getFacade().findByJpql(j, params);
        }
        return items;
    }

    public List<Ampp> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<Ampp> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public VmppFacade getVmppFacade() {
        return vmppFacade;
    }

    public void setVmppFacade(VmppFacade vmppFacade) {
        this.vmppFacade = vmppFacade;
    }

    public void searchItems(AjaxBehaviorEvent e) {
        String jpql = "select c from Ampp c where c.retired=false "
                + "and c.departmentType=:dep "
                + "and (c.name) like :qry order by c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("qry", "%" + getSelectText().toUpperCase() + "%");
        params.put("dep", DepartmentType.Pharmacy);
        selectedItems = getFacade().findByJpql(jpql, params);
    }

    public String getSelectText() {
        return selectText;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     *
     */
    @FacesConverter(forClass = Ampp.class)
    public static class AmppConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            AmppController controller = (AmppController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "amppController");
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
            if (object instanceof Ampp) {
                Ampp o = (Ampp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AmppController.class.getName());
            }
        }
    }

    /**
     * JSF converter for AMPP DTO
     */
    @FacesConverter("amppDtoConverter")
    public static class AmppDtoConverter implements Converter {
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                AmppController controller = (AmppController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "amppController");

                Ampp entity = controller.getFacade().find(id);
                return entity != null ? controller.createAmppDto(entity) : null;
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
}

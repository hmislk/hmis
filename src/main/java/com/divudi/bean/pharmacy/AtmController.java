/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.AuditEventController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.AtmDto;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.pharmacy.Atm;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.facade.AtmFacade;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class AtmController implements Serializable {

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
    List<Atm> selectedItems;
    private Atm current;
    private List<Atm> items;
    List<Atm> atmList;
    String selectText;
    private boolean editable;

    // DTO properties - For optimized display and reporting
    private List<AtmDto> atmDtoList;
    private AtmDto selectedAtmDto;

    // Audit properties - For viewing audit history
    private List<AuditEvent> atmAuditEvents;

    public String navigateToListAllAtms() {
        String jpql = "Select atm "
                + " from Atm atm "
                + " where atm.retired=:ret "
                + " order by atm.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(jpql, m);
        return "/emr/reports/atms?faces-redirect=trues";
    }

    public List<Atm> completeAtm(String query) {
        String sql;
        if (query == null) {
            atmList = new ArrayList<>();
        } else {
            sql = "select c from Atm c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            atmList = getFacade().findByJpql(sql);
        }
        return atmList;
    }

    public Atm findAndSaveAtmByNameAndCode(Atm atm) {
        String jpql;
        Map m = new HashMap();
        Atm natm;
        if (atm == null) {
            return null;
        } else {
            m.put("ret", false);
            m.put("name", atm.getName());
            m.put("code", atm.getCode());
            jpql = "select c "
                    + " from Atm c "
                    + " where c.retired=:ret "
                    + " and c.name=:name"
                    + " and c.code=:code";
            natm = getFacade().findFirstByJpql(jpql, m);
        }
        if (natm != null) {
            return natm;
        }
        if (atm.getId() == null) {
            getFacade().create(atm);
        }
        return atm;
    }

    public Atm findAtmByName(String atm) {
        String jpql;
        Map m = new HashMap();
        Atm natm;
        if (atm == null) {
            return null;
        } else {
            m.put("ret", false);
            m.put("name", atm);
            jpql = "select c "
                    + " from Atm c "
                    + " where c.retired=:ret "
                    + " and c.name=:name";
            natm = getFacade().findFirstByJpql(jpql, m);
        }
        return natm;
    }

    public Atm findAndSaveAtmByNameAndCode(Atm atm, Vtm vtm) {
        String jpql;
        Map m = new HashMap();
        Atm natm;
        if (atm == null) {
            return null;
        } else {
            m.put("ret", false);
            m.put("name", atm.getName());
            m.put("code", atm.getCode());
            m.put("vtm", vtm);
            jpql = "select c "
                    + " from Atm c "
                    + " where c.retired=:ret "
                    + " and c.vtm=:vtm "
                    + " and c.name=:name"
                    + " and c.code=:code";
            natm = getFacade().findFirstByJpql(jpql, m);
        }
        if (natm != null) {
            return natm;
        }
        if (atm.getId() == null) {
            atm.setVtm(vtm);
            getFacade().create(atm);
        }
        return atm;
    }

    public List<Atm> getSelectedItems() {

        if (selectText == null || selectText.trim().isEmpty()) {
            selectedItems = getFacade().findByJpql("select c from Atm c where c.retired=false order by c.name");
        } else {
            String sql = "select c from Atm c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
            selectedItems = getFacade().findByJpql(sql);

        }
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Atm();
        selectedAtmDto = null; // Clear DTO selection when adding new
        editable = true;
    }

    public void edit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to edit");
            return;
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedAtmDto = null; // Clear DTO selection on cancel
        editable = false;
    }

    public void setSelectedItems(List<Atm> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        try {
            if (getCurrent().getId() != null && getCurrent().getId() > 0) {
                // UPDATE - capture before state for audit
                Atm beforeUpdate = getFacade().find(getCurrent().getId());
                Map<String, Object> beforeData = createAuditMap(beforeUpdate);

                getFacade().edit(getCurrent());

                // Capture after state for audit
                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "Atm", "Update ATM", getCurrent().getId());

                JsfUtil.addSuccessMessage("Updated Successfully.");
            } else {
                // CREATE - no before state
                getCurrent().setCreatedAt(new Date());
                getCurrent().setCreater(getSessionController().getLoggedUser());
                getFacade().create(getCurrent());
                getFacade().edit(getCurrent());

                // Capture after state for audit (create operation)
                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(null, afterData,
                        getSessionController().getLoggedUser(),
                        "Atm", "Create ATM", getCurrent().getId());

                // Set DTO for newly created ATM
                selectedAtmDto = createAtmDto(getCurrent());
                JsfUtil.addSuccessMessage("Saved Successfully");
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving ATM: " + e.getMessage());
            return;
        }
        recreateModel();
        getItems();
        clearDtoCache(); // Clear DTO cache when data changes
        editable = false;
    }

    public void saveAtm(Atm atm) {
        if(atm==null) return;

        if (atm.getId() != null) {
            getFacade().edit(atm);
        } else {
            getFacade().create(atm);
        }
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
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

    public AtmController() {
    }

    public Atm getCurrent() {
        if (current == null) {
            current = new Atm();
        }
        return current;
    }

    public void setCurrent(Atm current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            String atmName = current.getName();
            try {
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
                        "Atm", "Delete ATM", current.getId());

                JsfUtil.addSuccessMessage("Deleted Successfully");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error deleting ATM: " + e.getMessage());
                return;
            }
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        selectedAtmDto = null; // Clear DTO selection after delete
        clearDtoCache(); // Clear DTO cache when data changes
        current = null;
        getCurrent();
        editable = false;
    }

    private AtmFacade getFacade() {
        return ejbFacade;
    }

    public List<Atm> getItems() {
         String sql = " select c from Atm c where "
                + " c.retired=false "
                + " order by c.name ";

        items = getFacade().findByJpql(sql);
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

    // ========================== DTO Methods ==========================

    /**
     * Gets ATM DTOs with department type filtering for pharmacy operations.
     * Uses lazy loading and caching for optimal performance.
     *
     * @return List of ATM DTOs filtered for pharmacy department
     */
    public List<AtmDto> getAtmDtos() {
        if (atmDtoList == null) {
            String jpql = "SELECT new com.divudi.core.data.dto.AtmDto("
                    + "a.id, "
                    + "a.name, "
                    + "a.code, "
                    + "a.descreption, "
                    + "a.retired, "
                    + "a.vtm.id, "
                    + "a.vtm.name, "
                    + "CAST(a.departmentType AS string)) "
                    + "FROM Atm a WHERE a.retired=:retired "
                    + "AND a.departmentType=:dep "
                    + "ORDER BY a.name";

            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Pharmacy);

            // Use findLightsByJpql for DTO constructor queries
            atmDtoList = (List<AtmDto>) getFacade().findLightsByJpql(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
        }
        return atmDtoList;
    }

    /**
     * Autocomplete method for ATM DTO search in UI components.
     * Provides filtered search results based on name pattern matching.
     *
     * @param query Search query string for ATM name filtering
     * @return List of matching ATM DTOs
     */
    public List<AtmDto> completeAtmDto(String query) {
        System.out.println("completeAtmDto called with query: '" + query + "'");

        if (query == null || query.trim().isEmpty()) {
            System.out.println("Query is null or empty, returning empty list");
            return new ArrayList<>();
        }

        String jpql = "SELECT new com.divudi.core.data.dto.AtmDto("
                + "a.id, "
                + "a.name, "
                + "a.code, "
                + "a.descreption, "
                + "a.retired, "
                + "a.vtm.id, "
                + "a.vtm.name) "
                + "FROM Atm a WHERE a.retired=:retired "
                + "AND a.name LIKE :query "
                + "AND a.departmentType=:dep "
                + "ORDER BY a.name";

        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("query", "%" + query + "%");
        params.put("dep", DepartmentType.Pharmacy);

        try {
            List<AtmDto> results = (List<AtmDto>) getFacade().findLightsByJpql(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
            return results;
        } catch (Exception e) {
            System.out.println("Error in completeAtmDto: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Sets the selected ATM DTO and synchronizes with the entity.
     * Automatically loads the full ATM entity when a DTO is selected.
     *
     * @param selectedAtmDto The selected ATM DTO from UI
     */
    public void setSelectedAtmDto(AtmDto selectedAtmDto) {
        System.out.println("setSelectedAtmDto called");
        System.out.println("selectedAtmDto = " + selectedAtmDto);
        this.selectedAtmDto = selectedAtmDto;

        // Sync with entity if DTO is selected
        if (selectedAtmDto != null && selectedAtmDto.getId() != null) {
            System.out.println("selectedAtmDto.getId() = " + selectedAtmDto.getId());
            this.current = getFacade().find(selectedAtmDto.getId());
            System.out.println("Loaded entity: " + (this.current != null ? this.current.getName() : "null"));
        } else {
            // Clear current entity when no DTO is selected
            System.out.println("Clearing current entity (selectedAtmDto is null or has null ID)");
            this.current = null;
        }
        System.out.println("current = " + current);
    }

    /**
     * Gets the currently selected ATM DTO.
     *
     * @return The selected ATM DTO
     */
    public AtmDto getSelectedAtmDto() {
        return selectedAtmDto;
    }

    /**
     * Creates an ATM DTO from an ATM entity.
     * Helper method for entity-to-DTO conversion.
     *
     * @param atm The ATM entity to convert
     * @return AtmDto representation of the entity
     */
    public AtmDto createAtmDto(Atm atm) {
        if (atm == null) {
            return null;
        }
        return new AtmDto(
                atm.getId(),
                atm.getName(),
                atm.getCode(),
                atm.getDescreption(),
                atm.isRetired(),
                atm.getVtm() != null ? atm.getVtm().getId() : null,
                atm.getVtm() != null ? atm.getVtm().getName() : null
        );
    }

    /**
     * Clears the DTO cache to force reload on next access.
     * Should be called after create, update, or delete operations.
     */
    public void clearDtoCache() {
        atmDtoList = null;
    }

    // ========================== Audit History Methods ==========================

    /**
     * Navigates to the ATM audit events page.
     * Loads audit history for the currently selected ATM.
     *
     * @return Navigation outcome to audit events page
     */
    public String navigateToAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select an ATM first");
            return null;
        }

        // Load audit events for the current ATM
        fillAtmAuditEvents();

        // Navigate to audit events page
        return "/pharmacy/admin/atm_audit_events?faces-redirect=true";
    }

    /**
     * Loads audit events for the current ATM entity.
     * Filters by ATM ID and calculates differences for display.
     */
    public void fillAtmAuditEvents() {
        atmAuditEvents = new ArrayList<>();
        if (current != null && current.getId() != null) {
            // Use simplified query - only filter by objectId, similar to VTM audit approach
            String jpql = "select a from AuditEvent a "
                    + "where a.objectId = :objectId "
                    + "order by a.eventDataTime desc";

            Map<String, Object> params = new HashMap<>();
            params.put("objectId", current.getId());

            atmAuditEvents = auditEventFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

            // Calculate differences for display (like the general audit page does)
            for (AuditEvent ae : atmAuditEvents) {
                ae.calculateDifference();
            }
        }
    }

    /**
     * Gets all audit events for the current ATM (for debugging purposes).
     *
     * @return List of audit events for current ATM
     */
    public List<AuditEvent> getAllAuditEventsForAtm() {
        if (current != null && current.getId() != null) {
            fillAtmAuditEvents();
        }
        return atmAuditEvents;
    }

    /**
     * Gets the ATM audit events list.
     *
     * @return List of audit events for the current ATM
     */
    public List<AuditEvent> getAtmAuditEvents() {
        return atmAuditEvents;
    }

    /**
     * Sets the ATM audit events list.
     *
     * @param atmAuditEvents List of audit events to set
     */
    public void setAtmAuditEvents(List<AuditEvent> atmAuditEvents) {
        this.atmAuditEvents = atmAuditEvents;
    }

    /**
     * Creates an audit data map from an ATM entity.
     * Captures all relevant fields for audit trail tracking.
     *
     * @param atm The ATM entity to create audit data for
     * @return Map containing audit-relevant field values
     */
    private Map<String, Object> createAuditMap(Atm atm) {
        Map<String, Object> auditData = new HashMap<>();
        if (atm != null) {
            auditData.put("id", atm.getId());
            auditData.put("name", atm.getName());
            auditData.put("code", atm.getCode());
            auditData.put("retired", atm.isRetired());
            auditData.put("departmentType", atm.getDepartmentType() != null ? atm.getDepartmentType().toString() : null);
            auditData.put("descreption", atm.getDescreption()); // Note: intentional spelling for backward compatibility
            auditData.put("vtmId", atm.getVtm() != null ? atm.getVtm().getId() : null);
            auditData.put("vtmName", atm.getVtm() != null ? atm.getVtm().getName() : null);
        }
        return auditData;
    }

    /**
     *
     */
    @FacesConverter(forClass = Atm.class)
    public static class AtmControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            AtmController controller = (AtmController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "atmController");
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
            if (object instanceof Atm) {
                Atm o = (Atm) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AtmController.class.getName());
            }
        }
    }

    /**
     * JSF Converter for AtmDto objects.
     * Handles conversion between AtmDto objects and String IDs for JSF components.
     */
    @FacesConverter("atmDtoConverter")
    public static class AtmDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            System.out.println("AtmDtoConverter.getAsObject called with value: '" + value + "'");

            if (value == null || value.isEmpty()) {
                System.out.println("Value is null or empty, returning null");
                return null;
            }

            try {
                Long id = Long.parseLong(value);
                System.out.println("Parsed ID: " + id);

                // Get controller instance
                AtmController controller = (AtmController) facesContext.getApplication().getELResolver().
                        getValue(facesContext.getELContext(), null, "atmController");

                if (controller == null) {
                    System.out.println("Controller not found!");
                    return null;
                }

                // Load entity from database and convert to DTO
                Atm entity = controller.getFacade().find(id);
                if (entity != null) {
                    AtmDto dto = controller.createAtmDto(entity);
                    System.out.println("Created DTO from entity: " + dto.getName());
                    return dto;
                } else {
                    System.out.println("Entity not found for ID: " + id);
                    return null;
                }

            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException: " + e.getMessage());
                return null;
            } catch (Exception e) {
                System.out.println("Exception in getAsObject: " + e.getMessage());
                e.printStackTrace();
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
                String result = dto.getId() != null ? dto.getId().toString() : null;
                System.out.println("AtmDtoConverter.getAsString: " + dto.getName() + " -> " + result);
                return result;
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AtmDto.class.getName());
            }
        }
    }

}

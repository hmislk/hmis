/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.AuditEventController;
import com.divudi.service.AuditService;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.VtmDto;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.VtmFacade;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
public class VtmController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VtmFacade ejbFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @EJB
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;
    @Inject
    private BillBeanController billBean;
    // Entity properties - Keep for business logic and backward compatibility
    List<Vtm> selectedItems;
    private Vtm current;
    private List<Vtm> items;
    List<Vtm> vtmList;

    // DTO properties - For optimized display and reporting
    private List<VtmDto> vtmDtoList;
    private VtmDto selectedVtmDto;

    // Audit properties - For viewing audit history
    private List<AuditEvent> vtmAuditEvents;

    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    private boolean editable;

    // Filter state for active/inactive VTMs
    private String filterStatus = "active"; // "active", "inactive", "all"

    public String navigateToListAllVtms() {
        return "/emr/reports/vtms?faces-redirect=true";
    }

    public void cleanceVTMs() {
        List<Vtm> vtms = ejbFacade.findAll();
        for (Vtm v : vtms) {
            if (v.getName() == null) {
                return;
            }
            String strVtm = v.getName();
            strVtm = cleanVTMName(strVtm);
            strVtm = removeSpecificWords(strVtm, convertInputToArray(bulkText));
            v.setName(strVtm);
            getFacade().edit(v);
        }
    }

    public String cleanVTMName(String input) {
        // Remove all words that contain numbers and special characters
        String output = input.replaceAll("\\b\\w*[0-9#%\\W]\\w*\\b", "").trim();

        // Remove extra spaces
        output = output.replaceAll(" +", " ");

        return output;
    }

    public String removeSpecificWords(String input, String[] wordsToRemove) {
        String output = input;

        for (String word : wordsToRemove) {
            output = output.replaceAll("\\b" + word + "\\b", "").trim();
        }

        // Remove extra spaces
        output = output.replaceAll(" +", " ");

        return output;
    }

    public String removeDuplicateWords(String input) {
        String[] words = input.split("\\s+");
        String output = String.join(" ", new LinkedHashSet<String>(Arrays.asList(words)));
        return output;
    }

    public String[] convertInputToArray(String bulkText) {
        return bulkText.split("\n");
    }

    public List<Vtm> completeVtm(String query) {
        if (query == null) {
            vtmList = new ArrayList<Vtm>();
        } else {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                    + "AND UPPER(c.name) LIKE :query "
                    + "AND c.inactive=false "
                    + "ORDER BY c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("query", "%" + query.toUpperCase() + "%");
            vtmList = getFacade().findByJpql(sql, params);
        }
        return vtmList;
    }
    
    public List<Vtm> completeVtmPharmacy(String query) {
        if (query == null) {
            vtmList = new ArrayList<Vtm>();
        } else {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                    + "AND UPPER(c.name) LIKE :query "
                    + "AND c.departmentType=:dep "
                    + "AND c.inactive=false "
                    + "ORDER BY c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("query", "%" + query.toUpperCase() + "%");
            params.put("dep", DepartmentType.Pharmacy);
            vtmList = getFacade().findByJpql(sql, params);
        }
        return vtmList;
    }

    public Vtm findAndSaveVtmByNameAndCode(Vtm vtm) {
        String jpql;
        Map m = new HashMap();
        Vtm nvtm = null;
        if (vtm == null) {
            return null;
        } else {
            m.put("retired", false);
            m.put("name", vtm.getName());
            m.put("code", vtm.getCode());
            jpql = "select c "
                    + " from Vtm c "
                    + " where c.retired=:retired "
                    + " and c.name=:name"
                    + " and c.code=:code";
            List<Vtm> vtms = getFacade().findByJpql(jpql, m);
            if (vtms != null) {
                if (!vtms.isEmpty()) {
                    nvtm = vtms.get(0);
                }
            }
        }
        if (nvtm != null) {
            return nvtm;
        }
        if (vtm.getId() == null) {
            getFacade().create(vtm);
        }
        return vtm;
    }

    public Vtm findAndSaveVtmByName(String name) {
        String jpql;
        Map m = new HashMap();
        Vtm nvtm;
        if (name == null || name.trim().isEmpty()) {
            return null;
        } else {
            m.put("ret", false);
            m.put("name", name);
            jpql = "select c "
                    + " from Vtm c "
                    + " where c.retired=:ret "
                    + " and c.name=:name";
            nvtm = getFacade().findFirstByJpql(jpql, m);
        }
        if (nvtm == null) {
            nvtm = new Vtm();
            nvtm.setName(name);
            nvtm.setCode(CommonFunctions.nameToCode("vtm_" + name));
            getFacade().create(nvtm);
        }
        return nvtm;
    }

    public boolean isBilledAs() {
        return billedAs;
    }

    public void setBilledAs(boolean billedAs) {
        this.billedAs = billedAs;
    }

    public boolean isReportedAs() {
        return reportedAs;
    }

    public void setReportedAs(boolean reportedAs) {
        this.reportedAs = reportedAs;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public String getBulkText() {

        return bulkText;
    }

    public void setBulkText(String bulkText) {
        this.bulkText = bulkText;
    }

    public List<Vtm> getSelectedItems() {
        if (selectText == null || selectText.trim().isEmpty()) {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                    + "AND (c.departmentType IS NULL OR c.departmentType=:dep) "
                    + "ORDER BY c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Pharmacy);
            selectedItems = getFacade().findByJpql(sql, params);
        } else {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                    + "AND UPPER(c.name) LIKE :query "
                    + "AND (c.departmentType IS NULL OR c.departmentType=:dep) "
                    + "ORDER BY c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("query", "%" + getSelectText().toUpperCase() + "%");
            params.put("dep", DepartmentType.Pharmacy);
            selectedItems = getFacade().findByJpql(sql, params);
        }
        return selectedItems;
    }

    public void fillItems() {
        String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                + "AND (c.departmentType IS NULL OR c.departmentType=:dep) "
                + "ORDER BY c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("dep", DepartmentType.Pharmacy);
        items = getFacade().findByJpql(sql, params);
    }

    public void prepareAdd() {
        current = new Vtm();
        current.setDepartmentType(DepartmentType.Pharmacy);
        selectedVtmDto = null;
        editable = true;
    }

    public void edit() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VTM to edit");
            return;
        }
        if (current.isInactive()) {
            JsfUtil.addWarningMessage("Editing inactive VTM '" + current.getName() + "'");
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        selectedVtmDto = null;
        editable = false;
    }

    public void bulkUpload() {
        String[] lstLines = getBulkText().split("\\r?\\n");
        for (String s : lstLines) {
            List<String> w = Arrays.asList(s.split(","));
            try {
                String code = w.get(0);
                String ix = w.get(1);
                String ic = w.get(2);
                String f = w.get(4);
                //////// // System.out.println(code + " " + ix + " " + ic + " " + f);

                Vtm tix = new Vtm(); // TODO : Why ?
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartment(null);

            } catch (Exception ignored) {
            }
        }
    }

    public void setSelectedItems(List<Vtm> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    public void saveSelected() {
        try {
            if (getCurrent().getId() != null && getCurrent().getId() > 0) {
                // UPDATE - capture before state for audit
                Vtm beforeUpdate = getFacade().find(getCurrent().getId());
                Map<String, Object> beforeData = createAuditMap(beforeUpdate);

                if (!billedAs) {
                    //////// // System.out.println("2");
                    getCurrent().setBilledAs(getCurrent());

                }
                if (!reportedAs) {
                    getCurrent().setReportedAs(getCurrent());
                }
                getFacade().edit(getCurrent());

                // Log audit for update
                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "Vtm", "Update VTM", getCurrent().getId());

                JsfUtil.addSuccessMessage("Updated Successfully.");
            } else {
                // CREATE - no before state
                getCurrent().setCreatedAt(new Date());
                getCurrent().setCreater(getSessionController().getLoggedUser());
                getFacade().create(getCurrent());
                if (!billedAs) {
                    getCurrent().setBilledAs(getCurrent());
                }
                if (!reportedAs) {
                    getCurrent().setReportedAs(getCurrent());
                }
                getFacade().edit(getCurrent());

                // Log audit for create
                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(null, afterData,
                        getSessionController().getLoggedUser(),
                        "Vtm", "Create VTM", getCurrent().getId());

                JsfUtil.addSuccessMessage("Saved Successfully");
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving VTM '" + (getCurrent().getName() != null ? getCurrent().getName() : "Unknown") + "': " + e.getMessage());
            e.printStackTrace();
        }
        items = null;
        getItems();
        editable = false;
    }

    public void save() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }

        // Validate before saving
        if (!validateVtm()) {
            return;
        }

        try {
            boolean isNewVtm = getCurrent().getId() == null;

            if (getCurrent().getId() != null) {
                // UPDATE - capture before state
                Vtm beforeUpdate = getFacade().find(getCurrent().getId());
                Map<String, Object> beforeData = createAuditMap(beforeUpdate);

                getFacade().edit(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(beforeData, afterData,
                        getSessionController().getLoggedUser(),
                        "Vtm", "Update VTM", getCurrent().getId());

                JsfUtil.addSuccessMessage("VTM '" + getCurrent().getName() + "' updated successfully");
            } else {
                // CREATE - no before state
                getCurrent().setCreatedAt(new Date());
                getCurrent().setCreater(getSessionController().getLoggedUser());
                getFacade().create(getCurrent());

                Map<String, Object> afterData = createAuditMap(getCurrent());
                auditService.logAudit(null, afterData,
                        getSessionController().getLoggedUser(),
                        "Vtm", "Create VTM", getCurrent().getId());

                JsfUtil.addSuccessMessage("VTM '" + getCurrent().getName() + "' created successfully");

                // For new VTMs, create and set the DTO to show it's automatically available
                if (isNewVtm && getCurrent().getId() != null) {
                    selectedVtmDto = createVtmDto(getCurrent());
                }
            }
            editable = false;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving VTM '" + (getCurrent().getName() != null ? getCurrent().getName() : "Unknown") + "': " + e.getMessage());
            e.printStackTrace();
        }

        items = null;
        clearDtoCache(); // Clear DTO cache to reflect changes
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
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

    public VtmController() {
    }

    public Vtm getCurrent() {
        if (current == null) {
            current = new Vtm();
        }
        return current;
    }

    public void setCurrent(Vtm current) {
        this.current = current;
        if (current != null) {
            if (current.getBilledAs() == current) {
                billedAs = false;
            } else {
                billedAs = true;
            }
            if (current.getReportedAs() == current) {
                reportedAs = false;
            } else {
                reportedAs = true;
            }
        }
    }

    public void delete() {
        if (current != null) {
            String vtmName = current.getName();
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
                        "Vtm", "Delete VTM", current.getId());

                JsfUtil.addSuccessMessage("VTM '" + (vtmName != null ? vtmName : "Unknown") + "' deleted successfully");
            } catch (Exception e) {
                JsfUtil.addErrorMessage("Error deleting VTM '" + (vtmName != null ? vtmName : "Unknown") + "': " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            JsfUtil.addErrorMessage("No VTM selected for deletion");
        }
        current = null;
        selectedVtmDto = null;
        items = null;
        clearDtoCache(); // Clear DTO cache to reflect changes
        getItems();
        getCurrent();
        editable = false;
    }

    private VtmFacade getFacade() {
        return ejbFacade;
    }

    public List<Vtm> getItems() {
        if (items == null) {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired "
                    + "AND (c.departmentType IS NULL OR c.departmentType=:dep) ";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Pharmacy);

            // Apply status filter on inactive field
            if ("active".equals(filterStatus)) {
                sql += "AND c.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                sql += "AND c.inactive=:inactive ";
                params.put("inactive", true);
            }
            // For "all", no additional inactive filter

            sql += "ORDER BY c.name";
            items = getFacade().findByJpql(sql, params);
        }
        return items;
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // DTO Methods - Optimized for display and reporting
    /**
     * Get VTMs as DTOs for display - optimized query
     */
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
                    + "COALESCE(CAST(v.departmentType AS string), 'Pharmacy')) "
                    + "FROM Vtm v WHERE v.retired=:retired "
                    + "AND (v.departmentType IS NULL OR v.departmentType=:dep) ";

            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Pharmacy);

            // Apply status filter on inactive field
            if ("active".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "AND v.inactive=:inactive ";
                params.put("inactive", true);
            }

            jpql += "ORDER BY v.name";

            vtmDtoList = (List<VtmDto>) getFacade().findLightsByJpql(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
        }
        return vtmDtoList;
    }

    /**
     * Autocomplete method for VTM DTOs - optimized for UI
     * @param query
     * @return 
     */
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
                + "AND (v.departmentType=:dep) ";

        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("query", "%" + query + "%");
        params.put("dep", DepartmentType.Pharmacy);

        // Apply status filter on inactive field
        if ("active".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", true);
        }

        jpql += "ORDER BY v.name";

        try {
            List<VtmDto> results = (List<VtmDto>) getFacade().findLightsByJpql(jpql, params, javax.persistence.TemporalType.TIMESTAMP);
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Clear DTO cache - call when data changes
     */
    public void clearDtoCache() {
        vtmDtoList = null;
    }

    /**
     * Create DTO from entity - helper method for displaying newly saved VTMs
     */
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

    /**
     * Navigation method: Load entity from selected DTO
     */
    public String navigateToVtmFromDto(VtmDto dto) {
        if (dto != null && dto.getId() != null) {
            // Load full entity for business operations
            this.current = getFacade().find(dto.getId());
        }
        return "/pharmacy/admin/vtm?faces-redirect=true";
    }

    // DTO Property accessors
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

        // Sync with entity if DTO is selected
        if (selectedVtmDto != null && selectedVtmDto.getId() != null) {
            this.current = getFacade().find(selectedVtmDto.getId());
        } else {
            // Clear current entity when no DTO is selected
            this.current = null;
        }
    }

    // Audit Events Methods

    public List<AuditEvent> getVtmAuditEvents() {
        return vtmAuditEvents;
    }

    public void setVtmAuditEvents(List<AuditEvent> vtmAuditEvents) {
        this.vtmAuditEvents = vtmAuditEvents;
    }

    /**
     * Load audit events for the selected VTM
     * Uses simplified query similar to general audit events page
     */
    public void fillVtmAuditEvents() {
        vtmAuditEvents = new ArrayList<>();
        if (current != null && current.getId() != null) {
            // Use simplified query - only filter by objectId, similar to general audit page approach
            String jpql = "select a from AuditEvent a " +
                         "where a.objectId = :objectId " +
                         "order by a.eventDataTime desc";

            Map<String, Object> params = new HashMap<>();
            params.put("objectId", current.getId());

            vtmAuditEvents = auditEventFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

            // Calculate differences for display (like the general audit page does)
            for (AuditEvent ae : vtmAuditEvents) {
                ae.calculateDifference();
            }

        }
    }

    /**
     * Get all audit events for a VTM regardless of event trigger - for debugging
     */
    private List<AuditEvent> getAllAuditEventsForVtm(Long vtmId) {
        String jpql = "select a from AuditEvent a " +
                     "where a.objectId = :id " +
                     "and (a.entityType = :entityType OR a.entityType is null) " +
                     "order by a.eventDataTime desc";

        Map<String, Object> params = new HashMap<>();
        params.put("id", vtmId);
        params.put("entityType", "Vtm");

        return auditEventFacade.findByJpql(jpql, params);
    }


    /**
     * Navigate to VTM audit events page
     * Prepares audit data and navigates to the audit events page
     */
    public String navigateToAuditEvents() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VTM first");
            return null;
        }

        // Load audit events for the current VTM
        fillVtmAuditEvents();

        // Navigate to audit events page
        return "/pharmacy/admin/vtm_audit_events?faces-redirect=true";
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
                return "Active VTMs";
            case "inactive":
                return "Inactive VTMs";
            case "all":
                return "All VTMs";
            default:
                return "Active VTMs";
        }
    }

    // ===================== Toggle Status Methods =====================

    public void toggleVtmStatus() {
        if (current == null) {
            JsfUtil.addErrorMessage("No VTM selected");
            return;
        }

        // Capture before state for audit
        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("VTM Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("VTM Deactivated Successfully");
        }

        getFacade().edit(current);

        // Log audit for status change
        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate VTM" : "Deactivate VTM";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "Vtm", action, current.getId());

        // Refresh displays
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

    // Entity Methods (existing) - Keep for backward compatibility and business logic
    private Map<String, Object> createAuditMap(Vtm vtm) {
        Map<String, Object> auditData = new HashMap<>();
        if (vtm != null) {
            auditData.put("id", vtm.getId());
            auditData.put("name", vtm.getName());
            auditData.put("code", vtm.getCode());
            auditData.put("retired", vtm.isRetired());
            auditData.put("inactive", vtm.isInactive());
            auditData.put("departmentType", vtm.getDepartmentType() != null ? vtm.getDepartmentType().toString() : null);
            auditData.put("descreption", vtm.getDescreption()); // Note: intentional spelling for backward compatibility
            auditData.put("instructions", vtm.getInstructions());
        }
        return auditData;
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

    private boolean validateVtm() {
        if (current == null) {
            JsfUtil.addErrorMessage("No VTM selected for validation");
            return false;
        }

        // Validate name
        if (current.getName() == null || current.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("VTM name is required");
            return false;
        }

        // Check name uniqueness
        if (checkVtmName(current.getName(), current)) {
            JsfUtil.addErrorMessage("A VTM with this name already exists");
            return false;
        }

        // Check code uniqueness if provided
        if (current.getCode() != null && !current.getCode().trim().isEmpty()) {
            if (checkVtmCode(current.getCode(), current)) {
                JsfUtil.addErrorMessage("A VTM with this code already exists");
                return false;
            }
        }

        // Validate department type consistency
        if (current.getDepartmentType() != null && current.getDepartmentType() != DepartmentType.Pharmacy) {
            JsfUtil.addWarningMessage("VTM department type should be Pharmacy for proper categorization");
        }

        return true;
    }

    /**
     * Converter for VtmDto objects - Fixed implementation
     */
    @FacesConverter("vtmDtoConverter")
    public static class VtmDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            System.out.println("VtmDtoConverter.getAsObject called with value: '" + value + "'");

            if (value == null || value.isEmpty()) {
                System.out.println("Value is null or empty, returning null");
                return null;
            }

            try {
                Long id = Long.parseLong(value);
                System.out.println("Parsed ID: " + id);

                // Get controller instance
                VtmController controller = (VtmController) facesContext.getApplication().getELResolver().
                        getValue(facesContext.getELContext(), null, "vtmController");

                if (controller == null) {
                    System.out.println("Controller not found!");
                    return null;
                }

                // Load entity from database and convert to DTO
                Vtm entity = controller.getFacade().find(id);
                if (entity != null) {
                    VtmDto dto = controller.createVtmDto(entity);
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

            if (object instanceof VtmDto) {
                VtmDto dto = (VtmDto) object;
                String result = dto.getId() != null ? dto.getId().toString() : null;
                System.out.println("VtmDtoConverter.getAsString: " + dto.getName() + " -> " + result);
                return result;
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + VtmDto.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = Vtm.class)
    public static class VtmControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            VtmController controller = (VtmController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vtmController");
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
            if (object instanceof Vtm) {
                Vtm o = (Vtm) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Vtm.class.getName());
            }
        }
    }

}

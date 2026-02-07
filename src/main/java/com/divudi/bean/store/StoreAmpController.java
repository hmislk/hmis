/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.store;

import com.divudi.bean.common.CategoryController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.ItemType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.VmpFacade;
import com.divudi.core.data.dto.AmpDto;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.bean.common.AuditEventController;
import com.divudi.core.entity.AuditEvent;
import com.divudi.service.AuditService;

import java.io.Serializable;
import java.text.DecimalFormat;
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
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class StoreAmpController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AmpFacade ejbFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    private VmpFacade vmpFacade;
    @EJB
    PharmacyBean pharmacyBean;

    private Amp current;
    private List<Amp> items = null;
    private List<Amp> itemsAll = null;
    List<Amp> itemsByCode = null;

    @Inject
    SessionController sessionController;
    @Inject
    CategoryController categoryController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    // Audit-related injections
    @Inject
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;

    private boolean duplicateCode;
    private boolean editable;

    // DTO Management fields
    private AmpDto selectedAmpDto;
    private List<AmpDto> ampDtos;
    private List<AuditEvent> ampAuditEvents;

    // Filter state for active/inactive AMPs.
    // "active"   -> inactive=false  (items in day-to-day use)
    // "inactive" -> inactive=true   (temporarily hidden, user can reactivate)
    // "all"      -> no inactive filter (shows both active and inactive)
    // NOTE: All three modes always enforce retired=false. Retired items are
    // permanently deleted and never appear in any listing or search.
    private String filterStatus = "active";

    // ========== Backward-compatibility methods ==========

    public List<Amp> getItemsByCode() {
        if (itemsByCode == null) {
            Map<String, Object> m = new HashMap<>();
            m.put("dep", DepartmentType.Store);
            itemsByCode = getFacade().findByJpql(
                    "select a from Amp a where a.retired=false and a.departmentType=:dep order by a.code", m);
        }
        return itemsByCode;
    }

    public void setItemsByCode(List<Amp> itemsByCode) {
        this.itemsByCode = itemsByCode;
    }

    public List<Amp> getItemsAll() {
        if (itemsAll == null) {
            itemsAll = new ArrayList<>();
        }
        return itemsAll;
    }

    public void setItemsAll(List<Amp> itemsAll) {
        this.itemsAll = itemsAll;
    }

    public void createStoreItemsWithRetierd() {
        items = null;
        getItems();
        itemsAll = new ArrayList<>();
        itemsAll.addAll(items);
        Map<String, Object> m = new HashMap<>();
        m.put("dt", DepartmentType.Store);
        String sql = "Select a from Amp a where a.retired=true and a.departmentType=:dt order by a.name";
        itemsAll.addAll(getFacade().findByJpql(sql, m));
    }

    public void recreateModel() {
        items = null;
        itemsByCode = null;
        itemsAll = null;
    }

    // ========== Core CRUD Methods ==========

    public void prepareAdd() {
        current = new Amp();
        current.setItemType(ItemType.Amp);
        current.setDepartmentType(DepartmentType.Store);
        selectedAmpDto = null;
        ampAuditEvents = null;
        editable = true;
    }

    public void edit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to edit");
            return;
        }
        if (current.getId() != null) {
            selectedAmpDto = createAmpDto(current);
        }
        editable = true;
    }

    public void prepareEdit() {
        edit();
    }

    public void cancel() {
        current = null;
        selectedAmpDto = null;
        ampAuditEvents = null;
        editable = false;
    }

    public void listnerCategorySelect() {
        if (getCurrent().getCategory() == null) {
            JsfUtil.addErrorMessage("Please Select Category");
            getCurrent().setCode("");
            return;
        }
        if (getCurrent().getCategory().getCode() == null || getCurrent().getCategory().getCode().isEmpty()) {
            JsfUtil.addErrorMessage("Please Select Category Code");
            getCurrent().setCode("");
            return;
        }

        Map m = new HashMap();
        String sql = "select c from Amp c "
                + " where c.retired=false"
                + " and c.category=:cat "
                + " and c.departmentType=:dep "
                + " order by c.code desc";

        m.put("dep", DepartmentType.Store);
        m.put("cat", getCurrent().getCategory());

        Amp amp = getFacade().findFirstByJpql(sql, m);

        DecimalFormat df = new DecimalFormat("0000");
        if (amp != null && !amp.getCode().isEmpty()) {
            String s = amp.getCode().substring(getCurrent().getCategory().getCode().length());
            int i = Integer.valueOf(s);
            i++;
            if (getCurrent().getId() != null) {
                Amp selectedAmp = getFacade().find(getCurrent().getId());
                if (!getCurrent().getCategory().equals(selectedAmp.getCategory())) {
                    getCurrent().setCode(getCurrent().getCategory().getCode() + df.format(i));
                } else {
                    getCurrent().setCode(selectedAmp.getCode());
                }
            } else {
                getCurrent().setCode(getCurrent().getCategory().getCode() + df.format(i));
            }
        } else {
            getCurrent().setCode(getCurrent().getCategory().getCode() + df.format(1));
        }
    }

    public void save() {
        if (current == null) {
            JsfUtil.addErrorMessage("No AMP is selected");
            return;
        }

        if (current.getId() != null) {
            if (!configOptionApplicationController.getBooleanValueByKey("Enable edit and delete AMP from Pharmacy Administration.", false)) {
                JsfUtil.addErrorMessage("Deleting and Editing is disabled by Configuration Options.");
                return;
            }
        }

        if (current.getName() == null || current.getName().isEmpty()) {
            JsfUtil.addErrorMessage("Please add a name to AMP");
            return;
        }

        int maxCodeLength = Integer.parseInt(configOptionApplicationController.getShortTextValueByKey("Minimum Number of Characters to Search for Item", "4"));

        if (current.getCode().trim().length() < maxCodeLength) {
            JsfUtil.addErrorMessage("Minimum " + maxCodeLength + " characters are Required for Item Code");
            return;
        }

        if (checkItemCode(current.getCode(), current)) {
            JsfUtil.addErrorMessage("This Code has Already been Used.");
            return;
        }

        // Set department type to Store for new AMPs
        if (current.getDepartmentType() == null) {
            current.setDepartmentType(DepartmentType.Store);
        }

        if (current.getVmp() == null) {
            JsfUtil.addErrorMessage("No VMP selected");
            return;
        }

        if (current.getCategory() == null) {
            if (current.getVmp().getCategory() != null) {
                current.setCategory(current.getVmp().getCategory());
                JsfUtil.addSuccessMessage("Taken the category from VMP");
            } else {
                JsfUtil.addErrorMessage("No category");
                return;
            }
        }

        if (current.getItemType() == null) {
            current.setItemType(ItemType.Amp);
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            // UPDATE
            Amp beforeUpdate = getFacade().find(getCurrent().getId());
            Map<String, Object> beforeData = createAuditMap(beforeUpdate);

            getCurrent().setEditedAt(new Date());
            getCurrent().setEditer(getSessionController().getLoggedUser());
            getFacade().edit(getCurrent());

            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(beforeData, afterData,
                    getSessionController().getLoggedUser(),
                    "StoreAmp", "Update Store AMP", getCurrent().getId());

            JsfUtil.addSuccessMessage("Store AMP Updated Successfully.");
        } else {
            // CREATE
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getCurrent().setItemType(ItemType.Amp);
            getFacade().create(getCurrent());

            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(null, afterData,
                    getSessionController().getLoggedUser(),
                    "StoreAmp", "Create Store AMP", getCurrent().getId());

            JsfUtil.addSuccessMessage("Store AMP Created Successfully.");
        }

        recreateModel();
        getItems();
        selectedAmpDto = createAmpDto(current);
        ampDtos = null;
        editable = false;
    }

    public boolean checkItemCode(String code, Amp savingAmp) {
        if (savingAmp == null) {
            return false;
        }
        Map m = new HashMap();
        String jpql = "select c from Amp c "
                + " where c.retired=false"
                + " and (c.code is not null and c.code=:icode)";
        if (savingAmp.getId() != null) {
            jpql += " and c.id <> :id ";
            m.put("id", savingAmp.getId());
        }
        m.put("icode", code);
        Amp amp = getFacade().findFirstByJpql(jpql, m);
        return amp != null;
    }

    public void checkCodeDuplicate() {
        duplicateCode = checkItemCode(current.getCode(), current);
        if (duplicateCode) {
            JsfUtil.addErrorMessage("This Code has Already been Used.");
        }
    }

    public void generateCode() {
        int length = configOptionApplicationController.getIntegerValueByKey("AMP_CODE_LENGTH", 4);
        String code;

        if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_NUMERIC_ONLY")) {
            code = generateNumericCode(length);
        } else if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_CHARACTERS_ONLY")) {
            code = generateCharacterCode(length);
        } else if (configOptionApplicationController.getBooleanValueByKey("AMP_CODE_ALPHANUMERIC")) {
            code = generateAlphaNumericCode(length);
        } else {
            code = generateNumericCode(length);
            JsfUtil.addSuccessMessage("Generated numeric code (default mode). Configure AMP_CODE_* options for other formats.");
        }

        if (code != null && !code.trim().isEmpty()) {
            current.setCode(code);
            checkCodeDuplicate();
            if (!duplicateCode) {
                JsfUtil.addSuccessMessage("Unique code generated successfully: " + code);
            }
        } else {
            JsfUtil.addErrorMessage("Failed to generate code. Please check configuration.");
        }
    }

    private String generateNumericCode(int length) {
        long max = 0;
        List<Amp> all = findItems();
        for (Amp a : all) {
            try {
                long val = Long.parseLong(a.getCode());
                if (val > max) {
                    max = val;
                }
            } catch (Exception e) {
            }
        }
        long next = max + 1;
        String format = "%0" + length + "d";
        String code = String.format(format, next);
        while (checkItemCode(code, current)) {
            next++;
            code = String.format(format, next);
        }
        return code;
    }

    private String generateCharacterCode(int length) {
        String base = generateShortCode(current.getName());
        if (base.isEmpty()) {
            base = "AMP";
        }
        if (base.length() > length) {
            base = base.substring(0, length);
        }
        String code = base.toUpperCase();
        int index = 1;
        while (checkItemCode(code, current)) {
            String suffix = String.valueOf(index);
            int cut = Math.max(0, length - suffix.length());
            String prefix = base.length() > cut ? base.substring(0, cut) : base;
            code = (prefix + suffix).toUpperCase();
            index++;
        }
        if (code.length() > length) {
            code = code.substring(0, length);
        }
        return code;
    }

    private String generateAlphaNumericCode(int length) {
        String base = generateShortCode(current.getName()).toUpperCase();
        if (base.length() >= length) {
            base = base.substring(0, length - 1);
        }
        int digits = Math.max(1, length - base.length());
        long index = 1;
        String code;
        String format = "%0" + digits + "d";
        code = base + String.format(format, index);
        while (checkItemCode(code, current)) {
            index++;
            code = base + String.format(format, index);
        }
        return code;
    }

    private String generateShortCode(String name) {
        StringBuilder sc = new StringBuilder();
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String[] words = name.split(" ");
        if (words.length == 1 && words[0].length() >= 3) {
            sc = new StringBuilder(words[0].substring(0, 3).toLowerCase());
        } else {
            for (String w : words) {
                if (!w.isEmpty()) {
                    sc.append(w.charAt(0));
                }
            }
            sc = new StringBuilder(sc.toString().toLowerCase());
        }
        return sc.toString();
    }

    /**
     * Permanently soft-deletes the current AMP by setting retired=true.
     * Retired items are excluded from every query and cannot be restored
     * from the UI. For temporary removal use toggleAmpStatus() which
     * sets inactive instead.
     */
    public void delete() {
        if (current != null) {
            Map<String, Object> beforeData = createAuditMap(current);

            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);

            Map<String, Object> afterData = createAuditMap(current);
            auditService.logAudit(beforeData, afterData,
                    getSessionController().getLoggedUser(),
                    "StoreAmp", "Delete Store AMP", current.getId());

            JsfUtil.addSuccessMessage("Store AMP Deleted Successfully");
        } else {
            JsfUtil.addErrorMessage("No AMP Selected to Delete");
        }

        recreateModel();
        getItems();
        current = null;
        selectedAmpDto = null;
        ampAuditEvents = null;
        getCurrent();
        editable = false;
    }

    // ========== Getters/Setters ==========

    public StoreAmpController() {
    }

    public AmpFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(AmpFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    private AmpFacade getFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Amp getCurrent() {
        if (current == null) {
            current = new Amp();
            current.setDepartmentType(DepartmentType.Store);
        }
        return current;
    }

    public void setCurrent(Amp current) {
        this.current = current;
    }

    private List<Amp> filteredItems;

    public List<Amp> getItems() {
        if (items == null) {
            String jpql;
            Map<String, Object> params = new HashMap<>();
            jpql = "select a "
                    + " from Amp a "
                    + " where a.retired=false "
                    + " and a.departmentType=:dep ";

            params.put("dep", DepartmentType.Store);

            if ("active".equals(filterStatus)) {
                jpql += "and a.inactive=:inact ";
                params.put("inact", false);
            } else if ("inactive".equals(filterStatus)) {
                jpql += "and a.inactive=:inact ";
                params.put("inact", true);
            }

            jpql += "order by a.name";
            items = getFacade().findByJpql(jpql, params);
        }
        return items;
    }

    public void setItems(List<Amp> items) {
        this.items = items;
    }

    public List<Amp> findItems() {
        String jpql = "select a "
                + " from Amp a "
                + " where a.retired=:ret"
                + " and a.departmentType=:dep"
                + " order by a.name";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("dep", DepartmentType.Store);
        return getFacade().findByJpql(jpql, params);
    }

    public List<Vmp> completeVmpByName(String qry) {
        List<Vmp> vmps = new ArrayList<>();
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        if (qry != null) {
            vmps = vmpFacade.findByJpql("select c from Vmp c where "
                    + " c.retired=false and"
                    + " ((c.name) like :n ) order by c.name", m, 30);
        }
        return vmps;
    }

    public List<Amp> getFilteredItems() {
        return filteredItems;
    }

    public void setFilteredItems(List<Amp> filteredItems) {
        this.filteredItems = filteredItems;
    }

    public boolean isDuplicateCode() {
        return duplicateCode;
    }

    public void setDuplicateCode(boolean duplicateCode) {
        this.duplicateCode = duplicateCode;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    // ===================== DTO Management Methods =====================

    public List<AmpDto> getAmpDtos() {
        String jpql = "SELECT new com.divudi.core.data.dto.AmpDto("
                + "a.id, a.name, a.code, a.barcode, a.retired, "
                + "a.vmp.id, a.vmp.name) "
                + "FROM Amp a WHERE a.retired=false "
                + "AND a.departmentType=:dep ";

        Map<String, Object> params = new HashMap<>();
        params.put("dep", DepartmentType.Store);

        if ("active".equals(filterStatus)) {
            jpql += "AND a.inactive=:inact ";
            params.put("inact", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND a.inactive=:inact ";
            params.put("inact", true);
        }

        jpql += "ORDER BY a.name";

        return (List<AmpDto>) getFacade().findLightsByJpql(jpql, params);
    }

    public List<AmpDto> completeAmpDto(String query) {
        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }

        String jpql = "SELECT new com.divudi.core.data.dto.AmpDto("
                + "a.id, a.name, a.code, a.barcode, a.retired, "
                + "a.vmp.id, a.vmp.name) "
                + "FROM Amp a WHERE a.retired=false "
                + "AND LOWER(a.name) LIKE :query "
                + "AND a.departmentType=:dep ";

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toLowerCase() + "%");
        params.put("dep", DepartmentType.Store);

        if ("active".equals(filterStatus)) {
            jpql += "AND a.inactive=:inact ";
            params.put("inact", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND a.inactive=:inact ";
            params.put("inact", true);
        }

        jpql += "ORDER BY a.name";

        return (List<AmpDto>) getFacade().findLightsByJpql(jpql, params);
    }

    public void setSelectedAmpDto(AmpDto selectedAmpDto) {
        this.selectedAmpDto = selectedAmpDto;
        if (selectedAmpDto != null && selectedAmpDto.getId() != null) {
            this.current = getFacade().find(selectedAmpDto.getId());
        } else {
            this.current = null;
        }
    }

    public AmpDto getSelectedAmpDto() {
        return selectedAmpDto;
    }

    public AmpDto createAmpDto(Amp amp) {
        if (amp == null) {
            return null;
        }
        return new AmpDto(amp.getId(), amp.getName(), amp.getCode(),
                amp.getBarcode(), amp.isRetired(),
                amp.getVmp() != null ? amp.getVmp().getId() : null,
                amp.getVmp() != null ? amp.getVmp().getName() : null);
    }

    // ===================== Filter Status Management =====================

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
        ampDtos = null;

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
                selectedAmpDto = null;
                ampAuditEvents = null;
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
                return "Active Store AMPs";
            case "inactive":
                return "Inactive Store AMPs";
            case "all":
                return "All Store AMPs";
            default:
                return "Active Store AMPs";
        }
    }

    // ===================== Audit Management Methods =====================

    private Map<String, Object> createAuditMap(Amp amp) {
        Map<String, Object> auditData = new HashMap<>();
        if (amp != null) {
            auditData.put("id", amp.getId());
            auditData.put("name", amp.getName());
            auditData.put("code", amp.getCode());
            auditData.put("barcode", amp.getBarcode());
            auditData.put("retired", amp.isRetired());
            auditData.put("inactive", amp.isInactive());
            auditData.put("departmentType", amp.getDepartmentType() != null ?
                    amp.getDepartmentType().toString() : null);
            auditData.put("vmpId", amp.getVmp() != null ? amp.getVmp().getId() : null);
            auditData.put("vmpName", amp.getVmp() != null ? amp.getVmp().getName() : null);
            auditData.put("atmId", amp.getAtm() != null ? amp.getAtm().getId() : null);
            auditData.put("atmName", amp.getAtm() != null ? amp.getAtm().getName() : null);
            auditData.put("categoryId", amp.getCategory() != null ? amp.getCategory().getId() : null);
            auditData.put("categoryName", amp.getCategory() != null ? amp.getCategory().getName() : null);
            auditData.put("itemType", amp.getItemType() != null ? amp.getItemType().toString() : null);
            auditData.put("discountAllowed", amp.isDiscountAllowed());
            auditData.put("refundsAllowed", amp.isRefundsAllowed());
            auditData.put("consumptionAllowed", amp.isConsumptionAllowed());
            auditData.put("allowFractions", amp.isAllowFractions());
            auditData.put("strengthOfAnIssueUnit", amp.getStrengthOfAnIssueUnit());
            auditData.put("strengthUnit", amp.getStrengthUnit());
            auditData.put("issueUnit", amp.getIssueUnit());
        }
        return auditData;
    }

    /**
     * Toggles the temporary active/inactive status of the current AMP.
     * This sets the 'inactive' flag, NOT 'retired'. The item remains in
     * the system and can be reactivated at any time by the user.
     * For permanent removal see delete() which sets retired=true.
     */
    public void toggleAmpStatus() {
        if (current == null) {
            JsfUtil.addErrorMessage("No AMP selected");
            return;
        }

        Map<String, Object> beforeData = createAuditMap(current);
        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            current.setInactive(false);
            JsfUtil.addSuccessMessage("Store AMP Activated Successfully");
        } else {
            current.setInactive(true);
            JsfUtil.addSuccessMessage("Store AMP Deactivated Successfully");
        }

        getFacade().edit(current);

        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate Store AMP" : "Deactivate Store AMP";
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "StoreAmp", action, current.getId());

        recreateModel();
        ampDtos = null;
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

    // ===================== Audit History Management =====================

    public void fillAmpAuditEvents() {
        if (current != null && current.getId() != null) {
            try {
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId "
                        + "AND a.entityType = :entityType ORDER BY a.eventDataTime DESC";
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("objectId", current.getId());
                parameters.put("entityType", "StoreAmp");

                ampAuditEvents = auditEventFacade.findByJpql(jpql, parameters);
            } catch (Exception e) {
                ampAuditEvents = new ArrayList<>();
                JsfUtil.addErrorMessage("Error loading audit history: " + e.getMessage());
            }
        } else {
            ampAuditEvents = new ArrayList<>();
        }
    }

    public String navigateToAmpAuditEvents() {
        fillAmpAuditEvents();
        return "/pharmacy/admin/store_amp_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getAmpAuditEvents() {
        if (ampAuditEvents == null) {
            fillAmpAuditEvents();
        }
        return ampAuditEvents;
    }

    public void refreshAuditEvents() {
        ampAuditEvents = null;
        fillAmpAuditEvents();
    }

    // ===================== JSF AmpDto Converter =====================

    @FacesConverter("storeAmpDtoConverter")
    public static class StoreAmpDtoConverter implements Converter {
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                StoreAmpController controller = (StoreAmpController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "storeAmpController");

                Amp entity = controller.getFacade().find(id);
                return entity != null ? controller.createAmpDto(entity) : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof AmpDto) {
                AmpDto dto = (AmpDto) object;
                return dto.getId() != null ? dto.getId().toString() : null;
            }
            throw new IllegalArgumentException("Expected AmpDto object");
        }
    }

    @FacesConverter("stoAmpCon")
    public static class AmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StoreAmpController controller = (StoreAmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "storeAmpController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Amp) {
                Amp o = (Amp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StoreAmpController.class.getName());
            }
        }
    }
}

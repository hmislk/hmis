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
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.SymanticType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.VmpDto;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.PharmaceuticalItem;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.entity.pharmacy.VirtualProductIngredient;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.VmpFacade;
import com.divudi.core.facade.VirtualProductIngredientFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.AuditService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
public class VmpController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VmpFacade ejbFacade;
    @EJB
    AmpFacade ampFacade;
    @EJB
    private SpecialityFacade specialityFacade;
    @Inject
    private BillBeanController billBean;
    @Inject
    private AuditService auditService;
    @EJB
    private AuditEventFacade auditEventFacade;
    @Inject
    private AuditEventController auditEventController;
    List<Vmp> selectedItems;
    private Vmp current;
    private List<Vmp> items = null;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    VirtualProductIngredient addingVtmInVmp;
    VirtualProductIngredient removingVtmInVmp;

    @EJB
    VirtualProductIngredientFacade vivFacade;
    List<VirtualProductIngredient> vivs;

    @EJB
    VmpFacade vmpFacade;
    private boolean editable;

    // DTO Management Fields
    private VmpDto selectedVmpDto;
    private List<VmpDto> vmpDtos;
    private List<AuditEvent> vmpAuditEvents;

    // Filter state for active/inactive VMPs
    private String filterStatus = "active"; // "active", "inactive", "all"

    public String navigateToListAllVmps() {
        String jpql = "Select vmp "
                + " from Vmp vmp "
                + " where vmp.inactive=:ret "
                + " order by vmp.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(jpql, m);
        return "/emr/reports/vmps?faces-redirect=true";
    }

    public Vmp findOrCreateVmpByName(String vmpName) {
        String jpql = "Select vmp "
                + " from Vmp vmp "
                + " where vmp.inactive=:ret "
                + " and vmp.name=:vmpName "
                + " order by vmp.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("vmpName", vmpName);
        Vmp vmp = getFacade().findFirstByJpql(jpql, m);
        if (vmp == null) {
            vmp = new Vmp();
            vmp.setName(vmpName);
            vmp.setDepartmentType(DepartmentType.Pharmacy); // Set to Pharmacy
            String vmpCode = CommonFunctions.nameToCode("vmp_" + vmpName);
            vmp.setSymanticType(SymanticType.Pharmacologic_Substance);
            getFacade().create(vmp);
        }
        return vmp;
    }

    public void cleanceVMPs() {
        items = ejbFacade.findAll();
        for (Vmp v : getItems()) {
            if (v.getName() == null) {
                return;
            }
            String strVmp = v.getName();
            strVmp = removeDuplicateWordsIgnoreCase(strVmp);
            strVmp = cleanVTMName(strVmp);
            strVmp = removeSpecificWords(strVmp, convertInputToArray(bulkText));
            strVmp = removeExactWords(strVmp, convertInputToArray(bulkText));
            v.setName(strVmp);
            getFacade().edit(v);
        }
    }

    public String removeDuplicateWordsIgnoreCase(String input) {
        String[] words = input.split("\\s+");

        LinkedHashSet<String> uniqueWords = new LinkedHashSet<>();
        for (String word : words) {
            uniqueWords.add(word.toLowerCase());
        }

        StringBuilder output = new StringBuilder();
        for (String word : words) {
            if (uniqueWords.contains(word.toLowerCase())) {
                output.append(word);
                output.append(" ");
                uniqueWords.remove(word.toLowerCase());
            }
        }

        return output.toString().trim();
    }

    public String cleanVTMName(String input) {
        String output;
        // Remove extra spaces
        output = input.replaceAll(" +", " ");
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

    public String removeExactWords(String input, String[] phrasesToRemove) {
        String output = input;

        for (String phrase : phrasesToRemove) {
            output = output.replaceAll("\\s*" + Pattern.quote(phrase) + "\\s*", " ").trim();
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

    public List<Vmp> completeVmp(String query) {
        List<Vmp> vmpList;
        if (query == null || query.trim().isEmpty()) {
            vmpList = new ArrayList<>();
        } else {
            String jpql = "SELECT c FROM Vmp c WHERE c.retired = false AND LOWER(c.name) LIKE :query AND c.departmentType=:dep ORDER BY c.name";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("query", "%" + query.trim().toLowerCase() + "%");
            parameters.put("dep", DepartmentType.Pharmacy);

            vmpList = getFacade().findByJpqlWithoutCache(jpql, parameters);
        }
        return vmpList;
    }

    public List<Amp> ampsOfVmp(Item vmp) {
        List<Amp> suggestions = new ArrayList<>();
        if (!(vmp instanceof Vmp)) {
            return suggestions;
        }

        String jpql;
        Map m = new HashMap();
        jpql = "select a from Amp a "
                + " where a.inactive=:ret"
                + " and a.vmp=:vmp "
                + " order by a.name";
        m.put("ret", false);
        m.put("vmp", vmp);
        suggestions = ampFacade.findByJpql(jpql, m);
        return suggestions;
    }

    public Vmp findVmpByName(String name) {
        String jpql;
        if (name == null) {
            return null;
        }
        jpql = "select c "
                + " from Vmp c "
                + " where c.inactive=:ret "
                + " and c.name=:name";
        Map m = new HashMap();
        m.put("name", name);
        m.put("ret", false);
        return getFacade().findFirstByJpql(jpql, m);
    }

    public List<PharmaceuticalItem> ampsAndVmpsContainingVtm(Item item) {
        List<PharmaceuticalItem> vmpsAndAmps = new ArrayList<>();
        if (!(item instanceof Vtm)) {
            return vmpsAndAmps;
        }

        Vtm vtm = (Vtm) item;
        List<Vmp> vmps = vmpsContainingVtm(vtm);
        if (vmps == null || vmps.isEmpty()) {
            return vmpsAndAmps;
        }
        vmpsAndAmps.addAll(vmps);
        for (Vmp v : vmps) {
            vmpsAndAmps.addAll(ampsOfVmp(v));
        }
        return vmpsAndAmps;
    }

    public List<Vmp> vmpsContainingVtm(Item item) {
        List<Vmp> vmps = new ArrayList<>();
        if (!(item instanceof Vtm)) {
            return vmps;
        }
        Vtm vtm = (Vtm) item;
        String j;
        Map m = new HashMap();
        j = "Select vmp "
                + " from VirtualProductIngredient viv join viv.vmp vmp"
                + " where viv.inactive=:ret "
                + " and viv.vtm=:vtm "
                + " order by vmp.name";
        m.put("ret", false);
        m.put("vtm", vtm);
        return vmpFacade.findByJpql(j, m);
    }

    public Vmp createVmp(String vmpName,
            Vtm vtm,
            Category dosageForm,
            Double strengthOfAnIssueUnit,
            MeasurementUnit strengthUnit,
            Double issueUnitsPerPack,
            MeasurementUnit packUnit,
            Double minimumIssueQuantity,
            MeasurementUnit minimumIssueQuantityUnit,
            Double issueMultipliesQuantity,
            MeasurementUnit issueMultipliesQuantityUnit) {
        Vmp v;
        v = findVmpByName(vmpName);
        if (v != null) {
            return v;
        }
        v = new Vmp();
        v.setName(vmpName);
        v.setCode("vmp_" + CommonFunctions.nameToCode(vmpName));
        v.setDepartmentType(DepartmentType.Pharmacy); // Set to Pharmacy
        v.setVtm(vtm);
        v.setDosageForm(dosageForm);
        v.setStrengthOfAnIssueUnit(strengthOfAnIssueUnit);
        v.setStrengthUnit(strengthUnit);
        v.setPackUnit(packUnit);
        v.setIssueUnitsPerPackUnit(issueUnitsPerPack);
        v.setMinimumIssueQuantity(minimumIssueQuantity);
        v.setMinimumIssueQuantityUnit(minimumIssueQuantityUnit);
        v.setIssueMultipliesQuantity(issueMultipliesQuantity);
        v.setIssueMultipliesUnit(issueMultipliesQuantityUnit);
        getFacade().create(v);
        return v;
    }

    public Vmp createVmp(String vmpName,
            Vtm vtm,
            Category dosageForm,
            MeasurementUnit strengthUnit,
            MeasurementUnit issueUnit,
            Double strengthUnitsPerIssueUnit,
            Double minimumIssueQuantity,
            Double issueMultipliesQuantity) {
        Vmp v;
        v = findVmpByName(vmpName);
        if (v != null) {
            return v;
        }
        v = new Vmp();
        v.setName(vmpName);
        v.setCode("vmp_" + CommonFunctions.nameToCode(vmpName));
        v.setDepartmentType(DepartmentType.Pharmacy); // Set to Pharmacy
        v.setVtm(vtm);
        v.setDosageForm(dosageForm);
        v.setStrengthUnit(strengthUnit);
        v.setIssueUnit(issueUnit);
        v.setStrengthOfAnIssueUnit(strengthUnitsPerIssueUnit);
        v.setMinimumIssueQuantity(minimumIssueQuantity);
        v.setIssueMultipliesQuantity(issueMultipliesQuantity);
        getFacade().create(v);
        return v;
    }

    public List<VirtualProductIngredient> getVivs() {
        if (getCurrent().getId() == null) {
            return new ArrayList<VirtualProductIngredient>();
        } else {
            Long currentId = getCurrent().getId();
            String jpqlQuery = "SELECT v FROM VirtualProductIngredient v WHERE v.vmp.id = :vmpId";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("vmpId", currentId);
            vivs = getVivFacade().findByJpql(jpqlQuery, parameters);

            if (vivs == null) {
                return new ArrayList<VirtualProductIngredient>();
            }

            return vivs;
        }
    }

    public String getVivsAsString(Vmp vmp) {
        return getVivsAsString(getVivs(vmp));
    }

    public String getVivsAsString(List<VirtualProductIngredient> gs) {
        String str = "";
        for (VirtualProductIngredient g : gs) {
            if (g.getVtm() == null || g.getVtm().getName() == null) {
                continue;
            }
            if ("".equals(str)) {
                str = g.getVtm().getName();
            } else {
                str = str + ", " + g.getVtm().getName();
            }
        }
        return str;
    }

    public List<VirtualProductIngredient> getVivs(Vmp vmp) {
        List<VirtualProductIngredient> gs;
        if (vmp == null) {
            return new ArrayList<>();
        } else {
            String j = "select v from VirtualProductIngredient v where v.vmp=:vmp";
            Map m = new HashMap();
            m.put("vmp", vmp);
            gs = getVivFacade().findByJpql(j, m);
            if (gs == null) {
                return new ArrayList<>();
            }
            return gs;
        }
    }

    public void remove() {
        getVivFacade().remove(removingVtmInVmp);
    }

    public void setVivs(List<VirtualProductIngredient> vivs) {
        this.vivs = vivs;
    }

    private boolean errorCheck() {
        if (addingVtmInVmp == null) {
            return true;
        }
        if (addingVtmInVmp.getVtm() == null) {
            JsfUtil.addErrorMessage("Select Vtm");
            return true;
        }
//        TODO:Message
        if (current == null) {
            return true;
        }
        if (addingVtmInVmp.getStrength() == 0.0) {
            JsfUtil.addErrorMessage("Type Strength");
            return true;
        }
        if (current.getCategory() == null) {
            JsfUtil.addErrorMessage("Select Category");
            return true;
        }
        if (addingVtmInVmp.getStrengthUnit() == null) {
            JsfUtil.addErrorMessage("Select Strenth Unit");
            return true;
        }

        return false;
    }

    public void addVtmInVmp() {
        if (errorCheck()) {
            return;
        }

        saveVmp();
        getAddingVtmInVmp().setVmp(current);
        getVivFacade().create(getAddingVtmInVmp());

        JsfUtil.addSuccessMessage("Added");

        addingVtmInVmp = null;

    }

    private void saveVmp() {
        if (current.getName() == null || current.getName().isEmpty()) {
            current.setName(createVmpName());
        }

        if (current.getId() == null || current.getId() == 0) {
            getFacade().create(current);
        } else {
            getFacade().edit(current);
        }

    }

    public String createVmpName() {
        return addingVtmInVmp.getVtm().getName() + " " + addingVtmInVmp.getStrength() + " " + addingVtmInVmp.getStrengthUnit().getName() + " " + current.getCategory().getName();
    }

    public VirtualProductIngredient getAddingVtmInVmp() {
        if (addingVtmInVmp == null) {
            addingVtmInVmp = new VirtualProductIngredient();
        }
        return addingVtmInVmp;
    }

    public void setAddingVtmInVmp(VirtualProductIngredient addingVtmInVmp) {
        this.addingVtmInVmp = addingVtmInVmp;
    }

    public VirtualProductIngredient getRemovingVtmInVmp() {
        return removingVtmInVmp;
    }

    public void setRemovingVtmInVmp(VirtualProductIngredient removingVtmInVmp) {
        this.removingVtmInVmp = removingVtmInVmp;
    }

    public VirtualProductIngredientFacade getVivFacade() {
        return vivFacade;
    }

    public void setVivFacade(VirtualProductIngredientFacade vivFacade) {
        this.vivFacade = vivFacade;
    }

    public List<Vmp> completeInvest(String query) {
        List<Vmp> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Vmp>();
        } else {
            sql = "select c from Vmp c where c.inactive=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql, true); // true = noCache
        }
        return suggestions;
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

    public List<Vmp> getSelectedItems() {
        if (selectText.trim().isEmpty()) {
            selectedItems = getFacade().findByJpql("select c from Vmp c where c.inactive=false order by c.name");
        } else {
            String sql = "select c from Vmp c where c.inactive=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
            selectedItems = getFacade().findByJpql(sql);
        }
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Vmp();
        current.setDepartmentType(DepartmentType.Pharmacy); // Automatically set to Pharmacy
        selectedVmpDto = null; // Clear DTO selection
        vmpAuditEvents = null; // Clear audit events
        addingVtmInVmp = new VirtualProductIngredient();
        editable = true;
    }

    public void edit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to edit");
            return;
        }
        // Sync the DTO with current entity
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

                Vmp tix = new Vmp();
                tix.setCode(code);
                tix.setName(ix);
                tix.setDepartmentType(DepartmentType.Pharmacy); // Set to Pharmacy
                tix.setDepartment(null);

            } catch (Exception ignored) {
            }
        }
    }

    public void setSelectedItems(List<Vmp> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private Map<String, Object> createAuditMap(Vmp vmp) {
        Map<String, Object> auditData = new HashMap<>();
        if (vmp != null) {
            auditData.put("id", vmp.getId());
            auditData.put("name", vmp.getName());
            auditData.put("code", vmp.getCode());
            auditData.put("retired", vmp.isRetired());
            auditData.put("inactive", vmp.isInactive());
            auditData.put("departmentType", vmp.getDepartmentType() != null ?
                vmp.getDepartmentType().toString() : null);
            auditData.put("descreption", vmp.getDescreption());
            auditData.put("vtmId", vmp.getVtm() != null ? vmp.getVtm().getId() : null);
            auditData.put("vtmName", vmp.getVtm() != null ? vmp.getVtm().getName() : null);
            auditData.put("dosageFormId", vmp.getDosageForm() != null ? vmp.getDosageForm().getId() : null);
            auditData.put("dosageFormName", vmp.getDosageForm() != null ? vmp.getDosageForm().getName() : null);
        }
        return auditData;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().editAndCommit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        }
        recreateModel();
        getItems();
        editable = false;
    }

    public void save() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            // UPDATE - capture before state
            Vmp beforeUpdate = getFacade().find(getCurrent().getId());
            Map<String, Object> beforeData = createAuditMap(beforeUpdate);

            getCurrent().setDepartmentType(DepartmentType.Pharmacy); // Ensure Pharmacy type
            getFacade().edit(getCurrent());

            // Log audit for update
            Map<String, Object> afterData = createAuditMap(getCurrent());
            auditService.logAudit(beforeData, afterData,
                    getSessionController().getLoggedUser(),
                    "Vmp", "Update VMP", getCurrent().getId());

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
                    "Vmp", "Create VMP", getCurrent().getId());

            JsfUtil.addSuccessMessage("Saved Successfully.");
        }
        recreateModel();
        getItems();
        editable = false;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
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

    public VmpController() {
    }

    public Vmp getCurrent() {
        if (current == null) {
            current = new Vmp();
        }
        return current;
    }

    public void setCurrent(Vmp current) {
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
                    "Vmp", "Delete VMP", current.getId());

            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete");
        }

        // Clear all selections and state
        recreateModel();
        getItems();
        current = null;
        selectedVmpDto = null; // Clear DTO selection
        vmpAuditEvents = null; // Clear audit events
        getCurrent();
        editable = false;
    }

    private VmpFacade getFacade() {
        return ejbFacade;
    }

    public List<Vmp> getItems() {
        if (items == null) {
            String j;
            Map<String, Object> params = new HashMap<>();
            j = "select v "
                    + " from Vmp v "
                    + " where v.departmentType=:dep ";

            params.put("dep", DepartmentType.Pharmacy);

            // Apply status filter
            if ("active".equals(filterStatus)) {
                j += "and v.retired=:retired ";
                params.put("inactive", false);
            } else if ("inactive".equals(filterStatus)) {
                j += "and v.retired=:retired ";
                params.put("inactive", true);
            }
            // For "all", no additional filter needed

            j += "order by v.name";
            items = getFacade().findByJpql(j, params);
        }
        return items;
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    // DTO Management Methods

    public List<VmpDto> getVmpDtos() {
        String jpql = "SELECT new com.divudi.core.data.dto.VmpDto("
                + "v.id, v.name, v.code, v.descreption, v.retired, v.inactive) "
                + "FROM Vmp v WHERE v.departmentType=:dep AND v.retired=:retired ";

        Map<String, Object> params = new HashMap<>();
        params.put("dep", DepartmentType.Pharmacy);
        params.put("retired", false); // Only show non-deleted items

        // Apply status filter based on inactive attribute
        if ("active".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", true);
        }
        // For "all", no additional filter needed

        jpql += "ORDER BY v.name";

        return (List<VmpDto>) getFacade().findLightsByJpql(jpql, params);
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
        params.put("dep", DepartmentType.Pharmacy);
        params.put("retired", false); // Only show non-deleted items

        // Apply status filter based on inactive attribute
        if ("active".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", false);
        } else if ("inactive".equals(filterStatus)) {
            jpql += "AND v.inactive=:inactive ";
            params.put("inactive", true);
        }
        // For "all", no additional filter needed

        jpql += "ORDER BY v.name";

        return (List<VmpDto>) getFacade().findLightsByJpql(jpql, params);
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

    // Audit History Management
    public void fillVmpAuditEvents() {
        if (current != null && current.getId() != null) {
            try {
                // Direct query approach for VMP audit events
                String jpql = "SELECT a FROM AuditEvent a WHERE a.objectId = :objectId " +
                             "AND a.entityType = :entityType " +
                             "ORDER BY a.eventDataTime DESC";

                Map<String, Object> params = new HashMap<>();
                params.put("objectId", current.getId());
                params.put("entityType", "Vmp");

                vmpAuditEvents = auditEventFacade.findByJpql(jpql, params);

                System.out.println("Direct VMP Audit Query - ID: " + current.getId());
                System.out.println("Total audit events found: " + (vmpAuditEvents != null ? vmpAuditEvents.size() : "null"));

                if (vmpAuditEvents != null && !vmpAuditEvents.isEmpty()) {
                    for (AuditEvent event : vmpAuditEvents) {
                        System.out.println("Event: " + event.getEventTrigger() + " at " + event.getEventDataTime());
                    }
                }

            } catch (Exception e) {
                System.out.println("Error fetching audit events: " + e.getMessage());
                e.printStackTrace();
                vmpAuditEvents = new ArrayList<>();
            }
        } else {
            vmpAuditEvents = new ArrayList<>();
        }
    }

    public String navigateToVmpAuditEvents() {
        fillVmpAuditEvents();
        return "/pharmacy/admin/vmp_audit_events?faces-redirect=true";
    }

    public List<AuditEvent> getVmpAuditEvents() {
        if (vmpAuditEvents == null) {
            fillVmpAuditEvents();
        }
        return vmpAuditEvents;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
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
        vmpDtos = null; // Clear DTO cache

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
                selectedVmpDto = null;
                vmpAuditEvents = null;
            }
        }

//        getItems(); // Refresh items based on new filter
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
                return "Active VMPs";
            case "inactive":
                return "Inactive VMPs";
            case "all":
                return "All VMPs";
            default:
                return "Active VMPs";
        }
    }

    // Toggle Status Methods
    public void toggleVmpStatus() {
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Please select a VMP to change status");
            return;
        }

        // Capture before state for audit
        Map<String, Object> beforeData = createAuditMap(current);

        boolean wasInactive = current.isInactive();

        if (wasInactive) {
            // Reactivate VMP
            current.setInactive(false);
            JsfUtil.addSuccessMessage("VMP Activated Successfully");
        } else {
            // Deactivate VMP
            current.setInactive(true);
            JsfUtil.addSuccessMessage("VMP Deactivated Successfully");
        }

        getFacade().edit(current);

        // Capture after state for audit
        Map<String, Object> afterData = createAuditMap(current);
        String action = wasInactive ? "Activate VMP" : "Deactivate VMP";

        // Log audit event
        auditService.logAudit(beforeData, afterData,
                getSessionController().getLoggedUser(),
                "Vmp", action, current.getId());

        // Update DTO to reflect new status
        if (selectedVmpDto != null) {
            selectedVmpDto.setInactive(current.isInactive());
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
        // If inactive (inactive), show green "Activate" button
        // If active, show orange "Deactivate" button
        return current.isInactive() ? "ui-button-success" : "ui-button-warning";
    }

    /**
     *
     */
    @FacesConverter("vmp")
    public static class VmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            VmpController controller = (VmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vmpController");
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
            if (object instanceof Vmp) {
                Vmp o = (Vmp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + VmpController.class.getName());
            }
        }
    }

    /**
     * JSF converter for VMP DTO
     */
    @FacesConverter("vmpDtoConverter")
    public static class VmpDtoConverter implements Converter {
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.parseLong(value);
                VmpController controller = (VmpController) facesContext.getApplication()
                        .getELResolver().getValue(facesContext.getELContext(), null, "vmpController");

                Vmp entity = controller.getFacade().find(id);
                return entity != null ? controller.createVmpDto(entity) : null;
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
}

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
import com.divudi.service.AuditService;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.entity.pharmacy.Vtm;
import com.divudi.core.facade.SpecialityFacade;
import com.divudi.core.facade.VtmFacade;
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
    @Inject
    private BillBeanController billBean;
    List<Vtm> selectedItems;
    private Vtm current;
    private List<Vtm> items;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    List<Vtm> vtmList;
    private boolean editable;

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
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired " +
                        "AND UPPER(c.name) LIKE :query " +
                        "AND (c.departmentType IS NULL OR c.departmentType=:dep) " +
                        "ORDER BY c.name";
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
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired " +
                        "AND (c.departmentType IS NULL OR c.departmentType=:dep) " +
                        "ORDER BY c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Pharmacy);
            selectedItems = getFacade().findByJpql(sql, params);
        } else {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired " +
                        "AND UPPER(c.name) LIKE :query " +
                        "AND (c.departmentType IS NULL OR c.departmentType=:dep) " +
                        "ORDER BY c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("query", "%" + getSelectText().toUpperCase() + "%");
            params.put("dep", DepartmentType.Pharmacy);
            selectedItems = getFacade().findByJpql(sql, params);
        }
        return selectedItems;
    }

    public void fillItems() {
        String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired " +
                    "AND (c.departmentType IS NULL OR c.departmentType=:dep) " +
                    "ORDER BY c.name";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("dep", DepartmentType.Pharmacy);
        items = getFacade().findByJpql(sql, params);
    }

    public void prepareAdd() {
        current = new Vtm();
        current.setDepartmentType(DepartmentType.Pharmacy);
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
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            if (!billedAs) {
                //////// // System.out.println("2");
                getCurrent().setBilledAs(getCurrent());

            }
            if (!reportedAs) {
                getCurrent().setReportedAs(getCurrent());
            }
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
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
            JsfUtil.addSuccessMessage("Saved Successfully");
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
                editable = false;
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
                editable = false;
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving VTM '" + (getCurrent().getName() != null ? getCurrent().getName() : "Unknown") + "': " + e.getMessage());
            e.printStackTrace();
        }

        items = null;
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
        items = null;
        getItems();
        getCurrent();
        editable = false;
    }

    private VtmFacade getFacade() {
        return ejbFacade;
    }

    public List<Vtm> getItems() {
        if (items == null) {
            String sql = "SELECT c FROM Vtm c WHERE c.retired=:retired " +
                        "AND (c.departmentType IS NULL OR c.departmentType=:dep) " +
                        "ORDER BY c.name";
            Map<String, Object> params = new HashMap<>();
            params.put("retired", false);
            params.put("dep", DepartmentType.Pharmacy);
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

    private Map<String, Object> createAuditMap(Vtm vtm) {
        Map<String, Object> auditData = new HashMap<>();
        if (vtm != null) {
            auditData.put("id", vtm.getId());
            auditData.put("name", vtm.getName());
            auditData.put("code", vtm.getCode());
            auditData.put("retired", vtm.isRetired());
            auditData.put("departmentType", vtm.getDepartmentType() != null ? vtm.getDepartmentType().toString() : null);
            auditData.put("description", vtm.getDescription());
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

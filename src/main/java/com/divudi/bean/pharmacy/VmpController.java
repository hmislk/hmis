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
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.SymanticType;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.entity.pharmacy.VirtualProductIngredient;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.SpecialityFacade;
import com.divudi.facade.VmpFacade;
import com.divudi.facade.VirtualProductIngredientFacade;
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
    List<Vmp> selectedItems;
    private Vmp current;
    private List<Vmp> items = null;
    String selectText = "";
    String bulkText = "";
    boolean billedAs;
    boolean reportedAs;
    VirtualProductIngredient addingVtmInVmp;
    VirtualProductIngredient removingVtmInVmp;
    @Inject
    VtmInVmpController vtmInVmpController;
    @EJB
    VirtualProductIngredientFacade vivFacade;
    List<VirtualProductIngredient> vivs;

    @EJB
    VmpFacade vmpFacade;

    List<Vmp> vmpList;

    public String navigateToListAllVmps() {
        String jpql = "Select vmp "
                + " from Vmp vmp "
                + " where vmp.retired=:ret "
                + " order by vmp.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(jpql, m);
        return "/emr/reports/vmps?faces-redirect=true";
    }

    public Vmp findOrCreateVmpByName(String vmpName) {
        String jpql = "Select vmp "
                + " from Vmp vmp "
                + " where vmp.retired=:ret "
                + " and vmp.name=:vmpName "
                + " order by vmp.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("vmpName", vmpName);
        Vmp vmp = getFacade().findFirstByJpql(jpql, m);
        if (vmp == null) {
            vmp = new Vmp();
            vmp.setName(vmpName);
            String vmpCode = CommonController.nameToCode("vmp_" + vmpName);
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
            String jpql = "SELECT c FROM Vmp c WHERE c.retired = false AND LOWER(c.name) LIKE :query ORDER BY c.name";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("query", "%" + query.trim().toLowerCase() + "%");

            vmpList = getFacade().findByJpql(jpql, parameters);
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
                + " where a.retired=:ret"
                + " and a.vmp=:vmp "
                + " order by a.name";
        m.put("ret", false);
        m.put("vmp", (Vmp) vmp);
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
                + " where c.retired=:ret "
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
                + " from VtmsVmps viv join viv.vmp vmp"
                + " where viv.retired=:ret "
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
        v.setCode("vmp_" + CommonController.nameToCode(vmpName));
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
        v.setCode("vmp_" + CommonController.nameToCode(vmpName));
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
            String j = "select v from VtmsVmps v where v.vmp=:vmp";
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
        if (current.getName() == null || current.getName().equals("")) {
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
            sql = "select c from Vmp c where c.retired=false and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
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
        if (selectText.trim().equals("")) {
            selectedItems = getFacade().findByJpql("select c from Vmp c where c.retired=false order by c.name");
        } else {
            String sql = "select c from Vmp c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name";
            selectedItems = getFacade().findByJpql(sql);
        }
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Vmp();
        addingVtmInVmp = new VirtualProductIngredient();
    }

    public void bulkUpload() {
        List<String> lstLines = Arrays.asList(getBulkText().split("\\r?\\n"));
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
                tix.setDepartment(null);

            } catch (Exception e) {
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

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        }
        recreateModel();
        getItems();
    }

    public void save() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getFacade().create(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully.");
        }
        recreateModel();
        getItems();
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
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);

            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private VmpFacade getFacade() {
        return ejbFacade;
    }

    public List<Vmp> getItems() {
        if (items == null) {
            String j;
            j = "select v "
                    + " from Vmp v "
                    + " where v.retired=false "
                    + " order by v.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    public SpecialityFacade getSpecialityFacade() {
        return specialityFacade;
    }

    public void setSpecialityFacade(SpecialityFacade specialityFacade) {
        this.specialityFacade = specialityFacade;
    }

    /**
     *
     */
    @FacesConverter("vmp")
    public static class VmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            VmpController controller = (VmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vmpController");
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
            if (object instanceof Vmp) {
                Vmp o = (Vmp) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + VmpController.class.getName());
            }
        }
    }
}

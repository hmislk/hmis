package com.divudi.bean.common;

import com.divudi.core.data.ItemLight;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemMapping;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemMappingFacade;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named(value = "itemMappingController")
@SessionScoped
public class ItemMappingController implements Serializable {

    /**
     * Creates a new instance of ItemMappingController
     */
    public ItemMappingController() {
    }
    @Inject
    private SessionController sessionController;

    @EJB
    private ItemMappingFacade ejbFacade;
    @EJB
    private ItemFacade itemFacade;

    private ItemMapping current;
    private List<ItemMapping> items = null;
    private List<ItemMapping> selectedItemMappings = null;
    private Institution institution;
    private Department department;
    private Item item;
    private ItemMapping selectedItemMapping;
    private List<Item> selectedItems;
    // Outside-charge-specific mapping page state (issue #23250, Mode B) —
    // kept separate from `institution`/`department` above so the general
    // mapping pages and this one don't clobber each other's selection when
    // both are open in the same session.
    private Institution outsideChargeSite;

    public void addAllSelectedItemsToInstitution() {
        if (selectedItems == null) {
            JsfUtil.addErrorMessage("No Items Selected");
            return;
        }
        if (selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Items Selected");
            return;
        }
        if (institution == null) {
            JsfUtil.addErrorMessage("No Institution Selected");
            return;
        }
        if (items == null) {
            items = new ArrayList<>();
        }
        for (Item i : selectedItems) {
            ItemMapping im1 = findItemMapping(i, institution);
            if (im1 == null) {
                im1 = new ItemMapping();
                im1.setItem(i);
                im1.setInstitution(institution);
                im1.setCreater(sessionController.getLoggedUser());
                im1.setCreatedAt(new Date());
                getFacade().create(im1);
            } else if (im1.isRetired()) {
                im1.setRetired(false);
                getFacade().edit(im1);
                items.add(im1);
            }
        }
        selectedItems = new ArrayList<>();
        fillItemMappingsForSelectedInstitution();
        JsfUtil.addSuccessMessage("All Added");
    }

    public void addAllSelectedItemsToDepartment() {
        if (selectedItems == null || selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Items Selected");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("No Department Selected");
            return;
        }
        if (items == null) {
            items = new ArrayList<>();
        }
        for (Item i : selectedItems) {
            ItemMapping im = findItemMapping(i, department);
            if (im == null) {
                im = new ItemMapping();
                im.setItem(i);
                im.setDepartment(department);
                im.setCreater(sessionController.getLoggedUser());
                im.setCreatedAt(new Date());
                getFacade().create(im);
            } else if (im.isRetired()) {
                im.setRetired(false);
                getFacade().edit(im);
                items.add(im);
            }
        }
        JsfUtil.addSuccessMessage("All Added");
    }

    public boolean mappingExists(Item i, Institution ins) {
        // Excludes outside-charge-only mappings (issue #23250) so this
        // general-purpose check/reactivate path never hijacks a row created
        // by the dedicated outside-charge mapping page.
        String jpql = "select pavan "
                + " from ItemMapping pavan "
                + " where pavan.institution=:ins "
                + " and pavan.item=:item "
                + " and pavan.outsideChargeMapping=false ";
        Map m = new HashMap<>();
        m.put("ins", ins);
        m.put("item", i);
        List<ItemMapping> ims = getFacade().findByJpql(jpql, m);
        if (ims == null) {
            return false;
        }
        if (ims.isEmpty()) {
            return false;
        }
        return true;
    }

    public ItemMapping findItemMapping(Item i, Institution ins) {
        // Excludes outside-charge-only mappings (issue #23250) — see mappingExists(Item, Institution).
        String jpql = "select pavan "
                + " from ItemMapping pavan "
                + " where pavan.institution=:ins "
                + " and pavan.item=:item "
                + " and pavan.outsideChargeMapping=false ";
        Map m = new HashMap<>();
        m.put("ins", ins);
        m.put("item", i);
        List<ItemMapping> ims = getFacade().findByJpql(jpql, m);
        if (ims == null) {
            return null;
        }
        if (ims.isEmpty()) {
            return null;
        }
        return ims.get(0);
    }

    public ItemMapping findItemMapping(Item i, Department dep) {
        String jpql = "select pavan "
                + " from ItemMapping pavan "
                + " where pavan.department=:dep "
                + " and pavan.item=:item ";
        Map m = new HashMap<>();
        m.put("dep", dep);
        m.put("item", i);
        List<ItemMapping> ims = getFacade().findByJpql(jpql, m);
        if (ims == null) {
            return null;
        }
        if (ims.isEmpty()) {
            return null;
        }
        return ims.get(0);
    }

    public boolean mappingExists(Item i, Department dep) {
        String jpql = "select pavan "
                + " from ItemMapping pavan "
                + " where pavan.department=:dep "
                + " and pavan.item=:item ";
        Map m = new HashMap<>();
        m.put("dep", dep);
        m.put("item", i);
        List<ItemMapping> ims = getFacade().findByJpql(jpql, m);
        if (ims == null) {
            return false;
        }
        if (ims.isEmpty()) {
            return false;
        }
        return true;
    }

    private void removeSelectedItemMapping() {
        for (ItemMapping im : selectedItemMappings) {
            im.setRetired(true);
            getFacade().edit(im);
        }
        items.removeAll(selectedItemMappings);
        selectedItemMappings = new ArrayList<>();
    }

    public void removeSelectedItemMappingForInstitution() {
        if (selectedItemMappings == null || selectedItemMappings.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        removeSelectedItemMapping();
        fillItemMappingsForSelectedInstitution();
    }

    public void removeSelectedItemMappingForDepartment() {
        if (selectedItemMappings == null || selectedItemMappings.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        removeSelectedItemMapping();
        fillItemMappingsForSelectedDepartment();
    }

    public void fillItemMappingsForSelectedDepartment() {
        if (department == null) {
            JsfUtil.addErrorMessage("Department ?");
            return;
        }
        String jpql = "SELECT im "
                + "FROM ItemMapping im "
                + "WHERE im.retired=false "
                + "AND im.department = :dep";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dep", department);
        List<ItemMapping> itemMappings = getFacade().findByJpql(jpql, parameters);
        items = itemMappings;
    }

    public void fillItemMappingsForSelectedInstitution() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Institution");
            return;
        }
        // Excludes outside-charge-only mappings (issue #23250) — see mappingExists(Item, Institution).
        String jpql = "SELECT im "
                + "FROM ItemMapping im "
                + "WHERE im.retired=false "
                + "and im.institution = :inst "
                + "and im.outsideChargeMapping=false";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("inst", institution);
        List<ItemMapping> itemMappings = getFacade().findByJpql(jpql, parameters);
        items = itemMappings;
    }

    // ------------------------------------------------------------------
    // Outside Charge Item Mapping (issue #23250, Mode B) — item eligibility
    // for the Inward "Add Outside Charges" Item field, opt-in via the
    // "Inward Outside Charge Requires Item Mapping" config key, keyed to
    // the department's SITE (Department.getSite()), not the bare
    // department. Kept on the same ItemMapping table but always guarded by
    // outsideChargeMapping=true so it never overlaps with the generic
    // mapping methods above.
    // ------------------------------------------------------------------
    public void addAllSelectedItemsToOutsideChargeSite() {
        if (selectedItems == null || selectedItems.isEmpty()) {
            JsfUtil.addErrorMessage("No Items Selected");
            return;
        }
        if (outsideChargeSite == null) {
            JsfUtil.addErrorMessage("No Site Selected");
            return;
        }
        if (items == null) {
            items = new ArrayList<>();
        }
        for (Item i : selectedItems) {
            ItemMapping im = findOutsideChargeItemMapping(i, outsideChargeSite);
            if (im == null) {
                im = new ItemMapping();
                im.setItem(i);
                im.setInstitution(outsideChargeSite);
                im.setOutsideChargeMapping(true);
                im.setCreater(sessionController.getLoggedUser());
                im.setCreatedAt(new Date());
                getFacade().create(im);
                items.add(im);
            } else if (im.isRetired()) {
                im.setRetired(false);
                getFacade().edit(im);
                items.add(im);
            }
        }
        selectedItems = new ArrayList<>();
        JsfUtil.addSuccessMessage("All Added");
    }

    public ItemMapping findOutsideChargeItemMapping(Item i, Institution site) {
        String jpql = "select im "
                + " from ItemMapping im "
                + " where im.institution=:site "
                + " and im.item=:item "
                + " and im.outsideChargeMapping=true ";
        Map<String, Object> m = new HashMap<>();
        m.put("site", site);
        m.put("item", i);
        List<ItemMapping> ims = getFacade().findByJpql(jpql, m);
        if (ims == null || ims.isEmpty()) {
            return null;
        }
        return ims.get(0);
    }

    public void fillItemMappingsForSelectedOutsideChargeSite() {
        if (outsideChargeSite == null) {
            JsfUtil.addErrorMessage("Site ?");
            return;
        }
        String jpql = "SELECT im "
                + "FROM ItemMapping im "
                + "WHERE im.retired=false "
                + "AND im.institution = :site "
                + "AND im.outsideChargeMapping=true";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("site", outsideChargeSite);
        items = getFacade().findByJpql(jpql, parameters);
    }

    public void removeSelectedItemMappingForOutsideChargeSite() {
        if (selectedItemMappings == null || selectedItemMappings.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        removeSelectedItemMapping();
        fillItemMappingsForSelectedOutsideChargeSite();
    }

    // Navigation method for managing Outside Charge Item Mappings
    public String navigateToManageOutsideChargeItemMappings() {
        return "/admin/items/manage_outside_charge_item_mappings?faces-redirect=true";
    }

    public Institution getOutsideChargeSite() {
        return outsideChargeSite;
    }

    public void setOutsideChargeSite(Institution outsideChargeSite) {
        this.outsideChargeSite = outsideChargeSite;
    }

    // Navigation method for managing Department Item Mappings
    public String navigateToManageDepartmentItemMappings() {
        return "/admin/items/manage_department_item_mappings?faces-redirect=true";
    }

    // Navigation method for managing Institution Item Mappings
    public String navigateToManageInstitutionItemMappings() {
        return "/admin/items/manage_institution_item_mappings?faces-redirect=true";
    }

    public List<Item> completeItemByInstitution(String qry, Institution institution) {
        List<Item> results;
        // NOTE: deliberately NOT filtering out outsideChargeMapping=true rows here.
        // This is a live OPD/Optician/Collecting-Centre billing autocomplete path
        // (a projection query that otherwise never needs to touch the new column),
        // and adding that filter would make it require OUTSIDECHARGEMAPPING to exist
        // in the DB from the moment this deploys, before an admin has necessarily
        // run the "Add Missing Fields" DDL step (issue #23250). The admin-only
        // mapping-management methods below already require that column regardless
        // (they SELECT the full entity), so the isolation is enforced there instead.
        String jpql = "SELECT im.item FROM ItemMapping im "
                + "WHERE (LOWER(im.item.name) LIKE :qry OR LOWER(im.item.fullName) LIKE :qry OR LOWER(im.item.code) LIKE :qry) "
                + "AND im.institution = :institution "
                + "AND im.retired = false "
                + "ORDER BY im.item.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("qry", "%" + qry.toLowerCase() + "%");
        parameters.put("institution", institution);
        results = itemFacade.findByJpql(jpql, parameters);

        return results != null ? results : new ArrayList<>();
    }

    public List<Item> completeItemByDepartment(String qry, Department department) {
        List<Item> results;
        String jpql = "SELECT im.item FROM ItemMapping im "
                + "WHERE (LOWER(im.item.name) LIKE :qry OR LOWER(im.item.fullName) LIKE :qry OR LOWER(im.item.code) LIKE :qry) "
                + "AND im.department = :department "
                + "AND im.retired = false "
                + "ORDER BY im.item.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("qry", "%" + qry.toLowerCase() + "%");
        parameters.put("department", department);
        results = itemFacade.findByJpql(jpql, parameters);
        return results != null ? results : new ArrayList<>();
    }

    public List<Item> completeItemByDepartment(String qry, Institution institution) {
        List<Item> results;
        String jpql = "SELECT im.item FROM ItemMapping im "
                + "WHERE (im.item.name LIKE :qry OR im.item.fullName LIKE :qry OR im.item.code LIKE :qry) "
                + "AND im.department.institution = :ins "
                + "AND im.retired = false "
                + "ORDER BY im.item.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("qry", "%" + qry.toLowerCase() + "%");
        parameters.put("ins", institution);
        results = itemFacade.findByJpql(jpql, parameters);
        return results != null ? results : new ArrayList<>();
    }

    public List<Item> fillItemByInstitution(Institution institution) {
        List<Item> results;
        // NOTE: deliberately not filtering outsideChargeMapping here — see
        // completeItemByInstitution(String, Institution) above. Also, outside-charge
        // mappings never set `department`, so they cannot match `im.department.institution`
        // regardless.
        String jpql = "SELECT im.item FROM ItemMapping im "
                + " WHERE im.retired = false "
                + " AND im.department.institution = :ins "
                + " ORDER BY im.item.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("ins", institution);
        results = itemFacade.findByJpql(jpql, parameters);
        return results != null ? results : new ArrayList<>();
    }

    public List<ItemLight> fillItemLightByInstitution(Institution institution) {
        List<ItemLight> results;
        String jpql = "SELECT new com.divudi.core.data.ItemLight("
                + "im.item.id, "
                + "im.item.department.name, "
                + "im.item.name, "
                + "im.item.code, "
                + "im.item.total, "
                + "im.item.department.id) "
                + "FROM ItemMapping im "
                + "WHERE im.retired = false "
                + "AND im.department.institution = :ins "
                + "ORDER BY im.item.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("ins", institution);
        results = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);
        return results != null ? results : new ArrayList<>();
    }

    public List<Item> fillItemByDepartment(Department dept) {
        List<Item> results;
        String jpql = "SELECT im.item FROM ItemMapping im "
                + " WHERE im.retired = false "
                + " AND im.department=:dept "
                + " ORDER BY im.item.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("dept", dept);
        results = itemFacade.findByJpql(jpql, parameters);
        return results != null ? results : new ArrayList<>();
    }

    public List<ItemLight> fillItemLightByDepartment(Department dept) {
        List<ItemLight> results;
        String jpql = "SELECT new com.divudi.core.data.ItemLight("
                + "im.item.id, "
                + "im.item.department.name, "
                + "im.item.name, "
                + "im.item.code, "
                + "im.item.total, "
                + "im.item.department.id) "
                + "FROM ItemMapping im "
                + "WHERE im.retired = false "
                + "AND im.department = :dept "
                + "ORDER BY im.item.name";

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("dept", dept);
        results = (List<ItemLight>) itemFacade.findLightsByJpql(jpql, parameters);
        return results != null ? results : new ArrayList<>();
    }

    public List<ItemMapping> completeItemMapping(String qry) {
        List<ItemMapping> list;
        String sql;
        HashMap<String, Object> hm = new HashMap<>();
        sql = "select c from ItemMapping c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public List<ItemMapping> findItemMappingsByInstitution(Institution institution) {
        List<ItemMapping> list;
        String jpql;
        HashMap<String, Object> parameters = new HashMap<>();
        // Excludes outside-charge-only mappings (issue #23250) — see mappingExists(Item, Institution).
        jpql = "SELECT im FROM ItemMapping im "
                + "WHERE im.institution = :institution "
                + "AND im.retired = false "
                + "AND im.outsideChargeMapping = false "
                + "ORDER BY im.item.name";
        parameters.put("institution", institution);
        list = getFacade().findByJpql(jpql, parameters);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public List<ItemMapping> findItemMappingsByDepartment(Department department) {
        List<ItemMapping> list;
        String jpql = "SELECT im FROM ItemMapping im "
                + "WHERE im.department = :department "
                + "AND im.retired = false "
                + "ORDER BY im.item.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("department", department);
        list = getFacade().findByJpql(jpql, parameters);
        return list != null ? list : new ArrayList<>();
    }

    public List<ItemMapping> findItemMappingsByItem(Item item) {
        List<ItemMapping> list;
        String jpql = "SELECT im FROM ItemMapping im "
                + "WHERE im.item = :item "
                + "AND im.retired = false "
                + "ORDER BY im.institution.name, im.department.name";
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("item", item);
        list = getFacade().findByJpql(jpql, parameters);

        return list != null ? list : new ArrayList<>();
    }

    public void prepareAdd() {
        current = new ItemMapping();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        // Implementation of save logic
    }

    public void delete() {
        // Implementation of delete logic
    }

    private ItemMappingFacade getFacade() {
        return ejbFacade;
    }

    public List<ItemMapping> getItems() {
        return items;
    }

    public ItemMapping getCurrent() {
        return current;
    }

    public void setCurrent(ItemMapping current) {
        this.current = current;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemMapping getSelectedItemMapping() {
        return selectedItemMapping;
    }

    public void setSelectedItemMapping(ItemMapping selectedItemMapping) {
        this.selectedItemMapping = selectedItemMapping;
    }

    public List<Item> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<Item> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public List<ItemMapping> getSelectedItemMappings() {
        return selectedItemMappings;
    }

    public void setSelectedItemMappings(List<ItemMapping> selectedItemMappings) {
        this.selectedItemMappings = selectedItemMappings;
    }

    // No getters and setters as per request
    @FacesConverter(forClass = ItemMapping.class)
    public static class ItemMappingConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ItemMappingController controller = (ItemMappingController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemMappingController");
            return controller.getFacade().find(getKey(value));
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
            if (object instanceof ItemMapping) {
                ItemMapping o = (ItemMapping) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemMappingController.class.getName());
            }
        }
    }

}

package com.divudi.bean.common;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemMapping;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemMappingFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

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
    private Institution institution;
    private Department department;
    private Item item;
    private ItemMapping selectedItemMapping;
    private List<Item> selectedItems;

    public void addAllSelectedItemsToInstitution() {
        String jpql;
        HashMap<String, Object> parameters = new HashMap<>();
        jpql = "SELECT im FROM ItemMapping im "
                + "WHERE im.institution = :institution "
                + "AND im.retired = false "
                + "ORDER BY im.item.name";
        parameters.put("institution", institution);
        items = getFacade().findByJpql(jpql, parameters);

        if (items == null) {
            items = new ArrayList<>();
        }

    }

    public void addAllSelectedItemsToDepartment() {
        // Logic to add all selected items to the department if not already added
    }

    public boolean checkItemAddedToInstitution(Item item, Institution institution) {
        // Logic to check if an item is already added to the institution
        return false; // Placeholder return
    }

    public boolean checkItemAddedToDepartment(Item item, Department department) {
        // Logic to check if an item is already added to the department
        return false; // Placeholder return
    }

    public void removeSelectedItemMapping() {
        // Logic to remove the selected item mapping
    }

    public void fillItemMappingsForSelectedDepartment() {
        // Logic to fill item mappings for the selected department
    }

    public void fillItemMappingsForSelectedInstitution() {
        // Logic to fill item mappings for the selected institution
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
        jpql = "SELECT im FROM ItemMapping im "
                + "WHERE im.institution = :institution "
                + "AND im.retired = false "
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
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
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

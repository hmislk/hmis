/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Item;
import com.divudi.entity.lab.ItemForItem;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemForItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
public class ItemForItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ItemForItemFacade ejbFacade;
    @EJB
    ItemFacade itemFacade;
    private ItemForItem current;
    Item parentItem;
    Item childItem;
    private List<ItemForItem> items = null;
    PatientInvestigation patientIx;

    public PatientInvestigation getPatientIx() {
        return patientIx;
    }

    public void setPatientIx(PatientInvestigation patientIx) {
        this.patientIx = patientIx;
        setParentItem(patientIx.getInvestigation());
    }

    public Item getChildItem() {
        return childItem;
    }

    public void setChildItem(Item childItem) {
        this.childItem = childItem;
    }

    public Item getParentItem() {
        return parentItem;
    }

    public void setParentItem(Item parentItem) {
        this.parentItem = parentItem;
    }

    public void prepareAdd() {
        current = new ItemForItem();
    }

    private void recreateModel() {
        items = null;
    }

    public List<Item> getItemsForParentItem(Item i) {
        String sql;
        Map m = new HashMap();
        m.put("it", i);
        sql = "select c.childItem from ItemForItem c where c.retired=false and c.parentItem=:it order by c.childItem.name ";
        return getItemFacade().findByJpql(sql, m);
    }

    public void addItem() {
        if (parentItem == null || childItem == null) {
            JsfUtil.addErrorMessage("Please select");
            return;
        }
        ItemForItem temIi = new ItemForItem();
        temIi.setParentItem(parentItem);
        temIi.setChildItem(childItem);
        temIi.setCreatedAt(Calendar.getInstance().getTime());
        temIi.setCreater(getSessionController().getLoggedUser());
        getFacade().create(temIi);
        setChildItem(null);
    }

    public ItemForItem addItemForItem(Item parentItemToAdd, Item childItemToAdd) {
        if (parentItemToAdd == null || childItemToAdd == null) {
            JsfUtil.addErrorMessage("Please select");
            return null;
        }
        ItemForItem addingIi = new ItemForItem();
        addingIi.setParentItem(parentItemToAdd);
        addingIi.setChildItem(childItemToAdd);
        addingIi.setCreatedAt(Calendar.getInstance().getTime());
        addingIi.setCreater(getSessionController().getLoggedUser());
        getFacade().create(addingIi);
        return addingIi;
    }

    public ItemForItem findItemForItem(Item parentItemToAdd, Item childItemToAdd) {
        if (parentItemToAdd == null || childItemToAdd == null) {
            JsfUtil.addErrorMessage("Please select");
            return null;
        }
        ItemForItem addingIi = null;
        String jpql = "select ii "
                + " from ItemForItem ii "
                + " where ii.retired=:ret "
                + " and ii.parentItem=:pi "
                + " and ii.childItem=:ci ";
        Map params = new HashMap();
        params.put("ret", false);
        params.put("pi", parentItemToAdd);
        params.put("ci", childItemToAdd);
        addingIi = getFacade().findFirstByJpql(jpql, params);
        return addingIi;
    }

    public void removeItem() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select one to remove");
            return;
        }
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(getSessionController().getLoggedUser());
        getFacade().edit(current);
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }
    
    
    
    public void save(ItemForItem saving) {
        if (saving != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public ItemForItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ItemForItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ItemForItemController() {
    }

    public ItemForItem getCurrent() {
        return current;
    }

    public void setCurrent(ItemForItem current) {
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

    private ItemForItemFacade getFacade() {
        return ejbFacade;
    }

    public List<ItemForItem> getItems() {
        if (getParentItem() != null && getParentItem().getId() != null) {
            items = getFacade().findByJpql("select c from ItemForItem c where c.retired=false and c.parentItem.id = " + getParentItem().getId() + " ");
        }
        if (items == null) {
            items = new ArrayList<ItemForItem>();
        }
        return items;
    }

    public List<ItemForItem> findItemsForParent(Item parentItem) {
        if (parentItem == null) {
            JsfUtil.addErrorMessage("Please select a parent item.");
            return Collections.emptyList(); // Returns an empty list if parentItem is null to avoid null pointer exceptions
        }

        // JPQL query to find all child items for a given parent item, assuming the ItemForItem entity is not retired
        String jpql = "SELECT ii FROM ItemForItem ii WHERE ii.retired = :ret AND ii.parentItem = :pi";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false); // Assuming you want to filter out retired items
        params.put("pi", parentItem);

        List<ItemForItem> childItems = getFacade().findByJpql(jpql, params);
        return childItems;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    /**
     *
     */
    @FacesConverter(forClass = ItemForItem.class)
    public static class ItemForItemControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ItemForItemController controller = (ItemForItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemForItemController");
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
            if (object instanceof ItemForItem) {
                ItemForItem o = (ItemForItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemForItemController.class.getName());
            }
        }
    }
}

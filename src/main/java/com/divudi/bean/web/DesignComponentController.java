package com.divudi.bean.web;

import com.divudi.core.data.web.ComponentDataType;
import com.divudi.core.data.web.ComponentPresentationType;
import com.divudi.core.entity.web.DesignComponent;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.web.ComponentMappingType;
import com.divudi.core.facade.web.DesignComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@SessionScoped
public class DesignComponentController implements Serializable {

    @EJB
    DesignComponentFacade facade;
    private int manageEmrIndex;
    private DesignComponent current;
    private List<DesignComponent> list;
    private DesignComponent currentDataEntryForm;
    private List<DesignComponent> listOfDataEntryForms;
    private DesignComponent currentDataEntryItem;
    private List<DesignComponent> listOfDataEntryItems;

    public List<ComponentPresentationType> getComponentPresentationTypes() {
        return Arrays.asList(ComponentPresentationType.values());
    }

    public List<ComponentMappingType> getComponentMappingTypes() {
        return Arrays.asList(ComponentMappingType.values());
    }

    public List<ComponentDataType> getComponentDataTypes() {
        return Arrays.asList(ComponentDataType.values());
    }

    public DesignComponent getCurrent() {
        return current;
    }

    public void setCurrent(DesignComponent current) {
        this.current = current;
    }

    public List<DesignComponent> getList() {
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void setList(List<DesignComponent> list) {
        this.list = list;
    }

    @Deprecated
    public String navigateToAddDesignComponent() {
        current = new DesignComponent();
        return "/webcontent/design_component?faces-redirect=true";
    }

    public String navigateToAddNewDataEntryForm() {
        currentDataEntryForm = new DesignComponent();
        currentDataEntryForm.setComponentPresentationType(ComponentPresentationType.DataEntryForm);
        return "/forms/data_entry_form?faces-redirect=true";
    }

    public String navigateToEditDesignComponent() {
        if (currentDataEntryItem == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/forms/data_entry_item?faces-redirect=true";
    }

    public String navigateToEditDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        listOfDataEntryItems = listItemsOfDataEntryForm(currentDataEntryForm);
        return "/forms/data_entry_form?faces-redirect=true";
    }

    public String navigateToListDataEntryForms() {
        listOfDataEntryForms = listDataEntryForms();
        return "/forms/data_entry_forms?faces-redirect=true";
    }

    public String navigateToListDataEntryItems() {

        listOfDataEntryItems = listDataEntryForms();
        return "/forms/data_entry_items?faces-redirect=true";
    }

    public String navigateToAddComponentsToDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }

        if (currentDataEntryForm.getId() == null) {
            JsfUtil.addErrorMessage("Please save first");
            return "";
        }

        currentDataEntryItem = new DesignComponent();
        currentDataEntryItem.setDataEntryForm(currentDataEntryForm);
        return "/forms/data_entry_item?faces-redirect=true";
    }

    public String navigateToListComponentsOfDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }

        if (currentDataEntryForm.getId() == null) {
            JsfUtil.addErrorMessage("Please save first");
            return "";
        }

        listOfDataEntryItems = listItemsOfDataEntryForm(currentDataEntryForm);
        return "/forms/data_entry_items?faces-redirect=true";
    }

    public void saveCurrent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (current.getId() == null) {
            facade.create(current);
        } else {
            facade.edit(current);
        }
    }

    public void saveCurrentDataEntryForm() {
        if (currentDataEntryForm == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (currentDataEntryForm.getId() == null) {
            facade.create(currentDataEntryForm);
            getListOfDataEntryForms().add(currentDataEntryForm);
        } else {
            facade.edit(currentDataEntryForm);
        }
    }

    public DesignComponentFacade getFacade() {
        return facade;
    }

    public void saveDataEntryItemOfForm() {
        if (currentDataEntryItem == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (currentDataEntryItem.getId() == null) {
            facade.create(currentDataEntryItem);
        } else {
            facade.edit(currentDataEntryItem);
        }
        getListOfDataEntryItems().add(currentDataEntryItem);
        currentDataEntryItem = new DesignComponent();
    }

    public String saveDataEntryComponentOfForm() {
        if (currentDataEntryItem == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }

        if (currentDataEntryItem.getId() == null) {
            facade.create(currentDataEntryItem);
            getListOfDataEntryItems().add(currentDataEntryItem);
        } else {
            facade.edit(currentDataEntryItem);
        }

        currentDataEntryItem = new DesignComponent();
        return navigateToListComponentsOfDataEntryForm();
    }

    private void listItems() {
        String jpql = "select d "
                + " from DesignComponent d";
        list = facade.findByJpql(jpql);
    }

    private List<DesignComponent> listDataEntryForms() {
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.componentPresentationType=:pt";
        Map m = new HashMap();
        m.put("pt", ComponentPresentationType.DataEntryForm);
        designComponents = facade.findByJpql(jpql, m);

        return designComponents;
    }

    private List<DesignComponent> listItemsOfDataEntryForm(DesignComponent dataEntryForm) {
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.dataEntryForm=:def";
        Map m = new HashMap();
        m.put("def", dataEntryForm);
        designComponents = facade.findByJpql(jpql, m);

        return designComponents;
    }

    public List<DesignComponent> completeDesignComponents(String query) {
        String jpql = "SELECT d FROM DesignComponent d WHERE "
                + " d.retired=false"
                + "and LOWER(d.name) LIKE :query";
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toLowerCase() + "%");
        return facade.findByJpql(jpql, params);
    }

    public int getManageEmrIndex() {
        return manageEmrIndex;
    }

    public void setManageEmrIndex(int manageEmrIndex) {
        this.manageEmrIndex = manageEmrIndex;
    }

    public DesignComponent getCurrentDataEntryForm() {
        return currentDataEntryForm;
    }

    public void setCurrentDataEntryForm(DesignComponent currentDataEntryForm) {
        this.currentDataEntryForm = currentDataEntryForm;
    }

    public List<DesignComponent> getListOfDataEntryForms() {
        if (listOfDataEntryForms == null) {
            listOfDataEntryForms = new ArrayList<>();
        }
        return listOfDataEntryForms;
    }

    public void setListOfDataEntryForms(List<DesignComponent> listOfDataEntryForms) {
        this.listOfDataEntryForms = listOfDataEntryForms;
    }

    public DesignComponent getCurrentDataEntryItem() {
        return currentDataEntryItem;
    }

    public void setCurrentDataEntryItem(DesignComponent currentDataEntryItem) {
        this.currentDataEntryItem = currentDataEntryItem;
    }

    public List<DesignComponent> getListOfDataEntryItems() {
        if (listOfDataEntryItems == null) {
            listOfDataEntryItems = new ArrayList<>();
        }
        return listOfDataEntryItems;
    }

    public void setListOfDataEntryItems(List<DesignComponent> listOfDataEntryItems) {
        this.listOfDataEntryItems = listOfDataEntryItems;
    }

    @FacesConverter(forClass = DesignComponent.class)
    public static class DesignComponentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DesignComponentController controller = (DesignComponentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "designComponentController");
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
            if (object instanceof DesignComponent) {
                DesignComponent o = (DesignComponent) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DesignComponentController.class.getName());
            }
        }
    }
}

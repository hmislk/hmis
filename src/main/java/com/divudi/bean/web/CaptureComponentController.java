/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.web;

import com.divudi.bean.clinical.PatientEncounterController;
import com.divudi.bean.common.PatientController;
import com.divudi.core.data.web.ComponentPresentationType;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.web.CaptureComponent;
import com.divudi.core.entity.web.DesignComponent;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.DynamicHtmlFormComponent;
import com.divudi.core.facade.web.CaptureComponentFacade;
import com.divudi.core.facade.web.DesignComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author ACER
 */
@Named
@SessionScoped
public class CaptureComponentController implements Serializable {

    /**
     * Creates a new instance of CaptureComponentController
     */
    private CaptureComponent current;

    private List<CaptureComponent> items;

    private List<CaptureComponent> dataEntryItems;
    private List<DynamicHtmlFormComponent> formComponents = new ArrayList<>();

    private List<DesignComponent> dataEntryForms;

    private DesignComponent selectedDataEntryForm;

    @Inject
    PatientController patientController;
    private PatientEncounter opdVisit;

    @Inject
    PatientEncounterController patientEncounterController;

    @EJB
    CaptureComponentFacade facade;

    @EJB
    DesignComponentFacade designComponentFacade;

    public String navigateToAddCaptureComponent() {
        dataEntryItems = new ArrayList<>();
        List<DesignComponent> designComponents = listDataEntryForms();
        for (DesignComponent d : designComponents) {
            CaptureComponent tempCaptureComponent = new CaptureComponent();
            tempCaptureComponent.setName(d.getName());
            tempCaptureComponent.setComponentDataType(d.getComponentDataType());
            tempCaptureComponent.setComponentPresentationType(d.getComponentPresentationType());
            tempCaptureComponent.setDesignComponent(d);
            dataEntryItems.add(tempCaptureComponent);

        }
        current = new CaptureComponent();
        return "/webcontent/capture_component.xhtml";

    }

    public String navigateToSelectDataEntryForm() {

        dataEntryForms = listDataEntryForms();
        return "/webcontent/select_data_entry_form";

    }

    public String navgateToListCaptureComponents() {

        listItems();
        return "/webcontent/capture_components.xhtml";
    }

    public String navigateToStartDataEntry() {
        // Reset or initialize the list of dynamic form components
        formComponents = new ArrayList<>();

        // Retrieve the HTML template with placeholders
        String htmlTemplateWithPlaceholders = selectedDataEntryForm.getEditHtml();

        // Fetch design components, which define data entry items
        dataEntryItems = new ArrayList<>();
        List<DesignComponent> designComponents = listComponentsOfDataEntryForm(selectedDataEntryForm);
        current = new CaptureComponent();
        current.setDesignComponent(selectedDataEntryForm);
        saveByAjax(current);
        Map<String, CaptureComponent> componentsMap = new HashMap<>();

        // Prepare components for insertion into the template
        for (DesignComponent d : designComponents) {
            CaptureComponent tempCaptureComponent = new CaptureComponent();
            tempCaptureComponent.setName(d.getName());
            tempCaptureComponent.setComponentDataType(d.getComponentDataType());
            tempCaptureComponent.setComponentPresentationType(d.getComponentPresentationType());
            tempCaptureComponent.setDesignComponent(d);
            tempCaptureComponent.setParent(current);
            dataEntryItems.add(tempCaptureComponent);
            saveByAjax(tempCaptureComponent);
            componentsMap.put("{" + d.getCode() + "}", tempCaptureComponent); // Mapping placeholders to components
        }

        // Split and integrate HTML content and components
        String[] parts = htmlTemplateWithPlaceholders.split("(\\{[^}]+\\})"); // Split by placeholders
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}"); // Pattern to extract placeholders
        Matcher matcher = pattern.matcher(htmlTemplateWithPlaceholders);

        int partIndex = 0;
        while (matcher.find()) {
            if (partIndex < parts.length) {
                formComponents.add(new DynamicHtmlFormComponent(parts[partIndex++]));
            }
            String key = matcher.group(0);
            if (componentsMap.containsKey(key)) {
                formComponents.add(new DynamicHtmlFormComponent(componentsMap.get(key)));
            }
        }
        // Add the last part if there's any leftover HTML after the last placeholder
        if (partIndex < parts.length) {
            formComponents.add(new DynamicHtmlFormComponent(parts[partIndex]));
        }

        current = new CaptureComponent();
        return "/emr/dataentry/dataentry?faces-redirect=true";
    }

    public String navgateToStartDataEntryForOPD() {
        opdVisit = patientEncounterController.getCurrent();
        dataEntryItems = new ArrayList<>();
        List<DesignComponent> designComponents = listComponentsOfDataEntryForm(selectedDataEntryForm);
        for (DesignComponent d : designComponents) {
            CaptureComponent tempCaptureComponent = new CaptureComponent();
            tempCaptureComponent.setName(d.getName());
            tempCaptureComponent.setComponentDataType(d.getComponentDataType());
            tempCaptureComponent.setComponentPresentationType(d.getComponentPresentationType());
            tempCaptureComponent.setDesignComponent(d);
            dataEntryItems.add(tempCaptureComponent);
        }
        current = new CaptureComponent();
        return "/emr/data_entry_form";
    }

    public String navigateToEditCaptureComponent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return " ";
        }
        return "/webcontent/capture_component";

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

    public void saveDataForm() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }

        if (current.getId() == null) {
            facade.create(current);
        } else {
            facade.edit(current);
        }

        for (CaptureComponent cc : dataEntryItems) {
            if (cc.getId() == null) {
                facade.create(cc);
            } else {
                facade.edit(cc);
            }
        }

        JsfUtil.addErrorMessage("Saved");
    }

    private void listItems() {

        String jpql = "select c "
                + " from CaptureComponent c";
        items = facade.findByJpql(jpql);

    }

    private List<DesignComponent> listDesignComponents() {
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d";
        designComponents = designComponentFacade.findByJpql(jpql);

        return designComponents;
    }

    public List<DesignComponent> listDataEntryForms() {
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.componentPresentationType=:pt";
        Map m = new HashMap();
        m.put("pt", ComponentPresentationType.DataEntryForm);
        designComponents = designComponentFacade.findByJpql(jpql, m);

        return designComponents;
    }

    private List<DesignComponent> listComponentsOfDataEntryForm(DesignComponent dataEntryForm) {
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.dataEntryForm=:def";
        Map m = new HashMap();
        m.put("def", dataEntryForm);
        designComponents = designComponentFacade.findByJpql(jpql, m);

        return designComponents;
    }

    public CaptureComponentController() {

    }

    public void saveByAjax(CaptureComponent sc) {
        if (sc == null) {
            return;
        }
        if (sc.getId() == null) {
            facade.create(sc);
        } else {
            facade.edit(sc);
        }
    }

    public CaptureComponent getCurrent() {
        return current;
    }

    public void setCurrent(CaptureComponent current) {
        this.current = current;
    }

    public List<CaptureComponent> getItems() {
        return items;
    }

    public void setItems(List<CaptureComponent> items) {
        this.items = items;
    }

    public List<CaptureComponent> getDataEntryItems() {
        return dataEntryItems;
    }

    public void setDataEntryItems(List<CaptureComponent> dataEntryItems) {
        this.dataEntryItems = dataEntryItems;
    }

    public List<DesignComponent> getDataEntryForms() {
        return dataEntryForms;
    }

    public void setDataEntryForms(List<DesignComponent> dataEntryForms) {
        this.dataEntryForms = dataEntryForms;
    }

    public DesignComponent getSelectedDataEntryForm() {
        return selectedDataEntryForm;
    }

    public void setSelectedDataEntryForm(DesignComponent selectedDataEntryForm) {
        this.selectedDataEntryForm = selectedDataEntryForm;
    }

    public CaptureComponentFacade getFacade() {
        return facade;
    }

    public PatientEncounter getOpdVisit() {
        return opdVisit;
    }

    public void setOpdVisit(PatientEncounter opdVisit) {
        this.opdVisit = opdVisit;
    }

    public List<DynamicHtmlFormComponent> getFormComponents() {
        return formComponents;
    }

    public void setFormComponents(List<DynamicHtmlFormComponent> formComponents) {
        this.formComponents = formComponents;
    }

    @FacesConverter(forClass = CaptureComponent.class)
    public static class CaptureComponentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CaptureComponentController controller = (CaptureComponentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "captureComponentController");
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
            if (object instanceof CaptureComponent) {
                CaptureComponent o = (CaptureComponent) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + CaptureComponentController.class.getName());
            }
        }
    }

}

package com.divudi.bean.web;


import com.divudi.data.web.ComponentDataType;
import com.divudi.data.web.ComponentPresentationType;
import com.divudi.entity.web.DesignComponent;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.facade.web.DesignComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
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
    
    
    
    public List<ComponentPresentationType> getComponentPresentationTypes(){
        return Arrays.asList(ComponentPresentationType.values());
    }
    
    public List<ComponentDataType> getComponentDataTypes(){
        return Arrays.asList(ComponentDataType.values());
    }

    
    public DesignComponent getCurrent() {
        return current;
    }

    public void setCurrent(DesignComponent current) {
        this.current = current;
    }

    public List<DesignComponent> getList() {
        return list;
    }

    public void setList(List<DesignComponent> list) {
        this.list = list;
    }
    
    public String navigateToAddDesignComponent(){
        current= new DesignComponent();
        return "/webcontent/design_component";
    }
    
    public String navigateToAddNewDataEntryForm(){
        current= new DesignComponent();
        current.setComponentPresentationType(ComponentPresentationType.DataEntryForm);
        return "/forms/data_entry_form";
    }
    
    public String navigateToEditDesignComponent(){
         if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/forms/design_component";
    }
    
    public String navigateToEditDataEntryForm(){
         if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/forms/data_entry_form";
    }
    
    public String navigateToListDesignComponent(){
        listItems();
        return "/webcontent/design_components";
    }
    
    public String navigateToListDataEntryForms(){
        list=listDataEntryForms();
        return "/forms/data_entry_forms";
    }
    
    public String navigateToAddComponentsToDataEntryForm(){
        if(current==null){
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        
        if(current.getId()==null){
            JsfUtil.addErrorMessage("Please save first");
            return "";
        }
        
        DesignComponent tempDataEntryForm = current;
        current = new DesignComponent();
        current.setDataEntryForm(tempDataEntryForm);
        return "/forms/design_component";
    }
    
    public String navigateToListComponentsOfDataEntryForm(){
        if(current==null){
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        
        if(current.getId()==null){
            JsfUtil.addErrorMessage("Please save first");
            return "";
        }
     
        list = listComponentsOfDataEntryForm(current);
        return "/forms/design_components";
    }
    
    public void saveCurrent(){
        if(current==null){
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        
        if(current.getId()==null){
            facade.create(current);
        }
        
        else{
            facade.edit(current);
        }
    }

    public DesignComponentFacade getFacade() {
        return facade;
    }
    
    
    
    public String saveDataEntryComponentOfForm(){
        if(current==null){
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        
        if(current.getId()==null){
            facade.create(current);
        }
        
        else{
            facade.edit(current);
        }
        
        current = current.getDataEntryForm();
        return navigateToEditDataEntryForm();
    }
    
    private void listItems(){
        String jpql = "select d "
                + " from DesignComponent d";
        list = facade.findByJpql(jpql);
    }
    
    private List<DesignComponent> listDataEntryForms(){
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.componentPresentationType=:pt";
        Map m = new HashMap();
        m.put("pt", ComponentPresentationType.DataEntryForm);
         designComponents = facade.findByJpql(jpql,m);
         
        return designComponents; 
    }
    
    private List<DesignComponent> listComponentsOfDataEntryForm(DesignComponent dataEntryForm){
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d"
                + " where d.dataEntryForm=:def";
        Map m = new HashMap();
        m.put("def", dataEntryForm);
         designComponents = facade.findByJpql(jpql,m);
         
        return designComponents; 
    }

    public int getManageEmrIndex() {
        return manageEmrIndex;
    }

    public void setManageEmrIndex(int manageEmrIndex) {
        this.manageEmrIndex = manageEmrIndex;
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
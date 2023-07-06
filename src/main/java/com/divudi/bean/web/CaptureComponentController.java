/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.web;

import com.divudi.entity.web.CaptureComponent;
import com.divudi.entity.web.DesignComponent;
import com.divudi.facade.util.JsfUtil;
import com.divudi.facade.web.CaptureComponentFacade;
import com.divudi.facade.web.DesignComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;

/**
 *
 * @author ACER
 */
@Named(value = "captureComponentController")
@SessionScoped
public class CaptureComponentController implements Serializable {

    /**
     * Creates a new instance of CaptureComponentController
     */
    private CaptureComponent current;

    private List<CaptureComponent> items;
    
    private List<CaptureComponent> dataEntryItems;
    

    @EJB
    CaptureComponentFacade facade;
    
    @EJB
    DesignComponentFacade designComponentFacade;

    public String navigateToAddCaptureComponent() {
        dataEntryItems = new ArrayList<>();
        List<DesignComponent> designComponents = listDesignComponents();
        for(DesignComponent d: designComponents){
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

    public String navgateToListCaptureComponents() {

        listItems();
        return "/webcontent/capture_components.xhtml";
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

    private void listItems() {

        String jpql = "select c "
                + " from CaptureComponent c";
        items = facade.findBySQL(jpql);

    }
    
    private List<DesignComponent> listDesignComponents(){
        List<DesignComponent> designComponents;
        String jpql = "select d "
                + " from DesignComponent d";
         designComponents = designComponentFacade.findBySQL(jpql);
         
        return designComponents; 
    }

    public CaptureComponentController() {

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
    
    

}

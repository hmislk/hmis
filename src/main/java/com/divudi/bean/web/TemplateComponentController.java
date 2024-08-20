package com.divudi.bean.web;

import com.divudi.entity.web.TemplateComponent;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.facade.web.TemplateComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class TemplateComponentController implements Serializable {

    @EJB
    TemplateComponentFacade facade;

    private TemplateComponent current;
    private List<TemplateComponent> items;

    /**
     * Creates a new instance of TemplateComponentController
     */
    public TemplateComponentController() {
    }

    public String navigateToAddNewTemplateComponent() {
        current = new TemplateComponent();
        return "/webcontent/template_component";
    }

    public String navigateToEditTemplateComponent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/webcontent/template_component";
    }

    public String navigateToListTemplateComponent() {
        listItems();
        return "/webcontent/template_components";
    }

    private void listItems(){
        String jpql = "select t "
                + " from TemplateComponent t";
        items = facade.findByJpql(jpql);
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
        JsfUtil.addSuccessMessage("Saved");
    }

    public TemplateComponent getCurrent() {
        return current;
    }

    public void setCurrent(TemplateComponent current) {
        this.current = current;
    }

    public List<TemplateComponent> getItems() {
        return items;
    }

    public void setItems(List<TemplateComponent> items) {
        this.items = items;
    }
    
    

}

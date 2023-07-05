package com.divudi.bean.web;

import com.divudi.data.web.ComponentPresentationType;
import com.divudi.entity.web.DesignComponent;
import com.divudi.facade.util.JsfUtil;
import com.divudi.facade.web.DesignComponentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import java.util.Arrays;

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@SessionScoped
public class DesignComponentController implements Serializable {

    @EJB
    DesignComponentFacade facade;
    
    private DesignComponent current;
    private List<DesignComponent> list;
    
    public List<ComponentPresentationType> getComponentPresentationTypes(){
        return Arrays.asList(ComponentPresentationType.values());
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
        System.out.println("Hello");
        current= new DesignComponent();
        return "/webcontent/design_component";
    }
    
    public String navigateToEditDesignComponent(){
         if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/webcontent/design_component";
    }
    
    public String navigateToListDesignComponent(){
        listItems();
        return "/webcontent/design_components";
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
    
    private void listItems(){
        String jpql = "select d "
                + " from DesignComponent d";
        list = facade.findBySQL(jpql);
    }
    
    
}
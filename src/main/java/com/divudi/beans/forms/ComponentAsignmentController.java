/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.beans.forms;

import com.divudi.entity.forms.ComponentAsignment;
import com.divudi.facade.forms.ComponentAssignmentFacade;
import com.divudi.bean.common.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.ejb.EJB;

/**
 *
 * @author Thamara Waidyarathna
 */
@Named
@SessionScoped
public class ComponentAsignmentController implements Serializable {


    @EJB
    ComponentAssignmentFacade componentAsignmentFacade;
    
    
    public ComponentAsignmentController() {
    }
    private ComponentAsignment current;
    
    public void saveCurrent(){
        if(current==null){
            JsfUtil.addErrorMessage("Nothing to sava");
            return;
        }
        if(current.getId()==null){
           componentAsignmentFacade.create(current);
        }
        else{
            componentAsignmentFacade.edit(current);
        }
        JsfUtil.addSuccessMessage("Saved");
    }
    
    public String navigateToAddNewForms(){
      current =new ComponentAsignment();
      return "/forms/add";  
    }
    public String navigateToListAllForms(){
      return "/forms/add";  
    }

    public ComponentAsignment getCurrent() {
        return current;
    }

    public void setCurrent(ComponentAsignment current) {
        this.current = current;
    }
}

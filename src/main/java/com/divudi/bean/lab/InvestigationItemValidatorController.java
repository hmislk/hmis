/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.lab;

import com.divudi.entity.lab.InvestigationTube;
import com.divudi.entity.lab.InvestigationValidator;
import com.divudi.facade.InvestigationItemValidatorFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author pasan
 */
@Named
@SessionScoped
public class InvestigationItemValidatorController implements Serializable {

    /**
     * Creates a new instance of InvestigationItemValidatorController
     */
    public InvestigationItemValidatorController() {
    }
    private InvestigationValidator current;
    @EJB
    private InvestigationItemValidatorFacade investigationItemValidatorFacade;
   
    
    public void updateValidator() {
        getInvestigationItemValidatorFacade().edit(current);
    }

    public InvestigationValidator getCurrent() {
        return current;
    }

    public void setCurrent(InvestigationValidator current) {
        this.current = current;
    }

    public InvestigationItemValidatorFacade getInvestigationItemValidatorFacade() {
        return investigationItemValidatorFacade;
    }

    public void setInvestigationItemValidatorFacade(InvestigationItemValidatorFacade investigationItemValidatorFacade) {
        this.investigationItemValidatorFacade = investigationItemValidatorFacade;
    }

    
    
    @FacesConverter("investigationValidatorConverter")
    public static class InvestigationValidatorConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationItemValidatorController controller = (InvestigationItemValidatorController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationValidatorController");
            return controller.investigationItemValidatorFacade.find(getKey(value));
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
            if (object instanceof InvestigationTube) {
                InvestigationTube o = (InvestigationTube) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationTubeController.class.getName());
            }
        }
    }
       
    
    @FacesConverter(forClass = InvestigationTube.class)
    public static class InvestigationValidatorControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            
            InvestigationItemValidatorController controller = (InvestigationItemValidatorController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationValidatorController");
            
            if(controller==null){
                return null;
            }
            
            if(controller.investigationItemValidatorFacade.find(getKey(value))==null){
                return null;
            }
            
            return controller.investigationItemValidatorFacade.find(getKey(value));
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
            if (object instanceof InvestigationTube) {
                InvestigationTube o = (InvestigationTube) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationTubeController.class.getName());
            }
        }
    }
       
   
}

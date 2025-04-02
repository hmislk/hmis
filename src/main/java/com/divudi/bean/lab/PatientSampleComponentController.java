/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.*;

import com.divudi.core.entity.lab.PatientSampleComponant;
import com.divudi.core.facade.PatientSampleComponantFacade;
import java.io.Serializable;
import java.util.List;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientSampleComponentController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PatientSampleComponantFacade ejbFacade;
    private PatientSampleComponant current;
    private List<PatientSampleComponant> items = null;
    private List<PatientSampleComponant> itemsSelected = null;

    public PatientSampleComponantFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PatientSampleComponentController() {
    }

    public PatientSampleComponant getCurrent() {
        return current;
    }

    public void setCurrent(PatientSampleComponant current) {
        this.current = current;
    }

    public List<PatientSampleComponant> getItems() {
        return items;
    }

    public void setItems(List<PatientSampleComponant> items) {
        this.items = items;
    }

    public List<PatientSampleComponant> getItemsSelected() {
        return itemsSelected;
    }

    public void setItemsSelected(List<PatientSampleComponant> itemsSelected) {
        this.itemsSelected = itemsSelected;
    }






    /**
     *
     */
    @FacesConverter(forClass = PatientSampleComponant.class)
    public static class PatientSampleComponentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientSampleComponentController controller = (PatientSampleComponentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientSampleComponentController");
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
            if (object instanceof PatientSampleComponant) {
                PatientSampleComponant o = (PatientSampleComponant) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientSampleComponant.class.getName());
            }
        }
    }

}

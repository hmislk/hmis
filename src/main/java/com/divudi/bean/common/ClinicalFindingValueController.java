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
import com.divudi.data.clinical.ClinicalFindingValueType;
import com.divudi.entity.Patient;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.facade.ClinicalFindingValueFacade;
import java.io.Serializable;
import java.util.ArrayList;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ClinicalFindingValueController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ClinicalFindingValueFacade ejbFacade;
    private ClinicalFindingValue current;
    private List<ClinicalFindingValue> items = null;

    public void prepareAdd() {
        current = new ClinicalFindingValue();
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public ClinicalFindingValueFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ClinicalFindingValueFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ClinicalFindingValueController() {
    }

    public ClinicalFindingValue getCurrent() {
        if (current == null) {
            current = new ClinicalFindingValue();
        }
        return current;
    }

    public void setCurrent(ClinicalFindingValue current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
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

    private ClinicalFindingValueFacade getFacade() {
        return ejbFacade;
    }

    public List<ClinicalFindingValue> getItems() {
        return items;
    }
    
    public List<ClinicalFindingValue> findClinicalFindingValues(Patient pt, ClinicalFindingValueType type) {
        if (items == null) {
            String j;
            j = "select a "
                    + " from ClinicalFindingValue a "
                    + " where a.retired=:ret "
                    + " and a.patient=:pt "
                    + " and a.clinicalFindingValueType=:type";
            Map params = new HashMap();
            params.put("ret", false);
            params.put("pt", pt);
            params.put("type", type);
            items = getFacade().findByJpql(j,params);
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = ClinicalFindingValue.class)
    public static class ClinicalFindingValueConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ClinicalFindingValueController controller = (ClinicalFindingValueController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "clinicalFindingValueController");
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
            if (object instanceof ClinicalFindingValue) {
                ClinicalFindingValue o = (ClinicalFindingValue) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ClinicalFindingValueController.class.getName());
            }
        }
    }

}

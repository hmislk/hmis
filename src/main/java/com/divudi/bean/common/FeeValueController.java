/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 *  Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.FeeValue;
import com.divudi.facade.FeeValueFacade;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * (Health Informatics)
 */
@Named
@SessionScoped
public class FeeValueController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private FeeValueFacade ejbFacade;
    private FeeValue current;

    public void save(FeeValue feeValue) {
        if (feeValue == null) {
            return;
        }
        if (feeValue.getId() != null) {
            getFacade().edit(feeValue);
        } else {
            feeValue.setCreatedAt(new Date());
            feeValue.setCreater(getSessionController().getLoggedUser());
            getFacade().create(feeValue);
        }
    }

    public void prepareAdd() {
        current = new FeeValue();
    }

    public void saveSelected() {
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public FeeValueFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(FeeValueFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public FeeValueController() {
    }

    public FeeValue getCurrent() {
        if (current == null) {
            current = new FeeValue();
        }
        return current;
    }

    public void setCurrent(FeeValue current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
    }

    private FeeValueFacade getFacade() {
        return ejbFacade;
    }

    

    /**
     *
     */
    @FacesConverter(forClass = FeeValue.class)
    public static class FeeValueConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            FeeValueController controller = (FeeValueController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "feeValueController");
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
            if (object instanceof FeeValue) {
                FeeValue o = (FeeValue) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + FeeValue.class.getName());
            }
        }
    }

}

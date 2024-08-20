/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.entity.pharmacy.Make;
import com.divudi.facade.MakeFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.common.util.JsfUtil.PersistAction;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named("makeController")
@SessionScoped
public class MakeController implements Serializable {

    @EJB
    private MakeFacade ejbFacade;
    @Inject
    SessionController sessionController;

    private List<Make> items = null;
    private Make selected;

    public MakeController() {
    }

    public Make getSelected() {
        return selected;
    }

    public void setSelected(Make selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private MakeFacade getFacade() {
        return ejbFacade;
    }

    public Make prepareCreate() {
        selected = new Make();
        initializeEmbeddableKey();
        return selected;
    }

    public void create() {
        persist(PersistAction.CREATE, "Saved Successfully");
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, "Updated Successfully");
    }

    public void destroy() {
        persist(PersistAction.DELETE, "Deleted Successfully");
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<Make> getItems() {
        if (items == null) {
            String jpql;
            jpql = "Select m from Make m where m.retired=false order by m.name";
            items = getFacade().findByJpql(jpql);
        }
        return items;
    }

    private void persist(PersistAction persistAction) {
        persist(persistAction, "Saved");
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    selected.setRetired(true);
                    selected.setRetireComments("");
                    selected.setRetirer(getSessionController().getLoggedUser());
                    selected.setRetiredAt(new Date());
                    getFacade().edit(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ("PersistenceErrorOccured"));
            }
        }
    }

    public Make getMake(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<Make> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<Make> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    public MakeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(MakeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    @FacesConverter(forClass = Make.class)
    public static class MakeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MakeController controller = (MakeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "makeController");
            return controller.getMake(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {
                key =0l;
            }
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
            if (object instanceof Make) {
                Make o = (Make) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), Make.class.getName()});
                return null;
            }
        }

    }

}

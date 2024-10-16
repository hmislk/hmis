package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.cashTransaction.Drawer;
import com.divudi.entity.cashTransaction.DrawerEntry;
import com.divudi.facade.DrawerEntryFacade;
import com.divudi.service.DrawerEntryService;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class DrawerEntryController implements Serializable {

    @EJB
    DrawerEntryFacade ejbFacade;
    @EJB
    DrawerEntryService service;
    
    @Inject
    SessionController sessionController;

    private DrawerEntry current;

    public void saveCurrent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select");
            return;
        }
        service.save(current, sessionController.getLoggedUser());
    }

    public DrawerEntryController() {
    }

    private DrawerEntryFacade getEjbFacade() {
        return ejbFacade;
    }

    public DrawerEntry getCurrent() {
        return current;
    }

    public void setCurrent(DrawerEntry current) {
        this.current = current;
    }

    @FacesConverter(forClass = DrawerEntry.class)
    public static class DrawerEntryConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DrawerEntryController controller = (DrawerEntryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "drawerEntryController");
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
            if (object instanceof DrawerEntry) {
                DrawerEntry o = (DrawerEntry) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DrawerEntry.class.getName());
            }
        }
    }

}

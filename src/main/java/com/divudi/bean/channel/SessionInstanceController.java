/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.*;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.SessionInstanceFacade;
import java.io.Serializable;
import java.util.Date;
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
public class SessionInstanceController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    ChannelScheduleController ChannelScheduleController;
    
    @EJB
    private SessionInstanceFacade ejbFacade;

    List<SessionInstance> selectedItems;
    private SessionInstance current;
    private List<SessionInstance> items = null;

    public void save(SessionInstance pa) {
        if (pa == null) {
            return;
        }
        if (pa.getId() != null) {
            getFacade().edit(pa);
        } else {
            pa.setCreatedAt(new Date());
            pa.setCreater(sessionController.getLoggedUser());
            getFacade().create(pa);
        }
    }

    public SessionInstanceController() {
    }

    public SessionInstanceFacade getFacade() {
        return ejbFacade;
    }

    @FacesConverter(forClass = SessionInstance.class)
    public static class PatientAppointmentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SessionInstanceController controller = (SessionInstanceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "sessionInstanceController");
            if (controller == null) {
                return null;
            }
            if (controller.getFacade() == null) {
                return null;
            }
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            try {
                java.lang.Long key;
                key = Long.valueOf(value);
                return key;
            } catch (NumberFormatException e) {
                return 0l;
            }
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
            if (object instanceof SessionInstance) {
                SessionInstance o = (SessionInstance) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SessionInstance.class.getName());
            }
        }
    }

}

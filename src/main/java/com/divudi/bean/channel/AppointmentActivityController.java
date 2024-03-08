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
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Area;
import com.divudi.entity.channel.AppointmentActivity;
import com.divudi.facade.AppointmentActivityFacade;
import java.io.Serializable;
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
public class AppointmentActivityController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private AppointmentActivityFacade appointmentActivityFacade;

    private AppointmentActivity current;
    private List<AppointmentActivity> items;

    public String navigateToManageAppointmentActivities() {
        return "/channel/channel_Shedule_Management";
    }

    public void addAppointmentActivity() {
        current = new AppointmentActivity();
    }

    public void save(AppointmentActivity aa) {
        if (aa == null) {
            return;
        }
        if (aa.getId() == null) {
            aa.setCreatedAt(new Date());
            aa.setCreater(sessionController.getLoggedUser());
            getFacade().create(aa);
        } else {
            getFacade().edit(aa);
        }
    }

    public void saveCurrent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        save(current);
        JsfUtil.addSuccessMessage("Saved");
    }

    public void deleteAppointMentActivity() {
        if (current == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(sessionController.getLoggedUser());
    }

    private AppointmentActivityFacade getFacade() {
        return appointmentActivityFacade;
    }

    public List<AppointmentActivity> fillAppointmentActivities() {
        String jpql = "select a "
                + " from AppointmentActivity a"
                + " where a.retired=:Ret"
                + " order by a.name";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(jpql, m);
    }

    public List<AppointmentActivity> getItems() {
        if (items == null) {
            items = fillAppointmentActivities();
        }
        return items;
    }

    public void setItems(List<AppointmentActivity> items) {
        this.items = items;
    }

    public AppointmentActivity getCurrent() {
        return current;
    }

    public void setCurrent(AppointmentActivity current) {
        this.current = current;
    }

    
    
    @FacesConverter(forClass = AppointmentActivity.class)
    public static class AppointmentActivityConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AppointmentActivityController controller = (AppointmentActivityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "appointmentActivityController");
            return controller.getFacade().find(getKey(value));
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
            if (object instanceof Area) {
                Area o = (Area) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AppointmentActivityController.class.getName());
            }
        }
    }

}

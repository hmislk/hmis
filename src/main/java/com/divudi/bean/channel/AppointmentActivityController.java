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
import com.divudi.entity.Institution;
import com.divudi.entity.channel.AppointmentActivity;
import com.divudi.facade.AppointmentActivityFacade;
import com.divudi.facade.AreaFacade;
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
import org.checkerframework.checker.units.qual.Current;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
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
    List<AppointmentActivity> items;
    
    public String navigateToManageAppointmentActivities(){
        return "";
    }
    
    public void addAppointmentActivity(){
        current = new AppointmentActivity();
    }
    
    public void save(){
        
    }
    
    public void saveCurrent(){
        
    }
    
    public void deleteAppointMentActivity(){
        
    }

    public AppointmentActivityFacade getAppointmentActivityFacade() {
        return appointmentActivityFacade;
    }

    public void setAppointmentActivityFacade(AppointmentActivityFacade appointmentActivityFacade) {
        this.appointmentActivityFacade = appointmentActivityFacade;
    }
 
   

    @FacesConverter(forClass = AppointmentActivity.class)
    public static class AreaConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AppointmentActivityController controller = (AppointmentActivityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "appointmentActivityController");
            return controller.getAppointmentActivityFacade().find(getKey(value));
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

package com.divudi.bean.common;

import com.divudi.bean.channel.ChannelScheduleController;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.facade.SessionNumberGeneratorFacade;
import java.io.Serializable;
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
public class SessionNumberGenerateController implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    ChannelScheduleController sheduleController;
    @EJB
    SessionNumberGeneratorFacade sessionNumberGeneratorFacade;

    List<SessionNumberGenerator> SessionNumberGeneratorlst;

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SessionNumberGeneratorFacade getSessionNumberGeneratorFacade() {
        return sessionNumberGeneratorFacade;
    }

    public void setSessionNumberGeneratorFacade(SessionNumberGeneratorFacade sessionNumberGeneratorFacade) {
        this.sessionNumberGeneratorFacade = sessionNumberGeneratorFacade;
    }

    public List<SessionNumberGenerator> getSessionNumberGeneratorlst() {
        return SessionNumberGeneratorlst;
    }

    public void setSessionNumberGeneratorlst(List<SessionNumberGenerator> SessionNumberGeneratorlst) {
        this.SessionNumberGeneratorlst = SessionNumberGeneratorlst;
    }

    public List<SessionNumberGenerator> completeSessionNumberGenerator(String qry) {

        String sql;
        Map m = new HashMap();
        sql = " SELECT sg FROM SessionNumberGenerator sg WHERE sg.retired=false"
                + " and (sg.name) like '%" + qry.toUpperCase() + "%' "
                + " and sg.speciality=:sp"
                + " and sg.staff=:s "
                + " order by sg.name";

        m.put("sp", sheduleController.getSpeciality());
        m.put("s", sheduleController.getCurrentStaff());

        SessionNumberGeneratorlst = sessionNumberGeneratorFacade.findByJpql(sql, m);

        return SessionNumberGeneratorlst;
    }

    /**
     *
     */
    // Configuration and code by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
    @FacesConverter(forClass = SessionNumberGenerator.class)
    public static class SessionNumberGenerateControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SessionNumberGenerateController controller = (SessionNumberGenerateController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "sessionNumberGenerateController");
            return controller.getSessionNumberGeneratorFacade().find(getKey(value));
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
            if (object instanceof SessionNumberGenerator) {  // Changed from Speciality to SessionNumberGenerator
                SessionNumberGenerator o = (SessionNumberGenerator) object;  // Changed from Speciality to SessionNumberGenerator
                return getStringKey(o.getId());  // Assuming getId() exists in SessionNumberGenerator
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SessionNumberGenerateController.class.getName());
            }
        }
    }

}

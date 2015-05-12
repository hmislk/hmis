/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.SessionController;
import com.divudi.facade.LabVisitFacade;
import com.divudi.entity.lab.LabVisit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named; import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 Informatics)
 */
@Named
@SessionScoped
public  class LabVisitController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private LabVisitFacade ejbFacade;
    List<LabVisit> adminRoles;
    List<LabVisit> circularEditorRoles;
    List<LabVisit> circularAdderRoles;
    List<LabVisit> circularViewerRoles;
    List<LabVisit> userRoles;
    private LabVisit current;
    private List<LabVisit> items = null;
    String selectText = "";

    public String getSelectText() {
        return selectText;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public LabVisitFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(LabVisitFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public LabVisitController() {
    }

    public List<LabVisit> getAdminRoles() {
        adminRoles = getFacade().findBySQL("Select d From LabVisit d");
        ////System.out.println("Count of admins roles is " + adminRoles.size());
        return adminRoles;
    }

    public void setAdminRoles(List<LabVisit> adminRoles) {
        this.adminRoles = adminRoles;
    }

    public LabVisit getCurrent() {
        return current;
    }

    public void setCurrent(LabVisit current) {
        this.current = current;
    }

    private LabVisitFacade getFacade() {
        return ejbFacade;
    }

    public List<LabVisit> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = LabVisit.class)
    public static class LabVisitControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            LabVisitController controller = (LabVisitController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "labVisitController");
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
            if (object instanceof LabVisit) {
                LabVisit o = (LabVisit) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + LabVisitController.class.getName());
            }
        }
    }
}

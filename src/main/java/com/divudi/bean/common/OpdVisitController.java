/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.bean.common.UtilityController;
import com.divudi.facade.OpdVisitFacade;
import com.divudi.entity.OpdVisit;
import java.util.TimeZone;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named; import javax.ejb.EJB;
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
public  class OpdVisitController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private OpdVisitFacade ejbFacade;
    @Inject
    SessionController sessionController;
    private OpdVisit current;
    private List<OpdVisit> items = null;

    /**
     *
     */
    public OpdVisitController() {
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new OpdVisit();
    }

    /**
     *
     * @return
     */
    public OpdVisitFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(OpdVisitFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    /**
     *
     * @return
     */
    public SessionController getSessionController() {
        return sessionController;
    }

    /**
     *
     * @param sessionController
     */
    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * Return the current opdVisit
     *
     * @return
     */
    public OpdVisit getCurrent() {
        if (current == null) {
            current = new OpdVisit();
        }
        return current;
    }

    /**
     * Set the current opdVisit
     *
     * @param current
     */
    public void setCurrent(OpdVisit current) {
        this.current = current;
    }

    private OpdVisitFacade getFacade() {
        return ejbFacade;
    }

    /**
     *
     * @return
     */
    public List<OpdVisit> getItems() {
        String temSql;
        temSql = "SELECT i FROM OpdVisit i where i.retired=false order by i.name";
        items = getFacade().findBySQL(temSql);
        return items;
    }

    /**
     *
     * Set all OpdVisits to null
     *
     */
    private void recreateModel() {
        items = null;
    }

    /**
     *
     */
    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    /**
     *
     * Delete the current OpdVisit
     *
     */
    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    /**
     *
     */
    @FacesConverter(forClass = OpdVisit.class)
    public static class OpdVisitControllerConverter implements Converter {

        /**
         *
         * @param facesContext
         * @param component
         * @param value
         * @return
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            OpdVisitController controller = (OpdVisitController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "opdVisitController");
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

        /**
         *
         * @param facesContext
         * @param component
         * @param object
         * @return
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof OpdVisit) {
                OpdVisit o = (OpdVisit) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + OpdVisitController.class.getName());
            }
        }
    }
}

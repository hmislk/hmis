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
import com.divudi.entity.BillSession;
import com.divudi.entity.Institution;
import com.divudi.entity.channel.AppointmentActivity;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.entity.channel.SessionInstanceActivity;
import com.divudi.facade.SessionInstanceActivityFacade;
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
public class SessionInstanceActivityController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private SessionInstanceActivityFacade ejbFacade;
    private SessionInstanceActivity current;
    private List<SessionInstanceActivity> items = null;

    public void save(SessionInstanceActivity sessionInstanceActivity) {
        if (sessionInstanceActivity == null) {
            return;
        }
        if (sessionInstanceActivity.getId() != null) {
            getFacade().edit(sessionInstanceActivity);
        } else {
            sessionInstanceActivity.setCreatedAt(new Date());
            sessionInstanceActivity.setCreater(getSessionController().getLoggedUser());
            getFacade().create(sessionInstanceActivity);
        }
    }

    // Base method with all parameters for returning a list
    public List<SessionInstanceActivity> findSessionInstanceActivities(SessionInstance si, AppointmentActivity aa, BillSession bs) {
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);

        String jpql = "select a from SessionInstanceActivity a where a.retired=:ret ";

        if (si != null) {
            jpql += "and a.sessionInstance=:si ";
            params.put("si", si);
        }
        if (aa != null) {
            jpql += "and a.appointmentActivity=:aa ";
            params.put("aa", aa);
        }
        if (bs != null) {
            jpql += "and a.billSession=:bs ";
            params.put("bs", bs);
        }

        return getFacade().findByJpql(jpql, params);
    }

    // Overloaded method for SessionInstance and AppointmentActivity only
    public List<SessionInstanceActivity> findSessionInstanceActivities(SessionInstance si, AppointmentActivity appointmentActivity) {
        return findSessionInstanceActivities(si, appointmentActivity, null);
    }

    // Overloaded method for SessionInstance and BillSession only
    public List<SessionInstanceActivity> findSessionInstanceActivities(SessionInstance si, BillSession billSession) {
        return findSessionInstanceActivities(si, null, billSession);
    }

    // Overloaded method for AppointmentActivity and BillSession only
    public List<SessionInstanceActivity> findSessionInstanceActivities(AppointmentActivity appointmentActivity, BillSession billSession) {
        return findSessionInstanceActivities(null, appointmentActivity, billSession);
    }

    // Overloaded method for SessionInstance only
    public List<SessionInstanceActivity> findSessionInstanceActivities(SessionInstance si) {
        return findSessionInstanceActivities(si, null, null);
    }

    // Overloaded method for AppointmentActivity only
    public List<SessionInstanceActivity> findSessionInstanceActivities(AppointmentActivity appointmentActivity) {
        return findSessionInstanceActivities(null, appointmentActivity, null);
    }

    // Overloaded method for BillSession only
    public List<SessionInstanceActivity> findSessionInstanceActivities(BillSession billSession) {
        return findSessionInstanceActivities(null, null, billSession);
    }

    // Helper method to build JPQL query dynamically based on parameters availability
    private String buildJpqlQuery(boolean hasSI, boolean hasAA, boolean hasBS) {
        String jpql = "select a from SessionInstanceActivity a where a.retired=:ret ";
        if (hasSI) {
            jpql += "and a.sessionInstance=:si ";
        }
        if (hasAA) {
            jpql += "and a.appointmentActivity=:aa ";
        }
        if (hasBS) {
            jpql += "and a.billSession=:bs ";
        }
        return jpql;
    }

    // Method to find a single SessionInstanceActivity
    public SessionInstanceActivity findSessionInstanceActivity(SessionInstance si, AppointmentActivity aa, BillSession bs) {
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);

        String jpql = "select a from SessionInstanceActivity a where a.retired=:ret ";

        if (si != null) {
            jpql += "and a.sessionInstance=:si ";
            params.put("si", si);
        }
        if (aa != null) {
            jpql += "and a.appointmentActivity=:aa ";
            params.put("aa", aa);
        }
        if (bs != null) {
            jpql += "and a.billSession=:bs ";
            params.put("bs", bs);
        }

        return getFacade().findFirstByJpql(jpql, params);
    }

    // Overloaded method for SessionInstance and AppointmentActivity only
    public SessionInstanceActivity findSessionInstanceActivityByName(SessionInstance si, AppointmentActivity appointmentActivity) {
        return findSessionInstanceActivity(si, appointmentActivity, null);
    }

    // Overloaded method for SessionInstance and BillSession only
    public SessionInstanceActivity findSessionInstanceActivityByName(SessionInstance si, BillSession billSession) {
        return findSessionInstanceActivity(si, null, billSession);
    }

    // Overloaded method for AppointmentActivity and BillSession only
    public SessionInstanceActivity findSessionInstanceActivityByName(AppointmentActivity appointmentActivity, BillSession billSession) {
        return findSessionInstanceActivity(null, appointmentActivity, billSession);
    }

    // Overloaded method for SessionInstance only
    public SessionInstanceActivity findSessionInstanceActivityByName(SessionInstance si) {
        return findSessionInstanceActivity(si, null, null);
    }

    // Overloaded method for AppointmentActivity only
    public SessionInstanceActivity findSessionInstanceActivityByName(AppointmentActivity appointmentActivity) {
        return findSessionInstanceActivity(null, appointmentActivity, null);
    }

    // Overloaded method for BillSession only
    public SessionInstanceActivity findSessionInstanceActivityByName(BillSession billSession) {
        return findSessionInstanceActivity(null, null, billSession);
    }

    public List<SessionInstanceActivity> completeSessionInstanceActivity(String qry) {
        List<SessionInstanceActivity> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from SessionInstanceActivity c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new SessionInstanceActivity();
    }

    public void recreateModel() {
        items = null;
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
        recreateModel();
        getItems();
    }

    private SessionInstanceActivityFacade getEjbFacade() {
        return ejbFacade;
    }

    private SessionInstanceActivityFacade getFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SessionInstanceActivityController() {
    }

    public SessionInstanceActivity getCurrent() {
        if (current == null) {
            current = new SessionInstanceActivity();
        }
        return current;
    }

    public void setCurrent(SessionInstanceActivity current) {
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
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public List<SessionInstanceActivity> getItems() {
        if (items == null) {
            String j;
            j = "select a "
                    + " from SessionInstanceActivity a "
                    + " where a.retired=false "
                    + " order by a.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = SessionInstanceActivity.class)
    public static class SessionInstanceActivityConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SessionInstanceActivityController controller = (SessionInstanceActivityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "sessionInstanceActivityController");
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
            if (object instanceof SessionInstanceActivity) {
                SessionInstanceActivity o = (SessionInstanceActivity) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SessionInstanceActivityController.class.getName());
            }
        }
    }

}

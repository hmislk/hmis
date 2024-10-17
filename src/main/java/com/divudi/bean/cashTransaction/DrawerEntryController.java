package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.BillFee;
import com.divudi.entity.cashTransaction.DrawerEntry;
import com.divudi.facade.DrawerEntryFacade;
import com.divudi.service.DrawerEntryService;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.persistence.TemporalType;

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
    private DrawerEntryService service;

    @Inject
    private SessionController sessionController;

    private DrawerEntry current;
    private List<DrawerEntry> userDrawerEntry;
    private Date fromDate;
    private Date toDate;

    public DrawerEntryController() {
    }

    public void saveCurrent() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select");
            return;
        }
        service.save(current, sessionController.getLoggedUser());
    }

    public void saveCurrent(DrawerEntry drawerEntry) {
        if (drawerEntry == null) {
            JsfUtil.addErrorMessage("Select");
            return;
        }
        service.save(drawerEntry, sessionController.getLoggedUser());
    }

    public void myDrawerEntrys() {
        userDrawerEntry = new ArrayList();
        String jpql = "select de"
                + " from DrawerEntry de"
                + " where de.retired=:ret"
                + " and de.webUser=:wu";

        Map m = new HashMap();

        jpql += " order by de.id desc";

        m.put("ret", false);
        m.put("wu", sessionController.getLoggedUser());
        List<DrawerEntry> result = ejbFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP, 50);

        // Reverse the list to get the last entry at the end
        Collections.reverse(result);

        // Assign to the class field
        userDrawerEntry = result;
        System.out.println("userDrawerEntry = " + userDrawerEntry);
    }

    public String navigateToMyDrawerEntry() {
        userDrawerEntry = new ArrayList();
        myDrawerEntrys();
        return "/cashier/my_drawer_entry_history?faces-redirect=true;";
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

    public DrawerEntryService getService() {
        return service;
    }

    public void setService(DrawerEntryService service) {
        this.service = service;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<DrawerEntry> getUserDrawerEntry() {
        return userDrawerEntry;
    }

    public void setUserDrawerEntry(List<DrawerEntry> userDrawerEntry) {
        this.userDrawerEntry = userDrawerEntry;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
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

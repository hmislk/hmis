package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.DrawerEntry;
import com.divudi.core.facade.DrawerEntryFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.DrawerService;
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
    private DrawerService service;

    @Inject
    private SessionController sessionController;

    private DrawerEntry current;
    private List<DrawerEntry> userDrawerEntry;
    private Date fromDate;
    private Date toDate;
    private PaymentMethod paymentMethod;

    private WebUser webUser;

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

    public void makeNull() {
        fromDate = null;
        toDate = null;
        webUser = null;
    }

    public void findDrawerEntrys(Date fromDate, Date toDate, WebUser webUser, int resultCount) {
        findDrawerEntrys(fromDate, toDate, webUser, resultCount, null);
    }

    public void findDrawerEntrys(Date fromDate, Date toDate, WebUser webUser, int resultCount, PaymentMethod pm) {
        userDrawerEntry = new ArrayList();
        String jpql = "select de"
                + " from DrawerEntry de"
                + " where de.retired=:ret"
                + " and de.webUser=:wu";

        Map m = new HashMap();

        if (fromDate != null && toDate != null) {
            jpql += " and de.createdAt between :fd and :td";
            m.put("fd", fromDate);
            m.put("td", toDate);
        }

        if (pm != null) {
            jpql += " and de.paymentMethod =:pm ";
            m.put("pm", pm);
        }

        jpql += " order by de.id desc";

        m.put("ret", false);
        m.put("wu", webUser);
        List<DrawerEntry> result = new ArrayList();
        if (resultCount == 0) {
            result = ejbFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        } else {
            result = ejbFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP, resultCount);
        }

        // Reverse the list to get the last entry at the end
        Collections.reverse(result);

        // Assign to the class field
        userDrawerEntry = result;
        System.out.println("userDrawerEntry = " + userDrawerEntry);
    }

    public void findAllUsersDrawerDetails() {
        userDrawerEntry = new ArrayList();
        String jpql = "select de"
                + " from DrawerEntry de"
                + " where de.retired=:ret";

        Map m = new HashMap();

        if (fromDate != null && toDate != null) {
            jpql += " and de.createdAt between :fd and :td";
            m.put("fd", fromDate);
            m.put("td", toDate);
        }
        if (webUser != null) {
            jpql += " and de.webUser=:wu ";
            m.put("wu", webUser);
        }

        jpql += " order by de.id desc";

        m.put("ret", false);
        List<DrawerEntry> result = new ArrayList();
        result = ejbFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

        // Reverse the list to get the last entry at the end
        Collections.reverse(result);

        // Assign to the class field
        userDrawerEntry = result;
        System.out.println("userDrawerEntry = " + userDrawerEntry);
    }

    public String navigateToMyDrawerEntry() {
        return "/cashier/my_drawer_entry_history?faces-redirect=true";
    }

    public void listMyDrawerHistory() {
        findDrawerEntrys(fromDate, toDate, sessionController.getLoggedUser(), 0, paymentMethod);
    }

    public String navigateToCashierDrawerEntry() {
        makeNull();
        return "/cashier/cashier_drawer_entry_history?faces-redirect=true";
    }

    public void prosessingCashierDrawerEntry() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Select Dates..");
            return;
        }
        if (webUser == null) {
            JsfUtil.addErrorMessage("NO User Select");
            return;
        }
        findDrawerEntrys(fromDate, toDate, webUser, 0);
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

    public DrawerService getService() {
        return service;
    }

    public void setService(DrawerService service) {
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
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /*
    Pubudu Start
     */
 /*
    Pubudu End
     */
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

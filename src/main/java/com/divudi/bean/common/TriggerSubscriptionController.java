package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.TriggerType;
import com.divudi.entity.Department;
import com.divudi.entity.TriggerSubscription;
import com.divudi.entity.WebUser;
import com.divudi.facade.TriggerSubscriptionFacade;
import com.divudi.facade.WebUserFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author L C J Samarasekara
 */
@Named
@SessionScoped
public class TriggerSubscriptionController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    private SessionController sessionController;
    @EJB
    private TriggerSubscriptionFacade triggerSubscriptionFacade;
    @EJB
    WebUserFacade webUserFacade;

    private TriggerSubscription current;
    private List<TriggerSubscription> triggerSubscriptions = null;
    private List<TriggerType> triggerTypes;
    private TriggerType triggerType;
    private Department department;
    private List<Department> departments;
    private WebUser user;

    public void addUserSubscription() {
        if (triggerType == null) {
            JsfUtil.addErrorMessage("Select Subscription");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Select Department");
            return;
        }
        if (user == null) {
            JsfUtil.addErrorMessage("Program Error. Cannot have this page without a user. Create an issue in GitHub");
            return;
        }
        double newOrder = getTriggerSubscriptions().size() + 1;
        TriggerSubscription existingTS = findUserSubscriptionByOrder(newOrder);

        Date d = new Date();

        if (existingTS == null) {
            TriggerSubscription ts = new TriggerSubscription();
            ts.setWebUser(user);
            ts.setTriggerType(triggerType);
            ts.setOrderNumber(newOrder);
            ts.setDepartment(department);
            ts.setCreatedAt(d);
            ts.setCreater(sessionController.loggedUser);
            save(ts);
            JsfUtil.addSuccessMessage("Save Success");
            fillDepartmentSubscription();
            reOrderUserSubscriptions();
        } else {
            JsfUtil.addErrorMessage("Icon already exists at this position");
        }

    }

    public void fillDepartmentSubscription() {
        if (user == null) {
            JsfUtil.addErrorMessage("User?");
        }
        Map m = new HashMap();
        String jpql = "SELECT i "
                + " FROM TriggerSubscription i "
                + " where i.webUser=:u "
                + " and i.retired=:ret ";
        if (department != null) {
            jpql += " and i.department=:dep";
            m.put("dep", department);
        }
        m.put("u", user);
        m.put("ret", false);
        triggerSubscriptions = getTriggerSubscriptionFacade().findByJpql(jpql, m);
    }

    public List<WebUser> fillWebUsers(TriggerType tt) {
        List<WebUser> us = new ArrayList<>();
        if (tt == null) {
            return us;
        }
        Map m = new HashMap();
        String jpql = "SELECT i.webUser "
                + " FROM TriggerSubscription i "
                + " where i.triggerType=:tt "
                + " and i.retired=:ret ";

        m.put("tt", tt);
        m.put("ret", false);
        us= webUserFacade.findByJpql(jpql, m);
        return us;
    }

    public void moveSelectedUserSubscriptionUp() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        double currentOrder = current.getOrderNumber();
        TriggerSubscription prevSubscription = findUserSubscriptionByOrder(currentOrder - 1);
        if (prevSubscription != null) {
            prevSubscription.setOrderNumber(currentOrder);
            current.setOrderNumber(currentOrder - 1);
            save(prevSubscription);
            save(current);
        } else {
            JsfUtil.addErrorMessage("Already at the top");
        }
        fillUserSubscriptions();
    }

    public void moveSelectedUserSubscriptionDown() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        double currentOrder = current.getOrderNumber();
        TriggerSubscription nextSubscription = findUserSubscriptionByOrder(currentOrder + 1);
        if (nextSubscription != null) {
            nextSubscription.setOrderNumber(currentOrder);
            current.setOrderNumber(currentOrder + 1);
            save(nextSubscription);
            save(current);
        } else {
            JsfUtil.addErrorMessage("Already at the bottom");
        }
        fillUserSubscriptions();
    }

    private TriggerSubscription findUserSubscriptionByOrder(double order) {
        for (TriggerSubscription ts : triggerSubscriptions) {
            if (ts.getOrderNumber() == order) {
                return ts;
            }
        }
        return null;
    }

    // Method to re-order UserIcons based on orderNumber
    public void reOrderUserSubscriptions() {
        if (triggerSubscriptions == null || triggerSubscriptions.isEmpty()) {
            return;
        }
        double order = 0.0;
        for (TriggerSubscription ts : triggerSubscriptions) {
            ts.setOrderNumber(order++);
            save(ts);
        }
    }

    // Method to validate if the Icon is already added for the user
    public boolean isSubscriptionAlreadyAdded() {
        for (TriggerSubscription ts : triggerSubscriptions) {
            if (ts.getTriggerType() == triggerType) {
                JsfUtil.addErrorMessage("Subscription already added");
                return true;
            }
        }
        return false;
    }

    private void fillUserSubscriptions() {
        triggerSubscriptions = fillUserSubscriptions(user, department);
    }

    public List<TriggerSubscription> fillUserSubscriptions(WebUser u, Department dept) {
        List<TriggerSubscription> tss = null;
        if (u == null) {
            triggerSubscriptions = null;
            return tss;
        }
        String Jpql = "select i "
                + " from TriggerSubscription i "
                + " where i.retired=:ret "
                + " and i.webUser=:u "
                + " and i.department=:dept ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("u", u);
        m.put("dept", dept);
        tss = getFacade().findByJpql(Jpql, m);
        Collections.sort(tss, new UserSubscriptionOrderComparator());
        return tss;
    }

    public void save(TriggerSubscription ts) {
        if (ts == null) {
            return;
        }
        if (ts.getId() != null) {
            getFacade().edit(ts);
        } else {
            getFacade().create(ts);
        }
    }

    public void removeUserSubscription() {
        if (current != null) {
            current.setRetired(true);
            Date d = new Date();
            current.setRetiredAt(d);
            current.setRetirer(sessionController.loggedUser);
            save(current);
            JsfUtil.addSuccessMessage("Removed Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Remove");
        }
    }

    public TriggerSubscriptionController() {
    }

    private TriggerSubscriptionFacade getFacade() {
        return triggerSubscriptionFacade;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public TriggerSubscriptionFacade getTriggerSubscriptionFacade() {
        return triggerSubscriptionFacade;
    }

    public void setTriggerSubscriptionFacade(TriggerSubscriptionFacade triggerSubscriptionFacade) {
        this.triggerSubscriptionFacade = triggerSubscriptionFacade;
    }

    public TriggerSubscription getCurrent() {
        if (current == null) {
            current = new TriggerSubscription();
        }
        return current;
    }

    public void setCurrent(TriggerSubscription current) {
        this.current = current;
        fillUserSubscriptions();
    }

    public List<TriggerSubscription> getTriggerSubscriptions() {
        return triggerSubscriptions;
    }

    public void setTriggerSubscriptions(List<TriggerSubscription> triggerSubscriptions) {
        if (triggerSubscriptions == null) {
            triggerSubscriptions = new ArrayList<>();
        }
        this.triggerSubscriptions = triggerSubscriptions;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
        triggerSubscriptions = fillUserSubscriptions(user, department);
    }

    public List<TriggerType> getTriggerTypes() {
        if (triggerTypes == null) {
            triggerTypes = TriggerType.getAlphabeticallySortedValues();
        }
        return triggerTypes;
    }


    @FacesConverter(forClass = TriggerSubscription.class)
    public static class UserSubscriptionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TriggerSubscriptionController controller = (TriggerSubscriptionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "triggerSubscriptionController");
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
            if (object instanceof TriggerSubscription) {
                TriggerSubscription o = (TriggerSubscription) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + TriggerSubscriptionController.class.getName());
            }
        }
    }

    public static class UserSubscriptionOrderComparator implements Comparator<TriggerSubscription> {

        @Override
        public int compare(TriggerSubscription o1, TriggerSubscription o2) {
            return Double.compare(o1.getOrderNumber(), o2.getOrderNumber());
        }
    }

}

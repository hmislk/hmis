/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.data.TriggerType;
import com.divudi.entity.TriggerSubscription;
import com.divudi.facade.TriggerSubscriptionFacade;
import com.divudi.entity.Department;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.WebUserRole;
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
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Named
@SessionScoped
public class UserRoleTriggerSubscriptionController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    private SessionController sessionController;
    @EJB
    private TriggerSubscriptionFacade triggerSubscriptionFacade;
    private TriggerSubscription current;
    private List<TriggerSubscription> triggerSubscriptions = null;
    private List<TriggerType> triggerTypes;
    private TriggerType triggerType;
    private Department department;
    private List<Department> departments;
    private WebUserRole userRole;
     
    public void addUserRoleSubscription() {
        if (triggerType == null) {
            JsfUtil.addErrorMessage("Select Subscription");
            return;
        }
        if (userRole == null) {
            JsfUtil.addErrorMessage("Program Error. Cannot have this page without a user. Create an issue in GitHub");
            return;
        }

        double newOrder = getTriggerSubscriptions().size() + 1;
        TriggerSubscription existingTS = findUserSubscriptionByOrder(newOrder);

        if (existingTS == null) {
            TriggerSubscription ts = new TriggerSubscription();
            ts.setWebUserRole(userRole);
            ts.setTriggerType(triggerType);
            ts.setOrderNumber(newOrder);
            ts.setDepartment(department);
            ts.setCreatedAt(new Date());
            ts.setCreater(sessionController.loggedUser);
            save(ts);
            JsfUtil.addSuccessMessage("Save Success ");
            fillUserRoleSubscription();
            reOrderUserSubscriptions();
        } else {
            JsfUtil.addErrorMessage("Icon already exists at this position");
        }

    }
    
    public void fillUserRoleSubscription() {
        if (userRole == null) {
            JsfUtil.addErrorMessage("User Role?");
        }
        Map m = new HashMap();
        String jpql = "SELECT i "
                + " FROM TriggerSubscription i "
                + " where i.webUserRole=:u "
                + " and i.retired=:ret ";
        m.put("u", userRole);
        m.put("ret", false);
        triggerSubscriptions = getTriggerSubscriptionFacade().findByJpql(jpql, m);
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
        triggerSubscriptions = fillUserSubscriptions(userRole);
    }

    public List<TriggerSubscription> fillUserSubscriptions(WebUserRole u) {
        List<TriggerSubscription> tss = null;
        if (u == null) {
            triggerSubscriptions = null;
            return tss;
        }
        String Jpql = "select i "
                + " from TriggerSubscription i "
                + " where i.retired=:ret "
                + " and i.webUserRole=:u ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("u", u);
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
     
    public UserRoleTriggerSubscriptionController() {
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
        if(current == null){
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
        if(triggerSubscriptions == null){
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

    public List<TriggerType> getTriggerTypes() {
        if(triggerTypes == null){
            triggerTypes= Arrays.asList(TriggerType.values());
        }
        return triggerTypes;
    }

    public void setTriggerTypes(List<TriggerType> Subscription) {
        this.triggerTypes = triggerTypes;
    }

    public WebUserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(WebUserRole userRole) {
        this.userRole = userRole;
        triggerSubscriptions = fillUserSubscriptions(userRole);
    }
    
    
    
    @FacesConverter(forClass = TriggerSubscription.class)
    public static class UserSubscriptionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserRoleTriggerSubscriptionController controller = (UserRoleTriggerSubscriptionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userRoleTriggerSubscriptionController");
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
                        + object.getClass().getName() + "; expected type: " + UserRoleTriggerSubscriptionController.class.getName());
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

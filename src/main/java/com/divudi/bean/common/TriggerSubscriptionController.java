/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.TriggerType;
import com.divudi.entity.Department;
import com.divudi.entity.TriggerSubscription;
import com.divudi.entity.WebUser;
import com.divudi.facade.TriggerSubscriptionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
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
        triggerTypes = getTriggerSubscriptionFacade().findByJpql(jpql, m);
    }

    public void moveSelectedUserIconUp() {
        
    }
     
    public TriggerSubscriptionController() {
    }

    public List<TriggerType> getTriggerTypes() {
        if (triggerTypes == null) {
            triggerTypes = new ArrayList<>();
        }
        return triggerTypes;
    }

    public void setTriggerTypes(List<TriggerType> triggerType) {
        this.triggerTypes = triggerTypes;
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
        return current;
    }

    public void setCurrent(TriggerSubscription current) {
        this.current = current;
    }

    public List<TriggerSubscription> getTriggerSubscriptions() {
        return triggerSubscriptions;
    }

    public void setTriggerSubscriptions(List<TriggerSubscription> triggerSubscriptions) {
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
    }
    
    
    
}

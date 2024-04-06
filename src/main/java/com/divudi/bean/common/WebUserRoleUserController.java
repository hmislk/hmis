/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.common;

import com.divudi.bean.common.UserPrivilageController.PrivilegeHolder;
import com.divudi.entity.TriggerSubscription;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserRole;
import com.divudi.entity.WebUserRoleUser;
import com.divudi.facade.WebUserRoleUserFacade;
import java.io.Serializable;
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
public class WebUserRoleUserController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    private SessionController sessionController;
    @Inject
    private UserPrivilageController userPrivilageController;
    @EJB
    private WebUserRoleUserFacade facade;
    private WebUserRoleUser current;
    private List<WebUser> users = null;
    private List<WebUserRoleUser> roleUsers = null;
    
    
     
    public void addUsers() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select UserRole");
            return;
        }
        if (current.getWebUserRole() == null) {
            JsfUtil.addErrorMessage("Program Error. Cannot have this page without a user. Create an issue in GitHub");
            return;
        }

        if (isUsersAlreadyAdded()) {
            JsfUtil.addErrorMessage("User already Added to User Role");
        } else {
            WebUserRoleUser ru = new WebUserRoleUser();
            ru.setWebUserRole(current.getWebUserRole());
            ru.setWebUser(current.getWebUser());
            ru.setCreatedAt(new Date());
            ru.setCreater(sessionController.loggedUser);
            save(ru);
            JsfUtil.addSuccessMessage("Save Success ");
            List<PrivilegeHolder> userRolePrivilage = userPrivilageController.getCurrentUserPrivilegeHolders();
            userPrivilageController.saveWebUserPrivileges(current.getWebUser(),userRolePrivilage);
            fillRoleUsers();
        }

    }
    
    public void fillUseRolePrivilage(){
        userPrivilageController.fillUserRolePrivileges(current.getWebUserRole());
    }
    
    public void fillUsers() {
        if (current.getWebUserRole() == null) {
            JsfUtil.addErrorMessage("User Role?");
        }
        Map m = new HashMap();
        String jpql = "SELECT i FROM WebUser i WHERE i.retired = :ret";
        m.put("ret", false);
        users = getFacade().findByJpql(jpql, m);
        fillRoleUsers();
        fillUseRolePrivilage();
    }


    // Method to validate if the Icon is already added for the user
    public boolean isUsersAlreadyAdded() {
        for (WebUser ru : users) {
            if (ru == current.getWebUser()) {
                JsfUtil.addErrorMessage("User already added");
                return true;
            }
        }
        return false;
    }

    private void fillRoleUsers() {
        roleUsers = fillRoleUsers(current.getWebUserRole());
    }

    public List<WebUserRoleUser> fillRoleUsers(WebUserRole u) {
        List<WebUserRoleUser> wus = null;
        if (u == null) {
            roleUsers = null;
            return wus;
        }
        String Jpql = "select i "
                + " from WebUserRoleUser i "
                + " where i.retired=:ret "
                + " and i.webUserRole=:u ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("u", u);
        wus = getFacade().findByJpql(Jpql, m);
//        Collections.sort(wus, new UserSubscriptionOrderComparator());
        return wus;
    }

    public void save(WebUserRoleUser ru) {
        if (ru == null) {
            return;
        }
        if (ru.getId() != null) {
            getFacade().edit(ru);
        } else {
            getFacade().create(ru);
        }
    }
    
    public void removeUser() {
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
        fillRoleUsers();
    }
     
    public WebUserRoleUserController() {
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebUserRoleUserFacade getFacade() {
        return facade;
    }

    public void setFacade(WebUserRoleUserFacade facade) {
        this.facade = facade;
    }

    public WebUserRoleUser getCurrent() {
        if(current == null){
            current = new WebUserRoleUser();
        }
        return current;
    }

    public void setCurrent(WebUserRoleUser current) {
        this.current = current;
    }

    public List<WebUser> getUsers() {
        return users;
    }

    public void setUsers(List<WebUser> users) {
        this.users = users;
    }

    public List<WebUserRoleUser> getRoleUsers() {
        return roleUsers;
    }

    public void setRoleUsers(List<WebUserRoleUser> roleUsers) {
        this.roleUsers = roleUsers;
    }
    
    
    
    
    
    @FacesConverter(forClass = WebUserRoleUser.class)
    public static class WebUserRoleUserControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebUserRoleUserController controller = (WebUserRoleUserController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webUserRoleUserController");
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
            if (object instanceof WebUserRoleUser) {
                WebUserRoleUser o = (WebUserRoleUser) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebUserRoleUser.class.getName());
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

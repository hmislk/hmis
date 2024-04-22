/*
 * Open Hospital Management Information System
 * 
 * Dr M H B Ariyaratne 
 * Acting Consultant (Health Informatics) 
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.WebUserRole;
import com.divudi.facade.WebUserRoleFacade;
import com.google.common.collect.HashBiMap;
import java.io.Serializable;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class WebUserRoleController implements Serializable {

    @Inject
    SessionController sessionController;
    @Inject
    WebUserRoleUserController webUserRoleUserController;
    @EJB
    private WebUserRoleFacade ejbFacade;

    private WebUserRole current;
    private List<WebUserRole> items = null;
    
    
    public String navigateToManageWebUserRoles(){
        items = findAllItems();
        return "/admin/users/user_roles?faces-redirect=true";
    }
    
    public String navigateToManageWebUserRolePrivileges(){
        if(current==null){
            JsfUtil.addErrorMessage("Select a role");
            return null;
        }
        if(current.getId()==null){
            JsfUtil.addErrorMessage("Save first");
            return null;
        }
        return "/admin/users/user_role_privileges?faces-redirect=true";
    }
    
    public String navigateToManageWebUserRoleIcons(){
        if(current==null){
            JsfUtil.addErrorMessage("Select a role");
            return null;
        }
        if(current.getId()==null){
            JsfUtil.addErrorMessage("Save first");
            return null;
        }
        return "/admin/users/user_role_icons?faces-redirect=true";
    }
    
    public String navigateToManageWebUserRoleUsers(){
        if(current==null){
            JsfUtil.addErrorMessage("Select a role");
            return null;
        }
        if(current.getId()==null){
            JsfUtil.addErrorMessage("Save first");
            return null;
        }
        webUserRoleUserController.getCurrent().setWebUserRole(current);
        return "/admin/users/user_role_users?faces-redirect=true";
    }
    
    public String navigateToManageWebUserTriggerSubscriptions(){
        if(current==null){
            JsfUtil.addErrorMessage("Select a role");
            return null;
        }
        if(current.getId()==null){
            JsfUtil.addErrorMessage("Save first");
            return null;
        }
        return "/admin/users/user_role_subscription?faces-redirect=true";
    }
    
    public void toAddNewUserRole(){
        current = new WebUserRole();
    }
    
    public void saveCurrent(){
        save(current);
        items = findAllItems();
        JsfUtil.addSuccessMessage("Saved");
    }
    
    public void save(WebUserRole r){
        if(r==null){
            return;
        }
        if(r.getId()==null){
            r.setCreatedAt(new Date());
            r.setCreater(sessionController.getLoggedUser());
            getFacade().create(r);
        }else{
            getFacade().edit(r);
        }
    }
    
    private List<WebUserRole> findAllItems(){
        String jpql = "Select r "
                + " from WebUserRole r "
                + " where r.retired=:ret"
                + " order by r.name";
        Map m = new HashMap<>();
        m.put("ret", false);
        return getFacade().findByJpql(jpql, m);
    }
    
    public WebUserRoleFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebUserRoleFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebUserRoleController() {
    }

   

    public WebUserRole getCurrent() {
        return current;
    }

    public void setCurrent(WebUserRole current) {
        this.current = current;
    }

    private WebUserRoleFacade getFacade() {
        return ejbFacade;
    }

    public List<WebUserRole> getItems() {
        if (items == null) {
            items = findAllItems();
        }
        return items;
    }
    

    /**
     *
     */
    @FacesConverter(forClass = WebUserRole.class)
    public static class WebUserRoleControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WebUserRoleController controller = (WebUserRoleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "webUserRoleController");
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
            if (object instanceof WebUserRole) {
                WebUserRole o = (WebUserRole) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebUserRole.class.getName());
            }
        }
    }
}

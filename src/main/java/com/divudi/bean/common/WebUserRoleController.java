/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.WebUserRole;
import com.divudi.core.facade.WebUserRoleFacade;
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

    public WebUserRoleController() {
    }
    
    @Inject
    SessionController sessionController;
    @Inject
    WebUserRoleUserController webUserRoleUserController;
    @Inject
    UserPrivilageController userPrivilageController;
    
    @EJB
    private WebUserRoleFacade ejbFacade;

    private WebUserRole current;
    private List<WebUserRole> items = null;
    private List<WebUserRole> activatediItems = null;
    private String comment;

    public String navigateToManageWebUserRoles(){
        items = findAllItems();
        comment = null;
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
        userPrivilageController.setPrivilegesLoaded(false);
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
        if(current == null){
            JsfUtil.addErrorMessage("Error in User Role.");
            return;
        }
        
        if(current.getName() == null || current.getName().trim().isEmpty()){
            JsfUtil.addErrorMessage("Please enter a Name.");
            return;
        }
        
        if(current.getDescription()== null || current.getDescription().trim().isEmpty()){
            JsfUtil.addErrorMessage("Please enter a Description.");
            return;
        }
        
        if (isRoleNameDuplicate(current)) {
            JsfUtil.addErrorMessage("A User Role with this name already exists.");
            return;
        }
        save(current);
        items = findAllItems();
        JsfUtil.addSuccessMessage("Saved");
    }

    private boolean isRoleNameDuplicate(WebUserRole role) {
        if (role == null || role.getName() == null || role.getName().trim().isEmpty()) {
            return false;
        }
        String jpql;
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("name", role.getName().trim());
        if (role.getId() != null) {
            jpql = "SELECT COUNT(r) FROM WebUserRole r "
                 + "WHERE r.retired = :ret "
                 + "AND lower(r.name) = lower(:name) "
                 + "AND r.id != :id";
            m.put("id", role.getId());
        } else {
            jpql = "SELECT COUNT(r) FROM WebUserRole r "
                 + "WHERE r.retired = :ret "
                 + "AND lower(r.name) = lower(:name)";
        }
        Long count = getFacade().findLongByJpql(jpql, m);
        return count != null && count > 0;
    }
    
    public void deleteCurrent() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addErrorMessage("Select a user role to delete");
        }
        
        items = findAllItems();
        current = null;
    }

    public void save(WebUserRole r){
        if(r==null){
            JsfUtil.addErrorMessage("Select the Role");
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
    
    public void removeRole(WebUserRole r){
        if(r == null){
            JsfUtil.addErrorMessage("Select the Role");
            return;
        }
        if(comment == null || comment.isEmpty()){
            JsfUtil.addErrorMessage("Comment is mandatory.");
            return;
        }
        r.setRetired(true);
        r.setRetiredAt(new Date());
        r.setRetirer(sessionController.getLoggedUser());
        r.setRetireComments(comment);
        getFacade().edit(r);
        JsfUtil.addSuccessMessage("Comment is Mandertory");
    }

    private List<WebUserRole> findAllItems(){
        String jpql = "Select r "
                + " from WebUserRole r "
                + " where r.retired=:ret "
                + " order by r.name";
        Map m = new HashMap<>();
        m.put("ret", false);
        return getFacade().findByJpql(jpql, m);
    }
    
    private List<WebUserRole> findActivatedItems(){
        String jpql = "Select r "
                + " from WebUserRole r "
                + " where r.retired=:ret "
                + " and r.activated=:act "
                + " order by r.name";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("act", true);
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<WebUserRole> getActivatediItems() {
        if(activatediItems == null){
            activatediItems = findActivatedItems();
        }
        return activatediItems;
    }

    public void setActivatediItems(List<WebUserRole> activatediItems) {
        this.activatediItems = activatediItems;
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

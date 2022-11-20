/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.data.Privileges;
import com.divudi.data.dataStructure.PrivilageNode;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserPrivilege;
import com.divudi.facade.WebUserPrivilegeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.primefaces.model.TreeNode;

//import org.primefaces.examples.domain.Document;  
//import org.primefaces.model;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class UserPrivilageController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private WebUserPrivilegeFacade ejbFacade;
    private List<WebUserPrivilege> selectedItems;
    private WebUserPrivilege current;
    private WebUser currentWebUser;
    private List<WebUserPrivilege> items = null;
    //private Privileges currentPrivileges;
    private List<Privileges> privilegeList;

    private List<Privileges> allPrivileges;
    private List<Privileges> selectedPrivileges;

    private List<WebUserPrivilege> userPrivilegesForSelectedUserToStartWith;
    private List<WebUserPrivilege> userPrivilegesForSelectedUserToEndWith;

    public String savePrivileges() {
        System.out.println("savePrivileges");
        if (currentWebUser == null) {
            System.out.println("currentWebUser = " + currentWebUser);
            UtilityController.addErrorMessage("Please select a user");
            return "";
        }
        retireAllPrivilege();
        for (Privileges o : getSelectedPrivileges()) {
            updateSinglePrivilege(o, true);
        }
        return "/admin_view_user";
    }

    public void updateSinglePrivilege(Privileges p, boolean selected) {
        System.out.println("updateSinglePrivilege");
        System.out.println("p = " + p);
        System.out.println("selected = " + selected);
        if (p == null) {
            System.out.println("p = " + p);
            return;
        }
        WebUserPrivilege wup;
        Map m = new HashMap();
        m.put("p", p);
        m.put("wu", getCurrentWebUser());
        String sql = "SELECT i "
                + " FROM WebUserPrivilege i "
                + " where i.webUser=:wu "
                + " and i.privilege=:p ";
        wup = getEjbFacade().findFirstBySQL(sql, m);
        System.out.println("wup = " + wup);
        if (wup == null) {
            wup = new WebUserPrivilege();
            wup.setCreater(getSessionController().getLoggedUser());
            wup.setCreatedAt(Calendar.getInstance().getTime());
            wup.setPrivilege(p);
            wup.setRetired(!selected);
            wup.setWebUser(getCurrentWebUser());
            getFacade().create(wup);
        } else {
            wup.setRetired(!selected);
            getFacade().edit(wup);
        }
        System.out.println("wup.isRetired() = " + wup.isRetired());
    }

    public void retireAllPrivilege() {
        if (getCurrentWebUser() == null) {
            return;
        }
        List<WebUserPrivilege> wups;
        Map m = new HashMap();
        m.put("wu", getCurrentWebUser());
        String sql = "SELECT i "
                + " FROM WebUserPrivilege i "
                + " where i.webUser=:wu ";
        wups = getEjbFacade().findBySQL(sql, m);
        for (WebUserPrivilege wup : wups) {
            wup.setRetired(true);
            getFacade().edit(wup);
        }
    }

    public void remove() {
        if (getCurrent() == null) {
            UtilityController.addErrorMessage("Select Privilage");
            return;
        }
        getCurrent().setRetired(true);
        getFacade().edit(getCurrent());
    }

    private void recreateModel() {
        items = null;
    }

    private WebUserPrivilegeFacade getEjbFacade() {
        return ejbFacade;
    }

    private SessionController getSessionController() {
        return sessionController;
    }

    public WebUserPrivilege getCurrent() {
        if (current == null) {
            current = new WebUserPrivilege();
        }
        return current;
    }

    public void setCurrent(WebUserPrivilege current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        createSelectedPrivilegesForUser();
        current = null;
        getCurrent();
    }

    private WebUserPrivilegeFacade getFacade() {
        return ejbFacade;
    }

    public void createSelectedPrivilegesForUser() {
        System.out.println("createRootForUser");
        if (getCurrentWebUser() == null) {
            return;
        }
        String sql = "SELECT i "
                + " FROM WebUserPrivilege i "
                + " where i.webUser=:wu "
                + " and i.retired=:ret";
        Map m = new HashMap();
        m.put("wu", getCurrentWebUser());
        m.put("ret", false);
        items = getEjbFacade().findBySQL(sql, m);
        if (items == null) {
            return;
        }
        Map<String, Privileges> mp = new HashMap();
        for (WebUserPrivilege wup : items) {
            mp.put(wup.getPrivilege().toString(), wup.getPrivilege());
        }
        selectedPrivileges = new ArrayList<>(mp.values());
    }

    public WebUser getCurrentWebUser() {
        return currentWebUser;
    }

    public void setCurrentWebUser(WebUser currentWebUser) {
        this.currentWebUser = currentWebUser;
        createSelectedPrivilegesForUser();
    }

    public List<Privileges> getPrivilegeList() {
        return privilegeList;
    }

    public void setPrivilegeList(List<Privileges> privilegeList) {
        this.privilegeList = privilegeList;

    }

    public List<WebUserPrivilege> getSelectedItems() {
        return selectedItems;
    }

    

    public List<Privileges> getAllPrivileges() {
        if (allPrivileges == null) {
            allPrivileges = new ArrayList<>();
            for (Privileges tp : Privileges.values()) {
                allPrivileges.add(tp);
            }
        }
        return allPrivileges;
    }

    public List<Privileges> getSelectedPrivileges() {
        return selectedPrivileges;
    }

    public void setSelectedPrivileges(List<Privileges> selectedPrivileges) {
        this.selectedPrivileges = selectedPrivileges;
    }

    public List<WebUserPrivilege> getUserPrivilegesForSelectedUserToStartWith() {
        return userPrivilegesForSelectedUserToStartWith;
    }

    public void setUserPrivilegesForSelectedUserToStartWith(List<WebUserPrivilege> userPrivilegesForSelectedUserToStartWith) {
        this.userPrivilegesForSelectedUserToStartWith = userPrivilegesForSelectedUserToStartWith;
    }

    public List<WebUserPrivilege> getUserPrivilegesForSelectedUserToEndWith() {
        return userPrivilegesForSelectedUserToEndWith;
    }

    public void setUserPrivilegesForSelectedUserToEndWith(List<WebUserPrivilege> userPrivilegesForSelectedUserToEndWith) {
        this.userPrivilegesForSelectedUserToEndWith = userPrivilegesForSelectedUserToEndWith;
    }

    /**
     *
     */
    @FacesConverter(forClass = WebUserPrivilege.class)
    public static class WebUserPrivilegeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserPrivilageController controller = (UserPrivilageController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userPrivilegeController");
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
            if (object instanceof WebUserPrivilege) {
                WebUserPrivilege o = (WebUserPrivilege) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserPrivilageController.class.getName());
            }
        }
    }
}

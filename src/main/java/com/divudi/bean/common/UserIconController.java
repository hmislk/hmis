/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.data.Icon;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.UserIcon;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.UserIconFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
public class UserIconController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private UserIconFacade ejbFacade;
    private UserIcon current;
    private List<UserIcon> userIcons = null;
    private List<Icon> icons;
    private WebUser user;
    private Icon icon;
    private Department department;
    private List<Department> departments;

// Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
    public void addUserIcon() {
        if (icon == null) {
            JsfUtil.addErrorMessage("Select Icon");
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

        // Check for an existing icon for the same user and department
        Map<String, Object> params = new HashMap<>();
        params.put("u", user);
        params.put("i", icon);
        params.put("d", department);
        params.put("ret", false);
        String jpql = "select ui from UserIcon ui where ui.webUser=:u and ui.icon=:i and ui.department=:d and ui.retired=:ret";
        UserIcon duplicate = getFacade().findFirstByJpql(jpql, params);
        if (duplicate != null) {
            JsfUtil.addErrorMessage("Icon already added for this department");
            return;
        }

        double newOrder = getUserIcons().size() + 1;
        UserIcon existingUI = findUserIconByOrder(newOrder);

        if (existingUI == null) {
            UserIcon ui = new UserIcon();
            ui.setWebUser(user);
            ui.setIcon(icon);
            ui.setOrderNumber(newOrder);
            ui.setDepartment(department);
            save(ui);
            JsfUtil.addSuccessMessage("Save Success ");
            fillDepartmentIcon();
            reOrderUserIcons();
            // Clear selected icon after successful addition
            icon = null;
        } else {
            JsfUtil.addErrorMessage("Icon already exists at this position");
        }

    }

    public void fillDepartmentIcon() {
        if (user == null) {
            JsfUtil.addErrorMessage("User?");
            return;
        }
        if (department == null) {
            JsfUtil.addErrorMessage("Department?");
            return;
        }
        
        Map m = new HashMap();
        String jpql = "SELECT i "
                + " FROM UserIcon i "
                + " where i.webUser=:u "
                + " and i.retired=:ret ";
        if (department != null) {
            jpql += " and i.department=:dep";
            m.put("dep", department);
        }
        m.put("u", user);
        m.put("ret", false);
        userIcons = getEjbFacade().findByJpql(jpql, m);
    }

    // Modified by Dr M H B Ariyaratne with assistance from ChatGPT from OpenAI
    public void moveSelectedUserIconUp() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        double currentOrder = current.getOrderNumber();
        UserIcon prevIcon = findUserIconByOrder(currentOrder - 1);
        if (prevIcon != null) {
            prevIcon.setOrderNumber(currentOrder);
            current.setOrderNumber(currentOrder - 1);
            save(prevIcon);
            save(current);
        } else {
            JsfUtil.addErrorMessage("Already at the top");
        }
        fillUserIcons();
    }

    public void moveSelectedUserIconDown() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        double currentOrder = current.getOrderNumber();
        UserIcon nextIcon = findUserIconByOrder(currentOrder + 1);
        if (nextIcon != null) {
            nextIcon.setOrderNumber(currentOrder);
            current.setOrderNumber(currentOrder + 1);
            save(nextIcon);
            save(current);
        } else {
            JsfUtil.addErrorMessage("Already at the bottom");
        }
        fillUserIcons();
    }

    private UserIcon findUserIconByOrder(double order) {
        for (UserIcon ui : userIcons) {
            if (ui.getOrderNumber() == order) {
                return ui;
            }
        }
        return null;
    }

    // Method to re-order UserIcons based on orderNumber
    public void reOrderUserIcons() {
        if (userIcons == null || userIcons.isEmpty()) {
            return;
        }
        double order = 0.0;
        for (UserIcon ui : userIcons) {
            ui.setOrderNumber(order++);
            save(ui);
        }
    }

    // Method to validate if the Icon is already added for the user
    public boolean isIconAlreadyAdded() {
        for (UserIcon ui : userIcons) {
            if (ui.getIcon() == icon) {
                JsfUtil.addErrorMessage("Icon already added");
                return true;
            }
        }
        return false;
    }

    private void fillUserIcons() {
        userIcons = fillUserIcons(user, department);
    }

    public List<UserIcon> fillUserIcons(WebUser u, Department dept) {
        List<UserIcon> uis = null;
        if (u == null) {
            userIcons = null;
            return uis;
        }
        String Jpql = "select i "
                + " from UserIcon i "
                + " where i.retired=:ret "
                + " and i.webUser=:u "
                + " and i.department=:dept ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("u", u);
        m.put("dept", dept);
        uis = getFacade().findByJpql(Jpql, m);
        Collections.sort(uis, new UserIconOrderComparator());
        return uis;
    }

    public void save(UserIcon ui) {
        if (ui == null) {
            return;
        }
        if (ui.getId() != null) {
            getFacade().edit(ui);
        } else {
            getFacade().create(ui);
        }
    }

    private UserIconFacade getEjbFacade() {
        return ejbFacade;
    }

    public UserIconController() {
    }

    public UserIcon getCurrent() {
        if (current == null) {
            current = new UserIcon();
        }
        return current;
    }

    public void setCurrent(UserIcon current) {
        this.current = current;
        fillUserIcons();
    }

    public void removeUserIcon() {
        if (current != null) {
            current.setRetired(true);
            save(current);
            JsfUtil.addSuccessMessage("Removed Successfully");
            fillDepartmentIcon();
            reOrderUserIcons();
        } else {
            JsfUtil.addSuccessMessage("Nothing to Remove");
        }
    }

    private UserIconFacade getFacade() {
        return ejbFacade;
    }

    public List<Icon> getIcons() {
        if (icons == null) {
            icons = Arrays.stream(Icon.values())
                    .sorted(Comparator.comparing(Icon::getLabel))
                    .collect(Collectors.toList());
        }
        return icons;
    }

    public List<Icon> getFilteredIcons(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Arrays.asList(Icon.values()); // Return all if no input
        }

        String[] keywords = query.toLowerCase().split("\\s+"); // Split by spaces

        return Arrays.stream(Icon.values())
                .filter(icon -> {
                    String label = icon.getLabel().toLowerCase();
                    return Arrays.stream(keywords).allMatch(label::contains);
                })
                .collect(Collectors.toList());
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
        fillDepartmentIcon();
    }

    public List<UserIcon> getUserIcons() {
        if (userIcons == null) {
            userIcons = new ArrayList<>();
        }
        return userIcons;
    }

    public void setUserIcons(List<UserIcon> userIcons) {
        this.userIcons = userIcons;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
        fillDepartmentIcon();
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    /**
     *
     */
    @FacesConverter(forClass = UserIcon.class)
    public static class UserIconConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserIconController controller = (UserIconController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userIconConverter");
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
            if (object instanceof UserIcon) {
                UserIcon o = (UserIcon) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserIconController.class.getName());
            }
        }
    }

    public static class UserIconOrderComparator implements Comparator<UserIcon> {

        @Override
        public int compare(UserIcon o1, UserIcon o2) {
            return Double.compare(o1.getOrderNumber(), o2.getOrderNumber());
        }
    }

}

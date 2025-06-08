/*
* Dr M H B Ariyaratne, Damith & ChatGPT contribution
 */
package com.divudi.bean.common;

import com.divudi.core.entity.DefaultServiceDepartment;
import com.divudi.core.facade.DefaultServiceDepartmentFacade;
import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.util.JsfUtil;

import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author Buddhika, Damith & ChatGPT
 */
@Named
@SessionScoped
public class DefaultServiceDepartmentController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private DefaultServiceDepartmentFacade ejbFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private DefaultServiceDepartment current;
    private List<DefaultServiceDepartment> items;
    Department orderingDepartment;
    Department serviceDepartment;
    Category category;
    Item item;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public DefaultServiceDepartmentController() {
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation Functions">
    public String navigateToAddNewDefaultServiceDepartment() {
        return "/admin/items/add_default_service_department?faces-redirect=true";
    }

    public String navigateToEditDefaultServiceDepartment() {
        return "/admin/items/edit_default_service_department?faces-redirect=true";
    }

    public String navigateToListDefaultServiceDepartments() {
        items = null;
        return "/admin/items/list_default_service_departments?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    public String saveNewDefaultServiceDepartment() {
        if (orderingDepartment == null) {
            JsfUtil.addErrorMessage("Must have an ordering department");
            return null;
        }
        if (serviceDepartment == null) {
            JsfUtil.addErrorMessage("Must have a service department");
            return null;
        }
        current = getDefaultServiceDepartment(orderingDepartment, serviceDepartment, category, item);
        return navigateToEditDefaultServiceDepartment();
    }

    public void listDefaultServiceDepartments() {
        if (orderingDepartment == null) {
            JsfUtil.addErrorMessage("Must have an ordering department");
            return;
        }
        if (serviceDepartment == null) {
            JsfUtil.addErrorMessage("Must have a service department");
            return;
        }
        items = getDefaultServiceDepartments(orderingDepartment, serviceDepartment, category, item);
    }

    public String saveExistingDefaultServiceDepartment() {
        if (current == null) {
            JsfUtil.addErrorMessage("No record selected");
            return null;
        }
        if (current.getBillingDepartment() == null) {
            JsfUtil.addErrorMessage("Must have an ordering department");
            return null;
        }
        if (current.getPerformingDepartment() == null) {
            JsfUtil.addErrorMessage("Must have a service department");
            return null;
        }
        save();
        return navigateToEditDefaultServiceDepartment();
    }

    public DefaultServiceDepartment getDefaultServiceDepartment(Department orderingDept, Department serviceDept, Category cat, Item itm) {
        if (orderingDept == null || serviceDept == null) {
            JsfUtil.addErrorMessage("Ordering and Service departments are required");
            return null;
        }
        String jpql = "select d "
                + " from DefaultServiceDepartment d "
                + " where d.retired=false "
                + " and d.billingDepartment=:bd "
                + " and d.performingDepartment=:pd "
                + " and d.category=:cat "
                + " and d.item=:itm";
        Map<String, Object> m = new HashMap<>();
        m.put("bd", orderingDept);
        m.put("pd", serviceDept);
        m.put("cat", cat);
        m.put("itm", itm);

        DefaultServiceDepartment dsd = getFacade().findFirstByJpql(jpql, m);

        if (dsd == null) {
            dsd = new DefaultServiceDepartment();
            dsd.setBillingDepartment(orderingDept);
            dsd.setPerformingDepartment(serviceDept);
            dsd.setCategory(cat);
            dsd.setItem(itm);
            dsd.setCreatedAt(new Date());
            dsd.setCreatedBy(sessionController.getLoggedUser());
            getFacade().create(dsd);
        }

        return dsd;
    }

    public List<DefaultServiceDepartment> getDefaultServiceDepartments(Department orderingDept, Department serviceDept, Category cat, Item itm) {
        String jpql = "select d from DefaultServiceDepartment d where d.retired = false and d.billingDepartment = :bd and d.performingDepartment = :pd";
        Map<String, Object> m = new HashMap<>();
        m.put("bd", orderingDept);
        m.put("pd", serviceDept);

        if (cat != null) {
            jpql += " and d.category = :cat";
            m.put("cat", cat);
        }

        if (itm != null) {
            jpql += " and d.item = :itm";
            m.put("itm", itm);
        }

        jpql += " order by d.id";  // optional, ensures stable order

        return getFacade().findByJpql(jpql, m);
    }

    public void save() {
        save(current);
    }

    public void save(DefaultServiceDepartment saving) {
        if (saving == null) {
            JsfUtil.addErrorMessage("No record selected.");
            return;
        }

        if (saving.getId() == null) {
            saving.setCreatedAt(new Date());
            saving.setCreatedBy(sessionController.getLoggedUser());
            getFacade().create(saving);
            JsfUtil.addSuccessMessage("Saved Successfully.");
        } else {
            getFacade().edit(saving);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public DefaultServiceDepartment getCurrent() {
        if (current == null) {
            current = new DefaultServiceDepartment();
        }
        return current;
    }

    public void setCurrent(DefaultServiceDepartment current) {
        this.current = current;
    }

    public List<DefaultServiceDepartment> getItems() {
        return items;
    }

    public Department getOrderingDepartment() {
        return orderingDepartment;
    }

    public void setOrderingDepartment(Department orderingDepartment) {
        this.orderingDepartment = orderingDepartment;
    }

    public Department getServiceDepartment() {
        return serviceDepartment;
    }

    public void setServiceDepartment(Department serviceDepartment) {
        this.serviceDepartment = serviceDepartment;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public DefaultServiceDepartmentFacade getFacade() {
        return ejbFacade;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Inner Classes Static Converter">
    @FacesConverter(forClass = DefaultServiceDepartment.class)
    public static class DefaultServiceDepartmentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            DefaultServiceDepartmentController controller = (DefaultServiceDepartmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "defaultServiceDepartmentController");
            return controller.getFacade().find(getKey(value));
        }

        Long getKey(String value) {
            return Long.valueOf(value);
        }

        String getStringKey(Long value) {
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof DefaultServiceDepartment) {
                DefaultServiceDepartment dsd = (DefaultServiceDepartment) object;
                return getStringKey(dsd.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName()
                        + "; expected type: " + DefaultServiceDepartment.class.getName());
            }
        }
    }
    // </editor-fold>
}

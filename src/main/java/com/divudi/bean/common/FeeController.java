/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.entity.Fee;
import com.divudi.facade.FeeFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.FeeType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
@SuppressWarnings("serial")
public class FeeController implements Serializable {

    @Inject
    SessionController sessionController;
    @EJB
    private FeeFacade ejbFacade;
    List<Fee> selectedItems;
    private Fee current;
    private List<Fee> items = null;
    String selectText = "";

    public List<Fee> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Fee c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Fee();
    }

    public void setSelectedItems(List<Fee> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public Fee findFee(String name) {
        String jpql = "select f "
                + " from Fee f "
                + " where f.retired=:ret "
                + " and f.name=:name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", name);
        Fee fee = getFacade().findFirstByJpql(jpql, m);
        if (fee != null) {
            return fee;
        }
        fee = new Fee();
        fee.setCreatedAt(new Date());
        fee.setCreater(sessionController.getLoggedUser());
        fee.setFeeType(FeeType.Staff);
        getFacade().create(fee);
        return fee;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public FeeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(FeeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public FeeController() {
    }

    public Fee getCurrent() {
        return current;
    }

    public void setCurrent(Fee current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private FeeFacade getFacade() {
        return ejbFacade;
    }

    public List<Fee> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public String navigateToAdminFee() {
        return "/admin/pricing/index?faces-redirect=true";
    }

    public String navigateToAdminDiscounts() {
        return "/admin/pricing/admin_discounts?faces-redirect=true";
    }

    /**
     *
     */
    @FacesConverter(forClass = Fee.class)
    public static class FeeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            FeeController controller = (FeeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "feeController");
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
            if (object instanceof Fee) {
                Fee o = (Fee) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + FeeController.class.getName());
            }
        }
    }
}

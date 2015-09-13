/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.entity.Category;
import com.divudi.entity.ServiceSubCategory;
import com.divudi.facade.ServiceSubCategoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class ServiceSubCategoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ServiceSubCategoryFacade ejbFacade;
    List<ServiceSubCategory> selectedItems;
    private ServiceSubCategory current;
    private Category parentCategory;
    private List<ServiceSubCategory> items = null;
    String selectText = "";

    public List<ServiceSubCategory> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from ServiceSubCategory c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new ServiceSubCategory();
    }

    public void setSelectedItems(List<ServiceSubCategory> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        current = new ServiceSubCategory();
    }

    public void saveSelected() {
        if (getParentCategory() == null) {
            UtilityController.addErrorMessage("Select Parent Catogorfy");
            return;
        }

        getCurrent().setParentCategory(getParentCategory());

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {

            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();

        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public ServiceSubCategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ServiceSubCategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ServiceSubCategoryController() {
        current = new ServiceSubCategory();
    }

    public ServiceSubCategory getCurrent() {
        return current;
    }

    public void setCurrent(ServiceSubCategory current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private ServiceSubCategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<ServiceSubCategory> getItems() {
        if (getParentCategory() == null) {
            return new ArrayList<>();
        }

        String sql = "SELECT sb from ServiceSubCategory sb "
                + " WHERE sb.retired=false "
                + " AND sb.parentCategory=:parent";

        HashMap hm = new HashMap();
        hm.put("parent", getParentCategory());
        items = getFacade().findBySQL(sql, hm);

        if (items == null) {
            return new ArrayList<>();
        }

        return items;
    }

    public Category getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(Category parentCategory) {
        this.parentCategory = parentCategory;
        current = new ServiceSubCategory();
    }

    /**
     *
     */
    @FacesConverter(forClass = ServiceSubCategory.class)
    public static class ServiceSubCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ServiceSubCategoryController controller = (ServiceSubCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "serviceSubCategoryController");
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
            if (object instanceof ServiceSubCategory) {
                ServiceSubCategory o = (ServiceSubCategory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ServiceSubCategoryController.class.getName());
            }
        }
    }
}

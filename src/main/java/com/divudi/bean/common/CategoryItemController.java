/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Category;
import com.divudi.entity.CategoryItem;

import com.divudi.entity.Institution;
import com.divudi.facade.CategoryItemFacade;

import java.io.Serializable;
import java.util.ArrayList;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * (Health Informatics)
 */
@Named
@SessionScoped
public class CategoryItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private CategoryItemFacade ejbFacade;
    private CategoryItem current;
    private List<CategoryItem> items = null;

    public void save(CategoryItem categoryItem) {
        if (categoryItem == null) {
            return;
        }
        if (categoryItem.getId() != null) {
            getFacade().edit(categoryItem);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            categoryItem.setCreatedAt(new Date());
            categoryItem.setCreater(sessionController.getLoggedUser());
            getFacade().create(categoryItem);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public CategoryItemFacade getFacade() {
        return ejbFacade;
    }

    public CategoryItemController() {
    }

    public CategoryItem getCurrent() {
        if (current == null) {
            current = new CategoryItem();
        }
        return current;
    }

    public void setCurrent(CategoryItem current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        getItems();
        current = null;
        getCurrent();
    }

    public List<CategoryItem> getItems() {
        if (items == null) {
            String j;
            j = "select a "
                    + " from CategoryItem a "
                    + " where a.retired=false "
                    + " order by a.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    public List<CategoryItem> fillCategoryItems(Category cat) {
        String j;
        j = "select a "
                + " from CategoryItem a "
                + " where a.retired=false"
                + " and a.category=:cat "
                + " order by a.name";
        Map m = new HashMap();
        m.put("cat", cat);
        items = getFacade().findByJpql(j,m);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = CategoryItem.class)
    public static class CategoryItemConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CategoryItemController controller = (CategoryItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "categoryItemController");
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
            if (object instanceof CategoryItem) {
                CategoryItem o = (CategoryItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + CategoryItem.class.getName());
            }
        }
    }

}

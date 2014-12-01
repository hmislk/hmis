/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import java.util.TimeZone;
import com.divudi.data.ReportItemType;
import com.divudi.entity.Category;
import com.divudi.facade.CategoryFacade;
import com.divudi.entity.MetadataSuperCategory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class MetadataSuperCategoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private CategoryFacade ejbFacade;
    List<MetadataSuperCategory> selectedItems;
    private MetadataSuperCategory current;
    private List<MetadataSuperCategory> items = null;
    String selectText = "";
    Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        items = null;
    }

    public MetadataSuperCategoryController() {

    }

    public void removeItem() {
        current.setRetired(true);
        current.setRetirer(getSessionController().getLoggedUser());
        current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getEjbFacade().edit(getCurrent());
        getItems().remove(getCurrent());
    }

    public void prepareAdd() {
        current = new MetadataSuperCategory();
    }

    public void setSelectedItems(List<MetadataSuperCategory> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
        current = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            getCurrent().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
//        recreateModel();
//        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public CategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(CategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public MetadataSuperCategory getCurrent() {
        if (current == null) {
            current = new MetadataSuperCategory();
        }
        return current;
    }

    public void setCurrent(MetadataSuperCategory current) {
        this.current = current;
    }

    private CategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<MetadataSuperCategory> getItems() {
        if (items != null) {
            return items;
        }
        String temSql;
        if (category != null) {
            temSql = "SELECT i FROM MetadataSuperCategory i where i.retired=false and i.category=:cat order by i.name";
            Map m = new HashMap();
            m.put("cat", category);
            //System.out.println("common report cat sql is " + temSql + " and " + m.toString());
            items = getFacade().findBySQL(temSql, m);
        } else {
            items = new ArrayList<>();
        }
        return items;
    }

    public List<MetadataSuperCategory> getCategoryItems(Category cat) {
        List<MetadataSuperCategory> cis;
        String temSql;
        if (cat != null) {
            temSql = "SELECT i FROM MetadataSuperCategory i where i.retired=false and i.category=:cat order by i.name";
            Map m = new HashMap();
            m.put("cat", cat);
            //System.out.println("common report cat sql is " + temSql + " and " + m.toString());
            cis = getFacade().findBySQL(temSql, m);
        } else {
            cis = new ArrayList<>();
        }
        return cis;
    }

    /**
     *
     */
    @FacesConverter(forClass = MetadataSuperCategory.class)
    public static class MetadataSuperCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MetadataSuperCategoryController controller = (MetadataSuperCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "metadataSuperCategoryController");
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
            if (object instanceof MetadataSuperCategory) {
                MetadataSuperCategory o = (MetadataSuperCategory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + MetadataSuperCategoryController.class.getName());
            }
        }
    }
}

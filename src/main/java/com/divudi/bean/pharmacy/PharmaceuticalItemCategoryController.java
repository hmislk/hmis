/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.facade.PharmaceuticalItemCategoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class PharmaceuticalItemCategoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PharmaceuticalItemCategoryFacade ejbFacade;
    private PharmaceuticalItemCategory current;
    private List<PharmaceuticalItemCategory> items = null;
    List<PharmaceuticalItemCategory> pharmaceuticalItemCategoryList = null;
  
    public List<PharmaceuticalItemCategory> completeCategory(String qry) {
        
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        String sql = "select c from PharmaceuticalItemCategory c where "
                + " c.retired=false and (upper(c.name) like :n) order by c.name";

        pharmaceuticalItemCategoryList = getFacade().findBySQL(sql, m, 20);
        ////System.out.println("a size is " + a.size());

        if (pharmaceuticalItemCategoryList == null) {
            pharmaceuticalItemCategoryList = new ArrayList<>();
        }
        return pharmaceuticalItemCategoryList;
    }

    public void prepareAdd() {
        current = new PharmaceuticalItemCategory();
    }

   
    

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

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

   
    public PharmaceuticalItemCategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PharmaceuticalItemCategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PharmaceuticalItemCategoryController() {
    }

    public PharmaceuticalItemCategory getCurrent() {
        if (current == null) {
            current = new PharmaceuticalItemCategory();
        }
        return current;
    }

    public void setCurrent(PharmaceuticalItemCategory current) {
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

    private PharmaceuticalItemCategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<PharmaceuticalItemCategory> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = PharmaceuticalItemCategory.class)
    public static class PharmaceuticalItemCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PharmaceuticalItemCategoryController controller = (PharmaceuticalItemCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "pharmaceuticalItemCategoryController");
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
            if (object instanceof PharmaceuticalItemCategory) {
                PharmaceuticalItemCategory o = (PharmaceuticalItemCategory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PharmaceuticalItemCategoryController.class.getName());
            }
        }
    }
}

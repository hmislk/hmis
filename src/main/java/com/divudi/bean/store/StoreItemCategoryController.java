/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.store;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.pharmacy.AssetCategory;
import com.divudi.entity.pharmacy.StoreItemCategory;
import com.divudi.facade.AssetCategoryFacade;
import com.divudi.facade.StoreItemCategoryFacade;
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
public class StoreItemCategoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private StoreItemCategoryFacade ejbFacade;
    @EJB
    AssetCategoryFacade assetCategoryFacade;
    private StoreItemCategory current;
    private List<StoreItemCategory> items = null;

    public List<StoreItemCategory> completeCategory(String qry) {
        List<StoreItemCategory> a = null;
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        String sql = "select c from StoreItemCategory c where "
                + " c.retired=false and (upper(c.name) like :n) order by c.name";

        a = getFacade().findBySQL(sql, m, 20);
        ////System.out.println("a size is " + a.size());

        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }
    
    public List<AssetCategory> completeAssetCategory(String qry) {
        List<AssetCategory> a = null;
        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        String sql = "select c from AssetCategory c where "
                + " c.retired=false and (upper(c.name) like :n) order by c.name";

        a = getAssetCategoryFacade().findBySQL(sql, m, 20);
        ////System.out.println("a size is " + a.size());

        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public void prepareAdd() {
        current = new StoreItemCategory();
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

  
    public StoreItemCategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(StoreItemCategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StoreItemCategoryController() {
    }

    public StoreItemCategory getCurrent() {
        if (current == null) {
            current = new StoreItemCategory();
        }
        return current;
    }

    public AssetCategoryFacade getAssetCategoryFacade() {
        return assetCategoryFacade;
    }

    public void setAssetCategoryFacade(AssetCategoryFacade assetCategoryFacade) {
        this.assetCategoryFacade = assetCategoryFacade;
    }
    
    

    public void setCurrent(StoreItemCategory current) {
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

    private StoreItemCategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<StoreItemCategory> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = StoreItemCategory.class)
    public static class StoreItemCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StoreItemCategoryController controller = (StoreItemCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "storeItemCategoryController");
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
            if (object instanceof StoreItemCategory) {
                StoreItemCategory o = (StoreItemCategory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StoreItemCategoryController.class.getName());
            }
        }
    }
}

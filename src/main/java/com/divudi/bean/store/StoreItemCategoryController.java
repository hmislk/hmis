/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.store;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.pharmacy.AssetCategory;
import com.divudi.entity.pharmacy.StoreItemCategory;
import com.divudi.facade.AssetCategoryFacade;
import com.divudi.facade.StoreItemCategoryFacade;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
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
                + " c.retired=false and ((c.name) like :n) order by c.name";

        a = getFacade().findByJpql(sql, m, 20);
        //////// // System.out.println("a size is " + a.size());

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
                + " c.retired=false and ((c.name) like :n) order by c.name";

        a = getAssetCategoryFacade().findByJpql(sql, m, 20);
        //////// // System.out.println("a size is " + a.size());

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

    private StoreItemCategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<StoreItemCategory> getItems() {
        if (items == null) {
            String j;
            j = "select c from StoreItemCategory c where c.retired=false order by c.name";
            items = getFacade().findByJpql(j);
        }
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

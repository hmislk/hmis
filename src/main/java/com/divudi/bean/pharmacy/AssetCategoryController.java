package com.divudi.bean.pharmacy;


import com.divudi.bean.common.SessionController;
import com.divudi.entity.pharmacy.AssetCategory;
import com.divudi.facade.AssetCategoryFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.common.util.JsfUtil.PersistAction;
//import com.divudi.bean.common.util.JsfUtil.PersistAction;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

@Named("assetCategoryController")
@SessionScoped
public class AssetCategoryController implements Serializable {

    @EJB
    private AssetCategoryFacade ejbFacade;
    @Inject
    SessionController sessionController;
    private List<AssetCategory> items = null;
    private AssetCategory selected;

    public AssetCategoryController() {
    }

    public AssetCategory getSelected() {
        return selected;
    }

    public void setSelected(AssetCategory selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private AssetCategoryFacade getFacade() {
        return ejbFacade;
    }

    public AssetCategory prepareCreate() {
        selected = new AssetCategory();
        initializeEmbeddableKey();
        return selected;
    }

    public void create() {
        persist(PersistAction.CREATE, "Asset Category Created");
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, "Asset Category Updated");
    }

    public void destroy() {
        persist(PersistAction.DELETE, "Asset Category Deleted");
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<AssetCategory> getItems() {
        if (items == null) {
            items = getFacade().findAll(true);
        }
        return items;
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    selected.setRetired(true);
                    selected.setRetireComments("");
                    selected.setRetiredAt(new Date());
                    selected.setRetirer(getSessionController().getLoggedUser());
                    getFacade().edit(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ("PersistenceErrorOccured"));
            }
        }
    }

    public AssetCategory getAssetCategory(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<AssetCategory> getItemsAvailableSelectMany() {
        return getFacade().findAll(true);
    }

    public List<AssetCategory> getItemsAvailableSelectOne() {
        return getFacade().findAll(true);
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    
    
    @FacesConverter(forClass = AssetCategory.class)
    public static class AssetCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AssetCategoryController controller = (AssetCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "assetCategoryController");
            return controller.getAssetCategory(getKey(value));
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
            if (object instanceof AssetCategory) {
                AssetCategory o = (AssetCategory) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), AssetCategory.class.getName()});
                return null;
            }
        }

    }

}

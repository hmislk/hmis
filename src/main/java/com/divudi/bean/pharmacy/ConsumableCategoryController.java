package com.divudi.bean.pharmacy;

import com.divudi.entity.pharmacy.ConsumableCategory;
import com.divudi.facade.ConsumableCategoryFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.common.util.JsfUtil.PersistAction;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;
import javax.persistence.TemporalType;

@Named("consumableCategoryController")
@SessionScoped
public class ConsumableCategoryController implements Serializable {

    @EJB
    private ConsumableCategoryFacade ejbFacade;
    private List<ConsumableCategory> items = null;
    private ConsumableCategory selected;

    public ConsumableCategoryController() {
    }

    public ConsumableCategory getSelected() {
        return selected;
    }

    public void setSelected(ConsumableCategory selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    public ConsumableCategoryFacade getFacade() {
        return ejbFacade;
    }

    public ConsumableCategory prepareCreate() {
        selected = new ConsumableCategory();
        initializeEmbeddableKey();
        return selected;
    }

    public void create() {
        if (errorCheck()) {
            return;
        }
        persist(PersistAction.CREATE, "ConsumableCategoryCreated");
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        if (errorCheck()) {
            return;
        }
        persist(PersistAction.UPDATE, "ConsumableCategoryUpdated");
    }

    public void destroy() {
        persist(PersistAction.DELETE, "ConsumableCategoryDeleted");
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<ConsumableCategory> getItems() {
        if (items == null) {
            items = getFacade().findAll();
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
                    getFacade().remove(selected);
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

    public ConsumableCategory getConsumableCategory(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<ConsumableCategory> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<ConsumableCategory> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

//    public List<ConsumableCategory> completeConsumableCategory(String qry) {
//        List<ConsumableCategory> cc;
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select c from Category c"
//                + "  where c.retired=false"
//                + " and (type(c)= :parm ) "
//                + " and (c.name) like :q "
//                + " order by c.name";
//
//        temMap.put("parm", ConsumableCategory.class);
//        temMap.put("q", "%" + qry.toUpperCase() + "%");
//
//        cc = getFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        if (cc == null) {
//            cc = new ArrayList<>();
//        }
//        return cc;
//    }

    private boolean errorCheck() {
        if (getSelected() != null) {
            if (getSelected().getCode() == null || getSelected().getCode().isEmpty()) {
                return false;
            } else {
                String sql;
                Map m = new HashMap();

                sql = " select c from ConsumableCategory c where "
                        + " c.retired=false "
                        + " and c.code=:code ";

                m.put("code", getSelected().getCode());
                List<ConsumableCategory> list = getFacade().findByJpql(sql, m);
                if (list.size() > 0) {
                    JsfUtil.addErrorMessage("Category Code " + getSelected().getCode() + " is alredy exsist.");
                    getSelected().setCode("");
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    @FacesConverter(forClass = ConsumableCategory.class)
    public static class ConsumableCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ConsumableCategoryController controller = (ConsumableCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "consumableCategoryController");
            return controller.getConsumableCategory(getKey(value));
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
            if (object instanceof ConsumableCategory) {
                ConsumableCategory o = (ConsumableCategory) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), ConsumableCategory.class.getName()});
                return null;
            }
        }

    }

    /**
     *
     */
   
}

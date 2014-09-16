/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.entity.BillItem;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.util.JsfUtil;
import com.divudi.facade.util.JsfUtil.PersistAction;
import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class BillItemController implements Serializable {

    @EJB
    private com.divudi.facade.BillItemFacade ejbFacade;
    private List<BillItem> items = null;
    private BillItem selected;

    public BillItemController() {
    }

    public BillItem getSelected() {
        return selected;
    }

    public void setSelected(BillItem selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private BillItemFacade getFacade() {
        return ejbFacade;
    }

    public BillItem prepareCreate() {
        selected = new BillItem();
        initializeEmbeddableKey();
        return selected;
    }

    public void create() {
        persist(PersistAction.CREATE, ResourceBundle.getBundle("/Bundle").getString("BillItemCreated"));
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, ResourceBundle.getBundle("/Bundle").getString("BillItemUpdated"));
    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("/Bundle").getString("BillItemDeleted"));
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public List<BillItem> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    public void setItems(List<BillItem> items) {
        this.items = items;
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
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            }
        }
    }

    public List<BillItem> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<BillItem> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    public BillItem findBillItemInListBySerial(int serial) {
        for (BillItem bi : items) {
            if (serial == bi.getSearialNo()) {
                return bi;
            }
        }
        return null;
    }

    @FacesConverter("temBillItemConverter")
    public static class TemBillItemConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                System.out.println("value = " + value);
                System.out.println("value.length() = " + value.length());
                return null;
            }
            BillItemController controller = (BillItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billItemController");
            if (controller == null) {
                System.out.println("null controller");
                return null;
            }
            if (getKey(value) == null) {
                System.out.println("value null");
                return null;
            }
            return controller.findBillItemInListBySerial(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            if(value==null){
                return 0;
            }
            try {
                java.lang.Integer key;
                key = Integer.valueOf(value);
                return key;
            } catch (Exception e) {
                System.out.println("e = " + e);
                return 0;
            }
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
            if (object instanceof BillItem) {
                BillItem o = (BillItem) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), BillItem.class.getName()});
                return null;
            }
        }

    }
//
//    @FacesConverter(forClass = BillItem.class)
//    public static class BillItemControllerConverter implements Converter {
//
//        @Override
//        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
//            if (value == null || value.length() == 0) {
//                System.out.println("value = " + value);
//                System.out.println("value.length() = " + value.length());
//                return null;
//            }
//            BillItemController controller = (BillItemController) facesContext.getApplication().getELResolver().
//                    getValue(facesContext.getELContext(), null, "billItemController");
//            if (controller == null) {
//                System.out.println("null controller");
//                return null;
//            }
//            if (controller.getFacade() == null) {
//                System.out.println("facade null");
//                return null;
//            }
//            if (getKey(value) == null) {
//                System.out.println("value null");
//                return null;
//            }
//            return controller.getFacade().find(getKey(value));
//        }
//
//        java.lang.Long getKey(String value) {
//            java.lang.Long key;
//            key = Long.valueOf(value);
//            return key;
//        }
//
//        String getStringKey(java.lang.Long value) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(value);
//            return sb.toString();
//        }
//
//        @Override
//        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
//            if (object == null) {
//                return null;
//            }
//            if (object instanceof BillItem) {
//                BillItem o = (BillItem) object;
//                return getStringKey(o.getId());
//            } else {
//                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), BillItem.class.getName()});
//                return null;
//            }
//        }
//
//    }

}

/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.entity.BillItem;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillItemController implements Serializable {

    private List<BillItem> items = null;
    private BillItem selected;

    public List<BillItem> getItems() {
        return items;
    }

    public void setItems(List<BillItem> items) {
        this.items = items;
    }

    public BillItem getSelected() {
        return selected;
    }

    public void setSelected(BillItem selected) {
        this.selected = selected;
    }

    public BillItemController() {
    }

    public BillItem findBillItemInListBySerial(Integer id) {
        if (items == null) {
            return null;
        }
        for (BillItem bi : items) {
            if (id.equals(bi.getSearialNo())) {
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
                ////// // System.out.println("value = " + value);
//                ////// // System.out.println("value.length() = " + value.length());
                return null;
            }
            BillItemController controller = (BillItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billItemController");
            if (controller == null) {
                ////// // System.out.println("null controller");
                return null;
            }
            if (getKey(value) == null) {
                ////// // System.out.println("value null");
                return null;
            }
            return controller.findBillItemInListBySerial(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key = null;
            try {
                key = Integer.valueOf(value);
            } catch (NumberFormatException e) {
            }

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
//                ////// // System.out.println("value = " + value);
//                ////// // System.out.println("value.length() = " + value.length());
//                return null;
//            }
//            BillItemController controller = (BillItemController) facesContext.getApplication().getELResolver().
//                    getValue(facesContext.getELContext(), null, "billItemController");
//            if (controller == null) {
//                ////// // System.out.println("null controller");
//                return null;
//            }
//            if (controller.getFacade() == null) {
//                ////// // System.out.println("facade null");
//                return null;
//            }
//            if (getKey(value) == null) {
//                ////// // System.out.println("value null");
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

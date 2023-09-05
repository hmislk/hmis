/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.facade.PharmaceuticalItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmaceuticalItemController implements Serializable {

    @EJB
    private PharmaceuticalItemFacade pharmaceuticalItemFacade;
    private PharmaceuticalItem pharmaceuticalItem;

    public List<PharmaceuticalItem> completeItem(String qry) {
        String jpql = "select c "
                + " from PharmaceuticalItem c "
                + " where c.retired=:ret "
                + " and c.name like :qry"
                + " order by c.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("qry", "%" + qry + "%");
        return pharmaceuticalItemFacade.findByJpql(jpql, m);
    }

    public PharmaceuticalItemFacade getPharmaceuticalItemFacade() {
        return pharmaceuticalItemFacade;
    }

    public void setPharmaceuticalItemFacade(PharmaceuticalItemFacade pharmaceuticalItemFacade) {
        this.pharmaceuticalItemFacade = pharmaceuticalItemFacade;
    }

    public PharmaceuticalItem getPharmaceuticalItem() {
        return pharmaceuticalItem;
    }

    public void setPharmaceuticalItem(PharmaceuticalItem pharmaceuticalItem) {
        this.pharmaceuticalItem = pharmaceuticalItem;
    }

    @EJB
    private PharmaceuticalItemFacade ejbFacade;

    public PharmaceuticalItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PharmaceuticalItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    @FacesConverter("phar")
    public static class PharmaceuticalItemConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PharmaceuticalItemController controller = (PharmaceuticalItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "pharmaceuticalItemController");
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
            if (object instanceof PharmaceuticalItem) {
                PharmaceuticalItem o = (PharmaceuticalItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PharmaceuticalItemController.class.getName());
            }
        }
    }
}

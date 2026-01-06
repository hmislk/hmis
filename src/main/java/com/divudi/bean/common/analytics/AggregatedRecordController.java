package com.divudi.bean.common.analytics;

import com.divudi.core.entity.analytics.AggregatedRecord;
import com.divudi.core.facade.AggregatedRecordFacade;
import com.divudi.service.AggregatedRecordService;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr M H B Ariyaratne buddhika.ari@gmail.com
 *
 */
@Named
@SessionScoped
public class AggregatedRecordController implements Serializable {

    private static final long serialVersionUID = 1L;

    // <editor-fold defaultstate="collapsed" desc="EJB">
    @EJB
    private AggregatedRecordFacade ejbFacade;
    AggregatedRecordService aggregatedRecordService;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private AggregatedRecord current;
    private List<AggregatedRecord> items;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public AggregatedRecordController() {
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public AggregatedRecord getCurrent() {
        if (current == null) {
            current = new AggregatedRecord();
        }
        return current;
    }

    public void setCurrent(AggregatedRecord current) {
        this.current = current;
    }

    public List<AggregatedRecord> getItems() {
        if (items == null) {
            items = getFacade().findAll();
        }
        return items;
    }

    public void setItems(List<AggregatedRecord> items) {
        this.items = items;
    }

    private AggregatedRecordFacade getFacade() {
        return ejbFacade;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Inner Classes">
    @FacesConverter(forClass = AggregatedRecord.class)
    public static class AggregatedRecordConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AggregatedRecordController controller = (AggregatedRecordController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "aggregatedRecordController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            return Long.valueOf(value);
        }

        String getStringKey(java.lang.Long value) {
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof AggregatedRecord) {
                AggregatedRecord o = (AggregatedRecord) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + AggregatedRecord.class.getName());
            }
        }
    }
    // </editor-fold>
}

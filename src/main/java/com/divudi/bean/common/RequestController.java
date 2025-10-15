package com.divudi.bean.common;

import com.divudi.core.entity.Request;
import com.divudi.core.facade.RequestFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class RequestController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private RequestFacade requestFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private static final long serialVersionUID = 1L;
    
    
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Function">
    
    
    
    public RequestController() {
    }

    @FacesConverter(forClass = Request.class)
    public static class AreaConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RequestController controller = (RequestController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "requestController");
            return controller.getRequestFacade().find(getKey(value));
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
            if (object instanceof Request) {
                Request o = (Request) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Request.class.getName());
            }
        }
    }

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public RequestFacade getRequestFacade() {
        return requestFacade;
    }

    // </editor-fold>
    
}

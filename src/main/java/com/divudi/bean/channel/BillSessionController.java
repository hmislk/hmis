/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.*;
import com.divudi.entity.BillSession;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.SessionInstanceFacade;
import java.io.Serializable;
import java.util.List;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillSessionController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private BillSessionFacade ejbFacade;

    List<BillSession> selectedItems;
    private BillSession current;
    private List<BillSession> items = null;

    public void save(BillSession pa) {
//        if (pa == null) {
//            return;
//        }
//        if (pa.getId() != null) {
//            getFacade().edit(pa);
//            JsfUtil.addSuccessMessage("Updated Successfully.");
//        } else {
//            pa.setCreatedAt(new Date());
//            pa.setCreater(getSessionController().getLoggedUser());
//            getFacade().create(pa);
//            JsfUtil.addSuccessMessage("Saved Successfully");
//        }
//        recreateModel();
//        getItems();
    }

    public BillSessionController() {
    }

    public BillSessionFacade getFacade() {
        return ejbFacade;
    }

    @FacesConverter(forClass = BillSession.class)
    public static class BillSessionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BillSessionController controller = (BillSessionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billSessionController");
            if (controller == null) {
                return null;
            }
            if (controller.getFacade() == null) {
                return null;
            }
            BillSession found = controller.getFacade().find(getKey(value));
            return found;
        }

        java.lang.Long getKey(String value) {
            try {
                java.lang.Long key;
                key = Long.valueOf(value);
                return key;
            } catch (NumberFormatException e) {
                return 0l;
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
            if (object instanceof BillSession) {
                BillSession o = (BillSession) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + BillSession.class.getName());
            }
        }
    }

}

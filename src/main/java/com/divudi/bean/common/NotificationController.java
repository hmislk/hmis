/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import static com.divudi.data.BillTypeAtomic.PHARMACY_TRANSFER_REQUEST;
import com.divudi.entity.Bill;
import com.divudi.entity.Notification;
import com.divudi.facade.NotificationFacade;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class NotificationController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    UserNotificationController userNotificationController;
    @EJB
    private NotificationFacade ejbFacade;
    private Notification current;
    private List<Notification> items = null;

    public void save(Notification notification) {
        if (notification == null) {
            return;
        }
        if (notification.getId() != null) {
            getFacade().edit(notification);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            notification.setCreatedAt(new Date());
            notification.setCreater(getSessionController().getLoggedUser());
            getFacade().create(notification);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void prepareAdd() {
        current = new Notification();
    }

    public void recreateModel() {
        items = null;
    }

    
    public void createNotification(Bill bill){
        if(bill==null){
            return;
        }
        BillTypeAtomic type = bill.getBillTypeAtomic();
        switch (type) {
            case PHARMACY_TRANSFER_REQUEST:
                createPharmacyTransferRequestNotification(bill);
                break;
            case PHARMACY_ORDER:
                createPharmacyReuestForBht(bill);
                break;
            default:
                throw new AssertionError();
        }
    }
    
    private void createPharmacyTransferRequestNotification(Bill bill){
        Date date=new Date();
        Notification nn = new Notification();
        nn.setCreatedAt(date);
        nn.setBill(bill);
        nn.setCreater(sessionController.getLoggedUser());
        nn.setMessage("New Request for Medicines from ");
        getFacade().create(nn);
    }
    
     private void createPharmacyReuestForBht(Bill bill){
        Notification nn = new Notification();
        nn.setBill(bill);
        nn.setCreater(sessionController.getLoggedUser());
        nn.setMessage("New Request for Medicines from ");
        getFacade().create(nn);
        userNotificationController.createUserNotifications(nn);
        
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

    public NotificationFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public NotificationController() {
    }

    public Notification getCurrent() {
        if (current == null) {
            current = new Notification();
        }
        return current;
    }

    public void setCurrent(Notification current) {
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

    private NotificationFacade getFacade() {
        return ejbFacade;
    }

    public List<Notification> getItems() {
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = Notification.class)
    public static class NotificationConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            NotificationController controller = (NotificationController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "notificationController");
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
            if (object instanceof Notification) {
                Notification o = (Notification) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Notification.class.getName());
            }
        }
    }

}

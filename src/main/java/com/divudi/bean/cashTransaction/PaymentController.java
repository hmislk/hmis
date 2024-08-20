/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.*;
import com.divudi.entity.Payment;
import com.divudi.facade.PaymentFacade;
import java.io.Serializable;
import java.util.Date;
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
public class PaymentController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PaymentFacade ejbFacade;
    private Payment current;
    private List<Payment> items = null;

    public void save(Payment payment) {
        if (payment == null) {
            return;
        }
        if (payment.getId() != null) {
            getFacade().edit(payment);
        } else {
            payment.setCreatedAt(new Date());
            payment.setCreater(getSessionController().getLoggedUser());
            getFacade().create(payment);
        }
    }

   
    public PaymentFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PaymentController() {
    }

    public Payment getCurrent() {
        if (current == null) {
            current = new Payment();
        }
        return current;
    }

    public void setCurrent(Payment current) {
        this.current = current;
    }



    private PaymentFacade getFacade() {
        return ejbFacade;
    }


    /**
     *
     */
    @FacesConverter(forClass = Payment.class)
    public static class PaymentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PaymentController controller = (PaymentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "paymentController");
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
            if (object instanceof Payment) {
                Payment o = (Payment) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PaymentController.class.getName());
            }
        }
    }

}

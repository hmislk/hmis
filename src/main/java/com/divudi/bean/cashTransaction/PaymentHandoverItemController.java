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
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillEjb;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Payment;
import com.divudi.entity.PaymentHandoverItem;
import com.divudi.entity.hr.BankAccount;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PaymentHandoverItemFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.service.PaymentService;
import java.io.Serializable;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PaymentHandoverItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;

    @EJB
    PaymentHandoverItemFacade paymentHandoverItemFacade;

    private PaymentHandoverItemFacade getEjbFacade() {
        return paymentHandoverItemFacade;
    }

    public void save(PaymentHandoverItem phi) {
        if (phi == null) {
            return;
        }
        if(phi.getId()==null){
            phi.setCreatedAt(new Date());
            phi.setCreater(sessionController.getLoggedUser());
            paymentHandoverItemFacade.create(phi);
        }else{
            paymentHandoverItemFacade.edit(phi);
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = PaymentHandoverItem.class)
    public static class PaymentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PaymentHandoverItemController controller = (PaymentHandoverItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "paymentHandoverItemController");
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
                        + object.getClass().getName() + "; expected type: " + PaymentHandoverItem.class.getName());
            }
        }
    }

}

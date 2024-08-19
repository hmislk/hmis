/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.entity.cashTransaction.CashBook;
import com.divudi.facade.CashBookFacade;
import java.io.Serializable;
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
 * @author Lawan Chaamindu
 */
@Named
@SessionScoped
public class CashBookController implements Serializable {

    @EJB
    private CashBookFacade CashbookFacade;
    @EJB
    private CashBook cashBook;
    
    @Inject
    private SessionController sessionController;

    
    public CashBookController() {
    }

    public CashBookFacade getCashbookFacade() {
        return CashbookFacade;
    }

    public void setCashbookFacade(CashBookFacade CashbookFacade) {
        this.CashbookFacade = CashbookFacade;
    }

    public CashBook getCashBook() {
        return cashBook;
    }

    public void setCashBook(CashBook cashBook) {
        this.cashBook = cashBook;
    }
    
    
    /**
     *
     */
    @FacesConverter(forClass = CashBook.class)
    public static class CashBookConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CashBookController controller = (CashBookController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "CashBookController");
            return controller.getCashbookFacade().find(getKey(value));
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
            if (object instanceof CashBook) {
                CashBook o = (CashBook) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + CashBook.class.getName());
            }
        }
    }

}

/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.cashTransaction.CashBook;
import com.divudi.core.facade.CashBookFacade;
import java.io.Serializable;
import java.util.HashMap;
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
 * @author Lawan Chaamindu
 */
@Named
@SessionScoped
public class CashBookController implements Serializable {

    @EJB
    private CashBookFacade cashbookFacade;

    @Inject
    private SessionController sessionController;


    private CashBook cashBook;

    @Deprecated //Use CashbookService
    public CashBook findAndSaveCashBookBySite(Institution site, Institution ins, Department dept) {
        if (site==null) {
            return null;
        }

        if (ins==null) {
            return null;
        }

        if (dept==null) {
            return null;
        }

        String sql;
        Map m = new HashMap();
        m.put("ins", ins);
        m.put("dept", dept);
        m.put("site", site);
        m.put("ret", false);
        sql = "select cb "
                + " from CashBook cb "
                + " where cb.institution=:ins"
                + " and cb.department=:dept"
                + " and cb.site=:site"
                + " and cb.retired=:ret";
        CashBook cb = cashbookFacade.findFirstByJpql(sql, m);

        if (cb == null) {
            cb = new CashBook();
            cb.setInstitution(ins);
            cb.setDepartment(dept);
            cb.setSite(site);
            cashbookFacade.create(cb);
        }
        return cb;
    }

    public CashBookController() {
    }

    public CashBookFacade getCashbookFacade() {
        return cashbookFacade;
    }

    public void setCashbookFacade(CashBookFacade CashbookFacade) {
        this.cashbookFacade = CashbookFacade;
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

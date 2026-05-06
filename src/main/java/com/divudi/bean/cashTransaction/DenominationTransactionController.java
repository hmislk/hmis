/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.cashTransaction.DenominationTransaction;
import com.divudi.core.facade.DenominationTransactionFacade;
import com.divudi.service.BillService;
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
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class DenominationTransactionController implements Serializable {

    @EJB
    private DenominationTransactionFacade denominationTransactionFacade;
    @EJB
    BillService billService;

    @Inject
    private SessionController sessionController;
    @Inject
    DenominationController denominationController;

    private DenominationTransaction selected;

    public DenominationTransactionController() {
    }

    public DenominationTransactionFacade getDenominationTransactionFacade() {
        return denominationTransactionFacade;
    }

    public void setDenominationTransactionFacade(DenominationTransactionFacade denominationTransactionFacade) {
        this.denominationTransactionFacade = denominationTransactionFacade;
    }

    public DenominationTransaction getSelected() {
        return selected;
    }

    public void setSelected(DenominationTransaction selected) {
        this.selected = selected;
    }

    public void save() {
        save(selected);
        JsfUtil.addSuccessMessage("Denomination transaction created successfully.");
    }

    public void save(DenominationTransaction transaction) {
        if (transaction == null) {
            return;
        }
        if (transaction.getId() == null) {
            transaction.setCreatedAt(new Date());
            transaction.setCreater(sessionController.getLoggedUser());
            denominationTransactionFacade.create(transaction);
        } else {
            denominationTransactionFacade.edit(transaction);
        }
    }

    public List<DenominationTransaction> createDefaultDenominationTransaction() {
        List<DenominationTransaction> dts = new ArrayList<>();
        List<com.divudi.core.entity.cashTransaction.Denomination> denominations = denominationController.getDenominations();
        for (com.divudi.core.entity.cashTransaction.Denomination d : denominations) {
            DenominationTransaction dt = new DenominationTransaction();
            dt.setDenomination(d);
            dt.setPaymentMethod(PaymentMethod.Cash);
            dts.add(dt);
        }
        return dts;
    }

//    @Deprecated // Directly User method with the same name in BillService
//    List<DenominationTransaction> fetchDenominationTransactionFromBill(Bill b) {
//        return billService.fetchDenominationTransactionFromBill(b);
//    }

    @FacesConverter(forClass = DenominationTransaction.class)
    public static class DenominationTransactionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DenominationTransactionController controller = (DenominationTransactionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "denominationTransactionController");
            return controller.getDenominationTransactionFacade().find(getKey(value));
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
            if (object instanceof DenominationTransaction) {
                DenominationTransaction o = (DenominationTransaction) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DenominationTransaction.class.getName());
            }
        }
    }

}

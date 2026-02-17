/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.cashTransaction.DetailedFinancialBill;
import com.divudi.core.facade.DetailedFinancialBillFacade;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for managing DetailedFinancialBill entities.
 * Handles creation, editing, and basic operations.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class DetailedFinancialBillController implements Serializable {

    @EJB
    private DetailedFinancialBillFacade detailedFinancialBillFacade;

    @Inject
    private SessionController sessionController;

    private DetailedFinancialBill selected;

    public DetailedFinancialBillController() {
    }

    public DetailedFinancialBillFacade getDetailedFinancialBillFacade() {
        return detailedFinancialBillFacade;
    }

    public void setDetailedFinancialBillFacade(DetailedFinancialBillFacade detailedFinancialBillFacade) {
        this.detailedFinancialBillFacade = detailedFinancialBillFacade;
    }

    public DetailedFinancialBill getSelected() {
        return selected;
    }

    public void setSelected(DetailedFinancialBill selected) {
        this.selected = selected;
    }

    /**
     * Save or update the current DetailedFinancialBill entity.
     */
    public void save() {
        save(selected);
        JsfUtil.addSuccessMessage("Financial bill details saved successfully.");
    }

    /**
     * Save or update a specific DetailedFinancialBill entity.
     *
     * @param financialBill The DetailedFinancialBill entity to save or update.
     */
    public void save(DetailedFinancialBill financialBill) {
        if (financialBill == null) {
            JsfUtil.addErrorMessage("No financial bill details to save.");
            return;
        }
        if (financialBill.getId() == null) {
            financialBill.setCreatedAt(new Date());
            financialBill.setCreater(sessionController.getLoggedUser());
            detailedFinancialBillFacade.create(financialBill);
            JsfUtil.addSuccessMessage("Financial bill created successfully.");
        } else {
            detailedFinancialBillFacade.edit(financialBill);
            JsfUtil.addSuccessMessage("Financial bill updated successfully.");
        }
    }

    @FacesConverter(forClass = DetailedFinancialBill.class)
    public static class DetailedFinancialBillConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DetailedFinancialBillController controller = (DetailedFinancialBillController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "detailedFinancialBillController");
            return controller.getDetailedFinancialBillFacade().find(getKey(value));
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
            if (object instanceof DetailedFinancialBill) {
                DetailedFinancialBill o = (DetailedFinancialBill) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DetailedFinancialBill.class.getName());
            }
        }
    }

}

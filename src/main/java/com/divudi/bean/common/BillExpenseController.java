/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.BillExpense;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
 * @author pdhs
 */
@Named
@SessionScoped
public class BillExpenseController implements Serializable {

    /**
     * Creates a new instance of BillExpenseController
     */
    public BillExpenseController() {
    }

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private BillExpenseFacade ejbFacade;
    List<BillExpense> selectedItems;
    private BillExpense current;
    private List<BillExpense> items = null;

    public String navigateToManageBillExpenses() {
        return "/admin/items/bill_expenses.xhtml?faces-redirect=true";
    }

    public void prepareAdd() {
        current = new BillExpense();
    }

    public void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getName() == null || current.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Name");
            return;
        }

        if (current.getPrintName() == null || current.getPrintName().trim().isEmpty()) {
            current.setPrintName(current.getName());
        }

        if (current.getFullName() == null || current.getFullName().trim().isEmpty()) {
            current.setFullName(current.getName());
        }

        if (current.getCode() == null || current.getCode().trim().isEmpty()) {
            String n = current.getName().replaceAll("\\s+", "").toUpperCase();
            n = n + "0000";
            current.setCode(n.substring(0, 4));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("cd", current.getCode().toUpperCase());
        String jpql = "select b from BillExpense b where b.retired=:ret and upper(b.code)=:cd";
        BillExpense dup = getFacade().findFirstByJpql(jpql, params);
        if (dup != null && (current.getId() == null || !dup.getId().equals(current.getId()))) {
            JsfUtil.addErrorMessage("Duplicate code");
            return;
        }

        if (getCurrent().getId() != null) {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getFacade().create(getCurrent());
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public BillExpenseFacade getEjbFacade() {
        return ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public BillExpense getCurrent() {
        if (current == null) {
            current = new BillExpense();
        }
        return current;
    }

    public void setCurrent(BillExpense current) {
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

    private BillExpenseFacade getFacade() {
        return ejbFacade;
    }

    public List<BillExpense> getItems() {
        if (items == null) {
            String sql = "select c from BillExpense c where c.retired=false order by c.name";
            items = getFacade().findByJpql(sql);
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = BillExpense.class)
    public static class BillExpenseControllerConverter implements Converter {

        public BillExpenseControllerConverter() {
        }

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BillExpenseController controller = (BillExpenseController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billExpenseController");
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
            if (object instanceof BillExpense) {
                BillExpense o = (BillExpense) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + BillExpense.class.getName());
            }
        }
    }

}

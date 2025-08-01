/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.hr.BankAccount;
import com.divudi.core.facade.BankAccountFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
public class BankAccountController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private BankAccountFacade ejbFacade;
    private BankAccount current;
    private List<BankAccount> items = null;

    public String manageBankAccounts() {
        current = new BankAccount();
        return "/admin/institutions/bank_account?faces-redirect=true";
    }

    public void save(BankAccount bankAccount) {
        if (bankAccount == null) {
            return;
        }
        if (bankAccount.getId() != null) {
            getFacade().edit(bankAccount);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            bankAccount.setCreatedAt(new Date());
            bankAccount.setCreater(getSessionController().getLoggedUser());
            getFacade().create(bankAccount);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public List<BankAccount> completeBankAccount(String qry) {
        List<BankAccount> list;
        String jpql;
        HashMap params = new HashMap();
        jpql = "select b from BankAccount b "
                + " where b.retired=false "
                + " and (b.accountNo like :q or b.accountName like :q ) "
                + " order by b.accountName";
        params.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(jpql, params);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new BankAccount();
    }

    public void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (getCurrent().getAccountNo().isEmpty() || getCurrent().getAccountNo() == null) {
            JsfUtil.addErrorMessage("Please enter Account Number");
            return;
        }

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

    public BankAccountFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(BankAccountFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BankAccountController() {
    }

    public BankAccount getCurrent() {
        if (current == null) {
            current = new BankAccount();
        }
        return current;
    }

    public void setCurrent(BankAccount current) {
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

    private BankAccountFacade getFacade() {
        return ejbFacade;
    }

    public List<BankAccount> getItems() {
        if (items == null || items.isEmpty()) {
            String j = "SELECT ba FROM BankAccount ba WHERE ba.retired = false ORDER BY ba.accountNo";
            items = getFacade().findByJpql(j);
            if (items == null) {
                items = new ArrayList<>();
            }
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = BankAccount.class)
    public static class BankAccountConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BankAccountController controller = (BankAccountController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "bankAccountController");
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
            if (object instanceof BankAccount) {
                BankAccount o = (BankAccount) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + BankAccount.class.getName());
            }
        }
    }

}

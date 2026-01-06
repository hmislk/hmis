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
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillEjb;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.hr.BankAccount;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
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
public class PaymentController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PaymentFacade ejbFacade;
    @EJB
    PaymentService paymentService;
    @EJB
    BillEjb billEjb;
    private Payment current;
    private boolean printPreview;
    private List<Payment> items = null;
    private List<Payment> itemsSelected = null;
    private Date fromDate;
    private Date toDate;
    private Double total;
    private BankAccount bankAccount;
    private Bill bill;

    public String navigateToPayCheques() {
        items = null;
        current = null;
        printPreview = false;
        return "/payments/cheque/list_cheques?faces-redirect=true";
    }

    public String navigateToMarkChequesAsCleared() {
        items = null;
        current = null;
        printPreview = false;
        return "/payments/cheque/mark_cheque?faces-redirect=true";
    }

    public void relazieCheques() {

        printPreview = true;
    }

    public String settlePayCheques() {
        if (items == null) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        if (items.isEmpty()) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        if (total == null) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        if (bankAccount == null) {
            JsfUtil.addErrorMessage("Select bank account");
            return null;
        }
        double absTotal = Math.abs(total);
        if (absTotal < 1) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        total = 0.0;
        for (Payment p : items) {
            total += p.getPaidValue();
            if (p.isRealized()) {
                total = 0.0;
                JsfUtil.addErrorMessage("You have selected some already realized cheques.");
                return null;
            }
            if (p.getPaymentMethod() != PaymentMethod.Cheque) {
                total = 0.0;
                JsfUtil.addErrorMessage("Only CHeques are managed here.");
                return null;
            }
        }
        bill = new Bill() ;
        bill.setCreatedAt(new Date());
        bill.setCreater(sessionController.getLoggedUser());
        bill.setDepartment(sessionController.getDepartment());
        bill.setInstitution(sessionController.getInstitution());
        bill.setBillType(BillType.CHEQUE_PAID);
        bill.setBillTypeAtomic(BillTypeAtomic.CHEQUE_PAID);
        bill.setBillClassType(BillClassType.BilledBill);
        bill.setTotal(total);
        bill.setNetTotal(total);
        bill.setGrantTotal(total);
        bill.setBillDate(new Date());
        bill.setBankAccount(bankAccount);
        bill.setBank(bankAccount.getBank());
        billEjb.save(bill, sessionController.getLoggedUser());
        for (Payment p : items) {
            p.setChequePaid(true);
            p.setChequePaidAt(new Date());
            p.setChequePaidBill(bill);
            p.setChequePayer(sessionController.getLoggedUser());
            save(p);
        }
        printPreview = true;
        return "/payments/cheque/pay_cheques?faces-redirect=true";
    }

    public String navigateToPaySelectedCheques() {
        if (itemsSelected == null) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        if (itemsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        total = 0.0;

        for (Payment p : itemsSelected) {
            total += p.getPaidValue();
            if (p.isRealized()) {
                total = 0.0;
                JsfUtil.addErrorMessage("You have selected some already realized cheques.");
                return null;
            }
            if (p.getPaymentMethod() != PaymentMethod.Cheque) {
                total = 0.0;
                JsfUtil.addErrorMessage("Only CHeques are managed here.");
                return null;
            }
        }
        items = itemsSelected;
        itemsSelected = null;
        printPreview = false;
        return "/payments/cheque/pay_cheques?faces-redirect=true";
    }

    public String navigateToMarkSelectedAsRealized() {
        if (itemsSelected == null) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        if (itemsSelected.isEmpty()) {
            JsfUtil.addErrorMessage("Select one or more cheques");
            return null;
        }
        total = 0.0;
        for (Payment p : itemsSelected) {
            total += p.getPaidValue();
            if (p.isRealized()) {
                total = 0.0;
                JsfUtil.addErrorMessage("You have selected some already realized cheques.");
                return null;
            }
            if (p.getPaymentMethod() != PaymentMethod.Cheque) {
                total = 0.0;
                JsfUtil.addErrorMessage("Only CHeques are managed here.");
                return null;
            }
        }
        items = itemsSelected;
        itemsSelected = null;
        printPreview = false;
        return "/payments/cheque/realize_cheques?faces-redirect=true";
    }

    public void listChequesToPay() {
        String jpql = "select p from Payment p"
                + " where p.retired = :retired"
                + " and p.paymentMethod = :paymentMethod"
                + " and p.createdAt between :fromDate and :toDate"
                + " and p.chequePaid = false";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("paymentMethod", PaymentMethod.Cheque);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        items = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void listChequesToMarkAsRealized() {
        String jpql = "select p from Payment p"
                + " where p.retired = :retired"
                + " and p.paymentMethod = :paymentMethod"
                + " and p.createdAt between :fromDate and :toDate"
                + " and p.chequeRealized = false";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("paymentMethod", PaymentMethod.Cheque);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        items = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void listAllCheques() {
        String jpql = "select p from Payment p"
                + " where p.retired = :retired"
                + " and p.createdAt between :fromDate and :toDate"
                + " and p.paymentMethod = :paymentMethod";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("paymentMethod", PaymentMethod.Cheque);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        items = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void listChequesRealized() {
        String jpql = "select p from Payment p"
                + " where p.retired = :retired"
                + " and p.createdAt between :fromDate and :toDate"
                + " and p.paymentMethod = :paymentMethod"
                + " and p.chequeRealized = true";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("paymentMethod", PaymentMethod.Cheque);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        items = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void save(Payment payment) {
        if (payment == null) {
            return;
        }
        if (payment.getId() == null) {
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(new Date());
            }
            if (payment.getCreater() == null) {
                payment.setCreater(getSessionController().getLoggedUser());
            }
            getFacade().create(payment);
        } else {
            getFacade().edit(payment);
        }
    }

    public void save(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return;
        }
        for (Payment payment : payments) {
            save(payment);
        }
    }

    public void addMissingPaymentDates() {
        String jpql = "select p "
                + " from Payment p "
                + " where p.paymentDate is null";
        List<Payment> ps = getFacade().findByJpql(jpql, 1000);
        if (ps == null) {
            JsfUtil.addErrorMessage("Nothing to add missing payments");
            return;
        }
        if (ps.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to add missing payments");
            return;
        }
        for (Payment p : ps) {
            if (p.getBill() != null) {
                if (p.getBill().getCreatedAt() != null) {
                    p.setPaymentDate(p.getBill().getCreatedAt());
                    getFacade().edit(p);
                } else {
                    p.setPaymentDate(new Date());
                    getFacade().edit(p);
                }
            } else {
                p.setPaymentDate(new Date());
                getFacade().edit(p);
            }
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

    public List<Payment> getItems() {
        return items;
    }

    public void setItems(List<Payment> items) {
        this.items = items;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<Payment> getItemsSelected() {
        return itemsSelected;
    }

    public void setItemsSelected(List<Payment> itemsSelected) {
        this.itemsSelected = itemsSelected;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
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

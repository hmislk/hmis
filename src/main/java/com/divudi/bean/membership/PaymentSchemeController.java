/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.membership;

import com.divudi.bean.common.SessionController;

import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.membership.AllowedPaymentMethod;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.AllowedPaymentMethodFacade;
import com.divudi.facade.PaymentSchemeFacade;
import com.divudi.bean.common.util.JsfUtil;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PaymentSchemeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PaymentSchemeFacade ejbFacade;
    @EJB
    AllowedPaymentMethodFacade allowedPaymentMethodFacade;
    MembershipScheme membershipScheme;
    AllowedPaymentMethod paymentSchemeAllowedPaymentMethod;
    List<PaymentScheme> selectedItems;
    private PaymentScheme paymentScheme;
    PaymentScheme paymentSchemeForAllowPayment;
    private List<PaymentScheme> items = null;
    List<AllowedPaymentMethod> allowedPaymentMethods;
    String selectText = "";

    public AllowedPaymentMethod getCurrentAllowedPaymentMethod() {
        if (paymentSchemeAllowedPaymentMethod == null) {
            paymentSchemeAllowedPaymentMethod = new AllowedPaymentMethod();
        }
        return paymentSchemeAllowedPaymentMethod;
    }

    public void setCurrentAllowedPaymentMethod(AllowedPaymentMethod paymentSchemeAllowedPaymentMethod) {
        this.paymentSchemeAllowedPaymentMethod = paymentSchemeAllowedPaymentMethod;
    }

    public PaymentScheme getPaymentSchemeForAllowPayment() {
        return paymentSchemeForAllowPayment;
    }

    public void setPaymentSchemeForAllowPayment(PaymentScheme paymentSchemeForAllowPayment) {
        this.paymentSchemeForAllowPayment = paymentSchemeForAllowPayment;
    }

    public AllowedPaymentMethodFacade getAllowedPaymentMethodFacade() {
        return allowedPaymentMethodFacade;
    }

    public void setAllowedPaymentMethodFacade(AllowedPaymentMethodFacade allowedPaymentMethodFacade) {
        this.allowedPaymentMethodFacade = allowedPaymentMethodFacade;
    }

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

    public boolean errorCheckPaymentMethod(PaymentMethod paymentMethod, PaymentMethodData paymentMethodData) {
        if (paymentMethod == PaymentMethod.Cheque) {
            if (paymentMethodData.getCheque().getInstitution() == null
                    || paymentMethodData.getCheque().getNo() == null
                    || paymentMethodData.getCheque().getDate() == null) {
                JsfUtil.addErrorMessage("Please select Cheque Number,Bank and Cheque Date");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.Slip) {
            if (paymentMethodData.getSlip().getInstitution() == null
                    || paymentMethodData.getSlip().getDate() == null) {
                JsfUtil.addErrorMessage("Please Fill Memo,Bank and Slip Date ");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.Card) {
            if (paymentMethodData.getCreditCard().getInstitution() == null
                    || paymentMethodData.getCreditCard().getNo() == null) {
                JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.ewallet) {
            if (paymentMethodData.getEwallet().getInstitution() == null
                    || paymentMethodData.getEwallet().getNo() == null) {
                JsfUtil.addErrorMessage("Please Fill eWallet Reference Number and Bank");
                return true;
            }
        }
        
        return false;
    }

    public boolean checkPaid(PaymentMethod paymentMethod, double paid, double amount) {

        if (paymentMethod == PaymentMethod.Cash) {
            if (paid == 0.0) {
                JsfUtil.addErrorMessage("Please select tendered amount correctly");
                return true;
            }
            if (paid < amount) {
                JsfUtil.addErrorMessage("Please select tendered amount correctly");
                return true;
            }
        }

        return false;
    }

    public void setItems(List<PaymentScheme> items) {
        this.items = items;
    }

    public List<PaymentScheme> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from PaymentScheme c"
                + " where c.retired=false "
                + " and c.membershipScheme is null "
                + " and (c.name) like '%" + getSelectText().toUpperCase() + "%' "
                + " order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        paymentScheme = new PaymentScheme();
        paymentSchemeAllowedPaymentMethod = new AllowedPaymentMethod();
        //membershipScheme = null;
    }

    public void setSelectedItems(List<PaymentScheme> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    public void recreateModel() {
        items = null;
        //   membershipScheme = null;
    }

    public void saveSelected() {

        //  getCurrent().setMembershipScheme(membershipScheme);
//        if (getCurrent().getPaymentMethod() == null) {
//            JsfUtil.addErrorMessage("Payment Method?");
//            return;
//        }
        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(paymentScheme);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            paymentScheme.setCreatedAt(new Date());
            paymentScheme.setCreater(getSessionController().getLoggedUser());
            getFacade().create(paymentScheme);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }

        paymentScheme = null;
        //  createPaymentSchemesMembership();
        //    recreateModel();

    }

    public void saveSelectedAllowedPaymentMethod() {
        
        if (getCurrentAllowedPaymentMethod().getPaymentMethod()==null) {
            JsfUtil.addErrorMessage("Please Select Payment Methord");
            return;
        }

        if (getCurrent() != null) {
            if (getCurrent().getId() != null) {
                getCurrentAllowedPaymentMethod().setPaymentScheme(getCurrent());
            }
        }
        if (getMembershipScheme() != null) {
            if (getMembershipScheme().getId() != null) {
                getCurrentAllowedPaymentMethod().setMembershipScheme(getMembershipScheme());
            }
        }
        

        if (getCurrentAllowedPaymentMethod().getId() != null && getCurrentAllowedPaymentMethod().getId() > 0) {
            getAllowedPaymentMethodFacade().edit(getCurrentAllowedPaymentMethod());
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getCurrentAllowedPaymentMethod().setCreatedAt(new Date());
            getCurrentAllowedPaymentMethod().setCreater(getSessionController().getLoggedUser());
            getAllowedPaymentMethodFacade().create(getCurrentAllowedPaymentMethod());
            JsfUtil.addSuccessMessage("Saved Successfully");
        }

        paymentSchemeAllowedPaymentMethod = null;
        createAllowedPaymentMethods();
        //  createPaymentSchemesMembership();
        //    recreateModel();

    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public PaymentSchemeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PaymentSchemeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PaymentSchemeController() {
    }

    public PaymentScheme getCurrent() {
        if (paymentScheme == null) {
            paymentScheme = new PaymentScheme();
        }
        return paymentScheme;
    }

    public void setCurrent(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public void delete() {

        if (paymentScheme != null) {
            paymentScheme.setRetired(true);
            paymentScheme.setRetiredAt(new Date());
            paymentScheme.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(paymentScheme);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        paymentScheme = null;
        getCurrent();
    }

    private PaymentSchemeFacade getFacade() {
        return ejbFacade;
    }

    public List<PaymentScheme> getItems() {
        if (items == null) {
            createPaymentSchemes();
        }
        return items;
    }

    public void createPaymentSchemes() {
        String temSql;
        temSql = "SELECT i FROM PaymentScheme i "
                + " where  i.retired=false "
                //+ " and i.membershipScheme is null "
                + " order by i.orderNo, i.name";
        items = getFacade().findByJpql(temSql);
    }

    public List<PaymentScheme> getPaymentSchemesForChannel() {
        return createPaymentSchemes(false, false, true);
    }

    public List<PaymentScheme> getPaymentSchemesForOPD() {
        return createPaymentSchemes(true, false, false);
    }

    public List<PaymentScheme> getPaymentSchemesForPharmacy() {
        return createPaymentSchemes(false, true, false);
    }

    public List<PaymentScheme> createPaymentSchemes(boolean opd, boolean pharmacy, boolean channel) {
        String temSql;
        temSql = "SELECT i FROM PaymentScheme i "
                + " where  i.retired=false ";

        if (pharmacy) {
            temSql += " and i.validForPharmacy=true ";
        }
        if (channel) {
            temSql += " and i.validForChanneling=true ";
        }
        if (opd) {
            temSql += " and i.validForBilledBills=true ";
        }

        temSql += " order by i.orderNo, i.name";

        return getFacade().findByJpql(temSql);
    }

    public List<PaymentScheme> completePaymentScheme(String qry) {
        List<PaymentScheme> c;
        HashMap hm = new HashMap();
        String sql = "select c from PaymentScheme c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        c = getFacade().findByJpql(sql, hm);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<PaymentScheme> completePaymentSchemeChannel(String qry) {
        List<PaymentScheme> c;
        HashMap hm = new HashMap();
        String sql = "select c from PaymentScheme c "
                + " where c.retired=false "
                + " and c.validForChanneling=true "
                + " and (c.name) like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        c = getFacade().findByJpql(sql, hm);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public void createAllowedPaymentMethods() {
        String temSql;
        temSql = "SELECT i FROM AllowedPaymentMethod i "
                + " where  i.retired=false "
                + " and (i.membershipScheme=:mem "
                + " or i.paymentScheme=:pay )"
                + " order by i.paymentMethod";
        HashMap hm = new HashMap();
        hm.put("mem", membershipScheme);
        hm.put("pay", paymentScheme);

        allowedPaymentMethods = getAllowedPaymentMethodFacade().findByJpql(temSql, hm);
    }

    public List<AllowedPaymentMethod> getAllowedPaymentMethods() {
        return allowedPaymentMethods;
    }

    public void setAllowedPaymentMethods(List<AllowedPaymentMethod> allowedPaymentMethods) {
        this.allowedPaymentMethods = allowedPaymentMethods;
    }

    @FacesConverter(forClass = PaymentScheme.class)
    public static class PaymentSchemeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PaymentSchemeController controller = (PaymentSchemeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "paymentSchemeController");
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
            if (object instanceof PaymentScheme) {
                PaymentScheme o = (PaymentScheme) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PaymentSchemeController.class.getName());
            }
        }
    }
}

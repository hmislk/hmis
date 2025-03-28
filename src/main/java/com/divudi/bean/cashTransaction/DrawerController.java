/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 91 2241603
 *
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.entity.cashTransaction.DrawerEntry;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.service.DrawerService;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Consultant
 * in Health Informatics buddhika.ari@gmail.com (+94) 71 5812399 (+94) 71
 * 1569020
 *
 */
@Named
@SessionScoped
public class DrawerController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private DrawerFacade ejbFacade;
    @EJB
    DrawerService drawerService;

    @Inject
    SessionController sessionController;
    @Inject
    DrawerEntryController drawerEntryController;

    DrawerEntry drawerEntry;
    private Drawer current;
    private List<Drawer> items = null;
    List<Drawer> drawers;

    private boolean editDrawerAccess;

    // <editor-fold defaultstate="collapsed" desc="UP">
    public void updateDrawerForIns(List<Payment> payments, WebUser webUser) {
        if (payments == null) {
            return;
        }
        for (Payment payment : payments) {
            updateDrawerForIns(payment, webUser);
        }
    }

    public void updateDrawerForIns(Payment payment, WebUser webUser) {
        updateDrawer(payment, Math.abs(payment.getPaidValue()), webUser);
    }

    public void updateDrawer(List<Payment> payments, WebUser webUser) {
        if (payments == null || payments.isEmpty()) {
            return;
        }
        for (Payment payment : payments) {
            updateDrawer(payment, payment.getPaidValue(), webUser);
        }
    }

    public void updateDrawer(Payment payment, double paidValue, WebUser webUser) {
        System.out.println("paidValue = " + paidValue);
        System.out.println("payment = " + payment);
        if (payment == null || webUser == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }

        Drawer drawer = getUsersDrawer(webUser);
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }

        //update Drover History
        drawerEntryUpdate(payment, drawer, webUser);

        synchronized (drawer) {
            switch (payment.getPaymentMethod()) {
                case OnCall:
                    drawer.setOnCallInHandValue(safeAdd(drawer.getOnCallInHandValue(), paidValue));
                    drawer.setOnCallBalance(safeAdd(drawer.getOnCallBalance(), paidValue));
                    break;
                case Cash:
                    drawer.setCashInHandValue(safeAdd(drawer.getCashInHandValue(), paidValue));
                    drawer.setCashBalance(safeAdd(drawer.getCashBalance(), paidValue));
                    break;
                case Card:
                    drawer.setCardInHandValue(safeAdd(drawer.getCardInHandValue(), paidValue));
                    drawer.setCardBalance(safeAdd(drawer.getCardBalance(), paidValue));
                    break;
                case MultiplePaymentMethods:
                    drawer.setMultiplePaymentMethodsInHandValue(safeAdd(drawer.getMultiplePaymentMethodsInHandValue(), paidValue));
                    drawer.setMultiplePaymentMethodsBalance(safeAdd(drawer.getMultiplePaymentMethodsBalance(), paidValue));
                    break;
                case Staff:
                    drawer.setStaffInHandValue(safeAdd(drawer.getStaffInHandValue(), paidValue));
                    drawer.setStaffBalance(safeAdd(drawer.getStaffBalance(), paidValue));
                    break;
                case Credit:
                    drawer.setCreditInHandValue(safeAdd(drawer.getCreditInHandValue(), paidValue));
                    drawer.setCreditBalance(safeAdd(drawer.getCreditBalance(), paidValue));
                    break;
                case Staff_Welfare:
                    drawer.setStaffWelfareInHandValue(safeAdd(drawer.getStaffWelfareInHandValue(), paidValue));
                    drawer.setStaffWelfareBalance(safeAdd(drawer.getStaffWelfareBalance(), paidValue));
                    break;
                case Voucher:
                    drawer.setVoucherInHandValue(safeAdd(drawer.getVoucherInHandValue(), paidValue));
                    drawer.setVoucherBalance(safeAdd(drawer.getVoucherBalance(), paidValue));
                    break;
                case IOU:
                    drawer.setIouInHandValue(safeAdd(drawer.getIouInHandValue(), paidValue));
                    drawer.setIouBalance(safeAdd(drawer.getIouBalance(), paidValue));
                    break;
                case Agent:
                    drawer.setAgentInHandValue(safeAdd(drawer.getAgentInHandValue(), paidValue));
                    drawer.setAgentBalance(safeAdd(drawer.getAgentBalance(), paidValue));
                    break;
                case Cheque:
                    drawer.setChequeInHandValue(safeAdd(drawer.getChequeInHandValue(), paidValue));
                    drawer.setChequeBalance(safeAdd(drawer.getChequeBalance(), paidValue));
                    break;
                case Slip:
                    drawer.setSlipInHandValue(safeAdd(drawer.getSlipInHandValue(), paidValue));
                    drawer.setSlipBalance(safeAdd(drawer.getSlipBalance(), paidValue));
                    break;
                case ewallet:
                    drawer.setEwalletInHandValue(safeAdd(drawer.getEwalletInHandValue(), paidValue));
                    drawer.setEwalletBalance(safeAdd(drawer.getEwalletBalance(), paidValue));
                    break;
                case PatientDeposit:
                    drawer.setPatientDepositInHandValue(safeAdd(drawer.getPatientDepositInHandValue(), paidValue));
                    drawer.setPatientDepositBalance(safeAdd(drawer.getPatientDepositBalance(), paidValue));
                    break;
                case PatientPoints:
                    drawer.setPatientPointsInHandValue(safeAdd(drawer.getPatientPointsInHandValue(), paidValue));
                    drawer.setPatientPointsBalance(safeAdd(drawer.getPatientPointsBalance(), paidValue));
                    break;
                case OnlineSettlement:
                    drawer.setOnlineSettlementInHandValue(safeAdd(drawer.getOnlineSettlementInHandValue(), paidValue));
                    drawer.setOnlineSettlementBalance(safeAdd(drawer.getOnlineSettlementBalance(), paidValue));
                    break;
                case None:
                    drawer.setNoneInHandValue(safeAdd(drawer.getNoneInHandValue(), paidValue));
                    drawer.setNoneBalance(safeAdd(drawer.getNoneBalance(), paidValue));
                    break;
                case YouOweMe:
                    drawer.setYouOweMeInHandValue(safeAdd(drawer.getYouOweMeInHandValue(), paidValue));
                    drawer.setYouOweMeBalance(safeAdd(drawer.getYouOweMeBalance(), paidValue));
                    break;
                default:
                    System.err.println("Unhandled payment method: " + payment.getPaymentMethod());
                    break;
            }

            ejbFacade.editAndCommit(drawer);
        }
    }

    public void updateDrawer(Bill bill, double paidValue, PaymentMethod paymentMethod, WebUser webUser) {
        System.out.println("paidValue = " + paidValue);
        System.out.println("bill = " + bill);
        System.out.println("webUser = " + webUser);
        if (bill == null || webUser == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }

        Drawer drawer = getUsersDrawer(webUser);
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }

        //update Drover History
        drawerEntryUpdate(bill, drawer, paymentMethod, webUser, paidValue);

        synchronized (drawer) {
            switch (paymentMethod) {
                case OnCall:
                    drawer.setOnCallInHandValue(safeAdd(drawer.getOnCallInHandValue(), paidValue));
                    drawer.setOnCallBalance(safeAdd(drawer.getOnCallBalance(), paidValue));
                    break;
                case Cash:
                    drawer.setCashInHandValue(safeAdd(drawer.getCashInHandValue(), paidValue));
                    drawer.setCashBalance(safeAdd(drawer.getCashBalance(), paidValue));
                    break;
                case Card:
                    drawer.setCardInHandValue(safeAdd(drawer.getCardInHandValue(), paidValue));
                    drawer.setCardBalance(safeAdd(drawer.getCardBalance(), paidValue));
                    break;
                case MultiplePaymentMethods:
                    drawer.setMultiplePaymentMethodsInHandValue(safeAdd(drawer.getMultiplePaymentMethodsInHandValue(), paidValue));
                    drawer.setMultiplePaymentMethodsBalance(safeAdd(drawer.getMultiplePaymentMethodsBalance(), paidValue));
                    break;
                case Staff:
                    drawer.setStaffInHandValue(safeAdd(drawer.getStaffInHandValue(), paidValue));
                    drawer.setStaffBalance(safeAdd(drawer.getStaffBalance(), paidValue));
                    break;
                case Credit:
                    drawer.setCreditInHandValue(safeAdd(drawer.getCreditInHandValue(), paidValue));
                    drawer.setCreditBalance(safeAdd(drawer.getCreditBalance(), paidValue));
                    break;
                case Staff_Welfare:
                    drawer.setStaffWelfareInHandValue(safeAdd(drawer.getStaffWelfareInHandValue(), paidValue));
                    drawer.setStaffWelfareBalance(safeAdd(drawer.getStaffWelfareBalance(), paidValue));
                    break;
                case Voucher:
                    drawer.setVoucherInHandValue(safeAdd(drawer.getVoucherInHandValue(), paidValue));
                    drawer.setVoucherBalance(safeAdd(drawer.getVoucherBalance(), paidValue));
                    break;
                case IOU:
                    drawer.setIouInHandValue(safeAdd(drawer.getIouInHandValue(), paidValue));
                    drawer.setIouBalance(safeAdd(drawer.getIouBalance(), paidValue));
                    break;
                case Agent:
                    drawer.setAgentInHandValue(safeAdd(drawer.getAgentInHandValue(), paidValue));
                    drawer.setAgentBalance(safeAdd(drawer.getAgentBalance(), paidValue));
                    break;
                case Cheque:
                    drawer.setChequeInHandValue(safeAdd(drawer.getChequeInHandValue(), paidValue));
                    drawer.setChequeBalance(safeAdd(drawer.getChequeBalance(), paidValue));
                    break;
                case Slip:
                    drawer.setSlipInHandValue(safeAdd(drawer.getSlipInHandValue(), paidValue));
                    drawer.setSlipBalance(safeAdd(drawer.getSlipBalance(), paidValue));
                    break;
                case ewallet:
                    drawer.setEwalletInHandValue(safeAdd(drawer.getEwalletInHandValue(), paidValue));
                    drawer.setEwalletBalance(safeAdd(drawer.getEwalletBalance(), paidValue));
                    break;
                case PatientDeposit:
                    drawer.setPatientDepositInHandValue(safeAdd(drawer.getPatientDepositInHandValue(), paidValue));
                    drawer.setPatientDepositBalance(safeAdd(drawer.getPatientDepositBalance(), paidValue));
                    break;
                case PatientPoints:
                    drawer.setPatientPointsInHandValue(safeAdd(drawer.getPatientPointsInHandValue(), paidValue));
                    drawer.setPatientPointsBalance(safeAdd(drawer.getPatientPointsBalance(), paidValue));
                    break;
                case OnlineSettlement:
                    drawer.setOnlineSettlementInHandValue(safeAdd(drawer.getOnlineSettlementInHandValue(), paidValue));
                    drawer.setOnlineSettlementBalance(safeAdd(drawer.getOnlineSettlementBalance(), paidValue));
                    break;
                case None:
                    drawer.setNoneInHandValue(safeAdd(drawer.getNoneInHandValue(), paidValue));
                    drawer.setNoneBalance(safeAdd(drawer.getNoneBalance(), paidValue));
                    break;
                case YouOweMe:
                    drawer.setYouOweMeInHandValue(safeAdd(drawer.getYouOweMeInHandValue(), paidValue));
                    drawer.setYouOweMeBalance(safeAdd(drawer.getYouOweMeBalance(), paidValue));
                    break;
                default:
                    break;
            }

            ejbFacade.editAndCommit(drawer);
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Down">
    public void updateDrawerForOuts(List<Payment> payments, WebUser webUser) {
        for (Payment payment : payments) {
            updateDrawerForOuts(payment, webUser);
        }
    }

    public void updateDrawerForOuts(Payment payment, WebUser webUser) {
        updateDrawer(payment, -Math.abs(payment.getPaidValue()), webUser);
    }

    public void updateDrawerForOuts(Bill bill, PaymentMethod paymentMethod, double value, WebUser webUser) {
        updateDrawer(bill, -Math.abs(value), paymentMethod, webUser);
    }

    public void updateDrawerForIns(Bill bill, PaymentMethod paymentMethod, double value, WebUser webUser) {
        updateDrawer(bill, Math.abs(value), paymentMethod, webUser);
    }

    // </editor-fold>
    public void updateDrawerForIns(List<Payment> payments) {
        if (payments == null) {
            return;
        }
        for (Payment payment : payments) {
            drawerService.updateDrawerForIns(payment);
        }
        System.out.println("Draver & Draver Entry Updated...");
    }

    public void drawerEntryUpdate(Payment payment, Drawer currentDrawer) {
        drawerService.drawerEntryUpdate(payment, currentDrawer);
    }

    public void drawerEntryUpdate(Payment payment, Drawer currentDrawer, WebUser user) {
        drawerService.drawerEntryUpdate(payment, currentDrawer, user);
    }

    public void drawerEntryUpdate(Bill bill, Drawer currentDrawer, PaymentMethod paymentMethod, WebUser user, Double value) {
        drawerService.drawerEntryUpdate(bill, currentDrawer, paymentMethod, user, value);
    }

    public void updateDrawerForOuts(List<Payment> payments) {
        drawerService.updateDrawerForOuts(payments);
    }

    public void updateDrawerForIns(Payment payment) {
        drawerService.updateDrawer(payment, Math.abs(payment.getPaidValue()));
    }

    public void updateDrawerForOuts(Payment payment) {
        drawerService.updateDrawer(payment, -Math.abs(payment.getPaidValue()));
    }

    public void updateDrawer(Payment payment, double paidValue) {
        drawerService.updateDrawer(payment, paidValue);
    }

    public double safeAdd(Double currentValue, double addValue) {
        if (currentValue == null) {
            return addValue;
        }
        return currentValue + addValue;
    }

    public void createDrawers() {
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Drawer c "
                + " where c.retired=false "
                + " order by c.name";

        drawers = getFacade().findByJpql(sql, hm);
    }

    public Drawer getUsersDrawer(WebUser webUser) {
        String jpql;
        HashMap m = new HashMap();
        jpql = "select d from Drawer d "
                + " where d.retired=false "
                + " and d.drawerUser=:user";

        m.put("user", webUser);

        Drawer drawer;
        drawer = getFacade().findFirstByJpql(jpql, m);

        if (drawer == null) {
            drawer = new Drawer();
            drawer.setDrawerUser(webUser);
            save(drawer);
        }
        return drawer;
    }

    public List<Drawer> getDrawers() {
        return drawers;
    }

    public void setDrawers(List<Drawer> drawers) {
        this.drawers = drawers;
    }

    public List<Drawer> completeDrawer(String qry) {
        List<Drawer> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Drawer c "
                + " where c.retired=false "
                + " and c.name like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new Drawer();
    }

    private void recreateModel() {
        items = null;
    }

    public void save(Drawer drawer) {
        if (drawer.getId() != null && drawer.getId() > 0) {
            getFacade().edit(drawer);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            drawer.setCreatedAt(new Date());
            drawer.setCreater(getSessionController().getLoggedUser());
            getFacade().create(drawer);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void saveSelected() {
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

    public DrawerFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(DrawerFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DrawerController() {
    }

    public Drawer getCurrent() {
        if (current == null) {
            current = new Drawer();
        }
        return current;
    }

    public void setCurrent(Drawer current) {
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

    private DrawerFacade getFacade() {
        return ejbFacade;
    }

    public List<Drawer> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public boolean isEditDrawerAccess() {
        return editDrawerAccess;
    }

    public void setEditDrawerAccess(boolean editDrawerAccess) {
        this.editDrawerAccess = editDrawerAccess;
    }

    /**
     *
     */
    @FacesConverter(forClass = Drawer.class)
    public static class DrawerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DrawerController controller = (DrawerController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "drawerController");
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
            if (object instanceof Drawer) {
                Drawer o = (Drawer) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + Drawer.class.getName());
            }
        }
    }

}

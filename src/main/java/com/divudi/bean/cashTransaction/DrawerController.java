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
import com.divudi.service.DrawerBusyException;
import com.divudi.service.DrawerService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
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
    private static final Logger LOGGER = Logger.getLogger(DrawerController.class.getName());
    static final String DRAWER_NOT_UPDATED_MESSAGE
            = "Bill saved, but the cash drawer could not be updated. Please tell the administrator.";

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
        for (int i = 0; i < payments.size(); i++) {
            Payment payment = payments.get(i);
            if (!updateDrawerOrReport(payment, Math.abs(payment.getPaidValue()), webUser)) {
                reportSkipped(payments.subList(i + 1, payments.size()), webUser, Math::abs);
                return;
            }
        }
    }

    public void updateDrawerForIns(Payment payment, WebUser webUser) {
        updateDrawer(payment, Math.abs(payment.getPaidValue()), webUser);
    }

    public void updateDrawer(List<Payment> payments, WebUser webUser) {
        if (payments == null || payments.isEmpty()) {
            return;
        }
        for (int i = 0; i < payments.size(); i++) {
            Payment payment = payments.get(i);
            if (!updateDrawerOrReport(payment, payment.getPaidValue(), webUser)) {
                reportSkipped(payments.subList(i + 1, payments.size()), webUser, v -> v);
                return;
            }
        }
    }

    public void updateDrawer(Payment payment, double paidValue, WebUser webUser) {
        updateDrawerOrReport(payment, paidValue, webUser);
    }

    public void updateDrawer(Bill bill, double paidValue, PaymentMethod paymentMethod, WebUser webUser) {
        try {
            drawerService.updateDrawer(bill, paidValue, paymentMethod, webUser);
        } catch (DrawerBusyException e) {
            LOGGER.log(Level.SEVERE, "Cash drawer NOT updated (drawer busy): bill id {0}, amount {1}, method {2}, drawer user {3}. Apply it with a drawer adjustment.",
                    new Object[]{String.valueOf(bill == null ? null : bill.getId()), String.valueOf(paidValue), String.valueOf(paymentMethod), userName(webUser)});
            tellCashier();
        }
    }

    /**
     * Applies one payment to the user's drawer. Read, history entry and balance
     * change run in one DrawerService transaction under a row lock (#25123). The
     * caller has already committed the bill and payment, so a busy drawer
     * (DrawerBusyException) must not undo them (#25230): it is logged with what an
     * admin needs to apply it by a drawer adjustment, and the cashier is told.
     *
     * @return false when the drawer was busy and nothing was applied
     */
    private boolean updateDrawerOrReport(Payment payment, double paidValue, WebUser webUser) {
        try {
            drawerService.updateUsersDrawer(payment, paidValue, webUser);
            return true;
        } catch (DrawerBusyException e) {
            logNotApplied(payment, paidValue, webUser);
            tellCashier();
            return false;
        }
    }

    /**
     * After one busy drawer, the bill's remaining payments are not tried: the row
     * is still locked, and each try would wait out the database lock timeout again.
     * They are logged so an admin can apply them, with the same sign the loop
     * would have applied.
     *
     * @param webUser the drawer user, or null for each payment's creator
     * @param signed maps a payment's paid value to the amount the loop applies
     */
    private void reportSkipped(List<Payment> payments, WebUser webUser, DoubleUnaryOperator signed) {
        for (Payment payment : payments) {
            if (payment == null) {
                continue;
            }
            logNotApplied(payment, signed.applyAsDouble(payment.getPaidValue()),
                    webUser != null ? webUser : payment.getCreater());
        }
    }

    private void logNotApplied(Payment payment, double value, WebUser drawerUser) {
        // Plain string values: Logger's MessageFormat would group ids (1,234,567)
        // and round amounts, and admins search and apply these values as logged.
        LOGGER.log(Level.SEVERE, "Cash drawer NOT updated (drawer busy): payment id {0}, bill id {1}, amount {2}, method {3}, drawer user {4}. Apply it with a drawer adjustment.",
                new Object[]{
                    String.valueOf(payment == null ? null : payment.getId()),
                    String.valueOf(payment == null || payment.getBill() == null ? null : payment.getBill().getId()),
                    String.valueOf(value),
                    String.valueOf(payment == null ? null : payment.getPaymentMethod()),
                    userName(drawerUser)});
    }

    /**
     * Tells the cashier once per request. Kept across a faces-redirect, since many
     * settle actions redirect to the print page. No-op without a FacesContext.
     * Package-private so unit tests can replace it.
     */
    void tellCashier() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        for (FacesMessage m : context.getMessageList()) {
            if (DRAWER_NOT_UPDATED_MESSAGE.equals(m.getSummary())) {
                return;
            }
        }
        JsfUtil.addErrorMessageForRedirect(DRAWER_NOT_UPDATED_MESSAGE);
    }

    private static String userName(WebUser user) {
        if (user == null) {
            return null;
        }
        return user.getName() + " (id " + user.getId() + ")";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Down">
    public void updateDrawerForOuts(List<Payment> payments, WebUser webUser) {
        for (int i = 0; i < payments.size(); i++) {
            Payment payment = payments.get(i);
            if (!updateDrawerOrReport(payment, -Math.abs(payment.getPaidValue()), webUser)) {
                reportSkipped(payments.subList(i + 1, payments.size()), webUser, v -> -Math.abs(v));
                return;
            }
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
        for (int i = 0; i < payments.size(); i++) {
            Payment payment = payments.get(i);
            if (!updateCreatorsDrawerOrReport(payment, Math.abs(payment.getPaidValue()))) {
                reportSkipped(payments.subList(i + 1, payments.size()), null, Math::abs);
                return;
            }
        }
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
        try {
            // One DrawerService transaction for the whole list: busy means none applied.
            drawerService.updateDrawerForOuts(payments);
        } catch (DrawerBusyException e) {
            for (Payment payment : payments) {
                logNotApplied(payment, payment == null ? 0 : -Math.abs(payment.getPaidValue()), payment == null ? null : payment.getCreater());
            }
            tellCashier();
        }
    }

    public void updateDrawerForIns(Payment payment) {
        updateCreatorsDrawerOrReport(payment, Math.abs(payment.getPaidValue()));
    }

    public void updateDrawerForOuts(Payment payment) {
        updateCreatorsDrawerOrReport(payment, -Math.abs(payment.getPaidValue()));
    }

    public void updateDrawer(Payment payment, double paidValue) {
        updateCreatorsDrawerOrReport(payment, paidValue);
    }

    /** As updateDrawerOrReport, for the drawer of the payment's creator. */
    private boolean updateCreatorsDrawerOrReport(Payment payment, double paidValue) {
        try {
            drawerService.updateDrawer(payment, paidValue);
            return true;
        } catch (DrawerBusyException e) {
            logNotApplied(payment, paidValue, payment == null ? null : payment.getCreater());
            tellCashier();
            return false;
        }
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
            JsfUtil.addErrorMessage("Nothing to Delete");
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

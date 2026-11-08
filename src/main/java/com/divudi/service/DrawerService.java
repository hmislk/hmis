package com.divudi.service;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.entity.cashTransaction.DrawerEntry;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.DrawerEntryFacade;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.ejb.BillNumberGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class DrawerService {

    @EJB
    DrawerEntryFacade ejbFacade;
    @EJB
    DrawerFacade drawerFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    DrawerEntry drawerEntry;

    /**
     * The PaymentMethod values actually handled by applyDrawerAdjustment/drawerEntryUpdate's
     * switch statements. OnlineBookingAgent is intentionally excluded — it is not handled
     * anywhere in Drawer/DrawerService.
     */
    private static final List<PaymentMethod> ADJUSTABLE_PAYMENT_METHODS = Collections.unmodifiableList(Arrays.asList(
            PaymentMethod.OnCall,
            PaymentMethod.Cash,
            PaymentMethod.Card,
            PaymentMethod.MultiplePaymentMethods,
            PaymentMethod.Staff,
            PaymentMethod.Credit,
            PaymentMethod.Staff_Welfare,
            PaymentMethod.Voucher,
            PaymentMethod.IOU,
            PaymentMethod.Agent,
            PaymentMethod.Cheque,
            PaymentMethod.Slip,
            PaymentMethod.ewallet,
            PaymentMethod.PatientDeposit,
            PaymentMethod.PatientPoints,
            PaymentMethod.OnlineSettlement,
            PaymentMethod.None,
            PaymentMethod.YouOweMe
    ));

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

    public void updateDrawer(Payment payment, double paidValue, WebUser webUser) {
        if (payment == null || payment.getCreater() == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }

        Drawer drawer = lockUsersDrawer(webUser);
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }

        //update Drover History with the signed value applied to the balance below
        drawerEntryUpdate(payment, drawer, payment.getCreater(), paidValue);

        addToBalance(drawer, payment.getPaymentMethod(), paidValue);
        drawerFacade.editAndCommit(drawer);
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

    // </editor-fold>
    public Drawer reloadDrawer(Drawer drawer) {
        if (drawer == null) {
            return null;
        }
        return drawerFacade.find(drawer.getId());
    }

    public void updateDrawerForIns(List<Payment> payments) {
        if (payments == null) {
            return;
        }
        for (Payment payment : payments) {
            updateDrawerForIns(payment);
        }
        System.out.println("Draver & Draver Entry Updated...");
    }

    public void drawerEntryUpdate(Payment payment, Drawer currentDrawer) {
        drawerEntryUpdate(payment, currentDrawer, payment.getCreater());
    }

    public void drawerEntryUpdate(Payment payment, Drawer currentDrawer, WebUser user) {
        if (payment == null) {
            return;
        }
        drawerEntryUpdate(payment, currentDrawer, user, payment.getPaidValue());
    }

    /**
     * Writes the drawer history row for a payment, using the signed value that
     * is applied to the drawer balance. A drawer "out" made with a positive
     * payment passes a negative value here, so the row's transaction value and
     * after-in-hand value agree with the balance change (#25111).
     *
     * @param payment the payment being recorded
     * @param currentDrawer the drawer whose balance changes
     * @param user the user the history row is filed under
     * @param signedValue the value added to the drawer balance (negative for an out)
     */
    public void drawerEntryUpdate(Payment payment, Drawer currentDrawer, WebUser user, double signedValue) {
        if (payment == null) {
            return;
        }

        drawerEntry = new DrawerEntry();
        drawerEntry.setPayment(payment);
        drawerEntry.setPaymentMethod(payment.getPaymentMethod());
        drawerEntry.setBill(payment.getBill());
        drawerEntry.setDrawer(currentDrawer);
        drawerEntry.setWebUser(user);
        drawerEntry.setTransactionValue(signedValue);
        Double beforeInHandValue = 0.0;

        if (payment.getPaymentMethod() != null) {
            switch (payment.getPaymentMethod()) {
                case Cash:
                    beforeInHandValue = currentDrawer.getCashInHandValue() != null ? currentDrawer.getCashInHandValue() : 0.0;
                    break;
                case Card:
                    beforeInHandValue = currentDrawer.getCardInHandValue() != null ? currentDrawer.getCardInHandValue() : 0.0;
                    break;
                case OnCall:
                    beforeInHandValue = currentDrawer.getOnCallInHandValue() != null ? currentDrawer.getOnCallInHandValue() : 0.0;
                    break;
                case MultiplePaymentMethods:
                    beforeInHandValue = currentDrawer.getMultiplePaymentMethodsInHandValue() != null ? currentDrawer.getMultiplePaymentMethodsInHandValue() : 0.0;
                    break;
                case Staff:
                    beforeInHandValue = currentDrawer.getStaffInHandValue() != null ? currentDrawer.getStaffInHandValue() : 0.0;
                    break;
                case Credit:
                    beforeInHandValue = currentDrawer.getCreditInHandValue() != null ? currentDrawer.getCreditInHandValue() : 0.0;
                    break;
                case Staff_Welfare:
                    beforeInHandValue = currentDrawer.getStaffWelfareInHandValue() != null ? currentDrawer.getStaffWelfareInHandValue() : 0.0;
                    break;
                case Voucher:
                    beforeInHandValue = currentDrawer.getVoucherInHandValue() != null ? currentDrawer.getVoucherInHandValue() : 0.0;
                    break;
                case IOU:
                    beforeInHandValue = currentDrawer.getIouInHandValue() != null ? currentDrawer.getIouInHandValue() : 0.0;
                    break;
                case Agent:
                    beforeInHandValue = currentDrawer.getAgentInHandValue() != null ? currentDrawer.getAgentInHandValue() : 0.0;
                    break;
                case Cheque:
                    beforeInHandValue = currentDrawer.getChequeInHandValue() != null ? currentDrawer.getChequeInHandValue() : 0.0;
                    break;
                case Slip:
                    beforeInHandValue = currentDrawer.getSlipInHandValue() != null ? currentDrawer.getSlipInHandValue() : 0.0;
                    break;
                case ewallet:
                    beforeInHandValue = currentDrawer.getEwalletInHandValue() != null ? currentDrawer.getEwalletInHandValue() : 0.0;
                    break;
                case PatientDeposit:
                    beforeInHandValue = currentDrawer.getPatientDepositInHandValue() != null ? currentDrawer.getPatientDepositInHandValue() : 0.0;
                    break;
                case PatientPoints:
                    beforeInHandValue = currentDrawer.getPatientPointsInHandValue() != null ? currentDrawer.getPatientPointsInHandValue() : 0.0;
                    break;
                case OnlineSettlement:
                    beforeInHandValue = currentDrawer.getOnlineSettlementInHandValue() != null ? currentDrawer.getOnlineSettlementInHandValue() : 0.0;
                    break;
                case None:
                    beforeInHandValue = currentDrawer.getNoneInHandValue() != null ? currentDrawer.getNoneInHandValue() : 0.0;
                    break;
                case YouOweMe:
                    beforeInHandValue = currentDrawer.getYouOweMeInHandValue() != null ? currentDrawer.getYouOweMeInHandValue() : 0.0;
                    break;
                default:

                    break;
            }
        }

        drawerEntry.setBeforeInHandValue(beforeInHandValue);
        drawerEntry.setAfterInHandValue(beforeInHandValue + signedValue);
        // Both totals skip null fields, so a drawer without a cash value (e.g. card only) still gets its real balance
        double totalBalance = currentDrawer.getTotalBalance();

        drawerEntry.setBeforeBalance(totalBalance);
        drawerEntry.setAfterBalance(totalBalance + signedValue);

        double totalShortageOrExcess = currentDrawer.getTotalShortageOrExcess();

        drawerEntry.setBeforeShortageExcess(totalShortageOrExcess);
        drawerEntry.setAfterShortageExcess(totalShortageOrExcess);

        save(drawerEntry);

        //System.out.println("Drawer Entry Created = " + drawerEntry);
    }

    public void drawerEntryUpdate(Bill bill, Drawer currentDrawer, PaymentMethod paymentMethod, WebUser user, Double value) {
        if (bill == null) {
            return;
        }

        drawerEntry = new DrawerEntry();
        drawerEntry.setPaymentMethod(paymentMethod);
        drawerEntry.setBill(bill);
        drawerEntry.setTransactionValue(value);
        double val = drawerEntry.getAfterBalance();
        drawerEntry.setDrawer(currentDrawer);
        drawerEntry.setWebUser(user);
        Double beforeInHandValue = 0.0;

        if (paymentMethod != null) {
            switch (paymentMethod) {
                case Cash:
                    beforeInHandValue = currentDrawer.getCashInHandValue() != null ? currentDrawer.getCashInHandValue() : 0.0;
                    break;
                case Card:
                    beforeInHandValue = currentDrawer.getCardInHandValue() != null ? currentDrawer.getCardInHandValue() : 0.0;
                    break;
                case OnCall:
                    beforeInHandValue = currentDrawer.getOnCallInHandValue() != null ? currentDrawer.getOnCallInHandValue() : 0.0;
                    break;
                case MultiplePaymentMethods:
                    beforeInHandValue = currentDrawer.getMultiplePaymentMethodsInHandValue() != null ? currentDrawer.getMultiplePaymentMethodsInHandValue() : 0.0;
                    break;
                case Staff:
                    beforeInHandValue = currentDrawer.getStaffInHandValue() != null ? currentDrawer.getStaffInHandValue() : 0.0;
                    break;
                case Credit:
                    beforeInHandValue = currentDrawer.getCreditInHandValue() != null ? currentDrawer.getCreditInHandValue() : 0.0;
                    break;
                case Staff_Welfare:
                    beforeInHandValue = currentDrawer.getStaffWelfareInHandValue() != null ? currentDrawer.getStaffWelfareInHandValue() : 0.0;
                    break;
                case Voucher:
                    beforeInHandValue = currentDrawer.getVoucherInHandValue() != null ? currentDrawer.getVoucherInHandValue() : 0.0;
                    break;
                case IOU:
                    beforeInHandValue = currentDrawer.getIouInHandValue() != null ? currentDrawer.getIouInHandValue() : 0.0;
                    break;
                case Agent:
                    beforeInHandValue = currentDrawer.getAgentInHandValue() != null ? currentDrawer.getAgentInHandValue() : 0.0;
                    break;
                case Cheque:
                    beforeInHandValue = currentDrawer.getChequeInHandValue() != null ? currentDrawer.getChequeInHandValue() : 0.0;
                    break;
                case Slip:
                    beforeInHandValue = currentDrawer.getSlipInHandValue() != null ? currentDrawer.getSlipInHandValue() : 0.0;
                    break;
                case ewallet:
                    beforeInHandValue = currentDrawer.getEwalletInHandValue() != null ? currentDrawer.getEwalletInHandValue() : 0.0;
                    break;
                case PatientDeposit:
                    beforeInHandValue = currentDrawer.getPatientDepositInHandValue() != null ? currentDrawer.getPatientDepositInHandValue() : 0.0;
                    break;
                case PatientPoints:
                    beforeInHandValue = currentDrawer.getPatientPointsInHandValue() != null ? currentDrawer.getPatientPointsInHandValue() : 0.0;
                    break;
                case OnlineSettlement:
                    beforeInHandValue = currentDrawer.getOnlineSettlementInHandValue() != null ? currentDrawer.getOnlineSettlementInHandValue() : 0.0;
                    break;
                case None:
                    beforeInHandValue = currentDrawer.getNoneInHandValue() != null ? currentDrawer.getNoneInHandValue() : 0.0;
                    break;
                case YouOweMe:
                    beforeInHandValue = currentDrawer.getYouOweMeInHandValue() != null ? currentDrawer.getYouOweMeInHandValue() : 0.0;
                    break;
                default:

                    break;
            }
        }

        drawerEntry.setBeforeInHandValue(beforeInHandValue);
        drawerEntry.setAfterInHandValue(beforeInHandValue + value);
        System.out.println("drawerEntry.getAfterInHandValue() = " + drawerEntry.getAfterInHandValue());
        double totalBalance;
        if (currentDrawer.getCashInHandValue() == null) {
            totalBalance = 0.0;
        } else {
            totalBalance = currentDrawer.getTotalBalance();
        }
        //System.out.println("totalBalance = " + totalBalance);

        drawerEntry.setBeforeBalance(totalBalance);
        drawerEntry.setAfterBalance(totalBalance + value);

        double totalShortageOrExcess;
        if (currentDrawer.getCashInHandValue() == null) {
            totalShortageOrExcess = 0.0;
        } else {
            totalShortageOrExcess = currentDrawer.getTotalShortageOrExcess();
        }
        //System.out.println("totalShortageOrExcess = " + totalShortageOrExcess);

        drawerEntry.setBeforeShortageExcess(totalShortageOrExcess);
        drawerEntry.setAfterShortageExcess(totalShortageOrExcess);

        save(drawerEntry);

        //System.out.println("Drawer Entry Created = " + drawerEntry);
    }

    public void updateDrawerForOuts(List<Payment> payments) {
        for (Payment payment : payments) {
            updateDrawerForOuts(payment);
        }
    }

    public void updateDrawerForIns(Payment payment) {
        updateDrawer(payment, Math.abs(payment.getPaidValue()));
    }

    public void updateDrawerForOuts(Payment payment) {
        updateDrawer(payment, -Math.abs(payment.getPaidValue()));
    }

    public void updateDrawer(Payment payment) {
        updateDrawer(payment, payment.getPaidValue());
    }

    public void updateDrawer(Payment payment, double paidValue) {
        if (payment == null || payment.getCreater() == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }

        Drawer drawer = lockUsersDrawer(payment.getCreater());
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }

        //update Drover History with the signed value applied to the balance below
        drawerEntryUpdate(payment, drawer, payment.getCreater(), paidValue);

        addToBalance(drawer, payment.getPaymentMethod(), paidValue);
        drawerFacade.editAndCommit(drawer);
    }

    /**
     * Applies {@code paidValue} to {@code webUser}'s drawer and records it in that
     * user's drawer history, in one transaction under a row lock. Unlike
     * {@link #updateDrawer(Payment, double, WebUser)}, the history entry is tagged
     * with {@code webUser} rather than the payment's creator - the behaviour
     * DrawerController has always had.
     */
    public void updateUsersDrawer(Payment payment, double paidValue, WebUser webUser) {
        if (payment == null || webUser == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }
        Drawer drawer = lockUsersDrawer(webUser);
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }
        drawerEntryUpdate(payment, drawer, webUser, paidValue);
        addToBalance(drawer, payment.getPaymentMethod(), paidValue);
        drawerFacade.editAndCommit(drawer);
    }

    /**
     * Applies {@code value} for a bill (no Payment row) to {@code webUser}'s drawer
     * and records it in the drawer history, in one transaction under a row lock.
     */
    public void updateDrawer(Bill bill, double value, PaymentMethod paymentMethod, WebUser webUser) {
        if (bill == null || webUser == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }
        Drawer drawer = lockUsersDrawer(webUser);
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }
        drawerEntryUpdate(bill, drawer, paymentMethod, webUser, value);
        addToBalance(drawer, paymentMethod, value);
        drawerFacade.editAndCommit(drawer);
    }

    /**
     * The user's drawer, re-read from the database with a row lock (SELECT ... FOR
     * UPDATE) held until the current transaction ends. Every balance change reads the
     * old value and writes the new one, so two requests on one drawer (the same
     * cashier in two tabs, or the online/API settle) must not interleave: without the
     * lock both read the same balance and one delta is lost (issue #25123). A drawer
     * that does not exist yet is created unlocked, since no other request can see it.
     */
    private Drawer lockUsersDrawer(WebUser webUser) {
        Drawer existing = findUsersDrawerWithoutCreate(webUser);
        if (existing == null) {
            return getUsersDrawer(webUser);
        }
        return drawerFacade.findForUpdate(existing.getId());
    }

    /** {@link #lockUsersDrawer} for a drawer the caller already holds. */
    private Drawer lockForUpdate(Drawer drawer) {
        if (drawer == null || drawer.getId() == null) {
            return drawer;
        }
        Drawer locked = drawerFacade.findForUpdate(drawer.getId());
        return locked != null ? locked : drawer;
    }

    /** Adds {@code value} to the in-hand value and balance of the payment method's columns. */
    private void addToBalance(Drawer drawer, PaymentMethod paymentMethod, double value) {
        if (paymentMethod == null) {
            System.err.println("Drawer update without a payment method.");
            return;
        }
        switch (paymentMethod) {
            case OnCall:
                drawer.setOnCallInHandValue(safeAdd(drawer.getOnCallInHandValue(), value));
                drawer.setOnCallBalance(safeAdd(drawer.getOnCallBalance(), value));
                break;
            case Cash:
                drawer.setCashInHandValue(safeAdd(drawer.getCashInHandValue(), value));
                drawer.setCashBalance(safeAdd(drawer.getCashBalance(), value));
                break;
            case Card:
                drawer.setCardInHandValue(safeAdd(drawer.getCardInHandValue(), value));
                drawer.setCardBalance(safeAdd(drawer.getCardBalance(), value));
                break;
            case MultiplePaymentMethods:
                drawer.setMultiplePaymentMethodsInHandValue(safeAdd(drawer.getMultiplePaymentMethodsInHandValue(), value));
                drawer.setMultiplePaymentMethodsBalance(safeAdd(drawer.getMultiplePaymentMethodsBalance(), value));
                break;
            case Staff:
                drawer.setStaffInHandValue(safeAdd(drawer.getStaffInHandValue(), value));
                drawer.setStaffBalance(safeAdd(drawer.getStaffBalance(), value));
                break;
            case Credit:
                drawer.setCreditInHandValue(safeAdd(drawer.getCreditInHandValue(), value));
                drawer.setCreditBalance(safeAdd(drawer.getCreditBalance(), value));
                break;
            case Staff_Welfare:
                drawer.setStaffWelfareInHandValue(safeAdd(drawer.getStaffWelfareInHandValue(), value));
                drawer.setStaffWelfareBalance(safeAdd(drawer.getStaffWelfareBalance(), value));
                break;
            case Voucher:
                drawer.setVoucherInHandValue(safeAdd(drawer.getVoucherInHandValue(), value));
                drawer.setVoucherBalance(safeAdd(drawer.getVoucherBalance(), value));
                break;
            case IOU:
                drawer.setIouInHandValue(safeAdd(drawer.getIouInHandValue(), value));
                drawer.setIouBalance(safeAdd(drawer.getIouBalance(), value));
                break;
            case Agent:
                drawer.setAgentInHandValue(safeAdd(drawer.getAgentInHandValue(), value));
                drawer.setAgentBalance(safeAdd(drawer.getAgentBalance(), value));
                break;
            case Cheque:
                drawer.setChequeInHandValue(safeAdd(drawer.getChequeInHandValue(), value));
                drawer.setChequeBalance(safeAdd(drawer.getChequeBalance(), value));
                break;
            case Slip:
                drawer.setSlipInHandValue(safeAdd(drawer.getSlipInHandValue(), value));
                drawer.setSlipBalance(safeAdd(drawer.getSlipBalance(), value));
                break;
            case ewallet:
                drawer.setEwalletInHandValue(safeAdd(drawer.getEwalletInHandValue(), value));
                drawer.setEwalletBalance(safeAdd(drawer.getEwalletBalance(), value));
                break;
            case PatientDeposit:
                drawer.setPatientDepositInHandValue(safeAdd(drawer.getPatientDepositInHandValue(), value));
                drawer.setPatientDepositBalance(safeAdd(drawer.getPatientDepositBalance(), value));
                break;
            case PatientPoints:
                drawer.setPatientPointsInHandValue(safeAdd(drawer.getPatientPointsInHandValue(), value));
                drawer.setPatientPointsBalance(safeAdd(drawer.getPatientPointsBalance(), value));
                break;
            case OnlineSettlement:
                drawer.setOnlineSettlementInHandValue(safeAdd(drawer.getOnlineSettlementInHandValue(), value));
                drawer.setOnlineSettlementBalance(safeAdd(drawer.getOnlineSettlementBalance(), value));
                break;
            case None:
                drawer.setNoneInHandValue(safeAdd(drawer.getNoneInHandValue(), value));
                drawer.setNoneBalance(safeAdd(drawer.getNoneBalance(), value));
                break;
            case YouOweMe:
                drawer.setYouOweMeInHandValue(safeAdd(drawer.getYouOweMeInHandValue(), value));
                drawer.setYouOweMeBalance(safeAdd(drawer.getYouOweMeBalance(), value));
                break;
            default:
                System.err.println("Unhandled payment method for drawer update: " + paymentMethod);
                break;
        }
    }

    public double safeAdd(Double currentValue, double addValue) {
        if (currentValue == null) {
            return addValue;
        }
        return currentValue + addValue;
    }

    public void save(DrawerEntry drawerEntry) {
        save(drawerEntry, null);
    }

    public void save(DrawerEntry drawerEntry, WebUser user) {
        if (drawerEntry == null) {
            return;
        }
        if (drawerEntry.getId() == null) {
            if (drawerEntry.getCreater() == null) {
                drawerEntry.setCreater(user);
            }
            if (drawerEntry.getCreatedAt() == null) {
                drawerEntry.setCreatedAt(new Date());
            }
            ejbFacade.create(drawerEntry);
        } else {
            ejbFacade.edit(drawerEntry);
        }
    }

    public void save(Drawer drawerEntry) {
        save(drawerEntry, null);
    }

    public void save(Drawer drawer, WebUser user) {
        if (drawer == null) {
            return;
        }
        if (drawer.getId() == null) {
            if (drawer.getCreater() == null) {
                drawer.setCreater(user);
            }
            if (drawer.getCreatedAt() == null) {
                drawer.setCreatedAt(new Date());
            }
            drawerFacade.create(drawer);
        } else {
            drawerFacade.edit(drawer);
        }
    }

    public Drawer getUsersDrawer(WebUser webUser) {
        String jpql;
        HashMap m = new HashMap();
        jpql = "select d from Drawer d "
                + " where d.retired=false "
                + " and d.drawerUser=:user";

        m.put("user", webUser);

        Drawer drawer;
        drawer = drawerFacade.findFirstByJpql(jpql, m);

        if (drawer == null) {
            drawer = new Drawer();
            drawer.setDrawerUser(webUser);
            save(drawer);
        }
        return drawer;
    }

    public Drawer findUsersDrawerWithoutCreate(WebUser webUser) {
        if (webUser == null) {
            return null;
        }
        HashMap m = new HashMap();
        String jpql = "select d from Drawer d "
                + " where d.retired=false "
                + " and d.drawerUser=:user";
        m.put("user", webUser);
        return drawerFacade.findFirstByJpql(jpql, m);
    }

    /**
     * Applies a drawer adjustment by creating a DrawerEntry and updating the
     * appropriate balance fields on the drawer for the given payment method.
     *
     * @param drawer the drawer to adjust
     * @param paymentMethod the payment method column to adjust
     * @param delta positive to add, negative to deduct
     * @param bill the adjustment bill (for audit trail)
     * @param user the user performing the adjustment
     */
    public void applyDrawerAdjustment(Drawer drawer, PaymentMethod paymentMethod, double delta, Bill bill, WebUser user) {
        if (drawer == null || paymentMethod == null || bill == null || user == null) {
            return;
        }
        drawer = lockForUpdate(drawer);
        // The drawer history filters by webUser, so the entry must be tagged with the
        // drawer owner (cashier whose drawer changed), not the actor approving/applying it.
        WebUser drawerOwner = drawer.getDrawerUser() != null ? drawer.getDrawerUser() : user;
        drawerEntryUpdate(bill, drawer, paymentMethod, drawerOwner, delta);
        if (drawerEntry != null) {
            drawerEntry.setCreater(user);
            save(drawerEntry);
        }
        addToBalance(drawer, paymentMethod, delta);
        drawerFacade.editAndCommit(drawer);
    }

    /**
     * Returns the full list of PaymentMethod values supported by drawer
     * adjustments (read or write). OnlineBookingAgent is excluded — see
     * ADJUSTABLE_PAYMENT_METHODS.
     *
     * @return an unmodifiable list of the 18 adjustable payment methods
     */
    public List<PaymentMethod> getAdjustablePaymentMethods() {
        return ADJUSTABLE_PAYMENT_METHODS;
    }

    /**
     * Read-only lookup of a drawer's current in-hand value for a payment
     * method, mirroring the beforeInHandValue switch in drawerEntryUpdate.
     *
     * @param drawer the drawer to read from
     * @param paymentMethod the payment method column to read
     * @return the raw (possibly null) in-hand value for that payment method,
     * or null if drawer/paymentMethod is null or the method is unsupported
     */
    public Double getDrawerInHandValue(Drawer drawer, PaymentMethod paymentMethod) {
        if (drawer == null || paymentMethod == null) {
            return null;
        }
        switch (paymentMethod) {
            case OnCall:
                return drawer.getOnCallInHandValue();
            case Cash:
                return drawer.getCashInHandValue();
            case Card:
                return drawer.getCardInHandValue();
            case MultiplePaymentMethods:
                return drawer.getMultiplePaymentMethodsInHandValue();
            case Staff:
                return drawer.getStaffInHandValue();
            case Credit:
                return drawer.getCreditInHandValue();
            case Staff_Welfare:
                return drawer.getStaffWelfareInHandValue();
            case Voucher:
                return drawer.getVoucherInHandValue();
            case IOU:
                return drawer.getIouInHandValue();
            case Agent:
                return drawer.getAgentInHandValue();
            case Cheque:
                return drawer.getChequeInHandValue();
            case Slip:
                return drawer.getSlipInHandValue();
            case ewallet:
                return drawer.getEwalletInHandValue();
            case PatientDeposit:
                return drawer.getPatientDepositInHandValue();
            case PatientPoints:
                return drawer.getPatientPointsInHandValue();
            case OnlineSettlement:
                return drawer.getOnlineSettlementInHandValue();
            case None:
                return drawer.getNoneInHandValue();
            case YouOweMe:
                return drawer.getYouOweMeInHandValue();
            default:
                return null;
        }
    }

    /**
     * Simple data holder describing a single payment method's balance before
     * and after a resetDrawerBalance() call.
     */
    public static class DrawerBalanceSnapshot {

        private final PaymentMethod paymentMethod;
        private final double before;
        private final double after;
        private final boolean changed;

        public DrawerBalanceSnapshot(PaymentMethod paymentMethod, double before, double after, boolean changed) {
            this.paymentMethod = paymentMethod;
            this.before = before;
            this.after = after;
            this.changed = changed;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public double getBefore() {
            return before;
        }

        public double getAfter() {
            return after;
        }

        public boolean isChanged() {
            return changed;
        }
    }

    /**
     * Resets (or sets) a target user's drawer balance to targetBalance, for
     * one payment method or, when paymentMethod is null, for every
     * adjustable payment method. Reuses applyDrawerAdjustment for the
     * actual Drawer/DrawerEntry mutation, so this goes through the same
     * audit trail as the "Adjust Drawer Balance -> Admin" UI flow
     * (issue #24433 - QA/E2E test setup).
     *
     * Only payment methods whose balance actually changes get a Bill +
     * DrawerEntry written, so idempotent re-runs do not flood the drawer
     * history with zero-value entries (methods already at the target value
     * are still reported in the result, with changed=false).
     *
     * @param targetUser the user whose drawer is being reset
     * @param paymentMethod a single payment method to reset, or null to
     * reset every adjustable payment method
     * @param targetBalance the balance each selected payment method should
     * end up at
     * @param comment optional caller-supplied comment; falls back to a
     * default QA/E2E placeholder when blank
     * @param actor the API/user performing the reset (recorded as the
     * creator of each adjustment bill and drawer entry)
     * @return one DrawerBalanceSnapshot per affected payment method
     */
    public List<DrawerBalanceSnapshot> resetDrawerBalance(WebUser targetUser, PaymentMethod paymentMethod, double targetBalance, String comment, WebUser actor) {
        if (targetUser == null || actor == null) {
            throw new IllegalArgumentException("targetUser and actor are required");
        }
        if (targetUser.getDepartment() == null || targetUser.getInstitution() == null) {
            throw new IllegalArgumentException("Target user has no department/institution set; cannot generate an adjustment bill number");
        }

        // Locked, so the deltas below are computed from the balance no other request can change.
        Drawer drawer = lockUsersDrawer(targetUser);

        List<PaymentMethod> methods = paymentMethod != null
                ? Collections.singletonList(paymentMethod)
                : getAdjustablePaymentMethods();

        List<DrawerBalanceSnapshot> snapshots = new ArrayList<>();
        for (PaymentMethod pm : methods) {
            Double beforeRaw = getDrawerInHandValue(drawer, pm);
            double before = beforeRaw != null ? beforeRaw : 0.0;
            double delta = targetBalance - before;
            // Half-a-cent tolerance so a stored value like 0.30000000000000004 compared
            // against a requested 0.3 isn't treated as a real change (double rounding noise,
            // not an actual balance difference) - see issue #24433 review.
            boolean changed = Math.abs(delta) >= 0.005;
            if (changed) {
                Bill bill = createDrawerAdjustmentBill(targetUser, pm, delta, comment, actor);
                applyDrawerAdjustment(drawer, pm, delta, bill, actor);
            }
            snapshots.add(new DrawerBalanceSnapshot(pm, before, changed ? before + delta : targetBalance, changed));
        }
        return snapshots;
    }

    /**
     * Creates and persists a DrawerAdjustment bill for resetDrawerBalance(),
     * mirroring DrawerAdjustmentController.createAndPersistAdjustmentBill but
     * using the target drawer user's department/institution (there is no
     * admin session here) and recording the actor (not the target user) as
     * the bill's creator (issue #24433, decisions D5/D7).
     */
    private Bill createDrawerAdjustmentBill(WebUser targetUser, PaymentMethod paymentMethod, double delta, String comment, WebUser actor) {
        Department department = targetUser.getDepartment();
        Institution institution = targetUser.getInstitution();

        BilledBill bill = new BilledBill();
        bill.setBillType(BillType.DrawerAdjustment);
        bill.setBillTypeAtomic(BillTypeAtomic.DRAWER_ADJUSTMENT);
        bill.setCreatedAt(new Date());
        bill.setCreater(actor);
        bill.setDeptId(billNumberBean.institutionBillNumberGenerator(
                department, BillType.DrawerAdjustment, BillClassType.BilledBill, BillNumberSuffix.DRADJ));
        bill.setInsId(billNumberBean.institutionBillNumberGenerator(
                institution, BillType.DrawerAdjustment, BillClassType.BilledBill, BillNumberSuffix.DRADJ));
        bill.setDepartment(department);
        bill.setInstitution(institution);
        bill.setFromDepartment(department);
        bill.setFromInstitution(institution);
        bill.setNetTotal(delta);
        String baseComment = (comment != null && !comment.trim().isEmpty())
                ? comment.trim()
                : "QA/E2E drawer balance reset via API";
        bill.setComments(baseComment + " [" + paymentMethod + "]");
        billFacade.create(bill);
        return bill;
    }

    /**
     * Checks if the specified drawer has enough balance for the given refund
     * amount using the specified payment method.
     *
     * @param drawer the drawer to check balance from
     * @param paymentMethod the payment method to consider
     * @param refundAmount the amount to be refunded
     * @return true if the drawer has sufficient balance, false otherwise
     */
    public boolean hasSufficientDrawerBalance(Drawer drawer, PaymentMethod paymentMethod, Double refundAmount) {
        // method implementation
        boolean canReturn = false;
        switch (paymentMethod) {
            case Cash:
                if (drawer.getCashInHandValue() != null) {
                    if (drawer.getCashInHandValue() < refundAmount) {
                        canReturn = false;
                    } else {
                        canReturn = true;
                    }
                } else {
                    canReturn = false;
                }
                break;
            case Card:
                canReturn = true;
                break;
            case ewallet:
                if (drawer.getEwalletInHandValue() != null) {
                    canReturn = drawer.getEwalletInHandValue() >= refundAmount;
                }
                break;
            case MultiplePaymentMethods:
                canReturn = true;
                break;
            case Staff:
                canReturn = true;
                break;
            case Credit:
                canReturn = true;
                break;
            case Staff_Welfare:
                canReturn = true;
                break;
            case Cheque:
                canReturn = true;
                break;
            case Slip:
                canReturn = true;
                break;
            case OnlineSettlement:
                canReturn = true;
                break;
            case PatientDeposit:
                canReturn = true;
                break;
            default:
                break;
        }
        if (!configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
            canReturn = true;
        }
        return canReturn;
    }

}

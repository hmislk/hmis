package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.entity.cashTransaction.DrawerEntry;
import com.divudi.core.facade.DrawerEntryFacade;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.bean.common.ConfigOptionApplicationController;
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
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    DrawerEntry drawerEntry;

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
        System.out.println("paidValue = " + paidValue);
        System.out.println("payment = " + payment);
        if (payment == null || payment.getCreater() == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }

        Drawer drawer = getUsersDrawer(webUser);
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }

        //update Drover History
        drawerEntryUpdate(payment, drawer);

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

            drawerFacade.editAndCommit(drawer);
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
        drawerEntryUpdate(payment, currentDrawer,payment.getCreater() );
    }

    public void drawerEntryUpdate(Payment payment, Drawer currentDrawer, WebUser user) {
        System.out.println("Drawer Entry Update");
        if (payment == null) {
            return;
        }

        drawerEntry = new DrawerEntry();
        drawerEntry.setPayment(payment);
        drawerEntry.setPaymentMethod(payment.getPaymentMethod());
        drawerEntry.setBill(payment.getBill());
        drawerEntry.setDrawer(currentDrawer);
        drawerEntry.setWebUser(user);
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
        System.out.println("beforeInHandValue = " + beforeInHandValue);
        System.out.println("payment.getPaidValue() = " + payment.getPaidValue());
        drawerEntry.setAfterInHandValue(beforeInHandValue + payment.getPaidValue());
        System.out.println("drawerEntry.getAfterInHandValue() = " + drawerEntry.getAfterInHandValue());
        double totalBalance;
        if (currentDrawer.getCashInHandValue() == null) {
            totalBalance = 0.0;
        } else {
            totalBalance = currentDrawer.getTotalBalance();
        }
        //System.out.println("totalBalance = " + totalBalance);

        drawerEntry.setBeforeBalance(totalBalance);
        drawerEntry.setAfterBalance(totalBalance + payment.getPaidValue());

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


    public void drawerEntryUpdate(Bill bill, Drawer currentDrawer, PaymentMethod paymentMethod, WebUser user, Double value) {
        System.out.println("Drawer Entry Update");
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
        System.out.println("paidValue = " + paidValue);
        System.out.println("payment = " + payment);
        if (payment == null || payment.getCreater() == null) {
            System.err.println("Payment or payment creator is null.");
            return;
        }

        Drawer drawer = getUsersDrawer(payment.getCreater());
        if (drawer == null) {
            System.err.println("No drawer found for the user.");
            return;
        }

        //update Drover History
        drawerEntryUpdate(payment, drawer);

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

            drawerFacade.editAndCommit(drawer);
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

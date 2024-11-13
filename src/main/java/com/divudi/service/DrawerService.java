package com.divudi.service;

import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.IOU;
import static com.divudi.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.data.PaymentMethod.None;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.PatientDeposit;
import static com.divudi.data.PaymentMethod.PatientPoints;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import static com.divudi.data.PaymentMethod.Staff_Welfare;
import static com.divudi.data.PaymentMethod.Voucher;
import static com.divudi.data.PaymentMethod.YouOweMe;
import static com.divudi.data.PaymentMethod.ewallet;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.Drawer;
import com.divudi.entity.cashTransaction.DrawerEntry;
import com.divudi.facade.DrawerEntryFacade;
import com.divudi.facade.DrawerFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

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
        System.out.println("Drawer Entry Update");
        //System.out.println("current Drawer = " + currentDrawer);
        //System.out.println("payment = " + payment);
        if (payment == null) {
            //System.out.println("Null");
            return;
        }

        drawerEntry = new DrawerEntry();
        drawerEntry.setPayment(payment);
        drawerEntry.setPaymentMethod(payment.getPaymentMethod());
        drawerEntry.setBill(payment.getBill());
        drawerEntry.setDrawer(currentDrawer);
        drawerEntry.setWebUser(payment.getCreater());

        if (payment.getPaymentMethod() == PaymentMethod.Cash) {
            //System.out.println("Cash");

            double beforeInHandValue;
            if (currentDrawer.getCashInHandValue() == null) {
                beforeInHandValue = 0.0;
            } else {
                beforeInHandValue = currentDrawer.getCashInHandValue();
            }
            //System.out.println("beforeInHandValue = " + beforeInHandValue);

            drawerEntry.setBeforeInHandValue(beforeInHandValue);
            drawerEntry.setAfterInHandValue(beforeInHandValue + payment.getPaidValue());
        }

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

}

package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Container-free rules for the Non-Cash Settlement approval workflow (#24541).
 * Kept separate from PaymentSettlementService so they can be unit tested.
 */
public final class NonCashSettlementRules {

    private NonCashSettlementRules() {
    }

    public static Map<PaymentMethod, Double> totalsByMethod(List<Payment> payments) {
        Map<PaymentMethod, Double> totals = new EnumMap<>(PaymentMethod.class);
        if (payments == null) {
            return totals;
        }
        for (Payment p : payments) {
            if (p == null || p.getPaymentMethod() == null) {
                continue;
            }
            totals.merge(p.getPaymentMethod(), p.getPaidValue(), Double::sum);
        }
        return totals;
    }

    public static String approvalBlocker(WebUser requester, WebUser approver) {
        if (requester == null) {
            return "The settlement request has no requester.";
        }
        if (approver == null) {
            return "Approver is required.";
        }
        if (requester.equals(approver)) {
            return "You cannot approve your own settlement request. Another authorised user must approve it.";
        }
        return null;
    }

    public static String paymentApprovalBlocker(Payment p, Bill settlementBill, WebUser requester) {
        if (p == null) {
            return "A payment in this request no longer exists.";
        }
        if (p.isCancelled() || p.isRetired()) {
            return "Payment " + p.getId() + " has been cancelled or removed since the request was made.";
        }
        if (p.isPaymentSettled()) {
            return "Payment " + p.getId() + " is already settled.";
        }
        if (p.getPaymentSettlementBill() == null || !p.getPaymentSettlementBill().equals(settlementBill)) {
            return "Payment " + p.getId() + " is no longer part of this settlement request.";
        }
        if (p.getCurrentHolder() == null || !p.getCurrentHolder().equals(requester)) {
            return "Payment " + p.getId() + " is no longer held by the requester.";
        }
        return null;
    }

    public static boolean reversesDrawerOnCancel(Bill settlementBill) {
        return settlementBill != null && settlementBill.getApproveAt() != null;
    }
}

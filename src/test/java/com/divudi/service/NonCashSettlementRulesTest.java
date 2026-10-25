package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NonCashSettlementRulesTest {

    private static WebUser user(long id) {
        WebUser u = new WebUser();
        u.setId(id);
        return u;
    }

    private static Bill bill(long id) {
        Bill b = new Bill();
        b.setId(id);
        return b;
    }

    private static Payment payment(long id, PaymentMethod m, double value, Bill settlementBill, WebUser holder) {
        Payment p = new Payment();
        p.setId(id);
        p.setPaymentMethod(m);
        p.setPaidValue(value);
        p.setPaymentSettlementBill(settlementBill);
        p.setCurrentHolder(holder);
        return p;
    }

    @Test
    public void totalsByMethod_mixed() {
        Bill b = bill(1);
        WebUser h = user(10);
        Map<PaymentMethod, Double> t = NonCashSettlementRules.totalsByMethod(Arrays.asList(
                payment(1, PaymentMethod.Card, 100.0, b, h),
                payment(2, PaymentMethod.Card, 50.5, b, h),
                payment(3, PaymentMethod.Cheque, 200.0, b, h),
                null));
        assertEquals(2, t.size());
        assertEquals(150.5, t.get(PaymentMethod.Card), 0.0001);
        assertEquals(200.0, t.get(PaymentMethod.Cheque), 0.0001);
    }

    @Test
    public void approvalBlocker_sameUser() {
        assertNotNull(NonCashSettlementRules.approvalBlocker(user(5), user(5)));
    }

    @Test
    public void approvalBlocker_differentUser() {
        assertNull(NonCashSettlementRules.approvalBlocker(user(5), user(6)));
    }

    @Test
    public void approvalBlocker_nulls() {
        assertNotNull(NonCashSettlementRules.approvalBlocker(null, user(6)));
        assertNotNull(NonCashSettlementRules.approvalBlocker(user(5), null));
    }

    @Test
    public void paymentBlocker_ok() {
        Bill b = bill(1);
        WebUser h = user(10);
        assertNull(NonCashSettlementRules.paymentApprovalBlocker(payment(7, PaymentMethod.Card, 1, b, h), b, h));
    }

    @Test
    public void paymentBlocker_otherBill() {
        WebUser h = user(10);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(payment(7, PaymentMethod.Card, 1, bill(2), h), bill(1), h));
    }

    @Test
    public void paymentBlocker_holderChanged() {
        Bill b = bill(1);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(payment(7, PaymentMethod.Card, 1, b, user(11)), b, user(10)));
    }

    @Test
    public void paymentBlocker_alreadySettled() {
        Bill b = bill(1);
        WebUser h = user(10);
        Payment p = payment(7, PaymentMethod.Card, 1, b, h);
        p.setPaymentSettled(true);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(p, b, h));
    }

    @Test
    public void paymentBlocker_cancelledOrRetired() {
        Bill b = bill(1);
        WebUser h = user(10);
        Payment c = payment(7, PaymentMethod.Card, 1, b, h);
        c.setCancelled(true);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(c, b, h));
        Payment r = payment(8, PaymentMethod.Card, 1, b, h);
        r.setRetired(true);
        assertNotNull(NonCashSettlementRules.paymentApprovalBlocker(r, b, h));
    }

    @Test
    public void reversesDrawerOnCancel_legacyBill() {
        assertFalse(NonCashSettlementRules.reversesDrawerOnCancel(bill(1)));
    }

    @Test
    public void reversesDrawerOnCancel_approvedBill() {
        Bill b = bill(1);
        b.setApproveAt(new Date());
        assertTrue(NonCashSettlementRules.reversesDrawerOnCancel(b));
    }
}

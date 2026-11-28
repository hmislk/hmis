package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Bill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Issue #26324: the pre-bill settle bill stores balance, cash paid and change like the normal OPD bill. */
class OpdPreSettleAmountsTest {

    private Bill bill(double net) {
        Bill b = new BilledBill();
        b.setNetTotal(net);
        return b;
    }

    @Test
    void cashPaidInFullGivesZeroBalanceAndChange() {
        Bill b = bill(1000);
        OpdPreSettleAmounts.apply(b, PaymentMethod.Cash, 1500);
        assertEquals(1500, b.getCashPaid(), 0.001);
        assertEquals(500, b.getCashBalance(), 0.001);
        assertEquals(0, b.getBalance(), 0.001);
    }

    @Test
    void cardPaidInFullGivesZeroBalance() {
        Bill b = bill(1000);
        OpdPreSettleAmounts.apply(b, PaymentMethod.Card, 1000);
        assertEquals(0, b.getBalance(), 0.001);
        assertEquals(0, b.getCashBalance(), 0.001);
    }

    @Test
    void creditAndStaffKeepFullBalance() {
        Bill b = bill(1000);
        OpdPreSettleAmounts.apply(b, PaymentMethod.Credit, 1000);
        assertEquals(1000, b.getBalance(), 0.001);
        Bill s = bill(800);
        OpdPreSettleAmounts.apply(s, PaymentMethod.Staff, 800);
        assertEquals(800, s.getBalance(), 0.001);
    }

    @Test
    void partialPaymentKeepsTheUnpaidPart() {
        Bill b = bill(1000);
        OpdPreSettleAmounts.apply(b, PaymentMethod.Cash, 400);
        assertEquals(600, b.getBalance(), 0.001);
    }
}

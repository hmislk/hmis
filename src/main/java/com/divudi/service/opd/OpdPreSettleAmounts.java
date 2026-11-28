package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;

/**
 * Cash paid, change and balance of the OPD pre-bill batch settle bill (#26324).
 * Mirrors the normal OPD bill (OpdBillController, batch bill save): the change is
 * cash paid minus net total, the balance is the unpaid part, and Credit and Staff
 * Credit keep the full net total as balance.
 */
public final class OpdPreSettleAmounts {

    private OpdPreSettleAmounts() {
    }

    public static void apply(Bill bill, PaymentMethod pm, double cashPaid) {
        double remaining = cashPaid - bill.getNetTotal();
        bill.setCashPaid(cashPaid);
        bill.setCashBalance(remaining);
        bill.setBalance(remaining < 0 ? Math.abs(remaining) : 0.0);
        if ((pm == PaymentMethod.Credit || pm == PaymentMethod.Staff) && bill.getBalance() <= 0.0) {
            bill.setBalance(Math.abs(bill.getNetTotal()));
        }
    }
}

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
        double net = bill.getNetTotal();
        if (pm == PaymentMethod.Credit || pm == PaymentMethod.Staff) {
            // Credit and Staff Credit: the whole net total is still owed.
            bill.setCashPaid(cashPaid);
            bill.setCashBalance(round2(cashPaid - net));
            bill.setBalance(Math.abs(net));
        } else if (pm == PaymentMethod.Cash) {
            // The settle action blocks Cash when the tendered amount is lower than the net total.
            double change = round2(cashPaid - net);
            bill.setCashPaid(cashPaid);
            bill.setCashBalance(change);
            bill.setBalance(change < 0 ? Math.abs(change) : 0.0);
        } else {
            // Every other method is paid in full; a stale cash paid value is ignored.
            bill.setCashPaid(net);
            bill.setCashBalance(0.0);
            bill.setBalance(0.0);
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}

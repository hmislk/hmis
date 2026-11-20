package com.divudi.core.util;

import com.divudi.core.data.BillCategory;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CancelledBill;

/**
 * A cancellation bill is the contra record a cancel creates. It must never be
 * cancelled itself: doing so adds the original charge back. To reverse a
 * cancellation, the charge is created again as a new bill.
 */
public final class CancellationBillCheck {

    public static final String MESSAGE
            = "This is a cancellation bill and cannot be cancelled. To reverse it, create the charge again.";

    private CancellationBillCheck() {
    }

    public static boolean isCancellationBill(Bill b) {
        if (b == null) {
            return false;
        }
        return b instanceof CancelledBill
                || (b.getBillTypeAtomic() != null
                && b.getBillTypeAtomic().getBillCategory() == BillCategory.CANCELLATION);
    }

    /**
     * Adds the standard error message and returns true when the bill is a cancellation bill.
     */
    public static boolean refuse(Bill b) {
        if (isCancellationBill(b)) {
            JsfUtil.addErrorMessage(MESSAGE);
            return true;
        }
        return false;
    }
}

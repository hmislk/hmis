package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;

/**
 * Stores the credit company and the credit reference number on a bill settled
 * with the Credit method, as the normal OPD bill does (#26326). The Outpatient
 * Credit Collection search finds bills by this credit company.
 */
public final class OpdPreSettleCredit {

    private OpdPreSettleCredit() {
    }

    public static void apply(Bill bill, PaymentMethod paymentMethod, PaymentMethodData data) {
        if (bill == null || data == null || paymentMethod != PaymentMethod.Credit || data.getCredit() == null) {
            return;
        }
        bill.setCreditCompany(data.getCredit().getInstitution());
        bill.setReferenceNumber(data.getCredit().getReferenceNo());
    }
}

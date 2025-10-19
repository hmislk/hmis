package com.divudi.bean.common;

import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;

/**
 * @author Senula Nanayakkara
 */
public interface ControllerWithMultiplePayments {
    double calculatRemainForMultiplePaymentTotal();

    void recieveRemainAmountAutomatically();

    void setPaymentMethodData(PaymentMethodData paymentMethodData);

    PaymentMethodData getPaymentMethodData();

    /**
     * Checks if the given ComponentDetail is the last payment entry in the multiple payment methods list.
     * This is used to determine which payment fields should be editable in the UI.
     *
     * @param cd The ComponentDetail to check
     * @return true if cd is the last entry, false otherwise
     */
    boolean isLastPaymentEntry(ComponentDetail cd);
}

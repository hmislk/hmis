package com.divudi.bean.common;

import com.divudi.core.data.dataStructure.PaymentMethodData;

/**
 * @author Senula Nanayakkara
 */
public interface ControllerWithMultiplePayments {
    double calculatRemainForMultiplePaymentTotal();

    void recieveRemainAmountAutomatically();

    void setPaymentMethodData(PaymentMethodData paymentMethodData);

    PaymentMethodData getPaymentMethodData();
}

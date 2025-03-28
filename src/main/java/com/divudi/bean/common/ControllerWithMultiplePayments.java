package com.divudi.bean.common;

import com.divudi.core.data.dataStructure.PaymentMethodData;

/**
 *
 * @author Senula Nanayakkara
 */
public interface ControllerWithMultiplePayments {
    public double calculatRemainForMultiplePaymentTotal();
    public void recieveRemainAmountAutomatically();
    public void setPaymentMethodData(PaymentMethodData paymentMethodData);
    public PaymentMethodData getPaymentMethodData();

}

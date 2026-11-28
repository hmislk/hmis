package com.divudi.bean.common;

import com.divudi.core.data.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Multiple Payment Methods chosen first in a new session must not throw: the
 * payment method data has to be created through its getter, not read from the
 * (still null) field.
 */
class OpdPreSettleControllerPaymentMethodDataTest {

    @Test
    void remainAmountWithMultiplePaymentMethodsOnFreshControllerDoesNotThrow() {
        OpdPreSettleController c = new OpdPreSettleController();
        c.getPreBill().setNetTotal(100.0);
        c.getPreBill().setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

        assertEquals(100.0, c.calculatRemainForMultiplePaymentTotal(), 0.0001);
    }
}

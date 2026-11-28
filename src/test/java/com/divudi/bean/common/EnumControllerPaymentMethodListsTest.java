package com.divudi.bean.common;

import com.divudi.core.data.PaymentMethod;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Issue #26262: loading a missing config option reloads the option cache,
 * and the reload calls EnumController.resetPaymentMethods(). That used to
 * null the list the getter was filling, which caused a NullPointerException.
 */
class EnumControllerPaymentMethodListsTest {

    /**
     * Stub that behaves like the real controller on the first read of a
     * missing option: it resets the payment method lists.
     */
    private static class ResettingConfigStub extends ConfigOptionApplicationController {

        private final EnumController enumController;
        private boolean resetDone;

        ResettingConfigStub(EnumController enumController) {
            this.enumController = enumController;
        }

        @Override
        public Boolean getBooleanValueByKey(String key, boolean defaultValue) {
            if (!resetDone) {
                resetDone = true;
                enumController.resetPaymentMethods();
            }
            return defaultValue;
        }
    }

    private EnumController newController() {
        EnumController c = new EnumController();
        c.configOptionApplicationController = new ResettingConfigStub(c);
        return c;
    }

    @Test
    void patientDepositListSurvivesResetDuringLoad() {
        List<PaymentMethod> list = newController().getPaymentMethodsForPatientDeposit();
        assertNotNull(list);
        assertEquals(PaymentMethod.values().length, list.size());
    }

    @Test
    void patientDepositRefundAndCancelListsSurviveResetDuringLoad() {
        assertNotNull(newController().getPaymentMethodsForPatientDepositRefund());
        assertNotNull(newController().getPaymentMethodsForPatientDepositCancel());
    }

    @Test
    void opdBillingListSurvivesResetDuringLoad() {
        List<PaymentMethod> list = newController().getPaymentMethodsForOpdBilling();
        assertNotNull(list);
        assertEquals(PaymentMethod.values().length, list.size());
    }
}

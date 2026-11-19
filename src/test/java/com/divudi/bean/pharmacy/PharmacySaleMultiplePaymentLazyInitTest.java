package com.divudi.bean.pharmacy;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.entity.PreBill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests verifying that PharmacySaleController1/2/3 lazily initialize
 * paymentMethodData when calculating remaining amounts or receiving remaining
 * amounts for multiple payment methods (#25347).
 */
public class PharmacySaleMultiplePaymentLazyInitTest {

    @Test
    @DisplayName("PharmacySaleController1: calculatRemainForMultiplePaymentTotal does not throw NPE when paymentMethodData is null (#25347)")
    void saleController1_calculatRemain_doesNotThrowNpeWhenPaymentMethodDataNull() {
        PharmacySaleController1 controller = new PharmacySaleController1();
        PreBill preBill = new PreBill();
        preBill.setNetTotal(500.0);
        preBill.setTotal(500.0);
        controller.setPreBill(preBill);
        controller.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

        double remain = assertDoesNotThrow(() -> controller.calculatRemainForMultiplePaymentTotal());
        assertEquals(500.0, remain, 0.001);
        assertNotNull(controller.getPaymentMethodData());
    }

    @Test
    @DisplayName("PharmacySaleController1: recieveRemainAmountAutomatically handles empty payment methods without NPE or IndexOutOfBounds (#25347)")
    void saleController1_recieveRemain_handlesEmptyPaymentMethodsSafely() {
        PharmacySaleController1 controller = new PharmacySaleController1();
        PreBill preBill = new PreBill();
        preBill.setNetTotal(750.0);
        preBill.setTotal(750.0);
        controller.setPreBill(preBill);
        controller.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

        assertDoesNotThrow(() -> controller.recieveRemainAmountAutomatically());
    }

    @Test
    @DisplayName("PharmacySaleController1: multiple payment components calculate correct remainder (#25347)")
    void saleController1_multiplePaymentComponents_calculatesCorrectRemainder() {
        PharmacySaleController1 controller = new PharmacySaleController1();
        PreBill preBill = new PreBill();
        preBill.setNetTotal(1000.0);
        preBill.setTotal(1000.0);
        controller.setPreBill(preBill);
        controller.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

        ComponentDetail cashComponent = new ComponentDetail();
        cashComponent.setPaymentMethod(PaymentMethod.Cash);
        cashComponent.getPaymentMethodData().getCash().setTotalValue(400.0);
        controller.getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().add(cashComponent);

        ComponentDetail cardComponent = new ComponentDetail();
        cardComponent.setPaymentMethod(PaymentMethod.Card);
        cardComponent.getPaymentMethodData().getCreditCard().setTotalValue(0.0);
        controller.getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().add(cardComponent);

        double remain = controller.calculatRemainForMultiplePaymentTotal();
        assertEquals(600.0, remain, 0.001);

        controller.recieveRemainAmountAutomatically();
        assertEquals(600.0, cardComponent.getPaymentMethodData().getCreditCard().getTotalValue(), 0.001);
    }

    @Test
    @DisplayName("PharmacySaleController2: calculatRemain and recieveRemain handle null paymentMethodData (#25347)")
    void saleController2_handlesNullPaymentMethodData() {
        PharmacySaleController2 controller = new PharmacySaleController2();
        PreBill preBill = new PreBill();
        preBill.setNetTotal(1200.0);
        preBill.setTotal(1200.0);
        controller.setPreBill(preBill);
        controller.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

        double remain = assertDoesNotThrow(() -> controller.calculatRemainForMultiplePaymentTotal());
        assertEquals(1200.0, remain, 0.001);

        assertDoesNotThrow(() -> controller.recieveRemainAmountAutomatically());
    }

    @Test
    @DisplayName("PharmacySaleController3: calculatRemain and recieveRemain handle null paymentMethodData (#25347)")
    void saleController3_handlesNullPaymentMethodData() {
        PharmacySaleController3 controller = new PharmacySaleController3();
        PreBill preBill = new PreBill();
        preBill.setNetTotal(850.0);
        preBill.setTotal(850.0);
        controller.setPreBill(preBill);
        controller.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

        double remain = assertDoesNotThrow(() -> controller.calculatRemainForMultiplePaymentTotal());
        assertEquals(850.0, remain, 0.001);

        assertDoesNotThrow(() -> controller.recieveRemainAmountAutomatically());
    }
}

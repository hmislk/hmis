package com.divudi.core.data.dataStructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChargeItemTotalTest {

    @Test
    public void netTotalSubtractsItemAndChargeTypeDiscounts() {
        ChargeItemTotal cit = new ChargeItemTotal();
        cit.setTotal(1000.0);
        cit.setDiscount(100.0);            // level 1 aggregate
        cit.setChargeTypeDiscount(50.0);   // level 2 manual
        assertEquals(850.0, cit.getNetTotal(), 0.001);
    }

    @Test
    public void chargeTypeDiscountDefaultsToZeroKeepingOldBehaviour() {
        ChargeItemTotal cit = new ChargeItemTotal();
        cit.setTotal(1000.0);
        cit.setDiscount(100.0);
        assertEquals(0.0, cit.getChargeTypeDiscount(), 0.001);
        assertEquals(900.0, cit.getNetTotal(), 0.001);
    }
}

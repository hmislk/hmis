package com.divudi.bean.pharmacy;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PreBill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for prefilling the credit company on PharmacyPreSettleController
 * from the counter PreBill (#25325).
 */
public class PharmacyPreSettleCreditCompanyPrefillTest {

    private PharmacyPreSettleController controller;
    private Institution creditCompany;
    private Institution alternateCompany;

    @BeforeEach
    public void setUp() {
        controller = new PharmacyPreSettleController();

        creditCompany = new Institution();
        creditCompany.setName("Sri Lanka Insurance");

        alternateCompany = new Institution();
        alternateCompany.setName("Ceylinco General Insurance");
    }

    @Test
    public void testSetPreBillPrefillsCreditCompanyFromPreBill() {
        PreBill preBill = new PreBill();
        preBill.setPaymentMethod(PaymentMethod.Credit);
        preBill.setCreditCompany(creditCompany);

        controller.setPreBill(preBill);

        assertNotNull(controller.getPaymentMethodData());
        assertNotNull(controller.getPaymentMethodData().getCredit());
        assertEquals(creditCompany, controller.getPaymentMethodData().getCredit().getInstitution());
    }

    @Test
    public void testSetPreBillPrefillsCreditCompanyFromToInstitutionFallback() {
        PreBill preBill = new PreBill();
        preBill.setPaymentMethod(PaymentMethod.Credit);
        preBill.setCreditCompany(null);
        preBill.setToInstitution(creditCompany);

        controller.setPreBill(preBill);

        assertNotNull(controller.getPaymentMethodData());
        assertNotNull(controller.getPaymentMethodData().getCredit());
        assertEquals(creditCompany, controller.getPaymentMethodData().getCredit().getInstitution());
    }

    @Test
    public void testSetPreBillWithoutCreditCompanyLeavesInstitutionNull() {
        PreBill preBill = new PreBill();
        preBill.setPaymentMethod(PaymentMethod.Credit);
        preBill.setCreditCompany(null);
        preBill.setToInstitution(null);

        controller.setPreBill(preBill);

        assertNotNull(controller.getPaymentMethodData());
        assertNotNull(controller.getPaymentMethodData().getCredit());
        assertNull(controller.getPaymentMethodData().getCredit().getInstitution());
    }

    @Test
    public void testCashierCanModifyPrefilledCreditCompany() {
        PreBill preBill = new PreBill();
        preBill.setPaymentMethod(PaymentMethod.Credit);
        preBill.setCreditCompany(creditCompany);

        controller.setPreBill(preBill);
        assertEquals(creditCompany, controller.getPaymentMethodData().getCredit().getInstitution());

        // Cashier changes the company in the UI
        controller.getPaymentMethodData().getCredit().setInstitution(alternateCompany);
        assertEquals(alternateCompany, controller.getPaymentMethodData().getCredit().getInstitution());
    }

    @Test
    public void testCheckAndUpdateBalanceSynchronizesCreditCompanyWhenNull() {
        PreBill preBill = new PreBill();
        preBill.setPaymentMethod(PaymentMethod.Credit);
        preBill.setCreditCompany(creditCompany);

        controller.setPreBill(preBill);
        // Clear institution explicitly
        controller.getPaymentMethodData().getCredit().setInstitution(null);

        controller.checkAndUpdateBalance();

        assertEquals(creditCompany, controller.getPaymentMethodData().getCredit().getInstitution());
    }

    @Test
    public void testCheckAndUpdateBalancePreservesModifiedCompany() {
        PreBill preBill = new PreBill();
        preBill.setPaymentMethod(PaymentMethod.Credit);
        preBill.setCreditCompany(creditCompany);

        controller.setPreBill(preBill);
        controller.getPaymentMethodData().getCredit().setInstitution(alternateCompany);

        controller.checkAndUpdateBalance();

        // Modified company must not be overwritten
        assertEquals(alternateCompany, controller.getPaymentMethodData().getCredit().getInstitution());
    }
}

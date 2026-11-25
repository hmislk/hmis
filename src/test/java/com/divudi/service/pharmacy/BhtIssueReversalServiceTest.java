package com.divudi.service.pharmacy;

import com.divudi.core.entity.Bill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BhtIssueReversalServiceTest {

    @Test
    public void reversalBillCarriesNegatedMoneyFields() {
        Bill original = new Bill();
        original.setNetTotal(100.0);
        original.setTotal(110.0);
        original.setGrantTotal(110.0);
        original.setDiscount(10.0);
        original.setMargin(5.0);
        original.setServiceCharge(3.0);
        original.setStaffFee(2.0);
        original.setHospitalFee(4.0);

        Bill reversal = new Bill();
        BhtIssueReversalService.applyNegatedBillValues(reversal, original);

        assertEquals(-100.0, reversal.getNetTotal(), 0.0001);
        assertEquals(-110.0, reversal.getTotal(), 0.0001);
        assertEquals(-110.0, reversal.getGrantTotal(), 0.0001);
        assertEquals(-10.0, reversal.getDiscount(), 0.0001);
        assertEquals(-5.0, reversal.getMargin(), 0.0001);
        assertEquals(-3.0, reversal.getServiceCharge(), 0.0001);
        assertEquals(-2.0, reversal.getStaffFee(), 0.0001);
        assertEquals(-4.0, reversal.getHospitalFee(), 0.0001);
        assertEquals(100.0, original.getNetTotal(), 0.0001);
    }

    @Test
    public void isReversalOnlyWhenOriginalPointsBackAsCancelledBy() {
        Bill original = new Bill();
        Bill reversal = new Bill();
        reversal.setReferenceBill(original);
        assertFalse(BhtIssueReversalService.isReversal(reversal));
        assertFalse(BhtIssueReversalService.isReversal(original));
        assertFalse(BhtIssueReversalService.isReversal(null));

        original.setCancelledBill(reversal);
        assertTrue(BhtIssueReversalService.isReversal(reversal));
        assertFalse(BhtIssueReversalService.isReversal(original));
    }
}

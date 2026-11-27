package com.divudi.core.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How the parts of an inpatient final bill's paid amount add up (#26152).
 */
public class FinalBillPaidFiguresTest {

    private static final double EPS = 0.0001;

    @Test
    public void companyReceiptThenPatientPaymentKeepsBoth() {
        // Company paid 700, patient then paid 300: both count, whatever the order.
        FinalBillPaidFigures f = FinalBillPaidFigures.of(300, 0, 0, 700);
        assertEquals(1000, f.getPaidAmount(), EPS);
        assertEquals(300, f.getSettledAmountByPatient(), EPS);
        assertEquals(700, f.getSettledAmountBySponsor(), EPS);
        assertEquals(0, f.balanceFor(1000), EPS);
    }

    @Test
    public void refundAndCancelAreSignedInThePatientFigure() {
        // Deposit 5000, refund -1000, cancel of a 500 payment -500 (a 500 payment is +500).
        FinalBillPaidFigures f = FinalBillPaidFigures.of(5000 - 1000 + 500 - 500, 0, 0, 0);
        assertEquals(4000, f.getPaidAmount(), EPS);
        assertEquals(4000, f.getSettledAmountByPatient(), EPS);
        assertEquals(0, f.getSettledAmountBySponsor(), EPS);
    }

    @Test
    public void cancelledCompanyReceiptIsTakenOut() {
        // Receipt 700 and its cancel -700 net to zero.
        FinalBillPaidFigures f = FinalBillPaidFigures.of(200, 0, 0, 700 - 700);
        assertEquals(200, f.getPaidAmount(), EPS);
        assertEquals(0, f.getSettledAmountBySponsor(), EPS);
    }

    @Test
    public void postFinalPaymentsCountAsPatientMoney() {
        FinalBillPaidFigures f = FinalBillPaidFigures.of(1000, 0, 250, 500);
        assertEquals(1750, f.getPaidAmount(), EPS);
        assertEquals(1250, f.getSettledAmountByPatient(), EPS);
        assertEquals(500, f.getSettledAmountBySponsor(), EPS);
        assertEquals(250, f.balanceFor(2000), EPS);
    }

    @Test
    public void creditMethodInwardPaymentCountsInPaidButNotAsPatientMoney() {
        // Old flows recorded company money as an inward payment with the Credit method.
        FinalBillPaidFigures f = FinalBillPaidFigures.of(1200, 400, 0, 0);
        assertEquals(1200, f.getPaidAmount(), EPS);
        assertEquals(800, f.getSettledAmountByPatient(), EPS);
        assertEquals(0, f.getSettledAmountBySponsor(), EPS);
    }

    @Test
    public void sameInputsGiveSameFiguresEveryTime() {
        FinalBillPaidFigures a = FinalBillPaidFigures.of(100, 0, 50, 25);
        FinalBillPaidFigures b = FinalBillPaidFigures.of(100, 0, 50, 25);
        assertEquals(a.getPaidAmount(), b.getPaidAmount(), EPS);
        assertEquals(a.getSettledAmountByPatient(), b.getSettledAmountByPatient(), EPS);
    }
}

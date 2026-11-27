package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the split of Multiple Payment Methods bills by payment rows (#26211).
 */
class MultiplePaymentSplitServiceTest {

    private static final double EPS = 0.0001;

    private static Map<PaymentMethod, Double> paid(double cash, double card) {
        Map<PaymentMethod, Double> m = new EnumMap<>(PaymentMethod.class);
        m.put(PaymentMethod.Cash, cash);
        m.put(PaymentMethod.Card, card);
        return m;
    }

    @Test
    void splitValueUsesShareOfEachPayment() {
        Map<PaymentMethod, Double> r = MultiplePaymentSplitService.splitValue(1000, paid(400, 600));
        assertEquals(400, r.get(PaymentMethod.Cash), EPS);
        assertEquals(600, r.get(PaymentMethod.Card), EPS);
    }

    @Test
    void splitValuePartsAddUpToTheValue() {
        Map<PaymentMethod, Double> r = MultiplePaymentSplitService.splitValue(333.33, paid(1, 2));
        assertEquals(333.33, r.get(PaymentMethod.Cash) + r.get(PaymentMethod.Card), EPS);
    }

    @Test
    void splitValueKeepsNegativeSign() {
        Map<PaymentMethod, Double> r = MultiplePaymentSplitService.splitValue(-1000, paid(400, 600));
        assertEquals(-400, r.get(PaymentMethod.Cash), EPS);
        assertEquals(-600, r.get(PaymentMethod.Card), EPS);
    }

    @Test
    void negativePaymentsOfCancellationGiveSamePositiveShare() {
        Map<PaymentMethod, Double> r = MultiplePaymentSplitService.splitValue(-2900, paid(-1500, -1400));
        assertEquals(-1500, r.get(PaymentMethod.Cash), EPS);
        assertEquals(-1400, r.get(PaymentMethod.Card), EPS);
    }

    @Test
    void splitValueWithNoPaymentsIsEmpty() {
        assertTrue(MultiplePaymentSplitService.splitValue(100, null).isEmpty());
        assertTrue(MultiplePaymentSplitService.splitValue(100, new EnumMap<>(PaymentMethod.class)).isEmpty());
        assertTrue(MultiplePaymentSplitService.splitValue(100, paid(0, 0)).isEmpty());
    }

    @Test
    void billsOfOneBatchShareTheSameRatio() {
        Map<Long, Map<PaymentMethod, Double>> byBatch = new HashMap<>();
        byBatch.put(10L, paid(1500, 1400));
        Map<Long, Map<PaymentMethod, Double>> r = MultiplePaymentSplitService.splitBills(Arrays.asList(
                new MultiplePaymentSplitService.BillValue(1L, 1000, 10L, null),
                new MultiplePaymentSplitService.BillValue(2L, 1900, 10L, null)), byBatch);
        double ratio1 = r.get(1L).get(PaymentMethod.Cash) / 1000;
        double ratio2 = r.get(2L).get(PaymentMethod.Cash) / 1900;
        assertEquals(ratio1, ratio2, EPS);
        Map<PaymentMethod, Double> totals = MultiplePaymentSplitService.totalByMethod(r);
        assertEquals(1500, totals.get(PaymentMethod.Cash), EPS);
        assertEquals(1400, totals.get(PaymentMethod.Card), EPS);
    }

    @Test
    void cancelledBillUsesForwardBatchFirst() {
        Map<Long, Map<PaymentMethod, Double>> byBatch = new HashMap<>();
        byBatch.put(10L, paid(1500, 1400));
        byBatch.put(20L, paid(-2900, 0));
        Map<Long, Map<PaymentMethod, Double>> r = MultiplePaymentSplitService.splitBills(Arrays.asList(
                new MultiplePaymentSplitService.BillValue(5L, -2900, 10L, 20L)), byBatch);
        assertEquals(-2900, r.get(5L).get(PaymentMethod.Cash), EPS);
        assertEquals(0, r.get(5L).get(PaymentMethod.Card), EPS);
    }

    @Test
    void cancelledBillFallsBackToBackwardBatch() {
        Map<Long, Map<PaymentMethod, Double>> byBatch = new HashMap<>();
        byBatch.put(10L, paid(1500, 1400));
        Map<Long, Map<PaymentMethod, Double>> r = MultiplePaymentSplitService.splitBills(Arrays.asList(
                new MultiplePaymentSplitService.BillValue(5L, -2900, 10L, 20L)), byBatch);
        assertEquals(-1500, r.get(5L).get(PaymentMethod.Cash), EPS);
        assertEquals(-1400, r.get(5L).get(PaymentMethod.Card), EPS);
    }

    @Test
    void billWithoutBatchPaymentsIsLeftOut() {
        Map<Long, Map<PaymentMethod, Double>> r = MultiplePaymentSplitService.splitBills(Arrays.asList(
                new MultiplePaymentSplitService.BillValue(7L, 500, null, null),
                new MultiplePaymentSplitService.BillValue(8L, 500, 99L, null)), new HashMap<>());
        assertFalse(r.containsKey(7L));
        assertNull(r.get(8L));
        assertTrue(MultiplePaymentSplitService.totalByMethod(r).isEmpty());
    }

    @Test
    void nonCashNonCardPartIsSplitOut() {
        Map<PaymentMethod, Double> p = new EnumMap<>(PaymentMethod.class);
        p.put(PaymentMethod.Cash, 400.0);
        p.put(PaymentMethod.PatientDeposit, 600.0);
        Map<PaymentMethod, Double> r = MultiplePaymentSplitService.splitValue(1000, p);
        assertEquals(400, r.get(PaymentMethod.Cash), EPS);
        assertEquals(600, r.get(PaymentMethod.PatientDeposit), EPS);
    }

    @Test
    void includedShareCountsOnlyIncludedMethods() {
        Map<PaymentMethod, Double> p = new EnumMap<>(PaymentMethod.class);
        p.put(PaymentMethod.Cash, 400.0);
        p.put(PaymentMethod.PatientDeposit, 600.0);
        Map<PaymentMethod, Double> oneUnit = MultiplePaymentSplitService.splitValue(1, p);
        Set<PaymentMethod> included = EnumSet.of(PaymentMethod.Cash, PaymentMethod.Card, PaymentMethod.Cheque, PaymentMethod.Slip);
        assertEquals(0.4, MultiplePaymentSplitService.includedShare(oneUnit, included), EPS);
        assertEquals(0.0, MultiplePaymentSplitService.includedShare(oneUnit, EnumSet.of(PaymentMethod.Card)), EPS);
    }

    @Test
    void scaledSumUsesShareOfEachBill() {
        Map<Long, Double> values = new HashMap<>();
        values.put(1L, 1000.0);
        values.put(2L, 500.0);
        values.put(3L, 700.0);
        Map<Long, Double> shares = new HashMap<>();
        shares.put(1L, 0.4);
        shares.put(2L, 1.0);
        // bill 3 has no share: nothing was paid by an included method
        assertEquals(900.0, MultiplePaymentSplitService.scaledSum(values, shares), EPS);
    }

    @Test
    void cancelRefundedByDifferentMethodsUsesCancellationBatch() {
        Map<Long, Map<PaymentMethod, Double>> byBatch = new HashMap<>();
        byBatch.put(10L, paid(1500, 1400));
        Map<PaymentMethod, Double> refund = new EnumMap<>(PaymentMethod.class);
        refund.put(PaymentMethod.Cash, -1000.0);
        refund.put(PaymentMethod.PatientDeposit, -1900.0);
        byBatch.put(20L, refund);
        Map<Long, Map<PaymentMethod, Double>> r = MultiplePaymentSplitService.splitBills(Arrays.asList(
                new MultiplePaymentSplitService.BillValue(5L, -2900, 10L, 20L)), byBatch);
        assertEquals(-1000, r.get(5L).get(PaymentMethod.Cash), EPS);
        assertEquals(-1900, r.get(5L).get(PaymentMethod.PatientDeposit), EPS);
        assertNull(r.get(5L).get(PaymentMethod.Card));
        Set<PaymentMethod> included = EnumSet.of(PaymentMethod.Cash, PaymentMethod.Card, PaymentMethod.Cheque, PaymentMethod.Slip);
        // the share is the size of the included part, whatever the sign
        Map<PaymentMethod, Double> oneUnit = MultiplePaymentSplitService.splitValue(-1, refund);
        assertEquals(1000.0 / 2900.0, Math.abs(MultiplePaymentSplitService.includedShare(oneUnit, included)), EPS);
    }
}

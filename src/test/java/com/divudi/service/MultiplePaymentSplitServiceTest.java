package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
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
}

package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
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

    @Test
    void singleBillCancelNetsToZeroPerMethod() {
        // Original bill 1750 paid Cash 700 + Card 1050 (rows on the batch bill 10).
        // Its cancel bill 5 has its own rows, refunded by the same methods.
        Map<Long, Map<PaymentMethod, Double>> byBill = new HashMap<>();
        byBill.put(10L, paid(700, 1050));
        byBill.put(5L, paid(-700, -1050));
        Map<Long, Map<PaymentMethod, Double>> r = MultiplePaymentSplitService.splitBills(Arrays.asList(
                new MultiplePaymentSplitService.BillValue(1L, 1750, 10L, null),
                new MultiplePaymentSplitService.BillValue(5L, -1750, 10L, null)), byBill);
        Map<PaymentMethod, Double> totals = MultiplePaymentSplitService.totalByMethod(r);
        assertEquals(0, totals.get(PaymentMethod.Cash), EPS);
        assertEquals(0, totals.get(PaymentMethod.Card), EPS);
        assertEquals(-700, r.get(5L).get(PaymentMethod.Cash), EPS);
        assertEquals(-1050, r.get(5L).get(PaymentMethod.Card), EPS);
    }

    @Test
    void amountsByShareGiveEachRowItsShare() {
        List<Double> a = MultiplePaymentSplitService.amountsByShare(1750, Arrays.asList(700.0, 1050.0));
        assertEquals(700, a.get(0), EPS);
        assertEquals(1050, a.get(1), EPS);
    }

    @Test
    void amountsByShareSplitOneBillOfABatch() {
        // Batch paid Cash 1500 + Card 1400 for two bills; the bill of 1000 gets its part of each row.
        List<Double> a = MultiplePaymentSplitService.amountsByShare(1000, Arrays.asList(1500.0, 1400.0));
        assertEquals(1000, a.get(0) + a.get(1), EPS);
        assertEquals(517.24, a.get(0), EPS);
    }

    @Test
    void amountsByShareAddUpAfterRounding() {
        List<Double> a = MultiplePaymentSplitService.amountsByShare(100, Arrays.asList(1.0, 1.0, 1.0));
        assertEquals(100, a.get(0) + a.get(1) + a.get(2), EPS);
    }

    @Test
    void amountsByShareUseAbsoluteValues() {
        List<Double> a = MultiplePaymentSplitService.amountsByShare(-1000, Arrays.asList(-400.0, -600.0));
        assertEquals(400, a.get(0), EPS);
        assertEquals(600, a.get(1), EPS);
    }
}

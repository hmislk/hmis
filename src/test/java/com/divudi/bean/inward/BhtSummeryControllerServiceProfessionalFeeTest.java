package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Staff;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for issue #23723: the Final Bill's ProfessionalCharge TOTAL
 * (InwardBeanController.calculateProfessionalCharges() +
 * calServiceBillItemsTotalByInwardChargeTypeBulk(), which includes Staff fees
 * on ProfessionalCharge-typed service items, e.g. an MRI "REPORTING - Dr X"
 * item) used to disagree with the per-doctor LIST (BhtSummeryController's
 * profesionallFee, InwardProfessional bills only) — a radiologist fee billed
 * on the service bill was missing from the doctor list.
 * <p>
 * These tests cover the pure, CDI-free pieces of the fix:
 * <ul>
 * <li>{@link BhtSummeryController#sumFeesByStaff(List)} — the per-staff
 * summation {@code addMergedDoctorFeesToProFees} uses to merge a
 * pro-fee-bill fee and a service-bill fee for the same doctor, and to net a
 * refunded service fee against its original.</li>
 * <li>{@link BhtSummeryController#professionalAdjustedTotal(double, List)} —
 * the delta-based adjusted-total helper that keeps the service-item part of
 * the ProfessionalCharge total when an editable (non-service-bill) fee is
 * adjusted.</li>
 * <li>{@link BillItem#getUnattributedProfessionalFeeValue()} — the remainder
 * of a bill item's adjustedValue not covered by any doctor in proFees.</li>
 * </ul>
 * Per-staff equality is id-based ({@code Staff.equals}/{@code hashCode}), so
 * distinct ids are used throughout to avoid accidentally merging two
 * different doctors (both otherwise default to id=null).
 */
class BhtSummeryControllerServiceProfessionalFeeTest {

    private static Staff staff(long id) {
        Staff s = new Staff();
        s.setId(id);
        return s;
    }

    private static BillFee fee(Staff staff, double feeValue, double feeAdjusted, int orderNo) {
        BillFee bf = new BillFee();
        bf.setStaff(staff);
        bf.setFeeValue(feeValue);
        bf.setFeeAdjusted(feeAdjusted);
        bf.setOrderNo(orderNo);
        return bf;
    }

    private static Bill billOfType(BillType type) {
        // BilledBill is a concrete Bill subclass already used elsewhere in the
        // inward tests/entities as a stand-in "real billed bill".
        BilledBill b = new BilledBill();
        b.setBillType(type);
        return b;
    }

    // ---- sumFeesByStaff -----------------------------------------------

    @Test
    void sumFeesByStaff_mergesProFeeBillFeeAndServiceBillFee_forSameDoctor() {
        Staff doctor = staff(1L);
        // A pro-fee-bill fee (InwardProfessional) and a service-bill fee
        // (InwardBill, e.g. the MRI "REPORTING - Dr X" item) for the same
        // doctor must be summed together.
        BillFee proFeeBillFee = fee(doctor, 10_000.0, 0.0, 0);
        BillFee serviceBillFee = fee(doctor, 7_500.0, 0.0, 1);

        Map<Staff, double[]> sums = BhtSummeryController.sumFeesByStaff(Arrays.asList(proFeeBillFee, serviceBillFee));

        assertEquals(1, sums.size());
        double[] sum = sums.get(doctor);
        assertEquals(17_500.0, sum[0], 0.001, "feeValue sum");
        assertEquals(17_500.0, sum[1], 0.001, "feeAdjusted sum (falls back to feeValue when feeAdjusted is 0)");
    }

    @Test
    void sumFeesByStaff_netsToZero_forPositiveFeeAndNegativeRefund() {
        Staff doctor = staff(2L);
        // A billed service fee fully reversed by a RefundBill's negative
        // contra fee for the same doctor nets to zero — the condition
        // addMergedDoctorFeesToProFees uses (abs < 0.005 on both sums) to
        // skip persisting/showing a doctor row with nothing left to pay.
        BillFee billed = fee(doctor, 5_000.0, 0.0, 0);
        BillFee refund = fee(doctor, -5_000.0, 0.0, 1);

        Map<Staff, double[]> sums = BhtSummeryController.sumFeesByStaff(Arrays.asList(billed, refund));

        double[] sum = sums.get(doctor);
        assertEquals(0.0, sum[0], 0.005);
        assertEquals(0.0, sum[1], 0.005);
    }

    @Test
    void sumFeesByStaff_keepsDoctorsSeparate() {
        Staff docA = staff(3L);
        Staff docB = staff(4L);
        BillFee feeA = fee(docA, 1_000.0, 0.0, 0);
        BillFee feeB = fee(docB, 2_000.0, 0.0, 1);

        Map<Staff, double[]> sums = BhtSummeryController.sumFeesByStaff(Arrays.asList(feeA, feeB));

        assertEquals(2, sums.size());
        assertEquals(1_000.0, sums.get(docA)[0], 0.001);
        assertEquals(2_000.0, sums.get(docB)[0], 0.001);
    }

    // ---- professionalAdjustedTotal --------------------------------------

    @Test
    void professionalAdjustedTotal_keepsServicePart_whenEditableFeeIsAdjusted() {
        // Total already includes both the InwardProfessional pro-fee part and
        // the service-item part (issue #23723's whole point); adjusting one
        // editable (non-service-bill) fee down by 2000 must only move the
        // total by that 2000, never dropping the untouched service part.
        BillFee editableFee = fee(staff(5L), 10_000.0, 8_000.0, 0);

        double result = BhtSummeryController.professionalAdjustedTotal(26_500.0, Collections.singletonList(editableFee));

        assertEquals(24_500.0, result, 0.001);
    }

    @Test
    void professionalAdjustedTotal_noEditableFees_returnsTotalUnchanged() {
        double result = BhtSummeryController.professionalAdjustedTotal(26_500.0, new ArrayList<>());
        assertEquals(26_500.0, result, 0.001);

        double resultNull = BhtSummeryController.professionalAdjustedTotal(26_500.0, null);
        assertEquals(26_500.0, resultNull, 0.001);
    }

    @Test
    void professionalAdjustedTotal_sumsMultipleEditableFeeDeltas() {
        BillFee feeA = fee(staff(6L), 10_000.0, 8_000.0, 0); // -2000
        BillFee feeB = fee(staff(7L), 5_000.0, 5_500.0, 1);  // +500

        double result = BhtSummeryController.professionalAdjustedTotal(26_500.0, Arrays.asList(feeA, feeB));

        assertEquals(25_000.0, result, 0.001);
    }

    // ---- isServiceBillFee -------------------------------------------------

    @Test
    void isServiceBillFee_trueForInwardBill_falseOtherwise() {
        BillFee onServiceBill = new BillFee();
        onServiceBill.setBill(billOfType(BillType.InwardBill));

        BillFee onProFeeBill = new BillFee();
        onProFeeBill.setBill(billOfType(BillType.InwardProfessional));

        BillFee noBill = new BillFee();

        assertTrue(BhtSummeryController.isServiceBillFee(onServiceBill));
        assertTrue(!BhtSummeryController.isServiceBillFee(onProFeeBill));
        assertTrue(!BhtSummeryController.isServiceBillFee(noBill));
    }

    // ---- BillItem.getUnattributedProfessionalFeeValue ----------------------

    @Test
    void getUnattributedProfessionalFeeValue_returnsRemainder() {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(26_500.0);
        List<BillFee> proFees = new ArrayList<>();
        proFees.add(feeWithAdjusted(10_000.0));
        proFees.add(feeWithAdjusted(7_500.0));
        proFees.add(feeWithAdjusted(1_500.0));
        bi.setProFees(proFees);

        assertEquals(7_500.0, bi.getUnattributedProfessionalFeeValue(), 0.001);
    }

    @Test
    void getUnattributedProfessionalFeeValue_returnsZero_whenBalanced() {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(19_000.0);
        List<BillFee> proFees = new ArrayList<>();
        proFees.add(feeWithAdjusted(12_000.0));
        proFees.add(feeWithAdjusted(7_000.0));
        bi.setProFees(proFees);

        assertEquals(0.0, bi.getUnattributedProfessionalFeeValue(), 0.001);
    }

    @Test
    void getUnattributedProfessionalFeeValue_nullSafe_whenProFeesNull() {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(500.0);
        bi.setProFees(null);

        assertEquals(500.0, bi.getUnattributedProfessionalFeeValue(), 0.001);
    }

    private static BillFee feeWithAdjusted(double feeAdjusted) {
        BillFee bf = new BillFee();
        bf.setFeeAdjusted(feeAdjusted);
        return bf;
    }
}

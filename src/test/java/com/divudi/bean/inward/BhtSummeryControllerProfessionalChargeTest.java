package com.divudi.bean.inward;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.Staff;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for issue #23723's ProfessionalCharge money fix: adjusting
 * one doctor's fee on the Final Bill must move the ProfessionalCharge
 * category's adjusted total by that fee's delta only, never by replacing it
 * with the sum of the doctor rows (which does not cover the whole category —
 * the category total also contains service items typed ProfessionalCharge,
 * plus timed/additional charges, that the per-doctor list never lists).
 * <p>
 * These tests cover the pure, CDI-free pieces of the fix:
 * <ul>
 * <li>{@link BhtSummeryController#sumFeesByStaff(List)} — the per-staff
 * summation {@code addMergedDoctorFeesToProFees} uses to merge a doctor's
 * individual fees into one, and to net a refunded fee against its
 * original.</li>
 * <li>{@link BhtSummeryController#professionalAdjustedTotal(double, List)} —
 * the delta-based adjusted-total helper that keeps the untouched part of the
 * ProfessionalCharge total (the part not covered by any doctor row) when a
 * doctor's fee is adjusted.</li>
 * <li>{@link BillItem#getUnattributedProfessionalFeeValue()} — the remainder
 * of a bill item's adjustedValue not covered by any doctor in proFees.</li>
 * </ul>
 * Per-staff equality is id-based ({@code Staff.equals}/{@code hashCode}), so
 * distinct ids are used throughout to avoid accidentally merging two
 * different doctors (both otherwise default to id=null).
 */
class BhtSummeryControllerProfessionalChargeTest {

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

    // ---- sumFeesByStaff -----------------------------------------------

    @Test
    void sumFeesByStaff_sumsTwoFees_forSameDoctor() {
        Staff doctor = staff(1L);
        // Two individual fees for the same doctor must be summed together.
        // The second fee's non-zero feeAdjusted differs from its feeValue, so
        // the effective-adjusted rule (feeAdjusted != 0 ? feeAdjusted :
        // feeValue) is exercised for both fees in the merge.
        BillFee feeOne = fee(doctor, 10_000.0, 0.0, 0);
        BillFee feeTwo = fee(doctor, 7_500.0, 6_000.0, 1);

        Map<Staff, double[]> sums = BhtSummeryController.sumFeesByStaff(Arrays.asList(feeOne, feeTwo));

        assertEquals(1, sums.size());
        double[] sum = sums.get(doctor);
        assertEquals(17_500.0, sum[0], 0.001, "feeValue sum");
        assertEquals(16_000.0, sum[1], 0.001, "adjusted sum uses feeTwo's feeAdjusted, not its feeValue");
    }

    @Test
    void sumFeesByStaff_netsToZero_forPositiveFeeAndNegativeRefund() {
        Staff doctor = staff(2L);
        // A billed fee fully reversed by a refund's negative contra fee for
        // the same doctor nets to zero — the condition
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
    void professionalAdjustedTotal_keepsUnattributedPart_whenADoctorFeeIsAdjusted() {
        // Total already includes both the doctor-row part and the
        // unattributed part (service items typed ProfessionalCharge, timed/
        // additional charges); adjusting one doctor's fee down by 2000 must
        // only move the total by that 2000, never dropping the untouched
        // unattributed part.
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

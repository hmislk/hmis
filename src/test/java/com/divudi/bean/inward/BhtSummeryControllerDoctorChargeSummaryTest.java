package com.divudi.bean.inward;

import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Staff;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the showProfessional-aware overload of
 * getSummaryOfDoctorChargers, added to fix a Hospital Copy total mismatch
 * found in CodeRabbit review of finalBillBundledCustom1.xhtml: with
 * "Display All Doctor Chargers as a One and Show Sum Value of a Doctor" on,
 * the aggregated row used to always include ProfessionalCharge even when
 * showProfessional was false (Hospital Copy), whose printed Total
 * (bill.hospitalFee) itself excludes ProfessionalCharge — so the printed
 * rows summed to more than the printed Total.
 */
class BhtSummeryControllerDoctorChargeSummaryTest {

    private static BillItem doctorItem(InwardChargeType type, double adjustedValue, Staff staff, double feeAdjusted) {
        BillItem bi = new BillItem();
        bi.setInwardChargeType(type);
        bi.setAdjustedValue(adjustedValue);
        BillFee fee = new BillFee();
        fee.setStaff(staff);
        fee.setFeeAdjusted(feeAdjusted);
        bi.setProFees(new ArrayList<>(Arrays.asList(fee)));
        return bi;
    }

    @Test
    void includesProfessionalCharge_whenIncludeProfessionalChargeTrue() {
        // Staff.equals()/hashCode() are id-based; distinct ids are required so the
        // method's staffFeeMap (keyed by Staff) treats these as two different people
        // instead of merging them (both default to id=null otherwise).
        Staff nurse = new Staff();
        nurse.setId(1L);
        Staff doctor = new Staff();
        doctor.setId(2L);
        List<BillItem> items = Arrays.asList(
                doctorItem(InwardChargeType.DoctorAndNurses, 500.0, nurse, 500.0),
                doctorItem(InwardChargeType.ProfessionalCharge, 1000.0, doctor, 1000.0));

        BhtSummeryController controller = new BhtSummeryController();
        List<BillItem> result = controller.getSummaryOfDoctorChargers(items, null, true);

        assertEquals(1, result.size());
        assertEquals(1500.0, result.get(0).getAdjustedValue());
        assertEquals(2, result.get(0).getProFees().size());
    }

    @Test
    void excludesProfessionalCharge_whenIncludeProfessionalChargeFalse() {
        Staff nurse = new Staff();
        nurse.setId(1L);
        Staff doctor = new Staff();
        doctor.setId(2L);
        List<BillItem> items = Arrays.asList(
                doctorItem(InwardChargeType.DoctorAndNurses, 500.0, nurse, 500.0),
                doctorItem(InwardChargeType.ProfessionalCharge, 1000.0, doctor, 1000.0));

        BhtSummeryController controller = new BhtSummeryController();
        List<BillItem> result = controller.getSummaryOfDoctorChargers(items, null, false);

        assertEquals(1, result.size());
        // Only the DoctorAndNurses amount — ProfessionalCharge's 1000.0 must not
        // be added to the aggregate's adjustedValue or its staff-fee breakdown,
        // otherwise this row would sum to more than a Hospital Copy's printed
        // Total (which uses bill.hospitalFee, excluding ProfessionalCharge).
        assertEquals(500.0, result.get(0).getAdjustedValue());
        assertEquals(1, result.get(0).getProFees().size());
        assertEquals(nurse, result.get(0).getProFees().get(0).getStaff());
    }

    @Test
    void twoArgOverload_stillIncludesProfessionalCharge_forExistingCallers() {
        Staff doctor = new Staff();
        List<BillItem> items = Arrays.asList(
                doctorItem(InwardChargeType.ProfessionalCharge, 1000.0, doctor, 1000.0));

        BhtSummeryController controller = new BhtSummeryController();
        // The pre-existing 2-arg overload (used by finalBill.xhtml) must behave
        // exactly as it did before this fix — no regression for existing callers.
        List<BillItem> result = controller.getSummaryOfDoctorChargers(items, null);

        assertEquals(1, result.size());
        assertEquals(1000.0, result.get(0).getAdjustedValue());
    }

    @Test
    void excludingProfessionalCharge_withOnlyProfessionalChargeItems_returnsEmpty() {
        Staff doctor = new Staff();
        List<BillItem> items = Arrays.asList(
                doctorItem(InwardChargeType.ProfessionalCharge, 1000.0, doctor, 1000.0));

        BhtSummeryController controller = new BhtSummeryController();
        List<BillItem> result = controller.getSummaryOfDoctorChargers(items, null, false);

        assertTrue(result.isEmpty());
    }
}

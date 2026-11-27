package com.divudi.service.inward;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Issue #17562: the sums taken from the fees in memory equal the sums the
 * database gave for the same fees.
 */
class InwardServiceBillSumsTest {

    private static BillFee fee(Double gross, double value, double margin, double discount, double vat) {
        BillFee f = new BillFee();
        f.setFeeGrossValue(gross);
        f.setFeeValue(value);
        f.setFeeMargin(margin);
        f.setFeeDiscount(discount);
        f.setFeeVat(vat);
        return f;
    }

    @Test
    void itemSumsAddUpTheFiveColumns() {
        List<BillFee> fees = new ArrayList<>();
        fees.add(fee(100.0, 90.0, 10.0, 20.0, 5.0));
        fees.add(fee(50.0, 50.0, 0.0, 0.0, 1.0));

        Object[] sums = InwardServiceBillSums.itemSums(fees);

        assertEquals(150.0, (Double) sums[0], 0.0001);
        assertEquals(140.0, (Double) sums[1], 0.0001);
        assertEquals(10.0, (Double) sums[2], 0.0001);
        assertEquals(20.0, (Double) sums[3], 0.0001);
        assertEquals(6.0, (Double) sums[4], 0.0001);
    }

    @Test
    void retiredFeesAndNullGrossAreLeftOutAsTheDatabaseSumDoes() {
        List<BillFee> fees = new ArrayList<>();
        BillFee retired = fee(999.0, 999.0, 0.0, 0.0, 0.0);
        retired.setRetired(true);
        fees.add(retired);
        fees.add(fee(null, 10.0, 0.0, 0.0, 0.0));

        Object[] sums = InwardServiceBillSums.itemSums(fees);

        assertNull(sums[0]);
        assertEquals(10.0, (Double) sums[1], 0.0001);
    }

    @Test
    void noFeesGivesNoSumsAndAZeroBill() {
        Object[] sums = InwardServiceBillSums.itemSums(new ArrayList<>());
        for (Object o : sums) {
            assertNull(o);
        }
        Bill bill = new BilledBill();
        bill.setTotal(5.0);
        InwardServiceBillSums.applyToBill(bill, new ArrayList<>());
        assertEquals(0.0, bill.getTotal());
        assertEquals(0.0, bill.getNetTotal());
        assertEquals(0.0, bill.getVatPlusNetTotal());
    }

    @Test
    void billTotalsComeFromTheFees() {
        List<BillFee> fees = new ArrayList<>();
        fees.add(fee(100.0, 90.0, 0.0, 10.0, 4.0));
        fees.add(fee(40.0, 40.0, 0.0, 0.0, 1.0));
        Bill bill = new BilledBill();

        InwardServiceBillSums.applyToBill(bill, fees);

        assertEquals(140.0, bill.getTotal(), 0.0001);
        assertEquals(130.0, bill.getNetTotal(), 0.0001);
        assertEquals(5.0, bill.getVat(), 0.0001);
        assertEquals(135.0, bill.getVatPlusNetTotal(), 0.0001);
    }

    // Issue #26242: a reused item already holds the fee; the append adds it once.
    @Test
    void appendOnceKeepsTheFeeOnceWhenTheItemAlreadyHoldsIt() {
        BillFee f = fee(100.0, 90.0, 10.0, 20.0, 0.0);
        List<BillFee> list = new ArrayList<>();
        list.add(f);

        assertEquals(false, InwardServiceBillSums.appendOnce(list, f));

        assertEquals(1, list.size());
        double hospitalFee = 0;
        double margin = 0;
        for (BillFee bf : list) {
            hospitalFee += bf.getFeeValue();
            margin += bf.getFeeMargin();
        }
        assertEquals(90.0, hospitalFee, 0.0001);
        assertEquals(10.0, margin, 0.0001);
    }

    @Test
    void appendOnceReplacesTheHeldCopyWithThePreparedOneOnTheSameId() {
        BillFee held = fee(100.0, 90.0, 10.0, 0.0, 0.0);
        held.setId(7L);
        BillFee prepared = fee(100.0, 90.0, 10.0, 0.0, 0.0);
        prepared.setId(7L);
        List<BillFee> list = new ArrayList<>();
        list.add(held);

        assertEquals(true, InwardServiceBillSums.appendOnce(list, prepared));

        assertEquals(1, list.size());
        org.junit.jupiter.api.Assertions.assertSame(prepared, list.get(0));
    }

    @Test
    void appendOnceAddsNewAndDifferentFees() {
        List<BillFee> list = new ArrayList<>();
        assertEquals(true, InwardServiceBillSums.appendOnce(list, fee(1.0, 1.0, 0, 0, 0)));
        assertEquals(true, InwardServiceBillSums.appendOnce(list, fee(1.0, 1.0, 0, 0, 0)));
        BillFee a = fee(1.0, 1.0, 0, 0, 0);
        a.setId(1L);
        BillFee b = fee(1.0, 1.0, 0, 0, 0);
        b.setId(2L);
        assertEquals(true, InwardServiceBillSums.appendOnce(list, a));
        assertEquals(true, InwardServiceBillSums.appendOnce(list, b));
        assertEquals(4, list.size());
    }

    // Issue #26273: the earlier bill is set again from the fees left on it.
    private static Bill staleBill() {
        Bill bill = new BilledBill();
        bill.setTotal(500.0);
        bill.setNetTotal(450.0);
        bill.setGrantTotal(500.0);
        bill.setBillTotal(450.0);
        bill.setMargin(30.0);
        bill.setServiceCharge(30.0);
        bill.setDiscount(50.0);
        bill.setVat(7.0);
        bill.setVatPlusNetTotal(457.0);
        bill.setStaffFee(100.0);
        bill.setPerformInstitutionFee(350.0);
        return bill;
    }

    @Test
    void earlierBillWithNoFeesLeftGoesToZero() {
        Bill bill = staleBill();

        InwardServiceBillSums.applyAllToBillFromFees(bill, new ArrayList<>());

        assertEquals(0.0, bill.getTotal(), 0.0001);
        assertEquals(0.0, bill.getNetTotal(), 0.0001);
        assertEquals(0.0, bill.getGrantTotal(), 0.0001);
        assertEquals(0.0, bill.getBillTotal(), 0.0001);
        assertEquals(0.0, bill.getMargin(), 0.0001);
        assertEquals(0.0, bill.getServiceCharge(), 0.0001);
        assertEquals(0.0, bill.getDiscount(), 0.0001);
        assertEquals(0.0, bill.getVat(), 0.0001);
        assertEquals(0.0, bill.getVatPlusNetTotal(), 0.0001);
        assertEquals(0.0, bill.getStaffFee(), 0.0001);
        assertEquals(0.0, bill.getPerformInstitutionFee(), 0.0001);
    }

    @Test
    void earlierBillKeepsTheSumsOfTheFeesLeftAndSkipsRetiredOnes() {
        Bill bill = staleBill();
        List<BillFee> fees = new ArrayList<>();
        fees.add(fee(100.0, 90.0, 10.0, 20.0, 5.0));
        fees.add(fee(null, 10.0, 0.0, 0.0, 0.0));
        BillFee retired = fee(999.0, 999.0, 9.0, 9.0, 9.0);
        retired.setRetired(true);
        fees.add(retired);

        InwardServiceBillSums.applyAllToBillFromFees(bill, fees);

        assertEquals(100.0, bill.getTotal(), 0.0001);
        assertEquals(100.0, bill.getGrantTotal(), 0.0001);
        assertEquals(100.0, bill.getNetTotal(), 0.0001);
        assertEquals(100.0, bill.getBillTotal(), 0.0001);
        assertEquals(10.0, bill.getMargin(), 0.0001);
        assertEquals(10.0, bill.getServiceCharge(), 0.0001);
        assertEquals(20.0, bill.getDiscount(), 0.0001);
        assertEquals(5.0, bill.getVat(), 0.0001);
        assertEquals(105.0, bill.getVatPlusNetTotal(), 0.0001);
        assertEquals(0.0, bill.getStaffFee(), 0.0001);
        assertEquals(100.0, bill.getPerformInstitutionFee(), 0.0001);
    }
}

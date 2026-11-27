package com.divudi.service.inward;

import com.divudi.core.data.FeeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Sums of the bill fees of one settle, taken from the fees in memory (#17562).
 * The settle used to ask the database for these right after saving the fees; the
 * fees are new, so the database holds exactly the fees in the list and the answer
 * is the same without the queries. Free of CDI so it can be unit tested.
 */
final class InwardServiceBillSums {

    private InwardServiceBillSums() {
    }

    /**
     * The five sums of a bill item's fees, in the order gross, net, margin,
     * discount, vat, as {@code BillBeanController.applyBillFeeSumsToBillItem}
     * reads them. Retired fees do not count; a null gross value counts as no row,
     * as the database SUM treats it.
     */
    static Object[] itemSums(List<BillFee> fees) {
        Double gross = null;
        double net = 0;
        double margin = 0;
        double discount = 0;
        double vat = 0;
        boolean any = false;
        if (fees != null) {
            for (BillFee bf : distinct(fees)) {
                if (bf == null || bf.isRetired()) {
                    continue;
                }
                any = true;
                if (bf.getFeeGrossValue() != null) {
                    gross = (gross == null ? 0.0 : gross) + bf.getFeeGrossValue();
                }
                net += bf.getFeeValue();
                margin += bf.getFeeMargin();
                discount += bf.getFeeDiscount();
                vat += bf.getFeeVat();
            }
        }
        if (!any) {
            return new Object[]{null, null, null, null, null};
        }
        return new Object[]{gross, net, margin, discount, vat};
    }

    /**
     * The fees with each one once. A bill item reused from an earlier visit (a
     * surgery's second visit) already holds the fees the entry brings, so the list
     * can hold the same fee, or the same saved row, twice. The database summed each
     * row once, so the sums do the same: a fee counts once by identity, and once by
     * id when it has one.
     */
    static List<BillFee> distinct(List<BillFee> fees) {
        List<BillFee> out = new ArrayList<>();
        if (fees == null) {
            return out;
        }
        Set<BillFee> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Long> seenIds = new HashSet<>();
        for (BillFee bf : fees) {
            if (bf == null || !seen.add(bf)) {
                continue;
            }
            if (bf.getId() != null && !seenIds.add(bf.getId())) {
                continue;
            }
            out.add(bf);
        }
        return out;
    }

    /**
     * Puts the fee in the list once. The same object already in the list stays as
     * it is. A different object with the same non-null id (a reused item holds a
     * copy of the saved fee; the entry brings the copy the writer has prepared
     * for the new bill) replaces the held copy, so the list keeps the prepared
     * one, as it did when both were listed and the prepared one was saved last.
     * Counting the fee twice made the loops that sum the list (hospital fee,
     * margin) double it (#26242).
     *
     * @return true when the list changed
     */
    static boolean appendOnce(List<BillFee> list, BillFee bf) {
        for (int i = 0; i < list.size(); i++) {
            BillFee existing = list.get(i);
            if (existing == bf) {
                return false;
            }
            if (existing != null && bf.getId() != null && bf.getId().equals(existing.getId())) {
                list.set(i, bf);
                return true;
            }
        }
        list.add(bf);
        return true;
    }

    /**
     * Sets total, net total, vat and vat plus net total on the bill from its
     * fees, as {@code BillBeanController.updateBillByBillFee} does from the
     * database. A missing sum is zero.
     */
    static void applyToBill(Bill bill, List<BillFee> fees) {
        Object[] sums = itemSums(fees);
        bill.setTotal(sumAt(sums, 0));
        bill.setNetTotal(sumAt(sums, 1));
        bill.setVat(sumAt(sums, 4));
        bill.setVatPlusNetTotal(bill.getNetTotal() + bill.getVat());
    }

    /**
     * Sets the bill-level figures of a service bill from the fees it still holds,
     * the way {@code BillBeanController.calculateBillItems} sets them for a new
     * bill (its non-partial-payment branch): staff fee, performing institution
     * fee, margin, service charge, gross and grant total, net total, bill total,
     * discount, vat and vat plus net total. Used for the earlier bill of a
     * surgery after its items moved to a new bill, so the money is counted on one
     * bill only (#26273). No fees gives zero in every figure. Retired fees do not
     * count; a null gross value counts as zero.
     */
    static void applyAllToBillFromFees(Bill bill, List<BillFee> fees) {
        double staff = 0.0;
        double institution = 0.0;
        double gross = 0.0;
        double discount = 0.0;
        double net = 0.0;
        double vat = 0.0;
        double margin = 0.0;
        for (BillFee bf : distinct(fees)) {
            if (bf.isRetired()) {
                continue;
            }
            gross += bf.getFeeGrossValue() == null ? 0.0 : bf.getFeeGrossValue();
            net += bf.getFeeValue();
            discount += bf.getFeeDiscount();
            vat += bf.getFeeVat();
            margin += bf.getFeeMargin();
            if (bf.getFee() != null && bf.getFee().getFeeType() == FeeType.Staff) {
                staff += bf.getFeeValue();
            } else {
                institution += bf.getFeeValue();
            }
        }
        bill.setStaffFee(staff);
        bill.setPerformInstitutionFee(institution);
        bill.setMargin(margin);
        bill.setServiceCharge(margin);
        bill.setGrantTotal(gross);
        bill.setTotal(gross);
        bill.setNetTotal(net);
        bill.setBillTotal(net);
        bill.setDiscount(discount);
        bill.setVat(vat);
        bill.setVatPlusNetTotal(vat + net);
    }

    private static double sumAt(Object[] sums, int index) {
        return sums[index] == null ? 0.0 : ((Number) sums[index]).doubleValue();
    }
}

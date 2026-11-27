package com.divudi.service.inward;

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
     * Adds the fee to the list only when the list does not hold it yet, by
     * identity or by the same non-null id. A bill item reused from an earlier
     * visit already holds the fee the entry brings; adding it again made the
     * loops that sum the list (hospital fee, margin) count it twice (#26242).
     *
     * @return true when the fee was added
     */
    static boolean appendOnce(List<BillFee> list, BillFee bf) {
        for (BillFee existing : list) {
            if (existing == bf
                    || (existing != null && bf.getId() != null && bf.getId().equals(existing.getId()))) {
                return false;
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

    private static double sumAt(Object[] sums, int index) {
        return sums[index] == null ? 0.0 : ((Number) sums[index]).doubleValue();
    }
}

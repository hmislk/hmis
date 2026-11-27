package com.divudi.service.inward;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import java.util.List;

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
            for (BillFee bf : fees) {
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

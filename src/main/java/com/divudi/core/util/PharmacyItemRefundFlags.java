package com.divudi.core.util;

import com.divudi.core.entity.BillItem;

/**
 * Rules for the refund flags of the original sale bill item when an item-only
 * pharmacy return is paid out, or that payment is cancelled (issue #24485).
 */
public final class PharmacyItemRefundFlags {

    private PharmacyItemRefundFlags() {
    }

    /**
     * Marks the original sale item as refunded.
     *
     * @return true when a flag changed
     */
    public static boolean markRefunded(BillItem original) {
        if (original == null || (original.isRefunded() && original.isBillItemRefunded())) {
            return false;
        }
        original.setRefunded(true);
        original.setBillItemRefunded(true);
        return true;
    }

    /**
     * Clears the refund flags after a refund payment was cancelled, unless
     * another active refund still covers the same original item.
     *
     * @param original the original sale item
     * @param otherActiveRefunds number of other refund items, on refund bills
     * that are not cancelled, that cover the same original item
     * @return true when a flag changed
     */
    public static boolean clearAfterRefundCancelled(BillItem original, long otherActiveRefunds) {
        if (original == null || otherActiveRefunds > 0) {
            return false;
        }
        if (!original.isRefunded() && !original.isBillItemRefunded()) {
            return false;
        }
        original.setRefunded(false);
        original.setBillItemRefunded(false);
        return true;
    }

    /**
     * Label rule for Search Sale Bill Items: refunded when the pre-bill item or
     * its billed-bill copy is flagged. The sale bill's refunded flag means
     * "returned", so it is not part of this rule.
     */
    public static boolean isShownRefunded(Boolean itemRefunded, Boolean linkedItemRefunded) {
        return Boolean.TRUE.equals(itemRefunded) || Boolean.TRUE.equals(linkedItemRefunded);
    }
}

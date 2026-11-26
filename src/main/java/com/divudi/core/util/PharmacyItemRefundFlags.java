package com.divudi.core.util;

import com.divudi.core.data.dto.PharmacySaleBillItemSearchDTO;
import com.divudi.core.entity.BillItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Key of a sale bill line: the billed bill and the item. The cashier
     * settle flow copies pre-bill items into the billed bill without an item
     * level link, so the line is matched by bill and item.
     */
    public static String lineKey(Long billId, Long itemId) {
        return billId + ":" + itemId;
    }

    /**
     * Turns the label on for search rows whose billed-bill item (same bill and
     * item) is flagged refunded. Rows already shown refunded stay so.
     *
     * @param rows search rows, may be null
     * @param refundedLines {@link #lineKey} values of the flagged billed items
     */
    public static void applyBilledItemFlags(List<PharmacySaleBillItemSearchDTO> rows, Set<String> refundedLines) {
        if (rows == null || refundedLines == null || refundedLines.isEmpty()) {
            return;
        }
        for (PharmacySaleBillItemSearchDTO row : rows) {
            if (row != null && !Boolean.TRUE.equals(row.getRefunded())
                    && refundedLines.contains(lineKey(row.getBillId(), row.getItemId()))) {
                row.setRefunded(true);
            }
        }
    }

    /**
     * Collects {@link #lineKey} values from rows of (bill id, item id).
     */
    public static Set<String> lineKeys(List<?> idPairs) {
        Set<String> keys = new HashSet<>();
        if (idPairs == null) {
            return keys;
        }
        for (Object o : idPairs) {
            if (o instanceof Object[]) {
                Object[] a = (Object[]) o;
                if (a.length >= 2 && a[0] != null && a[1] != null) {
                    keys.add(lineKey(((Number) a[0]).longValue(), ((Number) a[1]).longValue()));
                }
            }
        }
        return keys;
    }
}

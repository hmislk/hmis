package com.divudi.core.util;

import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.inward.InpatientPackageItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InpatientPackagePricing {

    private InpatientPackagePricing() {
    }

    public static double calculateTotalPrice(double fixedRoomCharge, List<InpatientPackageItem> components) {
        double total = fixedRoomCharge;
        if (components == null) {
            return total;
        }
        for (InpatientPackageItem component : components) {
            if (component == null || component.isRetired()) {
                continue;
            }
            if (component.getFixedPrice() != null) {
                total += component.getFixedPrice();
            }
        }
        return total;
    }

    public static double calculateTotalPrice(Map<String, Double> chargeTypeAmounts, List<InpatientPackageItem> components) {
        double chargeTypeTotal = 0.0;
        if (chargeTypeAmounts != null) {
            for (Double v : chargeTypeAmounts.values()) {
                if (v != null) {
                    chargeTypeTotal += v;
                }
            }
        }
        return calculateTotalPrice(chargeTypeTotal, components);
    }

    /**
     * The package's price broken down per InwardChargeType: chargeTypeAmounts'
     * entries (converted from their String enum-name keys) plus each
     * component's contribution under its own resolved charge type, summed
     * together when a category appears in both. Unknown/unparseable
     * chargeTypeAmounts keys and null values are skipped rather than thrown.
     */
    public static Map<InwardChargeType, Double> calculateChargeTypeAllocations(
            Map<String, Double> chargeTypeAmounts, Map<InwardChargeType, Double> componentAllocations) {
        Map<InwardChargeType, Double> result = new HashMap<>();
        if (chargeTypeAmounts != null) {
            for (Map.Entry<String, Double> e : chargeTypeAmounts.entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                InwardChargeType type;
                try {
                    type = InwardChargeType.valueOf(e.getKey());
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                result.merge(type, e.getValue(), Double::sum);
            }
        }
        if (componentAllocations != null) {
            for (Map.Entry<InwardChargeType, Double> e : componentAllocations.entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                result.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        return result;
    }

    /**
     * Whole-package netting: sums the admission's real, actual totals across
     * only the charge types the package covers (present in
     * perCategoryAllocations), and returns how much that sum exceeds
     * packageTotal — or 0.0 if it doesn't exceed it. A category's own
     * overspend can be absorbed by another category's underspend, since only
     * the combined total is compared against the package's combined total.
     */
    public static double calculatePackageExcess(
            Map<InwardChargeType, Double> perCategoryAllocations,
            Map<InwardChargeType, Double> actualTotalsByType,
            double packageTotal) {
        double actualCovered = 0.0;
        if (perCategoryAllocations != null && actualTotalsByType != null) {
            for (InwardChargeType type : perCategoryAllocations.keySet()) {
                Double actual = actualTotalsByType.get(type);
                if (actual != null) {
                    actualCovered += actual;
                }
            }
        }
        double excess = actualCovered - packageTotal;
        return excess > 0.0 ? excess : 0.0;
    }
}

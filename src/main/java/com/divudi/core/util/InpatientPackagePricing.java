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

    /**
     * A package admission's per-charge-type figures after package pricing,
     * as the interim bill shows them (BhtSummeryController.applyPackagePricingIfApplicable):
     * each covered charge type is held at its allocated amount with no margin
     * or discount of its own, and a PackageExcessCharges line carries any usage
     * above the package price.
     */
    public static final class PackageAdjustment {

        private final Map<InwardChargeType, Double> gross;
        private final Map<InwardChargeType, Double> droppedMargin;
        private final Map<InwardChargeType, Double> droppedDiscount;

        private PackageAdjustment(Map<InwardChargeType, Double> gross,
                Map<InwardChargeType, Double> droppedMargin, Map<InwardChargeType, Double> droppedDiscount) {
            this.gross = gross;
            this.droppedMargin = droppedMargin;
            this.droppedDiscount = droppedDiscount;
        }

        /** Gross per charge type after package pricing. */
        public Map<InwardChargeType, Double> getGross() {
            return gross;
        }

        /** Margin of the covered charge types, no longer charged. */
        public Map<InwardChargeType, Double> getDroppedMargin() {
            return droppedMargin;
        }

        /** Discount of the covered charge types, no longer given. */
        public Map<InwardChargeType, Double> getDroppedDiscount() {
            return droppedDiscount;
        }
    }

    /**
     * Applies package pricing to one admission's figures.
     *
     * @param gross          gross per charge type (before margin and discount)
     * @param margin         margin (service charge) per charge type
     * @param discount       discount per charge type
     * @param allocations    the package's price per covered charge type
     * @param packageTotal   the package's total price
     */
    public static PackageAdjustment applyPackage(Map<InwardChargeType, Double> gross,
            Map<InwardChargeType, Double> margin, Map<InwardChargeType, Double> discount,
            Map<InwardChargeType, Double> allocations, double packageTotal) {
        Map<InwardChargeType, Double> adjusted = new HashMap<>();
        if (gross != null) {
            adjusted.putAll(gross);
        }
        Map<InwardChargeType, Double> droppedMargin = new HashMap<>();
        Map<InwardChargeType, Double> droppedDiscount = new HashMap<>();
        if (allocations == null || allocations.isEmpty()) {
            return new PackageAdjustment(adjusted, droppedMargin, droppedDiscount);
        }

        // Actual usage per covered type is gross + margin, the interim bill's
        // row total (its discount is shown separately).
        Map<InwardChargeType, Double> actual = new HashMap<>();
        for (InwardChargeType type : allocations.keySet()) {
            double g = adjusted.getOrDefault(type, 0.0);
            double m = margin != null ? margin.getOrDefault(type, 0.0) : 0.0;
            actual.put(type, g + m);
        }
        double excess = calculatePackageExcess(allocations, actual, packageTotal);

        for (Map.Entry<InwardChargeType, Double> e : allocations.entrySet()) {
            InwardChargeType type = e.getKey();
            adjusted.put(type, e.getValue());
            if (margin != null && margin.containsKey(type)) {
                droppedMargin.put(type, margin.get(type));
            }
            if (discount != null && discount.containsKey(type)) {
                droppedDiscount.put(type, discount.get(type));
            }
        }
        if (excess > 0.0) {
            adjusted.merge(InwardChargeType.PackageExcessCharges, excess, Double::sum);
        }
        return new PackageAdjustment(adjusted, droppedMargin, droppedDiscount);
    }
}

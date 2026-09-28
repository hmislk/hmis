package com.divudi.core.util;

import com.divudi.core.entity.inward.InpatientPackageItem;
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
}

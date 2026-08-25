package com.divudi.core.util;

import com.divudi.core.entity.inward.InpatientPackageItem;
import java.util.List;

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
}

package com.divudi.core.util;

import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.entity.inward.InpatientPackageItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InpatientPackagePricingTest {

    private InpatientPackageItem component(double fixedPrice, boolean retired) {
        InpatientPackageItem item = new InpatientPackageItem();
        item.setComponentType(InpatientPackageComponentType.SERVICE);
        item.setFixedPrice(fixedPrice);
        item.setRetired(retired);
        return item;
    }

    @Test
    void sumsRoomChargeAndAllActiveComponents() {
        List<InpatientPackageItem> components = new ArrayList<>();
        components.add(component(5000.0, false));
        components.add(component(2500.0, false));

        double total = InpatientPackagePricing.calculateTotalPrice(50000.0, components);

        assertEquals(57500.0, total, 0.001);
    }

    @Test
    void excludesRetiredComponents() {
        List<InpatientPackageItem> components = new ArrayList<>();
        components.add(component(5000.0, false));
        components.add(component(9999.0, true));

        double total = InpatientPackagePricing.calculateTotalPrice(50000.0, components);

        assertEquals(55000.0, total, 0.001);
    }

    @Test
    void handlesNullComponentListAndNullFixedPrice() {
        assertEquals(50000.0, InpatientPackagePricing.calculateTotalPrice(50000.0, null), 0.001);

        List<InpatientPackageItem> components = new ArrayList<>();
        InpatientPackageItem noPrice = component(0.0, false);
        noPrice.setFixedPrice(null);
        components.add(noPrice);

        assertEquals(50000.0, InpatientPackagePricing.calculateTotalPrice(50000.0, components), 0.001);
    }

    @Test
    void sumsMultipleChargeTypeAmountsAndAllActiveComponents() {
        Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
        chargeTypeAmounts.put("RoomCharges", 30000.0);
        chargeTypeAmounts.put("NursingCharges", 15000.0);
        chargeTypeAmounts.put("MOCharges", 5000.0);

        List<InpatientPackageItem> components = new ArrayList<>();
        components.add(component(5000.0, false));
        components.add(component(2500.0, false));

        double total = InpatientPackagePricing.calculateTotalPrice(chargeTypeAmounts, components);

        assertEquals(57500.0, total, 0.001);
    }

    @Test
    void skipsNullChargeTypeAmountEntryWithoutThrowing() {
        Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
        chargeTypeAmounts.put("RoomCharges", 30000.0);
        chargeTypeAmounts.put("NursingCharges", null);

        List<InpatientPackageItem> components = new ArrayList<>();
        components.add(component(5000.0, false));

        double total = InpatientPackagePricing.calculateTotalPrice(chargeTypeAmounts, components);

        assertEquals(35000.0, total, 0.001);
    }

    @Test
    void handlesNullChargeTypeAmountsAndNullComponentList() {
        assertEquals(0.0, InpatientPackagePricing.calculateTotalPrice((Map<String, Double>) null, null), 0.001);
    }
}

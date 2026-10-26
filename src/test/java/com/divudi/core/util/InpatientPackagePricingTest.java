package com.divudi.core.util;

import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.inward.InpatientPackageItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void chargeTypeAllocationsSumsMapAndComponentsPerCategory() {
        Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
        chargeTypeAmounts.put("RoomCharges", 30000.0);
        chargeTypeAmounts.put("NursingCharges", 15000.0);

        Map<InwardChargeType, Double> componentAllocations = new LinkedHashMap<>();
        componentAllocations.put(InwardChargeType.RoomCharges, 5000.0); // same category as chargeTypeAmounts
        componentAllocations.put(InwardChargeType.ProfessionalCharge, 8000.0); // new category

        Map<InwardChargeType, Double> result = InpatientPackagePricing.calculateChargeTypeAllocations(chargeTypeAmounts, componentAllocations);

        assertEquals(35000.0, result.get(InwardChargeType.RoomCharges), 0.001);
        assertEquals(15000.0, result.get(InwardChargeType.NursingCharges), 0.001);
        assertEquals(8000.0, result.get(InwardChargeType.ProfessionalCharge), 0.001);
    }

    @Test
    void chargeTypeAllocationsSkipsUnknownKeysAndNulls() {
        Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
        chargeTypeAmounts.put("RoomCharges", 30000.0);
        chargeTypeAmounts.put("NotARealEnumName", 999.0);
        chargeTypeAmounts.put("NursingCharges", null);

        Map<InwardChargeType, Double> result = InpatientPackagePricing.calculateChargeTypeAllocations(chargeTypeAmounts, null);

        assertEquals(1, result.size());
        assertEquals(30000.0, result.get(InwardChargeType.RoomCharges), 0.001);
    }

    @Test
    void chargeTypeAllocationsHandlesBothNullInputs() {
        Map<InwardChargeType, Double> result = InpatientPackagePricing.calculateChargeTypeAllocations(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void packageExcessIsZeroWhenActualWithinPackageTotal() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);
        allocations.put(InwardChargeType.NursingCharges, 15000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 20000.0);
        actual.put(InwardChargeType.NursingCharges, 10000.0);

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 45000.0);

        assertEquals(0.0, excess, 0.001);
    }

    @Test
    void packageExcessIsWholePackageNetOverage() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);
        allocations.put(InwardChargeType.NursingCharges, 15000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 40000.0); // 10,000 over its own allocation
        actual.put(InwardChargeType.NursingCharges, 5000.0); // 10,000 under its own allocation

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 45000.0);

        // Whole-package netting: 40000+5000=45000 actual vs 45000 package total -> no excess,
        // even though Room Charges alone overspent by 10,000.
        assertEquals(0.0, excess, 0.001);
    }

    @Test
    void packageExcessNetsAcrossCategoriesBeforeReportingOverage() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);
        allocations.put(InwardChargeType.NursingCharges, 15000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 50000.0);
        actual.put(InwardChargeType.NursingCharges, 5000.0);

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 45000.0);

        // 50000+5000=55000 actual vs 45000 package total -> 10,000 net excess.
        assertEquals(10000.0, excess, 0.001);
    }

    @Test
    void packageExcessIgnoresActualCategoriesThePackageDoesNotCover() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 30000.0);
        actual.put(InwardChargeType.Medicine, 999999.0); // not package-covered, must not count

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 30000.0);

        assertEquals(0.0, excess, 0.001);
    }
}

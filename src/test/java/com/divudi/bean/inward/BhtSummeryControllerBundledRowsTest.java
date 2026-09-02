package com.divudi.bean.inward;

import com.divudi.core.data.dto.FinalBillPrintRowDTO;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BhtSummeryControllerBundledRowsTest {

    private static BillItem chargeItem(InwardChargeType type, double adjustedValue) {
        BillItem bi = new BillItem();
        bi.setInwardChargeType(type);
        bi.setAdjustedValue(adjustedValue);
        return bi;
    }

    /** Every charge type present in groupByType with an empty group, own order. */
    private static Map<InwardChargeType, String> emptyGroups(InwardChargeType... types) {
        Map<InwardChargeType, String> m = new EnumMap<>(InwardChargeType.class);
        for (InwardChargeType t : types) {
            m.put(t, "");
        }
        return m;
    }

    private static Map<InwardChargeType, Integer> sequentialOrders(InwardChargeType... types) {
        Map<InwardChargeType, Integer> m = new EnumMap<>(InwardChargeType.class);
        int order = 10;
        for (InwardChargeType t : types) {
            m.put(t, order);
            order += 10;
        }
        return m;
    }

    private static Map<InwardChargeType, String> defaultLabels(InwardChargeType... types) {
        Map<InwardChargeType, String> m = new EnumMap<>(InwardChargeType.class);
        for (InwardChargeType t : types) {
            m.put(t, t.getLabel());
        }
        return m;
    }

    @Test
    void groupsMatchingChargeTypesIntoOneSummedRow() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.RoomCharges, 3000.0));
        items.add(chargeItem(InwardChargeType.MealCharges, 750.0));
        items.add(chargeItem(InwardChargeType.CT, 500.0));

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.RoomCharges, InwardChargeType.MealCharges, InwardChargeType.CT);
        groups.put(InwardChargeType.RoomCharges, "Room Charges");
        groups.put(InwardChargeType.MealCharges, "Room Charges");
        groups.put(InwardChargeType.CT, "Room Charges");
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.RoomCharges, InwardChargeType.MealCharges, InwardChargeType.CT);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.RoomCharges, InwardChargeType.MealCharges, InwardChargeType.CT);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Room Charges", rows.get(0).getLabel());
        assertEquals(4250.0, rows.get(0).getAmount());
    }

    @Test
    void leavesUngroupedChargeTypeAsIndividualRowWithResolvedLabel() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = new EnumMap<>(InwardChargeType.class);
        labels.put(InwardChargeType.Laboratory, "Lab Charges");

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Lab Charges", rows.get(0).getLabel());
        assertEquals(1200.0, rows.get(0).getAmount());
    }

    @Test
    void skipsChargeTypeNotPresentInGroupMap() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.ProfessionalCharge, 26000.0));
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));

        // ProfessionalCharge deliberately absent — mirrors how Task 5 excludes it.
        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Laboratory Charges", rows.get(0).getLabel());
    }

    @Test
    void filtersOutRowsThatSumToZero() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.OxygenCharges, 0.0));

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.OxygenCharges);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.OxygenCharges);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.OxygenCharges);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertTrue(rows.isEmpty());
    }

    @Test
    void sortsByOrder_groupedRowUsesMinimumOrderAmongMembers() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));   // order 10, ungrouped
        items.add(chargeItem(InwardChargeType.RoomCharges, 3000.0));  // order 20, grouped
        items.add(chargeItem(InwardChargeType.MealCharges, 750.0));   // order 999, grouped (higher than RoomCharges)

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory, InwardChargeType.RoomCharges, InwardChargeType.MealCharges);
        groups.put(InwardChargeType.RoomCharges, "Room Charges");
        groups.put(InwardChargeType.MealCharges, "Room Charges");

        Map<InwardChargeType, Integer> orders = new EnumMap<>(InwardChargeType.class);
        orders.put(InwardChargeType.Laboratory, 10);
        orders.put(InwardChargeType.RoomCharges, 20);
        orders.put(InwardChargeType.MealCharges, 999);

        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory, InwardChargeType.RoomCharges, InwardChargeType.MealCharges);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        // Laboratory (order 10) first, then Room Charges group (min(20, 999) = 20)
        assertEquals(2, rows.size());
        assertEquals("Laboratory Charges", rows.get(0).getLabel());
        assertEquals("Room Charges", rows.get(1).getLabel());
        assertEquals(3750.0, rows.get(1).getAmount());
    }

    @Test
    void treatsBlankGroupTextAsUngrouped() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));

        Map<InwardChargeType, String> groups = new EnumMap<>(InwardChargeType.class);
        groups.put(InwardChargeType.Laboratory, "   "); // whitespace-only, must trim to empty
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Laboratory Charges", rows.get(0).getLabel());
    }

    @Test
    void returnsEmptyList_whenBillItemsIsNull() {
        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(null, groups, orders, labels);

        assertTrue(rows.isEmpty());
    }
}

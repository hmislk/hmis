package com.divudi.bean.inward;

import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PatientItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Charge-type reads are grouped once per run and empty charge types are skipped (#17562). */
class InwardChargeTypeRunDataTest {

    private static Object[] row(Object value, Object type) {
        return new Object[]{value, type};
    }

    @Test
    void groupsByChargeTypeKeepingRowOrderAndDroppingRowsWithoutType() {
        List<Object[]> rows = Arrays.asList(
                row("a", InwardChargeType.Medicine),
                row("b", InwardChargeType.RoomCharges),
                row("c", InwardChargeType.Medicine),
                row("d", null));
        Map<InwardChargeType, List<Object>> grouped = InwardChargeTypeRunData.groupByChargeType(rows, 0, 1);
        assertEquals(Arrays.<Object>asList("a", "c"), grouped.get(InwardChargeType.Medicine));
        assertEquals(Arrays.<Object>asList("b"), grouped.get(InwardChargeType.RoomCharges));
        assertEquals(2, grouped.size());
    }

    @Test
    void wholeRowIsKeptWhenValueColumnIsNegative() {
        Object[] r = {7L, 1.0, 2.0, null, InwardChargeType.RoomCharges};
        Map<InwardChargeType, List<Object[]>> grouped = InwardChargeTypeRunData.groupByChargeType(Collections.singletonList(r), -1, 4);
        assertSame(r, grouped.get(InwardChargeType.RoomCharges).get(0));
    }

    @Test
    void chargeTypeWithNoRowsIsEmptyAndReadsGiveEmptyLists() {
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setServiceFees(new ArrayList<>());
        data.setServiceItems(new ArrayList<>());
        data.setTimedItemRows(new ArrayList<>());
        data.setTimedPatientItems(new ArrayList<>());
        data.addOtherRowTypes(new ArrayList<>());
        for (InwardChargeType t : InwardChargeType.values()) {
            assertTrue(data.isEmpty(t));
        }
        assertTrue(data.serviceFees(InwardChargeType.RoomCharges).isEmpty());
        assertTrue(data.timedItemRows(InwardChargeType.RoomCharges).isEmpty());
        assertTrue(data.timedPatientItems(InwardChargeType.RoomCharges).isEmpty());
        assertTrue(data.serviceItems(InwardChargeType.RoomCharges).isEmpty());
    }

    @Test
    void anyKindOfRowMakesTheChargeTypeNonEmptyOnlyForThatType() {
        BillFee fee = new BillFee();
        BillItem item = new BillItem();
        PatientItem pi = new PatientItem();
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setServiceFees(Collections.singletonList(row(fee, InwardChargeType.Medicine)));
        data.setServiceItems(Collections.singletonList(row(item, InwardChargeType.RoomCharges)));
        data.setTimedItemRows(Collections.singletonList(new Object[]{1L, 1.0, 1.0, null, InwardChargeType.MOCharges}));
        data.setTimedPatientItems(Collections.singletonList(row(pi, InwardChargeType.LinenCharges)));
        data.addOtherRowTypes(Collections.singletonList(new Object[]{InwardChargeType.NursingCharges, 3L}));

        assertFalse(data.isEmpty(InwardChargeType.Medicine));
        assertFalse(data.isEmpty(InwardChargeType.RoomCharges));
        assertFalse(data.isEmpty(InwardChargeType.MOCharges));
        assertFalse(data.isEmpty(InwardChargeType.LinenCharges));
        assertFalse(data.isEmpty(InwardChargeType.NursingCharges));
        assertTrue(data.isEmpty(InwardChargeType.AdministrationCharge));
        assertSame(fee, data.serviceFees(InwardChargeType.Medicine).get(0));
        assertSame(pi, data.timedPatientItems(InwardChargeType.LinenCharges).get(0));
        assertTrue(data.serviceFees(InwardChargeType.RoomCharges).isEmpty());
    }

    @Test
    void updateIdListsAreBuiltFromTheGroupedRowsAndSplitInChunks() {
        List<Object[]> all = new ArrayList<>();
        for (long id = 1; id <= 1203; id++) {
            all.add(new Object[]{id, 100.0, 0.0, null, InwardChargeType.RoomCharges});
        }
        all.add(new Object[]{5000L, 50.0, 0.0, null, InwardChargeType.MOCharges});
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setTimedItemRows(all);

        BhtSummeryController.TimedDiscountPlan plan
                = BhtSummeryController.planTimedServiceDiscount(data.timedItemRows(InwardChargeType.RoomCharges), 10, null, null);
        assertEquals(1203, plan.ids.size());
        assertEquals(1203 * 10.0, plan.total, 1e-9);
        List<List<Long>> chunks = InwardBeanController.chunkIds(plan.ids, InwardBeanController.BULK_UPDATE_ID_CHUNK);
        assertEquals(3, chunks.size());
        assertEquals(500, chunks.get(0).size());
        assertEquals(203, chunks.get(2).size());
        assertEquals(Long.valueOf(1203), chunks.get(2).get(202));
        assertEquals(Arrays.asList(5000L),
                BhtSummeryController.planTimedServiceDiscount(data.timedItemRows(InwardChargeType.MOCharges), 10, null, null).ids);
        assertTrue(BhtSummeryController.planTimedServiceDiscount(data.timedItemRows(InwardChargeType.Medicine), 10, null, null).ids.isEmpty());
    }

    // Issue #26272: the final-bill discount step must see no-type lines under the
    // same charge type the interim bill counts them under.
    private static final InwardChargeType DEFAULT = InwardChargeType.OperationTheatreCharges;

    private static Object[] row3(Object value, Object itemType, Object billItemType) {
        return new Object[]{value, itemType, billItemType};
    }

    @Test
    void lineWithNoItemTypeCountsUnderStoredTypeElseDefault() {
        BillItem noType = new BillItem();
        BillItem stored = new BillItem();
        BillItem typed = new BillItem();
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setServiceItems(Arrays.asList(
                row3(noType, null, null),
                row3(stored, null, InwardChargeType.RoomCharges),
                row3(typed, InwardChargeType.Medicine, InwardChargeType.RoomCharges)), DEFAULT);
        assertSame(noType, data.serviceItems(DEFAULT).get(0));
        assertSame(stored, data.serviceItems(InwardChargeType.RoomCharges).get(0));
        assertSame(typed, data.serviceItems(InwardChargeType.Medicine).get(0));
        assertEquals(1, data.serviceItems(DEFAULT).size());
        assertFalse(data.isEmpty(DEFAULT));
    }

    @Test
    void noTypeLineIsFoundWithTypedLinesOfTheSameChargeType() {
        BillItem typed = new BillItem();
        BillItem noType = new BillItem();
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setServiceItems(Arrays.asList(
                row3(typed, DEFAULT, null),
                row3(noType, null, null)), DEFAULT);
        assertEquals(Arrays.asList(typed, noType), data.serviceItems(DEFAULT));
    }

    @Test
    void noTypeFeeIsFoundAndChargeTypeIsNotEmpty() {
        BillFee fee = new BillFee();
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setServiceFees(Collections.singletonList(row3(fee, null, null)), DEFAULT);
        data.addBillItemRowTypes(Collections.singletonList(new Object[]{null, null, 1L}), DEFAULT);
        assertSame(fee, data.serviceFees(DEFAULT).get(0));
        assertFalse(data.isEmpty(DEFAULT));
        assertTrue(data.isEmpty(InwardChargeType.RoomCharges));
    }

    @Test
    void discountOfNoTypeLineIsSeenByTheDiscountStep() {
        // gross 1000, settle discount 100: the discount step reads the line from the
        // run data, so the interim net 900 and the final net agree.
        BillItem line = new BillItem();
        line.setGrossValue(1000.0);
        line.setDiscount(100.0);
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setServiceItems(Collections.singletonList(row3(line, null, null)), DEFAULT);
        double discount = 0;
        for (BillItem b : data.serviceItems(DEFAULT)) {
            discount += b.getDiscount();
        }
        assertEquals(100.0, discount, 0.0001);
    }

    @Test
    void withoutADefaultAnUntypedRowIsStillDropped() {
        InwardChargeTypeRunData data = new InwardChargeTypeRunData();
        data.setServiceItems(Collections.singletonList(row3(new BillItem(), null, null)), null);
        for (InwardChargeType t : InwardChargeType.values()) {
            assertTrue(data.serviceItems(t).isEmpty());
        }
    }
}

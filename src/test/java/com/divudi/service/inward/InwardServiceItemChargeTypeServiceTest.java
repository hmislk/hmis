package com.divudi.service.inward;

import com.divudi.core.data.inward.InwardChargeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Issue #26272: inward service lines whose item has no charge type. */
class InwardServiceItemChargeTypeServiceTest {

    private static final InwardChargeType DEFAULT = InwardChargeType.OperationTheatreCharges;

    @Test
    void itemTypeIsNeverMoved() {
        assertEquals(InwardChargeType.Medicine,
                InwardServiceItemChargeTypeService.resolve(InwardChargeType.Medicine, InwardChargeType.RoomCharges, DEFAULT));
    }

    @Test
    void billItemTypeUsedWhenItemHasNone() {
        assertEquals(InwardChargeType.RoomCharges,
                InwardServiceItemChargeTypeService.resolve(null, InwardChargeType.RoomCharges, DEFAULT));
    }

    @Test
    void defaultUsedWhenNeitherHasType() {
        assertEquals(DEFAULT, InwardServiceItemChargeTypeService.resolve(null, null, DEFAULT));
    }

    @Test
    void parseFallsBackToDefault() {
        assertEquals(DEFAULT, InwardServiceItemChargeTypeService.parse(null));
        assertEquals(DEFAULT, InwardServiceItemChargeTypeService.parse("  "));
        assertEquals(DEFAULT, InwardServiceItemChargeTypeService.parse("NoSuchType"));
        assertEquals(InwardChargeType.RoomCharges, InwardServiceItemChargeTypeService.parse(" RoomCharges "));
    }

    @Test
    void jpqlConditionForDefaultTypeAlsoMatchesNullBillItemType() {
        String forDefault = InwardServiceItemChargeTypeService.lineCountsUnderJpql("s", "inw", true);
        assertEquals("(s.item.inwardChargeType = :inw or (s.item.inwardChargeType is null and"
                + " (s.inwardChargeType is null or s.inwardChargeType = :inw)))", forDefault);
        String other = InwardServiceItemChargeTypeService.lineCountsUnderJpql("s", "inw", false);
        assertEquals("(s.item.inwardChargeType = :inw or (s.item.inwardChargeType is null and"
                + " s.inwardChargeType = :inw))", other);
    }
}

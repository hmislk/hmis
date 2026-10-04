package com.divudi.core.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvcCardLayoutTest {

    @Test
    void defaultLayoutHasSensibleCardSizeAndAllSlots() {
        PvcCardLayout layout = PvcCardLayout.defaultLayout();

        assertEquals(85.6, layout.getWidthMm());
        assertEquals(54.0, layout.getHeightMm());
        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_NAME));
        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_BARCODE));
        assertTrue(layout.getSlots().get(PvcCardLayout.SLOT_BARCODE).isVisible());
        assertFalse(layout.getSlots().get(PvcCardLayout.SLOT_ADDRESS).isVisible());
        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_PHN));
        assertTrue(layout.getSlots().get(PvcCardLayout.SLOT_PHN).isVisible());
    }

    @Test
    void fromJsonNullOrBlankFallsBackToDefault() {
        PvcCardLayout fromNull = PvcCardLayout.fromJson(null);
        PvcCardLayout fromBlank = PvcCardLayout.fromJson("   ");

        assertEquals(85.6, fromNull.getWidthMm());
        assertEquals(85.6, fromBlank.getWidthMm());
    }

    @Test
    void fromJsonMalformedFallsBackToDefaultInsteadOfThrowing() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{not valid json");

        assertEquals(85.6, layout.getWidthMm());
        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_NAME));
    }

    @Test
    void fromJsonRoundTripsCustomValues() {
        PvcCardLayout original = PvcCardLayout.defaultLayout();
        original.setWidthMm(90.0);
        original.getSlots().get(PvcCardLayout.SLOT_NAME).setLeftMm(12.5);
        original.getSlots().get(PvcCardLayout.SLOT_ADDRESS).setVisible(true);

        PvcCardLayout restored = PvcCardLayout.fromJson(original.toJson());

        assertEquals(90.0, restored.getWidthMm());
        assertEquals(12.5, restored.getSlots().get(PvcCardLayout.SLOT_NAME).getLeftMm());
        assertTrue(restored.getSlots().get(PvcCardLayout.SLOT_ADDRESS).isVisible());
    }

    @Test
    void fromJsonFillsAnyMissingSlotWithDefault() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{\"widthMm\":85.6,\"heightMm\":54.0,\"marginMm\":2.0,\"slots\":{}}");

        assertTrue(layout.getSlots().containsKey(PvcCardLayout.SLOT_BARCODE));
        assertTrue(layout.getSlots().get(PvcCardLayout.SLOT_BARCODE).isVisible());
    }
}

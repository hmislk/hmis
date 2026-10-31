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

    @Test
    void fromJsonRejectsZeroOrNegativeDimensions() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{\"widthMm\":0,\"heightMm\":-5,\"marginMm\":-1,\"slots\":{}}");

        assertEquals(85.6, layout.getWidthMm());
        assertEquals(54.0, layout.getHeightMm());
        assertEquals(2.0, layout.getMarginMm());
    }

    @Test
    void layoutWithoutPageKeysIsNotPlacedOnPage() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{\"widthMm\":85.6,\"heightMm\":54,\"marginMm\":0,\"slots\":{}}");

        assertFalse(layout.isPlacedOnPage());
        assertEquals(0, layout.getRotationDeg());
        assertEquals(85.6, layout.getPlacedWidthMm());
        assertEquals(54.0, layout.getPlacedHeightMm());
    }

    @Test
    void pagePlacementRoundTripsAndSwapsBoundingBoxWhenRotated() {
        PvcCardLayout original = PvcCardLayout.defaultLayout();
        original.setPageWidthMm(210);
        original.setPageHeightMm(297);
        original.setRotationDeg(90);
        original.setCardLeftMm(12.5);
        original.setCardTopMm(33.5);

        PvcCardLayout restored = PvcCardLayout.fromJson(original.toJson());

        assertTrue(restored.isPlacedOnPage());
        assertEquals(210.0, restored.getPageWidthMm());
        assertEquals(297.0, restored.getPageHeightMm());
        assertEquals(90, restored.getRotationDeg());
        assertEquals(12.5, restored.getCardLeftMm());
        assertEquals(33.5, restored.getCardTopMm());
        assertEquals(54.0, restored.getPlacedWidthMm());
        assertEquals(85.6, restored.getPlacedHeightMm());
    }

    @Test
    void invalidRotationAndNegativePageValuesFallBackToSafeDefaults() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{\"pageWidthMm\":-5,\"pageHeightMm\":297,\"rotationDeg\":45,\"cardLeftMm\":-3,\"slots\":{}}");

        assertEquals(0, layout.getRotationDeg());
        assertEquals(0.0, layout.getPageWidthMm());
        assertFalse(layout.isPlacedOnPage());
        assertEquals(0.0, layout.getCardLeftMm());
        layout.setRotationDeg(-90);
        assertEquals(270, layout.getRotationDeg());
    }

    @Test
    void fromJsonInheritsMissingSlotPropertiesFromNamedDefaultNotGenericDefault() {
        PvcCardLayout layout = PvcCardLayout.fromJson("{\"slots\":{\"barcode\":{\"type\":\"code128\"}}}");

        PvcCardSlot barcode = layout.getSlots().get(PvcCardLayout.SLOT_BARCODE);
        assertTrue(barcode.isVisible());
        assertEquals(40.0, barcode.getWidthMm());
        assertEquals(8.0, barcode.getHeightMm());
    }
}

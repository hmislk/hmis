package com.divudi.core.data;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvcCardSlotTest {

    @Test
    void roundTripsThroughJson() {
        PvcCardSlot slot = new PvcCardSlot(true, 5.5, 20.25, 9.0, "#112233");

        JSONObject json = slot.toJson();
        PvcCardSlot restored = PvcCardSlot.fromJson(json);

        assertTrue(restored.isVisible());
        assertEquals(5.5, restored.getLeftMm());
        assertEquals(20.25, restored.getTopMm());
        assertEquals(9.0, restored.getFontSizePt());
        assertEquals("#112233", restored.getFontColor());
    }

    @Test
    void barcodeSlotCarriesWidthHeightAndType() {
        PvcCardSlot slot = PvcCardSlot.barcodeSlot(true, 5, 48, 40, 10, "code128");

        JSONObject json = slot.toJson();
        PvcCardSlot restored = PvcCardSlot.fromJson(json);

        assertEquals(40.0, restored.getWidthMm());
        assertEquals(10.0, restored.getHeightMm());
        assertEquals("code128", restored.getType());
    }

    @Test
    void fromJsonFillsMissingFieldsWithDefaults() {
        PvcCardSlot restored = PvcCardSlot.fromJson(new JSONObject());

        assertFalse(restored.isVisible());
        assertEquals(0.0, restored.getLeftMm());
        assertEquals(8.0, restored.getFontSizePt());
        assertEquals("#000000", restored.getFontColor());
        assertEquals("code128", restored.getType());
    }
}

package com.divudi.core.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvcCardBarcodeSvgTest {

    @Test
    void code128FillsTheConfiguredSize() {
        String svg = PvcCardBarcodeSvg.render("code128", "PHN0000001", 40, 8);

        assertNotNull(svg);
        assertTrue(svg.contains("width=\"40.00mm\""));
        assertTrue(svg.contains("height=\"8.00mm\""));
        assertTrue(svg.contains("preserveAspectRatio=\"none\""));
        assertTrue(svg.contains("<rect "));
    }

    @Test
    void typeIsCaseInsensitive() {
        assertNotNull(PvcCardBarcodeSvg.render(" Code128 ", "12345", 30, 10));
    }

    @Test
    void unsupportedTypeFallsBack() {
        assertNull(PvcCardBarcodeSvg.render("qr", "PHN0000001", 20, 20));
        assertNull(PvcCardBarcodeSvg.render(null, "PHN0000001", 20, 20));
    }

    @Test
    void valueTheTypeCannotEncodeFallsBack() {
        // EAN-13 accepts digits only
        assertNull(PvcCardBarcodeSvg.render("ean13", "PHN0000001", 40, 8));
    }

    @Test
    void emptyValueOrSizeRendersNothing() {
        assertNull(PvcCardBarcodeSvg.render("code128", "", 40, 8));
        assertNull(PvcCardBarcodeSvg.render("code128", null, 40, 8));
        assertNull(PvcCardBarcodeSvg.render("code128", "PHN0000001", 0, 8));
    }
}

package com.divudi.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardHtmlSanitizerTest {

    @Test
    void keepsFormattingAndImages() {
        String out = CardHtmlSanitizer.sanitize("<p class=\"ql-align-center\"><strong>Card ශ්‍රී</strong></p>"
                + "<img src=\"/rh/resources/logo.png\"><img src=\"data:image/png;base64,AAA\">"
                + "<table border=\"1\"><tr><td style=\"width:50%\">x</td></tr></table>");
        assertTrue(out.contains("class=\"ql-align-center\""));
        assertTrue(out.contains("ශ්‍රී"));
        assertTrue(out.contains("src=\"/rh/resources/logo.png\""));
        assertTrue(out.contains("src=\"data:image/png;base64,AAA\""));
        assertTrue(out.contains("border=\"1\""));
        assertTrue(out.contains("style=\"width:50%\""));
    }

    @Test
    void dropsActiveContent() {
        String out = CardHtmlSanitizer.sanitize("<p onclick=\"x()\">a<script>s()</script>"
                + "<a href=\"javascript:y()\">l</a></p><img src=x onerror=z()><svg onload=w()></svg><iframe src=\"/\"></iframe>");
        assertFalse(out.contains("onclick"));
        assertFalse(out.contains("<script"));
        assertFalse(out.contains("javascript:"));
        assertFalse(out.contains("onerror"));
        assertFalse(out.contains("<svg"));
        assertFalse(out.contains("<iframe"));
    }

    @Test
    void escapedTextStaysInactiveAfterDisplayUnescape() {
        // The visit page getter un-escapes entities before display; cleaning again afterwards must leave nothing live
        String stored = CardHtmlSanitizer.sanitize("<p>&lt;img src=x onerror=z()&gt;</p>");
        String unescaped = stored.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .replace("&quot;", "\"").replace("&#39;", "'");
        String shown = CardHtmlSanitizer.sanitize(unescaped);
        assertFalse(shown.contains("onerror"));
    }

    @Test
    void nullStaysNull() {
        assertNull(CardHtmlSanitizer.sanitize(null));
    }
}

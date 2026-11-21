package com.divudi.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardHtmlSanitizerTest {

    @Test
    void keepsFormattingAndImages() {
        String out = CardHtmlSanitizer.sanitize("<p class=\"ql-align-center\"><strong>Card ශ්‍රී</strong></p>"
                + "<img src=\"/rh/resources/logo.png\"><img src=\"data:image/png;base64,AAA\">"
                + "<table border=\"1\"><tr><td style=\"width:50%\" bgcolor=\"#eee\" valign=\"top\" height=\"20\">x</td></tr></table>");
        assertTrue(out.contains("class=\"ql-align-center\""));
        assertTrue(out.contains("ශ්‍රී"));
        assertTrue(out.contains("src=\"/rh/resources/logo.png\""));
        assertTrue(out.contains("src=\"data:image/png;base64,AAA\""));
        assertTrue(out.contains("border=\"1\""));
        assertTrue(out.contains("style=\"width:50%\""));
        assertTrue(out.contains("bgcolor=\"#eee\""));
        assertTrue(out.contains("valign=\"top\""));
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
    void storedTextOnlyDocumentAlwaysHoldsATag() {
        // Typed tags in a text-only document stay escaped text, and the result has a tag,
        // so the visit page never treats it as an old wholly-escaped document
        String stored = CardHtmlSanitizer.sanitizeForStorage("avoid &lt;b&gt;aspirin&lt;/b&gt;");
        assertTrue(stored.startsWith("<p>"));
        assertTrue(stored.contains("&lt;b&gt;"));
        assertEquals("<b>x</b>", CardHtmlSanitizer.sanitizeForStorage("<b>x</b>"));
        assertEquals("", CardHtmlSanitizer.sanitizeForStorage(""));
        assertNull(CardHtmlSanitizer.sanitizeForStorage(null));
    }

    @Test
    void legacyEscapedDocumentKeepsFormattingWhenSavedUnedited() {
        // An old wholly-escaped document opened and saved without editing
        String stored = CardHtmlSanitizer.sanitizeExistingForStorage(
                "&lt;p&gt;Review&lt;/p&gt;&lt;table border=&quot;1&quot;&gt;&lt;tr&gt;&lt;td&gt;x&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;");
        assertTrue(stored.startsWith("<p>Review</p>"));
        assertTrue(stored.contains("<table border=\"1\">"));
        // Saving it again changes nothing, and display shows the same
        assertEquals(stored, CardHtmlSanitizer.sanitizeExistingForStorage(stored));
        assertEquals(stored, CardHtmlSanitizer.sanitize(CardHtmlSanitizer.decodeLegacy(stored)));
    }

    @Test
    void legacyDecodeStillDropsActiveContent() {
        String stored = CardHtmlSanitizer.sanitizeExistingForStorage("&lt;img src=x onerror=z()&gt;&lt;script&gt;y()&lt;/script&gt;");
        assertFalse(stored.contains("onerror"));
        assertFalse(stored.contains("script"));
    }

    @Test
    void markupDocumentKeepsEscapedTextOnSave() {
        String stored = CardHtmlSanitizer.sanitizeExistingForStorage("<p>avoid &lt;b&gt;aspirin&lt;/b&gt;</p>");
        assertEquals("<p>avoid &lt;b&gt;aspirin&lt;/b&gt;</p>", stored);
    }

    @Test
    void nullStaysNull() {
        assertNull(CardHtmlSanitizer.sanitize(null));
    }
}

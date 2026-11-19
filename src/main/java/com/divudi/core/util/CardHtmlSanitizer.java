package com.divudi.core.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Cleans the HTML of clinical documents (diagnosis cards, referral letters,
 * visit documents) before it is stored. These pages show the stored HTML with
 * escape="false", so only formatting markup is kept: scripts, event handlers
 * and javascript: links are dropped. Relative and data: image sources are kept
 * so template logos survive a save.
 */
public final class CardHtmlSanitizer {

    private static final String BASE_URI = "http://localhost/";

    private CardHtmlSanitizer() {
    }

    private static Safelist safelist() {
        return Safelist.relaxed()
                .addTags("s", "hr", "font", "center")
                .addAttributes(":all", "style", "class", "align")
                .addAttributes("font", "color", "size", "face")
                .addAttributes("table", "border", "cellpadding", "cellspacing", "bgcolor", "height")
                .addAttributes("tr", "bgcolor", "valign", "height")
                .addAttributes("td", "bgcolor", "valign", "height")
                .addAttributes("th", "bgcolor", "valign", "height")
                .addProtocols("img", "src", "data")
                .preserveRelativeLinks(true);
    }

    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        // No pretty-print: keep the editor's own spacing
        return Jsoup.clean(html, BASE_URI, safelist(), new Document.OutputSettings().prettyPrint(false));
    }
}

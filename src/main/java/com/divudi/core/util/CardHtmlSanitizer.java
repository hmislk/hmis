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

    /**
     * Cleans a document for storage and makes sure it holds at least one tag. The OPD
     * visit page decodes a stored document only when it has no tag at all (documents
     * saved before this cleaning were stored wholly escaped), so a new text-only
     * document must not look like one of those.
     */
    public static String sanitizeForStorage(String html) {
        String clean = sanitize(html);
        if (clean == null || clean.isEmpty() || clean.indexOf('<') >= 0) {
            return clean;
        }
        return "<p>" + clean + "</p>";
    }

    /**
     * Decodes a document saved before this cleaning existed. Those were stored wholly
     * escaped, so they hold no real tag. Content that already holds a tag is returned
     * as it is, so escaped text in it stays text.
     */
    public static String decodeLegacy(String html) {
        if (html == null || html.indexOf('<') >= 0) {
            return html;
        }
        return html.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    /**
     * For saving an existing document as it was opened: an old wholly-escaped document
     * is decoded once, then cleaned and stored with a tag, so it keeps its formatting.
     */
    public static String sanitizeExistingForStorage(String html) {
        return sanitizeForStorage(decodeLegacy(html));
    }
}

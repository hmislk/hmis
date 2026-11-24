package com.divudi.core.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.VerticalAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Converts the HTML of a rendered clinical document (e.g. an inpatient
 * diagnosis card) into an editable Word (.docx) or RTF file (#25614).
 *
 * It works from the same rendered HTML that the screen shows and the PDF
 * download uses, so placeholders are already filled. Covers the markup the
 * document templates use: headings, paragraphs, line breaks, bold, italic,
 * underline, sub/superscript, font size and text alignment from inline
 * styles, lists, tables (one level; a nested table is written as text) and
 * images (data URIs, or paths resolved by the caller).
 */
public final class HtmlDocumentConverter {

    private static final Pattern DATA_URI = Pattern.compile("^data:image/([a-zA-Z+]+);base64,(.*)$", Pattern.DOTALL);
    private static final Pattern FONT_SIZE = Pattern.compile("font-size\\s*:\\s*([0-9.]+)\\s*(px|pt)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXT_ALIGN = Pattern.compile("text-align\\s*:\\s*(left|right|center|justify)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FONT_WEIGHT_BOLD = Pattern.compile("font-weight\\s*:\\s*(bold|[6-9]00)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDERLINE = Pattern.compile("text-decoration[a-z-]*\\s*:[^;]*underline", Pattern.CASE_INSENSITIVE);
    private static final Pattern ITALIC = Pattern.compile("font-style\\s*:\\s*italic", Pattern.CASE_INSENSITIVE);
    private static final double MAX_IMAGE_WIDTH_PT = 450;
    /** Largest image file accepted, before decoding. */
    private static final int MAX_IMAGE_BYTES = 5_000_000;
    /** Largest image accepted, in pixels (width x height), checked before decoding. */
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;
    /** Most images one card may carry into a file; later ones show their alt text. */
    static final int MAX_IMAGES_PER_DOCUMENT = 20;
    private static final int BASE_FONT_PT = 11;

    private HtmlDocumentConverter() {
    }

    /**
     * Raw image bytes for an img src, or null when it cannot be loaded.
     */
    public interface ImageResolver extends Function<String, byte[]> {
    }

    // ------------------------------------------------------------------
    // Shared model
    // ------------------------------------------------------------------
    private static final class Style {

        boolean bold;
        boolean italic;
        boolean underline;
        boolean sub;
        boolean sup;
        int fontPt = BASE_FONT_PT;

        Style copy() {
            Style s = new Style();
            s.bold = bold;
            s.italic = italic;
            s.underline = underline;
            s.sub = sub;
            s.sup = sup;
            s.fontPt = fontPt;
            return s;
        }
    }

    private static final class Img {

        final byte[] bytes;
        final String format;
        final double widthPt;
        final double heightPt;

        Img(byte[] bytes, String format, double widthPt, double heightPt) {
            this.bytes = bytes;
            this.format = format;
            this.widthPt = widthPt;
            this.heightPt = heightPt;
        }
    }

    private static boolean isBlock(String tag) {
        switch (tag) {
            case "p":
            case "div":
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
            case "li":
            case "ul":
            case "ol":
            case "table":
            case "tr":
            case "blockquote":
            case "section":
            case "header":
            case "footer":
            case "hr":
                return true;
            default:
                return false;
        }
    }

    private static boolean isSkipped(String tag) {
        return "head".equals(tag) || "title".equals(tag) || "meta".equals(tag)
                || "style".equals(tag) || "script".equals(tag) || "link".equals(tag);
    }

    private static Style applyInline(Element e, Style parent) {
        Style s = parent.copy();
        String tag = e.normalName();
        String css = e.attr("style");
        if ("b".equals(tag) || "strong".equals(tag) || "th".equals(tag) || FONT_WEIGHT_BOLD.matcher(css).find()) {
            s.bold = true;
        }
        if ("i".equals(tag) || "em".equals(tag) || ITALIC.matcher(css).find()) {
            s.italic = true;
        }
        if ("u".equals(tag) || UNDERLINE.matcher(css).find()) {
            s.underline = true;
        }
        if ("sub".equals(tag)) {
            s.sub = true;
        }
        if ("sup".equals(tag)) {
            s.sup = true;
        }
        int heading = headingSize(tag);
        if (heading > 0) {
            s.bold = true;
            s.fontPt = heading;
        }
        Matcher fm = FONT_SIZE.matcher(css);
        if (fm.find()) {
            try {
                double v = Double.parseDouble(fm.group(1));
                double pt = "px".equalsIgnoreCase(fm.group(2)) ? v * 0.75 : v;
                if (pt >= 5 && pt <= 72) {
                    s.fontPt = (int) Math.round(pt);
                }
            } catch (NumberFormatException ignored) {
                // keep the inherited size
            }
        }
        return s;
    }

    private static int headingSize(String tag) {
        switch (tag) {
            case "h1":
                return 20;
            case "h2":
                return 16;
            case "h3":
                return 14;
            case "h4":
                return 12;
            case "h5":
            case "h6":
                return 11;
            default:
                return 0;
        }
    }

    private static String alignOf(Element e) {
        Matcher m = TEXT_ALIGN.matcher(e.attr("style"));
        if (m.find()) {
            return m.group(1).toLowerCase(Locale.ROOT);
        }
        String a = e.attr("align").toLowerCase(Locale.ROOT);
        return a.isEmpty() ? null : a;
    }

    private static String collapse(String text) {
        return text.replace('\u00a0', ' ').replaceAll("[\\s]+", " ");
    }

    private static Img loadImage(Element img, ImageResolver resolver, int imageNumber) {
        if (imageNumber > MAX_IMAGES_PER_DOCUMENT) {
            return null;
        }
        String src = img.attr("src");
        if (src == null || src.trim().isEmpty()) {
            return null;
        }
        byte[] bytes = null;
        Matcher m = DATA_URI.matcher(src.trim());
        if (m.matches()) {
            if (m.group(2).length() > MAX_IMAGE_BYTES / 3 * 4 + 4) {
                return null;
            }
            try {
                bytes = Base64.getMimeDecoder().decode(m.group(2));
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else if (resolver != null) {
            bytes = resolver.apply(src.trim());
        }
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            return null;
        }
        // Hardening, tracked under #25614: format and size come from the image itself.
        String format;
        BufferedImage bi;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                long w = reader.getWidth(0);
                long h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w * h > MAX_IMAGE_PIXELS) {
                    return null;
                }
                String name = reader.getFormatName().toLowerCase(Locale.ROOT);
                format = name.contains("png") ? "png"
                        : (name.contains("jpeg") || name.contains("jpg")) ? "jpeg" : name;
                bi = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        if (bi == null) {
            return null;
        }
        // RTF and docx both take png/jpeg directly; convert anything else to png.
        if (!"png".equals(format) && !"jpeg".equals(format)) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(bi, "png", out);
                bytes = out.toByteArray();
                format = "png";
            } catch (IOException e) {
                return null;
            }
        }
        double wPt = bi.getWidth() * 0.75;
        double hPt = bi.getHeight() * 0.75;
        double requested = parseLength(img.attr("width"));
        if (requested > 0) {
            double scale = requested * 0.75 / wPt;
            wPt *= scale;
            hPt *= scale;
        }
        if (wPt > MAX_IMAGE_WIDTH_PT) {
            double scale = MAX_IMAGE_WIDTH_PT / wPt;
            wPt *= scale;
            hPt *= scale;
        }
        return new Img(bytes, format, wPt, hPt);
    }

    private static double parseLength(String v) {
        if (v == null) {
            return 0;
        }
        String digits = v.replaceAll("[^0-9.]", "");
        if (digits.isEmpty() || v.contains("%")) {
            return 0;
        }
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<Element> rowsOf(Element table) {
        List<Element> rows = new ArrayList<>();
        for (Element tr : table.select("tr")) {
            // Only rows of this table, not of a nested one.
            if (tr.closest("table") == table) {
                rows.add(tr);
            }
        }
        return rows;
    }

    private static List<Element> cellsOf(Element tr) {
        List<Element> cells = new ArrayList<>();
        for (Element c : tr.children()) {
            if ("td".equals(c.normalName()) || "th".equals(c.normalName())) {
                cells.add(c);
            }
        }
        return cells;
    }

    // ------------------------------------------------------------------
    // DOCX
    // ------------------------------------------------------------------
    public static byte[] toDocx(String html, ImageResolver resolver) throws IOException {
        Element body = Jsoup.parse(html == null ? "" : html).body();
        try (XWPFDocument doc = new XWPFDocument()) {
            DocxWriter w = new DocxWriter(resolver);
            w.writeChildren(body, new DocxTarget(doc), new Style(), null);
            if (doc.getParagraphs().isEmpty() && doc.getTables().isEmpty()) {
                doc.createParagraph();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Where new paragraphs and tables go: the document body or a table cell.
     */
    private static final class DocxTarget {

        final XWPFDocument doc;
        final XWPFTableCell cell;
        XWPFParagraph current;
        String pendingAlign;
        boolean cellFirstUsed;

        DocxTarget(XWPFDocument doc) {
            this.doc = doc;
            this.cell = null;
        }

        DocxTarget(XWPFTableCell cell) {
            this.doc = null;
            this.cell = cell;
        }

        XWPFParagraph newParagraph(String align) {
            XWPFParagraph p;
            if (cell != null) {
                if (!cellFirstUsed && !cell.getParagraphs().isEmpty()) {
                    p = cell.getParagraphs().get(0);
                    cellFirstUsed = true;
                } else {
                    p = cell.addParagraph();
                }
            } else {
                p = doc.createParagraph();
            }
            if (align != null) {
                switch (align) {
                    case "center":
                        p.setAlignment(ParagraphAlignment.CENTER);
                        break;
                    case "right":
                        p.setAlignment(ParagraphAlignment.RIGHT);
                        break;
                    case "justify":
                        p.setAlignment(ParagraphAlignment.BOTH);
                        break;
                    default:
                        p.setAlignment(ParagraphAlignment.LEFT);
                }
            }
            current = p;
            return p;
        }

        /** The open paragraph; created only when content arrives, so wrapper divs add no blank lines. */
        XWPFParagraph paragraph() {
            return current != null ? current : newParagraph(pendingAlign);
        }

        void startBlock(String align) {
            current = null;
            pendingAlign = align;
        }

        void endBlock() {
            current = null;
            pendingAlign = null;
        }
    }

    private static final class DocxWriter {

        final ImageResolver resolver;
        int imageCount;

        DocxWriter(ImageResolver resolver) {
            this.resolver = resolver;
        }

        void writeChildren(Element parent, DocxTarget t, Style style, String align) {
            for (Node n : parent.childNodes()) {
                write(n, t, style, align);
            }
        }

        void write(Node n, DocxTarget t, Style style, String align) {
            if (n instanceof TextNode) {
                String text = collapse(((TextNode) n).getWholeText());
                if (text.trim().isEmpty() && (t.current == null || t.current.getRuns().isEmpty())) {
                    return;
                }
                XWPFParagraph p = t.paragraph();
                if (p.getRuns().isEmpty()) {
                    text = text.replaceAll("^\\s+", "");
                }
                if (!text.isEmpty()) {
                    styledRun(p, style).setText(text);
                }
                return;
            }
            if (!(n instanceof Element)) {
                return;
            }
            Element e = (Element) n;
            String tag = e.normalName();
            if (isSkipped(tag)) {
                return;
            }
            Style s = applyInline(e, style);
            String a = alignOf(e) != null ? alignOf(e) : align;
            switch (tag) {
                case "br":
                    t.paragraph().createRun().addBreak();
                    return;
                case "hr":
                    t.endBlock();
                    t.paragraph().setBorderBottom(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
                    t.endBlock();
                    return;
                case "img":
                    writeImage(e, t);
                    return;
                case "table":
                    writeTable(e, t, s);
                    return;
                case "ul":
                case "ol":
                    t.endBlock();
                    int i = 1;
                    for (Element li : e.children()) {
                        if (!"li".equals(li.normalName())) {
                            continue;
                        }
                        t.startBlock(a);
                        XWPFParagraph p = t.paragraph();
                        p.setIndentationLeft(360);
                        styledRun(p, s).setText("ol".equals(tag) ? (i++) + ". " : "\u2022 ");
                        writeChildren(li, t, applyInline(li, s), a);
                        t.endBlock();
                    }
                    return;
                default:
            }
            if (isBlock(tag)) {
                t.startBlock(a);
                writeChildren(e, t, s, a);
                t.endBlock();
            } else {
                writeChildren(e, t, s, a);
            }
        }

        XWPFRun styledRun(XWPFParagraph p, Style s) {
            XWPFRun r = p.createRun();
            r.setFontFamily("Arial");
            r.setFontSize(s.fontPt);
            r.setBold(s.bold);
            r.setItalic(s.italic);
            if (s.underline) {
                r.setUnderline(UnderlinePatterns.SINGLE);
            }
            if (s.sub) {
                r.setSubscript(VerticalAlign.SUBSCRIPT);
            } else if (s.sup) {
                r.setSubscript(VerticalAlign.SUPERSCRIPT);
            }
            return r;
        }

        void writeImage(Element e, DocxTarget t) {
            Img img = loadImage(e, resolver, ++imageCount);
            if (img == null) {
                String alt = e.attr("alt");
                if (!alt.isEmpty()) {
                    t.paragraph().createRun().setText("[" + alt + "]");
                }
                return;
            }
            int type = "png".equals(img.format) ? Document.PICTURE_TYPE_PNG : Document.PICTURE_TYPE_JPEG;
            try {
                t.paragraph().createRun().addPicture(new ByteArrayInputStream(img.bytes), type,
                        "image." + img.format, Units.toEMU(img.widthPt), Units.toEMU(img.heightPt));
            } catch (Exception ex) {
                // An image that Word cannot take is left out; the text still converts.
            }
        }

        void writeTable(Element table, DocxTarget t, Style style) {
            t.endBlock();
            List<Element> rows = rowsOf(table);
            int cols = 0;
            for (Element tr : rows) {
                int c = 0;
                for (Element cell : cellsOf(tr)) {
                    c += Math.max(1, parseIntSafe(cell.attr("colspan")));
                }
                cols = Math.max(cols, c);
            }
            if (rows.isEmpty() || cols == 0) {
                return;
            }
            if (t.cell != null) {
                // Nested table: written as text lines inside the cell.
                for (Element tr : rows) {
                    StringBuilder sb = new StringBuilder();
                    for (Element cell : cellsOf(tr)) {
                        if (sb.length() > 0) {
                            sb.append(" | ");
                        }
                        sb.append(cell.text());
                    }
                    t.startBlock(null);
                    styledRun(t.paragraph(), style).setText(sb.toString());
                    t.endBlock();
                }
                return;
            }
            XWPFTable xt = t.doc.createTable(rows.size(), cols);
            xt.setWidth("100%");
            for (int r = 0; r < rows.size(); r++) {
                XWPFTableRow xr = xt.getRow(r);
                int c = 0;
                for (Element cell : cellsOf(rows.get(r))) {
                    if (c >= cols) {
                        break;
                    }
                    int span = Math.max(1, parseIntSafe(cell.attr("colspan")));
                    XWPFTableCell xc = xr.getCell(c);
                    if (span > 1) {
                        xc.getCTTc().addNewTcPr().addNewGridSpan().setVal(java.math.BigInteger.valueOf(Math.min(span, cols - c)));
                        for (int k = 1; k < span && c + 1 < xr.getTableCells().size(); k++) {
                            xr.removeCell(c + 1);
                        }
                    }
                    DocxTarget ct = new DocxTarget(xc);
                    Style cs = applyInline(cell, style);
                    String ca = alignOf(cell);
                    writeChildren(cell, ct, cs, ca);
                    if (ca != null && !ct.cellFirstUsed && !xc.getParagraphs().isEmpty()) {
                        // Empty cell keeps the alignment for anything typed into it later.
                        ct.newParagraph(ca);
                    }
                    c++;
                }
            }
            // Separate consecutive tables so Word does not merge them.
            t.newParagraph(null);
            t.endBlock();
        }
    }

    private static int parseIntSafe(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return 1;
        }
    }

    // ------------------------------------------------------------------
    // RTF
    // ------------------------------------------------------------------
    public static byte[] toRtf(String html, ImageResolver resolver) {
        Element body = Jsoup.parse(html == null ? "" : html).body();
        RtfWriter w = new RtfWriter(resolver);
        w.out.append("{\\rtf1\\ansi\\ansicpg1252\\deff0{\\fonttbl{\\f0\\fswiss Arial;}}\\viewkind4\\uc1\n");
        w.startParagraph(null);
        w.writeChildren(body, new Style(), null);
        w.endParagraph();
        w.out.append("}");
        return w.out.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static final class RtfWriter {

        final StringBuilder out = new StringBuilder();
        final ImageResolver resolver;
        boolean inCell;
        boolean paragraphOpen;
        boolean paragraphHasContent;
        int imageCount;

        RtfWriter(ImageResolver resolver) {
            this.resolver = resolver;
        }

        void startParagraph(String align) {
            if (inCell) {
                if (paragraphHasContent) {
                    out.append("\\line ");
                    paragraphHasContent = false;
                }
                return;
            }
            out.append("\\pard\\sa120");
            if (align != null) {
                switch (align) {
                    case "center":
                        out.append("\\qc");
                        break;
                    case "right":
                        out.append("\\qr");
                        break;
                    case "justify":
                        out.append("\\qj");
                        break;
                    default:
                        out.append("\\ql");
                }
            }
            out.append(' ');
            paragraphOpen = true;
            paragraphHasContent = false;
        }

        void endParagraph() {
            if (inCell) {
                return;
            }
            if (paragraphOpen && paragraphHasContent) {
                out.append("\\par\n");
            }
            paragraphOpen = false;
        }

        void writeChildren(Element parent, Style style, String align) {
            for (Node n : parent.childNodes()) {
                write(n, style, align);
            }
        }

        void write(Node n, Style style, String align) {
            if (n instanceof TextNode) {
                String text = collapse(((TextNode) n).getWholeText());
                if (!paragraphHasContent) {
                    text = text.replaceAll("^\\s+", "");
                }
                if (text.isEmpty()) {
                    return;
                }
                text(text, style);
                return;
            }
            if (!(n instanceof Element)) {
                return;
            }
            Element e = (Element) n;
            String tag = e.normalName();
            if (isSkipped(tag)) {
                return;
            }
            Style s = applyInline(e, style);
            String a = alignOf(e) != null ? alignOf(e) : align;
            switch (tag) {
                case "br":
                    out.append("\\line ");
                    paragraphHasContent = true;
                    return;
                case "hr":
                    if (inCell) {
                        // A paragraph mark inside a cell would end the table row and break the table.
                        out.append("\\line ");
                        paragraphHasContent = true;
                        return;
                    }
                    endParagraph();
                    out.append("\\pard\\brdrb\\brdrs\\brdrw10\\par\n");
                    startParagraph(null);
                    return;
                case "img":
                    image(e);
                    return;
                case "table":
                    table(e, s);
                    return;
                case "ul":
                case "ol":
                    int i = 1;
                    for (Element li : e.children()) {
                        if (!"li".equals(li.normalName())) {
                            continue;
                        }
                        endParagraph();
                        startParagraph(a);
                        if (!inCell) {
                            out.append("\\li360 ");
                        }
                        text("ol".equals(tag) ? (i++) + ". " : "\u2022 ", s);
                        writeChildren(li, applyInline(li, s), a);
                    }
                    endParagraph();
                    startParagraph(align);
                    return;
                default:
            }
            if (isBlock(tag)) {
                endParagraph();
                startParagraph(a);
                writeChildren(e, s, a);
                endParagraph();
                startParagraph(align);
            } else {
                writeChildren(e, s, a);
            }
        }

        void text(String text, Style s) {
            out.append("{\\f0\\fs").append(s.fontPt * 2);
            if (s.bold) {
                out.append("\\b");
            }
            if (s.italic) {
                out.append("\\i");
            }
            if (s.underline) {
                out.append("\\ul");
            }
            if (s.sub) {
                out.append("\\sub");
            } else if (s.sup) {
                out.append("\\super");
            }
            out.append(' ');
            escape(text);
            out.append('}');
            paragraphHasContent = true;
        }

        void escape(String text) {
            for (int k = 0; k < text.length(); k++) {
                char ch = text.charAt(k);
                if (ch == '\\' || ch == '{' || ch == '}') {
                    out.append('\\').append(ch);
                } else if (ch == '\t') {
                    out.append("\\tab ");
                } else if (ch < 128) {
                    out.append(ch);
                } else {
                    // \\uN takes a signed 16-bit value, followed by one fallback char.
                    out.append("\\u").append((int) (short) ch).append('?');
                }
            }
        }

        void image(Element e) {
            Img img = loadImage(e, resolver, ++imageCount);
            if (img == null) {
                String alt = e.attr("alt");
                if (!alt.isEmpty()) {
                    text("[" + alt + "]", new Style());
                }
                return;
            }
            int wTw = (int) Math.round(img.widthPt * 20);
            int hTw = (int) Math.round(img.heightPt * 20);
            out.append("{\\pict").append("png".equals(img.format) ? "\\pngblip" : "\\jpegblip")
                    .append("\\picwgoal").append(wTw).append("\\pichgoal").append(hTw).append('\n');
            int col = 0;
            for (byte b : img.bytes) {
                out.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
                if (++col == 64) {
                    out.append('\n');
                    col = 0;
                }
            }
            out.append("}");
            paragraphHasContent = true;
        }

        void table(Element table, Style style) {
            List<Element> rows = rowsOf(table);
            int cols = 0;
            for (Element tr : rows) {
                cols = Math.max(cols, cellsOf(tr).size());
            }
            if (rows.isEmpty() || cols == 0) {
                return;
            }
            if (inCell) {
                // Nested table: written as text lines inside the cell.
                for (Element tr : rows) {
                    StringBuilder sb = new StringBuilder();
                    for (Element cell : cellsOf(tr)) {
                        if (sb.length() > 0) {
                            sb.append(" | ");
                        }
                        sb.append(cell.text());
                    }
                    startParagraph(null);
                    text(sb.toString(), style);
                }
                return;
            }
            endParagraph();
            int totalTw = 9360; // 6.5 inches
            for (Element tr : rows) {
                List<Element> cells = cellsOf(tr);
                out.append("\\trowd\\trgaph108\\trleft0");
                int n = Math.max(cells.size(), 1);
                for (int c = 0; c < n; c++) {
                    int edge = (c == n - 1) ? totalTw : totalTw * (c + 1) / n;
                    out.append("\\clbrdrt\\brdrs\\brdrw10\\clbrdrl\\brdrs\\brdrw10\\clbrdrb\\brdrs\\brdrw10\\clbrdrr\\brdrs\\brdrw10\\cellx").append(edge);
                }
                out.append('\n');
                for (Element cell : cells) {
                    String ca = alignOf(cell);
                    out.append("\\pard\\intbl");
                    if ("center".equals(ca)) {
                        out.append("\\qc");
                    } else if ("right".equals(ca)) {
                        out.append("\\qr");
                    }
                    out.append(' ');
                    inCell = true;
                    paragraphHasContent = false;
                    writeChildren(cell, applyInline(cell, style), ca);
                    inCell = false;
                    out.append("\\cell\n");
                }
                out.append("\\row\n");
            }
            out.append("\\pard\\par\n");
            startParagraph(null);
        }
    }
}

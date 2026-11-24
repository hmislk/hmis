package com.divudi.core.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Diagnosis card download as Word / RTF (#25614).
 */
class HtmlDocumentConverterTest {

    private static String rtf(String html, HtmlDocumentConverter.ImageResolver resolver) {
        return new String(HtmlDocumentConverter.toRtf(html, resolver), StandardCharsets.US_ASCII);
    }

    /** The main document part of a docx, as text. */
    private static String docxXml(byte[] docx) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if ("word/document.xml".equals(e.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = zin.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                    return out.toString("UTF-8");
                }
            }
        }
        return "";
    }

    private static boolean docxHasMedia(byte[] docx) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().startsWith("word/media/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] image(String format, int w, int h) throws IOException {
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, format, out);
        return out.toByteArray();
    }

    /** A minimal PNG header with a chosen size, for the size-limit test. */
    private static byte[] pngHeaderOnly(int w, int h) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        byte[] ihdr = new byte[13];
        putInt(ihdr, 0, w);
        putInt(ihdr, 4, h);
        ihdr[8] = 8; // bit depth
        ihdr[9] = 2; // colour type RGB
        chunk(out, "IHDR", ihdr);
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        byte[] len = new byte[4];
        putInt(len, 0, data.length);
        out.write(len);
        byte[] t = type.getBytes(StandardCharsets.US_ASCII);
        out.write(t);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        byte[] c = new byte[4];
        putInt(c, 0, (int) crc.getValue());
        out.write(c);
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }

    @Test
    void rtfEscapesSpecialCharactersAndUnicode() {
        String out = rtf("<p>a {b} c\\d ශ්‍රී தமிழ்</p>", null);
        assertTrue(out.startsWith("{\\rtf1"));
        assertTrue(out.contains("\\{b\\}"));
        assertTrue(out.contains("c\\\\d"));
        assertTrue(out.contains("\\u3521?"), "Sinhala letter SHA as \\uN?");
        assertTrue(out.contains("\\u2980?"), "Tamil letter TA as \\uN?");
        assertFalse(out.chars().anyMatch(ch -> ch > 127), "RTF output is plain ASCII");
    }

    @Test
    void nullAndEmptyCardGiveValidFiles() throws IOException {
        assertTrue(rtf(null, null).startsWith("{\\rtf1"));
        assertTrue(rtf("", null).endsWith("}"));
        assertTrue(docxXml(HtmlDocumentConverter.toDocx(null, null)).contains("<w:body"));
        assertTrue(docxXml(HtmlDocumentConverter.toDocx("", null)).contains("<w:p"));
    }

    @Test
    void docxColspanWritesGridSpan() throws IOException {
        String xml = docxXml(HtmlDocumentConverter.toDocx(
                "<table><tr><td colspan=\"2\">Head</td></tr><tr><td>a</td><td>b</td></tr></table>", null));
        assertTrue(xml.contains("w:gridSpan"));
        assertTrue(xml.contains("Head"));
    }

    @Test
    void nestedTableKeepsTheText() throws IOException {
        String html = "<table><tr><td>outer<table><tr><td>inner1</td><td>inner2</td></tr></table></td></tr></table>";
        String xml = docxXml(HtmlDocumentConverter.toDocx(html, null));
        assertTrue(xml.contains("inner1") && xml.contains("inner2"));
        String out = rtf(html, null);
        assertTrue(out.contains("inner1") && out.contains("inner2"));
    }

    @Test
    void hrInsideCellKeepsTheRtfTable() {
        String out = rtf("<table><tr><td>a<hr>b</td></tr></table>", null);
        assertFalse(out.contains("\\brdrb"), "no paragraph border rule inside a cell");
        assertTrue(out.contains("\\cell"));
    }

    @Test
    void dataUriImageIsEmbedded() throws IOException {
        String src = "data:image/png;base64," + Base64.getEncoder().encodeToString(image("png", 4, 3));
        String html = "<p><img src=\"" + src + "\"></p>";
        assertTrue(rtf(html, null).contains("\\pngblip"));
        assertTrue(docxHasMedia(HtmlDocumentConverter.toDocx(html, null)));
    }

    @Test
    void imageFormatComesFromTheBytesNotTheName() {
        // A BMP behind a path with a query string: re-encoded as PNG, not labelled JPEG.
        HtmlDocumentConverter.ImageResolver resolver = s -> {
            try {
                return image("bmp", 4, 3);
            } catch (IOException e) {
                return null;
            }
        };
        String out = rtf("<p><img src=\"/app/resources/image/logo.png?v=2\"></p>", resolver);
        assertTrue(out.contains("\\pngblip"));
        assertFalse(out.contains("\\jpegblip"));
    }

    @Test
    void oversizedImageIsSkipped() throws IOException {
        String src = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngHeaderOnly(100_000, 100_000));
        String html = "<p>before<img src=\"" + src + "\">after</p>";
        String out = rtf(html, null);
        assertFalse(out.contains("\\pict"));
        assertTrue(out.contains("before") && out.contains("after"));
        assertNotNull(HtmlDocumentConverter.toDocx(html, null));
        assertFalse(docxHasMedia(HtmlDocumentConverter.toDocx(html, null)));
    }

    @Test
    void imagesFromUnknownSourcesAreSkipped() {
        String out = rtf("<p><img src=\"https://example.com/x.png\">text</p>", s -> null);
        assertFalse(out.contains("\\pict"));
        assertTrue(out.contains("text"));
    }
}

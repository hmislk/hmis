package com.divudi.core.util;

import com.divudi.core.util.InwardFinalBillTextRenderer.Data;
import com.divudi.core.util.InwardFinalBillTextRenderer.Row;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InwardFinalBillTextRendererTest {

    private Data sample() {
        Data d = new Data();
        d.title = "OPD CARD";
        d.billNo = "IW/FIN/000123";
        d.bhtNo = "BHT/1001";
        d.billDate = "04/Nov/2026";
        d.billTime = "10:15 am";
        d.patientName = "Mr Test Patient";
        d.nic = "123456789V";
        d.charges.add(new Row("Room Charges", "5,000.00"));
        d.charges.add(new Row("Dr A Very Long Consultant Name With Many Words (Cardiology Speciality)", "12,500.00"));
        d.payments.add(new Row("Cash", "10,000.00"));
        d.payments.add(new Row("Card", "7,500.00"));
        d.totalAmount = "17,500.00";
        d.discounts = "0.00";
        d.deposit = "17,500.00";
        d.netAmount = "0.00";
        d.preparedBy = "Cashier One";
        return d;
    }

    @Test
    void noLineExceedsWidthAndAmountsAreRightAligned() {
        for (int w : new int[]{36, 40, 80}) {
            String text = InwardFinalBillTextRenderer.render(sample(), w, 2, 33, false);
            for (String line : text.split("\n", -1)) {
                assertTrue(line.length() <= w, "line too long at width " + w + ": [" + line + "]");
            }
            boolean found = false;
            for (String line : text.split("\n")) {
                if (line.startsWith("Room Charges")) {
                    assertEquals(w, line.length());
                    assertTrue(line.endsWith("5,000.00"));
                    found = true;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    void longNamesWrapUnderNameColumn() {
        String text = InwardFinalBillTextRenderer.render(sample(), 40, 0, 0, false);
        assertTrue(text.contains("\n  Name With"), text);
        assertTrue(text.contains("12,500.00\n"), text);
    }

    @Test
    void headingDetailsAndSubtotalLayout() {
        Data d = sample();
        d.charges.clear();
        d.charges.add(Row.heading("Consultant Fee"));
        d.charges.add(Row.detail("Dr A Very Long Consultant Name (Cardiology)", "1,000.00"));
        d.charges.add(Row.detail("Dr B", "Free of Charge"));
        d.charges.add(Row.subtotal("1,000.00"));
        String text = InwardFinalBillTextRenderer.render(d, 36, 0, 0, false);
        String[] lines = text.split("\n", -1);
        int h = -1;
        for (int i = 0; i < lines.length; i++) {
            assertTrue(lines[i].length() <= 36, "too long: [" + lines[i] + "]");
            if (lines[i].equals("Consultant Fee")) {
                h = i;
            }
        }
        assertTrue(h > 0, text);
        assertTrue(lines[h + 1].startsWith("  Dr A Very"), text);
        assertTrue(lines[h + 1].endsWith("1,000.00"), text);
        assertTrue(lines[h + 2].startsWith("    Consultant"), text);
        assertTrue(lines[h + 3].startsWith("    (Cardiology)"), text);
        assertTrue(lines[h + 4].startsWith("  Dr B"), text);
        assertTrue(lines[h + 4].endsWith("Free of Charge"), text);
        assertEquals(36, lines[h + 5].length(), text);
        assertEquals("1,000.00", lines[h + 5].trim());
    }

    @Test
    void wideAmountTextAndNarrowWidthStayWithinLine() {
        Data d = sample();
        d.charges.add(new Row("Professional fee for a very long consultant name here", "Free of Charge"));
        d.preparedBy = "Cashier Supervisor";
        for (int w : new int[]{24, 30, 36}) {
            String text = InwardFinalBillTextRenderer.render(d, w, 0, 0, false);
            for (String line : text.split("\n", -1)) {
                assertTrue(line.length() <= w, "line too long at width " + w + ": [" + line + "]");
            }
        }
    }

    @Test
    void preparedByContinuationIsIndentedAfterColon() {
        Data d = sample();
        d.preparedBy = "Alexandra Wijesekara Perera Gunawardena";
        String text = InwardFinalBillTextRenderer.render(d, 36, 0, 0, false);
        String[] lines = text.split("\n");
        int i = 0;
        while (!lines[i].startsWith("BILL PREPARED BY : ")) {
            i++;
        }
        assertTrue(lines[i + 1].startsWith(" ".repeat("BILL PREPARED BY : ".length()) + "W"), text);
        assertTrue(lines[i + 1].length() <= 36);
    }

    @Test
    void topMarginAndEscpCodes() {
        String plain = InwardFinalBillTextRenderer.render(sample(), 40, 3, 33, false);
        assertTrue(plain.startsWith("\n\n\n"));
        assertFalse(plain.contains("\u001B"));
        String escp = InwardFinalBillTextRenderer.render(sample(), 40, 3, 33, true);
        assertTrue(escp.startsWith("\u001B@"));
        assertTrue(escp.contains("\u001BC" + (char) 33));
        assertTrue(escp.endsWith("\f"));
    }

    @Test
    void wideFormPlacesPaymentsBesideTotals() {
        String text = InwardFinalBillTextRenderer.render(sample(), 80, 0, 0, false);
        boolean sideBySide = false;
        for (String line : text.split("\n")) {
            if (line.startsWith("PAY TYPE") && line.contains("TOTAL AMOUNT")) {
                sideBySide = true;
            }
        }
        assertTrue(sideBySide, text);
        assertTrue(text.contains("BILL PREPARED BY : Cashier One"));
    }

    // ---- paging ----

    private Data longBill(int rows) {
        Data d = sample();
        d.charges.clear();
        for (int i = 1; i <= rows; i++) {
            d.charges.add(new Row("Charge number " + i, "100.00"));
        }
        return d;
    }

    private static String[] pages(String text) {
        return text.split("\f", -1);
    }

    private static int lineCount(String page) {
        return page.isEmpty() ? 0 : page.split("\n", -1).length - 1;
    }

    @Test
    void shortBillIsOnePage() {
        String text = InwardFinalBillTextRenderer.render(sample(), 40, 5, 3, 33, false);
        assertEquals(1, pages(text).length, text);
        assertFalse(text.contains("continued..."));
        assertFalse(text.contains("Page "));
    }

    @Test
    void longBillPagesWithinUsableLinesAndRepeatsMarginAndHeader() {
        int top = 5, bottom = 3, form = 33, usable = form - top - bottom;
        Data d = longBill(60);
        String text = InwardFinalBillTextRenderer.render(d, 40, top, bottom, form, true);
        assertTrue(text.endsWith("\f"));
        String[] p = pages(text.substring(0, text.length() - 1));
        assertTrue(p.length >= 3, "pages: " + p.length);
        for (int i = 0; i < p.length; i++) {
            String page = i == 0 ? p[i].substring(p[i].indexOf("\n") >= 0 ? 0 : 0) : p[i];
            String body = i == 0 ? p[i].replaceAll("^.*?\u001BC.", "") : p[i];
            assertTrue(lineCount(body) <= top + usable, "page " + (i + 1) + " has " + lineCount(body) + " lines");
            assertTrue(body.startsWith("\n".repeat(top)), "top margin on page " + (i + 1));
            assertFalse(body.startsWith("\n".repeat(top + 1)), "title follows margin on page " + (i + 1));
            if (i > 0) {
                assertTrue(body.contains("Page " + (i + 1) + "/" + p.length), body);
                assertTrue(body.contains("PARTICULAR"), body);
            }
            if (i < p.length - 1) {
                assertTrue(body.endsWith("continued...\n"), body);
            } else {
                assertFalse(body.contains("continued..."));
                assertTrue(body.contains("BILL PREPARED BY"));
            }
            for (String line : body.split("\n")) {
                assertTrue(line.length() <= 40, line);
            }
        }
        assertEquals(60, countOccurrences(text, "Charge number"));
    }

    @Test
    void exactLineCountsPerPage() {
        // width 40, top 2, bottom 2, form 20 => 16 usable lines.
        Data d = longBill(30);
        String text = InwardFinalBillTextRenderer.render(d, 40, 2, 2, 20, false);
        String[] p = pages(text);
        // first header: title, blank, bill, bht, date/time, patient, nic, 3 rules = 10 lines
        // page 1: 10 header + 5 rows + continued = 16 usable
        assertEquals(2 + 16, lineCount(p[0]), p[0]);
        assertEquals(5, countOccurrences(p[0], "Charge number"));
        // later page: 2 title lines + 3 header lines = 5 header; 10 rows + continued = 16
        assertEquals(2 + 16, lineCount(p[1]), p[1]);
        assertEquals(10, countOccurrences(p[1], "Charge number"));
        for (String page : p) {
            assertTrue(lineCount(page) <= 2 + 16);
        }
    }

    @Test
    void totalsBlockMovesWholeToNewPage() {
        // 26 usable: header 10 + 6 rows = 16, leaving 10 lines: the 11-line totals block cannot fit.
        Data d = longBill(6);
        String text = InwardFinalBillTextRenderer.render(d, 40, 2, 2, 30, false);
        String[] p = pages(text);
        assertEquals(2, p.length, text);
        assertFalse(p[0].contains("TOTAL AMOUNT"));
        assertTrue(p[0].contains("continued..."));
        assertTrue(p[1].contains("PAY TYPE"));
        assertTrue(p[1].contains("TOTAL AMOUNT"));
        assertTrue(p[1].contains("NET AMOUNT"));
        assertTrue(p[1].contains("BILL PREPARED BY : Cashier One"));
        assertTrue(p[1].contains("Page 2/2"));
    }

    @Test
    void wrappedRowsAndHeadingGroupsAreNeverSplit() {
        for (int rows = 1; rows <= 14; rows++) {
            Data d = sample();
            d.charges.clear();
            for (int i = 0; i < rows; i++) {
                d.charges.add(new Row("Plain " + i, "1.00"));
            }
            d.charges.add(Row.heading("Consultant Fee"));
            d.charges.add(Row.detail("Dr Longname Surname With Many Words (Cardiology Speciality Name)", "5.00"));
            d.charges.add(Row.detail("Dr B", "6.00"));
            d.charges.add(Row.subtotal("11.00"));
            String text = InwardFinalBillTextRenderer.render(d, 36, 2, 2, 20, false);
            String[] p = pages(text);
            for (int i = 0; i < p.length; i++) {
                String[] lines = p[i].split("\n", -1);
                for (int j = 0; j < lines.length; j++) {
                    if (lines[j].equals("Consultant Fee")) {
                        assertTrue(j + 1 < lines.length && lines[j + 1].startsWith("  Dr Longname"),
                                "heading alone at rows=" + rows + "\n" + text);
                        assertTrue(lines[j + 2].startsWith("    "), "doctor wrap split at rows=" + rows + "\n" + text);
                    }
                    if (lines[j].startsWith("    Speciality") || lines[j].startsWith("    Name)")
                            || lines[j].startsWith("    (Cardiology")) {
                        assertTrue(j > 0 && (lines[j - 1].startsWith("  Dr Longname") || lines[j - 1].startsWith("    ")),
                                "continuation separated from its row at rows=" + rows + "\n" + text);
                    }
                }
            }
        }
    }

    @Test
    void formLengthZeroMeansNoPaging() {
        String text = InwardFinalBillTextRenderer.render(longBill(80), 40, 5, 3, 0, false);
        assertEquals(1, pages(text).length);
        assertFalse(text.contains("continued..."));
        assertFalse(text.contains("Page "));
        assertEquals(80, countOccurrences(text, "Charge number"));
    }

    @Test
    void wideFormContinuationHeaderIsOneLine() {
        String text = InwardFinalBillTextRenderer.render(longBill(60), 80, 0, 0, 24, false);
        String[] lines = pages(text)[1].split("\n");
        assertTrue(lines[0].startsWith("OPD CARD"), lines[0]);
        assertTrue(lines[0].endsWith("Page 2/" + pages(text).length), lines[0]);
        assertEquals(80, lines[0].length());
    }

    @Test
    void creditDueLinesFollowNetAmount() {
        Data d = sample();
        d.dueFrom.add(new Row("DUE FROM Example Insurance Company Limited", "7,500.00"));
        String text = InwardFinalBillTextRenderer.render(d, 36, 0, 0, 0, false);
        String[] lines = text.split("\n");
        int n = 0;
        while (!lines[n].startsWith("NET AMOUNT")) {
            n++;
        }
        assertTrue(lines[n + 1].startsWith("DUE FROM Example"), text);
        assertTrue(lines[n + 1].endsWith("7,500.00"), text);
        for (String line : lines) {
            assertTrue(line.length() <= 36, line);
        }
    }

    private static int countOccurrences(String text, String what) {
        int c = 0;
        for (int i = text.indexOf(what); i >= 0; i = text.indexOf(what, i + 1)) {
            c++;
        }
        return c;
    }

    @Test
    void moneyFormatRoundsAndNeverPrintsNegativeZero() {
        java.text.DecimalFormat m = InwardFinalBillTextRenderer.newMoneyFormat();
        assertEquals("0.00", m.format(-0.004));
        assertEquals("0.00", m.format(-0.0));
        assertEquals("0.01", m.format(0.005));   // same DecimalFormat rounding as the screen (HALF_EVEN)
        assertEquals("-12.50", m.format(-12.5));
        assertEquals("1,234.57", m.format(1234.566));
        assertEquals("-0.01", m.format(-0.006));
    }

    @Test
    void threeFormBillOf71Lines() {
        // 71 content lines on 22 usable lines per form needs 3 forms.
        Data d = longBill(50);
        String text = InwardFinalBillTextRenderer.render(d, 40, 8, 3, 33, true);
        String[] p = pages(text.substring(0, text.length() - 1));
        assertTrue(p.length >= 3, "pages " + p.length);
        for (int i = 0; i < p.length; i++) {
            String body = i == 0 ? p[i].substring(p[i].indexOf("C") + 2) : p[i];
            assertTrue(lineCount(body) <= 33 - 3, "page " + (i + 1) + " has " + lineCount(body));
            assertTrue(body.startsWith("\n".repeat(8)));
        }
        assertEquals(50, countOccurrences(text, "Charge number"));
    }
}

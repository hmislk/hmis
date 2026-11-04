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
}

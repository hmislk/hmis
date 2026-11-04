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

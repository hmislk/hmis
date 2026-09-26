package com.divudi.core.util;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.Service;
import com.divudi.core.entity.WebUser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OpdBillTextRendererTest {

    private static Item item(String name) {
        Service s = new Service();
        s.setName(name);
        s.setPrintName("PN " + name);
        return s;
    }

    private static BillItem billItem(String name, double value) {
        BillItem bi = new BillItem();
        bi.setItem(item(name));
        bi.setQty(1.0);
        bi.setGrossValue(value);
        return bi;
    }

    private Bill sampleBill() {
        Person person = new Person();
        person.setName("Test Patient");
        person.setSex(Sex.Male);
        Patient patient = new Patient();
        patient.setPerson(person);

        Department dept = new Department();
        dept.setName("OPD - Diagnostic Centre");

        WebUser user = new WebUser();
        user.setName("cashier1");

        BilledBill b = new BilledBill();
        b.setDeptId("OPDDC/26/000001");
        b.setDepartment(dept);
        b.setPatient(patient);
        b.setPaymentMethod(PaymentMethod.Cash);
        b.setCreater(user);
        List<BillItem> items = new ArrayList<>();
        items.add(billItem("MRI - BRAIN", 25000));
        items.add(billItem("REPORTING - Dr. (MR) Longname Consultant Radiologist", 5000));
        b.setBillItems(items);
        b.setTotal(30000);
        b.setNetTotal(30000);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Colombo"));
        c.set(2026, Calendar.SEPTEMBER, 21, 11, 25, 0);
        b.setCreatedAt(c.getTime());
        return b;
    }

    private static OpdBillTextRenderer.Layout layout(int left, int right) {
        return new OpdBillTextRenderer.Layout()
                .setTopMarginLines(2)
                .setLeftMarginColumns(left)
                .setRightBorderColumn(right)
                .setEmitEscP(false);
    }

    private static String render(Bill b, boolean duplicate, OpdBillTextRenderer.Layout l) {
        return OpdBillTextRenderer.render(Collections.singletonList(b), null,
                duplicate, "user1", "2026-09-27 10:00 AM", false, l);
    }

    private static List<String> lines(String out) {
        return Arrays.asList(out.replace("\f", "").split("\n", -1));
    }

    @Test
    public void noLineGoesBeyondTheRightBorder() {
        for (int right : new int[]{40, 50, 60}) {
            for (int left : new int[]{0, 3}) {
                String out = render(sampleBill(), true, layout(left, right));
                for (String line : lines(out)) {
                    assertTrue(line.length() <= right,
                            "line exceeds border " + right + " (left " + left + "): [" + line + "]");
                }
            }
        }
    }

    @Test
    public void valuesEndExactlyOnTheRightBorder() {
        String out = render(sampleBill(), false, layout(2, 50));
        boolean foundItem = false, foundTotal = false, foundCount = false;
        for (String line : lines(out)) {
            if (line.contains("MRI - BRAIN")) {
                assertTrue(line.endsWith("25,000.00"), line);
                assertEquals(50, line.length(), line);
                foundItem = true;
            }
            if (line.trim().startsWith("Total:")) {
                assertTrue(line.endsWith("30,000.00"), line);
                assertEquals(50, line.length(), line);
                foundTotal = true;
            }
            if (line.trim().startsWith("No of Items:")) {
                assertTrue(line.endsWith("2"), line);
                assertEquals(50, line.length(), line);
                foundCount = true;
            }
        }
        assertTrue(foundItem && foundTotal && foundCount, out);
    }

    @Test
    public void changingRightBorderMovesValues() {
        String out = render(sampleBill(), false, layout(0, 44));
        for (String line : lines(out)) {
            if (line.startsWith("Total:")) {
                assertEquals(44, line.length(), line);
                return;
            }
        }
        fail("no Total line: " + out);
    }

    @Test
    public void longItemNameWrapsWithoutBreakingValueColumn() {
        String out = render(sampleBill(), false, layout(0, 50));
        List<String> ls = lines(out);
        int idx = -1;
        for (int i = 0; i < ls.size(); i++) {
            if (ls.get(i).contains("REPORTING")) {
                idx = i;
            }
        }
        assertTrue(idx >= 0, out);
        assertTrue(ls.get(idx).endsWith("5,000.00"), ls.get(idx));
        assertEquals(50, ls.get(idx).length());
        // continuation line carries the rest of the name, not a value
        assertTrue(ls.get(idx + 1).contains("Radiologist"), ls.get(idx + 1));
        assertFalse(ls.get(idx + 1).contains(".00"), ls.get(idx + 1));
    }

    @Test
    public void headerFieldsReplicateTheFiveFiveCustom3Bill() {
        String out = render(sampleBill(), false, layout(0, 50));
        assertTrue(out.contains("OPD - DIAGNOSTIC CENTRE"), out);
        assertTrue(out.contains("Name :"), out);
        assertTrue(out.contains("Age/Sex :"), out);
        assertTrue(out.contains("Bill Date:"), out);
        assertTrue(out.contains("2026-09-21"), out);
        assertTrue(out.contains("Bill Time:"), out);
        assertTrue(out.contains("11:25 AM"), out);
        assertTrue(out.contains("Bill No:"), out);
        assertTrue(out.contains("OPDDC/26/000001"), out);
        assertTrue(out.contains("Payment Method : Cash"), out);
        assertTrue(out.contains("NO"), out);
        assertTrue(out.contains("ITEM NAME"), out);
        assertTrue(out.contains("QTY"), out);
        assertTrue(out.contains("VALUE"), out);
        assertTrue(out.contains("Billed By : cashier1"), out);
        assertFalse(out.contains("**Duplicate**"), out);
        assertFalse(out.contains("Printed By"), out);
        assertFalse(out.contains("Discount"), out);
    }

    @Test
    public void rowWithoutRightColumnUsesFullWidth() {
        Bill b = sampleBill();
        b.setDeptId("OPDDC//26/0123456789");
        String out = render(b, false, layout(0, 40));
        assertTrue(out.contains("Bill No:   OPDDC//26/0123456789\n"), out);
    }

    @Test
    public void discountRowsOnlyWhenDiscounted() {
        Bill b = sampleBill();
        b.setDiscount(3000);
        b.setNetTotal(27000);
        String out = render(b, false, layout(0, 50));
        assertTrue(out.contains("Discount:"), out);
        assertTrue(out.contains("-3,000.00"), out);
        assertTrue(out.contains("Net Total:"), out);
        assertTrue(out.contains("27,000.00"), out);
    }

    @Test
    public void duplicateAndCancelledMarkers() {
        Bill b = sampleBill();
        b.setCancelled(true);
        String out = render(b, true, layout(0, 50));
        assertTrue(out.contains("**Duplicate** **Cancelled**"), out);
        assertTrue(out.contains("Printed By : user1  2026-09-27 10:00 AM"), out);
    }

    @Test
    public void printNameUsedWhenConfigured() {
        String out = OpdBillTextRenderer.render(Collections.singletonList(sampleBill()), null,
                false, "", "", true, layout(0, 50));
        assertTrue(out.contains("PN MRI - BRAIN"), out);
    }

    @Test
    public void paymentRowsRenderedBesideBilledBy() {
        Payment p = new Payment();
        p.setPaymentMethod(PaymentMethod.Card);
        p.setCreditCardRefNo("1234");
        p.setPaidValue(30000);
        String out = OpdBillTextRenderer.render(Collections.singletonList(sampleBill()),
                bill -> Collections.singletonList(p), false, "", "", false, layout(0, 50));
        boolean found = false;
        for (String line : lines(out)) {
            if (line.startsWith("Card (1234)")) {
                assertTrue(line.endsWith("30,000.00"), line);
                assertEquals(50, line.length());
                found = true;
            }
        }
        assertTrue(found, out);
    }

    @Test
    public void nonBreakingSpacesAndNonAsciiAreMadePrinterSafe() {
        assertEquals("Dr. (MR) FIRST LAST", OpdBillTextRenderer.safe("Dr. (MR)  FIRST LAST "));
        assertEquals("A?B", OpdBillTextRenderer.safe("AඅB"));
        Bill b = sampleBill();
        b.getBillItems().get(1).getItem().setName("REPORTING - Dr. (MR) FIRSTNAME LASTNAME CONSULTANT");
        String out = render(b, false, layout(0, 50));
        assertFalse(out.contains(" "), out);
        for (char c : out.toCharArray()) {
            assertTrue(c == '\n' || c == '\f' || (c >= 0x20 && c <= 0x7E), "non-printable char " + (int) c);
        }
    }

    @Test
    public void eachBillOnItsOwnForm() {
        String out = OpdBillTextRenderer.render(Arrays.asList(sampleBill(), sampleBill()), null,
                false, "", "", false, layout(0, 50));
        assertEquals(2, out.chars().filter(ch -> ch == '\f').count());
    }

    @Test
    public void escPCodesFollowSettings() {
        OpdBillTextRenderer.Layout l = new OpdBillTextRenderer.Layout();
        String out = render(sampleBill(), false, l);
        assertTrue(out.startsWith("\u001B@\u001Bx\u0001\u001BM"), "12 CPI by default");
        assertFalse(out.contains("\u001BC"), "no form length unless configured");

        l.setCharactersPerInch(10).setFormLengthLines(33);
        out = render(sampleBill(), false, l);
        assertTrue(out.startsWith("\u001B@\u001Bx\u0001\u001BP\u001BC!"), "10 CPI + ESC C 33");

        l.setEmitEscP(false);
        out = render(sampleBill(), false, l);
        assertFalse(out.contains("\u001B"));
    }

    @Test
    public void topAndLeftMarginsApplied() {
        String out = render(sampleBill(), false, layout(5, 55));
        assertTrue(out.startsWith("\n\n"), "two top margin lines");
        for (String line : lines(out)) {
            if (!line.isEmpty()) {
                assertTrue(line.startsWith("     "), "left margin: [" + line + "]");
            }
        }
    }
}

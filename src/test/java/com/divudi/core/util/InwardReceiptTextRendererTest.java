package com.divudi.core.util;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InwardReceiptTextRendererTest {

    private Bill sampleBill() {
        Person person = new Person();
        person.setName("H K Isali Lisansa");
        person.setSex(com.divudi.core.data.Sex.Female);
        person.setAddress("Rathgama");
        person.setPhone("0770000000");

        Patient patient = new Patient();
        patient.setPerson(person);

        AdmissionType at = new AdmissionType();
        at.setName("BHT");

        PatientEncounter pe = new PatientEncounter();
        pe.setBhtNo("BHT/57939");
        pe.setAdmissionType(at);
        pe.setPatient(patient);

        Department dept = new Department();
        dept.setPrintingName("Galle Co-operative Hospital Ltd.");
        dept.setAddress("No.65, H.W. Amarasooriya Mawatha, Galle");
        dept.setTelephone1("091-2234270");

        Person cashierPerson = new Person();
        cashierPerson.setName("Ziyana");
        WebUser cashier = new WebUser();
        cashier.setWebUserPerson(cashierPerson);

        BilledBill b = new BilledBill();
        b.setDeptId("Inward/26/052052");
        b.setPatientEncounter(pe);
        b.setDepartment(dept);
        b.setPaymentMethod(PaymentMethod.Cash);
        b.setTotal(10000.0);
        b.setCreater(cashier);
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.SEPTEMBER, 9, 21, 39, 0);
        b.setCreatedAt(c.getTime());
        return b;
    }

    @Test
    public void everyLineIsAtMost40CharsWide() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false, null);
        for (String line : out.split("\n", -1)) {
            assertTrue(line.length() <= InwardReceiptTextRenderer.WIDTH,
                    "line too wide (" + line.length() + "): [" + line + "]");
        }
    }

    @Test
    public void containsKeyFields() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false, null);
        assertTrue(out.contains("Deposit Receipt"));
        assertTrue(out.contains("BHT/57939"));
        assertTrue(out.contains("Inward/26/052052"));
        assertTrue(out.contains("10,000.00"));
        assertTrue(out.contains("Cashier : Ziyana"));
        assertTrue(out.contains("H K Isali Lisansa"));
    }

    @Test
    public void headerPrintedWhenNotPreprinted() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false, null);
        assertTrue(out.contains("Galle Co-operative Hospital Ltd."));
    }

    @Test
    public void headerSuppressedAndTopMarginAppliedWhenPreprinted() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, true, 5, false, null);
        assertFalse(out.contains("Galle Co-operative Hospital Ltd."));
        assertTrue(out.startsWith("\n\n\n\n\n"),
                "expected 5 leading blank lines");
    }

    @Test
    public void duplicateMarkerShownWhenDuplicate() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                true, false, 0, false, null);
        assertTrue(out.contains("**Duplicate**"));
    }

    @Test
    public void escPPrologueAndFormFeedWhenEmitEscP() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, true, null);
        assertEquals(0x1B, out.charAt(0), "ESC @ init expected at start");
        assertEquals('@', out.charAt(1));
        assertTrue(out.endsWith("\f"), "form feed expected at end");
    }

    @Test
    public void noEscPBytesWhenEmitEscPFalse() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false, null);
        assertFalse(out.contains("\u001B"));
        assertFalse(out.contains("\f"));
    }

    @Test
    public void multiplePaymentBreakdownShownForSplitTender() {
        Bill bill = sampleBill();
        bill.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);

        Payment cash = new Payment();
        cash.setPaymentMethod(PaymentMethod.Cash);
        cash.setPaidValue(4000.0);

        Payment card = new Payment();
        card.setPaymentMethod(PaymentMethod.Card);
        card.setPaidValue(6000.0);
        card.setCreditCardRefNo("REF123");

        String out = InwardReceiptTextRenderer.render(bill, "Deposit Receipt",
                false, false, 0, false, Arrays.asList(cash, card));

        assertTrue(out.contains("4,000.00"), "cash tender amount missing");
        assertTrue(out.contains("6,000.00"), "card tender amount missing");
        assertTrue(out.contains("REF123"), "card reference missing");
    }

    @Test
    public void noBreakdownEmittedWhenMultiplePaymentsIsNullOrNotMultiMethod() {
        Bill bill = sampleBill(); // PaymentMethod.Cash
        String out = InwardReceiptTextRenderer.render(bill, "Deposit Receipt",
                false, false, 0, false, null);
        // single-payment bill: no per-tender breakdown rule should be inserted
        assertEquals(1, countOccurrences(out, "----------------------------------------"),
                "unexpected breakdown rule on a single-payment bill");
    }

    @Test
    public void dateAndTimeAreFormattedInColomboTimeRegardlessOfDefaultTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            // Pick a default zone far from Colombo (+05:30) so a missing
            // explicit zone would visibly shift the printed date/time.
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Colombo"));
            c.set(2026, Calendar.SEPTEMBER, 9, 23, 45, 0);
            Bill bill = sampleBill();
            bill.setCreatedAt(c.getTime());

            String out = InwardReceiptTextRenderer.render(bill, "Deposit Receipt",
                    false, false, 0, false, null);

            assertTrue(out.contains("09/Sep/2026"),
                    "expected the Colombo-local date, not the JVM-default-zone date");
            assertTrue(out.contains("11:45 pm"),
                    "expected the Colombo-local time, not the JVM-default-zone time");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void headingForMapsInwardBillTypesToInwardTitles() {
        assertEquals("Inward Deposit",
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_DEPOSIT));
        assertEquals("Inward Payment",
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_PAYMENT));
        assertEquals("Inward Deposit Refund",
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_DEPOSIT_REFUND));
        assertEquals("Inward Deposit Cancellation",
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION));
        assertEquals("Inward Payment Cancellation",
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_PAYMENT_CANCELLATION));
        assertEquals("Inward Payment Refund Cancellation",
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_PAYMENT_REFUND_CANCELLATION));
        assertEquals("Inward Payment",
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT));
        assertEquals("Inward Receipt", InwardReceiptTextRenderer.headingFor(null));
    }

    @Test
    public void admissionTypeAddressAndPhoneRowsShownByDefault() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Inward Deposit",
                false, false, 0, false, null);
        assertTrue(out.contains("Admission Type : BHT"));
        assertTrue(out.contains("Address        : Rathgama"));
        assertTrue(out.contains("Phone          : 0770000000"));
    }

    @Test
    public void admissionTypeAddressAndPhoneRowsHiddenWhenFlagsOff() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Inward Deposit",
                false, false, 0, false, null, false, false, false);
        assertFalse(out.contains("Admission Type"));
        assertFalse(out.contains("Address"));
        assertFalse(out.contains("Rathgama"));
        assertFalse(out.contains("Phone"));
        assertFalse(out.contains("0770000000"));
        assertTrue(out.contains("Name           : H K Isali Lisansa"));
        assertTrue(out.contains("Age / Gender   : "));
        assertTrue(out.contains("BHT No         : BHT/57939"));
    }

    @Test
    public void eachShowFlagControlsOnlyItsOwnRow() {
        String noAdmission = InwardReceiptTextRenderer.render(sampleBill(), "Inward Deposit",
                false, false, 0, false, null, false, true, true);
        assertFalse(noAdmission.contains("Admission Type"));
        assertTrue(noAdmission.contains("Rathgama"));
        assertTrue(noAdmission.contains("0770000000"));

        String noAddress = InwardReceiptTextRenderer.render(sampleBill(), "Inward Deposit",
                false, false, 0, false, null, true, false, true);
        assertTrue(noAddress.contains("Admission Type"));
        assertFalse(noAddress.contains("Rathgama"));
        assertTrue(noAddress.contains("0770000000"));

        String noPhone = InwardReceiptTextRenderer.render(sampleBill(), "Inward Deposit",
                false, false, 0, false, null, true, true, false);
        assertTrue(noPhone.contains("Admission Type"));
        assertTrue(noPhone.contains("Rathgama"));
        assertFalse(noPhone.contains("0770000000"));
    }

    @Test
    public void preprintedCompactLayoutPutsNameDirectlyUnderTitleRule() {
        String out = InwardReceiptTextRenderer.render(sampleBill(),
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_PAYMENT),
                true, true, 3, false, null, false, false, false);
        String[] lines = out.split("\n", -1);
        assertEquals("", lines[0]);
        assertEquals("", lines[1]);
        assertEquals("", lines[2]);
        assertEquals("Inward Payment **Duplicate**", lines[3].trim());
        assertEquals("----------------------------------------", lines[4]);
        assertTrue(lines[5].startsWith("Name           : "),
                "Name row must follow the title rule directly: [" + lines[5] + "]");
        assertTrue(lines[6].startsWith("Age / Gender   : "));
        assertFalse(out.contains("Admission Type"));
        assertFalse(out.contains("Galle Co-operative Hospital Ltd."));
        assertFalse(out.contains("091-2234270"));
    }

    @Test
    public void longHeadingWithMarkersWrapsInsteadOfClipping() {
        Bill bill = sampleBill();
        bill.setCancelled(true);
        String out = InwardReceiptTextRenderer.render(bill,
                InwardReceiptTextRenderer.headingFor(BillTypeAtomic.INWARD_PAYMENT_REFUND_CANCELLATION),
                true, true, 0, false, null, false, false, false);
        String[] lines = out.split("\n", -1);
        assertEquals("Inward Payment Refund Cancellation", lines[0].trim());
        assertEquals("**Duplicate** **Cancelled**", lines[1].trim());
        assertEquals("----------------------------------------", lines[2]);
        for (String line : lines) {
            assertTrue(line.length() <= InwardReceiptTextRenderer.WIDTH,
                    "line too wide (" + line.length() + "): [" + line + "]");
        }
    }

    @Test
    public void shortHeadingWithMarkersStaysOnOneLine() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Inward Deposit",
                true, true, 0, false, null, false, false, false);
        String[] lines = out.split("\n", -1);
        assertEquals("Inward Deposit **Duplicate**", lines[0].trim());
        assertEquals("----------------------------------------", lines[1]);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}

package com.divudi.core.util;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import java.util.Calendar;
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
                false, false, 0, false);
        for (String line : out.split("\n", -1)) {
            assertTrue(line.length() <= InwardReceiptTextRenderer.WIDTH,
                    "line too wide (" + line.length() + "): [" + line + "]");
        }
    }

    @Test
    public void containsKeyFields() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false);
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
                false, false, 0, false);
        assertTrue(out.contains("Galle Co-operative Hospital Ltd."));
    }

    @Test
    public void headerSuppressedAndTopMarginAppliedWhenPreprinted() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, true, 5, false);
        assertFalse(out.contains("Galle Co-operative Hospital Ltd."));
        assertTrue(out.startsWith("\n\n\n\n\n"),
                "expected 5 leading blank lines");
    }

    @Test
    public void duplicateMarkerShownWhenDuplicate() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                true, false, 0, false);
        assertTrue(out.contains("**Duplicate**"));
    }

    @Test
    public void escPPrologueAndFormFeedWhenEmitEscP() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, true);
        assertEquals(0x1B, out.charAt(0), "ESC @ init expected at start");
        assertEquals('@', out.charAt(1));
        assertTrue(out.endsWith("\f"), "form feed expected at end");
    }

    @Test
    public void noEscPBytesWhenEmitEscPFalse() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false);
        assertFalse(out.contains("\u001B"));
        assertFalse(out.contains("\f"));
    }
}

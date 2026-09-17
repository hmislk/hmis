package com.divudi.bean.common;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfControllerFinalBillSnapshotTest {

    @Test
    public void createFinalBillSnapshotPdfProducesNonEmptyPdfBytes() throws Exception {
        Bill bill = new BilledBill();
        bill.setDeptId("TEST/001");
        bill.setNetTotal(1000.0);
        bill.setPaidAmount(600.0);
        bill.setApproveAt(new Date());

        Person person = new Person();
        person.setName("Test Patient");
        Patient patient = new Patient();
        patient.setPerson(person);
        PatientEncounter pe = new PatientEncounter();
        pe.setPatient(patient);
        bill.setPatientEncounter(pe);

        List<Map.Entry<String, Double>> categoryTotals = new ArrayList<>();
        categoryTotals.add(new AbstractMap.SimpleEntry<>("Room Charges", 500.0));
        categoryTotals.add(new AbstractMap.SimpleEntry<>("Pharmacy", 500.0));

        Bill payment = new BilledBill();
        payment.setNetTotal(600.0);
        payment.setCreatedAt(new Date());
        List<Bill> paymentBills = new ArrayList<>();
        paymentBills.add(payment);

        PdfController pdfController = new PdfController();
        byte[] pdfBytes = pdfController.createFinalBillSnapshotPdf(bill, categoryTotals, paymentBills);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    public void createFinalBillSnapshotPdfHandlesNullPatientEncounterAndNullLists() throws Exception {
        Bill bill = new BilledBill();
        bill.setDeptId("TEST/002");
        bill.setNetTotal(1000.0);
        bill.setPaidAmount(600.0);
        bill.setApproveAt(new Date());
        bill.setPatientEncounter(null);  // Null patient encounter

        PdfController pdfController = new PdfController();
        byte[] pdfBytes = pdfController.createFinalBillSnapshotPdf(bill, null, null);  // Null lists

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}

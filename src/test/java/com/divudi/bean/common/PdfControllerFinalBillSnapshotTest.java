package com.divudi.bean.common;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
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
        pe.setBhtNo("BHT12345");
        bill.setPatientEncounter(pe);

        Institution institution = new Institution();
        institution.setName("Test Snapshot Hospital");
        bill.setInstitution(institution);

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

        String text = extractText(pdfBytes);
        assertTrue(text.contains("Test Snapshot Hospital"), "Expected institution name in snapshot PDF text");
        assertTrue(text.contains("BHT12345"), "Expected BHT number in snapshot PDF text");
        assertTrue(text.contains("TEST/001"), "Expected bill number in snapshot PDF text");
    }

    private String extractText(byte[] pdfBytes) throws Exception {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))).append('\n');
            }
            return sb.toString();
        }
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

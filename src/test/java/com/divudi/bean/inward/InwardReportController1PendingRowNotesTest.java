package com.divudi.bean.inward;

import com.divudi.core.data.inward.AdmissionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issues #23793 / #25221: Discharge Date leaves currently admitted patients out
 * of the Credit Company Debtor reports, and the export says which date selected
 * the rows that have no final bill yet.
 */
class InwardReportController1PendingRowNotesTest {

    @Test
    void onlyDischargeDateExcludesAdmittedPatients() {
        assertTrue(InwardReportController1.excludesCurrentlyAdmitted("dischargeDate"));
        assertFalse(InwardReportController1.excludesCurrentlyAdmitted("createdAt"));
        assertFalse(InwardReportController1.excludesCurrentlyAdmitted("admissionDate"));
        assertFalse(InwardReportController1.excludesCurrentlyAdmitted("finalBillCreatedAt"));
        assertFalse(InwardReportController1.excludesCurrentlyAdmitted(null));
    }

    @Test
    void paymentBasisNamesTheDatesOfBothPendingRowKinds() {
        List<String> notes = InwardReportController1.pendingRowDateNotes(
                "createdAt", AdmissionStatus.ANY_STATUS, true, true);
        assertEquals(2, notes.size());
        assertTrue(notes.get(0).startsWith("(Active) rows are currently admitted patients"));
        assertTrue(notes.get(0).endsWith("They are selected by Admission Date, not Payment/Bill Date."));
        assertTrue(notes.get(1).startsWith("(Discharged - Bill Pending) rows are selected by Discharge Date, not Payment/Bill Date"));
    }

    @Test
    void admissionBasisNeedsNoDateNote() {
        List<String> notes = InwardReportController1.pendingRowDateNotes(
                "admissionDate", AdmissionStatus.ANY_STATUS, true, true);
        assertEquals(1, notes.size());
        assertFalse(notes.get(0).contains("selected by"));
    }

    @Test
    void groupedExportDateBasisLabel() {
        assertEquals("Discharge Date (currently admitted patients not included)",
                CreditCompanyDebtorGroupedReportController.exportDateBasisLabel("dischargeDate", false));
        assertEquals("Payment/Bill Date; (Active) rows by Admission Date",
                CreditCompanyDebtorGroupedReportController.exportDateBasisLabel("createdAt", true));
        assertEquals("Payment/Bill Date",
                CreditCompanyDebtorGroupedReportController.exportDateBasisLabel("createdAt", false));
        assertEquals("Admission Date",
                CreditCompanyDebtorGroupedReportController.exportDateBasisLabel("admissionDate", true));
    }

    @Test
    void dischargeBasisSaysAdmittedPatientsAreLeftOut() {
        List<String> notes = InwardReportController1.pendingRowDateNotes(
                "dischargeDate", AdmissionStatus.ANY_STATUS, false, true);
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).startsWith("Currently admitted patients are not included"));
        assertTrue(InwardReportController1.pendingRowDateNotes(
                "dischargeDate", AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED, false, false).isEmpty());
    }

    @Test
    void noPendingRowsNoNote() {
        assertTrue(InwardReportController1.pendingRowDateNotes(
                "finalBillCreatedAt", AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED, false, false).isEmpty());
    }
}

package com.divudi.bean.inward;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #25209: the Credit Company Debtor/Commitment export header must name
 * the Date Basis actually used, matching the report's Date Basis menu.
 */
class InwardReportController1DateBasisLabelTest {

    @Test
    void everyMenuOptionHasItsOwnLabel() {
        assertEquals("Payment/Bill Date", InwardReportController1.dateBasisLabel("createdAt"));
        assertEquals("Discharge Date", InwardReportController1.dateBasisLabel("dischargeDate"));
        assertEquals("Admission Date", InwardReportController1.dateBasisLabel("admissionDate"));
        assertEquals("Final Bill Created Date", InwardReportController1.dateBasisLabel("finalBillCreatedAt"));
    }

    @Test
    void unsetBasisFallsBackToPaymentBillDate() {
        assertEquals("Payment/Bill Date", InwardReportController1.dateBasisLabel(null));
    }
}

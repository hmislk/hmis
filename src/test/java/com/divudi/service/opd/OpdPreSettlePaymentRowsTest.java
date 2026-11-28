package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26261: in a Multiple Payment Methods settle, each row keeps its own
 * paid value, and a row changes only what its own method owns.
 */
class OpdPreSettlePaymentRowsTest {

    private ComponentDetail row(PaymentMethod pm) {
        ComponentDetail cd = new ComponentDetail();
        cd.setPaymentMethod(pm);
        cd.setPaymentMethodData(new PaymentMethodData());
        return cd;
    }

    private Patient patient(double balance) {
        Patient patient = new Patient();
        patient.setRunningBalance(balance);
        return patient;
    }

    @Test
    void patientDepositRowKeepsItsPaidValueAndLowersTheBalance() {
        ComponentDetail cash = row(PaymentMethod.Cash);
        cash.getPaymentMethodData().getCash().setTotalValue(300.0);
        ComponentDetail deposit = row(PaymentMethod.PatientDeposit);
        deposit.getPaymentMethodData().getPatient_deposit().setTotalValue(200.0);
        Patient patient = patient(1000.0);
        OpdPreSettleRequest request = new OpdPreSettleRequest();

        Payment cashPayment = new Payment();
        OpdPreSettlePaymentRows.fillMultipleRow(cashPayment, cash, patient, request);
        Payment depositPayment = new Payment();
        OpdPreSettlePaymentRows.fillMultipleRow(depositPayment, deposit, patient, request);

        assertEquals(300.0, cashPayment.getPaidValue(), 0.001);
        assertEquals(200.0, depositPayment.getPaidValue(), 0.001);
        assertEquals(800.0, patient.getRunningBalance(), 0.001);
        assertTrue(request.getPatientsToSave().contains(patient));
        assertTrue(request.getStaffCredits().isEmpty());

        request.restore();
        assertEquals(1000.0, patient.getRunningBalance(), 0.001);
    }

    @Test
    void slipRowKeepsItsPaidValue() {
        ComponentDetail slip = row(PaymentMethod.Slip);
        slip.getPaymentMethodData().getSlip().setTotalValue(150.0);
        Patient patient = patient(500.0);
        OpdPreSettleRequest request = new OpdPreSettleRequest();
        Payment p = new Payment();

        OpdPreSettlePaymentRows.fillMultipleRow(p, slip, patient, request);

        assertEquals(150.0, p.getPaidValue(), 0.001);
        assertEquals(500.0, patient.getRunningBalance(), 0.001);
        assertTrue(request.getStaffCredits().isEmpty());
    }

    @Test
    void ewalletAndAgentRowsDoNotTouchThePatientBalance() {
        Patient patient = patient(500.0);
        OpdPreSettleRequest request = new OpdPreSettleRequest();

        ComponentDetail ewallet = row(PaymentMethod.ewallet);
        ewallet.getPaymentMethodData().getEwallet().setTotalValue(75.0);
        ewallet.getPaymentMethodData().getPatient_deposit().setTotalValue(999.0);
        Payment ewalletPayment = new Payment();
        OpdPreSettlePaymentRows.fillMultipleRow(ewalletPayment, ewallet, patient, request);

        ComponentDetail agent = row(PaymentMethod.Agent);
        agent.getPaymentMethodData().getCredit().setTotalValue(60.0);
        agent.getPaymentMethodData().getPatient_deposit().setTotalValue(999.0);
        Payment agentPayment = new Payment();
        OpdPreSettlePaymentRows.fillMultipleRow(agentPayment, agent, patient, request);

        assertEquals(75.0, ewalletPayment.getPaidValue(), 0.001);
        assertEquals(60.0, agentPayment.getPaidValue(), 0.001);
        assertEquals(500.0, patient.getRunningBalance(), 0.001);
        assertTrue(request.getPatientsToSave().isEmpty());
        assertTrue(request.getStaffCredits().isEmpty());
    }

    @Test
    void staffRowKeepsItsPaidValueAndAddsOneStaffCredit() {
        ComponentDetail staffRow = row(PaymentMethod.Staff);
        staffRow.getPaymentMethodData().getStaffCredit().setTotalValue(40.0);
        staffRow.getPaymentMethodData().getStaffCredit().setToStaff(new Staff());
        Patient patient = patient(500.0);
        OpdPreSettleRequest request = new OpdPreSettleRequest();
        Payment p = new Payment();

        OpdPreSettlePaymentRows.fillMultipleRow(p, staffRow, patient, request);

        assertEquals(40.0, p.getPaidValue(), 0.001);
        assertEquals(1, request.getStaffCredits().size());
        assertEquals(500.0, patient.getRunningBalance(), 0.001);
    }
}

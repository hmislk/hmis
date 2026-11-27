package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PreBill;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26246: the settle runs as one unit. A failure in the payment step
 * stops the settle with an exception (so the container rolls the transaction
 * back) and the caller's objects are put back so the cashier can retry.
 * The rollback itself is done by the container; these tests show the writer
 * does not catch the failure and does not go on, and that the restore works.
 */
class OpdPreSettleWriterTest {

    private static class RecordingBillFacade extends BillFacade {

        final List<Bill> created = new ArrayList<>();
        final List<Bill> edited = new ArrayList<>();
        long nextId = 100;

        @Override
        public void create(Bill b) {
            b.setId(nextId++);
            created.add(b);
        }

        @Override
        public void edit(Bill b) {
            edited.add(b);
        }
    }

    private static class FailingPaymentFacade extends PaymentFacade {

        int attempts;

        @Override
        public void create(Payment p) {
            attempts++;
            throw new IllegalStateException("forced failure in the payment step");
        }
    }

    private static class RecordingPatientFacade extends PatientFacade {

        final List<Patient> edited = new ArrayList<>();

        @Override
        public void edit(Patient p) {
            edited.add(p);
        }
    }

    private OpdPreSettleRequest request(Bill sale, Bill pre, Patient patient) {
        OpdPreSettleRequest request = new OpdPreSettleRequest();
        request.setSaleBill(sale);
        request.setPreBill(pre);
        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.Cash);
        request.setPayments(new ArrayList<>(List.of(payment)));
        if (patient != null) {
            request.getPatientsToSave().add(patient);
        }
        return request;
    }

    @Test
    void failureInThePaymentStepStopsTheSettleAndTheBillsAreLeftAsTheyWere() {
        Bill sale = new BilledBill();
        Bill pre = new PreBill();
        Patient patient = new Patient();
        patient.setRunningBalance(500.0);
        OpdPreSettleRequest request = request(sale, pre, patient);
        request.captureBillState();
        // What the controller does while it builds the payments.
        request.addUndo(() -> patient.setRunningBalance(500.0));
        patient.setRunningBalance(200.0);

        RecordingBillFacade billFacade = new RecordingBillFacade();
        FailingPaymentFacade paymentFacade = new FailingPaymentFacade();
        RecordingPatientFacade patientFacade = new RecordingPatientFacade();
        OpdPreSettleWriter writer = new OpdPreSettleWriter();
        writer.billFacade = billFacade;
        writer.paymentFacade = paymentFacade;
        writer.patientFacade = patientFacade;

        assertThrows(IllegalStateException.class, () -> writer.settle(request));

        // The writer did not go past the failed payment: no link, no patient save, no individual bill.
        assertEquals(1, paymentFacade.attempts);
        assertTrue(patientFacade.edited.isEmpty());
        assertTrue(billFacade.edited.isEmpty());
        assertEquals(1, billFacade.created.size());
        assertNull(pre.getReferenceBill());

        // The bill got an id from the insert the database now rolls back; the retry needs it gone.
        request.restore();
        assertNull(sale.getId());
        assertNull(pre.getReferenceBill());
        assertEquals(500.0, patient.getRunningBalance());
    }

    @Test
    void restoreGivesBackTheLinksOfTheIndividualPreBills() {
        Bill sale = new BilledBill();
        Bill pre = new PreBill();
        Bill individual = new PreBill();
        Bill earlier = new BilledBill();
        individual.setReferenceBill(earlier);
        OpdPreSettleRequest request = request(sale, pre, null);
        request.getPreIndividualBills().add(individual);
        request.captureBillState();

        sale.setId(7L);
        pre.setReferenceBill(sale);
        individual.setReferenceBill(new BilledBill());

        request.restore();

        assertNull(sale.getId());
        assertNull(pre.getReferenceBill());
        assertSame(earlier, individual.getReferenceBill());
    }

    @Test
    void settleWithoutABillIsABusinessError() {
        OpdPreSettleWriter writer = new OpdPreSettleWriter();
        assertThrows(OpdPreSettleException.class, () -> writer.settle(new OpdPreSettleRequest()));
    }
}

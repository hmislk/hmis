package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientDepositFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.service.PatientDepositService;
import com.divudi.service.StaffService;
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
    void failedSettleWithStaffMethodThenRetryAddsTheStaffCreditOnce() {
        Staff staff = new Staff();
        staff.setCurrentCreditValue(100.0);
        Bill sale = new BilledBill();
        Bill pre = new PreBill();

        // The stub adds the credit as StaffService does; the database rollback does not undo the object.
        StaffService staffService = new StaffService() {
            @Override
            public void updateStaffCredit(Staff s, double value) {
                s.setCurrentCreditValue(s.getCurrentCreditValue() + value);
            }
        };
        final boolean[] fail = {true};
        RecordingBillFacade billFacade = new RecordingBillFacade() {
            @Override
            public void edit(Bill b) {
                if (fail[0]) {
                    throw new IllegalStateException("forced failure after the staff credit");
                }
                super.edit(b);
            }
        };
        OpdPreSettleWriter writer = new OpdPreSettleWriter();
        writer.billFacade = billFacade;
        writer.paymentFacade = new PaymentFacade() {
            @Override
            public void create(Payment p) {
            }
        };
        writer.patientFacade = new RecordingPatientFacade();
        writer.staffService = staffService;

        OpdPreSettleRequest first = request(sale, pre, null);
        first.captureBillState();
        first.addStaffCredit(staff, 40.0);
        assertThrows(IllegalStateException.class, () -> writer.settle(first));
        first.restore();
        assertEquals(100.0, staff.getCurrentCreditValue());

        // The cashier settles again.
        fail[0] = false;
        OpdPreSettleRequest second = request(sale, pre, null);
        second.captureBillState();
        second.addStaffCredit(staff, 40.0);
        writer.settle(second);
        assertEquals(140.0, staff.getCurrentCreditValue());
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

    /** Stub deposit service: one deposit record, and a record of what was taken from it. */
    private static class StubDepositService extends PatientDepositService {

        final PatientDeposit deposit = new PatientDeposit();
        final PatientDeposit locked = new PatientDeposit();
        PatientDeposit loweredCopy;
        final List<Double> taken = new ArrayList<>();

        StubDepositService(double balance) {
            deposit.setId(5L);
            deposit.setBalance(balance);
            locked.setId(5L);
            locked.setBalance(balance);
        }

        @Override
        public PatientDeposit getDepositOfThePatient(Patient p, com.divudi.core.entity.Department d) {
            return deposit;
        }

        @Override
        public void handleUtilization(Bill b, PatientDeposit pd, double amount) {
            taken.add(amount);
            loweredCopy = pd;
            pd.setBalance(pd.getBalance() - amount);
        }
    }

    private OpdPreSettleWriter writerWithDeposit(StubDepositService deposits) {
        OpdPreSettleWriter writer = new OpdPreSettleWriter();
        writer.billFacade = new RecordingBillFacade();
        writer.paymentFacade = new PaymentFacade() {
            @Override
            public void create(Payment p) {
            }
        };
        writer.patientFacade = new RecordingPatientFacade();
        writer.patientDepositService = deposits;
        writer.patientDepositFacade = new PatientDepositFacade() {
            @Override
            public PatientDeposit findWithLock(Object id) {
                return id.equals(5L) ? deposits.locked : null;
            }
        };
        return writer;
    }

    @Test
    void depositUseLowersTheDepositRecordByTheAmountOfTheRow() {
        Patient patient = new Patient();
        StubDepositService deposits = new StubDepositService(1000.0);
        OpdPreSettleRequest request = request(new BilledBill(), new PreBill(), patient);
        request.addDepositUse(patient, 400.0);

        writerWithDeposit(deposits).settle(request);

        assertEquals(List.of(400.0), deposits.taken);
        assertEquals(600.0, deposits.locked.getBalance());
    }

    @Test
    void theLockedCopyIsTheOneCheckedAndLowered() {
        Patient patient = new Patient();
        StubDepositService deposits = new StubDepositService(1000.0);
        // The plain lookup shows a stale high balance; the locked copy is the truth.
        deposits.deposit.setBalance(5000.0);
        deposits.locked.setBalance(100.0);
        OpdPreSettleRequest request = request(new BilledBill(), new PreBill(), patient);
        request.addDepositUse(patient, 400.0);
        assertThrows(OpdPreSettleException.class, () -> writerWithDeposit(deposits).settle(request));
        assertTrue(deposits.taken.isEmpty());

        deposits.locked.setBalance(1000.0);
        OpdPreSettleRequest again = request(new BilledBill(), new PreBill(), patient);
        again.addDepositUse(patient, 400.0);
        writerWithDeposit(deposits).settle(again);
        assertSame(deposits.locked, deposits.loweredCopy);
        assertEquals(600.0, deposits.locked.getBalance());
        assertEquals(5000.0, deposits.deposit.getBalance());
    }

    @Test
    void depositUseWithTooLittleBalanceIsRefusedAndNothingIsTaken() {
        Patient patient = new Patient();
        StubDepositService deposits = new StubDepositService(100.0);
        OpdPreSettleRequest request = request(new BilledBill(), new PreBill(), patient);
        request.addDepositUse(patient, 400.0);

        assertThrows(OpdPreSettleException.class, () -> writerWithDeposit(deposits).settle(request));

        assertTrue(deposits.taken.isEmpty());
        assertEquals(100.0, deposits.locked.getBalance());
    }

    @Test
    void creditLimitCountsAsAvailableForTheDeposit() {
        Patient patient = new Patient();
        patient.setCreditLimit(500.0);
        StubDepositService deposits = new StubDepositService(100.0);
        OpdPreSettleRequest request = request(new BilledBill(), new PreBill(), patient);
        request.addDepositUse(patient, 400.0);

        writerWithDeposit(deposits).settle(request);

        assertEquals(List.of(400.0), deposits.taken);
    }

    @Test
    void zeroDepositUseIsIgnored() {
        OpdPreSettleRequest request = new OpdPreSettleRequest();
        request.addDepositUse(new Patient(), 0.0);
        assertTrue(request.getDepositUses().isEmpty());
    }
}

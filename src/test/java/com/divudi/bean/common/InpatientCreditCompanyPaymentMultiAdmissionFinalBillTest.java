package com.divudi.bean.common;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying that inpatient credit company payments and cancellations
 * covering several admissions adjust each admission's final bill only by that
 * admission's share of items rather than the entire payment bill (#25547).
 */
public class InpatientCreditCompanyPaymentMultiAdmissionFinalBillTest {

    private BillBeanController billBeanController;
    private Map<Object, Bill> billStore;

    @BeforeEach
    void setUp() {
        billBeanController = new BillBeanController();
        billStore = new HashMap<>();

        // Fake BillFacade that returns final bills from the in-memory store
        BillFacade fakeFacade = new BillFacade() {
            @Override
            public Bill findWithoutCache(Object id) {
                return billStore.get(id);
            }

            @Override
            public void edit(Bill entity) {
                if (entity != null && entity.getId() != null) {
                    billStore.put(entity.getId(), entity);
                }
            }
        };
        billBeanController.setBillFacade(fakeFacade);
    }

    @Test
    @DisplayName("Inpatient CC payment covering multiple admissions updates each final bill by its admission item share (#25547)")
    void paymentCoveringMultipleAdmissionsUpdatesEachFinalBillByItsShare() {
        // Admission A: final bill of 10,000 net, 0 paid, 10,000 balance
        PatientEncounter admissionA = new PatientEncounter();
        admissionA.setId(101L);
        admissionA.setPaymentFinalized(true);

        Bill finalBillA = new BilledBill();
        finalBillA.setId(1001L);
        finalBillA.setNetTotal(10000.0);
        finalBillA.setBalance(10000.0);
        finalBillA.setPaidAmount(0.0);
        finalBillA.setSettledAmountBySponsor(0.0);
        admissionA.setFinalBill(finalBillA);
        billStore.put(1001L, finalBillA);

        // Admission B: final bill of 20,000 net, 0 paid, 20,000 balance
        PatientEncounter admissionB = new PatientEncounter();
        admissionB.setId(102L);
        admissionB.setPaymentFinalized(true);

        Bill finalBillB = new BilledBill();
        finalBillB.setId(1002L);
        finalBillB.setNetTotal(20000.0);
        finalBillB.setBalance(20000.0);
        finalBillB.setPaidAmount(0.0);
        finalBillB.setSettledAmountBySponsor(0.0);
        admissionB.setFinalBill(finalBillB);
        billStore.put(1002L, finalBillB);

        // Payment Bill covers both admissions: 5,000 for A + 7,000 for B = 12,000 total
        Bill paymentBill = new BilledBill();
        paymentBill.setId(5001L);
        paymentBill.setBillTypeAtomic(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
        paymentBill.setNetTotal(12000.0);

        // Update Admission A with 5,000
        boolean okA = billBeanController.updateInwardDipositList(admissionA, paymentBill, 5000.0);
        assertTrue(okA);
        assertEquals(5000.0, finalBillA.getBalance(), 0.001);
        assertEquals(5000.0, finalBillA.getPaidAmount(), 0.001);
        assertEquals(5000.0, finalBillA.getSettledAmountBySponsor(), 0.001);

        // Update Admission B with 7,000
        boolean okB = billBeanController.updateInwardDipositList(admissionB, paymentBill, 7000.0);
        assertTrue(okB);
        assertEquals(13000.0, finalBillB.getBalance(), 0.001);
        assertEquals(7000.0, finalBillB.getPaidAmount(), 0.001);
        assertEquals(7000.0, finalBillB.getSettledAmountBySponsor(), 0.001);
    }

    @Test
    @DisplayName("Inpatient CC payment cancellation reverses only the admission's share on the final bill (#25547)")
    void cancellationCoveringMultipleAdmissionsReversesOnlyItsShare() {
        // Admission A: settled 5,000 by sponsor out of 10,000
        PatientEncounter admissionA = new PatientEncounter();
        admissionA.setId(101L);
        admissionA.setPaymentFinalized(true);

        Bill finalBillA = new BilledBill();
        finalBillA.setId(1001L);
        finalBillA.setNetTotal(10000.0);
        finalBillA.setBalance(5000.0);
        finalBillA.setPaidAmount(5000.0);
        finalBillA.setSettledAmountBySponsor(5000.0);
        admissionA.setFinalBill(finalBillA);
        billStore.put(1001L, finalBillA);

        // Admission B: settled 7,000 by sponsor out of 20,000
        PatientEncounter admissionB = new PatientEncounter();
        admissionB.setId(102L);
        admissionB.setPaymentFinalized(true);

        Bill finalBillB = new BilledBill();
        finalBillB.setId(1002L);
        finalBillB.setNetTotal(20000.0);
        finalBillB.setBalance(13000.0);
        finalBillB.setPaidAmount(7000.0);
        finalBillB.setSettledAmountBySponsor(7000.0);
        admissionB.setFinalBill(finalBillB);
        billStore.put(1002L, finalBillB);

        // Cancellation Bill: total netTotal is -12,000
        Bill cancelBill = new CancelledBill();
        cancelBill.setId(6001L);
        cancelBill.setBillTypeAtomic(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        cancelBill.setNetTotal(-12000.0);

        // Cancel Admission A's share (-5,000)
        boolean okA = billBeanController.updateInwardDipositList(admissionA, cancelBill, -5000.0);
        assertTrue(okA);
        assertEquals(10000.0, finalBillA.getBalance(), 0.001);
        assertEquals(0.0, finalBillA.getPaidAmount(), 0.001);
        assertEquals(0.0, finalBillA.getSettledAmountBySponsor(), 0.001);

        // Cancel Admission B's share (-7,000)
        boolean okB = billBeanController.updateInwardDipositList(admissionB, cancelBill, -7000.0);
        assertTrue(okB);
        assertEquals(20000.0, finalBillB.getBalance(), 0.001);
        assertEquals(0.0, finalBillB.getPaidAmount(), 0.001);
        assertEquals(0.0, finalBillB.getSettledAmountBySponsor(), 0.001);
    }

    @Test
    @DisplayName("Single-admission payments default to bill net total via 2-arg overload (#25547)")
    void singleAdmissionPaymentUsesFullNetTotalByDefault() {
        PatientEncounter admission = new PatientEncounter();
        admission.setId(201L);
        admission.setPaymentFinalized(true);

        Bill finalBill = new BilledBill();
        finalBill.setId(2001L);
        finalBill.setNetTotal(15000.0);
        finalBill.setBalance(15000.0);
        finalBill.setPaidAmount(0.0);
        finalBill.setSettledAmountByPatient(0.0);
        admission.setFinalBill(finalBill);
        billStore.put(2001L, finalBill);

        Bill depositBill = new BilledBill();
        depositBill.setId(7001L);
        depositBill.setBillTypeAtomic(BillTypeAtomic.INWARD_DEPOSIT);
        depositBill.setNetTotal(4000.0);

        boolean ok = billBeanController.updateInwardDipositList(admission, depositBill);
        assertTrue(ok);
        assertEquals(11000.0, finalBill.getBalance(), 0.001);
        assertEquals(4000.0, finalBill.getPaidAmount(), 0.001);
        assertEquals(4000.0, finalBill.getSettledAmountByPatient(), 0.001);
    }
}

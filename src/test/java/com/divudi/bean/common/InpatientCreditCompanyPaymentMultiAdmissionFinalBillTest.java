package com.divudi.bean.common;

import com.divudi.bean.inward.InwardBeanController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FinalBillPaidFigures;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Inpatient payments, receipts and cancels covering one or several admissions
 * (#25547). Since #26152 the final bill's figures are recounted from the bills
 * by {@link InwardBeanController#recountFinalBillPaid}, not added to by
 * {@link BillBeanController#updateInwardDipositList}. These tests use a fake
 * recount that applies what the bills of each final bill add up to (set by the
 * test through {@link FinalBillPaidFigures}) and assert what the link step
 * still decides: which final bills are linked and recounted, how often, and
 * that cancelled or retired final bills are skipped. How the sums are counted
 * is covered by FinalBillPaidFiguresTest; the per-admission split of one
 * receipt is done by the recount reading the receipt's items per admission.
 */
public class InpatientCreditCompanyPaymentMultiAdmissionFinalBillTest {

    private BillBeanController billBeanController;
    private Map<Object, Bill> billStore;
    /** Final bill id -> what its bills add up to (what a real recount would read). */
    private Map<Long, FinalBillPaidFigures> billsAddUpTo;
    private List<Bill> recounted;
    /** Every entity passed to the facade's edit, in order. */
    private List<Bill> edited;
    /** What each edit saw: how many bills had been recounted before it. */
    private List<Integer> recountsBeforeEdit;

    @BeforeEach
    void setUp() {
        billBeanController = new BillBeanController();
        billStore = new HashMap<>();
        billsAddUpTo = new HashMap<>();
        recounted = new ArrayList<>();
        edited = new ArrayList<>();
        recountsBeforeEdit = new ArrayList<>();

        BillFacade fakeFacade = new BillFacade() {
            @Override
            public Bill findWithoutCache(Object id) {
                return billStore.get(id);
            }

            @Override
            public void edit(Bill entity) {
                edited.add(entity);
                recountsBeforeEdit.add(recounted.size());
                if (entity != null && entity.getId() != null) {
                    billStore.put(entity.getId(), entity);
                }
            }
        };
        billBeanController.setBillFacade(fakeFacade);

        billBeanController.setInwardBean(new InwardBeanController() {
            @Override
            public void recountFinalBillPaid(Bill finalBill) {
                recounted.add(finalBill);
                FinalBillPaidFigures f = billsAddUpTo.get(finalBill.getId());
                finalBill.setPaidAmount(f.getPaidAmount());
                finalBill.setSettledAmountByPatient(f.getSettledAmountByPatient());
                finalBill.setSettledAmountBySponsor(f.getSettledAmountBySponsor());
                finalBill.setBalance(f.balanceFor(finalBill.getNetTotal()));
            }
        });
    }

    private PatientEncounter admissionWithFinalBill(long encId, long billId, double net) {
        PatientEncounter admission = new PatientEncounter();
        admission.setId(encId);
        admission.setPaymentFinalized(true);
        Bill finalBill = new BilledBill();
        finalBill.setId(billId);
        finalBill.setNetTotal(net);
        finalBill.setBalance(net);
        admission.setFinalBill(finalBill);
        billStore.put(billId, finalBill);
        return admission;
    }

    private Bill bill(long id, BillTypeAtomic type, double net) {
        Bill b = type == BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION ? new CancelledBill() : new BilledBill();
        b.setId(id);
        b.setBillTypeAtomic(type);
        b.setNetTotal(net);
        return b;
    }

    @Test
    @DisplayName("A company receipt covering two admissions links and recounts each admission's own final bill once (#25547)")
    void receiptCoveringTwoAdmissionsRecountsEachFinalBill() {
        PatientEncounter admissionA = admissionWithFinalBill(101L, 1001L, 10000.0);
        PatientEncounter admissionB = admissionWithFinalBill(102L, 1002L, 20000.0);
        // The recount reads the receipt's items per admission: 5,000 for A and 7,000 for B.
        billsAddUpTo.put(1001L, FinalBillPaidFigures.of(0, 0, 0, 5000));
        billsAddUpTo.put(1002L, FinalBillPaidFigures.of(0, 0, 0, 7000));
        Bill receipt = bill(5001L, BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED, 12000.0);

        assertTrue(billBeanController.updateInwardDipositList(admissionA, receipt, 5000.0));
        assertTrue(billBeanController.updateInwardDipositList(admissionB, receipt, 7000.0));

        assertEquals(2, recounted.size());
        assertSame(billStore.get(1001L), recounted.get(0));
        assertSame(billStore.get(1002L), recounted.get(1));
        Bill finalBillA = billStore.get(1001L);
        Bill finalBillB = billStore.get(1002L);
        assertEquals(5000.0, finalBillA.getPaidAmount(), 0.001);
        assertEquals(5000.0, finalBillA.getBalance(), 0.001);
        assertEquals(5000.0, finalBillA.getSettledAmountBySponsor(), 0.001);
        assertEquals(7000.0, finalBillB.getPaidAmount(), 0.001);
        assertEquals(13000.0, finalBillB.getBalance(), 0.001);
        assertEquals(7000.0, finalBillB.getSettledAmountBySponsor(), 0.001);
        assertTrue(finalBillA.getBackwardReferenceBills().contains(receipt));
        assertTrue(finalBillB.getBackwardReferenceBills().contains(receipt));
        assertSame(finalBillB, receipt.getForwardReferenceBill());
    }

    @Test
    @DisplayName("The cancel of a receipt covering two admissions recounts each final bill back to its own figures (#25547)")
    void cancelCoveringTwoAdmissionsRecountsEachFinalBill() {
        PatientEncounter admissionA = admissionWithFinalBill(101L, 1001L, 10000.0);
        PatientEncounter admissionB = admissionWithFinalBill(102L, 1002L, 20000.0);
        // Receipt and cancel net to zero on each admission, with patient money untouched.
        billsAddUpTo.put(1001L, FinalBillPaidFigures.of(300, 0, 0, 0));
        billsAddUpTo.put(1002L, FinalBillPaidFigures.of(0, 0, 0, 0));
        Bill cancel = bill(6001L, BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION, -12000.0);

        assertTrue(billBeanController.updateInwardDipositList(admissionA, cancel, -5000.0));
        assertTrue(billBeanController.updateInwardDipositList(admissionB, cancel, -7000.0));

        assertEquals(2, recounted.size());
        Bill finalBillA = billStore.get(1001L);
        Bill finalBillB = billStore.get(1002L);
        assertEquals(300.0, finalBillA.getPaidAmount(), 0.001);
        assertEquals(9700.0, finalBillA.getBalance(), 0.001);
        assertEquals(0.0, finalBillA.getSettledAmountBySponsor(), 0.001);
        assertEquals(0.0, finalBillB.getPaidAmount(), 0.001);
        assertEquals(20000.0, finalBillB.getBalance(), 0.001);
    }

    @Test
    @DisplayName("A single-admission deposit links the bill and recounts the final bill (#25547)")
    void singleAdmissionDepositRecountsFinalBill() {
        PatientEncounter admission = admissionWithFinalBill(201L, 2001L, 15000.0);
        billsAddUpTo.put(2001L, FinalBillPaidFigures.of(4000, 0, 0, 0));
        Bill deposit = bill(7001L, BillTypeAtomic.INWARD_DEPOSIT, 4000.0);

        assertTrue(billBeanController.updateInwardDipositList(admission, deposit));

        Bill finalBill = billStore.get(2001L);
        assertEquals(1, recounted.size());
        assertEquals(11000.0, finalBill.getBalance(), 0.001);
        assertEquals(4000.0, finalBill.getPaidAmount(), 0.001);
        assertEquals(4000.0, finalBill.getSettledAmountByPatient(), 0.001);
        assertSame(finalBill, deposit.getForwardReferenceBill());
    }

    @Test
    @DisplayName("Running the link again for the same bill links it once and gives the same figures (retry is safe)")
    void sameBillTwiceLinksOnceAndKeepsFigures() {
        PatientEncounter admission = admissionWithFinalBill(301L, 3001L, 1000.0);
        billsAddUpTo.put(3001L, FinalBillPaidFigures.of(0, 0, 0, 400));
        Bill receipt = bill(8003L, BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED, 400.0);

        billBeanController.updateInwardDipositList(admission, receipt, 400.0);
        billBeanController.updateInwardDipositList(admission, receipt, 400.0);

        Bill finalBill = billStore.get(3001L);
        assertEquals(1, finalBill.getBackwardReferenceBills().size());
        assertEquals(400.0, finalBill.getPaidAmount(), 0.001);
        assertEquals(600.0, finalBill.getBalance(), 0.001);
    }

    @Test
    @DisplayName("Receiving the full net then cancelling gives the balance back whatever the order of earlier actions (#25570)")
    void receiveThenCancelRestoresBalance() {
        PatientEncounter admission = admissionWithFinalBill(302L, 3002L, 1000.0);
        Bill finalBill = admission.getFinalBill();

        billsAddUpTo.put(3002L, FinalBillPaidFigures.of(0, 0, 0, 1000));
        billBeanController.updateInwardDipositList(admission,
                bill(8001L, BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED, 1000.0), 1000.0);
        assertEquals(0.0, finalBill.getBalance(), 0.001);

        billsAddUpTo.put(3002L, FinalBillPaidFigures.of(0, 0, 0, 1000 - 1000));
        billBeanController.updateInwardDipositList(admission,
                bill(8002L, BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION, -1000.0), -1000.0);
        assertEquals(1000.0, finalBill.getBalance(), 0.001);
        assertEquals(0.0, finalBill.getPaidAmount(), 0.001);
        assertEquals(0.0, finalBill.getSettledAmountBySponsor(), 0.001);
    }

    @Test
    @DisplayName("A cancelled final bill is not linked or recounted, and the caller is told (#25413)")
    void cancelledFinalBillIsSkipped() {
        PatientEncounter admission = admissionWithFinalBill(401L, 4001L, 1000.0);
        billStore.get(4001L).setCancelled(true);
        Bill payment = bill(9001L, BillTypeAtomic.INWARD_PAYMENT, 200.0);

        assertFalse(billBeanController.updateInwardDipositList(admission, payment));

        assertTrue(recounted.isEmpty());
        assertTrue(billStore.get(4001L).getBackwardReferenceBills().isEmpty());
    }

    @Test
    @DisplayName("A retired final bill is not linked or recounted (#25413)")
    void retiredFinalBillIsSkipped() {
        PatientEncounter admission = admissionWithFinalBill(402L, 4002L, 1000.0);
        billStore.get(4002L).setRetired(true);

        assertFalse(billBeanController.updateInwardDipositList(admission,
                bill(9002L, BillTypeAtomic.INWARD_PAYMENT, 200.0)));

        assertTrue(recounted.isEmpty());
    }

    @Test
    @DisplayName("An admission whose payment is not finalized has no final bill to update")
    void notFinalizedAdmissionIsLeftAlone() {
        PatientEncounter admission = admissionWithFinalBill(403L, 4003L, 1000.0);
        admission.setPaymentFinalized(false);

        assertTrue(billBeanController.updateInwardDipositList(admission,
                bill(9003L, BillTypeAtomic.INWARD_PAYMENT, 200.0)));

        assertTrue(recounted.isEmpty());
    }

    @Test
    @DisplayName("The caller's detached payment bill is never merged; the link goes on the copy read in the transaction (#26152)")
    void detachedPaymentBillIsNotMerged() {
        PatientEncounter admission = admissionWithFinalBill(101L, 1001L, 10000.0);
        billsAddUpTo.put(1001L, FinalBillPaidFigures.of(3000, 0, 0, 0));
        Bill detached = bill(5001L, BillTypeAtomic.INWARD_PAYMENT, 3000.0);
        Bill managed = bill(5001L, BillTypeAtomic.INWARD_PAYMENT, 3000.0);
        billStore.put(5001L, managed);

        assertTrue(billBeanController.updateInwardDipositList(admission, detached));

        assertFalse(edited.stream().anyMatch(e -> e == detached), "the detached copy must not be merged");
        assertSame(billStore.get(1001L), managed.getForwardReferenceBill());
        assertSame(billStore.get(1001L), detached.getForwardReferenceBill());
        // The payment bill's edit is before the recount, but it is the managed copy.
        assertSame(managed, edited.get(0));
        assertEquals(1, recounted.size());
    }
}

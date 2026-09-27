package com.divudi.bean.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InwardPaymentControllerReceiptHeadingTest {

    private static final String FALLBACK = "Fallback Receipt";

    @Test
    void payment_printsInwardPaymentReceipt() {
        assertEquals("Inward Payment Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_PAYMENT, FALLBACK));
    }

    @Test
    void deposit_printsInwardDepositReceipt() {
        assertEquals("Inward Deposit Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_DEPOSIT, FALLBACK));
    }

    @Test
    void cancellationsAndRefunds_printTheirOwnHeadings() {
        assertEquals("Inward Payment Cancellation Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_PAYMENT_CANCELLATION, FALLBACK));
        assertEquals("Inward Payment Refund Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_PAYMENT_REFUND, FALLBACK));
        assertEquals("Inward Payment Refund Cancellation Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_PAYMENT_REFUND_CANCELLATION, FALLBACK));
        assertEquals("Inward Deposit Cancellation Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION, FALLBACK));
        assertEquals("Inward Deposit Refund Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_DEPOSIT_REFUND, FALLBACK));
        assertEquals("Inward Deposit Refund Cancellation Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION, FALLBACK));
    }

    @Test
    void postFinalBillPaymentRefund_printsPostFinalHeading() {
        assertEquals("Post Final Bill Payment Refund Receipt",
                InwardPaymentController.receiptHeadingFor(BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_REFUND, FALLBACK));
    }

    @Test
    void missingOrUnmappedType_usesFallback() {
        assertEquals(FALLBACK, InwardPaymentController.receiptHeadingFor(null, FALLBACK));
        assertEquals(FALLBACK, InwardPaymentController.receiptHeadingFor(BillTypeAtomic.OPD_BILL_WITH_PAYMENT, FALLBACK));
    }

    @Test
    void billOverload_readsTheBillType_andToleratesNullBill() {
        InwardPaymentController controller = new InwardPaymentController();
        Bill bill = new BilledBill();
        bill.setBillTypeAtomic(BillTypeAtomic.INWARD_PAYMENT);
        assertEquals("Inward Payment Receipt", controller.receiptHeading(bill, FALLBACK));
        assertEquals(FALLBACK, controller.receiptHeading(null, FALLBACK));
    }
}

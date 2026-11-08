package com.divudi.bean.cashTransaction;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.service.DrawerBusyException;
import com.divudi.service.DrawerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A busy drawer (#25230) must not propagate to the billing controller, which has
 * already committed the bill, and must not be retried for the bill's remaining
 * payments (each try would wait out the lock timeout again).
 */
class DrawerControllerBusyTest {

    /** Counts calls; throws DrawerBusyException while busy. */
    static class StubDrawerService extends DrawerService {

        boolean busy;
        final List<Payment> applied = new ArrayList<>();
        int calls;

        private void call(Payment p) {
            calls++;
            if (busy) {
                throw new DrawerBusyException("busy");
            }
            applied.add(p);
        }

        @Override
        public void updateUsersDrawer(Payment payment, double paidValue, WebUser webUser) {
            call(payment);
        }

        @Override
        public void updateDrawer(Payment payment, double paidValue) {
            call(payment);
        }

        @Override
        public void updateDrawerForOuts(List<Payment> payments) {
            call(null);
        }

        @Override
        public void updateDrawer(Bill bill, double value, PaymentMethod paymentMethod, WebUser webUser) {
            call(null);
        }
    }

    private StubDrawerService service;
    private DrawerController controller;
    private int messages;

    @BeforeEach
    void setUp() {
        service = new StubDrawerService();
        messages = 0;
        controller = new DrawerController() {
            @Override
            void tellCashier() {
                messages++;
            }
        };
        controller.drawerService = service;
    }

    private static Payment payment(long id, double value) {
        Payment p = new Payment();
        p.setId(id);
        p.setPaidValue(value);
        p.setPaymentMethod(PaymentMethod.Cash);
        return p;
    }

    @Test
    void busyDrawer_doesNotThrow_onAnyWrapper() {
        service.busy = true;
        WebUser u = new WebUser();
        Payment p = payment(1, 100);
        assertDoesNotThrow(() -> controller.updateDrawer(p, 100, u));
        assertDoesNotThrow(() -> controller.updateDrawerForIns(p, u));
        assertDoesNotThrow(() -> controller.updateDrawerForOuts(p, u));
        assertDoesNotThrow(() -> controller.updateDrawerForIns(p));
        assertDoesNotThrow(() -> controller.updateDrawerForOuts(p));
        assertDoesNotThrow(() -> controller.updateDrawer(p, 100));
        assertDoesNotThrow(() -> controller.updateDrawerForOuts(Arrays.asList(p)));
        assertDoesNotThrow(() -> controller.updateDrawer(new Bill(), 100, PaymentMethod.Cash, u));
        assertDoesNotThrow(() -> controller.updateDrawerForIns(new Bill(), PaymentMethod.Cash, 100, u));
        assertEquals(9, messages);
    }

    @Test
    void busyDrawer_stopsAfterFirstPayment_inUserLists() {
        service.busy = true;
        WebUser u = new WebUser();
        List<Payment> ps = Arrays.asList(payment(1, 10), payment(2, 20), payment(3, 30));

        controller.updateDrawerForIns(ps, u);
        assertEquals(1, service.calls);

        controller.updateDrawer(ps, u);
        assertEquals(2, service.calls);

        controller.updateDrawerForOuts(ps, u);
        assertEquals(3, service.calls);

        controller.updateDrawerForIns(ps);
        assertEquals(4, service.calls);
    }

    @Test
    void busyDrawer_logsSkippedPaymentsWithAppliedSignAndPlainIds() {
        service.busy = true;
        List<LogRecord> records = new ArrayList<>();
        Handler h = new Handler() {
            @Override
            public void publish(LogRecord r) {
                records.add(r);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger log = Logger.getLogger(DrawerController.class.getName());
        log.addHandler(h);
        try {
            controller.updateDrawerForOuts(Arrays.asList(payment(1234567, 10), payment(2, 20)), new WebUser());
        } finally {
            log.removeHandler(h);
        }
        assertEquals(2, records.size());
        assertEquals("1234567", records.get(0).getParameters()[0]);
        assertEquals("-10.0", records.get(0).getParameters()[2]);
        assertEquals("-20.0", records.get(1).getParameters()[2]);
    }

    @Test
    void freeDrawer_appliesEveryPayment() {
        WebUser u = new WebUser();
        List<Payment> ps = Arrays.asList(payment(1, 10), payment(2, 20), payment(3, 30));
        controller.updateDrawerForIns(ps, u);
        assertEquals(ps, service.applied);
        assertEquals(0, messages);
    }
}

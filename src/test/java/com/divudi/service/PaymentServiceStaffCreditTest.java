package com.divudi.service;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;
import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests verifying PaymentService.updateBalances with PaymentMethod.Staff (#25339).
 */
public class PaymentServiceStaffCreditTest {

    private PaymentService paymentService;
    private MockStaffService mockStaffService;

    static class MockStaffService extends StaffService {
        Staff creditStaff;
        double creditValue;
        int creditCallCount;

        Staff welfareStaff;
        double welfareValue;
        int welfareCallCount;

        @Override
        public void updateStaffCredit(Staff staff, double value) {
            this.creditStaff = staff;
            this.creditValue += value;
            this.creditCallCount++;
        }

        @Override
        public void updateStaffWelfare(Staff staff, double value) {
            this.welfareStaff = staff;
            this.welfareValue += value;
            this.welfareCallCount++;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        paymentService = new PaymentService();
        mockStaffService = new MockStaffService();

        Field staffServiceField = PaymentService.class.getDeclaredField("staffService");
        staffServiceField.setAccessible(true);
        staffServiceField.set(paymentService, mockStaffService);
    }

    @Test
    @DisplayName("Multi-payment staff payment with payment.toStaff reverses credit on cancellation")
    void updateBalances_multiPayment_withPaymentToStaff_updatesCredit() {
        Staff staff = new Staff();
        staff.setId(101L);

        Bill bill = new Bill();
        bill.setToStaff(null); // Multi-payment bill: bill.toStaff is null

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setToStaff(staff); // Staff is recorded on the payment row
        payment.setPaymentMethod(PaymentMethod.Staff);
        payment.setPaidValue(-500.0); // Cancellation reversal

        paymentService.updateBalances(Collections.singletonList(payment));

        // Fix for #25339: resolves payment.toStaff and calls updateStaffCredit
        assertEquals(1, mockStaffService.creditCallCount, "Must call updateStaffCredit");
        assertSame(staff, mockStaffService.creditStaff);
        assertEquals(-500.0, mockStaffService.creditValue, 0.001);
        assertEquals(0, mockStaffService.welfareCallCount, "Must NOT touch welfare");
    }

    @Test
    @DisplayName("Single-payment staff bill with bill.toStaff calls updateStaffCredit, not updateStaffWelfare")
    void updateBalances_withBillToStaff_callsCreditNotWelfare() {
        Staff staff = new Staff();
        staff.setId(102L);

        Bill bill = new Bill();
        bill.setToStaff(staff);

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setPaymentMethod(PaymentMethod.Staff);
        payment.setPaidValue(750.0);

        paymentService.updateBalances(Collections.singletonList(payment));

        // Fix for #25339: calls updateStaffCredit instead of updateStaffWelfare
        assertEquals(1, mockStaffService.creditCallCount, "Must call updateStaffCredit");
        assertSame(staff, mockStaffService.creditStaff);
        assertEquals(750.0, mockStaffService.creditValue, 0.001);
        assertEquals(0, mockStaffService.welfareCallCount, "Must NOT touch welfare");
    }
}

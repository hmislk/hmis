package com.divudi.bean.common;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests verifying OPD batch bill cancellation payment method initialization
 * and staff welfare / credit preservation (#25348).
 */
public class OpdBatchBillCancellationStaffReversalTest {

    private OpdBatchBillCancellationController controller;
    private Method initMethod;

    @BeforeEach
    void setUp() throws Exception {
        controller = new OpdBatchBillCancellationController();
        initMethod = OpdBatchBillCancellationController.class.getDeclaredMethod(
                "initializePaymentDataFromOriginalPayments", List.class);
        initMethod.setAccessible(true);
    }

    @Test
    @DisplayName("MultiplePaymentMethods batch bill with single payment row preserves MultiplePaymentMethods (#25348)")
    void initializePaymentData_multiPaymentWithSingleRow_preservesMultiplePaymentMethods() throws Exception {
        Staff staff = new Staff();
        staff.setId(201L);

        Bill batchBill = new Bill();
        batchBill.setNetTotal(1500.0);
        batchBill.setPaymentMethod(PaymentMethod.MultiplePaymentMethods);
        batchBill.setToStaff(null);
        controller.setBatchBill(batchBill);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.Staff_Welfare);
        payment.setPaidValue(1500.0);
        payment.setToStaff(staff);

        initMethod.invoke(controller, Collections.singletonList(payment));

        assertEquals(PaymentMethod.MultiplePaymentMethods, controller.getPaymentMethod(),
                "Batch bill settled with MultiplePaymentMethods must retain MultiplePaymentMethods on cancellation");

        assertNotNull(controller.getPaymentMethodData().getPaymentMethodMultiple());
        assertEquals(1, controller.getPaymentMethodData().getPaymentMethodMultiple()
                .getMultiplePaymentMethodComponentDetails().size(), "Must contain 1 component detail");

        ComponentDetail cd = controller.getPaymentMethodData().getPaymentMethodMultiple()
                .getMultiplePaymentMethodComponentDetails().get(0);
        assertEquals(PaymentMethod.Staff_Welfare, cd.getPaymentMethod());
        assertSame(staff, cd.getPaymentMethodData().getStaffWelfare().getToStaff(),
                "Component detail must carry the original payment staff member for welfare reversal");
    }

    @Test
    @DisplayName("Single payment method Staff_Welfare bill populates toStaff from original payment row (#25348)")
    void initializePaymentData_singleMethodStaffWelfare_populatesToStaff() throws Exception {
        Staff staff = new Staff();
        staff.setId(202L);

        Bill batchBill = new Bill();
        batchBill.setNetTotal(800.0);
        batchBill.setPaymentMethod(PaymentMethod.Staff_Welfare);
        batchBill.setToStaff(null); // Bill-level toStaff was missing/null
        controller.setBatchBill(batchBill);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.Staff_Welfare);
        payment.setPaidValue(800.0);
        payment.setToStaff(staff);

        initMethod.invoke(controller, Collections.singletonList(payment));

        assertEquals(PaymentMethod.Staff_Welfare, controller.getPaymentMethod());
        assertSame(staff, controller.getToStaff(),
                "Controller toStaff must be populated from original payment row for single-method reversal");
    }

    @Test
    @DisplayName("Single payment method Staff credit bill populates toStaff from original payment row (#25348)")
    void initializePaymentData_singleMethodStaffCredit_populatesToStaff() throws Exception {
        Staff staff = new Staff();
        staff.setId(203L);

        Bill batchBill = new Bill();
        batchBill.setNetTotal(1200.0);
        batchBill.setPaymentMethod(PaymentMethod.Staff);
        batchBill.setToStaff(null);
        controller.setBatchBill(batchBill);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.Staff);
        payment.setPaidValue(1200.0);
        payment.setToStaff(staff);

        initMethod.invoke(controller, Collections.singletonList(payment));

        assertEquals(PaymentMethod.Staff, controller.getPaymentMethod());
        assertSame(staff, controller.getToStaff(),
                "Controller toStaff must be populated from original payment row for staff credit reversal");
    }

    @Test
    @DisplayName("Single payment method Credit bill resets paymentMethodData and does not carry over previous staff (#25348)")
    void initializePaymentData_singleMethodCredit_resetsPreviousStaffCredit() throws Exception {
        Staff previousStaff = new Staff();
        previousStaff.setId(999L);
        controller.getPaymentMethodData().getStaffCredit().setToStaff(previousStaff);

        Bill batchBill = new Bill();
        batchBill.setNetTotal(1000.0);
        batchBill.setPaymentMethod(PaymentMethod.Credit);
        controller.setBatchBill(batchBill);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.Credit);
        payment.setPaidValue(1000.0);
        payment.setToStaff(null);

        initMethod.invoke(controller, Collections.singletonList(payment));

        assertEquals(PaymentMethod.Credit, controller.getPaymentMethod());
        assertNull(controller.getPaymentMethodData().getStaffCredit().getToStaff(),
                "Stale staff member from previous cancellation must be cleared on new payment data initialization");
    }
}

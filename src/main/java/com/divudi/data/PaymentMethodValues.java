package com.divudi.data;

import com.divudi.entity.Bill;
import com.divudi.entity.Payment;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodValues {

    private List<PaymentMethodWithValue> paymentMethodsWithValues;

    public PaymentMethodValues(PaymentMethod[] paymentMethods) {
        this.paymentMethodsWithValues = new ArrayList<>();
        for (PaymentMethod paymentMethod : paymentMethods) {
            this.paymentMethodsWithValues.add(new PaymentMethodWithValue(paymentMethod, 0.0));
        }
    }

    public List<PaymentMethodWithValue> getNonZeroValues() {
        List<PaymentMethodWithValue> nonZeroValues = new ArrayList<>();
        for (PaymentMethodWithValue pmwv : paymentMethodsWithValues) {
            if (pmwv.getValue() != null && pmwv.getValue() != 0.0) {
                nonZeroValues.add(pmwv);
            }
        }
        return nonZeroValues;
    }

    public double getTotalValue() {
        double total = 0.0;
        for (PaymentMethodWithValue pmwv : paymentMethodsWithValues) {
            if (pmwv.getValue() != null) {
                total += pmwv.getValue();
            }
        }
        return total;
    }

    public List<PaymentMethodWithValue> getPaymentMethodsWithValues() {
        return paymentMethodsWithValues;
    }

    public void setPaymentMethodsWithValues(List<PaymentMethodWithValue> paymentMethodsWithValues) {
        this.paymentMethodsWithValues = paymentMethodsWithValues;
    }

    // Overloaded addValue methods
    public void addValue(Payment payment) {
        updateValue(payment.getPaymentMethod(), payment.getPaidValue());
    }

    public void addValue(Bill bill) {
        updateValue(bill.getPaymentMethod(), bill.getNetTotal());
    }

    public void addAbsoluteValue(Payment payment) {
        updateValue(payment.getPaymentMethod(), Math.abs(payment.getPaidValue()));
    }

    public void addAbsoluteValue(Bill bill) {
        updateValue(bill.getPaymentMethod(), Math.abs(bill.getNetTotal()));
    }

    public void deductAbsoluteValue(Payment payment) {
        updateValue(payment.getPaymentMethod(), -Math.abs(payment.getPaidValue()));
    }

    public void deductAbsoluteValue(Bill bill) {
        updateValue(bill.getPaymentMethod(), -Math.abs(bill.getNetTotal()));
    }

    public void deductValue(Payment payment) {
        updateValue(payment.getPaymentMethod(), -(payment.getPaidValue()));
    }

    public void deductValue(Bill bill) {
        updateValue(bill.getPaymentMethod(), -(bill.getNetTotal()));
    }

    // Helper method to update the value for a specific payment method
    private void updateValue(PaymentMethod paymentMethod, double value) {
        for (PaymentMethodWithValue pmwv : paymentMethodsWithValues) {
            if (pmwv.getPaymentMethod() == paymentMethod) {
                pmwv.setValue(pmwv.getValue() + value); // Adds or deducts the value
                break; // Exit once the matching payment method is found and updated
            }
        }
    }

    /**
     * Inner class to hold each payment method along with its value.
     */
    public static class PaymentMethodWithValue {

        private PaymentMethod paymentMethod;
        private Double value;

        public PaymentMethodWithValue(PaymentMethod paymentMethod, Double value) {
            this.paymentMethod = paymentMethod;
            this.value = value;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }
}

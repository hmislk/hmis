package com.divudi.data;

import java.util.ArrayList;
import java.util.List;

public class AtomicBillTypeTotals {

    private List<AtomicBillRecord> atomicBillRecords;

    public AtomicBillTypeTotals() {
        this.atomicBillRecords = new ArrayList<>();
    }

    private AtomicBillRecord findRecord(BillTypeAtomic billType, PaymentMethod paymentMethod) {
        if (atomicBillRecords == null || billType == null || paymentMethod == null) {
            return null;
        }

        return atomicBillRecords.stream()
                .filter(record -> billType.equals(record.getBillType()) && paymentMethod.equals(record.getPaymentMethod()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Calculates the total paid value for a given list of bill types, across
     * all payment methods.
     *
     * @param billTypes List of BillTypeAtomic for which the total is to be
     * calculated.
     * @return The total paid value for the given bill types.
     */
    public double getTotalByBillTypes(List<BillTypeAtomic> billTypes) {
        return atomicBillRecords.stream()
                .filter(record -> billTypes.contains(record.getBillType()))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    /**
     * Calculates the total paid value for a given list of payment methods,
     * across all bill types.
     *
     * @param paymentMethods List of PaymentMethod for which the total is to be
     * calculated.
     * @return The total paid value for the given payment methods.
     */
    public double getTotalByPaymentMethods(List<PaymentMethod> paymentMethods) {
        return atomicBillRecords.stream()
                .filter(record -> paymentMethods.contains(record.getPaymentMethod()))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    /**
     * Adds a new AtomicBillRecord or updates an existing one with the given
     * values.
     *
     * @param billType The bill type of the record.
     * @param paymentMethod The payment method of the record.
     * @param paidValue The value to be added to the paid value of the record.
     */
    public void addOrUpdateAtomicRecord(BillTypeAtomic billType, PaymentMethod paymentMethod, double paidValue) {
        AtomicBillRecord existingRecord = findRecord(billType, paymentMethod);
        if (existingRecord == null) {
            atomicBillRecords.add(new AtomicBillRecord(billType, paymentMethod, paidValue));
        } else {
            existingRecord.addPaidValue(paidValue);
        }
    }

    /**
     * Calculates the total paid value for a single bill type, across all
     * payment methods.
     *
     * @param billType BillTypeAtomic for which the total is to be calculated.
     * @return The total paid value for the given bill type.
     */
    public double getTotalByBillTypes(BillTypeAtomic billType) {
        return atomicBillRecords.stream()
                .filter(record -> record.getBillType().equals(billType))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    /**
     * Calculates the total paid value for a single payment method, across all
     * bill types.
     *
     * @param paymentMethod PaymentMethod for which the total is to be
     * calculated.
     * @return The total paid value for the given payment method.
     */
    public double getTotalByPaymentMethods(PaymentMethod paymentMethod) {
        return atomicBillRecords.stream()
                .filter(record -> record.getPaymentMethod().equals(paymentMethod))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    /**
     * Calculates the total paid value for a combination of bill types and
     * payment methods.
     *
     * @param billTypes List of BillTypeAtomic for which the total is to be
     * calculated.
     * @param paymentMethods List of PaymentMethod for which the total is to be
     * calculated.
     * @return The total paid value for the given combination of bill types and
     * payment methods.
     */
    public double getTotal(List<BillTypeAtomic> billTypes, List<PaymentMethod> paymentMethods) {
        return atomicBillRecords.stream()
                .filter(record -> billTypes.contains(record.getBillType()) && paymentMethods.contains(record.getPaymentMethod()))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    /**
     * Calculates the total paid value for a given list of bill types.
     *
     * @param billTypes List of BillTypeAtomic for which the total is to be
     * calculated.
     * @return The total paid value for the given bill types.
     */
    public double getTotal(List<BillTypeAtomic> billTypes) {
        return atomicBillRecords.stream()
                .filter(record -> billTypes.contains(record.getBillType()))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }


    /**
     * Calculates the total paid value for a specific bill type and payment
     * method.
     *
     * @param billType BillTypeAtomic for which the total is to be calculated.
     * @param paymentMethod PaymentMethod for which the total is to be
     * calculated.
     * @return The total paid value for the given bill type and payment method.
     */
    public double getTotal(BillTypeAtomic billType, PaymentMethod paymentMethod) {
        return atomicBillRecords.stream()
                .filter(record -> record.getBillType().equals(billType) && record.getPaymentMethod().equals(paymentMethod))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    /**
     * Inner class to encapsulate bill type, payment method, and paid value.
     */
    public static class AtomicBillRecord {

        private BillTypeAtomic billType;
        private PaymentMethod paymentMethod;
        private double paidValue;

        public AtomicBillRecord(BillTypeAtomic billType, PaymentMethod paymentMethod, double paidValue) {
            this.billType = billType;
            this.paymentMethod = paymentMethod;
            this.paidValue = paidValue;
        }

        public BillTypeAtomic getBillType() {
            return billType;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public double getPaidValue() {
            return paidValue;
        }

        public void addPaidValue(double valueToAdd) {
            this.paidValue += valueToAdd;
        }
    }

}

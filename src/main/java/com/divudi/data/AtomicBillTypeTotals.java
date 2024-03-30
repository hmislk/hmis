package com.divudi.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AtomicBillTypeTotals {

    private List<AtomicBillRecord> atomicBillRecords;
    private List<ServiceTypeTotal> serviceTypeTotals = new ArrayList<>();

    public AtomicBillTypeTotals() {
        this.atomicBillRecords = new ArrayList<>();
    }

    private AtomicBillRecord findRecord(BillTypeAtomic billType, PaymentMethod paymentMethod) {
        return atomicBillRecords.stream()
                .filter(record -> record.getBillType().equals(billType) && record.getPaymentMethod().equals(paymentMethod))
                .findFirst()
                .orElse(null);
    }

    private void updateServiceTypeTotals() {
        serviceTypeTotals.clear();
        atomicBillRecords.stream()
                .collect(Collectors.groupingBy(record -> record.getBillType().getServiceType()))
                .forEach((serviceType, records) -> {
                    double totalByServiceType = records.stream()
                            .mapToDouble(AtomicBillRecord::getPaidValue)
                            .sum();
                    serviceTypeTotals.add(new ServiceTypeTotal(serviceType, totalByServiceType));
                });
    }

    public List<ServiceTypeTotal> getServiceTypeTotals() {
        return serviceTypeTotals;
    }

    public void setServiceTypeTotals(List<ServiceTypeTotal> serviceTypeTotals) {
        this.serviceTypeTotals = serviceTypeTotals;
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

    /**
     * Represents totals for a specific ServiceType, including totals for each
     * BillCategory beneath it.
     */
    public static class ServiceTypeTotal {

        private ServiceType serviceType;
        private List<BillCategoryTotal> billCategoryTotals = new ArrayList<>();
        private double totalByServiceType;

        public ServiceTypeTotal(ServiceType serviceType, double totalByServiceType) {
            this.serviceType = serviceType;
            this.totalByServiceType = totalByServiceType;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }

        public void setServiceType(ServiceType serviceType) {
            this.serviceType = serviceType;
        }

        public List<BillCategoryTotal> getBillCategoryTotals() {
            return billCategoryTotals;
        }

        public void setBillCategoryTotals(List<BillCategoryTotal> billCategoryTotals) {
            this.billCategoryTotals = billCategoryTotals;
        }

        public double getTotalByServiceType() {
            return totalByServiceType;
        }

        public void setTotalByServiceType(double totalByServiceType) {
            this.totalByServiceType = totalByServiceType;
        }
    }

    /**
     * Represents totals for a specific BillCategory within a ServiceType,
     * including totals for each PaymentMethod beneath it.
     */
    public static class BillCategoryTotal {

        private BillCategory billCategory;
        private List<PaymentMethodTotal> paymentMethodTotals = new ArrayList<>();
        private double totalByBillCategory;

        public BillCategoryTotal(BillCategory billCategory, double totalByBillCategory) {
            this.billCategory = billCategory;
            this.totalByBillCategory = totalByBillCategory;
        }

        public BillCategory getBillCategory() {
            return billCategory;
        }

        public void setBillCategory(BillCategory billCategory) {
            this.billCategory = billCategory;
        }

        public List<PaymentMethodTotal> getPaymentMethodTotals() {
            return paymentMethodTotals;
        }

        public void setPaymentMethodTotals(List<PaymentMethodTotal> paymentMethodTotals) {
            this.paymentMethodTotals = paymentMethodTotals;
        }

        public double getTotalByBillCategory() {
            return totalByBillCategory;
        }

        public void setTotalByBillCategory(double totalByBillCategory) {
            this.totalByBillCategory = totalByBillCategory;
        }
    }

    /**
     * Represents totals for a specific PaymentMethod within a BillCategory.
     */
    public static class PaymentMethodTotal {

        private PaymentMethod paymentMethod;
        private double totalByPaymentMethod;

        // Corrected constructor to accept PaymentMethod and double value
        public PaymentMethodTotal(PaymentMethod paymentMethod, double totalByPaymentMethod) {
            this.paymentMethod = paymentMethod;
            this.totalByPaymentMethod = totalByPaymentMethod;
        }

        // Getters and setters
        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public double getTotalByPaymentMethod() {
            return totalByPaymentMethod;
        }

        public void setTotalByPaymentMethod(double totalByPaymentMethod) {
            this.totalByPaymentMethod = totalByPaymentMethod;
        }
    }

    // Utility methods to manage and retrieve data
    /**
     * Adds a new record or updates an existing one based on the bill type
     * atomic and payment method.
     *
     * @param billTypeAtomic The atomic bill type of the record.
     * @param paymentMethod The payment method of the record.
     * @param paidValue The value associated with this record.
     */
    public void addRecord(BillTypeAtomic billType, PaymentMethod paymentMethod, double paidValue) {
        // Adds or updates an atomic bill record directly.
        addOrUpdateAtomicRecord(billType, paymentMethod, paidValue);

        // Updates the structured totals based on the atomic change.
        addAndUpdateTotals(billType, paymentMethod, paidValue);
    }

    public void addAndUpdateTotals(BillTypeAtomic billTypeAtomic, PaymentMethod paymentMethod, double paidValue) {
        // Find or create the ServiceTypeTotal for this billTypeAtomic
        ServiceTypeTotal serviceTypeTotal = serviceTypeTotals.stream()
                .filter(st -> st.getServiceType() == billTypeAtomic.getServiceType())
                .findFirst()
                .orElseGet(() -> {
                    ServiceTypeTotal newSt = new ServiceTypeTotal(billTypeAtomic.getServiceType(), 0);
                    serviceTypeTotals.add(newSt);
                    return newSt;
                });

        // Find or create the BillCategoryTotal for this billTypeAtomic within the found/created ServiceTypeTotal
        BillCategoryTotal billCategoryTotal = serviceTypeTotal.getBillCategoryTotals().stream()
                .filter(bc -> bc.getBillCategory() == billTypeAtomic.getBillCategory())
                .findFirst()
                .orElseGet(() -> {
                    BillCategoryTotal newBc = new BillCategoryTotal(billTypeAtomic.getBillCategory(), 0);
                    serviceTypeTotal.getBillCategoryTotals().add(newBc);
                    return newBc;
                });

        // Find or create the PaymentMethodTotal for this payment method within the found/created BillCategoryTotal
        PaymentMethodTotal paymentMethodTotal = billCategoryTotal.getPaymentMethodTotals().stream()
                .filter(pm -> pm.getPaymentMethod() == paymentMethod)
                .findFirst()
                .orElseGet(() -> {
                    PaymentMethodTotal newPm = new PaymentMethodTotal(paymentMethod, 0);
                    billCategoryTotal.getPaymentMethodTotals().add(newPm);
                    return newPm;
                });

        // Update the paid value in the found/created PaymentMethodTotal and recalculate totals
        paymentMethodTotal.setTotalByPaymentMethod(paymentMethodTotal.getTotalByPaymentMethod() + paidValue);

        // Recalculate totals for the bill category
        billCategoryTotal.setTotalByBillCategory(
                billCategoryTotal.getPaymentMethodTotals().stream()
                        .mapToDouble(PaymentMethodTotal::getTotalByPaymentMethod).sum());

        // Recalculate totals for the service type
        serviceTypeTotal.setTotalByServiceType(
                serviceTypeTotal.getBillCategoryTotals().stream()
                        .mapToDouble(BillCategoryTotal::getTotalByBillCategory).sum());
    }

    public void addOrUpdateAtomicRecord(BillTypeAtomic billType, PaymentMethod paymentMethod, double paidValue) {
        AtomicBillRecord existingRecord = findRecord(billType, paymentMethod);
        if (existingRecord == null) {
            atomicBillRecords.add(new AtomicBillRecord(billType, paymentMethod, paidValue));
        } else {
            existingRecord.addPaidValue(paidValue);
        }
        // If using the structured approach internally, update structured totals as well
        updateServiceTypeTotals();
    }
    // Additional utility methods can be added as necessary

}

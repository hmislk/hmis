package com.divudi.data;

import com.divudi.data.PaymentMethodValues.PaymentMethodWithValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AtomicBillTypeTotals {

    private List<AtomicBillRecord> atomicBillRecords;

    public AtomicBillTypeTotals() {
        this.atomicBillRecords = new ArrayList<>();
    }

    public void addRecord(BillTypeAtomic billType, PaymentMethod paymentMethod, double paidValue) {
        AtomicBillRecord existingRecord = findRecord(billType, paymentMethod);
        if (existingRecord == null) {
            atomicBillRecords.add(new AtomicBillRecord(billType, paymentMethod, paidValue));
        } else {
            existingRecord.addPaidValue(paidValue);
        }
    }

    public double getTotalValueForBillType(BillTypeAtomic billType) {
        return atomicBillRecords.stream()
                .filter(record -> record.getBillType().equals(billType))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    public double getTotalValueForPaymentMethod(PaymentMethod paymentMethod) {
        return atomicBillRecords.stream()
                .filter(record -> record.getPaymentMethod().equals(paymentMethod))
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    public double getTotalValueForBillCategoryAndServiceType(BillCategory billCategory, ServiceType serviceType) {
        return atomicBillRecords.stream()
                .filter(record -> record.getBillType().getBillCategory() == billCategory && record.getBillType().getServiceType() == serviceType)
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    public double getTotalValueForPaymentMethodAndServiceType(PaymentMethod paymentMethod, ServiceType serviceType) {
        return atomicBillRecords.stream()
                .filter(record -> record.getPaymentMethod() == paymentMethod && record.getBillType().getServiceType() == serviceType)
                .mapToDouble(AtomicBillRecord::getPaidValue)
                .sum();
    }

    private AtomicBillRecord findRecord(BillTypeAtomic billType, PaymentMethod paymentMethod) {
        for (AtomicBillRecord record : atomicBillRecords) {
            if (record.getBillType().equals(billType) && record.getPaymentMethod().equals(paymentMethod)) {
                return record;
            }
        }
        return null;
    }

    public List<PaymentMethodWithValue> getPaymentMethodsWithValuesForServiceType(ServiceType serviceType) {
        return atomicBillRecords.stream()
                .filter(record -> record.getBillType().getServiceType() == serviceType)
                .collect(Collectors.groupingBy(
                        AtomicBillRecord::getPaymentMethod,
                        Collectors.summingDouble(AtomicBillRecord::getPaidValue)))
                .entrySet().stream()
                .map(entry -> new PaymentMethodWithValue(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<ServiceTypeTotal> populateData(List<AtomicBillRecord> records) {
        Map<ServiceType, List<AtomicBillRecord>> byServiceType = records.stream()
                .collect(Collectors.groupingBy(record -> record.getBillType().getServiceType()));

        List<ServiceTypeTotal> serviceTypeTotals = new ArrayList<>();

        for (Map.Entry<ServiceType, List<AtomicBillRecord>> serviceTypeEntry : byServiceType.entrySet()) {
            ServiceTypeTotal serviceTypeTotal = new ServiceTypeTotal();
            serviceTypeTotal.setServiceType(serviceTypeEntry.getKey());

            Map<BillCategory, List<AtomicBillRecord>> byBillCategory = serviceTypeEntry.getValue().stream()
                    .collect(Collectors.groupingBy(record -> record.getBillType().getBillCategory()));

            List<BillCategoryTotal> billCategoryTotals = new ArrayList<>();

            for (Map.Entry<BillCategory, List<AtomicBillRecord>> billCategoryEntry : byBillCategory.entrySet()) {
                BillCategoryTotal billCategoryTotal = new BillCategoryTotal();
                billCategoryTotal.setBillCategory(billCategoryEntry.getKey());

                Map<PaymentMethod, Double> byPaymentMethod = billCategoryEntry.getValue().stream()
                        .collect(Collectors.groupingBy(AtomicBillRecord::getPaymentMethod, Collectors.summingDouble(AtomicBillRecord::getPaidValue)));

                List<PaymentMethodTotal> paymentMethodTotals = byPaymentMethod.entrySet().stream()
                        .map(entry -> new PaymentMethodTotal(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

                billCategoryTotal.setPaymentMethodTotals(paymentMethodTotals);
                billCategoryTotal.setTotalByBillCategory(paymentMethodTotals.stream().mapToDouble(PaymentMethodTotal::getTotalByPaymentMethod).sum());

                billCategoryTotals.add(billCategoryTotal);
            }

            serviceTypeTotal.setBillCategoryTotals(billCategoryTotals);
            serviceTypeTotal.setTotalByServiceType(billCategoryTotals.stream().mapToDouble(BillCategoryTotal::getTotalByBillCategory).sum());

            serviceTypeTotals.add(serviceTypeTotal);
        }

        return serviceTypeTotals;
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

    public static class ServiceTypeTotal {

        private ServiceType serviceType;
        private List<BillCategoryTotal> billCategoryTotals = new ArrayList<>();
        private double totalByServiceType;

        public ServiceTypeTotal() {
        }

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

        public BillCategoryTotal() {
        }

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

        public PaymentMethodTotal() {
        }

        public PaymentMethodTotal(PaymentMethod paymentMethod, double totalByPaymentMethod) {
            this.paymentMethod = paymentMethod;
            this.totalByPaymentMethod = totalByPaymentMethod;
        }

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

}

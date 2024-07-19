package com.divudi.light.common;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.Person;

public class BillSummaryRow {

    private BillTypeAtomic billTypeAtomic;
    private Double grossTotal;
    private Double discount;
    private Double netTotal;
    private Long billCount;
    private PaymentMethod paymentMethod;
    private Person person;
    private Double paidValue;

    public BillSummaryRow(PaymentMethod paymentMethod, double paidValue, long billCount) {
        this.paymentMethod = paymentMethod;
        this.paidValue = paidValue;
        this.billCount = billCount;
    }

    public BillSummaryRow(BillTypeAtomic billTypeAtomic, Double grossTotal, Double discount, Double netTotal, Long billCount, PaymentMethod paymentMethod) {
        this.billTypeAtomic = billTypeAtomic;
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.billCount = billCount;
        this.paymentMethod = paymentMethod;
    }

    public BillSummaryRow(BillTypeAtomic billTypeAtomic, Double paidValue, Long billCount, PaymentMethod paymentMethod) {
        this.billTypeAtomic = billTypeAtomic;
        this.paidValue = paidValue;
        this.billCount = billCount;
        this.paymentMethod = paymentMethod;
    }

    public BillSummaryRow(Double grossTotal, Double discount, Double netTotal, Long billCount, Person person) {
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.billCount = billCount;
        this.person = person;
    }

    public BillSummaryRow() {
    }

    public BillSummaryRow(BillTypeAtomic billTypeAtomic, Double grossTotal, Double discount, Double netTotal, Long billCount) {
        this.billTypeAtomic = billTypeAtomic;
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.billCount = billCount;
    }

    public BillSummaryRow(Double grossTotal, Double discount, Double netTotal, Long billCount, PaymentMethod paymentMethod, Person person) {
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.billCount = billCount;
        this.paymentMethod = paymentMethod;
        this.person = person;
    }

    public BillSummaryRow(Double grossTotal, Double discount, Double netTotal, Long billCount, PaymentMethod paymentMethod) {
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.billCount = billCount;
        this.paymentMethod = paymentMethod;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Long getBillCount() {
        return billCount;
    }

    public void setBillCount(Long billCount) {
        this.billCount = billCount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Double getPaidValue() {
        return paidValue;
    }

    public void setPaidValue(Double paidValue) {
        this.paidValue = paidValue;
    }

}

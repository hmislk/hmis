package com.divudi.light.common;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;

public class BillSummaryRow {
    private BillTypeAtomic billTypeAtomic;
    private Double grossTotal;
    private Double discount;
    private Double netTotal;
    private Long billCount;
    private PaymentMethod paymentMethod;

    public BillSummaryRow(BillTypeAtomic billTypeAtomic, Double grossTotal, Double discount, Double netTotal, Long billCount, PaymentMethod paymentMethod) {
        this.billTypeAtomic = billTypeAtomic;
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.billCount = billCount;
        this.paymentMethod = paymentMethod;
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
    
    
    
}

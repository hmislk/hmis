package com.divudi.data.analytics;

import com.divudi.data.BillTypeAtomic;
import java.util.Date;

/**
 *
 * @author Dr M H B Ariyaratne with ChatGpt
 *
 */
public class DailyBillTypeSummary {

    private Date date;
    private BillTypeAtomic billType;
    private Long billCount;
    private Double totalValue;

    public DailyBillTypeSummary(Date date, BillTypeAtomic billType, Long billCount, Double totalValue) {
        this.date = date;
        this.billType = billType;
        this.billCount = billCount;
        this.totalValue = totalValue;
    }

    // Getters & Setters
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public BillTypeAtomic getBillType() {
        return billType;
    }

    public void setBillType(BillTypeAtomic billType) {
        this.billType = billType;
    }

    public Long getBillCount() {
        return billCount;
    }

    public void setBillCount(Long billCount) {
        this.billCount = billCount;
    }

    public Double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(Double totalValue) {
        this.totalValue = totalValue;
    }
}

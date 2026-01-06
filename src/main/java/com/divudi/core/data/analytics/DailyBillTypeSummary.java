package com.divudi.core.data.analytics;

import com.divudi.core.data.BillTypeAtomic;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DailyBillTypeSummary {

    private Date date;
    private Long billCount;
    private Double totalValue;
    private Map<BillTypeAtomic, Long> billTypeCounts; // Stores count per bill type

    public DailyBillTypeSummary(Date date, Long billCount, Double totalValue) {
        this.date = date;
        this.billCount = billCount;
        this.totalValue = totalValue;
        this.billTypeCounts = new HashMap<>();
    }

    public void addBillTypeCount(BillTypeAtomic type, Long count) {
        billTypeCounts.put(type, count);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public Map<BillTypeAtomic, Long> getBillTypeCounts() {
        return billTypeCounts;
    }

    public void setBillTypeCounts(Map<BillTypeAtomic, Long> billTypeCounts) {
        this.billTypeCounts = billTypeCounts;
    }
}

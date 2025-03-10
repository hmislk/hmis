package com.divudi.data.analytics;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne with ChatGpt
 */
public class DailyBillReportBundle {

    private List<DailyBillTypeSummary> billSummaries;

    public DailyBillReportBundle() {
        this.billSummaries = new ArrayList<>();
    }

    public void setBillSummaries(List<DailyBillTypeSummary> billSummaries) {
        if(billSummaries==null){
            billSummaries = new ArrayList<>();
        }
        this.billSummaries = billSummaries;
    }

    public List<DailyBillTypeSummary> getBillSummaries() {
        return billSummaries;
    }

    public void addBillSummary(DailyBillTypeSummary summary) {
        this.billSummaries.add(summary);
    }
}

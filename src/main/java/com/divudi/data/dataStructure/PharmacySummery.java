/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.data.table.String1Value3;
import java.util.List;

/**
 *
 * @author safrin
 */
public class PharmacySummery {

    private List<String1Value3> bills;
    private Double billedTotal = 0.0;
    private Double cancelledTotal = 0.0;
    private Double refundedTotal = 0.0;
    private Long count;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public List<String1Value3> getBills() {
        return bills;
    }

    public void setBills(List<String1Value3> bills) {
        this.bills = bills;
    }

    public Double getBilledTotal() {
        return billedTotal;
    }

    public void setBilledTotal(Double billedTotal) {
        this.billedTotal = billedTotal;
    }

    public Double getCancelledTotal() {
        return cancelledTotal;
    }

    public void setCancelledTotal(Double cancelledTotal) {
        this.cancelledTotal = cancelledTotal;
    }

    public Double getRefundedTotal() {
        return refundedTotal;
    }

    public void setRefundedTotal(Double refundedTotal) {
        this.refundedTotal = refundedTotal;
    }

}

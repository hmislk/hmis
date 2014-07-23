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
    private double billedTotal;
    private double cancelledTotal;
    private double refundedTotal;


    public List<String1Value3> getBills() {
        return bills;
    }

    public void setBills(List<String1Value3> bills) {
        this.bills = bills;
    }

    public double getBilledTotal() {
        return billedTotal;
    }

    public void setBilledTotal(double billedTotal) {
        this.billedTotal = billedTotal;
    }

    public double getCancelledTotal() {
        return cancelledTotal;
    }

    public void setCancelledTotal(double cancelledTotal) {
        this.cancelledTotal = cancelledTotal;
    }

    public double getRefundedTotal() {
        return refundedTotal;
    }

    public void setRefundedTotal(double refundedTotal) {
        this.refundedTotal = refundedTotal;
    }

}

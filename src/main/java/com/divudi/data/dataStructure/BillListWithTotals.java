/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author buddhika
 */
public class BillListWithTotals {
    List<Bill> bills;
    Double grossTotal;
    Double netTotal;
    Double discount;
    Double cancelledTotal;
    Double refundTotal;

    public Double getCancelledTotal() {
        return cancelledTotal;
    }

    public void setCancelledTotal(Double cancelledTotal) {
        this.cancelledTotal = cancelledTotal;
    }

    public Double getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(Double refundTotal) {
        this.refundTotal = refundTotal;
    }

    
    
    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public Double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public BillListWithTotals() {
        discount=0.0;
        netTotal=0.0;
        grossTotal=0.0;
        bills = new ArrayList<>();
    }
    
    
    
    
}

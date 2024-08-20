/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
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
    Double vat;
    Double cancelledTotal;
    Double refundTotal;
    Double saleValueTotal;

    public Double getSaleValueTotal() {
        return saleValueTotal;
    }

    public void setSaleValueTotal(Double saleValueTotal) {
        this.saleValueTotal = saleValueTotal;
    }
    
    

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

    public Double getVat() {
        return vat;
    }

    public void setVat(Double vat) {
        this.vat = vat;
    }
    
    

    public BillListWithTotals() {
        discount=0.0;
        netTotal=0.0;
        grossTotal=0.0;
        vat=0.0;
        bills = new ArrayList<>();
    }
    
    
    
    
}

/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.data.dataStructure;

import com.divudi.data.table.String2Value4;
import java.util.List;

/**
 *
 * @author safrin
 */
public class PharmacyPaymetMethodSummery {
    private List<String2Value4> bills;
    private double cashTotal;
    private double creditTotal;
    private double cardTotal;

    public List<String2Value4> getBills() {
        return bills;
    }

    public void setBills(List<String2Value4> bills) {
        this.bills = bills;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getCreditTotal() {
        return creditTotal;
    }

    public void setCreditTotal(double creditTotal) {
        this.creditTotal = creditTotal;
    }

    public double getCardTotal() {
        return cardTotal;
    }

    public void setCardTotal(double cardTotal) {
        this.cardTotal = cardTotal;
    }
  
    
    
   
    
}

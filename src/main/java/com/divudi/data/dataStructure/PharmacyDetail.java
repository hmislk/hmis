/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.dataStructure;

import java.util.List;

/**
 *
 * @author safrin
 */
public class PharmacyDetail {
    private List<DatedBills> datedBills;
    private double cashTotal;
    private double creditTotal;
    private double cardTotal;
    private double discount;
    private double netTotal;

    public List<DatedBills> getDatedBills() {
        return datedBills;
    }

    public void setDatedBills(List<DatedBills> datedBills) {
        this.datedBills = datedBills;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
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

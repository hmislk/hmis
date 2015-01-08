/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author safrin
 */
public class DatedBills {

    private Date date;
    private List<Bill> bills;
    List<BillItem> billItems;
    private List<Bill> bills2;
    private double sumNetTotal;
    private double sumDiscount;
    private double sumCashTotal;
    private double sumCreditTotal;
    private double sumCardTotal;

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public double getSumNetTotal() {
        return sumNetTotal;
    }

    public void setSumNetTotal(double sumNetTotal) {
        this.sumNetTotal = sumNetTotal;
    }

    public double getSumDiscount() {
        return sumDiscount;
    }

    public void setSumDiscount(double sumDiscount) {
        this.sumDiscount = sumDiscount;
    }

    public double getSumCashTotal() {
        return sumCashTotal;
    }

    public void setSumCashTotal(double sumCashTotal) {
        this.sumCashTotal = sumCashTotal;
    }

    public double getSumCreditTotal() {
        return sumCreditTotal;
    }

    public void setSumCreditTotal(double sumCreditTotal) {
        this.sumCreditTotal = sumCreditTotal;
    }

    public double getSumCardTotal() {
        return sumCardTotal;
    }

    public void setSumCardTotal(double sumCardTotal) {
        this.sumCardTotal = sumCardTotal;
    }

    public List<Bill> getBills2() {
        return bills2;
    }

    public void setBills2(List<Bill> bills2) {
        this.bills2 = bills2;
    }
}

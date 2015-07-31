/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.BillItem;
import com.divudi.entity.Item;
import java.util.List;

/**
 *
 * @author Buddhika
 */
public class InvestigationSummeryData {
    private Item investigation;
    private List<BillItem> billItems;
    private long count;
    private double total;
    private double turnOverValue;

    public Item getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Item investigation) {
        this.investigation = investigation;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
    
    

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public double getTurnOverValue() {
        return turnOverValue;
    }

    public void setTurnOverValue(double turnOverValue) {
        this.turnOverValue = turnOverValue;
    }
}

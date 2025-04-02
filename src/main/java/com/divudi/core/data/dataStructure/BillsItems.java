/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import java.util.List;

/**
 *
 * @author safrin
 */
public class BillsItems {
    private Bill bill;
    private List<BillItem> billItems;

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

}

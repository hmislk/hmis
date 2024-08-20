/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import java.util.List;

/**
 *
 * @author safrin
 */
public class BillItemFeeCollection {
    private Bill bill;
    private List<BillItemBillFee> billItemBillFee;
    private List<BillComponent> billComponents;

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public List<BillItemBillFee> getBillItemBillFee() {
        return billItemBillFee;
    }

    public void setBillItemBillFee(List<BillItemBillFee> billItemBillFee) {
        this.billItemBillFee = billItemBillFee;
    }

    public List<BillComponent> getBillComponents() {
        return billComponents;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }
}

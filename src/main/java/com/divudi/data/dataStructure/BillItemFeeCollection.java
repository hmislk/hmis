/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

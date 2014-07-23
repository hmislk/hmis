/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.dataStructure;

import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import java.util.List;

/**
 *
 * @author safrin
 */
public class BillItemBillFee {
    private BillItem billItem;
    private List<BillFee> billFees;

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }
}

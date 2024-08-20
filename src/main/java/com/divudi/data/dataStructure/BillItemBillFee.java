/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
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

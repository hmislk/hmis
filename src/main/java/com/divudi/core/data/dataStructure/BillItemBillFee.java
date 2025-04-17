/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
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

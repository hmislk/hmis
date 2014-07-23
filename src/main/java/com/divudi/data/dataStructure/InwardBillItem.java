/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.dataStructure;


import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.BillItem;
import java.util.List;

/**
 *
 * @author safrin
 */
public class InwardBillItem {
    private InwardChargeType inwardChargeType;
    private List<BillItem> billItems;

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }
}

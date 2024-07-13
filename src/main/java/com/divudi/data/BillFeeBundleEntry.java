package com.divudi.data;

import com.divudi.entity.BillFee;
import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class BillFeeBundleEntry {
    private BillFee selectedBillFee;
    private List<BillFee> availableBillFees;

    public BillFee getSelectedBillFee() {
        return selectedBillFee;
    }

    public void setSelectedBillFee(BillFee selectedBillFee) {
        this.selectedBillFee = selectedBillFee;
    }

    public List<BillFee> getAvailableBillFees() {
        return availableBillFees;
    }

    public void setAvailableBillFees(List<BillFee> availableBillFees) {
        this.availableBillFees = availableBillFees;
    }

    
    
}

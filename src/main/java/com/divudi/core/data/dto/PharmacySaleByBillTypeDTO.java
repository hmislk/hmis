package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;

/**
 * DTO for Pharmacy Sale grouped by Bill Type
 * Used in pharmacy history item details tab
 *
 * @author Claude Code
 */
public class PharmacySaleByBillTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BillTypeAtomic billTypeAtomic;
    private Double quantity;

    public PharmacySaleByBillTypeDTO() {
    }

    public PharmacySaleByBillTypeDTO(BillTypeAtomic billTypeAtomic, Double quantity) {
        this.billTypeAtomic = billTypeAtomic;
        this.quantity = quantity;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "PharmacySaleByBillTypeDTO{" +
                "billTypeAtomic=" + billTypeAtomic +
                ", quantity=" + quantity +
                '}';
    }
}
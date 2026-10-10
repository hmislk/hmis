/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.core.entity.Bill;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * What {@link InwardServiceBillService} produced: the individual inward service
 * bills created by this call, and the batch bill they were linked under.
 *
 * @author Buddhika
 */
public class InwardServiceBillResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Only the bills created by this call, not the whole collector. */
    private List<Bill> bills = new ArrayList<>();

    private Bill batchBill;

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }
}

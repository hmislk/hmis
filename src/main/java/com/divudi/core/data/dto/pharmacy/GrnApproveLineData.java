package com.divudi.core.data.dto.pharmacy;

import java.util.Date;

/**
 * Extends {@link GrnLineData} with the additional fields needed only at GRN
 * Approve time: batch/expiry capture on the receiving PharmaceuticalBillItem,
 * and the linkage back to the originating Purchase Order's own BillItem so
 * its remaining/completed quantities can be decremented/incremented.
 * Related issue: #22874
 */
public class GrnApproveLineData extends GrnLineData {

    private Date expiryDate;
    private String batchNo;

    // The PO's own BillItem id whose remainingQty/remainingFreeQty and
    // completedQty/completedFreeQty must be adjusted on Approve.
    private long poBillItemId;

    private double poReceivedQty;
    private double poReceivedFreeQty;

    private double costRatePerUnit;
    // NOTE: wholesaleRatePerUnit is intentionally NOT redeclared here -- it is
    // already inherited from GrnLineData. Redeclaring it in this subclass
    // would field-hide the parent's copy instead of adding a new one.
    private double purchaseRatePerUnit;
    private double retailRatePerUnit;

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public long getPoBillItemId() {
        return poBillItemId;
    }

    public void setPoBillItemId(long poBillItemId) {
        this.poBillItemId = poBillItemId;
    }

    public double getPoReceivedQty() {
        return poReceivedQty;
    }

    public void setPoReceivedQty(double poReceivedQty) {
        this.poReceivedQty = poReceivedQty;
    }

    public double getPoReceivedFreeQty() {
        return poReceivedFreeQty;
    }

    public void setPoReceivedFreeQty(double poReceivedFreeQty) {
        this.poReceivedFreeQty = poReceivedFreeQty;
    }

    public double getCostRatePerUnit() {
        return costRatePerUnit;
    }

    public void setCostRatePerUnit(double costRatePerUnit) {
        this.costRatePerUnit = costRatePerUnit;
    }

    public double getPurchaseRatePerUnit() {
        return purchaseRatePerUnit;
    }

    public void setPurchaseRatePerUnit(double purchaseRatePerUnit) {
        this.purchaseRatePerUnit = purchaseRatePerUnit;
    }

    public double getRetailRatePerUnit() {
        return retailRatePerUnit;
    }

    public void setRetailRatePerUnit(double retailRatePerUnit) {
        this.retailRatePerUnit = retailRatePerUnit;
    }
}

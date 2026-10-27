package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One row per item of the Inpatient Pharmacy Item Summary (issue #24097):
 * every batch of the item combined, split into Direct Issue, Issued on
 * Request, Returned and Cancelled. All quantities and values are held as
 * positive magnitudes; {@link #getNetQty()} and {@link #getNetValue()} apply
 * the signs (issues minus returns minus cancellations).
 */
public class InpatientPharmacyItemMovementDTO implements Serializable {

    private Long itemId;
    private String itemName;
    private double directIssueQty;
    private double directIssueValue;
    private double requestIssueQty;
    private double requestIssueValue;
    private double returnQty;
    private double returnValue;
    private double cancelledQty;
    private double cancelledValue;

    public InpatientPharmacyItemMovementDTO() {
    }

    public InpatientPharmacyItemMovementDTO(Long itemId, String itemName) {
        this.itemId = itemId;
        this.itemName = itemName;
    }

    public void addDirectIssue(double qty, double value) {
        directIssueQty += qty;
        directIssueValue += value;
    }

    public void addRequestIssue(double qty, double value) {
        requestIssueQty += qty;
        requestIssueValue += value;
    }

    public void addReturn(double qty, double value) {
        returnQty += qty;
        returnValue += value;
    }

    public void addCancelled(double qty, double value) {
        cancelledQty += qty;
        cancelledValue += value;
    }

    public double getNetQty() {
        return directIssueQty + requestIssueQty - returnQty - cancelledQty;
    }

    public double getNetValue() {
        return directIssueValue + requestIssueValue - returnValue - cancelledValue;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getDirectIssueQty() {
        return directIssueQty;
    }

    public double getDirectIssueValue() {
        return directIssueValue;
    }

    public double getRequestIssueQty() {
        return requestIssueQty;
    }

    public double getRequestIssueValue() {
        return requestIssueValue;
    }

    public double getReturnQty() {
        return returnQty;
    }

    public double getReturnValue() {
        return returnValue;
    }

    public double getCancelledQty() {
        return cancelledQty;
    }

    public double getCancelledValue() {
        return cancelledValue;
    }
}

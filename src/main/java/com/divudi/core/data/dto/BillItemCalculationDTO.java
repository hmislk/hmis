package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for bulk calculation of issued and cancelled quantities for bill items.
 * Used to eliminate N+1 query patterns when processing transfer requests.
 * 
 * Combines the calculations that were previously done individually for each
 * bill item in getBilledIssuedByRequestedItem() and getCancelledIssuedByRequestedItem().
 */
public class BillItemCalculationDTO implements Serializable {
    
    private Long billItemId;
    private Double originalQty;
    private Double billedIssuedQty;
    private Double cancelledIssuedQty;
    private Double netIssuedQty;       // billedIssued - cancelledIssued
    private Double remainingQty;       // originalQty - netIssuedQty
    
    // ------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------
    
    public BillItemCalculationDTO() {
    }
    
    /**
     * Constructor for bulk query results.
     * Automatically calculates derived fields.
     */
    public BillItemCalculationDTO(Long billItemId, Double originalQty, Double billedIssued, Double cancelledIssued) {
        this.billItemId = billItemId;
        this.originalQty = originalQty != null ? originalQty : 0.0;
        this.billedIssuedQty = billedIssued != null ? billedIssued : 0.0;
        this.cancelledIssuedQty = cancelledIssued != null ? cancelledIssued : 0.0;
        
        // Calculate derived fields
        this.netIssuedQty = Math.abs(this.billedIssuedQty) - Math.abs(this.cancelledIssuedQty);
        this.remainingQty = this.originalQty - this.netIssuedQty;
        
        // Ensure remaining quantity is not negative
        if (this.remainingQty < 0) {
            this.remainingQty = 0.0;
        }
    }
    
    // ------------------------------------------------------------------
    // Getters & Setters
    // ------------------------------------------------------------------
    
    public Long getBillItemId() {
        return billItemId;
    }
    
    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }
    
    public Double getOriginalQty() {
        return originalQty;
    }
    
    public void setOriginalQty(Double originalQty) {
        this.originalQty = originalQty;
    }
    
    public Double getBilledIssuedQty() {
        return billedIssuedQty;
    }
    
    public void setBilledIssuedQty(Double billedIssuedQty) {
        this.billedIssuedQty = billedIssuedQty;
    }
    
    public Double getCancelledIssuedQty() {
        return cancelledIssuedQty;
    }
    
    public void setCancelledIssuedQty(Double cancelledIssuedQty) {
        this.cancelledIssuedQty = cancelledIssuedQty;
    }
    
    public Double getNetIssuedQty() {
        return netIssuedQty;
    }
    
    public void setNetIssuedQty(Double netIssuedQty) {
        this.netIssuedQty = netIssuedQty;
    }
    
    public Double getRemainingQty() {
        return remainingQty;
    }
    
    public void setRemainingQty(Double remainingQty) {
        this.remainingQty = remainingQty;
    }
    
    // ------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------
    
    /**
     * Check if there is any quantity remaining to be issued.
     */
    public boolean hasRemainingQty() {
        return remainingQty != null && remainingQty > 0.001; // Small tolerance for floating point precision
    }
    
    /**
     * Check if the item is fully issued.
     */
    public boolean isFullyIssued() {
        return !hasRemainingQty();
    }
    
    @Override
    public String toString() {
        return "BillItemCalculationDTO{" +
                "billItemId=" + billItemId +
                ", originalQty=" + originalQty +
                ", billedIssuedQty=" + billedIssuedQty +
                ", cancelledIssuedQty=" + cancelledIssuedQty +
                ", netIssuedQty=" + netIssuedQty +
                ", remainingQty=" + remainingQty +
                '}';
    }
}
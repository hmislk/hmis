package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public enum RequestStatus {
    
    // In-progress states
    PENDING("Pending"),
    
    // Review states
    UNDER_REVIEW("Under Review"),
    
    // Approval/Rejection states
    APPROVED("Approved"),
    REJECTED("Rejected"),
    
    // Execution states
    COMPLETED("Completed"),
    
    // Terminal states
    CANCELLED("Cancelled"),
    EXPIRED("Expired");
    
    
    private final String label;

    private RequestStatus(String label) {
        this.label = label;
    }
    
    
    public String getLabel() {
        return label;
    }

}

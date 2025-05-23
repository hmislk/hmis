package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public enum AuditEventStatus {
    STARTED("Started"),
    COMPLETED("Completed"),
    FAILED("Failed");
    
    private final String label;

    AuditEventStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public enum RequestCategory {
    
    CANCELLATION("Cancellation"),
    REFUND("Refund"),
    EDIT("Edit"),
    ADJUSTMENT("Adjustment");

    private final String label;

    RequestCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

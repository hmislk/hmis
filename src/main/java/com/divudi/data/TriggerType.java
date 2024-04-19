package com.divudi.data;

/**
 * Enum for defining various icons with human-readable labels. Note: Image and
 * action paths are removed as per request.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public enum TriggerType {
    Order_Request("Order Request - System Notification"),
    Order_Request_Sms("Order Request - SMS"),
    Order_Request_Email("Order Request - Email"),
    Transfer_Issue("Transfer Isuue"),
    Order_Approval("Order Aproval");

    private final String label;

    TriggerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

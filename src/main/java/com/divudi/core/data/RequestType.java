package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public enum RequestType {
    // Cancellation types
    BILL_CANCELLATION("Bill Cancellation", RequestCategory.CANCELLATION),
    
    // Refund types ()
    ITEM_REFUND("Item Refund", RequestCategory.REFUND),
    FULL_REFUND("Full Refund", RequestCategory.REFUND),
    PARTIAL_REFUND("Partial Refund", RequestCategory.REFUND),
    SERVICE_REFUND("Service Refund", RequestCategory.REFUND),
    
    // Edit types
    EDIT_REQUEST("Edit Request", RequestCategory.EDIT),
    INFORMATION_UPDATE("Information Update", RequestCategory.EDIT),
    QUANTITY_CHANGE("Quantity Change", RequestCategory.EDIT),
    DATE_MODIFICATION("Date Modification", RequestCategory.EDIT);
    
    private final String displayName;
    private final RequestCategory category;
    
    RequestType(String displayName, RequestCategory category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RequestCategory getCategory() {
        return category;
    }
    
}

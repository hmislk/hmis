package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public enum RequestType {
    // Cancellation types
    BILL_CANCELLATION("Bill Cancellation", RequestCategory.CANCELLATION,"CAN"),
    
    // Refund types
    ITEM_REFUND("Item Refund", RequestCategory.REFUND,"I-REF"),
    FULL_REFUND("Full Refund", RequestCategory.REFUND,"F-REF"),
    PARTIAL_REFUND("Partial Refund", RequestCategory.REFUND,"P-REF"),
    SERVICE_REFUND("Service Refund", RequestCategory.REFUND,"S-REF"),
    
    // Edit types
    EDIT_REQUEST("Edit Request", RequestCategory.EDIT,"EDT"),
    INFORMATION_UPDATE("Information Update", RequestCategory.EDIT,"UPT"),
    QUANTITY_CHANGE("Quantity Change", RequestCategory.EDIT,"QCH"),
    DATE_MODIFICATION("Date Modification", RequestCategory.EDIT,"DMOD");
    
    private final String displayName;
    private final RequestCategory category;
    private final String code;
    
    RequestType(String displayName, RequestCategory category,String code) {
        this.displayName = displayName;
        this.category = category;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RequestCategory getCategory() {
        return category;
    }

    public String getCode() {
        return code;
    }
    
}

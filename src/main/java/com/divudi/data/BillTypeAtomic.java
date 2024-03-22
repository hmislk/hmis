/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;


/**
 *
 * @author Buddhika
 */
public enum BillTypeAtomic {

    // Adjustments
    PHARMACY_RETAIL_SALE("Pharmacy Retail Sale"),
    PHARMACY_RETAIL_SALE_PRE("Pharmacy Retail Sale Pre"),
    PHARMACY_RETAIL_SALE_CANCELLED("Pharmacy Retail Sale Cancelled"),
    PHARMACY_RETAIL_SALE_REFUND("Pharmacy Retail Sale Refund"),

    PHARMACY_SALE_WITHOUT_STOCK("Pharmacy Sale Without Stock"),
    PHARMACY_SALE_WITHOUT_STOCK_PRE("Pharmacy Sale Without Stock Pre"),
    PHARMACY_SALE_WITHOUT_STOCK_CANCELLED("Pharmacy Sale Without Stock Cancelled"),
    PHARMACY_SALE_WITHOUT_STOCK_REFUND("Pharmacy Sale Without Stock Refund"),
    
    
    PHARMACY_WHOLESALE("Pharmacy Wholesale"),
    PHARMACY_WHOLESALE_PRE("Pharmacy Wholesale Pre"),
    PHARMACY_WHOLESALE_CANCELLED("Pharmacy Wholesale Cancelled"),
    PHARMACY_WHOLESALE_REFUND("Pharmacy Wholesale Refund"),
    
    
    PHARMACY_ORDER("Pharmacy Order"),
    PHARMACY_ORDER_PRE("Pharmacy Order Pre"),
    PHARMACY_ORDER_CANCELLED("Pharmacy Order Cancelled"),
    PHARMACY_ORDER_APPROVAL("Pharmacy Order Approval"),
    PHARMACY_ORDER_APPROVAL_CANCELLED("Pharmacy Order Approval Cancell"),
    
    PHARMACY_DIRECT_PURCHASE("Pharmacy Direct Purchase"),
    PHARMACY_DIRECT_PURCHASE_CANCELLED("Pharmacy Direct Purchase Cancelled"),
    PHARMACY_DIRECT_PURCHASE_REFUND("Pharmacy Direct Purchase Refund"),
    
    PHARMACY_GRN("Pharmacy GRN"),
    PHARMACY_GRN_CANCELLED("Pharmacy GRN Cancelled"),
    PHARMACY_GRN_REFUND("Pharmacy GRN Refund"),
    
    PHARMACY_GRN_WHOLESALE("Pharmacy GRN"),
    PHARMACY_GRN_CANCELLED_WHOLESALE("Pharmacy GRN Cancelled"),
    PHARMACY_GRN_REFUND_WHOLESALE("Pharmacy GRN Refund"),
    
    PHARMACY_GRN_PAYMENT("GRN Payment"),
    PHARMACY_GRN_PAYMENT_CANCELLED("GRN Payment Cancelled"),
    
    
    //To Do - Improve
    PHARMACY_ADJUSTMENT("Pharmacy Adjustment"),
    PHARMACY_ADJUSTMENT_CANCELLED("Pharmacy Adjustment Cancelled"),

    PHARMACY_TRANSFER("Pharmacy Transfer"),
    PHARMACY_TRANSFER_CANCELLED("Pharmacy Transfer Cancelled"),
    PHARMACY_ISSUE("Pharmacy Issue"),
    PHARMACY_ISSUE_CANCELLED("Pharmacy Issue Cancelled"),
    PHARMACY_RECEIVE("Pharmacy Receive"),
    PHARMACY_RECEIVE_CANCELLED("Pharmacy Receive Cancelled");

    private final String label;

    BillTypeAtomic(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }


}

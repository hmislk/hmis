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
    PHARMACY_TRANSFER_REQUEST("Pharmacy Transfer Request"),
    PHARMACY_TRANSFER_REQUEST_CANCELLED("Pharmacy Transfer Request Cancelled"),
    PHARMACY_ISSUE("Pharmacy Issue"),
    PHARMACY_ISSUE_CANCELLED("Pharmacy Issue Cancelled"),
    PHARMACY_RECEIVE("Pharmacy Receive"),
    PHARMACY_RECEIVE_CANCELLED("Pharmacy Receive Cancelled"),
    CHANNEL_BOOKING_PAYMENT("Channel Booking and Payment"),
    CHANNEL_BOOKING_WITHOUT_PAYMENT("Channel Booking without Payment"),
    CHANNEL_PAYMENT_FOR_BOOKING("Channel Payment for Booking"),
    CHANNEL_CANCELLATION_WITH_PAYMENT("Channel Cancellation with Payment"),
    CHANNEL_CANCELLATION_FOR_NON_PAIDS("Channel Cancellation for Non-Paids"),
    CHANNEL_REFUND("Channel Refund"),
    //
    OPD_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill"), //TO DO : Add more bill Types
    //
    OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Batch Bill to Collect Payment at Cashier"),
    OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Opd Batch Bill Payment Collection at Cashier"),
    OPD_BATCH_BILL_WITH_PAYMENT("Opd Batch Bill with Payment Collection"),
    OPD_BATCH_BILL_CANCELLATION("Opd Batch Bill Cancellation"),
    //
    OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Bill to Collect Payment at Cashier"),
    OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("OPD Bill Payment Collection at Cashier"),
    OPD_BILL_WITH_PAYMENT("OPD Bill Payment with Payment"),
    OPD_BILL_CANCELLATION("Opd Bill Cancellation"),
    OPD_BILL_REFUND("Opd Bill Refund"),;

    private final String label;

    BillTypeAtomic(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}

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

    // Pharmacy
    PHARMACY_RETAIL_SALE("Pharmacy Retail Sale", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_PRE("Pharmacy Retail Sale Pre", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_CANCELLED("Pharmacy Retail Sale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_REFUND("Pharmacy Retail Sale Refund", BillCategory.REFUND, ServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK("Pharmacy Sale Without Stock", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK_PRE("Pharmacy Sale Without Stock Pre", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK_CANCELLED("Pharmacy Sale Without Stock Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK_REFUND("Pharmacy Sale Without Stock Refund", BillCategory.REFUND, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE("Pharmacy Wholesale", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_PRE("Pharmacy Wholesale Pre", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_CANCELLED("Pharmacy Wholesale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_REFUND("Pharmacy Wholesale Refund", BillCategory.REFUND, ServiceType.PHARMACY),
    PHARMACY_ORDER("Pharmacy Order", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_ORDER_PRE("Pharmacy Order Pre", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_ORDER_CANCELLED("Pharmacy Order Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_ORDER_APPROVAL("Pharmacy Order Approval", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_ORDER_APPROVAL_CANCELLED("Pharmacy Order Approval Cancell", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_DIRECT_PURCHASE("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_DIRECT_PURCHASE_CANCELLED("Pharmacy Direct Purchase Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_DIRECT_PURCHASE_REFUND("Pharmacy Direct Purchase Refund", BillCategory.REFUND, ServiceType.PHARMACY),
    PHARMACY_GRN("Pharmacy GRN", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_GRN_WHOLESALE("Pharmacy Wholesale GRN", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_GRN_CANCELLED("Pharmacy GRN Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_GRN_REFUND("Pharmacy GRN Refund", BillCategory.REFUND, ServiceType.PHARMACY),
    // Wholesale Transactions
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED("Pharmacy Direct Purchase - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND("Pharmacy Direct Purchase - Refund", BillCategory.REFUND, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_GRN_BILL("Pharmacy GRN", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_GRN_BILL_CANCELLED("Pharmacy GRN - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_WHOLESALE_GRN_BILL_REFUND("Pharmacy GRN - Refund", BillCategory.REFUND, ServiceType.PHARMACY),
    PHARMACY_GRN_PAYMENT("GRN Payment", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_GRN_PAYMENT_CANCELLED("GRN Payment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_ADJUSTMENT("Pharmacy Adjustment", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_ADJUSTMENT_CANCELLED("Pharmacy Adjustment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_TRANSFER_REQUEST("Pharmacy Transfer Request", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_TRANSFER_REQUEST_CANCELLED("Pharmacy Transfer Request Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_ISSUE("Pharmacy Issue", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_ISSUE_CANCELLED("Pharmacy Issue Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    PHARMACY_RECEIVE("Pharmacy Receive", BillCategory.BILL, ServiceType.PHARMACY),
    PHARMACY_RECEIVE_CANCELLED("Pharmacy Receive Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY),
    // CHANNELLING
    CHANNEL_BOOKING_WITH_PAYMENT("Channel Booking and Payment", BillCategory.BILL, ServiceType.CHANNELLING),
    CHANNEL_BOOKING_WITHOUT_PAYMENT("Channel Booking without Payment", BillCategory.BILL, ServiceType.CHANNELLING),
    CHANNEL_PAYMENT_FOR_BOOKING_BILL("Channel Payment for Booking", BillCategory.BILL, ServiceType.CHANNELLING),
    CHANNEL_CANCELLATION_WITH_PAYMENT("Channel Cancellation with Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING),
    CHANNEL_CANCELLATION_WITHOUT_PAYMENT("Channel Cancellation without Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING),
    CHANNEL_REFUND("Channel Refund", BillCategory.REFUND, ServiceType.CHANNELLING),
    // OPD
    OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Batch Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD),
    OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Opd Batch Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD),
    OPD_BATCH_BILL_WITH_PAYMENT("Opd Batch Bill with Payment Collection", BillCategory.BILL, ServiceType.OPD),
    OPD_BATCH_BILL_CANCELLATION("Opd Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD),
    OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD),
    OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("OPD Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD),
    OPD_BILL_WITH_PAYMENT("OPD Bill Payment with Payment", BillCategory.BILL, ServiceType.OPD),
    OPD_BILL_CANCELLATION("Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD),
    OPD_BILL_REFUND("Opd Bill Refund", BillCategory.REFUND, ServiceType.OPD),
    OPD_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill", BillCategory.BILL, ServiceType.OPD),
    // Colleting Centre
    CC_BATCH_BILL("Collecting Centre Batch Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE),
    CC_BATCH_BILL_CANCELLATION("Collecting Centre  Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE),
    CC_BILL("Collecting Centre  Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE),
    CC_BILL_CANCELLATION("Collecting Centre  Bill Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE),
    CC_BILL_REFUND("Collecting Centre  Bill Refund", BillCategory.REFUND, ServiceType.COLLECTING_CENTRE),
    // Fund Transactions
    FUND_SHIFT_START_BILL("Shift Start Fund Bill", BillCategory.BILL, ServiceType.OTHER),
    FUND_SHIFT_END_BILL("Shift End Fund Bill", BillCategory.BILL, ServiceType.OTHER),
    FUND_TRANSFER_BILL("Fund Transfer Bill", BillCategory.BILL, ServiceType.OTHER),
    FUND_TRANSFER_RECEIVED_BILL("Fund Transfer Received Bill", BillCategory.BILL, ServiceType.OTHER),
    FUND_DEPOSIT_BILL("Deposit Fund Bill", BillCategory.BILL, ServiceType.OTHER),
    FUND_WITHDRAWAL_BILL("Withdrawal Fund Bill", BillCategory.BILL, ServiceType.OTHER),
    // Including cancellations for each of the above with updated names to match the new bill names
    FUND_SHIFT_START_BILL_CANCELLED("Shift Start Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER),
    FUND_SHIFT_END_BILL_CANCELLED("Shift End Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER),
    FUND_TRANSFER_BILL_CANCELLED("Fund Transfer Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER),
    FUND_TRANSFER_RECEIVED_BILL_CANCELLED("Fund Transfer Received Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER),
    FUND_DEPOSIT_BILL_CANCELLED("Deposit Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER),
    FUND_WITHDRAWAL_BILL_CANCELLED("Withdrawal Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER);

    private final String label;
    private final BillCategory billCategory;
    private final ServiceType serviceType;

    BillTypeAtomic(String label, BillCategory billCategory, ServiceType serviceType) {
        this.label = label;
        this.billCategory = billCategory;
        this.serviceType = serviceType;
    }

    public String getLabel() {
        return label;
    }

    public BillCategory getBillCategory() {
        return billCategory;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

}

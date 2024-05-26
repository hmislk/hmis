/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Buddhika
 */
public enum BillTypeAtomic {

    //INWARD
    INWARD_PHARMACY_REQUEST("Inward Request Medicines From Pharmacy", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    INWARD_SERVICE_BATCH_BILL("Inward Service Batch Bill", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    INWARD_SERVICE_BATCH_BILL_CANCELLATION("Inward Service Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    INWARD_SERVICE_BILL("Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    INWARD_SERVICE_BILL_CANCELLATION("Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    INWARD_SERVICE_BATCH_BILL_REFUND("Opd Bill Refund", BillCategory.REFUND, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    INWARD_SERVICE_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    // Pharmacy
    PHARMACY_RETAIL_SALE("Pharmacy Retail Sale", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_RETAIL_SALE_PRE("Pharmacy Retail Sale Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    @Deprecated
    PHARMACY_RETAIL_SALE_PRE_SETTLE_AT_CASHIER("NOT USED - Pharmacy Retail Sale Pre Bill Settled At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER("Pharmacy Retail Sale Pre Bill Settled At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER("Pharmacy Retail Sale Pre Bill To Settle At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_RETAIL_SALE_CANCELLED("Pharmacy Retail Sale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_RETAIL_SALE_REFUND("Pharmacy Retail Sale Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY("Pharmacy Retail Sale Return Items Only", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS("Pharmacy Retail Sale Return Item Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS("Pharmacy Retail Sale Return Items And Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_SALE_WITHOUT_STOCK("Pharmacy Sale Without Stock", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_SALE_WITHOUT_STOCK_PRE("Pharmacy Sale Without Stock Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_SALE_WITHOUT_STOCK_CANCELLED("Pharmacy Sale Without Stock Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_SALE_WITHOUT_STOCK_REFUND("Pharmacy Sale Without Stock Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK("Pharmacy Retail Sale Pre Bill Add to Stock", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_WHOLESALE("Pharmacy Wholesale", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_WHOLESALE_PRE("Pharmacy Wholesale Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_WHOLESALE_CANCELLED("Pharmacy Wholesale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_WHOLESALE_REFUND("Pharmacy Wholesale Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_ORDER("Pharmacy Order", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_ORDER_PRE("Pharmacy Order Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_ORDER_CANCELLED("Pharmacy Order Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_ORDER_APPROVAL("Pharmacy Order Approval", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_ORDER_APPROVAL_CANCELLED("Pharmacy Order Approval Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_DIRECT_PURCHASE("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_DIRECT_PURCHASE_CANCELLED("Pharmacy Direct Purchase Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_DIRECT_PURCHASE_REFUND("Pharmacy Direct Purchase Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_GRN("Pharmacy GRN", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_GRN_WHOLESALE("Pharmacy Wholesale GRN", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_GRN_CANCELLED("Pharmacy GRN Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_GRN_REFUND("Pharmacy GRN Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_GRN_RETURN("Pharmacy GRN Return", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED("Pharmacy Direct Purchase - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND("Pharmacy Direct Purchase - Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_WHOLESALE_GRN_BILL("Pharmacy GRN", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_WHOLESALE_GRN_BILL_CANCELLED("Pharmacy GRN - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_WHOLESALE_GRN_BILL_REFUND("Pharmacy GRN - Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_GRN_PAYMENT("GRN Payment", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    PHARMACY_GRN_PAYMENT_CANCELLED("GRN Payment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_ADJUSTMENT("Pharmacy Adjustment", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_ADJUSTMENT_CANCELLED("Pharmacy Adjustment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_TRANSFER_REQUEST("Pharmacy Transfer Request", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_TRANSFER_REQUEST_CANCELLED("Pharmacy Transfer Request Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_ISSUE("Pharmacy Issue", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_ISSUE_CANCELLED("Pharmacy Issue Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_DIRECT_ISSUE("Pharmacy Direct Issue", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_DIRECT_ISSUE_CANCELLED("Pharmacy Direct Issue Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_RECEIVE("Pharmacy Receive", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PHARMACY_RECEIVE_CANCELLED("Pharmacy Receive Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    @Deprecated
    PHARMACY_RETURN_ITEMS_Only("Pharmacy Return Items Only", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    @Deprecated
    PHARMACY_RETURN_ITEMS_AND_PAYMENTS("Pharmacy Return Items And Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT),
    PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION("Pharmacy Return Items And Payments Cancellation", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_IN),
    MULTIPLE_PHARMACY_ORDER_CANCELLED_BILL("Multiple Pharmacy Purchase Order Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    // CHANNELLING
    CHANNEL_BOOKING_WITH_PAYMENT("Channel Booking and Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.CASH_IN),
    CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT("Channel Booking For Online Payment - Pending Confirmation", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    @Deprecated
    CHANNEL_BOOKING_PAYMENT("Channel Booking For Online Payment - Pending Confirmation", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    CHANNEL_BOOKING_WITH_PAYMENT_ONLINE("Channel Booking Online Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.ONLINE_PAYMENT_IN),
    CHANNEL_BOOKING_WITHOUT_PAYMENT("Channel Booking without Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    CHANNEL_PAYMENT_FOR_BOOKING_BILL("Channel Payment for Booking", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.CASH_IN),
    CHANNEL_CANCELLATION_WITH_PAYMENT("Channel Cancellation with Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT),
    CHANNEL_CANCELLATION_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS("Channel Cancellation with Payment for Bills Where Credit Payment was Settled", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT),
    CHANNEL_REFUND_WITH_PAYMENT("Channel Refund with Payment", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT),
    CHANNEL_REFUND_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS("Channel Refund with Payment for Bills where Credit Payment was Settled", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT),
    CHANNEL_CANCELLATION_WITHOUT_PAYMENT("Channel Cancellation without Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    CHANNEL_REFUND("Channel Refund", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT),
    // OPD
    OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Batch Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Opd Batch Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN),
    OPD_BATCH_BILL_WITH_PAYMENT("Opd Batch Bill with Payment Collection", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN),
    OPD_BATCH_BILL_CANCELLATION("Opd Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("OPD Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    OPD_BILL_WITH_PAYMENT("OPD Bill Payment with Payment", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    OPD_BILL_CANCELLATION("Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT),
    OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT),
    OPD_BILL_REFUND("Opd Bill Refund", BillCategory.REFUND, ServiceType.OPD, BillFinanceType.CASH_OUT),
    OPD_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_OUT),
    //Packages
    PACKAGE_OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Package Opd Batch Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Package Opd Batch Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN),
    PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT("Package Opd Batch Bill with Payment Collection", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN),
    PACKAGE_OPD_BATCH_BILL_CANCELLATION("Package Opd Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Package Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("Package OPD Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PACKAGE_OPD_BILL_WITH_PAYMENT("Package OPD Bill Payment with Payment", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS),
    PACKAGE_OPD_BILL_CANCELLATION("Package Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT),
    PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Package Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT),
    PACKAGE_OPD_BILL_REFUND("Package Opd Bill Refund", BillCategory.REFUND, ServiceType.OPD, BillFinanceType.CASH_OUT),
    // Collecting Centre
    CC_BATCH_BILL("Collecting Centre Batch Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_IN),
    CC_BATCH_BILL_CANCELLATION("Collecting Centre Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT),
    CC_BILL("Collecting Centre Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_IN),
    CC_BILL_CANCELLATION("Collecting Centre Bill Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT),
    CC_BILL_REFUND("Collecting Centre Bill Refund", BillCategory.REFUND, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT),
    // Float Transactions
    FUND_SHIFT_START_BILL("Shift Start Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_STARTING_BALANCE),
    FUND_SHIFT_START_BILL_CANCELLED("Shift Start Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_STARTING_BALANCE),
    FUND_SHIFT_END_BILL("Shift End Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE),
    FUND_SHIFT_END_BILL_CANCELLED("Shift End Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE),
    FUND_TRANSFER_BILL("Fund Transfer Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE),
    FUND_TRANSFER_BILL_CANCELLED("Fund Transfer Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE),
    FUND_TRANSFER_RECEIVED_BILL("Fund Transfer Received Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE),
    FUND_TRANSFER_RECEIVED_BILL_CANCELLED("Fund Transfer Received Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE),
    FUND_DEPOSIT_BILL("Deposit Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.BANK_OUT),
    FUND_DEPOSIT_BILL_CANCELLED("Deposit Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.BANK_IN),
    FUND_WITHDRAWAL_BILL("Withdrawal Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.BANK_IN),
    FUND_WITHDRAWAL_BILL_CANCELLED("Withdrawal Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.BANK_OUT),
    // Professional Payments
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE("Inward Payment for Staff", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE("Channelling Payment for Staff", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_OUT),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION("Channelling session Payment for Staff", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_OUT),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES("Channelling Payment for Staff for agencies", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES("OPD Professional Payment bill", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_OUT),
    PETTY_CASH_ISSUE("Petty Cash Issue", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_OUT),
    PETTY_CASH_RETURN("Petty Cash Return", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN),
    IOU_CASH_ISSUE("Iou Cash Issue", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_OUT),
    IOU_CASH_RETURN("Iou Cash Return", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN);


    private final String label;
    private final BillCategory billCategory;
    private final ServiceType serviceType;
    private final BillFinanceType billFinanceType;

    BillTypeAtomic(String label, BillCategory billCategory, ServiceType serviceType, BillFinanceType billFinanceType) {
        this.label = label;
        this.billCategory = billCategory;
        this.serviceType = serviceType;
        this.billFinanceType = billFinanceType;
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

    public BillFinanceType getBillFinanceType() {
        return billFinanceType; // This is the getter method you need
    }

    // Method to find BillTypeAtomic by BillFinanceType
    public static List<BillTypeAtomic> findByFinanceType(BillFinanceType financeType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getBillFinanceType() == financeType)
                .collect(Collectors.toList());
    }

    public static List<BillTypeAtomic> findByCategory(BillCategory category) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getBillCategory() == category)
                .collect(Collectors.toList());
    }
}

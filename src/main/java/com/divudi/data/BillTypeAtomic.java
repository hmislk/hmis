package com.divudi.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumerates types of bills for atomic billing purposes. Includes
 * categorization by service type and finance type.
 */
public enum BillTypeAtomic {

    // INWARD
    INWARD_PHARMACY_REQUEST("Inward Request Medicines From Pharmacy", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    INWARD_SERVICE_BATCH_BILL("Inward Service Batch Bill", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    INWARD_SERVICE_BATCH_BILL_CANCELLATION("Inward Service Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    INWARD_SERVICE_BILL("Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    INWARD_SERVICE_BILL_CANCELLATION("Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    INWARD_SERVICE_BATCH_BILL_REFUND("Opd Bill Refund", BillCategory.REFUND, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    INWARD_SERVICE_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD),
    // STORE
    STORE_ORDER("Store Order", BillCategory.BILL, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE),
    STORE_ORDER_PRE("Store Order Pre", BillCategory.BILL, ServiceType.STORE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.STORE),
    STORE_ORDER_CANCELLED("Store Order Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE),
    STORE_ORDER_APPROVAL("Store Order Approval", BillCategory.BILL, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE),
    STORE_ORDER_APPROVAL_CANCELLED("Store Order Approval Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE),
    STORE_DIRECT_PURCHASE("Store Direct Purchase", BillCategory.BILL, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE),
    STORE_DIRECT_PURCHASE_CANCELLED("Store Direct Purchase Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE),
    STORE_DIRECT_PURCHASE_REFUND("Store Direct Purchase Refund", BillCategory.REFUND, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE),
    STORE_GRN("Store GRN", BillCategory.PAYMENTS, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE),
    STORE_GRN_CANCELLED("Store GRN Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE),
    STORE_GRN_REFUND("Store GRN Refund", BillCategory.REFUND, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE),
    STORE_GRN_RETURN("Store GRN Return", BillCategory.REFUND, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE),
    // PHARMACY
    PHARMACY_RETAIL_SALE("Pharmacy Retail Sale", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_PRE("Pharmacy Retail Sale Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    @Deprecated
    PHARMACY_RETAIL_SALE_PRE_SETTLE_AT_CASHIER("NOT USED - Pharmacy Retail Sale Pre Bill Settled At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER("Pharmacy Retail Sale Pre Bill Settled At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER("Pharmacy Retail Sale Pre Bill To Settle At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_CANCELLED("Pharmacy Retail Sale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_REFUND("Pharmacy Retail Sale Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY("Pharmacy Retail Sale Return Items Only", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS("Pharmacy Retail Sale Return Item Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS("Pharmacy Retail Sale Return Items And Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK("Pharmacy Sale Without Stock", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK_PRE("Pharmacy Sale Without Stock Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK_CANCELLED("Pharmacy Sale Without Stock Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_SALE_WITHOUT_STOCK_REFUND("Pharmacy Sale Without Stock Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK("Pharmacy Retail Sale Pre Bill Add to Stock", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE("Pharmacy Wholesale", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_PRE("Pharmacy Wholesale Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_CANCELLED("Pharmacy Wholesale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_REFUND("Pharmacy Wholesale Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_ORDER("Pharmacy Order", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_ORDER_PRE("Pharmacy Order Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_ORDER_CANCELLED("Pharmacy Order Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_ORDER_APPROVAL("Pharmacy Order Approval", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_ORDER_APPROVAL_CANCELLED("Pharmacy Order Approval Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_DIRECT_PURCHASE("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_DIRECT_PURCHASE_CANCELLED("Pharmacy Direct Purchase Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_DIRECT_PURCHASE_REFUND("Pharmacy Direct Purchase Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_GRN("Pharmacy GRN", BillCategory.PAYMENTS, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_GRN_PRE("Pharmacy GRN Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_GRN_WHOLESALE("Pharmacy Wholesale GRN", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_GRN_CANCELLED("Pharmacy GRN Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_GRN_REFUND("Pharmacy GRN Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_GRN_RETURN("Pharmacy GRN Return", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED("Pharmacy Direct Purchase - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND("Pharmacy Direct Purchase - Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_GRN_BILL("Pharmacy GRN", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_GRN_BILL_CANCELLED("Pharmacy GRN - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_WHOLESALE_GRN_BILL_REFUND("Pharmacy GRN - Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_GRN_PAYMENT("GRN Payment", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    PHARMACY_GRN_PAYMENT_CANCELLED("GRN Payment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_ADJUSTMENT("Pharmacy Adjustment", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_ADJUSTMENT_CANCELLED("Pharmacy Adjustment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_TRANSFER_REQUEST("Pharmacy Transfer Request", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_TRANSFER_REQUEST_PRE("Pharmacy Transfer Request Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_TRANSFER_REQUEST_CANCELLED("Pharmacy Transfer Request Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_ISSUE("Pharmacy Issue", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_ISSUE_CANCELLED("Pharmacy Issue Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_DIRECT_ISSUE("Pharmacy Direct Issue", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_DIRECT_ISSUE_CANCELLED("Pharmacy Direct Issue Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_RECEIVE("Pharmacy Receive", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_RECEIVE_PRE("Pharmacy Receive Request", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    PHARMACY_RECEIVE_CANCELLED("Pharmacy Receive Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    @Deprecated
    PHARMACY_RETURN_ITEMS_Only("Pharmacy Return Items Only", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    @Deprecated
    PHARMACY_RETURN_ITEMS_AND_PAYMENTS("Pharmacy Return Items And Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY),
    PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION("Pharmacy Return Items And Payments Cancellation", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY),
    MULTIPLE_PHARMACY_ORDER_CANCELLED_BILL("Multiple Pharmacy Purchase Order Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY),
    // CHANNELLING
    CHANNEL_BOOKING_WITH_PAYMENT("Channel Booking and Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.CASH_IN, CountedServiceType.CHANNELLING),
    CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT("Channel Booking For Online Payment - Pending Confirmation", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING),
    CHANNEL_RESHEDULE_WITH_PAYMENT("Channel Reshedule for paid Appointment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING),
    CHANNEL_RESHEDULE_WITH_OUT_PAYMENT("Channel Reshedule For Non Paid Appointment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING),
    @Deprecated
    CHANNEL_BOOKING_PAYMENT("Channel Booking For Online Payment - Pending Confirmation", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING),
    CHANNEL_BOOKING_WITH_PAYMENT_ONLINE("Channel Booking Online Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.ONLINE_PAYMENT_IN, CountedServiceType.CHANNELLING),
    CHANNEL_BOOKING_WITHOUT_PAYMENT("Channel Booking without Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING),
    CHANNEL_PAYMENT_FOR_BOOKING_BILL("Channel Payment for Booking", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.CASH_IN, CountedServiceType.CHANNELLING),
    CHANNEL_CANCELLATION_WITH_PAYMENT("Channel Cancellation with Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING),
    CHANNEL_CANCELLATION_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS("Channel Cancellation with Payment for Bills Where Credit Payment was Settled", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING),
    CHANNEL_REFUND_WITH_PAYMENT("Channel Refund with Payment", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING),
    CHANNEL_REFUND_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS("Channel Refund with Payment for Bills where Credit Payment was Settled", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING),
    CHANNEL_CANCELLATION_WITHOUT_PAYMENT("Channel Cancellation without Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING),
    CHANNEL_REFUND("Channel Refund", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING),
    // OPD_IN
    OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Batch Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.NONE),
    OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Opd Batch Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.NONE),
    OPD_BATCH_BILL_WITH_PAYMENT("Opd Batch Bill with Payment Collection", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.NONE),
    OPD_BATCH_BILL_CANCELLATION("Opd Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.NONE),
    OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.NONE),
    OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("OPD Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    OPD_BILL_WITH_PAYMENT("OPD Bill Payment with Payment", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    OPD_BILL_CANCELLATION("Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_OUT),
    OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_OUT),
    OPD_BILL_REFUND("Opd Bill Refund", BillCategory.REFUND, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_OUT),
    OPD_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill", BillCategory.PAYMENTS, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_PROFESSIONAL_PAYMENT),
    OPD_PROFESSIONAL_PAYMENT_BILL_RETURN("OPD Professional Payment bill", BillCategory.PAYMENTS, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_PROFESSIONAL_PAYMENT_RETURN),
    @Deprecated
    OPD_BILL_WITH_PAYMENT_UNDER_BATCH_BILL("OPD Bill with payment under batch bill", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    // PACKAGES
    PACKAGE_OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Package Opd Batch Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Package Opd Batch Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT("Package Opd Batch Bill with Payment Collection", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BATCH_BILL_CANCELLATION("Package Opd Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Package Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("Package OPD Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BILL_WITH_PAYMENT("Package OPD Bill Payment with Payment", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BILL_CANCELLATION("Package Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Package Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_IN),
    PACKAGE_OPD_BILL_REFUND("Package Opd Bill Refund", BillCategory.REFUND, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_IN),
    // COLLECTING CENTRE
    CC_BATCH_BILL("Collecting Centre Batch Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_IN, CountedServiceType.COLLECTING_CENTRE),
    CC_BATCH_BILL_CANCELLATION("Collecting Centre Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE),
    CC_BILL("Collecting Centre Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_IN, CountedServiceType.COLLECTING_CENTRE),
    CC_BILL_CANCELLATION("Collecting Centre Bill Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE),
    CC_BILL_REFUND("Collecting Centre Bill Refund", BillCategory.REFUND, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE),
    // FLOAT TRANSACTIONS
    FUND_SHIFT_START_BILL("Shift Start Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_STARTING_BALANCE, CountedServiceType.OTHER),
    FUND_SHIFT_START_BILL_CANCELLED("Shift Start Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_STARTING_BALANCE, CountedServiceType.OTHER),
    FUND_SHIFT_END_BILL("Shift End Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE, CountedServiceType.OTHER),
    FUND_SHIFT_END_BILL_CANCELLED("Shift End Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE, CountedServiceType.OTHER),
    FUND_TRANSFER_BILL("Fund Transfer Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE, CountedServiceType.OTHER),
    FUND_TRANSFER_BILL_CANCELLED("Fund Transfer Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE, CountedServiceType.OTHER),
    FUND_TRANSFER_RECEIVED_BILL("Fund Transfer Received Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE, CountedServiceType.OTHER),
    FUND_TRANSFER_RECEIVED_BILL_CANCELLED("Fund Transfer Received Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE, CountedServiceType.OTHER),
    FUND_DEPOSIT_BILL("Deposit Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.BANK_OUT, CountedServiceType.OTHER),
    FUND_DEPOSIT_BILL_CANCELLED("Deposit Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.BANK_IN, CountedServiceType.OTHER),
    FUND_WITHDRAWAL_BILL("Withdrawal Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.BANK_IN, CountedServiceType.OTHER),
    FUND_WITHDRAWAL_BILL_CANCELLED("Withdrawal Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.BANK_OUT, CountedServiceType.OTHER),
    // PROFESSIONAL PAYMENTS
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE("Inward Payment for Staff", BillCategory.PAYMENTS, ServiceType.INWARD, BillFinanceType.CASH_IN, CountedServiceType.INWARD_PROFESSIONAL_PAYMENT),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE("Channelling Payment for Staff", BillCategory.PAYMENTS, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION("Channelling session Payment for Staff", BillCategory.PAYMENTS, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES("Channelling Payment for Staff for agencies", BillCategory.PAYMENTS, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER_PROFESSIONAL_PAYMENT),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES("OPD Professional Payment bill", BillCategory.PAYMENTS, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_PROFESSIONAL_PAYMENT),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN("Inward Payment for Staff - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.INWARD, BillFinanceType.CASH_OUT, CountedServiceType.INWARD_PROFESSIONAL_PAYMENT_RETURN),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN("Channelling Payment for Staff - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.CHANNELLING, BillFinanceType.CASH_IN, CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT_RETURN),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES_RETURN("Channelling Payment for Staff for agencies - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN("OPD Professional Payment bill - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_PROFESSIONAL_PAYMENT_RETURN),
    PETTY_CASH_ISSUE("Petty Cash Issue", BillCategory.PAYMENTS, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER),
    PETTY_CASH_RETURN("Petty Cash Return", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER),
    PETTY_CASH_BILL_CANCELLATION("Petty Cash Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OTHER),
    IOU_CASH_ISSUE("Iou Cash Issue", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER),
    IOU_CASH_RETURN("Iou Cash Return", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER),
    STAFF_CREDIT_SETTLE("Staff Credit Settle", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER),
    PATIENT_DEPOSIT("Patient Deposit Settle", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER),
    PATIENT_DEPOSIT_REFUND("Patient Deposit - Refund", BillCategory.REFUND, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER),
    PATIENT_DEPOSIT_CANCELLED("Patient Deposit - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER);
    
    private final String label;
    private final BillCategory billCategory;
    private final ServiceType serviceType;
    private final BillFinanceType billFinanceType;
    private final CountedServiceType countedServiceType;

    BillTypeAtomic(String label, BillCategory billCategory, ServiceType serviceType, BillFinanceType billFinanceType, CountedServiceType countedServiceType) {
        this.label = label;
        this.billCategory = billCategory;
        this.serviceType = serviceType;
        this.billFinanceType = billFinanceType;
        this.countedServiceType = countedServiceType;
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
        return billFinanceType;
    }

    public CountedServiceType getCountedServiceType() {
        return countedServiceType;
    }

    // Method to find BillTypeAtomic by BillFinanceType
    public static List<BillTypeAtomic> findByFinanceType(BillFinanceType financeType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getBillFinanceType() == financeType)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by BillCategory
    public static List<BillTypeAtomic> findByCategory(BillCategory category) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getBillCategory() == category)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by ServiceType
    public static List<BillTypeAtomic> findByServiceType(ServiceType serviceType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getServiceType() == serviceType)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by ServiceType
    public static List<BillTypeAtomic> findByCountedServiceType(CountedServiceType counterServiceType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getCountedServiceType() == counterServiceType)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by ServiceType and BillFinanceType
    public static List<BillTypeAtomic> findByServiceTypeAndFinanceType(ServiceType serviceType, BillFinanceType financeType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getServiceType() == serviceType && e.getBillFinanceType() == financeType)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by ServiceType and BillCategory
    public static List<BillTypeAtomic> findBillTypeAtomic(ServiceType serviceType, BillCategory billCategory) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getServiceType() == serviceType && e.getBillCategory() == billCategory)
                .collect(Collectors.toList());
    }
}

package com.divudi.data;

import static com.divudi.data.BillType.PharmacyAddtoStock;
import static com.divudi.data.BillType.PharmacyAdjustment;
import static com.divudi.data.BillType.PharmacyAdjustmentDepartmentSingleStock;
import static com.divudi.data.BillType.PharmacyGrnBill;
import static com.divudi.data.BillType.PharmacyPre;
import static com.divudi.data.BillType.PharmacyPurchaseBill;
import static com.divudi.data.BillType.PharmacyWholeSale;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumerates types of bills for atomic billing purposes. Includes
 * categorization by service type, finance type, and payment category.
 */
public enum BillTypeAtomic {
    INWARD_PHARMACY_REQUEST("Inward Request Medicines From Pharmacy", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    INWARD_SERVICE_BATCH_BILL("Inward Service Batch Bill", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    INWARD_SERVICE_BATCH_BILL_CANCELLATION("Inward Service Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    INWARD_SERVICE_BILL("Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    INWARD_SERVICE_BILL_CANCELLATION("Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    INWARD_SERVICE_BATCH_BILL_REFUND("Opd Bill Refund", BillCategory.REFUND, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    INWARD_SERVICE_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.CREDIT_SPEND),
    // STORE
    STORE_ORDER("Store Order", BillCategory.BILL, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    STORE_ORDER_PRE("Store Order Pre", BillCategory.BILL, ServiceType.STORE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.STORE, PaymentCategory.CREDIT_SPEND),
    STORE_ORDER_CANCELLED("Store Order Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    STORE_ORDER_APPROVAL("Store Order Approval", BillCategory.BILL, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    STORE_ORDER_APPROVAL_CANCELLED("Store Order Approval Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    STORE_DIRECT_PURCHASE("Store Direct Purchase", BillCategory.BILL, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    STORE_DIRECT_PURCHASE_CANCELLED("Store Direct Purchase Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    STORE_DIRECT_PURCHASE_REFUND("Store Direct Purchase Refund", BillCategory.REFUND, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    STORE_GRN("Store GRN", BillCategory.PAYMENTS, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE, PaymentCategory.CREDIT_SPEND),
    STORE_GRN_CANCELLED("Store GRN Cancelled", BillCategory.CANCELLATION, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE, PaymentCategory.CREDIT_SPEND),
    STORE_GRN_REFUND("Store GRN Refund", BillCategory.REFUND, ServiceType.STORE, BillFinanceType.CASH_OUT, CountedServiceType.STORE, PaymentCategory.CREDIT_SPEND),
    STORE_GRN_RETURN("Store GRN Return", BillCategory.REFUND, ServiceType.STORE, BillFinanceType.CASH_IN, CountedServiceType.STORE, PaymentCategory.NON_CREDIT_SPEND),
    // PHARMACY
    PHARMACY_RETAIL_SALE("Pharmacy Retail Sale", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_RETAIL_SALE_PRE("Pharmacy Retail Sale Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY,
            PaymentCategory.NO_PAYMENT),
    PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER("Pharmacy Retail Sale Pre Bill Settled At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER("Pharmacy Retail Sale Pre Bill To Settle At Cashier", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY,
            PaymentCategory.NO_PAYMENT),
    PHARMACY_RETAIL_SALE_CANCELLED("Pharmacy Retail Sale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_RETAIL_SALE_REFUND("Pharmacy Retail Sale Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY("Pharmacy Retail Sale Return Items Only", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY,
            PaymentCategory.NO_PAYMENT),
    PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS("Pharmacy Retail Sale Return Item Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS("Pharmacy Retail Sale Return Items And Payments", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_SALE_WITHOUT_STOCK("Pharmacy Sale Without Stock", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_SALE_WITHOUT_STOCK_PRE("Pharmacy Sale Without Stock Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY,
            PaymentCategory.NO_PAYMENT),
    PHARMACY_SALE_WITHOUT_STOCK_CANCELLED("Pharmacy Sale Without Stock Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_SALE_WITHOUT_STOCK_REFUND("Pharmacy Sale Without Stock Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK("Pharmacy Retail Sale Pre Bill Add to Stock", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY,
            PaymentCategory.CREDIT_SPEND),
    PHARMACY_WHOLESALE("Pharmacy Wholesale", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_WHOLESALE_PRE("Pharmacy Wholesale Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY,
            PaymentCategory.CREDIT_SPEND),
    PHARMACY_WHOLESALE_CANCELLED("Pharmacy Wholesale Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_WHOLESALE_REFUND("Pharmacy Wholesale Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_ORDER("Pharmacy Order", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_ORDER_PRE("Pharmacy Order Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY,
            PaymentCategory.NO_PAYMENT),
    PHARMACY_ORDER_CANCELLED("Pharmacy Order Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_ORDER_APPROVAL("Pharmacy Order Approval", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_ORDER_APPROVAL_CANCELLED("Pharmacy Order Approval Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_DIRECT_PURCHASE("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_DIRECT_PURCHASE_CANCELLED("Pharmacy Direct Purchase Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_DIRECT_PURCHASE_REFUND("Pharmacy Direct Purchase Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY,
            PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_GRN("Pharmacy GRN", BillCategory.PAYMENTS, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_GRN_PRE("Pharmacy GRN Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_GRN_WHOLESALE("Pharmacy Wholesale GRN", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_GRN_CANCELLED("Pharmacy GRN Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_GRN_REFUND("Pharmacy GRN Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_GRN_RETURN("Pharmacy GRN Return", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL("Pharmacy Direct Purchase", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_CANCELLED("Pharmacy Direct Purchase - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_WHOLESALE_DIRECT_PURCHASE_BILL_REFUND("Pharmacy Direct Purchase - Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_WHOLESALE_GRN_BILL("Pharmacy GRN", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_WHOLESALE_GRN_BILL_CANCELLED("Pharmacy GRN - Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_WHOLESALE_GRN_BILL_REFUND("Pharmacy GRN - Refund", BillCategory.REFUND, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_GRN_PAYMENT("GRN Payment", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_GRN_PAYMENT_CANCELLED("GRN Payment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    PHARMACY_ADJUSTMENT("Pharmacy Adjustment", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_ADJUSTMENT_CANCELLED("Pharmacy Adjustment Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_TRANSFER_REQUEST("Pharmacy Transfer Request", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_TRANSFER_REQUEST_PRE("Pharmacy Transfer Request Pre", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_TRANSFER_REQUEST_CANCELLED("Pharmacy Transfer Request Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_ISSUE("Pharmacy Issue", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_ISSUE_CANCELLED("Pharmacy Issue Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_DIRECT_ISSUE("Pharmacy Direct Issue", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_DIRECT_ISSUE_CANCELLED("Pharmacy Direct Issue Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_RECEIVE("Pharmacy Receive", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_RECEIVE_PRE("Pharmacy Receive Request", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_RECEIVE_CANCELLED("Pharmacy Receive Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    MULTIPLE_PHARMACY_ORDER_CANCELLED_BILL("Multiple Pharmacy Purchase Order Cancelled", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.CREDIT_SPEND),
    PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION("Pharmacy Return Items And Payments Cancellation", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_SPEND),
    // CHANNELLING
    CHANNEL_BOOKING_WITH_PAYMENT("Channel Booking and Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.CASH_IN, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_PENDING_PAYMENT("Channel Booking For Online Payment - Pending Confirmation", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.CREDIT_SPEND),
    CHANNEL_RESHEDULE_WITH_PAYMENT("Channel Reschedule for paid Appointment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.CREDIT_SPEND),
    CHANNEL_RESHEDULE_WITH_OUT_PAYMENT("Channel Reschedule For Non Paid Appointment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.CREDIT_SPEND),
    @Deprecated
    CHANNEL_BOOKING_PAYMENT("Channel Booking For Online Payment - Pending Confirmation", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_BOOKING_WITH_PAYMENT_ONLINE("Channel Booking Online Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.ONLINE_PAYMENT_IN, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_BOOKING_WITHOUT_PAYMENT("Channel Booking without Payment", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.CREDIT_SPEND),
    CHANNEL_PAYMENT_FOR_BOOKING_BILL("Channel Payment for Booking", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.CASH_IN, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_CANCELLATION_WITH_PAYMENT("Channel Cancellation with Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_CANCELLATION_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS("Channel Cancellation with Payment for Bills Where Credit Payment was Settled", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_REFUND_WITH_PAYMENT("Channel Refund with Payment", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_REFUND_WITH_PAYMENT_FOR_CREDIT_SETTLED_BOOKINGS("Channel Refund with Payment for Bills where Credit Payment was Settled", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    CHANNEL_CANCELLATION_WITHOUT_PAYMENT("Channel Cancellation without Payment", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.CREDIT_SPEND),
    CHANNEL_REFUND("Channel Refund", BillCategory.REFUND, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_SPEND),
    // OPD_IN
    OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Batch Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.NONE, PaymentCategory.NO_PAYMENT),
    OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Opd Batch Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.NONE, PaymentCategory.NON_CREDIT_SPEND),
    OPD_BATCH_BILL_WITH_PAYMENT("Opd Batch Bill with Payment Collection", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.NONE, PaymentCategory.NON_CREDIT_SPEND),
    OPD_BATCH_BILL_CANCELLATION("Opd Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.NONE, PaymentCategory.CREDIT_SPEND),
    OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.NONE, PaymentCategory.CREDIT_SPEND),
    OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("OPD Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    OPD_BILL_WITH_PAYMENT("OPD Bill with Payment", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    OPD_BILL_CANCELLATION("Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_OUT, PaymentCategory.NON_CREDIT_SPEND),
    OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_OUT, PaymentCategory.NON_CREDIT_SPEND),
    OPD_BILL_REFUND("Opd Bill Refund", BillCategory.REFUND, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_OUT, PaymentCategory.NON_CREDIT_SPEND),
    OPD_PROFESSIONAL_PAYMENT_BILL("OPD Professional Payment bill", BillCategory.PAYMENTS, ServiceType.PROFESSIONAL_PAYMENT, BillFinanceType.CASH_OUT, CountedServiceType.OPD_PROFESSIONAL_PAYMENT, PaymentCategory.NON_CREDIT_SPEND),
    OPD_PROFESSIONAL_PAYMENT_BILL_RETURN("OPD Professional Payment bill Return", BillCategory.PAYMENTS, ServiceType.PROFESSIONAL_PAYMENT, BillFinanceType.CASH_IN, CountedServiceType.OPD_PROFESSIONAL_PAYMENT_RETURN, PaymentCategory.NON_CREDIT_SPEND),
    // PACKAGES
    PACKAGE_OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Package Opd Batch Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN, PaymentCategory.CREDIT_SPEND),
    PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER("Package Opd Batch Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT("Package Opd Batch Bill with Payment Collection", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    PACKAGE_OPD_BATCH_BILL_CANCELLATION("Package Opd Batch Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN, PaymentCategory.CREDIT_SPEND),
    PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER("Package Opd Bill to Collect Payment at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.OPD_IN, PaymentCategory.CREDIT_SPEND),
    PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER("Package OPD Bill Payment Collection at Cashier", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    PACKAGE_OPD_BILL_WITH_PAYMENT("Package OPD Bill with Payment", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    PACKAGE_OPD_BILL_CANCELLATION("Package Opd Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION("Package Opd Bill Cancellation with Batch Bill", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    PACKAGE_OPD_BILL_REFUND("Package Opd Bill Refund", BillCategory.REFUND, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.OPD_IN, PaymentCategory.NON_CREDIT_SPEND),
    // COLLECTING CENTRE
    CC_BILL("Collecting Centre Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_IN, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.NON_CREDIT_SPEND),
    CC_BILL_CANCELLATION("Collecting Centre Bill Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.NON_CREDIT_SPEND),
    CC_BILL_REFUND("Collecting Centre Bill Refund", BillCategory.REFUND, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.NON_CREDIT_SPEND),
    CC_PAYMENT_RECEIVED_BILL("Collecting Centre Payment Received Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_IN, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.NON_CREDIT_SPEND),
    CC_PAYMENT_CANCELLATION_BILL("Collecting Centre Payment Cancellation Bill", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.NON_CREDIT_SPEND),
    CC_CREDIT_NOTE("Collecting Centre Credit Note", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.CREDIT_SPEND),
    CC_DEBIT_NOTE("Collecting Centre Debit Note", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.CREDIT_SPEND),
    CC_CREDIT_NOTE_CANCELLATION("Collecting Centre Credit Note Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.CREDIT_SPEND),
    CC_DEBIT_NOTE_CANCELLATION("Collecting Centre Debit Note Cancellation", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.CREDIT_SPEND),
    CC_PAYMENT_MADE_BILL("Collecting Centre Payment Made Bill", BillCategory.BILL, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.NON_CREDIT_SPEND),
    CC_PAYMENT_MADE_CANCELLATION_BILL("Collecting Centre Payment Made Cancellation Bill", BillCategory.CANCELLATION, ServiceType.COLLECTING_CENTRE, BillFinanceType.CASH_OUT, CountedServiceType.COLLECTING_CENTRE, PaymentCategory.NON_CREDIT_SPEND),
    // FLOAT TRANSACTIONS
    FUND_SHIFT_START_BILL("Shift Start Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_STARTING_BALANCE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    FUND_SHIFT_START_BILL_CANCELLED("Shift Start Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_STARTING_BALANCE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    FUND_SHIFT_END_BILL("Shift End Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    FUND_SHIFT_COMPONANT_HANDOVER_CREATE("Shift Handover Componant Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE, CountedServiceType.OTHER, PaymentCategory.OTHER),
    FUND_SHIFT_HANDOVER_CREATE("Shift Handover Create Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE, CountedServiceType.OTHER, PaymentCategory.OTHER),
    FUND_SHIFT_HANDOVER_ACCEPT("Shift Handover Accept", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE, CountedServiceType.OTHER, PaymentCategory.OTHER),
    FUND_SHIFT_END_BILL_CANCELLED("Shift End Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_CLOSING_BALANCE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    FUND_TRANSFER_BILL("Fund Transfer Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    FUND_TRANSFER_BILL_CANCELLED("Fund Transfer Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    FUND_TRANSFER_RECEIVED_BILL("Fund Transfer Received Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    FUND_TRANSFER_RECEIVED_BILL_CANCELLED("Fund Transfer Received Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    // ENUM EXTENSION FOR NEW BILL TYPES
    FUND_SHIFT_SHORTAGE_BILL("Shift Shortage Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    FUND_SHIFT_SHORTAGE_BILL_CANCELLED("Shift Shortage Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_DECREASE, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),
    FUND_SHIFT_EXCESS_BILL("Shift Excess Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    FUND_SHIFT_EXCESS_BILL_CANCELLED("Shift Excess Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.FLOAT_INCREASE, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    TRANSFER_PAYMENT_METHOD_BILL("Transfer Payment Method Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    TRANSFER_PAYMENT_METHOD_BILL_CANCELLED("Transfer Payment Method Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER, PaymentCategory.CREDIT_SPEND),

// PROFESSIONAL PAYMENTS
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE("Inward Payment for Staff", BillCategory.PAYMENTS, ServiceType.INWARD, BillFinanceType.CASH_IN, CountedServiceType.INWARD_PROFESSIONAL_PAYMENT, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE("Channelling Payment for Staff", BillCategory.PAYMENTS, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION("Channelling session Payment for Staff", BillCategory.PAYMENTS, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES("Channelling Payment for Staff for agencies", BillCategory.PAYMENTS, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER_PROFESSIONAL_PAYMENT, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES("OPD Professional Payment bill", BillCategory.PAYMENTS, ServiceType.PROFESSIONAL_PAYMENT, BillFinanceType.CASH_OUT, CountedServiceType.OPD_PROFESSIONAL_PAYMENT, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN("Inward Payment for Staff - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.INWARD, BillFinanceType.CASH_OUT, CountedServiceType.INWARD_PROFESSIONAL_PAYMENT_RETURN, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN("Channelling Payment for Staff - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.CHANNELLING, BillFinanceType.CASH_IN, CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT_RETURN, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_FOR_AGENCIES_RETURN("Channelling Payment for Staff for agencies - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES_RETURN("OPD Professional Payment bill - Return and Cancellation", BillCategory.PAYMENTS, ServiceType.PROFESSIONAL_PAYMENT, BillFinanceType.CASH_IN, CountedServiceType.OPD_PROFESSIONAL_PAYMENT_RETURN, PaymentCategory.NON_CREDIT_SPEND),
    // PETTY CASH AND OTHERS
    PETTY_CASH_ISSUE("Petty Cash Issue", BillCategory.PAYMENTS, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    PETTY_CASH_RETURN("Petty Cash Return", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    PETTY_CASH_BILL_CANCELLATION("Petty Cash Bill Cancellation", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    IOU_CASH_ISSUE("Iou Cash Issue", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    IOU_CASH_RETURN("Iou Cash Return", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    STAFF_CREDIT_SETTLE("Staff Credit Settle", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    PATIENT_DEPOSIT("Patient Deposit Settle", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.CASH_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    PATIENT_DEPOSIT_REFUND("Patient Deposit - Refund", BillCategory.REFUND, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    PATIENT_DEPOSIT_CANCELLED("Patient Deposit - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.CASH_OUT, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    //Agency Payments
    AGENCY_PAYMENT_RECEIVED("Agency Payment Received", BillCategory.BILL, ServiceType.AGENCY, BillFinanceType.CASH_IN, CountedServiceType.AGENCY, PaymentCategory.NON_CREDIT_SPEND),
    AGENCY_PAYMENT_CANCELLATION("Agency Payment Cancellation", BillCategory.CANCELLATION, ServiceType.AGENCY, BillFinanceType.CASH_OUT, CountedServiceType.AGENCY, PaymentCategory.NON_CREDIT_SPEND),
    AGENCY_CREDIT_NOTE("Agency Credit Note", BillCategory.BILL, ServiceType.AGENCY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.AGENCY, PaymentCategory.CREDIT_SPEND),
    AGENCY_CREDIT_NOTE_CANCELLATION("Agency Credit Note Cancellation", BillCategory.CANCELLATION, ServiceType.AGENCY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.AGENCY, PaymentCategory.CREDIT_SPEND),
    AGENCY_DEBIT_NOTE("Agency Debit Note", BillCategory.BILL, ServiceType.AGENCY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.AGENCY, PaymentCategory.CREDIT_SPEND),
    AGENCY_DEBIT_NOTE_CANCELLATION("Agency Debit Note Cancellation", BillCategory.CANCELLATION, ServiceType.AGENCY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.AGENCY, PaymentCategory.CREDIT_SPEND),
    // OPD Payments from Credit Companies
    OPD_CREDIT_COMPANY_PAYMENT_RECEIVED("OPD Payment Received from Credit Company", BillCategory.BILL, ServiceType.OPD, BillFinanceType.CASH_IN, CountedServiceType.AGENCY, PaymentCategory.NON_CREDIT_COLLECTION),
    OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION("OPD Payment Cancellation from Credit Company", BillCategory.CANCELLATION, ServiceType.OPD, BillFinanceType.CASH_OUT, CountedServiceType.AGENCY, PaymentCategory.NON_CREDIT_COLLECTION),
    OPD_CREDIT_COMPANY_CREDIT_NOTE("OPD Credit Note from Credit Company", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.AGENCY, PaymentCategory.NON_CREDIT_COLLECTION),
    OPD_CREDIT_COMPANY_DEBIT_NOTE("OPD Debit Note from Credit Company", BillCategory.BILL, ServiceType.OPD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.AGENCY, PaymentCategory.NON_CREDIT_COLLECTION),
    // Pharmacy Payments from Credit Companies
    PHARMACY_CREDIT_COMPANY_PAYMENT_RECEIVED("Pharmacy Payment Received from Credit Company", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.CASH_IN, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_CREDIT_COMPANY_PAYMENT_CANCELLATION("Pharmacy Payment Cancellation from Credit Company", BillCategory.CANCELLATION, ServiceType.PHARMACY, BillFinanceType.CASH_OUT, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_CREDIT_COMPANY_CREDIT_NOTE("Pharmacy Credit Note from Credit Company", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_COLLECTION),
    PHARMACY_CREDIT_COMPANY_DEBIT_NOTE("Pharmacy Debit Note from Credit Company", BillCategory.BILL, ServiceType.PHARMACY, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.PHARMACY, PaymentCategory.NON_CREDIT_COLLECTION),
    // Inpatient Payments from Credit Companies
    INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED("Inpatient Payment Received from Credit Company", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.CASH_IN, CountedServiceType.INWARD, PaymentCategory.NON_CREDIT_COLLECTION),
    INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION("Inpatient Payment Cancellation from Credit Company", BillCategory.CANCELLATION, ServiceType.INWARD, BillFinanceType.CASH_OUT, CountedServiceType.INWARD, PaymentCategory.NON_CREDIT_COLLECTION),
    INPATIENT_CREDIT_COMPANY_CREDIT_NOTE("Inpatient Credit Note from Credit Company", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.NON_CREDIT_COLLECTION),
    INPATIENT_CREDIT_COMPANY_DEBIT_NOTE("Inpatient Debit Note from Credit Company", BillCategory.BILL, ServiceType.INWARD, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.NON_CREDIT_COLLECTION),
    // Channelling Payments from Credit Companies
    CHANNELLING_CREDIT_COMPANY_PAYMENT_RECEIVED("Channelling Payment Received from Credit Company", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.CASH_IN, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_COLLECTION),
    CHANNELLING_CREDIT_COMPANY_PAYMENT_CANCELLATION("Channelling Payment Cancellation from Credit Company", BillCategory.CANCELLATION, ServiceType.CHANNELLING, BillFinanceType.CASH_OUT, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_COLLECTION),
    CHANNELLING_CREDIT_COMPANY_CREDIT_NOTE("Channelling Credit Note from Credit Company", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_COLLECTION),
    CHANNELLING_CREDIT_COMPANY_DEBIT_NOTE("Channelling Debit Note from Credit Company", BillCategory.BILL, ServiceType.CHANNELLING, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.CHANNELLING, PaymentCategory.NON_CREDIT_COLLECTION),
    // BANKING TRANSACTIONS
    FUND_DEPOSIT_BILL("Deposit Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.BANK_OUT, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    FUND_DEPOSIT_BILL_CANCELLED("Deposit Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.BANK_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    FUND_WITHDRAWAL_BILL("Withdrawal Fund Bill", BillCategory.BILL, ServiceType.OTHER, BillFinanceType.BANK_IN, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),
    FUND_WITHDRAWAL_BILL_CANCELLED("Withdrawal Fund Bill - Cancelled", BillCategory.CANCELLATION, ServiceType.OTHER, BillFinanceType.BANK_OUT, CountedServiceType.OTHER, PaymentCategory.NON_CREDIT_SPEND),;

    private final String label;
    private final BillCategory billCategory;
    private final ServiceType serviceType;
    private final BillFinanceType billFinanceType;
    private final CountedServiceType countedServiceType;
    private final PaymentCategory paymentCategory;

    public static BillTypeAtomic getBillTypeAtomic(BillType billType, BillClassType billClassType) {
        switch (billClassType) {
            case Bill:
                switch (billType) {
                    case PharmacyWholeSale:
                        return BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL;
                    case PharmacyPurchaseBill:
                        return BillTypeAtomic.PHARMACY_DIRECT_PURCHASE;
                }
                break;
            case BilledBill:
                switch (billType) {
                    case PharmacyGrnBill:
                        return BillTypeAtomic.PHARMACY_GRN;
                    case PharmacyWholeSale:
                        return BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL;
                    case PharmacyPurchaseBill:
                        return BillTypeAtomic.PHARMACY_DIRECT_PURCHASE;
                }
            case CancelledBill:
                switch (billType) {
                    case PharmacyGrnBill:
                        return BillTypeAtomic.PHARMACY_GRN_CANCELLED;
                    case PharmacyWholeSale:
                        return BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL_CANCELLED;
                    case PharmacyPurchaseBill:
                        return BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED;
                }
            case OtherBill:
                return null;
            case PreBill:
                return null;
            case RefundBill:
                switch (billType) {
                    case PharmacyGrnBill:
                        return BillTypeAtomic.PHARMACY_GRN_REFUND;
                    case PharmacyWholeSale:
                        return BillTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL_REFUND;
                    case PharmacyPurchaseBill:
                        return BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED;
                }

            default:
                return null;
        }
        return null;
    }

    BillTypeAtomic(String label, BillCategory billCategory, ServiceType serviceType, BillFinanceType billFinanceType, CountedServiceType countedServiceType, PaymentCategory paymentCategory) {
        this.label = label;
        this.billCategory = billCategory;
        this.serviceType = serviceType;
        this.billFinanceType = billFinanceType;
        this.countedServiceType = countedServiceType;
        this.paymentCategory = paymentCategory;
    }

    public static List<BillTypeAtomic> findByServiceTypeAndPaymentCategory(ServiceType serviceType, PaymentCategory paymentCategory) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(billType -> billType.getServiceType() == serviceType && billType.getPaymentCategory() == paymentCategory)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by BillFinanceType
    public static List<BillTypeAtomic> findByFinanceType(BillFinanceType financeType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getBillFinanceType() == financeType)
                .collect(Collectors.toList());
    }

    public PaymentCategory getPaymentCategory() {
        return paymentCategory;
    }

    // Method to find BillTypeAtomic by BillCategory
    public static List<BillTypeAtomic> findByCategory(BillCategory category) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(e -> e.getBillCategory() == category)
                .collect(Collectors.toList());
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

// Method to find BillTypeAtomic by ServiceType
    public static List<BillTypeAtomic> findByServiceType(ServiceType serviceType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(billType -> billType.getServiceType() == serviceType)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by CountedServiceType
    public static List<BillTypeAtomic> findByCountedServiceType(CountedServiceType countedServiceType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(billType -> billType.getCountedServiceType() == countedServiceType)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by ServiceType and BillFinanceType
    public static List<BillTypeAtomic> findByServiceTypeAndFinanceType(ServiceType serviceType, BillFinanceType financeType) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(billType -> billType.getServiceType() == serviceType && billType.getBillFinanceType() == financeType)
                .collect(Collectors.toList());
    }

    // Method to find BillTypeAtomic by ServiceType and BillCategory
    public static List<BillTypeAtomic> findBillTypeAtomic(ServiceType serviceType, BillCategory billCategory) {
        return Arrays.stream(BillTypeAtomic.values())
                .filter(billType -> billType.getServiceType() == serviceType && billType.getBillCategory() == billCategory)
                .collect(Collectors.toList());
    }

}

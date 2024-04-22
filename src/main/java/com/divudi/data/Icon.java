package com.divudi.data;

/**
 * Enum for defining various icons with human-readable labels. Note: Image and
 * action paths are removed as per request.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public enum Icon {
//for OPD Section
    Patient_Lookup("Patient Lookup"),
    Patient_Add_New("Add New Patient"),
    
    Opd_Billing("OPD Billing"),
    Billing_For_Cashier("Billing for Cashier"),
    Collecting_Centre_Billing("Collecting Centre Billing"),
    Medical_Package_Billing("Medical Package Billing"),
    
    Scan_to_Pay("Scan to Pay"),
    Accept_Payments("Accept Payments"),
    Accept_Payments_For_OPD_Bills("Accept Payments for OPD Bills"),
    Accept_Payments_For_OPD_Batch_Bills("Accept Payments for OPD Batch Bills"),
    Accept_Payments_For_Pharmacy_Bills("Accept Payments for Pharmacy Bills"),
    Refunds("Refunds"),
    RefundsforOPDBills("Refunds for OPD Bills"),
    RefundsforPharmacyBills("Refunds for Pharmacy Bills"),
    
    Search_Opd_Bill("Search OPD Bill"),
    Search_Opd_Bill_Item("Search OPD Bill Item"),
    Search_Opd_Payment("Search OPD Payment"),
    Search_Collecting_Centre_Bill("Search Collecting Centre Bill"),
    Search_Bill_to_Pay("Search Bill to Pay"),
    Search_Credit_Paid_Bill("Search Credit Paid Bill"),
    Search_Credit_Paid_Bill_with_OPD_Bill("Search Credit Paid Bill with OPD Bill"),

    Doctor_Mark_in("Doctor Mark in"),
    Doctor_Mark_out("Doctor Mark out"),
    Doctor_Working_Times("Doctor Working Times"),
    
    Manage_Token("Manage Token"),
    
    Lab_Report_Print("Lab Report Print"),
    
    OPD_Summaries ("OPD Summaries"),
    
    OPD_Analytics ("OPD Analytics"),
    
//for Pharmacy
    pharmacy_sale ("Pharmacy - Sale"),
    pharmacy_sale_for_cashier ("Pharmacy - Sale for cashier"),
    pharmacy_sale_without_stock ("Pharmacy - Sale without Stock"),
    pharmacy_Search_sale_bill ("Pharmacy - Search Sale Bill"),
    pharmacy_search_sale_pre_bill ("Pharmacy - Search Sale Pre Bill"),
    pharmacy_search_sale_bill_item ("Pharmacy - Search Sale Bill Item"),
    pharmacy_return_items_only ("Pharmacy - Return Items Only"),
    pharmacy_return_items_and_payments ("Pharmacy - Return Items and Payments"),
    pharmacy_search_return_bill ("Pharmacy - Search Return Bill"),
    pharmacy_add_to_stock ("Pharmacy - Add to Stock"),
    
    wholesale_sale ("Wholesale - Sale"),
    wholesale_sale_for_cashier ("Wholesale - Sale for cashier"),
    wholesale_Search_sale_bill ("Wholesale - Search Sale Bill"),
    wholesale_Search_sale_bill_to_pay ("Wholesale - Search Sale Bill to Pay"),
    wholesale_search_sale_bill_item ("Wholesale - Search Sale Bill Item"),
    wholesale_return_items_only ("Wholesale - Return (Items Only)"),
    wholesale_return_items_and_payments ("Wholesale - Return (Items and Payments)"),
    wholesale_search_return_bill_item ("Wholesale - Search Return Bill (Item)"),
    wholesale_add_to_stock ("Wholesale - Add to Stock"),
    wholesale_purchase ("Wholesale - Wholesale Purchase"),
    
    direct_issue_to_BHTs ("Direct Issue to BHTs"),
    direct_issue_to_theatre ("Direct Issue to Theatre"),
    BHT_issue_requests ("BHT Issue Request"),
    search_inpatint_direct_issue_by_bill ("Search Inpatint Direct Issues by Bill"),
    search_inpatint_direct_issue_by_item ("Search Inpatint Direct Issues by Item"),
    search_inpatint_direct_issue_returns_by_bill ("Search Inpatint Direct Issues Returns by Bill"),
    search_inpatint_direct_issue_returns_by_item ("Search Inpatint Direct Issues Returns by Bill"),
    
    Create_Purchase_Order("Create Purchase Order"),
    Auto_Order_P_Model("Auto Order (P Model)"),
    Auto_Order_Q_Model("Auto Order (Q Model)"),
    Direct_Purchase("Direct Purchase"),
    Purchase_Orders_Approval("Purchase Orders Approval"),
    Goods_Receipt("Goods Receipt"),
    Return_Received_Goods("Return Received Goods"),
    Return_without_Receipt("Return without Receipt"),
    Multiple_Purchase_Order_Cancellation("Multiple Purchase Order Cancellation"),
    
    pharmacy_issue("Pharmacy - Issue"),
    pharmacy_search_issue_bill("Pharmacy - Search Issue Bill"),
    pharmacy_search_issue_bill_items("Pharmacy - Search Issue Bill Items"),
    pharmacy_search_issue_return_bill("Pharmacy - Search Issue Return Bill"),
    pharmacy_uint_issue_margin("Pharmacy - Uint Issue Margin"),
    
    pharmacy_request("Pharmacy - Request"),
    pharmacy_issue_for_request("Pharmacy - Issue for Request"),
    pharmacy_direct_issue("Pharmacy - Direct Issue"),
    pharmacy_receive("Pharmacy - Receive"),
    pharmacy_disbursement_reports("Pharmacy - Disbursement Reports"),
    
    adjustments_Depaetment_stock("Adjustments - Depaetment Stock"),
    adjustments_staff_stock("Adjustments - Staff Stock"),
    adjustments_purchase_rate("Adjustments - Purchase Rate"),
    adjustments_sale_rate("Adjustments - Sale Rate"),
    adjustments_wholesale_rate("Adjustments - Wholesale Rate"),
    adjustments_expiry_date("Adjustments - Expiry Date"),
    adjustments_search_adjustment_bill("Adjustments - Search Adjustment Bill"),
    adjustments_transfer_all_stock("Adjustments - Transfer All Stock"),
    
    supplier_due_search("Supplier Due Search"),
    dealer_due_by_age("Dealer Due by Age"),
    payment_by_supplier("Payment by Supplier"),
    payment_by_bill("Payment by Bill"),
    GRN_payment_approve("GRN Payment Approve"),
    GRN_payment_done_search("GRN Payment done search"),
    credit_dues_and_access("Credit Dues and Access"),
    
    pharmacy_item_search("Pharmacy - Item Search"),
    
    pharmacy_generate_report("Pharmacy - Generate Report"),
    
    pharmacy_summary_views("Pharmacy - Summary Views"),
    
    
    
    
    Channel_Booking("Channel Booking"),
    Cashier_Summaries("Cashier Summaries"),
    Shift_End_Summary("Shift End Summary"),
    Day_End_Summary("Day End Summary"),
    Admit("Admit Patient"),
    Manage_Shift_Fund_Bills("Manage Shift Fund Bills"),
    ;

    private final String label;

    Icon(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
